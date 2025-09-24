package mpdc4gsr.network

import kotlinx.coroutines.flow.StateFlow

/**
 * Common interface for bidirectional command/control connections.
 * Supports both TCP and Bluetooth RFCOMM connections where Android acts as client.
 */
interface CommandConnection {
    
    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR
    }
    
    /**
     * Current connection state
     */
    val connectionState: StateFlow<ConnectionState>
    
    /**
     * Connect to the remote server
     * @return true if connection successful, false otherwise
     */
    suspend fun connect(): Boolean
    
    /**
     * Send a text message to the remote server
     * @param message The message to send (will be terminated with newline)
     * @return true if sent successfully, false otherwise
     */
    suspend fun sendMessage(message: String): Boolean
    
    /**
     * Disconnect from the remote server
     */
    suspend fun disconnect()
    
    /**
     * Check if currently connected
     */
    fun isConnected(): Boolean
    
    /**
     * Set callback for incoming messages
     */
    fun setMessageCallback(callback: (String) -> Unit)
    
    /**
     * Set callback for connection state changes
     */
    fun setConnectionCallback(callback: (ConnectionState) -> Unit)
    
    /**
     * Clean up resources
     */
    fun cleanup()
}