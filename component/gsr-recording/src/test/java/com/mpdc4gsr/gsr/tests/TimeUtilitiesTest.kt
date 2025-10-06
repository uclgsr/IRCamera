package com.mpdc4gsr.gsr.tests
import com.mpdc4gsr.gsr.util.TimeUtils
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Test
@Ignore("All tests disabled")
class TimeUtilitiesTest {
    @Test
    fun testPcTimeOffset() {
        TimeUtils.initializeGroundTruthTiming()
        val initialOffset = TimeUtils.getPcTimeOffset()
        assertEquals(0L, initialOffset)
        val testOffset = 5000L
        TimeUtils.setPcTimeOffset(testOffset)
        assertEquals(testOffset, TimeUtils.getPcTimeOffset())
        TimeUtils.setPcTimeOffset(0L)
    }
    @Test
    fun testUtcTimestamp() {
        val offset = 1000L
        TimeUtils.setPcTimeOffset(offset)
        val systemTime = System.currentTimeMillis()
        val utcTime = TimeUtils.getUtcTimestamp()
        assertTrue("UTC time should be greater than system time", utcTime > systemTime)
        assertTrue(
            "UTC time should be close to system time + offset",
            Math.abs(utcTime - (systemTime + offset)) < 100,
        )
        TimeUtils.setPcTimeOffset(0L)
    }
    @Test
    fun testTimeConversion() {
        TimeUtils.initializeGroundTruthTiming()
        val offset = 2000L
        TimeUtils.setPcTimeOffset(offset)
        val systemTime = System.currentTimeMillis()
        val utcTime = TimeUtils.systemToUtc(systemTime)
        val backToSystem = TimeUtils.utcToSystem(utcTime)
        assertTrue(
            "UTC time should include PC offset",
            Math.abs(utcTime - (systemTime + offset)) < 100
        )
        assertTrue(
            "Back conversion should be close to original",
            Math.abs(backToSystem - systemTime) < 100
        )
        TimeUtils.setPcTimeOffset(0L)
    }
    @Test
    fun testFormatTimestamp() {
        val timestamp = 1640995200000L
        val formatted = TimeUtils.formatTimestamp(timestamp)
        assertTrue(
            "Formatted time should contain year",
            formatted.contains("2022") || formatted.contains("2021")
        )
        assertTrue("Formatted time should contain time separator", formatted.contains(":"))
    }
    @Test
    fun testGenerateSessionId() {
        val sessionId1 = TimeUtils.generateSessionId()
        val sessionId2 = TimeUtils.generateSessionId()
        val customId = TimeUtils.generateSessionId("CUSTOM")
        assertTrue("Session ID should start with GSR", sessionId1.startsWith("GSR_"))
        assertTrue("Session ID should start with GSR", sessionId2.startsWith("GSR_"))
        assertTrue("Custom session ID should start with CUSTOM", customId.startsWith("CUSTOM_"))
        assertTrue("Session ID should not be empty", sessionId1.length > 4)
        assertTrue("Session ID should contain underscore", sessionId1.contains("_"))
    }
    @Test
    fun testGroundTruthTiming() {
        TimeUtils.initializeGroundTruthTiming()
        val groundTruthBase = TimeUtils.getGroundTruthBase()
        assertTrue(
            "Ground truth base should be recent",
            System.currentTimeMillis() - groundTruthBase < 1000,
        )
        val syncTime = TimeUtils.getSynchronizedTimestamp()
        assertTrue("Synchronized timestamp should be valid", syncTime > 0)
    }
    @Test
    fun testTimingMetadata() {
        TimeUtils.initializeGroundTruthTiming()
        TimeUtils.setPcTimeOffset(1500L)
        val metadata = TimeUtils.getTimingMetadata()
        assertTrue("Should contain ground truth base", metadata.containsKey("ground_truth_base"))
        assertTrue("Should contain PC offset", metadata.containsKey("pc_offset_ms"))
        assertTrue("Should contain device model", metadata.containsKey("device_model"))
        assertTrue("Should contain timing mode", metadata.containsKey("timing_mode"))
        assertEquals("1500", metadata["pc_offset_ms"])
        assertNotNull("Device model should be detected", metadata["device_model"])
        assertEquals("unified_ntp_style", metadata["timing_mode"])
        assertNotNull("Device processor should be detected", metadata["device_processor"])
        TimeUtils.setPcTimeOffset(0L)
    }
}
