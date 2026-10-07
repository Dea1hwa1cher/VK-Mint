// File: media/StoryVideoDownloadService.kt
package vk.mint.media

import android.content.Context
import android.content.Intent
import android.os.Build
import vk.mint.R

/**
 * StoryVideoDownloadService — foreground-сервис для уведомлений о загрузке
 * ВИДЕО-историй (stories).
 *
 * Наследует общую логику из [BaseMediaDownloadService]:
 *  - отдельный NOTIFICATION_ID (2002 — не конфликтует с 2001 video / 2003 music / 2004 clip)
 *  - отдельный канал `story_video_downloads`
 *  - отдельные строковые ресурсы
 */
class StoryVideoDownloadService : BaseMediaDownloadService() {

    override val logTag: String get() = TAG
    override val notificationId: Int get() = NOTIFICATION_ID
    override val channelId: String get() = NOTIFICATION_CHANNEL_ID

    override val channelNameRes: Int get() = R.string.story_video_download_channel_name
    override val channelDescRes: Int get() = R.string.story_video_download_channel_description
    override val completeTitleRes: Int get() = R.string.story_video_download_notification_complete_title
    override val activeTitleRes: Int get() = R.string.story_video_download_notification_active_title
    override val progressTextRes: Int get() = R.string.story_video_download_notification_text

    companion object {
        private const val TAG = "StoryVideoDlService"
        private const val NOTIFICATION_ID = 2002
        private const val NOTIFICATION_CHANNEL_ID = "story_video_downloads"

        fun start(context: Context) {
            val intent = Intent(context, StoryVideoDownloadService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, StoryVideoDownloadService::class.java))
        }
    }
}
