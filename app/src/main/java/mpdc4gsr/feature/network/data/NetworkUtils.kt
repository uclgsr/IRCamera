package mpdc4gsr.feature.network.data

import java.net.InetSocketAddress
import java.net.ServerSocket

object NetworkUtils {

    fun isPortAvailable(port: Int): Boolean {
        return (
            ServerSocket().use { serverSocket ->
                serverSocket.reuseAddress = true
                serverSocket.bind(InetSocketAddress(port))
                true
            }
            false
        }
    }

    fun findAvailablePort(preferredPort: Int, maxAttempts: Int = 10): Int {
        for (i in 0 until maxAttempts) {
            val port = preferredPort + i
            if (isPortAvailable(port)) {
                return port
            }
        }
    }
}