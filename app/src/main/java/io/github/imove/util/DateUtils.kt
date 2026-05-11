package io.github.imove.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val displayFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val dayKeyFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

    fun formatTimestamp(timestamp: Long): String = displayFormat.format(Date(timestamp))

    fun getDayKey(timestamp: Long): String = dayKeyFormat.format(Date(timestamp))

    private val headerFormat = SimpleDateFormat("yyyy-M-d", Locale.US)

    fun formatDateHeader(timestamp: Long): String = headerFormat.format(Date(timestamp))

    fun getTodayStart(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getThreeDaysAgoStart(): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -3)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun isToday(timestamp: Long): Boolean = timestamp >= getTodayStart()

    fun isWithinLastThreeDays(timestamp: Long): Boolean = timestamp >= getThreeDaysAgoStart()
}
