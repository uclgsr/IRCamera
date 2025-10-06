package mpdc4gsr.feature.network.data

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.*
import java.util.*

class BluetoothClient(
    private val context: Context,
    private val bluetoothDevice: BluetoothDevice,
    private val serviceUuid: UUID = DEFAULT_SPP_UUID
) : CommandConnection {
    companion object {
        private const val TAG = "BluetoothClient"

        // Standard Serial Port Profile UUID
        val DEFAULT_SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private var bluetoothSocket: BluetoothSocket? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null
    private val clientScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var readerJob: Job? = null
    private val _connectionState = MutableStateFlow(CommandConnection.ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<CommandConnection.ConnectionState> =
        _connectionState.asStateFlow()
    private var messageCallback: ((String) -> Unit)? = null
    private var connectionCallback: ((CommandConnection.ConnectionState) -> Unit)? = null
    override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isConnected()) {
                Log.i(
                    TAG,
                    "Already connected to ${bluetoothDevice.name} (${bluetoothDevice.address})"
                )
                return@withContext true
            }
            val bluetoothManager =
                context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val bluetoothAdapter = bluetoothManager?.adapter
            if (bluetoothManager == null || bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                AppLogger.e(TAG, "Bluetooth not available or disabled")
                handleConnectionError("Bluetooth not available")
                return@withContext false
            }
            Log.i(
                TAG,
                "Connecting to PC via Bluetooth: ${bluetoothDevice.name} (${bluetoothDevice.address})"
            )
            _connectionState.value = CommandConnection.ConnectionState.CONNECTING
            connectionCallback?.invoke(CommandConnection.ConnectionState.CONNECTING)
            // Cancel discovery to improve connection performance
            bluetoothAdapter.cancelDiscovery()
            // Create RFCOMM socket
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(serviceUuid)
            // Connect to the device
            bluetoothSocket?.connect()
            val inputStream = bluetoothSocket?.inputStream
            val outputStream = bluetoothSocket?.outputStream
            if (inputStream != null && outputStream != null) {
                reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                writer = BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8))
                _connectionState.value = CommandConnection.ConnectionState.CONNECTED
                connectionCallback?.invoke(CommandConnection.ConnectionState.CONNECTED)
                // Start reader thread
                startReaderLoop()
                AppLogger.i(TAG, "Successfully connected to PC via Bluetooth")
                return@withContext true
            } else {
                throw IOException("Failed to get Bluetooth socket streams")
            }
        } catch (e: IOException) {
            AppLogger.e(TAG, "Failed to connect via Bluetooth to ${bluetoothDevice.address}", e)
            handleConnectionError("Bluetooth connection failed: ${e.message}")
            return@withContext false
        } catch (e: SecurityException) {
            AppLogger.e(TAG, "Security exception during Bluetooth connection - missing permissions?", e)
            handleConnectionError("Bluetooth permission denied")
            return@withContext false
        } catch (e: Exception) {
            AppLogger.e(TAG, "Unexpected error during Bluetooth connection", e)
            handleConnectionError("Connection failed: ${e.message}")
            return@withContext false
        }
    }

    override suspend fun sendMessage(message: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentWriter = writer
            if (currentWriter != null && isConnected()) {
                currentWriter.write(message)
                currentWriter.write("\n")
                currentWriter.flush()
                AppLogger.d(TAG, "Sent Bluetooth message: $message")
                return@withContext true
            } else {
                AppLogger.w(TAG, "Cannot send Bluetooth message - not connected")
                return@withContext false
            }
        } catch (e: IOException) {
            AppLogger.e(TAG, "Failed to send Bluetooth message: $message", e)
            handleConnectionError("Send failed: ${e.message}")
            return@withContext false
        }
    }

    override suspend fun disconnect(): Unit = withContext(Dispatchers.IO) {
        AppLogger.i(TAG, "Disconnecting from PC Bluetooth server")
        // Cancel reader job first to stop any ongoing reads
        readerJob?.cancel()
        readerJob = null
        // Close resources in proper order: writer, reader, then socket
        try {
            writer?.let { w ->
                try {
                    w.flush()
                    w.close()
                } catch (e: IOException) {
                    AppLogger.w(TAG, "Error closing Bluetooth writer", e)
                }
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Error flushing/closing Bluetooth writer", e)
        }
        try {
            reader?.close()
        } catch (e: IOException) {
            AppLogger.w(TAG, "Error closing Bluetooth reader", e)
        }
        try {
            bluetoothSocket?.let { socket ->
                if (socket.isConnected) {
                    socket.close()
                }
            }
        } catch (e: IOException) {
            AppLogger.w(TAG, "Error closing Bluetooth socket", e)
        } catch (e: SecurityException) {
            AppLogger.w(TAG, "Security exception closing Bluetooth socket", e)
        }
        // Clear all references
        writer = null
        reader = null
        bluetoothSocket = null
        _connectionState.value = CommandConnection.ConnectionState.DISCONNECTED
        connectionCallback?.invoke(CommandConnection.ConnectionState.DISCONNECTED)
    }

    override fun isConnected(): Boolean {
        return bluetoothSocket?.isConnected == true && _connectionState.value == CommandConnection.ConnectionState.CONNECTED
    }

    override fun setMessageCallback(callback: (String) -> Unit) {
        messageCallback = callback
    }

    override fun setConnectionCallback(callback: (CommandConnection.ConnectionState) -> Unit) {
        connectionCallback = callback
    }

    override fun cleanup() {
        clientScope.launch {
            disconnect()
        }
        clientScope.cancel()
        messageCallback = null
        connectionCallback = null
    }

    private fun startReaderLoop() {
        readerJob = clientScope.launch {
            val currentReader = reader ?: return@launch
            try {
                while (isActive && isConnected()) {
                    try {
                        val message = currentReader.readLine()
                        if (message != null) {
                            AppLogger.d(TAG, "Received Bluetooth message: $message")
                            messageCallback?.invoke(message)
                        } else {
                            AppLogger.w(TAG, "PC closed Bluetooth connection")
                            break
                        }
                    } catch (e: IOException) {
                        if (isActive) {
                            AppLogger.w(TAG, "IOException in Bluetooth reader loop", e)
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    AppLogger.e(TAG, "Error in Bluetooth reader loop", e)
                    handleConnectionError("Reader error: ${e.message}")
                }
            }
            if (isActive) {
                handleConnectionError("PC disconnected")
            }
        }
    }

    private fun handleConnectionError(errorMessage: String) {
        AppLogger.w(TAG, "Bluetooth connection error: $errorMessage")
        _connectionState.value = CommandConnection.ConnectionState.ERROR
        connectionCallback?.invoke(CommandConnection.ConnectionState.ERROR)
        clientScope.launch {
            disconnect()
        }
    }
}