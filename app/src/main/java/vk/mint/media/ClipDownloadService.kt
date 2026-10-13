// File: media/ClipDownloadService.kt
package vk.mint.media

import android.content.Context
import android.content.Intent
import android.os.Build
import vk.mint.R

/**
 * ClipDownloadService — foreground-сервис для уведомлений о загрузке VK Clips
 * (коротких вертикальных видео).
 *
 * Наследует общую логику из [BaseMediaDownloadService]:
 *  - отдельный NOTIFICATION_ID (2004 — не конфликтует с 2001 video / 2002 story / 2003 music)
 *  - отдельный канал `clip_downloads`
 *  - отдельные строковые ресурсы (`clip_video_download_*`)
 */
class ClipDownloadService : BaseMediaDownloadService() {

    override val logTag: String get() = TAG
    override val notificationId: Int get() = NOTIFICATION_ID
    override val channelId: String get() = NOTIFICATION_CHANNEL_ID
    override val downloadsFlow get() = ClipVideoDownloadManager.downloads

    override val channelNameRes: Int get() = R.string.clip_video_download_channel_name
    override val channelDescRes: Int get() = R.string.clip_video_download_channel_description
    override val completeTitleRes: Int get() = R.string.clip_video_download_notification_complete_title
    override val activeTitleRes: Int get() = R.string.clip_video_download_notification_active_title
    override val progressTextRes: Int get() = R.string.clip_video_download_notification_text

    companion object {
        private const val TAG = "ClipDownloadService"
        private const val NOTIFICATION_ID = 2004
        private const val NOTIFICATION_CHANNEL_ID = "clip_downloads"

        fun start(context: Context) {
            val intent = Intent(context, ClipDownloadService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ClipDownloadService::class.java))
        }
    }
}
