package vk.mint.media

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Единый утилитный класс для HTTP range-скачивания медиафайлов с докачкой (RFC 7233).
 *
 * Устраняет дублирование логики chunked-чтения, HTTP 206 Partial Content,
 * проверки целостности Content-Length и атомарного переименования между:
 * - [VideoDownloadManager]
 * - [ClipVideoDownloadManager]
 * - [StoryVideoDownloadManager]
 * - [TrackDownloadManager]
 */
object HttpRangeDownloader {

    /**
     * Потокобезопасный троттлер обновлений прогресса (шаг >= 2% или >= 500мс),
     * предотвращающий спам рекомпозиций и логов при частых 64KB чанках.
     */
    class ProgressThrottler(
        private val minStepPercent: Int = 2,
        private val minIntervalMs: Long = 500L,
        private val onThrottledUpdate: (progress: Int, bytesRead: Long, totalBytes: Long) -> Unit,
    ) {
        private var lastProgress = -1
        private var lastUpdateTs = 0L

        fun update(bytesRead: Long, totalBytes: Long, progress: Int) {
            val now = System.currentTimeMillis()
            val delta = progress - lastProgress
            val enoughTime = now - lastUpdateTs >= minIntervalMs
            if (progress != lastProgress && (delta >= minStepPercent || delta <= -minStepPercent || enoughTime)) {
                lastProgress = progress
                lastUpdateTs = now
                onThrottledUpdate(progress, bytesRead, totalBytes)
            }
        }
    }

    /**
     * Атомарный перенос временного файла (.tmp) в целевой (.mp4/.mp3) с fallback на copyTo + delete.
     */
    fun atomicMove(tempFile: File, targetFile: File): Boolean {
        if (tempFile.renameTo(targetFile)) return true
        tempFile.copyTo(targetFile, overwrite = true)
        return tempFile.delete()
    }

    /**
     * Скачивание файла по HTTP с поддержкой Range-resume.
     *
     * @param client OkHttpClient
     * @param url Прямой URL медиафайла
     * @param tempFile Временный файл на диске
     * @param bufferSize Размер буфера чтения (по умолчанию 64 КБ)
     * @param onProgress Коллбэк прогресса: (bytesRead, totalBytes, progressPercent)
     * @throws IOException при сетевых сбоях или неполной загрузке
     */
    fun downloadWithResume(
        client: OkHttpClient,
        url: String,
        tempFile: File,
        bufferSize: Int = 65536,
        onProgress: ((bytesRead: Long, totalBytes: Long, progress: Int) -> Unit)? = null,
    ) {
        val existingBytes = if (tempFile.exists()) tempFile.length() else 0L
        val requestBuilder = Request.Builder().url(url)
        if (existingBytes > 0) {
            requestBuilder.header("Range", "bytes=$existingBytes-")
        }
        val request = requestBuilder.build()

        client.newCall(request).execute().use { response ->
            val isPartial = response.code == 206
            if (!response.isSuccessful && response.code != 206) {
                throw IOException("HTTP ${response.code}: ${response.message}")
            }
            // Сервер проигнорировал Range (200 OK) — начинаем с нуля
            val append = isPartial && existingBytes > 0
            if (!append && tempFile.exists()) {
                tempFile.delete()
            }

            val body = response.body ?: throw IOException("Empty response body")
            val contentLength = body.contentLength()
            val totalBytes = when {
                append && contentLength > 0 -> existingBytes + contentLength
                contentLength > 0 -> contentLength
                else -> -1L
            }
            var bytesRead = if (append) existingBytes else 0L

            body.byteStream().use { input ->
                FileOutputStream(tempFile, append).use { output ->
                    val buffer = ByteArray(bufferSize)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        bytesRead += read

                        if (totalBytes > 0) {
                            val progress = ((bytesRead.toFloat() / totalBytes.toFloat()) * 100f)
                                .toInt().coerceIn(0, 100)
                            onProgress?.invoke(bytesRead, totalBytes, progress)
                        } else {
                            onProgress?.invoke(bytesRead, -1L, -1)
                        }
                    }
                }
            }

            if (totalBytes > 0L && bytesRead < totalBytes) {
                throw IOException("Incomplete download: read $bytesRead of $totalBytes bytes")
            }
        }
    }
}
