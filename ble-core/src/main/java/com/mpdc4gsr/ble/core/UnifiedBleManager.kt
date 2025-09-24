package com.mpdc4gsr.ble.core

import android.content.Context
import android.util.Log
import com.topdon.ble.Device
import com.topdon.ble.EasyBLE
import com.topdon.ble.callback.ScanListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Unified BLE Manager that provides a common interface for BLE operations
 */
class UnifiedBleManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "UnifiedBleManager"
        @Volatile
        private var instance: UnifiedBleManager? = null
        
        fun getInstance(context: Context): UnifiedBleManager {
            return instance ?: synchronized(this) {
                instance ?: UnifiedBleManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private var easyBLE: EasyBLE? = null
    private val connectedDevices = mutableListOf<UnifiedDevice>()
    
    fun initialize(): Boolean {
        return try {
            easyBLE = EasyBLE.getBuilder().build()
            Log.i(TAG, "UnifiedBleManager initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize UnifiedBleManager", e)
            false
        }
    }
    
    fun enableMultiDeviceMode(): Boolean {
        // Multi-device mode is enabled by default in this implementation
        Log.i(TAG, "Multi-device mode enabled")
        return true
    }
    
    fun getSystemStatus(): SystemBleStatus {
        return if (easyBLE != null) {
            SystemBleStatus.READY
        } else {
            SystemBleStatus.NOT_INITIALIZED
        }
    }
    
    fun getConnectedShimmerDevices(): List<UnifiedDevice> {
        return connectedDevices.filter { device ->
            device.getName()?.contains("shimmer", ignoreCase = true) == true ||
            device.getName()?.contains("gsr", ignoreCase = true) == true
        }
    }
    
    suspend fun scanForShimmerDevices(
        timeoutMs: Long,
        callback: ShimmerScanCallback
    ) = suspendCancellableCoroutine<Unit> { continuation ->
        val ble = easyBLE ?: run {
            continuation.resumeWithException(IllegalStateException("BLE not initialized"))
            return@suspendCancellableCoroutine
        }
        
        val foundDevices = mutableListOf<UnifiedDevice>()
        
        val scanListener = object : ScanListener {
            override fun onScanStart() {
                Log.d(TAG, "Shimmer device scan started")
            }
            
            override fun onScanStop() {
                Log.d(TAG, "Shimmer device scan stopped")
                callback.onScanComplete(foundDevices)
                if (continuation.isActive) {
                    continuation.resume(Unit)
                }
            }
            
            override fun onScanResult(device: Device, isConnectedBySys: Boolean) {
                val deviceName = device.name
                if (deviceName != null && isShimmerDevice(deviceName)) {
                    val unifiedDevice = UnifiedDevice(device)
                    foundDevices.add(unifiedDevice)
                    callback.onDeviceFound(unifiedDevice)
                }
            }
            
            override fun onScanError(errorCode: Int, errorMsg: String) {
                Log.e(TAG, "Shimmer device scan failed with error: $errorCode - $errorMsg")
                callback.onScanFailed(errorCode)
                if (continuation.isActive) {
                    continuation.resumeWithException(Exception("Scan failed with error: $errorCode - $errorMsg"))
                }
            }
        }
        
        // Add listener and start scan
        ble.addScanListener(scanListener)
        ble.startScan()
        
        continuation.invokeOnCancellation {
            ble.stopScan()
            ble.removeScanListener(scanListener)
        }
    }
    
    private fun isShimmerDevice(deviceName: String): Boolean {
        val shimmerNames = listOf("shimmer", "gsr", "rn4", "shimmer3")
        return shimmerNames.any { deviceName.contains(it, ignoreCase = true) }
    }
    
    interface ShimmerScanCallback {
        fun onDeviceFound(device: UnifiedDevice)
        fun onScanComplete(foundDevices: List<UnifiedDevice>)
        fun onScanFailed(errorCode: Int)
    }
}