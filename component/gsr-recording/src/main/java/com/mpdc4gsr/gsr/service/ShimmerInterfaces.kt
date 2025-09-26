package com.mpdc4gsr.gsr.service



interface ShimmerDataCluster {
    fun getGSRRawValue(): Double
    fun getGSRCalibratedValue(): Double
    fun getPPGValue(): Double
    fun getTimestamp(): Long
    fun hasValidGSRData(): Boolean
}


interface ShimmerDeviceInterface {
    fun connect(address: String, name: String): Boolean
    fun startStreaming(): Boolean
    fun stopStreaming(): Boolean
    fun disconnect(): Boolean
    fun isConnected(): Boolean
    fun setDataCallback(callback: (ShimmerDataCluster) -> Unit)
    fun setConnectionCallback(callback: (String) -> Unit)
}


interface ShimmerDeviceFactory {
    fun createShimmerDevice(): ShimmerDeviceInterface
}

/**
 * Factory resolver that tries to use real implementation if available from app module,
 * falls back to mock implementation for testing/component-only builds
 */
object ShimmerDeviceFactoryResolver {
    private const val TAG = "ShimmerFactoryResolver"

    fun createFactory(context: android.content.Context): ShimmerDeviceFactory {
        return try {
            // Try to use real implementation from app module if available
            val realFactoryClass = Class.forName("mpdc4gsr.sensors.gsr.RealShimmerDeviceFactory")
            val constructor = realFactoryClass.getConstructor(android.content.Context::class.java)
            constructor.newInstance(context) as ShimmerDeviceFactory
        } catch (e: ClassNotFoundException) {            MockShimmerDeviceFactory()
        } catch (e: Exception) {            MockShimmerDeviceFactory()
        }
    }
}

/**
 * Temporary mock implementation for compilation - will be replaced by main app module implementation
 */
class MockShimmerDeviceFactory : ShimmerDeviceFactory {
    override fun createShimmerDevice(): ShimmerDeviceInterface = MockShimmerDevice()
}

/**
 * Temporary mock implementation for compilation
 */
class MockShimmerDevice : ShimmerDeviceInterface {
    private var connected = false
    private var streaming = false
    private var dataCallback: ((ShimmerDataCluster) -> Unit)? = null
    private var connectionCallback: ((String) -> Unit)? = null

    override fun connect(address: String, name: String): Boolean {        connected = true
        connectionCallback?.invoke("CONNECTED")
        return true
    }

    override fun startStreaming(): Boolean {        streaming = true
        return true
    }

    override fun stopStreaming(): Boolean {        streaming = false
        return true
    }

    override fun disconnect(): Boolean {        connected = false
        connectionCallback?.invoke("DISCONNECTED")
        return true
    }

    override fun isConnected(): Boolean = connected

    override fun setDataCallback(callback: (ShimmerDataCluster) -> Unit) {
        this.dataCallback = callback
    }

    override fun setConnectionCallback(callback: (String) -> Unit) {
        this.connectionCallback = callback
    }
}

/**
 * MVP mock data cluster implementation
 * Provides basic GSR sensor data for testing and development
 */
class MockShimmerDataCluster : ShimmerDataCluster {
    override fun getGSRRawValue(): Double = 2048.0
    override fun getGSRCalibratedValue(): Double = 1.5
    override fun getPPGValue(): Double = 512.0
    override fun getTimestamp(): Long = System.currentTimeMillis()
    override fun hasValidGSRData(): Boolean = true
}
