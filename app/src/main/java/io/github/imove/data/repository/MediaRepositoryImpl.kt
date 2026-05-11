package io.github.imove.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.imove.data.local.database.dao.TransferredFileDao
import io.github.imove.data.local.database.entity.TransferredFileEntity
import io.github.imove.domain.model.MediaFile
import io.github.imove.domain.model.StorageDevice
import io.github.imove.domain.repository.MediaRepository
import io.github.imove.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transferredFileDao: TransferredFileDao
) : MediaRepository {

    companion object {
        private const val PARALLELISM = 4
    }

    override fun getFilesFromDevice(device: StorageDevice): Flow<List<MediaFile>> =
        scanDirectory(device.sourcePath, 0, 0)

    override fun getFilesByDateRange(
        device: StorageDevice,
        startDate: Long,
        endDate: Long
    ): Flow<List<MediaFile>> =
        scanDirectory(device.sourcePath, startDate, endDate)

    override suspend fun isFileTransferred(fileName: String, deviceId: String): Boolean {
        return transferredFileDao.exists(fileName, deviceId)
    }

    override suspend fun markAsTransferred(file: MediaFile, deviceId: String) {
        transferredFileDao.insert(
            TransferredFileEntity(
                id = UUID.randomUUID().toString(),
                fileName = file.name,
                sourceDeviceId = deviceId,
                transferredAt = System.currentTimeMillis(),
                destinationPath = file.path
            )
        )
    }

    private fun scanDirectory(
        treeUri: String,
        dateStart: Long,
        dateEnd: Long
    ): Flow<List<MediaFile>> = flow {
        val uri = Uri.parse(treeUri)
        val treeDocId = try {
            DocumentsContract.getTreeDocumentId(uri)
        } catch (e: Exception) {
            Log.e("MediaRepo", "Failed to get tree document ID: $uri", e)
            emit(emptyList())
            return@flow
        }

        if (treeDocId == null) {
            Log.e("MediaRepo", "treeDocId is null for $uri")
            emit(emptyList())
            return@flow
        }

        val entries = collectFileEntries(uri, treeDocId, dateStart, dateEnd)
        Log.d("MediaRepo", "Found ${entries.size} files")

        val sorted = entries.sortedByDescending { it.dateModified }
        emit(sorted)
    }.flowOn(Dispatchers.IO)

    /**
     * Collect files from a directory. Subdirectories are scanned in parallel
     * (up to PARALLELISM concurrent scans).
     */
    private suspend fun collectFileEntries(
        treeUri: Uri,
        treeDocId: String,
        dateStart: Long,
        dateEnd: Long
    ): List<MediaFile> = coroutineScope {
        val files = mutableListOf<MediaFile>()
        val subDirs = mutableListOf<String>()
        val hasDateFilter = dateStart > 0 && dateEnd > 0
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocId)

        // Phase 1: scan current directory cursor, collect files + subdirectory IDs
        try {
            context.contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_SIZE,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED
                ),
                null, null, null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val modifiedCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                while (cursor.moveToNext()) {
                    val docId = cursor.getString(idCol)
                    val name = cursor.getString(nameCol) ?: continue
                    val mime = cursor.getString(mimeCol)

                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        subDirs.add(docId)
                        continue
                    }

                    if (!FileUtils.isMediaFile(name)) continue

                    val modified = cursor.getLong(modifiedCol)

                    if (hasDateFilter && (modified < dateStart || modified > dateEnd)) {
                        continue
                    }

                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    files.add(
                        MediaFile(
                            id = docId,
                            name = name,
                            path = docUri.toString(),
                            size = cursor.getLong(sizeCol),
                            mimeType = mime ?: FileUtils.getMimeType(name),
                            dateTaken = modified,
                            dateModified = modified,
                            isVideo = FileUtils.isVideo(name)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MediaRepo", "collectFileEntries error", e)
        }

        // Phase 2: scan subdirectories in parallel (up to PARALLELISM concurrent)
        if (subDirs.isNotEmpty()) {
            val semaphore = Semaphore(PARALLELISM)
            val subResults = subDirs.map { dirDocId ->
                async {
                    semaphore.withPermit {
                        collectFileEntries(treeUri, dirDocId, dateStart, dateEnd)
                    }
                }
            }.awaitAll()

            subResults.forEach { files.addAll(it) }
        }

        files
    }
}
