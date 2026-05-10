package io.github.imove.util

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream

object ExifUtils {
    fun getDateTaken(context: Context, uri: Uri): Long? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                getDateTakenFromStream(stream)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getDateTakenFromStream(stream: InputStream): Long? {
        return try {
            val exif = ExifInterface(stream)
            exif.dateTimeOriginal
        } catch (e: Exception) {
            null
        }
    }
}
