package com.mpdc4gsr.gsr.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class ZeroconfDiscoveryService(private val context: Context) {
    companion object {
        private const val TAG = "ZeroconfDiscovery"
        private const val SERVICE_TYPE = "_ircamera._tcp."
        private const val SERVICE_NAME = "IRCamera-Device"
        private const val DISCOVERY_TIMEOUT = 30000L
    }

    private val nsdManager: NsdManager by lazy {
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }
    private val discoveredServices = ConcurrentHashMap<String, NsdServiceInfo>()
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var isDiscovering = false
    private var isRegistered = false

    interface ServiceDiscoveryListener {
        fun onServiceDiscovered(serviceInfo: NetworkClient.ControllerInfo)
        fun onServiceLost(serviceName: String)
        fun onServiceRegistered(serviceName: String)
        fun onDiscoveryError(
            errorCode: Int,
            message: String,
        )
    }

    private var serviceListener: ServiceDiscoveryListener? = null
    fun setServiceListener(listener: ServiceDiscoveryListener?) {
        serviceListener = listener
    }

    suspend fun startDiscovery(): Boolean =
        withContext(Dispatchers.Main) {
            if (isDiscovering) {
                Log.w(TAG, "Discovery already in progress")
                return@withContext true
            }
            try {
                discoveryListener = createDiscoveryListener()
                nsdManager.discoverServices(
                    SERVICE_TYPE,
                    NsdManager.PROTOCOL_DNS_SD,
                    discoveryListener
                )
                isDiscovering = true
                Log.i(TAG, "Started mDNS service discovery for type: $SERVICE_TYPE")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start service discovery", e)
                serviceListener?.onDiscoveryError(-1, e.message ?: "Discovery failed")
                false
            }
        }

    fun stopDiscovery() {
        if (!isDiscovering) return
        try {
            discoveryListener?.let { nsdManager.stopServiceDiscovery(it) }
            isDiscovering = false
            discoveredServices.clear()
            Log.i(TAG, "Stopped service discovery")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping discovery", e)
        }
    }

    suspend fun registerService(
        deviceId: String,
        port: Int,
    ): Boolean =
        withContext(Dispatchers.Main) {
            if (isRegistered) {
                Log.w(TAG, "Service already registered")
                return@withContext true
            }
            try {
                val serviceInfo =
                    NsdServiceInfo().apply {
                        serviceName = "$SERVICE_NAME-$deviceId"
                        serviceType = SERVICE_TYPE
                        setPort(port)
                    }
                registrationListener = createRegistrationListener()
                nsdManager.registerService(
                    serviceInfo,
                    NsdManager.PROTOCOL_DNS_SD,
                    registrationListener
                )
                Log.i(TAG, "Registering service: ${serviceInfo.serviceName}")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register service", e)
                false
            }
        }

    fun unregisterService() {
        if (!isRegistered) return
        try {
            registrationListener?.let { nsdManager.unregisterService(it) }
            isRegistered = false
            Log.i(TAG, "Unregistered service")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering service", e)
        }
    }

    @Suppress("DEPRECATION")
    fun getDiscoveredControllers(): List<NetworkClient.ControllerInfo> {
        return discoveredServices.values.mapNotNull { serviceInfo ->
            try {
                val host = serviceInfo.host?.hostAddress ?: return@mapNotNull null
                val port = serviceInfo.port
                val deviceName = serviceInfo.serviceName
                val capabilities = emptyList<String>()
                NetworkClient.ControllerInfo(
                    ipAddress = host,
                    port = port,
                    deviceName = deviceName,
                    capabilities = capabilities,
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse service info: ${serviceInfo.serviceName}", e)
                null
            }
        }
    }

    private fun createDiscoveryListener(): NsdManager.DiscoveryListener {
        return object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Service discovery started: $regType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(TAG, "Service discovery success: ${service.serviceName}")
                if (service.serviceName.startsWith(SERVICE_NAME)) {
                    return
                }
                @Suppress("DEPRECATION")
                nsdManager.resolveService(service, createResolveListener())
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.i(TAG, "Service lost: ${service.serviceName}")
                discoveredServices.remove(service.serviceName)
                serviceListener?.onServiceLost(service.serviceName)
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.i(TAG, "Discovery stopped: $serviceType")
                isDiscovering = false
            }

            override fun onStartDiscoveryFailed(
                serviceType: String,
                errorCode: Int,
            ) {
                Log.e(TAG, "Discovery failed to start: $serviceType, error: $errorCode")
                isDiscovering = false
                serviceListener?.onDiscoveryError(errorCode, "Failed to start discovery")
            }

            override fun onStopDiscoveryFailed(
                serviceType: String,
                errorCode: Int,
            ) {
                Log.e(TAG, "Discovery failed to stop: $serviceType, error: $errorCode")
                serviceListener?.onDiscoveryError(errorCode, "Failed to stop discovery")
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun createResolveListener(): NsdManager.ResolveListener {
        return object : NsdManager.ResolveListener {
            override fun onResolveFailed(
                serviceInfo: NsdServiceInfo,
                errorCode: Int,
            ) {
                Log.e(TAG, "Resolve failed: ${serviceInfo.serviceName}, error: $errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.i(
                    TAG,
                    "Service resolved: ${serviceInfo.serviceName} at ${serviceInfo.host}:${serviceInfo.port}"
                )
                discoveredServices[serviceInfo.serviceName] = serviceInfo
                try {
                    val host = serviceInfo.host?.hostAddress ?: return
                    val port = serviceInfo.port
                    val deviceName = serviceInfo.serviceName
                    val capabilities =
                        emptyList<String>()
                    val controllerInfo =
                        NetworkClient.ControllerInfo(
                            ipAddress = host,
                            port = port,
                            deviceName = deviceName,
                            capabilities = capabilities,
                        )
                    serviceListener?.onServiceDiscovered(controllerInfo)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse resolved service", e)
                }
            }
        }
    }

    private fun createRegistrationListener(): NsdManager.RegistrationListener {
        return object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                Log.i(TAG, "Service registered: ${serviceInfo.serviceName}")
                isRegistered = true
                serviceListener?.onServiceRegistered(serviceInfo.serviceName)
            }

            override fun onRegistrationFailed(
                serviceInfo: NsdServiceInfo,
                errorCode: Int,
            ) {
                Log.e(
                    TAG,
                    "Service registration failed: ${serviceInfo.serviceName}, error: $errorCode"
                )
                isRegistered = false
                serviceListener?.onDiscoveryError(errorCode, "Registration failed")
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                Log.i(TAG, "Service unregistered: ${serviceInfo.serviceName}")
                isRegistered = false
            }

            override fun onUnregistrationFailed(
                serviceInfo: NsdServiceInfo,
                errorCode: Int,
            ) {
                Log.e(
                    TAG,
                    "Service unregistration failed: ${serviceInfo.serviceName}, error: $errorCode"
                )
                serviceListener?.onDiscoveryError(errorCode, "Unregistration failed")
            }
        }
    }

    fun cleanup() {
        stopDiscovery()
        unregisterService()
        discoveredServices.clear()
        serviceListener = null
        discoveryListener = null
        registrationListener = null
    }
}
