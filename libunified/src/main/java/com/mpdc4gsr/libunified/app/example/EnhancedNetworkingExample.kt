package com.mpdc4gsr.libunified.app.example

import android.content.Context
import com.mpdc4gsr.libunified.app.discovery.NetworkDiscoveryService
import com.mpdc4gsr.libunified.app.messaging.ReliableMessageService
import com.mpdc4gsr.libunified.app.security.CertificateManager
import com.mpdc4gsr.libunified.app.socket.WebSocketProxy
import com.mpdc4gsr.libunified.app.sync.TimeSyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class EnhancedNetworkingExample(
    private val context: Context,
) {
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
                certManager.initialize()
                webSocketProxy.initializeSecurity(context)
                discoveryService.startDiscovery()
                kotlinx.coroutines.delay(5000)
                val discoveredDevices = discoveryService.getDiscoveredDevices()
                discoveredDevices.forEach { device ->
                }
                if (discoveredDevices.isNotEmpty()) {
                    val pcController =
                        discoveredDevices.find { it.deviceType == NetworkDiscoveryService.DeviceType.PC_CONTROLLER }
                    if (pcController != null) {
                        val syncResult =
                            timeSyncService.synchronizeTime(
                                pcController.ipAddress,
                                pcController.port,
                            )
                        if (syncResult.isSuccess) {
                            val syncTimestamp =
                                timeSyncService.getSynchronizedTime(syncResult.clockOffsetMs)
                        } else {
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
            } catch (e: Exception) {
            } finally {
                discoveryService.stopDiscovery()
            }
        }
    }

    private suspend fun demonstrateReliableMessaging(
        targetHost: String,
        targetPort: Int,
    ) {
        val reliableMessaging = ReliableMessageService(context)
        reliableMessaging.setTransport(
            object : ReliableMessageService.MessageTransport {
                override suspend fun sendMessage(
                    host: String,
                    port: Int,
                    message: JSONObject,
                ): Boolean = true
            },
        )
        reliableMessaging.initialize()
        reliableMessaging.registerMessageHandler(
            "session_start",
            object : ReliableMessageService.MessageHandler {
                override fun handleMessage(message: JSONObject): JSONObject? =
                    JSONObject().apply {
                        put("message_type", "session_ack")
                        put("status", "ready")
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
                        }

                        override fun onFailed(
                            messageId: String,
                            error: String,
                        ) {
                        }

                        override fun onRetrying(
                            messageId: String,
                            attempt: Int,
                        ) {
                        }
                    },
            )
        kotlinx.coroutines.delay(2000)
        reliableMessaging.shutdown()
    }

    private fun demonstrateSecureWebSocket(deviceName: String = "TS004_DEMO_DEVICE") {
        webSocketProxy.startWebSocket(deviceName)
        val command =
            JSONObject().apply {
                put("cmd", "get_temperature")
                put("timestamp", System.currentTimeMillis())
            }
        webSocketProxy.sendMessage(command.toString())
    }

    fun cleanup() {
        discoveryService.stopDiscovery()
        webSocketProxy.stopWebSocket()
    }
}
