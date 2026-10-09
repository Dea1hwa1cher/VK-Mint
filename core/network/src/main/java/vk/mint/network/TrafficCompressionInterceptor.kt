package vk.mint.network

import com.github.luben.zstd.ZstdInputStream
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.GzipSource
import okio.InflaterSource
import okio.Source
import okio.Timeout
import okio.buffer
import okio.source
import vk.mint.util.AppLog
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.Inflater

/**
 * Transparent network compression interceptor for OkHttp.
 *
 * Implements native RFC 8878 Zstandard (zstd), GZIP, and Deflate decompression.
 * When enabled:
 *  1. Injects `Accept-Encoding: zstd, gzip, deflate` into outgoing requests (unless Range or custom Accept-Encoding is present).
 *  2. For compressed responses (`Content-Encoding: zstd`, `gzip`, or `deflate`):
 *     - Transparently decompresses payload streams via native ZSTD JNI or Okio decompressors.
 *     - Accounts wire downloaded bytes vs uncompressed payload bytes via [TrafficStatsTracker].
 *     - Strips `Content-Encoding` and `Content-Length` headers, matching standard HTTP transparent decompression contract.
 */
class TrafficCompressionInterceptor(
    private val isCompressionEnabled: () -> Boolean = { true },
    private val statsTracker: TrafficStatsTracker? = TrafficStatsTracker,
) : Interceptor {

    private companion object {
        const val TAG = "TrafficCompression"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip if disabled, or byte range request (audio/video chunk streaming), or custom encoding specified
        val enabled = try {
            isCompressionEnabled()
        } catch (_: Throwable) {
            true
        }

        val hasRange = originalRequest.header("Range") != null
        val hasCustomAccept = originalRequest.header("Accept-Encoding") != null

        val requestToProceed = if (enabled && !hasRange && !hasCustomAccept) {
            originalRequest.newBuilder()
                .header("Accept-Encoding", "zstd, gzip, deflate")
                .build()
        } else {
            originalRequest
        }

        val response = chain.proceed(requestToProceed)
        val body = response.body

        // Guard: no body or HEAD request
        if (body == null || response.code == 204 || response.code == 304 || originalRequest.method.equals("HEAD", ignoreCase = true)) {
            return response
        }

        val encodingHeader = response.header("Content-Encoding")
        if (encodingHeader.isNullOrBlank() || encodingHeader.equals("identity", ignoreCase = true)) {
            return response
        }

        val encoding = encodingHeader.trim().lowercase(Locale.US)

        val wireCountingSource = CountingSource(body.source())

        val decompressedSource: Source = try {
            when {
                encoding.contains("zstd") -> {
                    // RFC 8878 native Zstandard streaming decompression via zstd-jni
                    val zstdStream = ZstdInputStream(wireCountingSource.buffer().inputStream())
                    zstdStream.source()
                }
                encoding.contains("gzip") -> {
                    GzipSource(wireCountingSource)
                }
                encoding.contains("deflate") -> {
                    InflaterSource(wireCountingSource, Inflater(false))
                }
                else -> {
                    // Unknown content-encoding (e.g. br / lz4), leave untouched
                    return response
                }
            }
        } catch (e: Throwable) {
            AppLog.e(TAG, "Failed to initialize decompressor for encoding '$encodingHeader': ${e.message}", e)
            return response
        }

        var decompCountingSource: CountingSource? = null
        val recorded = AtomicBoolean(false)

        fun notifyStatsRecorded() {
            if (recorded.compareAndSet(false, true)) {
                val comp = wireCountingSource.bytesRead.get()
                val uncomp = decompCountingSource?.bytesRead?.get() ?: 0L
                if (comp > 0L || uncomp > 0L) {
                    statsTracker?.record(comp, uncomp)
                    AppLog.d(TAG, "Decompressed [$encoding] wire: $comp B -> payload: $uncomp B (saved: ${uncomp - comp} B)")
                }
            }
        }

        decompCountingSource = CountingSource(
            delegate = decompressedSource,
            onEofOrClose = { notifyStatsRecorded() },
        )

        val bufferedDecompSource = decompCountingSource.buffer()

        val decompressedBody = object : ResponseBody() {
            override fun contentType(): MediaType? = body.contentType()
            override fun contentLength(): Long = -1L
            override fun source(): BufferedSource = bufferedDecompSource
            override fun close() {
                try {
                    bufferedDecompSource.close()
                } finally {
                    notifyStatsRecorded()
                }
            }
        }

        val strippedHeaders = response.headers.newBuilder()
            .removeAll("Content-Encoding")
            .removeAll("Content-Length")
            .add("X-Decompressed-By", "VK-Mint-Native-Compression")
            .build()

        return response.newBuilder()
            .headers(strippedHeaders)
            .body(decompressedBody)
            .build()
    }

    /**
     * Okio [Source] wrapper that tracks the number of bytes read and triggers [onEofOrClose]
     * when EOF (-1) or [close] is encountered.
     */
    private class CountingSource(
        private val delegate: Source,
        private val onEofOrClose: (() -> Unit)? = null,
    ) : Source {
        val bytesRead = AtomicLong(0L)
        private val callbackInvoked = AtomicBoolean(false)

        override fun read(sink: Buffer, byteCount: Long): Long {
            val count = delegate.read(sink, byteCount)
            if (count > 0L) {
                bytesRead.addAndGet(count)
            } else if (count == -1L) {
                triggerCallback()
            }
            return count
        }

        override fun timeout(): Timeout = delegate.timeout()

        override fun close() {
            try {
                delegate.close()
            } finally {
                triggerCallback()
            }
        }

        private fun triggerCallback() {
            if (callbackInvoked.compareAndSet(false, true)) {
                onEofOrClose?.invoke()
            }
        }
    }
}
