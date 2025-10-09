package mpdc4gsr.feature.network.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.*

class PcServerDiscovery(private val context: Context) {
    companion object {
        private const val DISCOVERY_PORT = 8081
        private const val PC_SERVER_PORT = 8080
        private const val BROADCAST_MESSAGE = "IRCamera_Discovery_Request"
        private const val DISCOVERY_TIMEOUT = 5000L
        private const val SCAN_INTERVAL = 30000L
    }

    data class DiscoveredServer(
        val ipAddress: String,
        val port: Int,
        val deviceName: String?,
        val capabilities: List<String>,
        val discoveredAt: Long = System.currentTimeMillis(),
        val responseTime: Long = -1
    )

    private val discoveryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var discoveryJob: Job? = null
    private var continuousDiscoveryJob: Job? = null
    private val _discoveredServers = MutableStateFlow<List<DiscoveredServer>>(emptyList())
    val discoveredServers: StateFlow<List<DiscoveredServer>> = _discoveredServers.asStateFlow()
    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    suspend fun discoverServers(): List<DiscoveredServer> {
        return withContext(Dispatchers.IO) {
            _isDiscovering.value = true
            val servers = mutableListOf<DiscoveredServer>()
            try {
                // Method 1: Broadcast discovery
                servers.addAll(broadcastDiscovery())
                // Method 2: Network range scanning
                servers.addAll(networkRangeScanning())
                // Remove duplicates based on IP address
                val uniqueServers = servers.distinctBy { it.ipAddress }
                _discoveredServers.value = uniqueServers uniqueServers
            } catch (e: Exception) {
                emptyList()
            } finally {
                _isDiscovering.value = false
            }
        }
    }

    fun startContinuousDiscovery() {
        continuousDiscoveryJob?.cancel()
        continuousDiscoveryJob = discoveryScope.launch {
            while (isActive) {
                try {
                    discoverServers()
                    delay(SCAN_INTERVAL)
                } catch (e: Exception) {
                    delay(SCAN_INTERVAL)
                }
            }
        }
    }

    fun stopContinuousDiscovery() {
        continuousDiscoveryJob?.cancel()
        continuousDiscoveryJob = null
    }

