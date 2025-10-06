package com.mpdc4gsr.libunified.app.discovery
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
class NetworkDiscoveryService(private val context: Context) {
    companion object {
        private const val TAG = "NetworkDiscovery"
        private const val SERVICE_TYPE_PC_CONTROLLER = "_topdon-pc._tcp"
        private const val SERVICE_TYPE_THERMAL_CAMERA = "_topdon-thermal._tcp"
        private const val SERVICE_NAME_PREFIX = "TOPDON-"
        private const val DISCOVERY_TIMEOUT_MS = 30000L
    }
    private val nsdManager: NsdManager by lazy {
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }
    private val discoveredServices = ConcurrentHashMap<String, DiscoveredDevice>()
    private val activeDiscoveryListeners = ConcurrentHashMap<String, NsdManager.DiscoveryListener>()
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var isDiscovering = false
    private var isRegistered = false
    private val discoveryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    data class DiscoveredDevice(
        val serviceName: String,
        val serviceType: String,
        val ipAddress: String,
        val port: Int,
        val deviceType: DeviceType,
        val attributes: Map<String, String> = emptyMap(),
        val discoveredAt: Long = System.currentTimeMillis(),
    )
    enum class DeviceType {
        PC_CONTROLLER,
        // THERMAL_CAMERA_TS004 removed - TS004 functionality disabled
        // THERMAL_CAMERA_TC007 removed - TC007 functionality disabled
        UNKNOWN,
    }
    interface DiscoveryEventListener {
        fun onDeviceDiscovered(device: DiscoveredDevice)
        fun onDeviceLost(serviceName: String)
        fun onDiscoveryStarted()
        fun onDiscoveryStopped()
        fun onError(
            operation: String,
            error: String,
        )
    }
    private var eventListener: DiscoveryEventListener? = null
    fun setEventListener(listener: DiscoveryEventListener?) {
        eventListener = listener
    }
    fun startDiscovery(): Boolean {
        return try {
            if (isDiscovering) {
                Log.w(TAG, "Discovery already in progress")
                return true
            }
            Log.i(TAG, "Starting network service discovery")
            startServiceDiscovery(SERVICE_TYPE_PC_CONTROLLER)
            startServiceDiscovery(SERVICE_TYPE_THERMAL_CAMERA)
            isDiscovering = true
            eventListener?.onDiscoveryStarted()
            discoveryScope.launch {
                delay(DISCOVERY_TIMEOUT_MS)
                if (isDiscovering) {
                    Log.i(TAG, "Discovery timeout reached, stopping discovery")
                    stopDiscovery()
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start discovery", e)
            eventListener?.onError("start_discovery", e.message ?: "Unknown error")
            false
        }
    }
    fun stopDiscovery() {
        if (!isDiscovering) return
        try {
            activeDiscoveryListeners.values.forEach { listener ->
                try {
                    nsdManager.stopServiceDiscovery(listener)
                } catch (e: Exception) {
                    Log.w(TAG, "Error stopping individual discovery listener", e)
                }
            }
            activeDiscoveryListeners.clear()
            isDiscovering = false
            eventListener?.onDiscoveryStopped()
            Log.i(TAG, "Network service discovery stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping discovery", e)
            eventListener?.onError("stop_discovery", e.message ?: "Unknown error")
        }
    }
    fun registerService(
        serviceName: String,
        port: Int,
        deviceType: DeviceType,
        attributes: Map<String, String> = emptyMap(),
    ): Boolean {
        return try {
            if (isRegistered) {
                Log.w(TAG, "Service already registered")
                return true
            }
            val serviceType =
                when (deviceType) {
                    DeviceType.PC_CONTROLLER -> SERVICE_TYPE_PC_CONTROLLER
                    // TS004/TC007 device types removed
                    else -> SERVICE_TYPE_THERMAL_CAMERA
                }
            val serviceInfo =
                NsdServiceInfo().apply {
                    this.serviceName = "$SERVICE_NAME_PREFIX$serviceName"
                    this.serviceType = serviceType
                    this.port = port
                    attributes.forEach { (key, value) ->
                        setAttribute(key, value)
                    }
                    setAttribute("device_type", deviceType.name)
                    setAttribute("version", "1.0")
                }
            registrationListener =
                object : NsdManager.RegistrationListener {
                    override fun onRegistrationFailed(
                        serviceInfo: NsdServiceInfo,
                        errorCode: Int,
                    ) {
                        Log.e(TAG, "Service registration failed: $errorCode")
                        eventListener?.onError(
                            "register_service",
                            "Registration failed: $errorCode"
                        )
                    }
                    override fun onUnregistrationFailed(
                        serviceInfo: NsdServiceInfo,
                        errorCode: Int,
                    ) {
                        Log.e(TAG, "Service unregistration failed: $errorCode")
                        eventListener?.onError(
                            "unregister_service",
                            "Unregistration failed: $errorCode"
                        )
                    }
                    override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                        Log.i(TAG, "Service registered: ${serviceInfo.serviceName}")
                        isRegistered = true
                    }
                    override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                        Log.i(TAG, "Service unregistered: ${serviceInfo.serviceName}")
                        isRegistered = false
                    }
                }
            nsdManager.registerService(
                serviceInfo,
                NsdManager.PROTOCOL_DNS_SD,
                registrationListener
            )
            Log.i(TAG, "Registering service: $serviceName on port $port")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register service", e)
            eventListener?.onError("register_service", e.message ?: "Unknown error")
            false
        }
    }
    fun unregisterService() {
        if (!isRegistered) return
        try {
            registrationListener?.let { listener ->
                nsdManager.unregisterService(listener)
            }
            registrationListener = null
            Log.i(TAG, "Service unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering service", e)
            eventListener?.onError("unregister_service", e.message ?: "Unknown error")
        }
    }
    fun getDiscoveredDevices(): List<DiscoveredDevice> {
        return discoveredServices.values.toList()
    }
    fun getDiscoveredDevicesByType(deviceType: DeviceType): List<DiscoveredDevice> {
        return discoveredServices.values.filter { it.deviceType == deviceType }
    }
    fun clearDiscoveredDevices() {
        discoveredServices.clear()
    }
    private fun startServiceDiscovery(serviceType: String) {
        val discoveryListener =
            object : NsdManager.DiscoveryListener {
                override fun onStartDiscoveryFailed(
                    serviceType: String,
                    errorCode: Int,
                ) {
                    Log.e(TAG, "Discovery start failed for $serviceType: $errorCode")
                    eventListener?.onError(
                        "start_discovery",
                        "Failed to start discovery: $errorCode"
                    )
                }
                override fun onStopDiscoveryFailed(
                    serviceType: String,
                    errorCode: Int,
                ) {
                    Log.e(TAG, "Discovery stop failed for $serviceType: $errorCode")
                    eventListener?.onError("stop_discovery", "Failed to stop discovery: $errorCode")
                }
                override fun onDiscoveryStarted(serviceType: String) {
                    Log.d(TAG, "Discovery started for $serviceType")
                }
                override fun onDiscoveryStopped(serviceType: String) {
                    Log.d(TAG, "Discovery stopped for $serviceType")
                    activeDiscoveryListeners.remove(serviceType)
                }
                override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                    Log.d(TAG, "Service found: ${serviceInfo.serviceName}")
                    if (serviceInfo.serviceName.startsWith(SERVICE_NAME_PREFIX)) {
                        resolveService(serviceInfo)
                    }
                }
                override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                    Log.d(TAG, "Service lost: ${serviceInfo.serviceName}")
                    discoveredServices.remove(serviceInfo.serviceName)
                    eventListener?.onDeviceLost(serviceInfo.serviceName)
                }
            }
        activeDiscoveryListeners[serviceType] = discoveryListener
        nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }
    private fun resolveService(serviceInfo: NsdServiceInfo) {
        val resolveListener =
            object : NsdManager.ResolveListener {
                override fun onResolveFailed(
                    serviceInfo: NsdServiceInfo,
                    errorCode: Int,
                ) {
                    Log.w(TAG, "Resolve failed for ${serviceInfo.serviceName}: $errorCode")
                }
                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    Log.d(TAG, "Service resolved: ${serviceInfo.serviceName}")
                    val deviceType = determineDeviceType(serviceInfo)
                    val attributes = extractAttributes(serviceInfo)
                    @Suppress("DEPRECATION")
                    val ipAddress = serviceInfo.host?.hostAddress ?: "unknown"
                    val discoveredDevice =
                        DiscoveredDevice(
                            serviceName = serviceInfo.serviceName,
                            serviceType = serviceInfo.serviceType,
                            ipAddress = ipAddress,
                            port = serviceInfo.port,
                            deviceType = deviceType,
                            attributes = attributes,
                        )
                    discoveredServices[serviceInfo.serviceName] = discoveredDevice
                    eventListener?.onDeviceDiscovered(discoveredDevice)
                    Log.i(
                        TAG,
                        "Discovered ${deviceType.name}: ${discoveredDevice.ipAddress}:${discoveredDevice.port}"
                    )
                }
            }
        @Suppress("DEPRECATION")
        nsdManager.resolveService(serviceInfo, resolveListener)
    }
    private fun determineDeviceType(serviceInfo: NsdServiceInfo): DeviceType {
        val deviceTypeAttr =
            serviceInfo.attributes["device_type"]?.let {
                String(it, Charsets.UTF_8)
            }
        return when {
            deviceTypeAttr == "PC_CONTROLLER" -> DeviceType.PC_CONTROLLER
            // TS004/TC007 device type detection removed
            serviceInfo.serviceType.contains("pc") -> DeviceType.PC_CONTROLLER
            // All thermal devices now return UNKNOWN since TS004/TC007 support removed
            else -> DeviceType.UNKNOWN
        }
    }
    private fun extractAttributes(serviceInfo: NsdServiceInfo): Map<String, String> {
        val attributes = mutableMapOf<String, String>()
        serviceInfo.attributes.forEach { (key, value) ->
            attributes[key] = String(value, Charsets.UTF_8)
        }
        return attributes
    }
    fun cleanup() {
        stopDiscovery()
        unregisterService()
        discoveryScope.cancel()
        discoveredServices.clear()
        activeDiscoveryListeners.clear()
    }
}
