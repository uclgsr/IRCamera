package mpdc4gsr.tests

import mpdc4gsr.feature.network.data.Protocol
import org.junit.Assert.*
import org.junit.Test

class NetworkProtocolTest {
    @Test
    fun testCreateSyncInitMessage() {
        val syncInit = Protocol.createSyncInitMessage()
        assertEquals("SYNC_INIT", syncInit)
    }

    @Test
    fun testParseSyncInitMessage() {
        val message = Protocol.parseMessage("SYNC_INIT")
        assertNotNull(message)
        assertEquals(Protocol.MSG_SYNC_INIT, message?.type)
    }

    @Test
    fun testSyncInitConstant() {
        assertEquals("SYNC_INIT", Protocol.MSG_SYNC_INIT)
    }

    @Test
    fun testCreateSyncRequestMessage() {
        val syncRequest = Protocol.createSyncRequestMessage(1234567890L)
        assertTrue(syncRequest.contains("SYNC_REQUEST"))
        assertTrue(syncRequest.contains("t_pc=1234567890"))
    }

    @Test
    fun testCreateSyncResponseMessage() {
        val syncResponse = Protocol.createSyncResponseMessage(1000L, 1005L)
        assertTrue(syncResponse.contains("SYNC_RESPONSE"))
        assertTrue(syncResponse.contains("t_pc=1000"))
        assertTrue(syncResponse.contains("t_ph=1005"))
    }

    @Test
    fun testCreateSyncResultMessage() {
        val syncResult = Protocol.createSyncResultMessage(1000L, 1005L, 1010L, 5L, 10L)
        assertTrue(syncResult.contains("SYNC_RESULT"))
        assertTrue(syncResult.contains("t1=1000"))
        assertTrue(syncResult.contains("t2=1005"))
        assertTrue(syncResult.contains("t3=1010"))
        assertTrue(syncResult.contains("offset=5"))
        assertTrue(syncResult.contains("rtt=10"))
    }
}
