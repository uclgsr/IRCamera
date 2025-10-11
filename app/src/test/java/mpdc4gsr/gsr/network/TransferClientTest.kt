package mpdc4gsr.gsr.network

import java.io.File
import java.security.MessageDigest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class TransferClientTest {

    private val dispatcher = StandardTestDispatcher()
    private val commandClient: CommandClient = mock()
    private val transferClient = TransferClient(commandClient, dispatcher)
    private val tempDir = createTempDir(prefix = "transfer-client-test")

    @After
    fun cleanup() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `uploadFile streams begin chunk and end envelopes`() = runTest(dispatcher) {
        val payload = "sample-payload-for-transfer".repeat(8).toByteArray()
        val file = File(tempDir, "example.txt").apply { writeBytes(payload) }

        transferClient.uploadFile(
            sessionId = "session-123",
            deviceId = "device-abc",
            file = file,
            modality = "GSR",
        )

        val captor = argumentCaptor<CommandEnvelope>()
        verify(commandClient, times(3)).sendCommand(captor.capture())

        val (begin, chunk, end) = captor.allValues
        assertEquals("file_begin", begin.type)
        assertEquals("file_chunk", chunk.type)
        assertEquals("file_end", end.type)

        assertEquals("session-123", begin.payload.requireString("session_id"))
        assertEquals(file.length(), begin.payload.requireLong("size_bytes"))
        assertEquals(1, begin.payload.requireInt("estimated_chunks"))

        val chunkData = chunk.payload.requireString("data")
        val decoded = java.util.Base64.getDecoder().decode(chunkData)
        assertTrue(decoded.contentEquals(payload))

        val expectedDigest =
            MessageDigest
                .getInstance("SHA-256")
                .digest(payload)
                .joinToString("") { "%02x".format(it) }
        assertEquals(expectedDigest, end.payload.requireString("sha256"))
        assertEquals(file.length(), end.payload.requireLong("size_bytes"))
        assertEquals(1, end.payload.requireInt("chunks"))
    }

    private fun JsonObject.requireString(key: String): String =
        this[key]?.jsonPrimitive?.content ?: error("Missing key $key")

    private fun JsonObject.requireLong(key: String): Long =
        this[key]?.jsonPrimitive?.content?.toLongOrNull() ?: error("Missing key $key")

    private fun JsonObject.requireInt(key: String): Int =
        this[key]?.jsonPrimitive?.content?.toIntOrNull() ?: error("Missing key $key")
}
