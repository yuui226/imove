package io.github.imove.data.repository

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.imove.domain.model.MediaFile
import io.github.imove.domain.model.TransferItem
import io.github.imove.domain.model.TransferStatus
import io.github.imove.domain.repository.MediaRepository
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
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaRepository: MediaRepository,
    private val preferencesRepository: UserPreferencesRepository
) : TransferRepository {

    companion object {
        private const val TAG = "TransferRepo"
        private const val BUFFER_SIZE = 262144
    }

    private val _queue = MutableStateFlow<List<TransferItem>>(emptyList())
    private val _transferredFileIds = MutableStateFlow<Set<String>>(emptySet())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val copySemaphore = Semaphore(20)

    @Volatile private var cachedTargetUri: Uri? = null
    @Volatile private var cachedTreeDoc: DocumentFile? = null

    private fun toast(msg: String) {
        Log.d(TAG, msg)
        mainHandler.post { Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
    }

    override fun addToQueue(files: List<MediaFile>) {
        val newItems = files.map { file ->
            TransferItem(
                id = UUID.randomUUID().toString(),
                file = file,
                status = TransferStatus.QUEUED,
                addedAt = System.currentTimeMillis()
            )
        }
        _queue.update { it + newItems }
        for (item in newItems) {
            scope.launch {
                copySemaphore.withPermit { copyFile(item) }
            }
        }
    }

    private suspend fun resolveTargetDir(): Pair<Uri, DocumentFile> {
        cachedTreeDoc?.let { doc -> cachedTargetUri?.let { uri -> return Pair(uri, doc) } }
        val prefs = preferencesRepository.getPreferences().first()
        val targetDir = prefs.targetDirectory
        if (targetDir.isBlank()) throw Exception("未设置目标目录")
        val uri = Uri.parse(targetDir)
        if (uri.scheme != "content") throw Exception("目标目录无效: $targetDir")
        val doc = DocumentFile.fromTreeUri(context, uri)
            ?: throw Exception("无法写入目标目录: $targetDir")
        if (!doc.canWrite()) throw Exception("无法写入目标目录: $targetDir")
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
                ?: throw Exception("无法创建文件: $fileName")
            val destUri = destFile.uri

            val sourceUri = Uri.parse(item.file.path)
            val src = context.contentResolver.openInputStream(sourceUri)
                ?: throw Exception("无法读取: ${item.file.path}")
            val dst = context.contentResolver.openOutputStream(destUri)
                ?: throw Exception("无法写入: $destUri")

            var bytes = 0L
            BufferedInputStream(src, BUFFER_SIZE).use { input ->
                BufferedOutputStream(dst, BUFFER_SIZE).use { output ->
                    val buf = ByteArray(BUFFER_SIZE)
                    var read: Int
                    while (input.read(buf).also { read = it } != -1) {
                        output.write(buf, 0, read)
                        bytes += read
                    }
                }
            }

            mediaRepository.markAsTransferred(item.file, "")
            _transferredFileIds.update { it + item.file.id }
            _queue.update { list -> list.filter { it.id != item.id } }
        } catch (e: Exception) {
            Log.e(TAG, "Failed: $fileName", e)
            toast("复制失败: $fileName - ${e.message}")
            _queue.update { list ->
                list.map { if (it.id == item.id) it.copy(status = TransferStatus.FAILED) else it }
            }
        }
    }

    override fun getQueue(): Flow<List<TransferItem>> = _queue.asStateFlow()
    override fun getTransferredFileIds(): StateFlow<Set<String>> = _transferredFileIds.asStateFlow()

    override fun removeFromQueue(itemId: String) {
        _queue.update { it.filterNot { item -> item.id == itemId } }
    }

    override fun clearQueue() {
        _queue.update { it.filter { item -> item.status == TransferStatus.TRANSFERRING } }
    }

    override fun cancelTransfer() {
        _queue.update { emptyList() }
    }
}
