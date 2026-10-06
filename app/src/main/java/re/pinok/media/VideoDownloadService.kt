// File: media/VideoDownloadService.kt
package re.pinok.media

import android.content.Context
import android.content.Intent
import android.os.Build
import re.pinok.R

/**
 * VideoDownloadService — foreground-сервис для уведомлений о загрузке видео.
 *
 * Показывает уведомление пока [VideoDownloadManager] имеет активные загрузки.
 */
class VideoDownloadService : BaseMediaDownloadService() {

    override val logTag: String get() = TAG
    override val notificationId: Int get() = NOTIFICATION_ID
    override val channelId: String get() = NOTIFICATION_CHANNEL_ID

    override val channelNameRes: Int get() = R.string.video_download_channel_name
    override val channelDescRes: Int get() = R.string.video_download_channel_description
    override val completeTitleRes: Int get() = R.string.video_download_notification_complete_title
    override val activeTitleRes: Int get() = R.string.video_download_notification_active_title
    override val progressTextRes: Int get() = R.string.video_download_notification_text

    companion object {
        private const val TAG = "VideoDownloadService"
        // Fix #233 (P1-9): ранее 2001 — коллизия с MusicDownloadService (тоже 2001).
        // Уведомления перетирались: запуск Music-загрузки убивал Video-нотификацию
        // и наоборот. Теперь Music=2001, Story=2002, Video=2003, Clip=2004 — все уникальны.
        private const val NOTIFICATION_ID = 2003
        private const val NOTIFICATION_CHANNEL_ID = "video_downloads"

        fun start(context: Context) {
            val intent = Intent(context, VideoDownloadService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, VideoDownloadService::class.java))
        }
    }
}