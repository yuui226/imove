package io.github.imove.data.usb

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageAccessManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun createOpenDocumentTreeIntent(): Intent {
        val sm = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val volumes = sm.storageVolumes.filter { it.isRemovable }

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        }

        // Try to set initial URI to first removable volume
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            volumes.firstOrNull()?.directory?.let { dir ->
                val initialUri = Uri.parse("content://com.android.externalstorage.documents/tree/${dir.name}%3A")
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
            }
        }

        return intent
    }

    fun takePersistablePermission(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }

}
