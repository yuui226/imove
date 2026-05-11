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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transferredFileDao: TransferredFileDao
) : MediaRepository {

    override fun getFilesFromDevice(device: StorageDevice): Flow<List<MediaFile>> =
        scanDirectory(device.sourcePath, 0, 0)

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
            return@flow
        }
        if (treeDocId == null) {
            Log.e("MediaRepo", "treeDocId is null for $uri")
            return@flow
        }

        val hasDateFilter = dateStart > 0 && dateEnd > 0
        val files = mutableListOf<MediaFile>()
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, treeDocId)

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
                    val mime = cursor.getString(mimeCol) ?: continue

                    // Skip directories — only scan the selected folder
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) continue
                    if (!FileUtils.isMediaFile(name)) continue

                    val modified = cursor.getLong(modifiedCol)
                    if (hasDateFilter && (modified < dateStart || modified > dateEnd)) continue

                    val docUri = DocumentsContract.buildDocumentUriUsingTree(uri, docId)
                    files.add(
                        MediaFile(
                            id = docId,
                            name = name,
                            path = docUri.toString(),
                            size = cursor.getLong(sizeCol),
                            mimeType = mime,
                            dateTaken = modified,
                            dateModified = modified,
                            isVideo = FileUtils.isVideo(name)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MediaRepo", "scanDirectory error", e)
        }

        Log.d("MediaRepo", "Found ${files.size} files")
        emit(files.sortedByDescending { it.dateModified })
    }.flowOn(Dispatchers.IO)
}
