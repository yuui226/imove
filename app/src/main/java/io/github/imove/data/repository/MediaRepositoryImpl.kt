package io.github.imove.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.imove.domain.model.MediaFile
import io.github.imove.domain.model.StorageDevice
import io.github.imove.domain.repository.MediaRepository
import io.github.imove.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : MediaRepository {

    override fun getFilesFromDevice(device: StorageDevice): Flow<List<MediaFile>> =
        scanDirectory(device.sourcePath)

    private fun scanDirectory(treeUri: String): Flow<List<MediaFile>> = flow {
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

        val files = mutableListOf<MediaFile>()
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, treeDocId)

        try {
            context.contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED
                ),
                null, null, null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val modifiedCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                while (cursor.moveToNext()) {
                    val docId = cursor.getString(idCol)
                    val name = cursor.getString(nameCol) ?: continue
                    val mime = cursor.getString(mimeCol) ?: continue

                    // Skip directories — only scan the selected folder
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) continue
                    if (!FileUtils.isMediaFile(name)) continue

                    val docUri = DocumentsContract.buildDocumentUriUsingTree(uri, docId)
                    files.add(
                        MediaFile(
                            id = docId,
                            name = name,
                            path = docUri.toString(),
                            mimeType = mime,
                            dateModified = cursor.getLong(modifiedCol),
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
