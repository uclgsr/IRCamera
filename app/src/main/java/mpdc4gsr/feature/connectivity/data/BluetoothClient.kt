package mpdc4gsr.feature.connectivity.data

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mpdc4gsr.core.common.AppLogger
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID

class BluetoothClient(
    private val context: Context,
    private val bluetoothDevice: BluetoothDevice,
    private val serviceUuid: UUID = DEFAULT_SPP_UUID,
) : CommandConnection {

    companion object {
        /** Standard Serial Port Profile UUID. */
        val DEFAULT_SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        private const val TAG = "BluetoothClient"
    }

    private var bluetoothSocket: android.bluetooth.BluetoothSocket? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var readerJob: Job? = null

    private val _connectionState = MutableStateFlow(CommandConnection.ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<CommandConnection.ConnectionState> =
        _connectionState.asStateFlow()

    private var messageCallback: ((String) -> Unit)? = null
    private var connectionCallback: ((CommandConnection.ConnectionState) -> Unit)? = null

    override suspend fun connect(): Boolean =
        withContext(Dispatchers.IO) {
            if (isConnected()) {
                return@withContext true
            }

            try {
                val bluetoothManager =
                    context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val adapter = bluetoothManager?.adapter
                if (bluetoothManager == null || adapter == null || !adapter.isEnabled) {
                    handleConnectionError("Bluetooth not available")
                    return@withContext false
                }

                adapter.cancelDiscovery()

                _connectionState.value = CommandConnection.ConnectionState.CONNECTING
                connectionCallback?.invoke(CommandConnection.ConnectionState.CONNECTING)

                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(serviceUuid)
                bluetoothSocket?.connect()

                val input = bluetoothSocket?.inputStream
                val output = bluetoothSocket?.outputStream
                if (input == null || output == null) {
                    throw IOException("Failed to obtain Bluetooth socket streams")
                }

                reader = BufferedReader(InputStreamReader(input, Charsets.UTF_8))
                writer = BufferedWriter(OutputStreamWriter(output, Charsets.UTF_8))

                _connectionState.value = CommandConnection.ConnectionState.CONNECTED
                connectionCallback?.invoke(CommandConnection.ConnectionState.CONNECTED)

                readerJob = scope.launch { listenForMessages() }
                true
            } catch (e: Exception) {
                handleConnectionError("Connection failed: ${e.message}")
                false
            }
        }

    override suspend fun sendMessage(message: String): Boolean =
        withContext(Dispatchers.IO) {
            val currentWriter = writer
            if (currentWriter == null || !isConnected()) {
                return@withContext false
            }

            try {
                currentWriter.write(message)
                currentWriter.newLine()
                currentWriter.flush()
                true
            } catch (e: IOException) {
                handleConnectionError("Send failed: ${e.message}")
                false
            }
        }

    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            cleanupInternal()
        }
    }

    override fun isConnected(): Boolean =
        bluetoothSocket?.isConnected == true

    override fun setMessageCallback(callback: (String) -> Unit) {
        messageCallback = callback
    }

    override fun setConnectionCallback(callback: (CommandConnection.ConnectionState) -> Unit) {
        connectionCallback = callback
    }

    override fun cleanup() {
        scope.coroutineContext.cancelChildren()
        cleanupInternal()
    }

    private fun handleConnectionError(reason: String) {
        AppLogger.e(TAG, reason)
        _connectionState.value = CommandConnection.ConnectionState.ERROR
        connectionCallback?.invoke(CommandConnection.ConnectionState.ERROR)
        cleanupInternal()
    }

    private fun cleanupInternal() {
        readerJob?.cancel()
        readerJob = null

        try {
            reader?.close()
            writer?.close()
            bluetoothSocket?.close()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error while closing Bluetooth socket", e)
        } finally {
            reader = null
            writer = null
            bluetoothSocket = null
            if (_connectionState.value != CommandConnection.ConnectionState.ERROR) {
                _connectionState.value = CommandConnection.ConnectionState.DISCONNECTED
                connectionCallback?.invoke(CommandConnection.ConnectionState.DISCONNECTED)
            }
        }
    }

    private suspend fun listenForMessages() {
        try {
            while (isConnected()) {
                val line = reader?.readLine() ?: break
                messageCallback?.invoke(line)
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Reader loop failed", e)
        } finally {
            cleanupInternal()
        }
    }
}
