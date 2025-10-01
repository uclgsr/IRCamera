package mpdc4gsr.network

/**
 * Public API facade for Protocol
 * Exposes the Protocol object and its nested types
 */

// Import the actual Protocol object
import mpdc4gsr.feature.network.data.Protocol as InternalProtocol

// Re-export as an object to maintain the same API
object Protocol {
    // Delegate all constants
    const val MSG_HELLO = InternalProtocol.MSG_HELLO
    const val MSG_SYNC_REQUEST = InternalProtocol.MSG_SYNC_REQUEST
    const val MSG_SYNC_RESPONSE = InternalProtocol.MSG_SYNC_RESPONSE
    const val MSG_SYNC_RESULT = InternalProtocol.MSG_SYNC_RESULT
    const val MSG_START_RECORD = InternalProtocol.MSG_START_RECORD
    const val MSG_STOP_RECORD = InternalProtocol.MSG_STOP_RECORD
    const val MSG_ACK = InternalProtocol.MSG_ACK
    const val MSG_ERROR = InternalProtocol.MSG_ERROR
    const val MSG_DATA_GSR = InternalProtocol.MSG_DATA_GSR
    const val MSG_FRAME = InternalProtocol.MSG_FRAME

    const val PROTOCOL_VERSION = InternalProtocol.PROTOCOL_VERSION
    const val DEFAULT_PORT = InternalProtocol.DEFAULT_PORT
    const val DEFAULT_SERVER_PORT = InternalProtocol.DEFAULT_SERVER_PORT
    const val MAX_MESSAGE_SIZE = InternalProtocol.MAX_MESSAGE_SIZE

    const val ERR_FAIL = InternalProtocol.ERR_FAIL
    const val ERR_BUSY = InternalProtocol.ERR_BUSY
    const val ERR_SENSOR_FAIL = InternalProtocol.ERR_SENSOR_FAIL
    const val ERR_THERMAL_NOT_FOUND = InternalProtocol.ERR_THERMAL_NOT_FOUND
    const val ERR_GSR_NOT_FOUND = InternalProtocol.ERR_GSR_NOT_FOUND
    const val ERR_INVALID_SESSION = InternalProtocol.ERR_INVALID_SESSION

    // Type aliases for nested classes
    typealias ProtocolMessage = InternalProtocol.ProtocolMessage

    // Delegate methods
    fun createHelloMessage(deviceId: String, sensors: List<String>) =
        InternalProtocol.createHelloMessage(deviceId, sensors)

    fun createSyncRequestMessage(pcTimestamp: Long) =
        InternalProtocol.createSyncRequestMessage(pcTimestamp)

    fun createSyncResponseMessage(pcTimestamp: Long, phoneTimestamp: Long) =
        InternalProtocol.createSyncResponseMessage(pcTimestamp, phoneTimestamp)

    fun createSyncResultMessage(t1: Long, t2: Long, t3: Long, offsetMs: Long, rttMs: Long) =
        InternalProtocol.createSyncResultMessage(t1, t2, t3, offsetMs, rttMs)

    fun createStartRecordMessage(sessionId: String) =
        InternalProtocol.createStartRecordMessage(sessionId)

    fun createStopRecordMessage(sessionId: String) =
        InternalProtocol.createStopRecordMessage(sessionId)

    fun createAckMessage(command: String, info: Map<String, String> = emptyMap()) =
        InternalProtocol.createAckMessage(command, info)

    fun createErrorMessage(command: String?, errorCode: String, message: String) =
        InternalProtocol.createErrorMessage(command, errorCode, message)

    fun createDataGsrMessage(timestamp: Long, value: Double) =
        InternalProtocol.createDataGsrMessage(timestamp, value)

    fun parseMessage(message: String) =
        InternalProtocol.parseMessage(message)
}
