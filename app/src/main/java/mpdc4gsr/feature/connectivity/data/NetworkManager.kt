package mpdc4gsr.feature.connectivity.data

import android.bluetooth.BluetoothDevice
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mpdc4gsr.core.common.logging.StructuredLogger

class NetworkManager(
    private val context: Context,
    private val recordingController: RecordingController,
) {
    private val logger = StructuredLogger.getInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val networkClient = NetworkClient(context)

    suspend fun sendResponse(message: String) {
        dispatchToServer(message)
    }

    suspend fun sendTelemetry(message: String) {
        dispatchToServer(message)
    }

    suspend fun connectWifi(
        ipAddress: String,
        port: Int,
    ): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                networkClient.connectToController(ipAddress, port)
            }.getOrElse { false }
        }

    suspend fun connectBluetooth(device: BluetoothDevice): Boolean =
        withContext(Dispatchers.IO) {
            logger.log(
                StructuredLogger.LogLevel.WARNING,
                "NetworkManager",
                "bluetooth_connection_not_supported",
                mapOf("device_address" to device.address),
            )
            false
        }

    suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            networkClient.disconnect()
        }
    }

    fun cleanup() {
        scope.launch {
            runCatching { networkClient.disconnect() }
        }
    }

    private suspend fun dispatchToServer(message: String) {
        val server = recordingController.getAttachedNetworkServer()
        if (server != null) {
            runCatching { server.sendMessage(message) }
                .onFailure {
                    logger.log(
                        StructuredLogger.LogLevel.WARNING,
                        "NetworkManager",
                        "server_send_failed",
                        mapOf("message" to message.take(128)),
                    )
                }
        } else {
            StructuredLogger.logInfo(
                component = "NetworkManager",
                event = "network_message_queued",
                details = mapOf("message" to message.take(128)),
            )
        }
    }
}

