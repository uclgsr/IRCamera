package mpdc4gsr.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolTest {

    @Test
    fun testCreateSyncRequestMessage() {
        val pcTimestamp = 1640995200000L
        val message = Protocol.createSyncRequestMessage(pcTimestamp)

        assertEquals("SYNC_REQUEST t_pc=1640995200000", message)
        assertTrue("Message should contain MSG_SYNC_REQUEST", message.startsWith(Protocol.MSG_SYNC_REQUEST))
        assertTrue("Message should contain timestamp", message.contains("t_pc=$pcTimestamp"))
    }

    @Test
    fun testCreateSyncResponseMessage() {
        val pcTimestamp = 1640995200000L
        val phoneTimestamp = 1640995200050L
        val message = Protocol.createSyncResponseMessage(pcTimestamp, phoneTimestamp)

        assertEquals("SYNC_RESPONSE t_pc=1640995200000 t_ph=1640995200050", message)
        assertTrue("Message should contain MSG_SYNC_RESPONSE", message.startsWith(Protocol.MSG_SYNC_RESPONSE))
        assertTrue("Message should contain PC timestamp", message.contains("t_pc=$pcTimestamp"))
        assertTrue("Message should contain phone timestamp", message.contains("t_ph=$phoneTimestamp"))
    }

    @Test
    fun testCreateSyncResultMessage() {
        val t1 = 1640995200000L
        val t2 = 1640995200050L
        val t3 = 1640995200100L
        val offsetMs = 50L
        val rttMs = 100L

        val message = Protocol.createSyncResultMessage(t1, t2, t3, offsetMs, rttMs)

        val expected = "SYNC_RESULT t1=1640995200000 t2=1640995200050 t3=1640995200100 offset=50 rtt=100"
        assertEquals(expected, message)

        assertTrue("Message should contain MSG_SYNC_RESULT", message.startsWith(Protocol.MSG_SYNC_RESULT))
        assertTrue("Message should contain t1", message.contains("t1=$t1"))
        assertTrue("Message should contain t2", message.contains("t2=$t2"))
        assertTrue("Message should contain t3", message.contains("t3=$t3"))
        assertTrue("Message should contain offset", message.contains("offset=$offsetMs"))
        assertTrue("Message should contain rtt", message.contains("rtt=$rttMs"))
    }

    @Test
    fun testProtocolConstants() {
        // Verify all required message types are defined
        assertEquals("HELLO", Protocol.MSG_HELLO)
        assertEquals("SYNC_REQUEST", Protocol.MSG_SYNC_REQUEST)
        assertEquals("SYNC_RESPONSE", Protocol.MSG_SYNC_RESPONSE)
        assertEquals("SYNC_RESULT", Protocol.MSG_SYNC_RESULT)
        assertEquals("START_RECORD", Protocol.MSG_START_RECORD)
        assertEquals("STOP_RECORD", Protocol.MSG_STOP_RECORD)
        assertEquals("ACK", Protocol.MSG_ACK)
        assertEquals("ERROR", Protocol.MSG_ERROR)
    }

    @Test
    fun testCreateHelloMessage() {
        val deviceId = "android_device_001"
        val sensors = listOf("RGB", "THERMAL", "GSR")
        val message = Protocol.createHelloMessage(deviceId, sensors)

        val expected = "HELLO device_name=android_device_001 sensors=[RGB,THERMAL,GSR]"
        assertEquals(expected, message)

        assertTrue("Message should contain device name", message.contains("device_name=$deviceId"))
        assertTrue("Message should contain all sensors", message.contains("RGB,THERMAL,GSR"))
    }

    @Test
    fun testCreateStartRecordMessage() {
        val sessionId = "session_20240115_143022"
        val message = Protocol.createStartRecordMessage(sessionId)

        assertEquals("START_RECORD session_id=session_20240115_143022", message)
        assertTrue("Message should contain session ID", message.contains("session_id=$sessionId"))
    }

    @Test
    fun testCreateStopRecordMessage() {
        val sessionId = "session_20240115_143022"
        val message = Protocol.createStopRecordMessage(sessionId)

        assertEquals("STOP_RECORD session_id=session_20240115_143022", message)
        assertTrue("Message should contain session ID", message.contains("session_id=$sessionId"))
    }
}