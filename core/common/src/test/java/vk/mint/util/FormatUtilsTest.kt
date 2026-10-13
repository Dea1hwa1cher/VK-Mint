package vk.mint.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatUtilsTest {

    @Test
    fun testDurationFormatting() {
        assertEquals("0:00", 0.toDurationString())
        assertEquals("0:45", 45.toDurationString())
        assertEquals("3:15", 195.toDurationString())
        assertEquals("1:01:05", 3665.toDurationString())
        assertEquals("0:00", (-10).toDurationString())
    }

    @Test
    fun testDurationMsFormatting() {
        assertEquals("3:15", 195_000L.toDurationString())
        assertEquals("0:00", 0L.toDurationString())
    }

    @Test
    fun testCountFormatting() {
        assertEquals("999", 999.toCountString())
        assertEquals("1K", 1000.toCountString())
        assertEquals("1.5K", 1500.toCountString())
        assertEquals("80K", 80000.toCountString())
        assertEquals("2.3M", 2300000.toCountString())
    }

    @Test
    fun testFileSizeFormatting() {
        assertEquals("0 Б", (-10L).toFileSizeString())
        assertEquals("500 Б", 500L.toFileSizeString())
        assertEquals("1 КБ", 1024L.toFileSizeString())
        assertEquals("1.5 КБ", 1536L.toFileSizeString())
        assertEquals("1 МБ", (1024L * 1024).toFileSizeString())
        assertEquals("1 ГБ", (1024L * 1024 * 1024).toFileSizeString())
    }

    @Test
    fun testCompactMetricValue() {
        assertEquals("1K", compactMetricValue(1.0, "K"))
        assertEquals("1.2K", compactMetricValue(1.2, "K"))
        assertEquals("10M", compactMetricValue(10.0, "M"))
    }
}
