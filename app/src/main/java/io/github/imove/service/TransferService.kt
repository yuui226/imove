package io.github.imove.service

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.core.app.ServiceCompat.startForeground
import io.github.imove.data.repository.TransferRepositoryImpl
import io.github.imove.domain.model.TransferItem
import io.github.imove.domain.model.TransferStatus
import io.github.imove.domain.repository.MediaRepository
import io.github.imove.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import javax.inject.Inject

class TransferService : Service() {

    companion object {
        const val ACTION_CANCEL = "io.github.imove.action.CANCEL"
        private const val BUFFER_SIZE = 8192
        private const val PROGRESS_UPDATE_BYTES = 1024 * 1024 // 1MB
    }

    @Inject lateinit var transferRepository: TransferRepositoryImpl
    @Inject lateinit var mediaRepository: MediaRepository
    @Inject lateinit var preferencesRepository: UserPreferencesRepository
    @Inject lateinit var notificationManager: TransferNotificationManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var transferJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            transferRepository.cancelTransfer()
            transferJob?.cancel()
            stopSelf()
            return START_NOT_STICKY
        }

        if (transferJob?.isActive != true) {
            startTransfer()
        }

        return START_NOT_STICKY
    }

    private fun startTransfer() {
        acquireWakeLock()

        transferJob = scope.launch {
            try {
                val queue = transferRepository.getQueue().first()
                val total = queue.size
                var completed = 0
                var success = 0
                var skipped = 0
                var failed = 0

                startForeground(
                    TransferNotificationManager.NOTIFICATION_ID_PROGRESS,
                    notificationManager.buildProgressNotification(0, total, 0)
                )

                for (item in queue) {
                    if (item.status != TransferStatus.QUEUED) continue

                    transferRepository.updateItemStatus(item.id, TransferStatus.TRANSFERRING)

                    try {
                        val result = transferFile(item)
                        when (result) {
                            TransferResult.SUCCESS -> {
                                transferRepository.updateItemStatus(
                                    item.id, TransferStatus.COMPLETED, System.currentTimeMillis()
                                )
                                mediaRepository.markAsTransferred(item.file, "")
                                success++
                            }
                            TransferResult.SKIPPED -> {
                                transferRepository.updateItemStatus(
                                    item.id, TransferStatus.SKIPPED, System.currentTimeMillis()
                                )
                                skipped++
                            }
                        }
                    } catch (e: Exception) {
                        transferRepository.updateItemStatus(
                            item.id, TransferStatus.FAILED, System.currentTimeMillis()
                        )
                        failed++
                    }

                    completed++
                    val remaining = queue.size - completed
                    notificationManager.cancelProgressNotification()
                    startForeground(
                        TransferNotificationManager.NOTIFICATION_ID_PROGRESS,
                        notificationManager.buildProgressNotification(completed, total, remaining)
                    )
                }

                notificationManager.cancelProgressNotification()
                notificationManager.showCompletionNotification(success, skipped, failed)
            } catch (e: Exception) {
                notificationManager.showErrorNotification(e.message ?: "未知错误")
            } finally {
                releaseWakeLock()
                stopSelf()
            }
        }
    }

    private suspend fun transferFile(item: TransferItem): TransferResult {
        val prefs = preferencesRepository.getPreferences().first()
        val targetDir = prefs.targetDirectory
        val targetUri = Uri.parse(targetDir)
        val fileName = item.file.name

        // Check for duplicate
        if (mediaRepository.isFileTransferred(fileName, "")) {
            return TransferResult.SKIPPED
        }

        // Copy file with buffered IO
        val sourceUri = Uri.parse(item.file.path)
        val inputStream = BufferedInputStream(
            contentResolver.openInputStream(sourceUri) ?: throw Exception("Cannot open source"),
            BUFFER_SIZE
        )

        val destUri = Uri.withAppendedPath(targetUri, fileName)
        val outputStream = BufferedOutputStream(
            contentResolver.openOutputStream(destUri) ?: throw Exception("Cannot open destination"),
            BUFFER_SIZE
        )

        inputStream.use { input ->
            outputStream.use { output ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                var totalBytes = 0L
                var lastProgressBytes = 0L

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalBytes += bytesRead

                    if (totalBytes - lastProgressBytes >= PROGRESS_UPDATE_BYTES) {
                        lastProgressBytes = totalBytes
                    }
                }
            }
        }

        return TransferResult.SUCCESS
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "iMove::Transfer").apply {
            acquire(60 * 60 * 1000L) // 1 hour max
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    override fun onDestroy() {
        scope.cancel()
        releaseWakeLock()
        super.onDestroy()
    }
}

private enum class TransferResult {
    SUCCESS, SKIPPED
}
