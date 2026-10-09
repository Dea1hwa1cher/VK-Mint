package vk.mint.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/**
 * Потокобезопасный счётчик и трекер статистики сжатия сетевого трафика.
 *
 * Учитывает:
 *  - [compressedBytes] — объём данных, фактически полученных из сети в сжатом виде (ZSTD / GZIP).
 *  - [uncompressedBytes] — объём данных после нативного разжатия.
 *  - [savedBytes] — сэкономленный трафик (uncompressed - compressed).
 *  - [savedPercent] — процент экономии трафика.
 *
 * Поддерживает пакетный сброс (batch persist) для минимизации дисковых операций в DataStore.
 */
object TrafficStatsTracker {

    data class Stats(
        val compressedBytes: Long = 0L,
        val uncompressedBytes: Long = 0L,
    ) {
        val savedBytes: Long
            get() = (uncompressedBytes - compressedBytes).coerceAtLeast(0L)

        val savedPercent: Double
            get() = if (uncompressedBytes > 0L) {
                ((savedBytes.toDouble() * 100.0) / uncompressedBytes.toDouble()).coerceIn(0.0, 100.0)
            } else {
                0.0
            }
    }

    private val _compressedBytes = AtomicLong(0L)
    private val _uncompressedBytes = AtomicLong(0L)

    private val _pendingCompressed = AtomicLong(0L)
    private val _pendingUncompressed = AtomicLong(0L)

    private val _statsFlow = MutableStateFlow(Stats())
    val statsFlow: StateFlow<Stats> = _statsFlow.asStateFlow()

    @Volatile
    var onStatsPersist: ((compressedDelta: Long, uncompressedDelta: Long) -> Unit)? = null

    @Volatile
    var onReset: (() -> Unit)? = null

    /** Порог накопления дельты перед сохранением в DataStore (64 КБ). */
    private const val BATCH_FLUSH_THRESHOLD_BYTES = 64 * 1024L

    fun seed(compressed: Long, uncompressed: Long) {
        _compressedBytes.set(compressed.coerceAtLeast(0L))
        _uncompressedBytes.set(uncompressed.coerceAtLeast(0L))
        _pendingCompressed.set(0L)
        _pendingUncompressed.set(0L)
        _statsFlow.value = Stats(
            compressedBytes = _compressedBytes.get(),
            uncompressedBytes = _uncompressedBytes.get(),
        )
    }

    fun record(compressedDelta: Long, uncompressedDelta: Long) {
        if (compressedDelta <= 0L && uncompressedDelta <= 0L) return

        val comp = _compressedBytes.addAndGet(compressedDelta.coerceAtLeast(0L))
        val uncomp = _uncompressedBytes.addAndGet(uncompressedDelta.coerceAtLeast(0L))

        _statsFlow.value = Stats(
            compressedBytes = comp,
            uncompressedBytes = uncomp,
        )

        val pendingComp = _pendingCompressed.addAndGet(compressedDelta.coerceAtLeast(0L))
        val pendingUncomp = _pendingUncompressed.addAndGet(uncompressedDelta.coerceAtLeast(0L))

        if (pendingComp + pendingUncomp >= BATCH_FLUSH_THRESHOLD_BYTES) {
            flushPending()
        }
    }

    @Synchronized
    fun flushPending() {
        val comp = _pendingCompressed.getAndSet(0L)
        val uncomp = _pendingUncompressed.getAndSet(0L)
        if (comp > 0L || uncomp > 0L) {
            onStatsPersist?.invoke(comp, uncomp)
        }
    }

    fun reset() {
        _compressedBytes.set(0L)
        _uncompressedBytes.set(0L)
        _pendingCompressed.set(0L)
        _pendingUncompressed.set(0L)
        _statsFlow.value = Stats(0L, 0L)
        onReset?.invoke()
    }

    fun formatBytes(bytes: Long): String {
        val b = bytes.coerceAtLeast(0L)
        return when {
            b < 1024L -> "$b Б"
            b < 1024L * 1024L -> String.format(Locale.US, "%.1f КБ", b / 1024.0)
            b < 1024L * 1024L * 1024L -> String.format(Locale.US, "%.2f МБ", b / (1024.0 * 1024.0))
            else -> String.format(Locale.US, "%.2f ГБ", b / (1024.0 * 1024.0 * 1024.0))
        }
    }
}
