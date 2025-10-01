package mpdc4gsr.network

/**
 * Public API facade for Protocol
 * Exposes the Protocol object and its nested types
 */

// Import the actual Protocol object
import mpdc4gsr.feature.network.data.Protocol as InternalProtocol

// Re-export as an object to maintain the same API
object Protocol {
    // Delegate all properties and methods to the internal Protocol
    val VERSION = InternalProtocol.VERSION
    val MESSAGE_TYPE_HELLO = InternalProtocol.MESSAGE_TYPE_HELLO
    val MESSAGE_TYPE_SESSION_START = InternalProtocol.MESSAGE_TYPE_SESSION_START
    val MESSAGE_TYPE_SESSION_STOP = InternalProtocol.MESSAGE_TYPE_SESSION_STOP
    val MESSAGE_TYPE_SESSION_STATUS = InternalProtocol.MESSAGE_TYPE_SESSION_STATUS
    val MESSAGE_TYPE_DATA_PREVIEW = InternalProtocol.MESSAGE_TYPE_DATA_PREVIEW
    val MESSAGE_TYPE_SYNC_REQUEST = InternalProtocol.MESSAGE_TYPE_SYNC_REQUEST
    val MESSAGE_TYPE_SYNC_RESPONSE = InternalProtocol.MESSAGE_TYPE_SYNC_RESPONSE
    val MESSAGE_TYPE_ERROR = InternalProtocol.MESSAGE_TYPE_ERROR
    val MESSAGE_TYPE_ACK = InternalProtocol.MESSAGE_TYPE_ACK
    
    val ERR_INVALID_MESSAGE = InternalProtocol.ERR_INVALID_MESSAGE
    val ERR_UNSUPPORTED_VERSION = InternalProtocol.ERR_UNSUPPORTED_VERSION
    val ERR_SESSION_ACTIVE = InternalProtocol.ERR_SESSION_ACTIVE
    val ERR_SESSION_INACTIVE = InternalProtocol.ERR_SESSION_INACTIVE
    val ERR_FAIL = InternalProtocol.ERR_FAIL
    
    // Type aliases for nested classes
    typealias ProtocolMessage = InternalProtocol.ProtocolMessage
    
    // Delegate methods
    fun createHelloMessage(deviceId: String, capabilities: Map<String, Any>) = 
        InternalProtocol.createHelloMessage(deviceId, capabilities)
    
    fun createSessionStartMessage(sessionId: String, config: Map<String, Any>) = 
        InternalProtocol.createSessionStartMessage(sessionId, config)
    
    fun createSessionStopMessage(sessionId: String) = 
        InternalProtocol.createSessionStopMessage(sessionId)
    
    fun createSessionStatusMessage(sessionId: String, status: String, data: Map<String, Any>) = 
        InternalProtocol.createSessionStatusMessage(sessionId, status, data)
    
    fun createSyncResponseMessage(pcTimestamp: Long, phoneTimestamp: Long, offsetNs: Long) = 
        InternalProtocol.createSyncResponseMessage(pcTimestamp, phoneTimestamp, offsetNs)
    
    fun createErrorMessage(messageType: String, errorCode: String, errorMessage: String) = 
        InternalProtocol.createErrorMessage(messageType, errorCode, errorMessage)
    
    fun createAckMessage(messageType: String) = 
        InternalProtocol.createAckMessage(messageType)
    
    fun parseMessage(data: String) = 
        InternalProtocol.parseMessage(data)
    
    fun serializeMessage(message: ProtocolMessage) = 
        InternalProtocol.serializeMessage(message)
}
