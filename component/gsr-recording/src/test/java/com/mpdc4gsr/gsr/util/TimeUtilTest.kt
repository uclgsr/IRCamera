package com.mpdc4gsr.gsr.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeUtilTest {
    @Test
    fun testPcTimeOffset() {

        TimeUtil.initializeGroundTruthTiming()

        val initialOffset = TimeUtil.getPcTimeOffset()
        assertEquals(0L, initialOffset)

        val testOffset = 5000L
        TimeUtil.setPcTimeOffset(testOffset)
        assertEquals(testOffset, TimeUtil.getPcTimeOffset())

        TimeUtil.setPcTimeOffset(0L)
    }

    @Test
    fun testUtcTimestamp() {
        val offset = 1000L
        TimeUtil.setPcTimeOffset(offset)

        val systemTime = System.currentTimeMillis()
        val utcTime = TimeUtil.getUtcTimestamp()

        assertTrue("UTC time should be greater than system time", utcTime > systemTime)
        assertTrue(
            "UTC time should be close to system time + offset",
            Math.abs(utcTime - (systemTime + offset)) < 100,
        )

        TimeUtil.setPcTimeOffset(0L)
    }

    @Test
    fun testTimeConversion() {

        TimeUtil.initializeGroundTruthTiming()

        val offset = 2000L
        TimeUtil.setPcTimeOffset(offset)

        val systemTime = System.currentTimeMillis()
        val utcTime = TimeUtil.systemToUtc(systemTime)
        val backToSystem = TimeUtil.utcToSystem(utcTime)

        assertTrue(
            "UTC time should include PC offset",
            Math.abs(utcTime - (systemTime + offset)) < 100
        )
        assertTrue(
            "Back conversion should be close to original",
            Math.abs(backToSystem - systemTime) < 100
        )

        TimeUtil.setPcTimeOffset(0L)
    }

    @Test
    fun testFormatTimestamp() {
        val timestamp = 1640995200000L 
        val formatted = TimeUtil.formatTimestamp(timestamp)

        assertTrue(
            "Formatted time should contain year",
            formatted.contains("2022") || formatted.contains("2021")
        )
        assertTrue("Formatted time should contain time separator", formatted.contains(":"))
    }

    @Test
    fun testGenerateSessionId() {
        val sessionId1 = TimeUtil.generateSessionId()
        val sessionId2 = TimeUtil.generateSessionId()
        val customId = TimeUtil.generateSessionId("CUSTOM")

        assertTrue("Session ID should start with GSR", sessionId1.startsWith("GSR_"))
        assertTrue("Session ID should start with GSR", sessionId2.startsWith("GSR_"))
        assertTrue("Custom session ID should start with CUSTOM", customId.startsWith("CUSTOM_"))

        assertTrue("Session ID should not be empty", sessionId1.length > 4)
        assertTrue("Session ID should contain underscore", sessionId1.contains("_"))
    }

    @Test
    fun testGroundTruthTiming() {

        TimeUtil.initializeGroundTruthTiming()

        val groundTruthBase = TimeUtil.getGroundTruthBase()
        assertTrue(
            "Ground truth base should be recent",
            System.currentTimeMillis() - groundTruthBase < 1000,
        )

        val syncTime = TimeUtil.getSynchronizedTimestamp()
        assertTrue("Synchronized timestamp should be valid", syncTime > 0)
    }

    @Test
    fun testTimingMetadata() {
        TimeUtil.initializeGroundTruthTiming()
        TimeUtil.setPcTimeOffset(1500L)

        val metadata = TimeUtil.getTimingMetadata()

        assertTrue("Should contain ground truth base", metadata.containsKey("ground_truth_base"))
        assertTrue("Should contain PC offset", metadata.containsKey("pc_offset_ms"))
        assertTrue("Should contain device model", metadata.containsKey("device_model"))
        assertTrue("Should contain timing mode", metadata.containsKey("timing_mode"))

        assertEquals("1500", metadata["pc_offset_ms"])

        assertNotNull("Device model should be detected", metadata["device_model"])
        assertEquals("unified_ntp_style", metadata["timing_mode"])

        assertNotNull("Device processor should be detected", metadata["device_processor"])

        TimeUtil.setPcTimeOffset(0L)
    }
}
