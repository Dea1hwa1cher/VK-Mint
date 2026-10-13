package vk.mint.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicLong

class TrafficStatsTrackerTest {

    @Before
    fun setUp() {
        TrafficStatsTracker.reset()
        TrafficStatsTracker.onStatsPersist = null
    }

    @Test
    fun testInitialState() {
        val stats = TrafficStatsTracker.statsFlow.value
        assertEquals(0L, stats.compressedBytes)
        assertEquals(0L, stats.uncompressedBytes)
        assertEquals(0L, stats.savedBytes)
        assertEquals(0.0, stats.savedPercent, 0.001)
    }

    @Test
    fun testRecordAndCalculations() {
        TrafficStatsTracker.record(compressedDelta = 300L, uncompressedDelta = 1000L)
        val stats = TrafficStatsTracker.statsFlow.value

        assertEquals(300L, stats.compressedBytes)
        assertEquals(1000L, stats.uncompressedBytes)
        assertEquals(700L, stats.savedBytes)
        assertEquals(70.0, stats.savedPercent, 0.001)
    }

    @Test
    fun testBatchFlushCallback() {
        val persistedComp = AtomicLong(0L)
        val persistedUncomp = AtomicLong(0L)
        TrafficStatsTracker.onStatsPersist = { comp, uncomp ->
            persistedComp.addAndGet(comp)
            persistedUncomp.addAndGet(uncomp)
        }

        // Below threshold (64 KB)
        TrafficStatsTracker.record(compressedDelta = 10_000L, uncompressedDelta = 20_000L)
        assertEquals(0L, persistedComp.get())

        // Cross threshold (64 KB = 65536)
        TrafficStatsTracker.record(compressedDelta = 20_000L, uncompressedDelta = 40_000L)
        assertEquals(30_000L, persistedComp.get())
        assertEquals(60_000L, persistedUncomp.get())
    }

    @Test
    fun testReset() {
        TrafficStatsTracker.seed(5000L, 10000L)
        TrafficStatsTracker.reset()

        val stats = TrafficStatsTracker.statsFlow.value
        assertEquals(0L, stats.compressedBytes)
        assertEquals(0L, stats.uncompressedBytes)
        assertEquals(0L, stats.savedBytes)
    }

    @Test
    fun testFormatBytes() {
        assertEquals("0 Б", TrafficStatsTracker.formatBytes(0L))
        assertEquals("0 Б", TrafficStatsTracker.formatBytes(-100L))
        assertEquals("500 Б", TrafficStatsTracker.formatBytes(500L))
        assertEquals("1.0 КБ", TrafficStatsTracker.formatBytes(1024L))
        assertEquals("1.5 КБ", TrafficStatsTracker.formatBytes(1536L))
        assertEquals("1.00 МБ", TrafficStatsTracker.formatBytes(1024L * 1024L))
        assertEquals("2.50 ГБ", TrafficStatsTracker.formatBytes((2.5 * 1024 * 1024 * 1024).toLong()))
    }
}
