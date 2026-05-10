package io.github.imove.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.imove.data.local.database.dao.TransferredFileDao
import io.github.imove.data.local.database.entity.TransferredFileEntity
import io.github.imove.domain.model.MediaFile
import io.github.imove.domain.model.StorageDevice
import io.github.imove.domain.repository.MediaRepository
import io.github.imove.util.DateUtils
import io.github.imove.util.ExifUtils
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

    override fun getFilesFromDevice(device: StorageDevice): Flow<List<MediaFile>> = flow {
        val files = scanDirectory(device.sourcePath)
        emit(files)
    }.flowOn(Dispatchers.IO)

    override suspend fun getFilesByDateRange(
        device: StorageDevice,
        startDate: Long,
        endDate: Long
    ): List<MediaFile> {
        return scanDirectory(device.sourcePath).filter { file ->
            val date = file.dateTaken.takeIf { it > 0 } ?: file.dateModified
            date in startDate..endDate
        }
    }

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

    private fun scanDirectory(treeUri: String): List<MediaFile> {
        val files = mutableListOf<MediaFile>()
        val uri = Uri.parse(treeUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            uri, DocumentsContract.getTreeDocumentId(uri)
        )

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
                if (!FileUtils.isMediaFile(name)) continue

                val docUri = DocumentsContract.buildDocumentUriUsingTree(uri, docId)
                val size = cursor.getLong(sizeCol)
                val mime = cursor.getString(mimeCol) ?: FileUtils.getMimeType(name)
                val modified = cursor.getLong(modifiedCol)
                val dateTaken = ExifUtils.getDateTaken(context, docUri) ?: modified

                files.add(
                    MediaFile(
                        id = docId,
                        name = name,
                        path = docUri.toString(),
                        size = size,
                        mimeType = mime,
                        dateTaken = dateTaken,
                        dateModified = modified,
                        isVideo = FileUtils.isVideo(name)
                    )
                )
            }
        }

        return files.sortedByDescending { it.dateTaken }
    }
}
