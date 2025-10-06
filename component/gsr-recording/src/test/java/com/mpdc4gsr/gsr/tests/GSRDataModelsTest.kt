package com.mpdc4gsr.gsr.tests
import com.mpdc4gsr.gsr.model.GSRSample
import com.mpdc4gsr.gsr.model.SessionInfo
import com.mpdc4gsr.gsr.model.SyncMark
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Test
@Ignore("All tests disabled")
class GSRDataModelsTest {
    @Test
    fun testGSRSampleCreation() {
        val timestamp = System.currentTimeMillis()
        val utcTimestamp = timestamp + 1000
        val sampleIndex = 42L
        val sessionId = "test_session"
        val sample = GSRSample.createSimulated(timestamp, utcTimestamp, sampleIndex, sessionId)
        assertEquals(timestamp, sample.timestamp)
        assertEquals(utcTimestamp, sample.utcTimestamp)
        assertEquals(sampleIndex, sample.sampleIndex)
        assertEquals(sessionId, sample.sessionId)
        assertTrue("Conductance should be positive", sample.conductance > 0)
        assertTrue("Resistance should be positive", sample.resistance > 0)
    }
    @Test
    fun testGSRSampleToCsvRow() {
        val sample =
            GSRSample(
                timestamp = 1234567890L,
                utcTimestamp = 1234567891L,
                conductance = 12.345678,
                resistance = 80.987654,
                rawValue = 2048,
                sampleIndex = 100L,
                sessionId = "test_session",
            )
        val csvRow = sample.toCsvRow()
        assertEquals(7, csvRow.size)
        assertEquals("1234567890", csvRow[0])
        assertEquals("1234567891", csvRow[1])
        assertEquals("12.345678", csvRow[2])
        assertEquals("80.987654", csvRow[3])
        assertEquals("2048", csvRow[4])
        assertEquals("100", csvRow[5])
        assertEquals("test_session", csvRow[6])
    }
    @Test
    fun testSessionInfo() {
        val sessionId = "test_session"
        val startTime = System.currentTimeMillis()
        val session = SessionInfo(sessionId = sessionId, startTime = startTime)
        assertEquals(sessionId, session.sessionId)
        assertEquals(startTime, session.startTime)
        assertNull(session.endTime)
        assertTrue(session.isActive())
        assertTrue(session.getDurationMs() >= 0)
        session.endTime = startTime + 5000
        assertFalse(session.isActive())
        assertEquals(5000L, session.getDurationMs())
    }
    @Test
    fun testSyncMark() {
        val timestamp = System.currentTimeMillis()
        val utcTimestamp = timestamp + 1000
        val eventType = "THERMAL_CAPTURE"
        val sessionId = "test_session"
        val metadata = mapOf("camera" to "thermal", "frame" to "123")
        val syncMark =
            SyncMark(
                timestamp = timestamp,
                utcTimestamp = utcTimestamp,
                eventType = eventType,
                sessionId = sessionId,
                metadata = metadata,
            )
        assertEquals(timestamp, syncMark.timestamp)
        assertEquals(utcTimestamp, syncMark.utcTimestamp)
        assertEquals(eventType, syncMark.eventType)
        assertEquals(sessionId, syncMark.sessionId)
        assertEquals(metadata, syncMark.metadata)
        val csvRow = syncMark.toCsvRow()
        assertEquals(5, csvRow.size)
        assertEquals(timestamp.toString(), csvRow[0])
        assertEquals(utcTimestamp.toString(), csvRow[1])
        assertEquals(eventType, csvRow[2])
        assertEquals(sessionId, csvRow[3])
        assertTrue(csvRow[4].contains("camera=thermal"))
        assertTrue(csvRow[4].contains("frame=123"))
    }
    @Test
    fun testSyncMarkEmptyMetadata() {
        val syncMark =
            SyncMark(
                timestamp = 123L,
                utcTimestamp = 124L,
                eventType = "TEST",
                sessionId = "test",
            )
        val csvRow = syncMark.toCsvRow()
        assertEquals("", csvRow[4])
    }
}
