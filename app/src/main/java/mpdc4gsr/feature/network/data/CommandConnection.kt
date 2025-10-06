package mpdc4gsr.feature.network.data
import kotlinx.coroutines.flow.StateFlow

interface CommandConnection {
    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR
    }

    val connectionState: StateFlow<ConnectionState>

    suspend fun connect(): Boolean

    suspend fun sendMessage(message: String): Boolean

    suspend fun disconnect()

    fun isConnected(): Boolean

    fun setMessageCallback(callback: (String) -> Unit)

    fun setConnectionCallback(callback: (ConnectionState) -> Unit)

    fun cleanup()
}