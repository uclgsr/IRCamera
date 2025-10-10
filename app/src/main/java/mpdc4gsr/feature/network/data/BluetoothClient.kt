package mpdc4gsr.feature.network.data

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mpdc4gsr.core.utils.AppLogger
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.*

class BluetoothClient(
    private val context: Context,
    private val bluetoothDevice: BluetoothDevice,
    private val serviceUuid: UUID = DEFAULT_SPP_UUID,
) : CommandConnection {
    companion object {
        // Standard Serial Port Profile UUID
        val DEFAULT_SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val TAG = "BluetoothClient"
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

    override suspend fun connect(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                if (isConnected()) {
                    return@withContext true
                }

                val bluetoothManager =
                    context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val bluetoothAdapter = bluetoothManager?.adapter
                if (bluetoothManager == null || bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                    handleConnectionError("Bluetooth not available")
                    return@withContext false
                }


                _connectionState.value = CommandConnection.ConnectionState.CONNECTING
                connectionCallback?.invoke(CommandConnection.ConnectionState.CONNECTING)

                bluetoothAdapter.cancelDiscovery()

                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(serviceUuid)
                bluetoothSocket?.connect()

                val inputStream = bluetoothSocket?.inputStream
                val outputStream = bluetoothSocket?.outputStream
                if (inputStream == null || outputStream == null) {
                    throw IOException("Failed to get Bluetooth socket streams")
                }

                reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                writer = BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8))

                _connectionState.value = CommandConnection.ConnectionState.CONNECTED
                connectionCallback?.invoke(CommandConnection.ConnectionState.CONNECTED)

                startReaderLoop()
                true
            } catch (ioException: IOException) {
                handleConnectionError("Bluetooth connection failed: ${ioException.message}")
                false
            } catch (securityException: SecurityException) {
                handleConnectionError("Bluetooth permission denied")
                false
            } catch (exception: Exception) {
                handleConnectionError("Connection failed: ${exception.message}")
                false
            }
        }

    override suspend fun sendMessage(message: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val currentWriter = writer
                if (currentWriter != null && isConnected()) {
                    currentWriter.write(message)
                    currentWriter.newLine()
                    currentWriter.flush()
                    true
                } else {
                    false
                }
            } catch (ioException: IOException) {
                handleConnectionError("Send failed: ${ioException.message}")
                false
            }
        }

    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            readerJob?.cancel()
            readerJob = null

            runCatching {
                writer?.run {
                    flush()
                    close()
                }
            }.onFailure { throwable ->
                AppLogger.e(TAG, "Failed to close writer", throwable)
            }

            runCatching { reader?.close() }.onFailure { throwable ->
                AppLogger.e(TAG, "Failed to close reader", throwable)
            }

            runCatching {
                bluetoothSocket?.let { socket ->
                    if (socket.isConnected) {
                        socket.close()
                    }
                }
            }.onFailure { throwable ->
                AppLogger.e(TAG, "Failed to close Bluetooth socket", throwable)
            }

            writer = null
            reader = null
            bluetoothSocket = null

            _connectionState.value = CommandConnection.ConnectionState.DISCONNECTED
            connectionCallback?.invoke(CommandConnection.ConnectionState.DISCONNECTED)
        }
    }

    override fun isConnected(): Boolean {
        return bluetoothSocket?.isConnected == true &&
                _connectionState.value == CommandConnection.ConnectionState.CONNECTED
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
                    val message = currentReader.readLine()
                    if (message != null) {
                        messageCallback?.invoke(message)
                    } else {
                        break
                    }
                }
            } catch (throwable: Throwable) {
                if (isActive) {
                    handleConnectionError("Reader error: ${throwable.message}")
                }
            } finally {
                if (isActive) {
                    handleConnectionError("PC disconnected")
                }
            }
        }
    }

    private fun handleConnectionError(errorMessage: String) {
        _connectionState.value = CommandConnection.ConnectionState.ERROR
        connectionCallback?.invoke(CommandConnection.ConnectionState.ERROR)
        clientScope.launch {
            disconnect()
        }
    }
}
