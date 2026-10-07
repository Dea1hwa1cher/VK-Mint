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

    @get:StringRes abstract val channelNameRes: Int
    @get:StringRes abstract val channelDescRes: Int
    @get:StringRes abstract val completeTitleRes: Int
    @get:StringRes abstract val activeTitleRes: Int
    @get:StringRes abstract val progressTextRes: Int

    override fun onCreate() {
        AppLog.i(logTag, "onCreate()")
        ensureChannel()
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AppLog.i(logTag, "onStartCommand")
        try {
            startForegroundCompat(notificationId, buildNotification(0, 0))
        } catch (e: Exception) {
            AppLog.e(logTag, "startForeground() failed: ${e.javaClass.simpleName}: ${e.message}", e)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        AppLog.i(logTag, "onDestroy")
        super.onDestroy()
    }

    fun updateNotification(activeCount: Int, progressPercent: Int) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        nm.notify(notificationId, buildNotification(activeCount, progressPercent))
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
