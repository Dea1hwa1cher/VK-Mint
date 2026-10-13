package vk.mint.util

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Общие утилиты форматирования.
 * Вынесены из дублирующихся private-функций в FeedScreen, MusicScreen,
 * ProfileScreen, CommunityScreen, NotificationsScreen, ChatDetailScreen, etc.
 *
 * Аудит #90: объединено 5× formatDuration, 5× formatTime, 3× formatCount,
 * 2× formatMsgTime, 1× formatRecordingTime, 2× formatDurationMs.
 */

// ─── Длительность ─────────────────────────────────────────────

/** Форматирование длительности трека/видео: 195 → "3:15", 3665 → "1:01:05" */
fun Int.toDurationString(): String {
    val nonNeg = this.coerceAtLeast(0)
    val h = nonNeg / 3600
    val m = (nonNeg % 3600) / 60
    val s = nonNeg % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** Форматирование длительности из миллисекунд: 195000 → "3:15" */
fun Long.toDurationString(): String {
    val totalSec = (this / 1000).coerceAtLeast(0).toInt()
    return totalSec.toDurationString()
}

/** Короткое относительное время (timestamp в секундах): "только что", "5 мин назад", "2 ч назад", "3 д назад" */
fun Long.toRelativeShortTime(): String {
    if (this == 0L) return ""
    val now = System.currentTimeMillis() / 1000
    val diff = now - this
    return when {
        diff < 60 -> "только что"
        diff < 3600 -> "${diff / 60} мин назад"
        diff < 86400 -> "${diff / 3600} ч назад"
        else -> "${diff / 86400} д назад"
    }
}

/** Короткое относительное время для скачанного офлайн-контента (timestamp в миллисекундах) */
fun Long.toOfflineDownloadedAtString(): String = (this / 1000).toRelativeShortTime()

/** Форматирование времени записи голосового: 90 → "1:30", 45 → "0:45" */
fun Int.toRecordingTimeString(): String {
    val m = this / 60
    val s = this % 60
    return if (m > 0) "%d:%02d".format(m, s) else "0:%02d".format(s)
}

// ─── Время ────────────────────────────────────────────────────

private val RU_LOCALE = Locale.forLanguageTag("ru")

/** Относительное время: "только что", "5 мин назад", "вчера", "12 июн 2025" */
fun Long.toRelativeTime(): String {
    val diff = System.currentTimeMillis() / 1000 - this
    return when {
        diff < 60 -> "только что"
        diff < 3600 -> "${diff / 60} мин назад"
        diff < 86400 -> "${diff / 3600} ч назад"
        diff < 172800 -> "вчера"
        else -> {
            val sdf = SimpleDateFormat("d MMM yyyy", RU_LOCALE)
            sdf.format(Date(this * 1000))
        }
    }
}

/** Абсолютное время: "12 июн 2025, 14:30" */
fun Long.toAbsoluteTime(): String {
    return try {
        val sdf = SimpleDateFormat("d MMM yyyy, HH:mm", RU_LOCALE)
        sdf.format(Date(this * 1000))
    } catch (_: Exception) {
        ""
    }
}

/** Время сообщения: "14:30" */
fun Long.toMsgTime(): String {
    val sdf = SimpleDateFormat("HH:mm", RU_LOCALE)
    return sdf.format(Date(this * 1000))
}

/**
 * P1.1: Дата для date-separator'а в чате: «Сегодня», «Вчера», «12 июля»,
 * «12 июля 2024» (для старых лет).
 *
 * [timestampSec] — Unix timestamp в секундах (как VK API возвращает message.date).
 * Сравнение с «сегодня/вчера» по локальному календарю.
 */
fun Long.toChatDate(): String {
    val nowMs = System.currentTimeMillis()
    val tsMs = this * 1000L
    val calNow = java.util.Calendar.getInstance().apply { timeInMillis = nowMs }
    val calTs = java.util.Calendar.getInstance().apply { timeInMillis = tsMs }
    val dayNow = calNow.get(java.util.Calendar.DAY_OF_YEAR)
    val dayTs = calTs.get(java.util.Calendar.DAY_OF_YEAR)
    val yearNow = calNow.get(java.util.Calendar.YEAR)
    val yearTs = calTs.get(java.util.Calendar.YEAR)
    return when {
        yearNow == yearTs && dayNow == dayTs -> "Сегодня"
        yearNow == yearTs && dayNow - dayTs == 1 -> "Вчера"
        yearNow == yearTs -> SimpleDateFormat("d MMMM", RU_LOCALE).format(Date(tsMs))
        else -> SimpleDateFormat("d MMMM yyyy", RU_LOCALE).format(Date(tsMs))
    }
}

/**
 * P1.1: Возвращает «день» (год + dayOfYear) как стабильный ключ для группировки
 * сообщений по дате. Два сообщения с одним [dayKey] belong to one date-separator group.
 */
fun Long.toDayKey(): Int {
    // Audit #S5: `this` внутри apply{} ссылается на Calendar, а не на Long-приёмник
    // (shadowing) → `this * 1000L` = Calendar * Long = «None of the following candidates
    // is applicable». Выносим умножение наружу, в scope extension-функции.
    val tsMs = this * 1000L
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = tsMs }
    val year = cal.get(java.util.Calendar.YEAR)
    val day = cal.get(java.util.Calendar.DAY_OF_YEAR)
    return year * 1000 + day
}

