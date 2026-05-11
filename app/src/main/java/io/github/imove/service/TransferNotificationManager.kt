package io.github.imove.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import io.github.imove.MainActivity
import io.github.imove.R

class TransferNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "transfer_channel"
        const val NOTIFICATION_ID_PROGRESS = 1
        const val NOTIFICATION_ID_COMPLETE = 2
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannel()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "文件传输",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "显示文件传输进度"
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun buildProgressNotification(
        completed: Int,
        total: Int,
        queued: Int
    ): Notification {
        val pendingIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("正在传输照片")
            .setContentText("已传输 $completed/$total，队列等待 $queued 个")
            .setProgress(total, completed, false)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    fun showCompletionNotification(success: Int, skipped: Int, failed: Int) {
        val pendingIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("传输完成")
            .setContentText("成功 $success 个，跳过 $skipped 个，失败 $failed 个")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_COMPLETE, notification)
    }

    fun showErrorNotification(reason: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("传输中断")
            .setContentText(reason)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_COMPLETE, notification)
    }

    fun cancelProgressNotification() {
        notificationManager.cancel(NOTIFICATION_ID_PROGRESS)
    }
}
