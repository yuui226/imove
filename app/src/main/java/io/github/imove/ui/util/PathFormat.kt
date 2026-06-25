package io.github.imove.ui.util

import android.net.Uri

/**
 * Turn a SAF tree URI into a human-readable path, e.g.
 * "content://.../tree/primary%3ADCIM%2FCamera" -> "内部存储/DCIM/Camera".
 *
 * @param internalStorageLabel localized label for the "primary" volume (e.g. "内部存储" / "Internal storage")
 */
fun readableTreePath(sourcePath: String, internalStorageLabel: String): String {
    val treeId = sourcePath.substringAfterLast("/tree/", "")
    val decoded = Uri.decode(treeId) ?: ""
    if (decoded.isBlank()) return internalStorageLabel
    val colon = decoded.indexOf(':')
    val volume = if (colon >= 0) decoded.substring(0, colon) else decoded
    val relative = if (colon >= 0) decoded.substring(colon + 1) else ""
    val volumeLabel = if (volume == "primary") internalStorageLabel else volume
    return if (relative.isBlank()) volumeLabel else "$volumeLabel/$relative"
}
