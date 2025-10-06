package com.mpdc4gsr.libunified.app.example
import android.content.Context
import android.util.Log
import com.mpdc4gsr.libunified.app.discovery.NetworkDiscoveryService
import com.mpdc4gsr.libunified.app.messaging.ReliableMessageService
import com.mpdc4gsr.libunified.app.security.CertificateManager
import com.mpdc4gsr.libunified.app.socket.WebSocketProxy
import com.mpdc4gsr.libunified.app.sync.TimeSyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
class EnhancedNetworkingExample(private val context: Context) {
    companion object {
        private const val TAG = "NetworkingExample"
    }
    private val discoveryService = NetworkDiscoveryService(context)
    private val certManager = CertificateManager(context)
    private val timeSyncService = TimeSyncService()
    private val webSocketProxy = WebSocketProxy.getInstance()
    private val exampleScope = CoroutineScope(Dispatchers.IO)
    fun demonstrateEnhancedNetworking() {
        exampleScope.launch {
            try {
                Log.i(TAG, "=== Enhanced Networking Demo Started ===")
                Log.i(TAG, "1. Initializing security manager...")
                certManager.initialize()
                Log.i(TAG, "2. Initializing secure WebSocket...")
                webSocketProxy.initializeSecurity(context)
                Log.i(TAG, "3. Starting device discovery...")
                discoveryService.startDiscovery()
                kotlinx.coroutines.delay(5000)
                val discoveredDevices = discoveryService.getDiscoveredDevices()
                Log.i(TAG, "Found ${discoveredDevices.size} devices")
                discoveredDevices.forEach { device ->
                    Log.i(
                        TAG,
                        "  - Device: ${device.serviceName} at ${device.ipAddress}:${device.port} (${device.deviceType})"
                    )
                }
                if (discoveredDevices.isNotEmpty()) {
                    val pcController =
                        discoveredDevices.find { it.deviceType == NetworkDiscoveryService.DeviceType.PC_CONTROLLER }
                    if (pcController != null) {
                        Log.i(TAG, "4. Synchronizing time with ${pcController.ipAddress}...")
                        val syncResult = timeSyncService.synchronizeTime(
                            pcController.ipAddress,
                            pcController.port
                        )
                        if (syncResult.isSuccess) {
                            Log.i(
                                TAG,
                                " Time synchronized. Offset: ${syncResult.clockOffsetMs}ms, RTT: ${syncResult.roundTripDelayMs}ms"
                            )
                            val syncTimestamp =
                                timeSyncService.getSynchronizedTime(syncResult.clockOffsetMs)
                            Log.i(TAG, "5. Synchronized timestamp: $syncTimestamp")
                        } else {
                            Log.w(TAG, "Time synchronization failed: ${syncResult.errorMessage}")
                        }
                        demonstrateReliableMessaging(pcController.ipAddress, pcController.port)
                    }
                }
                val thermalCamera =
                    discoveredDevices.find {
                        // TS004/TC007 device types removed - using UNKNOWN for compatibility
                        it.deviceType == NetworkDiscoveryService.DeviceType.UNKNOWN
                    }
                if (thermalCamera != null) {
                    demonstrateSecureWebSocket(thermalCamera.serviceName)
                }
                Log.i(TAG, "=== Enhanced Networking Demo Completed ===")
            } catch (e: Exception) {
                Log.e(TAG, "Demo failed", e)
            } finally {
                discoveryService.stopDiscovery()
            }
        }
    }
    private suspend fun demonstrateReliableMessaging(
        targetHost: String,
        targetPort: Int,
    ) {
        Log.i(TAG, "6. Demonstrating reliable messaging...")
        val reliableMessaging = ReliableMessageService(context)
        reliableMessaging.setTransport(
            object : ReliableMessageService.MessageTransport {
                override suspend fun sendMessage(
                    host: String,
                    port: Int,
                    message: JSONObject,
                ): Boolean {
                    Log.d(
                        TAG,
                        "Sending message to $host:$port - ${message.optString("message_type")}"
                    )
                    return true
                }
            },
        )
        reliableMessaging.initialize()
        reliableMessaging.registerMessageHandler(
            "session_start",
            object : ReliableMessageService.MessageHandler {
                override fun handleMessage(message: JSONObject): JSONObject? {
                    Log.i(TAG, "Received session start: ${message.optString("session_id")}")
                    return JSONObject().apply {
                        put("message_type", "session_ack")
                        put("status", "ready")
                    }
                }
            },
        )
        val messageId =
            reliableMessaging.sendMessage(
                targetHost = targetHost,
                targetPort = targetPort,
                messageType = "measurement_start",
                content =
                    JSONObject().apply {
                        put("session_id", "demo_session_123")
                        put("sensors", listOf("gsr", "thermal", "visual"))
                    },
                priority = ReliableMessageService.MessagePriority.CRITICAL,
                callback =
                    object : ReliableMessageService.MessageCallback {
                        override fun onAcknowledged(messageId: String) {
                            Log.i(TAG, " Message acknowledged: $messageId")
                        }
                        override fun onFailed(
                            messageId: String,
                            error: String,
                        ) {
                            Log.e(TAG, " Message failed: $messageId - $error")
                        }
                        override fun onRetrying(
                            messageId: String,
                            attempt: Int,
                        ) {
                            Log.w(TAG, "↻ Retrying message: $messageId (attempt $attempt)")
                        }
                    },
            )
        Log.i(TAG, "Sent reliable message with ID: $messageId")
        kotlinx.coroutines.delay(2000)
        reliableMessaging.shutdown()
    }
    private fun demonstrateSecureWebSocket(deviceName: String = "TS004_DEMO_DEVICE") {
        Log.i(TAG, "7. Demonstrating secure WebSocket connection...")
        webSocketProxy.startWebSocket(deviceName)
        val command =
            JSONObject().apply {
                put("cmd", "get_temperature")
                put("timestamp", System.currentTimeMillis())
            }
        webSocketProxy.sendMessage(command.toString())
        Log.i(TAG, "Sent command to thermal camera via secure WebSocket")
    }
    fun cleanup() {
        discoveryService.stopDiscovery()
        webSocketProxy.stopWebSocket()
    }
}
