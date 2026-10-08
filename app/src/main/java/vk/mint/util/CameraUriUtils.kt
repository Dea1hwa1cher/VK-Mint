package vk.mint.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Вспомогательные функции для создания временных URI для снимков камеры через [FileProvider].
 */
object CameraUriUtils {

    /**
     * Создаёт временный URI для сохранения фото с камеры через [FileProvider].
     * Возвращает null, если FileProvider не настроен или создание файла завершилось ошибкой.
     */
    fun createCameraImageUri(ctx: Context): Uri? {
        return try {
            val photoFile = File(ctx.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
            FileProvider.getUriForFile(
                ctx,
                "${ctx.packageName}.fileprovider",
                photoFile,
            )
        } catch (e: Exception) {
            AppLog.e("CameraUriUtils", "createCameraImageUri failed", e)
            null
        }
    }
}

/**
 * Top-level шорткат для удобного импорта без изменения call sites.
 */
fun createCameraImageUri(ctx: Context): Uri? = CameraUriUtils.createCameraImageUri(ctx)
