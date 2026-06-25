package io.github.imove.util

object FileUtils {
    private val VIDEO_EXTENSIONS = setOf("mp4", "mov", "avi", "mkv", "3gp", "webm")
    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif", "dng", "raw", "cr2", "nef", "arw")

    fun isVideo(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in VIDEO_EXTENSIONS
    }

    private fun isImage(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in IMAGE_EXTENSIONS
    }

    fun isMediaFile(fileName: String): Boolean = isVideo(fileName) || isImage(fileName)
}
