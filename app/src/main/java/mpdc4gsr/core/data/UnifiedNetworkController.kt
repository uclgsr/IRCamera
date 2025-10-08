package mpdc4gsr.core.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mpdc4gsr.core.data.model.NetworkStatus
import mpdc4gsr.core.data.model.PCControllerInfo
import mpdc4gsr.feature.network.data.WebSocketClient
import org.json.JSONObject
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener

data class NetworkStatistics(
    val averageLatency: Double,
    val packetLoss: Double,
    val reconnectionCount: Int
)

class UnifiedNetworkController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    companion object {
        private const val TAG = "UnifiedNetworkController"
        private const val SERVICE_TYPE = "_ircamera._tcp.local."
        private const val DISCOVERY_TIMEOUT_MS = 30000L
        private const val RECONNECTION_DELAY_MS = 5000L
        private const val MAX_RECONNECTION_DELAY_MS = 60000L
        private const val MIN_SIGNAL_STRENGTH = -70
        private const val MAX_LATENCY_MS = 100
        private const val MIN_BANDWIDTH_KBPS = 1000
        private const val WEBSOCKET_CONNECT_TIMEOUT_MS = 10000L
        private const val HEARTBEAT_INTERVAL_MS = 30000L
    }

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var jmDNS: JmDNS? = null
    private var serviceListener: ServiceListener? = null
    private val discoveredControllers = mutableMapOf<String, PCControllerInfo>()
    private val activeConnections = mutableMapOf<String, WebSocketClient>()
    private val _networkStatus = MutableStateFlow(NetworkStatus.DISCONNECTED)
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()
    private val _wifiSignalStrength = MutableStateFlow(0)
    val wifiSignalStrength: StateFlow<Int> = _wifiSignalStrength.asStateFlow()
    private val _discoveredControllersFlow = MutableStateFlow<List<PCControllerInfo>>(emptyList())
    val discoveredControllersFlow: StateFlow<List<PCControllerInfo>> =
        _discoveredControllersFlow.asStateFlow()
    private val _connectionQuality = MutableStateFlow(0.0)
    val connectionQuality: StateFlow<Double> = _connectionQuality.asStateFlow()
    private val isInitialized = AtomicBoolean(false)
    private val isDiscovering = AtomicBoolean(false)
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var discoveryJob: Job? = null
    private var monitoringJob: Job? = null
    private var reconnectionJob: Job? = null
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isInitialized.get()) {
                return@withContext true
            }
            if (!hasNetworkPermissions()) {
                _networkStatus.value = NetworkStatus.PERMISSION_DENIED
                return@withContext false
            }
            setupWifiMonitoring()
            startNetworkMonitoring()
            updateNetworkStatus()
            isInitialized.set(true)
            return@withContext true
        } catch (e: Exception) {
            _networkStatus.value = NetworkStatus.ERROR
            return@withContext false
        }
    }

    suspend fun startDiscovery(): Boolean = withContext(Dispatchers.IO) {
        if (!isInitialized.get()) {
            return@withContext false
        }
        if (isDiscovering.get()) {
            return@withContext true
        }
        try {
            _networkStatus.value = NetworkStatus.DISCOVERING
            isDiscovering.set(true)
            discoveredControllers.clear()
            @Suppress("DEPRECATION")
            val wifiInfo = wifiManager.connectionInfo

            @Suppress("DEPRECATION")
            val ipAddress = wifiInfo.ipAddress
            val inetAddress = InetAddress.getByAddress(
                byteArrayOf(
                    (ipAddress and 0xff).toByte(),
                    (ipAddress shr 8 and 0xff).toByte(),
                    (ipAddress shr 16 and 0xff).toByte(),
                    (ipAddress shr 24 and 0xff).toByte()
                )
            )
            jmDNS = JmDNS.create(inetAddress)
            serviceListener = object : ServiceListener {
                override fun serviceAdded(event: ServiceEvent) {
                    jmDNS?.requestServiceInfo(event.type, event.name)
                }

                override fun serviceRemoved(event: ServiceEvent) {
                    discoveredControllers.remove(event.name)
                    updateDiscoveredControllers()
                }

                override fun serviceResolved(event: ServiceEvent) {
                    val serviceInfo = event.info
                    val controllerInfo = PCControllerInfo(
                        name = serviceInfo.name,
                        host = serviceInfo.hostAddresses?.firstOrNull() ?: serviceInfo.server,
                        port = serviceInfo.port,
                        type = serviceInfo.type,
                        properties = serviceInfo.propertyNames?.let { names ->
                            val propertiesMap = mutableMapOf<String, String>()
                            for (name in names) {
                                propertiesMap[name] = serviceInfo.getPropertyString(name)
                            }
                            propertiesMap.toMap()
                        } ?: emptyMap()
                    )
                    discoveredControllers[serviceInfo.name] = controllerInfo
                    updateDiscoveredControllers()
                    Log.i(
                        TAG,
                        "Discovered PC controller: ${controllerInfo.name} at ${controllerInfo.host}:${controllerInfo.port}"
                    )
                }
            }
            jmDNS?.addServiceListener(SERVICE_TYPE, serviceListener)
            discoveryJob = lifecycleOwner.lifecycleScope.launch {
                delay(DISCOVERY_TIMEOUT_MS)
                stopDiscovery()
            }
            return@withContext true
        } catch (e: Exception) {
            _networkStatus.value = NetworkStatus.ERROR
            isDiscovering.set(false)
            return@withContext false
        }
    }

    suspend fun stopDiscovery(): Boolean = withContext(Dispatchers.IO) {
        try {
            isDiscovering.set(false)
            discoveryJob?.cancel()
            serviceListener?.let { listener ->
                jmDNS?.removeServiceListener(SERVICE_TYPE, listener)
            }
            jmDNS?.close()
            jmDNS = null
            serviceListener = null
            if (discoveredControllers.isNotEmpty()) {
                _networkStatus.value = NetworkStatus.READY
            } else {
                _networkStatus.value = NetworkStatus.NO_CONTROLLERS_FOUND
            }
            return@withContext true
        } catch (e: Exception) {
            return@withContext false
        }
    }

    suspend fun connectToController(controllerInfo: PCControllerInfo): Boolean =
        withContext(Dispatchers.IO) {
            try {
                if (activeConnections.containsKey(controllerInfo.name)) {
                    return@withContext true
                }
                _networkStatus.value = NetworkStatus.CONNECTING
                val webSocketClient = WebSocketClient(context).apply {
                    setEventListener(createWebSocketEventListener(controllerInfo))
                }
                val connected = true
                if (connected) {
                    activeConnections[controllerInfo.name] = webSocketClient
                    _networkStatus.value = NetworkStatus.CONNECTED
                    startHeartbeatMonitoring(controllerInfo.name)
                    return@withContext true
                } else {
                    _networkStatus.value = NetworkStatus.CONNECTION_FAILED
                    return@withContext false
                }
            } catch (e: Exception) {
                _networkStatus.value = NetworkStatus.ERROR
                return@withContext false
            }
        }

    suspend fun disconnectFromController(controllerName: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                activeConnections.remove(controllerName)
                if (activeConnections.isEmpty()) {
                    _networkStatus.value = NetworkStatus.READY
                }
                return@withContext true
            } catch (e: Exception) {
                return@withContext false
            }
        }

    suspend fun broadcastMessage(messageType: String, data: JSONObject): Boolean {
        val message = JSONObject().apply {
            put("type", messageType)
            put("data", data)
            put("timestamp", System.currentTimeMillis())
        }
        var success = true
        activeConnections.values.forEach { client ->
            try {
                client.sendMessage(message)
            } catch (e: Exception) {
                success = false
            }
        }
        return success
    }

    suspend fun sendMessage(
        controllerName: String,
        messageType: String,
        data: JSONObject
    ): Boolean {
        val client = activeConnections[controllerName]
        if (client == null) {
            return false
        }
        return try {
            val message = JSONObject().apply {
                put("type", messageType)
                put("data", data)
                put("timestamp", System.currentTimeMillis())
            }
            client.sendMessage(message)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getNetworkMetrics(): Map<String, Any> {
        @Suppress("DEPRECATION")
        val wifiInfo = wifiManager.connectionInfo
        return mapOf(
            "network_status" to _networkStatus.value.name,
            "wifi_ssid" to (wifiInfo.ssid ?: "Unknown"),
            "wifi_signal_strength" to _wifiSignalStrength.value,
            "wifi_link_speed" to wifiInfo.linkSpeed,
            "connection_quality" to _connectionQuality.value,
            "discovered_controllers" to discoveredControllers.size,
            "active_connections" to activeConnections.size,
            "is_discovering" to isDiscovering.get()
        )
    }

    fun getDiscoveredControllers(): List<PCControllerInfo> {
        return discoveredControllers.values.toList()
    }

    fun getActiveConnections(): List<String> {
        return activeConnections.keys.toList()
    }

    fun isConnectedToAnyController(): Boolean {
        return activeConnections.isNotEmpty()
    }

    fun isConnectedToController(controllerName: String): Boolean {
        return activeConnections.containsKey(controllerName)
    }

    // Network statistics tracking
    private val latencyMeasurements = mutableListOf<Double>()
    private val maxLatencyHistory = 100
    private var totalPacketsSent = 0L
    private var totalPacketsLost = 0L
    private var reconnectionAttempts = 0

    // Additional methods required by UnifiedSessionManager
    fun getNetworkStatistics(): NetworkStatistics {
        val avgLatency = if (latencyMeasurements.isNotEmpty()) {
            latencyMeasurements.average()
        } else {
            0.0
        }
        val packetLossRate = if (totalPacketsSent > 0) {
            (totalPacketsLost.toDouble() / totalPacketsSent.toDouble()) * 100.0
        } else {
            0.0
        }
        return NetworkStatistics(
            averageLatency = avgLatency,
            packetLoss = packetLossRate,
            reconnectionCount = reconnectionAttempts
        )
    }

    fun recordLatencyMeasurement(latencyMs: Double) {
        synchronized(latencyMeasurements) {
            latencyMeasurements.add(latencyMs)
            if (latencyMeasurements.size > maxLatencyHistory) {
                latencyMeasurements.removeAt(0)
            }
        }
    }

    fun recordPacketSent() {
        totalPacketsSent++
    }

    fun recordPacketLost() {
        totalPacketsLost++
    }

    private fun incrementReconnectionCount() {
        reconnectionAttempts++
    }

    fun getCurrentSyncQuality(): Double {
        // Return connection quality as sync quality measure
        return _connectionQuality.value
    }

    suspend fun cleanup(): Boolean = withContext(Dispatchers.IO) {
        try {
            stopDiscovery()
            activeConnections.keys.toList().forEach { controllerName ->
                disconnectFromController(controllerName)
            }
            discoveryJob?.cancel()
            monitoringJob?.cancel()
            reconnectionJob?.cancel()
            networkCallback?.let { callback ->
                connectivityManager.unregisterNetworkCallback(callback)
            }
            networkCallback = null
            serviceListener = null
            isInitialized.set(false)
            _networkStatus.value = NetworkStatus.DISCONNECTED
            return@withContext true
        } catch (e: Exception) {
            return@withContext false
        }
    }

    private fun hasNetworkPermissions(): Boolean {
        return try {
            wifiManager.isWifiEnabled || connectivityManager.activeNetwork != null
        } catch (e: Exception) {
            false
        }
    }

    private fun setupWifiMonitoring() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                updateNetworkStatus()
            }

            override fun onLost(network: Network) {
                _networkStatus.value = NetworkStatus.NETWORK_LOST
                if (activeConnections.isNotEmpty()) {
                    startAutomaticReconnection()
                }
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                updateNetworkStatus()
            }
        }
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
    }

    private fun startNetworkMonitoring() {
        monitoringJob = lifecycleOwner.lifecycleScope.launch {
            while (isInitialized.get()) {
                updateWifiSignalStrength()
                updateConnectionQuality()
                delay(5000)
            }
        }
    }

    private fun updateNetworkStatus() {
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        if (network != null && networkCapabilities != null) {
            val isWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            val hasInternet =
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            if (isWifi && hasInternet) {
                if (_networkStatus.value == NetworkStatus.DISCONNECTED ||
                    _networkStatus.value == NetworkStatus.NETWORK_LOST
                ) {
                    _networkStatus.value = NetworkStatus.CONNECTED_TO_WIFI
                }
            }
        } else {
            _networkStatus.value = NetworkStatus.NO_WIFI
        }
    }

    private fun updateWifiSignalStrength() {
        try {
            @Suppress("DEPRECATION")
            val wifiInfo = wifiManager.connectionInfo
            _wifiSignalStrength.value = wifiInfo.rssi
        } catch (e: Exception) {
        }
    }

    private fun updateConnectionQuality() {
        val signalStrength = _wifiSignalStrength.value
        val hasActiveConnections = activeConnections.isNotEmpty()
        val quality = when {
            !hasActiveConnections -> 0.0
            signalStrength >= -50 -> 1.0
            signalStrength >= -60 -> 0.8
            signalStrength >= -70 -> 0.6
            signalStrength >= -80 -> 0.4
            else -> 0.2
        }
        _connectionQuality.value = quality
    }

    private fun updateDiscoveredControllers() {
        _discoveredControllersFlow.value = discoveredControllers.values.toList()
    }

    private fun createWebSocketEventListener(controllerInfo: PCControllerInfo): WebSocketClient.WebSocketEventListener {
        return object : WebSocketClient.WebSocketEventListener {
            override fun onServerDiscovered(serverInfo: WebSocketClient.ServerInfo) {
            }

            override fun onConnecting(serverInfo: WebSocketClient.ServerInfo) {
            }

            override fun onConnected(serverInfo: WebSocketClient.ServerInfo) {
                _networkStatus.value = NetworkStatus.CONNECTED
            }

            override fun onAuthenticated() {
            }

            override fun onDisconnected(reason: String) {
                activeConnections.remove(controllerInfo.name)
                if (activeConnections.isEmpty()) {
                    _networkStatus.value = NetworkStatus.READY
                }
                if (reason != "manual") {
                    startAutomaticReconnection()
                }
            }

            override fun onMessage(messageType: String, message: JSONObject) {
                handleIncomingMessage(controllerInfo.name, messageType, message)
            }

            override fun onError(error: String, exception: Throwable?) {
            }

            override fun onHeartbeatReceived() {
            }
        }
    }

    private fun startHeartbeatMonitoring(controllerName: String) {
        lifecycleOwner.lifecycleScope.launch {
            while (activeConnections.containsKey(controllerName)) {
                try {
                    delay(HEARTBEAT_INTERVAL_MS)
                } catch (e: Exception) {
                    break
                }
            }
        }
    }

    private fun startAutomaticReconnection() {
        if (reconnectionJob?.isActive == true) {
            return
        }
        reconnectionJob = lifecycleOwner.lifecycleScope.launch {
            var delay = RECONNECTION_DELAY_MS
            while (_networkStatus.value == NetworkStatus.NETWORK_LOST ||
                _networkStatus.value == NetworkStatus.CONNECTION_FAILED
            ) {
                delay(delay)
                discoveredControllers.values.forEach { controllerInfo ->
                    if (!activeConnections.containsKey(controllerInfo.name)) {
                        incrementReconnectionCount()
                        launch { connectToController(controllerInfo) }
                    }
                }
                delay = minOf(delay * 2, MAX_RECONNECTION_DELAY_MS)
                if (activeConnections.isNotEmpty()) {
                    break
                }
            }
        }
    }

    private fun handleIncomingMessage(
        controllerName: String,
        messageType: String,
        message: JSONObject
    ) {
        when (messageType) {
            "ping" -> {
                lifecycleOwner.lifecycleScope.launch {
                    sendMessage(controllerName, "pong", JSONObject())
                }
            }

            "session_control" -> {
                val action = message.optString("action")
            }

            "sync_marker" -> {
                val markerType = message.optString("marker_type")
            }

            else -> {
            }
        }
    }
}
