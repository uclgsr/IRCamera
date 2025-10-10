package mpdc4gsr.feature.connectivity.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Process
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mpdc4gsr.core.common.AppLogger
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.Enumeration
import java.util.concurrent.atomic.AtomicBoolean
import java.net.NetworkInterface

class PcServerDiscovery(
    private val context: Context,
) {
    companion object {
        private const val DISCOVERY_PORT = 8081
        private const val PC_SERVER_PORT = 8080
        private const val BROADCAST_MESSAGE = "IRCamera_Discovery_Request"
        private const val DISCOVERY_TIMEOUT_MS = 5_000
        private const val SCAN_INTERVAL_MS = 30_000L
    }


    data class DiscoveredServer(
        val ipAddress: String,
        val port: Int,
        val deviceName: String?,
        val capabilities: List<String>,
        val discoveredAt: Long = System.currentTimeMillis(),
        val responseTimeMs: Long = -1,
    )

    private val discoveryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val discoveryInProgress = AtomicBoolean(false)
    private var continuousDiscoveryJob: Job? = null

    private val _discoveredServers = MutableStateFlow<List<DiscoveredServer>>(emptyList())
    val discoveredServers: StateFlow<List<DiscoveredServer>> = _discoveredServers.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    suspend fun discoverServers(): List<DiscoveredServer> =
        withContext(Dispatchers.IO) {
            if (!discoveryInProgress.compareAndSet(false, true)) {
                return@withContext _discoveredServers.value
            }

            _isDiscovering.value = true

            return@withContext try {
                val results = mutableListOf<DiscoveredServer>()
                results += broadcastDiscovery()
                results += networkRangeScan()
                val unique = results.distinctBy { it.ipAddress }

                _discoveredServers.value = unique
                unique
            } catch (e: Exception) {
                AppLogger.e("PcServerDiscovery", "Discovery failed", e)
                emptyList()
            } finally {
                _isDiscovering.value = false
                discoveryInProgress.set(false)
            }
        }


    fun startContinuousDiscovery() {
        continuousDiscoveryJob?.cancel()
        continuousDiscoveryJob =
            discoveryScope.launch {
                while (isActive) {
                    try {
                        discoverServers()
                    } catch (ignored: Exception) {
                        // best effort: keep trying
                    }

                    delay(SCAN_INTERVAL_MS)
                }
            }
    }


    fun stopContinuousDiscovery() {
        continuousDiscoveryJob?.cancel()
        continuousDiscoveryJob = null
    }


    private suspend fun broadcastDiscovery(): List<DiscoveredServer> =
        withContext(Dispatchers.IO) {
            val servers = mutableListOf<DiscoveredServer>()
            val socket = DatagramSocket()
            try {
                TrafficStats.setThreadStatsTag(Process.myTid())
                TrafficStats.tagDatagramSocket(socket)
                socket.broadcast = true
                socket.soTimeout = DISCOVERY_TIMEOUT_MS

                val request = BROADCAST_MESSAGE.toByteArray()
                val packetBuffer = ByteArray(1024)
                val responsePacket = DatagramPacket(packetBuffer, packetBuffer.size)

                for (broadcastAddress in getBroadcastAddresses()) {
                    val address = InetAddress.getByName(broadcastAddress)
                    val requestPacket = DatagramPacket(request, request.size, address, DISCOVERY_PORT)
                    try {
                        val startTime = System.currentTimeMillis()
                        socket.send(requestPacket)
                        socket.receive(responsePacket)
                        val responseTime = System.currentTimeMillis() - startTime
                        val response =
                            String(responsePacket.data, 0, responsePacket.length).trim()
                        parseDiscoveryResponse(
                            responsePacket.address?.hostAddress.orEmpty(),
                            response,
                            responseTime,
                        )?.let { servers.add(it) }
                    } catch (_: SocketTimeoutException) {
                        // no response for this broadcast address
                    } catch (e: Exception) {
                        AppLogger.e("PcServerDiscovery", "Broadcast discovery error", e)
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("PcServerDiscovery", "Failed during broadcast discovery", e)
            } finally {
                socket.close()
                TrafficStats.clearThreadStatsTag()
            }

            servers
        }


    private suspend fun networkRangeScan(): List<DiscoveredServer> =
        withContext(Dispatchers.IO) {
            val servers = mutableListOf<DiscoveredServer>()
            val localIp = getLocalIpAddress() ?: return@withContext servers
            val octets = localIp.split(".")
            if (octets.size != 4) {
                return@withContext servers
            }

            val baseIp = "${octets[0]}.${octets[1]}.${octets[2]}"
            for (i in 1..254) {
                if (!isActive) break
                val targetIp = "$baseIp.$i"
                if (targetIp == localIp) continue
                testServerConnection(targetIp)?.let { servers.add(it) }
            }

            servers
        }


    private suspend fun testServerConnection(ipAddress: String): DiscoveredServer? =
        withContext(Dispatchers.IO) {
            var socket: Socket? = null
            try {
                TrafficStats.setThreadStatsTag(Process.myTid())
                socket = Socket()
                TrafficStats.tagSocket(socket)
                val start = System.currentTimeMillis()
                socket.connect(InetSocketAddress(ipAddress, PC_SERVER_PORT), /* timeout */ 2_000)
                val responseTime = System.currentTimeMillis() - start

                socket.getOutputStream().use { output ->
                    socket.getInputStream().use { input ->
                        output.write("INFO_QUERY\n".toByteArray())
                        output.flush()
                        val buffer = ByteArray(1024)
                        val count = input.read(buffer)
                        if (count > 0) {
                            val response = String(buffer, 0, count)
                            if (response.contains("IRCamera") || response.contains("PC_Controller")) {
                                return@withContext DiscoveredServer(
                                    ipAddress = ipAddress,
                                    port = PC_SERVER_PORT,
                                    deviceName = "PC Controller",
                                    capabilities = listOf("recording", "control"),
                                    responseTimeMs = responseTime,
                                )
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // ignore unreachable hosts
            } finally {
                try {
                    socket?.let { TrafficStats.untagSocket(it) }

                    socket?.close()
                } catch (_: Exception) {
                }

                TrafficStats.clearThreadStatsTag()
            }

            null
        }


    private fun parseDiscoveryResponse(
        ipAddress: String,
        response: String,
        responseTimeMs: Long,
    ): DiscoveredServer? {
        if (!response.startsWith("IRCamera_Discovery_Response"))
            return null

        val parts =
            response
                .substringAfter("::", missingDelimiterValue = "")
                .split(";")
                .mapNotNull {
                    val (key, value) = it.split("=").let { tokens ->
                        if (tokens.size == 2) tokens[0] to tokens[1] else null
                    } ?: return@mapNotNull null
                    key to value
                }.toMap()

        val name = parts["name"]
        val capabilities =
            parts["capabilities"]
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?: emptyList()
            return DiscoveredServer(
            ipAddress = ipAddress,
            port = parts["port"]?.toIntOrNull() ?: PC_SERVER_PORT,
            deviceName = name,
            capabilities = capabilities,
            responseTimeMs = responseTimeMs,
        )
    }


    private fun getBroadcastAddresses(): List<String> {
        val addresses = mutableListOf<String>()
        val interfaces: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
        try {
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                networkInterface.interfaceAddresses.forEach { address ->
                    address.broadcast?.hostAddress?.let(addresses::add)
                }
            }
        } catch (e: Exception) {
            AppLogger.e("PcServerDiscovery", "Failed to enumerate broadcast addresses", e)
        }

        return addresses.ifEmpty { listOf("255.255.255.255") }
    }


    private fun getLocalIpAddress(): String? {
        if (!isWifiConnected())
            return null
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        return address.hostAddress
                    }
                }
            }

            null
        } catch (e: Exception) {
            AppLogger.e("PcServerDiscovery", "Failed to determine local IP", e)
            null
        }
    }


    private fun isWifiConnected(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}
