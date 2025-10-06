package mpdc4gsr.feature.network.data

object Protocol {
    // Message types as per the specification
    const val MSG_HELLO = "HELLO"
    const val MSG_SYNC_INIT = "SYNC_INIT"
    const val MSG_SYNC_REQUEST = "SYNC_REQUEST"
    const val MSG_SYNC_RESPONSE = "SYNC_RESPONSE"
    const val MSG_SYNC_RESULT = "SYNC_RESULT"
    const val MSG_START_RECORD = "START_RECORD"
    const val MSG_STOP_RECORD = "STOP_RECORD"
    const val MSG_ACK = "ACK"
    const val MSG_ERROR = "ERROR"
    const val MSG_DATA_GSR = "DATA_GSR"
    const val MSG_FRAME = "FRAME"

    // Protocol configuration
    const val PROTOCOL_VERSION = "1.0"
    const val DEFAULT_PORT = 8080
    const val DEFAULT_SERVER_PORT = 8081  // Different port for NetworkServer to avoid conflicts
    const val MAX_MESSAGE_SIZE = 10 * 1024 * 1024 // 10MB for frames

    // Error codes
    const val ERR_FAIL = "FAIL"
    const val ERR_BUSY = "BUSY"
    const val ERR_SENSOR_FAIL = "SENSOR_FAIL"
    const val ERR_THERMAL_NOT_FOUND = "THERMAL_NOT_FOUND"
    const val ERR_GSR_NOT_FOUND = "GSR_NOT_FOUND"
    const val ERR_INVALID_SESSION = "INVALID_SESSION"

    fun createHelloMessage(deviceId: String, sensors: List<String>): String {
        return "$MSG_HELLO device_name=$deviceId sensors=[${sensors.joinToString(",")}]"
    }

    fun createSyncInitMessage(): String {
        return MSG_SYNC_INIT
    }

    fun createSyncRequestMessage(pcTimestamp: Long): String {
        return "$MSG_SYNC_REQUEST t_pc=$pcTimestamp"
    }

    fun createSyncResponseMessage(pcTimestamp: Long, phoneTimestamp: Long): String {
        return "$MSG_SYNC_RESPONSE t_pc=$pcTimestamp t_ph=$phoneTimestamp"
    }

    fun createSyncResultMessage(t1: Long, t2: Long, t3: Long, offsetMs: Long, rttMs: Long): String {
        return "$MSG_SYNC_RESULT t1=$t1 t2=$t2 t3=$t3 offset=$offsetMs rtt=$rttMs"
    }

    fun createStartRecordMessage(sessionId: String): String {
        return "$MSG_START_RECORD session_id=$sessionId"
    }

    fun createStopRecordMessage(sessionId: String): String {
        return "$MSG_STOP_RECORD session_id=$sessionId"
    }

    fun createAckMessage(command: String, info: Map<String, String> = emptyMap()): String {
        val infoStr = if (info.isNotEmpty()) {
            " " + info.entries.joinToString(" ") { "${it.key}=${it.value}" }
        } else ""
        return "$MSG_ACK cmd=$command$infoStr"
    }

    fun createErrorMessage(command: String?, errorCode: String, message: String): String {
        val cmdStr = if (command != null) "cmd=$command " else ""
        return "$MSG_ERROR ${cmdStr}code=$errorCode msg=\"$message\""
    }

    fun createDataGsrMessage(timestamp: Long, value: Double): String {
        return "$MSG_DATA_GSR ts=$timestamp value=$value"
    }

    fun parseMessage(message: String): ProtocolMessage? {
        return try {
            val parts = message.trim().split(" ", limit = 2)
            if (parts.isEmpty()) return null
            val messageType = parts[0]
            val params = if (parts.size > 1) parseParameters(parts[1]) else emptyMap()
            ProtocolMessage(messageType, params)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseParameters(paramString: String): Map<String, String> {
        val params = mutableMapOf<String, String>()
        // Updated regex to properly handle quoted strings
        val regex = Regex("""(\w+)=("([^"]*)"|([^\s]+))""")
        regex.findAll(paramString).forEach { match ->
            val key = match.groups[1]?.value ?: return@forEach
            // If quoted (group 3 has content), use quoted value, else use unquoted (group 4)
            val value = match.groups[3]?.value ?: match.groups[4]?.value ?: return@forEach
            params[key] = value
        }
        return params
    }

    data class ProtocolMessage(
        val type: String,
        val parameters: Map<String, String>
    )
}