// ─── Даты ──────────────────────────────────────────────────────

/** Форматирование даты: 1720000000L -> "12 июл 2024" (epoch секунды) */
fun Long.toDateString(): String {
    if (this == 0L) return ""
    return SimpleDateFormat("d MMM yyyy", RU_LOCALE).format(Date(this * 1000))
}

/** Форматирование длительности из секунд (Long): 195L → "3:15" */
fun Long.toDurationSecondsString(): String = this.toInt().toDurationString()

// ─── Счётчики ─────────────────────────────────────────────────

/** Компактная метрика со срезанием .0: целые без хвоста ("80K"), дробные ("1.2K") */
fun compactMetricValue(value: Double, suffix: String): String {
    val s = String.format(Locale.US, "%.1f", value)
    val trimmed = if (s.endsWith(".0")) s.dropLast(2) else s
    return trimmed + suffix
}

/** Форматирование счётчика: 999 → "999", 1500 → "1.5K", 2300000 → "2.3M", целые → "80K", "2M" */
fun Int.toCountString(): String = when {
    this >= 1_000_000 -> compactMetricValue(this / 1_000_000.0, "M")
    this >= 1_000 -> compactMetricValue(this / 1_000.0, "K")
    else -> this.toString()
}

/** Форматирование счётчика в стиле ВКонтакте с опциональной запятой: 1200 -> "1,2K" */
fun Int.toCompactMetricString(useComma: Boolean = false): String {
    val s = toCountString()
    return if (useComma) s.replace('.', ',') else s
}

/** Форматирование количества участников/подписчиков сообщества ("1,2 млн", "345 тыс", "42") */
fun Int.toMemberCountString(): String = when {
    this >= 1_000_000 -> {
        val v = this / 1_000_000.0
        val formatted = if (v == v.toLong().toDouble()) v.toLong().toString() else String.format(Locale.US, "%.1f", v).replace('.', ',')
        "$formatted млн"
    }
    this >= 1_000 -> {
        val v = this / 1_000.0
        val formatted = if (v == v.toLong().toDouble()) v.toLong().toString() else String.format(Locale.US, "%.1f", v).replace('.', ',')
        "$formatted тыс"
    }
    else -> this.toString()
}

// ─── Размеры файлов ──────────────────────────────────────────

/** Форматирование размера файла: 1024 -> "1 КБ", 1048576 -> "1 МБ", 1073741824 -> "1 ГБ" */
fun Long.toFileSizeString(): String {
    if (this < 1024) return "$this Б"
    if (this < 1024 * 1024) return DecimalFormat("#.#").format(this / 1024.0) + " КБ"
    if (this < 1024L * 1024 * 1024) return DecimalFormat("#.#").format(this / (1024.0 * 1024.0)) + " МБ"
    return DecimalFormat("#.#").format(this / (1024.0 * 1024.0 * 1024.0)) + " ГБ"
}