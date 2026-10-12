package vk.mint.media

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import vk.mint.data.model.Attachment
import vk.mint.data.model.Message
import vk.mint.util.AppLog

/**
 * Скачивание голосовых сообщений VK как файл .ogg (Opus) в системную папку Downloads/VK/.
 *
 * VK отдаёт голосовые сообщения в формате OGG (Opus) через поле [Attachment.Doc.AudioMsg.linkOgg].
 * Файл сохраняется через системный [DownloadManager] с MIME-типом audio/ogg и
 * системным уведомлением о завершении скачивания.
 */
object VoiceDownloadHelper {

    private const val TAG = "VoiceDownloadHelper"

    /**
     * Построить имя файла и URL для скачивания голосового сообщения.
     * Приоритет формата: .ogg (оригинальный формат VK voice), fallback .mp3 если link_ogg отсутствует.
     */
    fun buildFileInfo(audioMsg: Attachment.Doc.AudioMsg, messageId: Long = 0L): Pair<String, String> {
        val oggUrl = audioMsg.linkOgg?.takeIf { it.isNotBlank() }
        val mp3Url = audioMsg.linkMp3?.takeIf { it.isNotBlank() }
        val downloadUrl = oggUrl ?: mp3Url ?: ""

        val isMp3Only = oggUrl == null && (mp3Url?.contains(".mp3") == true || downloadUrl.contains(".mp3"))
        val ext = if (isMp3Only) "mp3" else "ogg"

        val idPart = when {
            audioMsg.ownerId != 0L && audioMsg.id != 0L -> "${audioMsg.ownerId}_${audioMsg.id}"
            messageId != 0L -> "$messageId"
            else -> "${System.currentTimeMillis()}"
        }
        val fileName = "voice_$idPart.$ext"
        return Pair(fileName, downloadUrl)
    }

    /**
     * Скачать голосовое сообщение через системный [DownloadManager].
     * @return true, если задача успешно передана в очередь загрузчика.
     */
    fun download(context: Context, audioMsg: Attachment.Doc.AudioMsg, messageId: Long = 0L): Boolean {
        val (fileName, url) = buildFileInfo(audioMsg, messageId)
        if (url.isBlank()) {
            Toast.makeText(context, "Ссылка на голосовое сообщение недоступна", Toast.LENGTH_SHORT).show()
            return false
        }
        return try {
            val mimeType = if (fileName.endsWith(".ogg", ignoreCase = true)) "audio/ogg" else "audio/mpeg"
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(fileName)
                .setDescription("Голосовое сообщение VK")
                .setMimeType(mimeType)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "VK/$fileName")
                .setAllowedOverMetered(true)

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (dm == null) {
                Toast.makeText(context, "Загрузка недоступна на этом устройстве", Toast.LENGTH_SHORT).show()
                return false
            }
            dm.enqueue(request)
            Toast.makeText(context, "Скачивание началось: $fileName", Toast.LENGTH_SHORT).show()
            AppLog.i(TAG, "Voice download enqueued: $fileName -> $url")
            true
        } catch (e: Exception) {
            AppLog.e(TAG, "Voice download failed", e)
            Toast.makeText(context, "Не удалось скачать: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Скачать голосовое сообщение из [Attachment.Doc].
     */
    fun download(context: Context, doc: Attachment.Doc, messageId: Long = 0L): Boolean {
        val am = doc.audioMsg ?: return false
        return download(context, am, messageId)
    }

    /**
     * Скачать голосовое сообщение из [Attachment].
     */
    fun download(context: Context, attachment: Attachment, messageId: Long = 0L): Boolean {
        val am = attachment.audioMessage ?: attachment.doc?.audioMsg ?: return false
        return download(context, am, messageId)
    }

    /**
     * Скачать голосовое сообщение из сообщения чата [Message].
     */
    fun downloadFromMessage(context: Context, message: Message): Boolean {
        val am = message.attachments?.firstOrNull {
            (it.type == "audio_message" && it.audioMessage != null) ||
            (it.type == "doc" && it.doc?.isVoiceMessage == true)
        }?.let { it.audioMessage ?: it.doc?.audioMsg }

        if (am == null) {
            Toast.makeText(context, "Голосовое сообщение не найдено", Toast.LENGTH_SHORT).show()
            return false
        }
        return download(context, am, message.id)
    }
}
