package com.mpdc4gsr.gsr.service

import android.util.Log


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

    override fun connect(address: String, name: String): Boolean {
        Log.d("MockShimmerDevice", "Mock connect to $address")
        connected = true
        connectionCallback?.invoke("CONNECTED")
        return true
    }

    override fun startStreaming(): Boolean {
        Log.d("MockShimmerDevice", "Mock start streaming")
        streaming = true
        return true
    }

    override fun stopStreaming(): Boolean {
        Log.d("MockShimmerDevice", "Mock stop streaming")
        streaming = false
        return true
    }

    override fun disconnect(): Boolean {
        Log.d("MockShimmerDevice", "Mock disconnect")
        connected = false
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
 * Temporary mock data cluster for compilation
 */
class MockShimmerDataCluster : ShimmerDataCluster {
    override fun getGSRRawValue(): Double = 2048.0
    override fun getGSRCalibratedValue(): Double = 1.5
    override fun getPPGValue(): Double = 512.0
    override fun getTimestamp(): Long = System.currentTimeMillis()
    override fun hasValidGSRData(): Boolean = true
}
