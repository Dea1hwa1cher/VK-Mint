// File: media/BaseMediaDownloadService.kt
package vk.mint.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import vk.mint.data.model.DownloadState
import vk.mint.data.model.DownloadStatus
import vk.mint.ui.MainActivity
import vk.mint.util.AppLog

/**
 * Базовый foreground-сервис для уведомлений о загрузках медиа (видео, истории, клипы).
 *
 * Предоставляет общую логику:
 * - Создание NotificationChannel
 * - Корректный запуск startForegroundCompat с dataSync типом на Android 14+
 * - Отображение прогресса и завершения загрузки
 */
abstract class BaseMediaDownloadService : Service() {

    abstract val logTag: String
    abstract val notificationId: Int
    abstract val channelId: String
    abstract val downloadsFlow: kotlinx.coroutines.flow.StateFlow<Map<String, DownloadState>>

    @get:StringRes abstract val channelNameRes: Int
    @get:StringRes abstract val channelDescRes: Int
    @get:StringRes abstract val completeTitleRes: Int
    @get:StringRes abstract val activeTitleRes: Int
    @get:StringRes abstract val progressTextRes: Int

    private val serviceScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main.immediate
    )
    private var collectorJob: kotlinx.coroutines.Job? = null

    override fun onCreate() {
        AppLog.i(logTag, "onCreate()")
        ensureChannel()
        super.onCreate()
        startObservingDownloads()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AppLog.i(logTag, "onStartCommand")
        try {
            val active = downloadsFlow.value.values.filter {
                it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED
            }
            val initialCount = active.size.coerceAtLeast(1)
            val initialProgress = if (active.isNotEmpty()) {
                active.sumOf { it.progress } / active.size
            } else {
                0
            }
            startForegroundCompat(notificationId, buildNotification(initialCount, initialProgress))
        } catch (e: Exception) {
            AppLog.e(logTag, "startForeground() failed: ${e.javaClass.simpleName}: ${e.message}", e)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startObservingDownloads() {
        if (collectorJob != null) return
        collectorJob = serviceScope.launch {
            downloadsFlow.collect { map ->
                val active = map.values.filter {
                    it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED
                }
                if (active.isNotEmpty()) {
                    val avgProgress = active.sumOf { it.progress } / active.size
                    updateNotification(active.size, avgProgress)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        AppLog.i(logTag, "onDestroy")
        collectorJob?.cancel()
        collectorJob = null
        serviceScope.cancel()
        try {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(notificationId)
        } catch (e: Exception) {
            AppLog.w(logTag, "onDestroy cleanup failed: ${e.message}")
        }
        super.onDestroy()
    }

    fun updateNotification(activeCount: Int, progressPercent: Int) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        try {
            nm.notify(notificationId, buildNotification(activeCount, progressPercent))
        } catch (e: Exception) {
            AppLog.w(logTag, "updateNotification failed: ${e.message}")
        }
    }

    protected open fun buildNotification(activeCount: Int, progressPercent: Int): Notification {
        val title = if (activeCount == 0) {
            getString(completeTitleRes)
        } else {
            getString(activeTitleRes, activeCount)
        }

        val contentIntent = Intent(this, MainActivity::class.java).let { intent ->
            PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(getString(progressTextRes, progressPercent))
            .setProgress(100, progressPercent, progressPercent == 0 && activeCount > 0)
            .setOngoing(activeCount > 0)
            .setContentIntent(contentIntent)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (nm.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(
                    channelId,
                    getString(channelNameRes),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = getString(channelDescRes)
                    setShowBadge(false)
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    private fun startForegroundCompat(id: Int, notification: Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else 0
        ServiceCompat.startForeground(this, id, notification, type)
    }
}
