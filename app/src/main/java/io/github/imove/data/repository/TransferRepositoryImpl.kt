package io.github.imove.data.repository

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.imove.domain.model.MediaFile
import io.github.imove.domain.model.TransferItem
import io.github.imove.domain.model.TransferStatus
import io.github.imove.domain.repository.TransferRepository
import io.github.imove.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import io.github.imove.R
import java.io.BufferedOutputStream
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: UserPreferencesRepository
) : TransferRepository {

    companion object {
        private const val TAG = "TransferRepo"
        private const val BUFFER_SIZE = 262144
    }

    private val _queue = MutableStateFlow<List<TransferItem>>(emptyList())
    private val _transferredFileIds = MutableStateFlow<Set<String>>(emptySet())
    private val _failedFileIds = MutableStateFlow<Set<String>>(emptySet())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val copySemaphore = Semaphore(6)

    @Volatile private var cachedTargetUri: Uri? = null
    @Volatile private var cachedTreeDoc: DocumentFile? = null

    private fun toast(msg: String) {
        Log.d(TAG, msg)
        mainHandler.post { Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
    }

    override fun addToQueue(files: List<MediaFile>) {
        // Centralized de-dup: skip files already queued/transferring or already transferred,
        // so every entry point (grid tap, date-group button, preview FAB) is covered.
        val existingFileIds = _queue.value.mapTo(mutableSetOf()) { it.file.id }
        val transferred = _transferredFileIds.value
        val newItems = files
            .distinctBy { it.id }
            .filter { it.id !in existingFileIds && it.id !in transferred }
            .map { file ->
                TransferItem(
                    id = UUID.randomUUID().toString(),
                    file = file,
                    status = TransferStatus.QUEUED
                )
            }
        if (newItems.isEmpty()) return
        // Re-queueing a previously-failed file clears its failed mark (this is the retry path).
        _failedFileIds.update { it - newItems.map { item -> item.file.id }.toSet() }
        _queue.update { it + newItems }
        for (item in newItems) {
            scope.launch {
                copySemaphore.withPermit { copyFile(item) }
            }
        }
    }

    private suspend fun resolveTargetDir(): Pair<Uri, DocumentFile> {
        val prefs = preferencesRepository.getPreferences().first()
        val targetDir = prefs.targetDirectory
        if (targetDir.isBlank()) throw Exception(context.getString(R.string.error_no_target_dir))
        val uri = Uri.parse(targetDir)
        // Reuse the cached DocumentFile only when it still points at the current target.
        // Without this check, changing the save location in Settings has no effect until restart.
        cachedTreeDoc?.let { doc -> if (cachedTargetUri == uri) return Pair(uri, doc) }
        if (uri.scheme != "content") throw Exception(context.getString(R.string.error_invalid_target))
        val doc = DocumentFile.fromTreeUri(context, uri)
            ?: throw Exception(context.getString(R.string.error_cannot_write_target))
        if (!doc.canWrite()) throw Exception(context.getString(R.string.error_cannot_write_target))
        cachedTargetUri = uri
        cachedTreeDoc = doc
        return Pair(uri, doc)
    }

    private suspend fun copyFile(item: TransferItem) {
        val fileName = item.file.name
        try {
            _queue.update { list ->
                list.map { if (it.id == item.id) it.copy(status = TransferStatus.TRANSFERRING) else it }
            }

            val (_, treeDoc) = resolveTargetDir()
            val destFile = treeDoc.createFile(item.file.mimeType, fileName)
                ?: throw Exception(context.getString(R.string.error_create_file, fileName))
            val destUri = destFile.uri

            val sourceUri = Uri.parse(item.file.path)
            val pfd = context.contentResolver.openFileDescriptor(sourceUri, "r")
                ?: throw Exception(context.getString(R.string.error_read_source, item.file.path))
            // AutoCloseInputStream owns the pfd and closes it exactly once when the stream closes
            // (also closes it if openOutputStream below throws).
            ParcelFileDescriptor.AutoCloseInputStream(pfd).use { input ->
                // statSize is reliable for removable mass-storage volumes; -1 if unknown.
                val declaredSize = pfd.statSize
                val dst = context.contentResolver.openOutputStream(destUri)
                    ?: throw Exception(context.getString(R.string.error_write_dest, destUri.toString()))
                BufferedOutputStream(dst, BUFFER_SIZE).use { output ->
                    val buf = ByteArray(BUFFER_SIZE)
                    if (declaredSize >= 0) {
                        // Size-bounded copy: some SAF providers for removable volumes block on
                        // the trailing read() instead of returning -1, which hangs the coroutine
                        // (file is fully written but the item never leaves the queue). Stop once
                        // we've copied exactly declaredSize bytes.
                        var remaining = declaredSize
                        while (remaining > 0) {
                            val toRead = minOf(remaining, BUFFER_SIZE.toLong()).toInt()
                            val read = input.read(buf, 0, toRead)
                            if (read < 0) break
                            output.write(buf, 0, read)
                            remaining -= read
                        }
                        // Source ended before we got all expected bytes: fail loudly (and keep it
                        // retryable) rather than silently marking a truncated file as transferred.
                        if (remaining > 0) {
                            throw IOException("Incomplete copy: $remaining/$declaredSize bytes missing")
                        }
                    } else {
                        var read: Int
                        while (input.read(buf).also { read = it } != -1) {
                            output.write(buf, 0, read)
                        }
                    }
                }
            }

            Log.d(TAG, "Copied $fileName")
            _transferredFileIds.update { it + item.file.id }
            _queue.update { list -> list.filter { it.id != item.id } }
        } catch (e: Exception) {
            Log.e(TAG, "Failed: $fileName", e)
            toast(context.getString(R.string.copy_failed, fileName, e.message ?: ""))
            // Drop the failed item out of the active queue and mark it failed, so the queue
            // count / "Done" state stay correct and the file can be retried by tapping it again.
            _failedFileIds.update { it + item.file.id }
            _queue.update { list -> list.filter { it.id != item.id } }
        }
    }

    override fun getQueue(): Flow<List<TransferItem>> = _queue.asStateFlow()
    override fun getTransferredFileIds(): StateFlow<Set<String>> = _transferredFileIds.asStateFlow()
    override fun getFailedFileIds(): StateFlow<Set<String>> = _failedFileIds.asStateFlow()

    override fun clearTransferredIds() {
        _transferredFileIds.value = emptySet()
        _failedFileIds.value = emptySet()
    }
}
