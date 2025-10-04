package mpdc4gsr.tests

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import mpdc4gsr.feature.network.data.NetworkServer
import mpdc4gsr.feature.network.data.Protocol
import mpdc4gsr.feature.network.data.ProtocolHandler
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.net.Socket

/**
 * Network protocol integration test for PC-Android communication
 *
 * Verifies protocol parsing, command handling, and time synchronization
 */
@Ignore("All tests disabled")
@RunWith(AndroidJUnit4::class)
class NetworkProtocolIntegrationTest {

    private lateinit var context: Context
    private lateinit var networkServer: NetworkServer
    private lateinit var protocolHandler: ProtocolHandler
    private var mockPcSocket: Socket? = null

    companion object {
        // Use a different port for testing to avoid conflicts with production server (8081)
        // and to isolate test traffic. Production port is 8081.
        private const val TEST_PORT = 8182
        private const val CONNECTION_TIMEOUT_MS = 5000L
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun cleanup() = runBlocking {
        mockPcSocket?.close()
        mockPcSocket = null

        if (::networkServer.isInitialized) {
            networkServer.stop()
        }
    }

    @Test
    fun testProtocolMessageParsing() {
        val testMessages = listOf(
            "HELLO device_name=test sensors=[GSR,RGB,THERMAL]",
            "START_RECORD session_id=test_session",
            "STOP_RECORD session_id=test_session",
            "SYNC_INIT",
            "SYNC_REQUEST t_pc=1234567890",
            "ACK cmd=START_RECORD",
            "ERROR cmd=START_RECORD code=FAIL msg=\"Test error\""
        )

        testMessages.forEach { message ->
            val parsed = Protocol.parseMessage(message)
            assertNotNull("Should parse: $message", parsed)
            assertTrue("Should have type", parsed!!.type.isNotEmpty())
        }
    }

    @Test
    fun testProtocolMessageCreation() {
        val hello = Protocol.createHelloMessage("test_device", listOf("GSR", "RGB"))
        assertTrue("HELLO should contain device name", hello.contains("device_name=test_device"))
        assertTrue("HELLO should contain sensors", hello.contains("sensors=[GSR,RGB]"))

        val startRecord = Protocol.createStartRecordMessage("session_001")
        assertTrue("START_RECORD should contain session_id", startRecord.contains("session_id=session_001"))

        val stopRecord = Protocol.createStopRecordMessage("session_001")
        assertTrue("STOP_RECORD should contain session_id", stopRecord.contains("session_id=session_001"))

        val syncInit = Protocol.createSyncInitMessage()
        assertEquals("SYNC_INIT message should match", "SYNC_INIT", syncInit)

        val syncRequest = Protocol.createSyncRequestMessage(1234567890L)
        assertTrue("SYNC_REQUEST should contain t_pc", syncRequest.contains("t_pc=1234567890"))

        val syncResponse = Protocol.createSyncResponseMessage(1234567890L, 1234567895L)
        assertTrue("SYNC_RESPONSE should contain t_pc", syncResponse.contains("t_pc=1234567890"))
        assertTrue("SYNC_RESPONSE should contain t_ph", syncResponse.contains("t_ph=1234567895"))

        val ack = Protocol.createAckMessage("START_RECORD", mapOf("session_id" to "test"))
        assertTrue("ACK should contain cmd", ack.contains("cmd=START_RECORD"))
        assertTrue("ACK should contain session_id", ack.contains("session_id=test"))

        val error = Protocol.createErrorMessage("START_RECORD", Protocol.ERR_FAIL, "Test error")
        assertTrue("ERROR should contain cmd", error.contains("cmd=START_RECORD"))
        assertTrue("ERROR should contain code", error.contains("code=FAIL"))
        assertTrue("ERROR should contain msg", error.contains("msg=\"Test error\""))
    }

    @Test
    fun testProtocolHandlerWithMockCommands() = runBlocking {
        networkServer = NetworkServer(context, TEST_PORT)
        protocolHandler = ProtocolHandler(context, networkServer)

        var startRecordingCalled = false
        var stopRecordingCalled = false
        var syncRequestCalled = false

        protocolHandler.setCommandHandler(object : ProtocolHandler.CommandHandler {
            override suspend fun onStartRecording(sessionId: String): ProtocolHandler.CommandResult {
                startRecordingCalled = true
                assertEquals("test_session_001", sessionId)
                return ProtocolHandler.CommandResult(
                    success = true,
                    message = "Recording started",
                    data = mapOf("session_id" to sessionId)
                )
            }

            override suspend fun onStopRecording(sessionId: String): ProtocolHandler.CommandResult {
                stopRecordingCalled = true
                return ProtocolHandler.CommandResult(
                    success = true,
                    message = "Recording stopped",
                    data = mapOf("session_id" to sessionId)
                )
            }

            override suspend fun onSyncRequest(pcTimestamp: Long): ProtocolHandler.SyncResult {
                syncRequestCalled = true
                assertTrue("PC timestamp should be positive", pcTimestamp > 0)
                return ProtocolHandler.SyncResult(
                    success = true,
                    phoneTimestamp = System.currentTimeMillis(),
                    offsetNs = 0L
                )
            }
        })

        val startMessage = Protocol.parseMessage("START_RECORD session_id=test_session_001")
        assertNotNull("START_RECORD should parse", startMessage)

        val startResponse = protocolHandler.processMessage(startMessage!!)
        assertNotNull("Should return response", startResponse)
        assertTrue("Should call handler", startRecordingCalled)
        assertTrue("Response should be ACK", startResponse!!.contains("ACK"))

        val stopMessage = Protocol.parseMessage("STOP_RECORD session_id=test_session_001")
        assertNotNull("STOP_RECORD should parse", stopMessage)

        val stopResponse = protocolHandler.processMessage(stopMessage!!)
        assertNotNull("Should return response", stopResponse)
        assertTrue("Should call handler", stopRecordingCalled)
        assertTrue("Response should be ACK", stopResponse!!.contains("ACK"))

        val syncMessage = Protocol.parseMessage("SYNC_REQUEST t_pc=1234567890")
        assertNotNull("SYNC_REQUEST should parse", syncMessage)

        val syncResponse = protocolHandler.processMessage(syncMessage!!)
        assertNotNull("Should return response", syncResponse)
        assertTrue("Should call handler", syncRequestCalled)
        assertTrue("Response should be SYNC_RESPONSE", syncResponse!!.contains("SYNC_RESPONSE"))
    }

    @Test
    fun testProtocolHandlerErrorCases() = runBlocking {
        networkServer = NetworkServer(context, TEST_PORT)
        protocolHandler = ProtocolHandler(context, networkServer)

        protocolHandler.setCommandHandler(object : ProtocolHandler.CommandHandler {
            override suspend fun onStartRecording(sessionId: String): ProtocolHandler.CommandResult {
                return ProtocolHandler.CommandResult(
                    success = false,
                    message = "Sensor not connected"
                )
            }

            override suspend fun onStopRecording(sessionId: String): ProtocolHandler.CommandResult {
                return ProtocolHandler.CommandResult(
                    success = false,
                    message = "Not recording"
                )
            }

            override suspend fun onSyncRequest(pcTimestamp: Long): ProtocolHandler.SyncResult {
                return ProtocolHandler.SyncResult(
                    success = false
                )
            }
        })

        val startMessage = Protocol.parseMessage("START_RECORD session_id=test")
        val startResponse = protocolHandler.processMessage(startMessage!!)
        assertTrue("Failed start should return ERROR", startResponse!!.contains("ERROR"))
        assertTrue(
            "Error should mention failure",
            startResponse.contains("FAIL") || startResponse.contains("Sensor not connected")
        )

        val stopMessage = Protocol.parseMessage("STOP_RECORD session_id=test")
        val stopResponse = protocolHandler.processMessage(stopMessage!!)
        assertTrue("Failed stop should return ERROR", stopResponse!!.contains("ERROR"))
    }

    @Test
    fun testMessageFormatCompatibility() {
        val pcFormattedMessages = listOf(
            "START_RECORD session_id=session_20240101_120000",
            "STOP_RECORD session_id=session_20240101_120000",
            "SYNC_REQUEST t_pc=1234567890",
            "SYNC_RESULT t1=1000 t2=1005 t3=1010 offset=5 rtt=10"
        )

        pcFormattedMessages.forEach { message ->
            val parsed = Protocol.parseMessage(message)
            assertNotNull("Android should parse PC message: $message", parsed)

            when (parsed!!.type) {
                Protocol.MSG_START_RECORD -> {
                    assertTrue(
                        "START_RECORD should have session_id",
                        parsed.parameters.containsKey("session_id")
                    )
                }

                Protocol.MSG_STOP_RECORD -> {
                    assertTrue(
                        "STOP_RECORD should have session_id",
                        parsed.parameters.containsKey("session_id")
                    )
                }

                Protocol.MSG_SYNC_REQUEST -> {
                    assertTrue(
                        "SYNC_REQUEST should have t_pc",
                        parsed.parameters.containsKey("t_pc")
                    )
                }

                Protocol.MSG_SYNC_RESULT -> {
                    assertTrue(
                        "SYNC_RESULT should have t1",
                        parsed.parameters.containsKey("t1")
                    )
                    assertTrue(
                        "SYNC_RESULT should have t2",
                        parsed.parameters.containsKey("t2")
                    )
                    assertTrue(
                        "SYNC_RESULT should have t3",
                        parsed.parameters.containsKey("t3")
                    )
                    assertTrue(
                        "SYNC_RESULT should have offset",
                        parsed.parameters.containsKey("offset")
                    )
                    assertTrue(
                        "SYNC_RESULT should have rtt",
                        parsed.parameters.containsKey("rtt")
                    )
                }
            }
        }
    }

    @Test
    fun testParameterParsing() {
        val message = Protocol.parseMessage("START_RECORD session_id=test_session_123")
        assertNotNull(message)
        assertEquals("START_RECORD", message!!.type)
        assertEquals("test_session_123", message.parameters["session_id"])

        val messageWithQuotes = Protocol.parseMessage("ERROR cmd=START_RECORD code=FAIL msg=\"Sensor not found\"")
        assertNotNull(messageWithQuotes)
        assertEquals("ERROR", messageWithQuotes!!.type)
        assertEquals("START_RECORD", messageWithQuotes.parameters["cmd"])
        assertEquals("FAIL", messageWithQuotes.parameters["code"])
        assertEquals("Sensor not found", messageWithQuotes.parameters["msg"])
    }

    @Test
    fun testArrayParameterParsing() {
        val message = Protocol.parseMessage("HELLO device_name=test sensors=[GSR,RGB,THERMAL]")
        assertNotNull(message)
        assertEquals("HELLO", message!!.type)
        assertEquals("test", message.parameters["device_name"])

        val sensors = message.parameters["sensors"]
        assertNotNull(sensors)
        assertTrue("Sensors should contain array brackets", sensors!!.contains("["))
        assertTrue("Sensors should contain array brackets", sensors.contains("]"))
    }

    @Test
    fun testProtocolVersionConstants() {
        assertEquals("Protocol version", "1.0", Protocol.PROTOCOL_VERSION)
        assertEquals("Default port", 8080, Protocol.DEFAULT_PORT)
        assertEquals("Server port", 8081, Protocol.DEFAULT_SERVER_PORT)
    }

    @Test
    fun testErrorCodes() {
        assertEquals("FAIL", Protocol.ERR_FAIL)
        assertEquals("BUSY", Protocol.ERR_BUSY)
        assertEquals("SENSOR_FAIL", Protocol.ERR_SENSOR_FAIL)
        assertEquals("THERMAL_NOT_FOUND", Protocol.ERR_THERMAL_NOT_FOUND)
        assertEquals("GSR_NOT_FOUND", Protocol.ERR_GSR_NOT_FOUND)
        assertEquals("INVALID_SESSION", Protocol.ERR_INVALID_SESSION)
    }
}
