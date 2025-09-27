package mpdc4gsr.network

import java.net.InetSocketAddress
import java.net.ServerSocket

/**
 * Utility class for network port operations
 */
object NetworkUtils {
    
    /**
     * Check if a port is available for binding
     */
    fun isPortAvailable(port: Int): Boolean {
        return try {
            ServerSocket().use { serverSocket ->
                serverSocket.reuseAddress = true
                serverSocket.bind(InetSocketAddress(port))
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Find an available port starting from the preferred port
     */
    fun findAvailablePort(preferredPort: Int, maxAttempts: Int = 10): Int {
        for (i in 0 until maxAttempts) {
            val port = preferredPort + i
            if (isPortAvailable(port)) {
                return port
            }
        }
        throw IllegalStateException("Could not find available port starting from $preferredPort")
    }
}