    private suspend fun broadcastDiscovery(): List<DiscoveredServer> = withContext(Dispatchers.IO) {
        val servers = mutableListOf<DiscoveredServer>()
        try {
            android.net.TrafficStats.setThreadStatsTag(android.os.Process.myTid())
            val socket = DatagramSocket()
            android.net.TrafficStats.tagDatagramSocket(socket)
            socket.broadcast = true
            socket.soTimeout = DISCOVERY_TIMEOUT.toInt()
            // Get broadcast addresses
            val broadcastAddresses = getBroadcastAddresses()
            for (broadcastAddress in broadcastAddresses) {
                try {
                    val sendData = BROADCAST_MESSAGE.toByteArray()
                    val sendPacket = DatagramPacket(
                        sendData, sendData.size,
                        InetAddress.getByName(broadcastAddress), DISCOVERY_PORT
                    )
                    val startTime = System.currentTimeMillis()
                    socket.send(sendPacket)
                    // Listen for responses
                    val buffer = ByteArray(1024)
                    val receivePacket = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(receivePacket)
                        val responseTime = System.currentTimeMillis() - startTime
                        val response = String(receivePacket.data, 0, receivePacket.length)
                        if (response.startsWith("IRCamera_Discovery_Response")) {
                            val server = parseDiscoveryResponse(
                                receivePacket.address.hostAddress ?: "unknown",
                                response,
                                responseTime
                            )
                            server?.let { servers.add(it) }
                        }
                    } catch (e: SocketTimeoutException) {
                        // No response from this broadcast address
                    }
                } catch (e: Exception) {
                }
            }
            socket.close()
        } catch (e: Exception) {
        } finally {
            android.net.TrafficStats.clearThreadStatsTag()
        }
        servers
    }

    private suspend fun networkRangeScanning(): List<DiscoveredServer> =
        withContext(Dispatchers.IO) {
            val servers = mutableListOf<DiscoveredServer>()
            try {
                val localIp = getLocalIpAddress()
                if (localIp != null) {
                    val ipParts = localIp.split(".")
                    if (ipParts.size == 4) {
                        val baseIp = "${ipParts[0]}.${ipParts[1]}.${ipParts[2]}"
                        // Scan common IP range (1-254)
                        for (i in 1..254) {
                            if (!isActive) break
                            val targetIp = "$baseIp.$i"
                            if (targetIp != localIp) {
                                val server = testServerConnection(targetIp)
                                server?.let { servers.add(it) }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
            }
            servers
        }

    private suspend fun testServerConnection(ipAddress: String): DiscoveredServer? =
        withContext(Dispatchers.IO) {
            try {
                android.net.TrafficStats.setThreadStatsTag(android.os.Process.myTid())
                val startTime = System.currentTimeMillis()
                val socket = Socket()
                android.net.TrafficStats.tagSocket(socket)
                socket.connect(InetSocketAddress(ipAddress, PC_SERVER_PORT), 2000)
                val responseTime = System.currentTimeMillis() - startTime
                // Send a quick info query
                val output = socket.getOutputStream()
                val input = socket.getInputStream()
                val query = "INFO_QUERY\n".toByteArray()
                output.write(query)
                output.flush()
                val buffer = ByteArray(1024)
                val bytesRead = input.read(buffer)
                socket.close()
                if (bytesRead > 0) {
                    val response = String(buffer, 0, bytesRead)
                    if (response.contains("IRCamera") || response.contains("PC_Controller")) {
                        return@withContext DiscoveredServer(
                            ipAddress = ipAddress,
                            port = PC_SERVER_PORT,
                            deviceName = "PC Controller",
                            capabilities = listOf("recording", "control"),
                            responseTime = responseTime
                        )
                    }
                }
            } catch (e: Exception) {
                // Server not responding or not a PC server
            } finally {
                android.net.TrafficStats.clearThreadStatsTag()
            }
            null
        }

    private fun parseDiscoveryResponse(
        ipAddress: String,
        response: String,
        responseTime: Long
    ): DiscoveredServer? {
        try {
            val parts = response.split(";")
            var deviceName = "PC Controller"
            val capabilities = mutableListOf<String>()
            for (part in parts) {
                when {
                    part.startsWith("name=") -> deviceName = part.substring(5)
                    part.startsWith("capabilities=") -> {
                        capabilities.addAll(part.substring(13).split(","))
                    }
                }
            }
            return DiscoveredServer(
                ipAddress = ipAddress,
                port = PC_SERVER_PORT,
                deviceName = deviceName,
                capabilities = capabilities,
                responseTime = responseTime
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun getBroadcastAddresses(): List<String> {
        val addresses = mutableListOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (networkInterface in interfaces) {
                if (networkInterface.isLoopback || !networkInterface.isUp) continue
                for (interfaceAddress in networkInterface.interfaceAddresses) {
                    val broadcast = interfaceAddress.broadcast
                    if (broadcast != null) {
                        broadcast.hostAddress?.let { addresses.add(it) }
                    }
                }
            }
        } catch (e: Exception) {
        }
        // Fallback to common broadcast addresses
        if (addresses.isEmpty()) {
            addresses.addAll(listOf("192.168.1.255", "192.168.0.255", "10.0.0.255"))
        }
        return addresses
    }

    private fun getLocalIpAddress(): String? {
        try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            if (networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                for (networkInterface in interfaces) {
                    if (!networkInterface.isLoopback && networkInterface.isUp) {
                        for (interfaceAddress in networkInterface.interfaceAddresses) {
                            val address = interfaceAddress.address
                            if (!address.isLoopbackAddress && address.address.size == 4) {
                                return address.hostAddress
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
        }
        return null
    }

    fun clearDiscoveredServers() {
        _discoveredServers.value = emptyList()
    }

    fun cleanup() {
        stopContinuousDiscovery()
        discoveryScope.cancel()
        clearDiscoveredServers()
    }
}