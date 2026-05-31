package io.github.imove.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    private val dayKeyFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

    fun getDayKey(timestamp: Long): String = dayKeyFormat.format(Date(timestamp))

    private val headerFormat = SimpleDateFormat("yyyy-M-d", Locale.US)

    fun formatDateHeader(timestamp: Long): String = headerFormat.format(Date(timestamp))
}
