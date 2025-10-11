package mpdc4gsr.gsr.network

import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.Base64
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Handles post-session data upload to the PC orchestrator (FR10). Files are streamed over the
 * command channel so the desktop can persist artefacts alongside metadata manifests.
 */
class TransferClient(
    private val commandClient: CommandClient,
    private val dispatcher: CoroutineDispatcher,
) {

    suspend fun uploadFile(
        sessionId: String,
        deviceId: String,
        file: File,
        modality: String,
    ) = withContext(dispatcher) {
        require(file.exists()) { "File does not exist: ${file.absolutePath}" }
        val fileName = file.name
        val sizeBytes = file.length()
        val mimeType = guessMimeType(file)
        val estimatedChunks = ((sizeBytes + CHUNK_SIZE_BYTES - 1) / CHUNK_SIZE_BYTES).toInt()

        commandClient.sendCommand(
            CommandEnvelope(
                type = "file_begin",
                payload =
                    buildJsonObject {
                        putCommonFields(
                            sessionId = sessionId,
                            deviceId = deviceId,
                            filename = fileName,
                            modality = modality,
                        )
                        put("size_bytes", JsonPrimitive(sizeBytes))
                        put("chunk_size", JsonPrimitive(CHUNK_SIZE_BYTES))
                        put("estimated_chunks", JsonPrimitive(estimatedChunks))
                        mimeType?.let { put("mime_type", JsonPrimitive(it)) }
                    },
            ),
        )

        val digest = MessageDigest.getInstance("SHA-256")
        var chunkIndex = 0
        file.inputStream().use { input ->
            streamFileChunks(
                input = input,
                onChunk = { data, bytesRead ->
                    digest.update(data, 0, bytesRead)
                    val encoded = Base64.getEncoder().encodeToString(data.copyOf(bytesRead))
                    chunkIndex += 1
                    commandClient.sendCommand(
                        CommandEnvelope(
                            type = "file_chunk",
                            payload =
                                buildJsonObject {
                                    putCommonFields(
                                        sessionId = sessionId,
                                        deviceId = deviceId,
                                        filename = fileName,
                                        modality = modality,
                                    )
                                    put("chunk_index", JsonPrimitive(chunkIndex))
                                    put("data", JsonPrimitive(encoded))
                                },
                        ),
                    )
                },
            )
        }

        val checksum =
            digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }

        commandClient.sendCommand(
            CommandEnvelope(
                type = "file_end",
                payload =
                    buildJsonObject {
                        putCommonFields(
                            sessionId = sessionId,
                            deviceId = deviceId,
                            filename = fileName,
                            modality = modality,
                        )
                        put("size_bytes", JsonPrimitive(sizeBytes))
                        put("chunks", JsonPrimitive(chunkIndex))
                        put("sha256", JsonPrimitive(checksum))
                        mimeType?.let { put("mime_type", JsonPrimitive(it)) }
                    },
            ),
        )
    }

    private inline fun streamFileChunks(
        input: InputStream,
        onChunk: (ByteArray, Int) -> Unit,
    ) {
        val buffer = ByteArray(CHUNK_SIZE_BYTES)
        while (true) {
            val bytesRead = input.read(buffer)
            if (bytesRead == -1) break
            onChunk(buffer, bytesRead)
        }
    }

    private fun JsonObjectBuilder.putCommonFields(
        sessionId: String,
        deviceId: String,
        filename: String,
        modality: String,
    ) {
        put("session_id", JsonPrimitive(sessionId))
        put("device_id", JsonPrimitive(deviceId))
        put("filename", JsonPrimitive(filename))
        put("modality", JsonPrimitive(modality))
    }

    private fun guessMimeType(file: File): String? =
        java.net.URLConnection.guessContentTypeFromName(file.name)

    companion object {
        private const val CHUNK_SIZE_BYTES = 256 * 1024
    }
}
