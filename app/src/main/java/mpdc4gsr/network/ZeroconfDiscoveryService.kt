package mpdc4gsr.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
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
            if (isDiscovering) {                return@withContext true
            }

            try {
                discoveryListener = createDiscoveryListener()
                nsdManager.discoverServices(
                    SERVICE_TYPE,
                    NsdManager.PROTOCOL_DNS_SD,
                    discoveryListener
                )
                isDiscovering = true                true
            } catch (e: Exception) {                serviceListener?.onDiscoveryError(-1, e.message ?: "Discovery failed")
                false
            }
        }

    fun stopDiscovery() {
        if (!isDiscovering) return

        try {
            discoveryListener?.let { nsdManager.stopServiceDiscovery(it) }
            isDiscovering = false
            discoveredServices.clear()        } catch (e: Exception) {        }
    }

    suspend fun registerService(
        deviceId: String,
        port: Int,
    ): Boolean =
        withContext(Dispatchers.Main) {
            if (isRegistered) {                return@withContext true
            }

            try {
                val serviceInfo =
                    NsdServiceInfo().apply {
                        serviceName = "$SERVICE_NAME-$deviceId"
                        serviceType = SERVICE_TYPE
                        setPort(port)
                        setAttribute("device_id", deviceId)
                        setAttribute("device_type", "android_phone")
                        setAttribute("capabilities", "gsr,thermal,visual,audio")
                        setAttribute("version", "1.0")
                    }

                registrationListener = createRegistrationListener()
                nsdManager.registerService(
                    serviceInfo,
                    NsdManager.PROTOCOL_DNS_SD,
                    registrationListener
                )                true
            } catch (e: Exception) {                false
            }
        }

    fun unregisterService() {
        if (!isRegistered) return

        try {
            registrationListener?.let { nsdManager.unregisterService(it) }
            isRegistered = false        } catch (e: Exception) {        }
    }

    fun getDiscoveredControllers(): List<NetworkClient.ControllerInfo> {
        return discoveredServices.values.mapNotNull { serviceInfo ->
            try {
                val host = serviceInfo.host?.hostAddress ?: return@mapNotNull null
                val port = serviceInfo.port
                val deviceName =
                    serviceInfo.attributes?.get("device_name")?.let { String(it) }
                        ?: serviceInfo.serviceName
                val capabilities =
                    serviceInfo.attributes?.get("capabilities")?.let { String(it) }
                        ?.split(",") ?: emptyList()

                NetworkClient.ControllerInfo(
                    ipAddress = host,
                    port = port,
                    deviceName = deviceName,
                    capabilities = capabilities,
                )
            } catch (e: Exception) {                null
            }
        }
    }

    private fun createDiscoveryListener(): NsdManager.DiscoveryListener {
        return object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {            }

            override fun onServiceFound(service: NsdServiceInfo) {                if (service.serviceName.startsWith(SERVICE_NAME)) {
                    return
                }

                nsdManager.resolveService(service, createResolveListener())
            }

            override fun onServiceLost(service: NsdServiceInfo) {                discoveredServices.remove(service.serviceName)
                serviceListener?.onServiceLost(service.serviceName)
            }

            override fun onDiscoveryStopped(serviceType: String) {                isDiscovering = false
            }

            override fun onStartDiscoveryFailed(
                serviceType: String,
                errorCode: Int,
            ) {                isDiscovering = false
                serviceListener?.onDiscoveryError(errorCode, "Failed to start discovery")
            }

            override fun onStopDiscoveryFailed(
                serviceType: String,
                errorCode: Int,
            ) {                serviceListener?.onDiscoveryError(errorCode, "Failed to stop discovery")
            }
        }
    }

    private fun createResolveListener(): NsdManager.ResolveListener {
        return object : NsdManager.ResolveListener {
            override fun onResolveFailed(
                serviceInfo: NsdServiceInfo,
                errorCode: Int,
            ) {            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {                discoveredServices[serviceInfo.serviceName] = serviceInfo

                try {
                    val host = serviceInfo.host?.hostAddress ?: return
                    val port = serviceInfo.port
                    val deviceName =
                        serviceInfo.attributes?.get("device_name")?.let { String(it) }
                            ?: serviceInfo.serviceName
                    val capabilities =
                        serviceInfo.attributes?.get("capabilities")?.let { String(it) }
                            ?.split(",") ?: emptyList()

                    val controllerInfo =
                        NetworkClient.ControllerInfo(
                            ipAddress = host,
                            port = port,
                            deviceName = deviceName,
                            capabilities = capabilities,
                        )

                    serviceListener?.onServiceDiscovered(controllerInfo)
                } catch (e: Exception) {                }
            }
        }
    }

    private fun createRegistrationListener(): NsdManager.RegistrationListener {
        return object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {                isRegistered = true
                serviceListener?.onServiceRegistered(serviceInfo.serviceName)
            }

            override fun onRegistrationFailed(
                serviceInfo: NsdServiceInfo,
                errorCode: Int,
            ) {                isRegistered = false
                serviceListener?.onDiscoveryError(errorCode, "Registration failed")
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {                isRegistered = false
            }

            override fun onUnregistrationFailed(
                serviceInfo: NsdServiceInfo,
                errorCode: Int,
            ) {                serviceListener?.onDiscoveryError(errorCode, "Unregistration failed")
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
