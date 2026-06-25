package io.github.imove.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {
    // DateTimeFormatter is immutable and thread-safe, unlike SimpleDateFormat —
    // these are called concurrently from the IO filtering flow and the Compose main thread.
    private val dayKeyFormat = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.getDefault())
    private val headerFormat = DateTimeFormatter.ofPattern("yyyy-M-d", Locale.US)

    fun getDayKey(timestamp: Long): String =
        dayKeyFormat.format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()))

    fun formatDateHeader(timestamp: Long): String =
        headerFormat.format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()))
}
