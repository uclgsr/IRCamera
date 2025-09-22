package com.mpdc4gsr.ble.core

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import com.mpdc4gsr.ble.core.RequestCallback
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

internal class ConnectionImpl(
    private val easyBle: EasyBLE,
    private val bluetoothAdapter: BluetoothAdapter?,
    override val device: Device,
    configuration: ConnectionConfiguration?,
    connectDelay: Int,
    observer: EventObserver?
) : Connection, ScanListener {
    private val configuration: ConnectionConfiguration? = configuration
    private val requestQueue: MutableList<GenericRequest> = ArrayList<GenericRequest>()
    private val observer: EventObserver? = observer
    private val connHandler: Handler = Handler(Looper.getMainLooper())
    private val logger: Logger = DefaultLogger()
    private val observable: Observable = DefaultObservable()
    private val posterDispatcher: PosterDispatcher = DefaultPosterDispatcher()
    private val gattCallback: BluetoothGattCallback = BleGattCallback()
    private var bluetoothGatt: BluetoothGatt? = null
    private var currentRequest: GenericRequest? = null
    private var isReleased = false
    private var connStartTime: Long = 0L
    private var refreshCount = 0
    private var tryReconnectCount = 0
    private var lastConnectionState: ConnectionState? = null
    private var reconnectImmediatelyCount = 0
    private var refreshing = false
    private var isActiveDisconnect = false
    private var lastScanStopTime: Long = 0
    override var mtu = 23
    private var originCallback: BluetoothGattCallback? = null
    private val connectRunnable: Runnable = object : Runnable {
        override fun run() {
            if (!isReleased && hasBluetoothPermission()) {
                easyBle.stopScan()

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        bluetoothGatt = device.originDevice.connectGatt(
                            easyBle.context, false, gattCallback,
                            configuration!!.transport, configuration.phy
                        )
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        bluetoothGatt = device.originDevice.connectGatt(
                            easyBle.context, false, gattCallback,
                            configuration!!.transport
                        )
                    } else {
                        bluetoothGatt = device.originDevice.connectGatt(easyBle.context, false, gattCallback)
                    }
                } catch (e: SecurityException) {
                    logE(
                        Logger.Companion.TYPE_CONNECTION_STATE,
                        "SecurityException in connectGatt(): %s [name: %s, addr: %s]",
                        e.message,
                        device.name,
                        device.address
                    )
                    if (observer != null) {
                        posterDispatcher.post(
                            observer,
                            MethodInfoGenerator.onConnectFailed(
                                device,
                                Connection.Companion.CONNECT_FAIL_TYPE_NO_PERMISSION
                            )
                        )
                    }
                    observable.notifyObservers(
                        MethodInfoGenerator.onConnectFailed(
                            device,
                            Connection.Companion.CONNECT_FAIL_TYPE_NO_PERMISSION
                        )
                    )
                }
            } else if (!hasBluetoothPermission()) {
                logE(
                    Logger.Companion.TYPE_CONNECTION_STATE,
                    "connect failed! [type: no bluetooth permission, name: %s, addr: %s]",
                    device.name,
                    device.address
                )
                if (observer != null) {
                    posterDispatcher.post(
                        observer,
                        MethodInfoGenerator.onConnectFailed(
                            device,
                            Connection.Companion.CONNECT_FAIL_TYPE_NO_PERMISSION
                        )
                    )
                }
                observable.notifyObservers(
                    MethodInfoGenerator.onConnectFailed(
                        device,
                        Connection.Companion.CONNECT_FAIL_TYPE_NO_PERMISSION
                    )
                )
            }
        }
    }

    init {
        if (configuration == null) {
            this.configuration = ConnectionConfiguration()
        } else {
            this.configuration = configuration
        }
        this.observer = observer
        logger = easyBle.getLogger()
        observable = easyBle.getObservable()
        posterDispatcher = easyBle.getPosterDispatcher()
        connHandler = ConnHandler(this)
        connStartTime = System.currentTimeMillis()
        connHandler.sendEmptyMessageDelayed(MSG_CONNECT, connectDelay.toLong())
        connHandler.sendEmptyMessageDelayed(MSG_TIMER, connectDelay.toLong())
        easyBle.addScanListener(this)
    }

    override fun onScanStart() {
    }

    override fun onScanStop() {
        synchronized(this) {
            lastScanStopTime = System.currentTimeMillis()
        }
    }

    companion object {
        private const val MSG_CONNECT = 1
        private const val MSG_RECONNECT = 2
        private const val MSG_RELEASE = 3
        private const val MSG_PROCESS_REQUEST = 4
    }

    // Required Connection interface properties
    override val connectionState: ConnectionState
        get() = device.connectionState

    // Required Connection interface methods
    override fun getConnectionState(): ConnectionState {
        return device.connectionState
    }

    override val isAutoReconnectEnabled: Boolean
        get() = configuration?.isAutoReconnectEnabled ?: false

    override val gatt: BluetoothGatt?
        get() = bluetoothGatt

    override val connectionConfiguration: ConnectionConfiguration
        get() = configuration ?: ConnectionConfiguration()

    // Required ScanListener interface methods
    override fun onScanStart() {
        // Implementation for scan start
    }

    override fun onScanStop() {
        // Implementation for scan stop
    }

    override fun onScanResult(device: Device, rssi: Int, data: ByteArray) {
        synchronized(this) {
            if (!isReleased && this.device == device && this.device.connectionState == ConnectionState.SCANNING_FOR_RECONNECTION) {
                connHandler.sendEmptyMessage(MSG_CONNECT)
            }
        }
    }

    override fun onScanFailed(reason: Int) {
        // Implementation for scan failed
    }

    override fun onScanComplete() {
        // Implementation for scan complete
    }

    override fun onScanError(errorCode: Int, errorMessage: String) {
        // Implementation for scan error
    }

    private fun hasBluetoothPermission(): Boolean {
        val context = easyBle.context ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun logD(type: String, message: String, vararg args: Any?) {
        logger.d("BLE_CONN", String.format(message, *args))
    }

    private fun logE(type: String, message: String, vararg args: Any?) {
        logger.e("BLE_CONN", String.format(message, *args))
    }

    inner class BleGattCallback : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            doOnConnectionStateChange(status, newState)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            // Handle service discovery
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: android.bluetooth.BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            // Handle characteristic read
        }
    }

    // Connection interface implementations
    override fun reconnect() {
        if (isReleased) return
        device.connectionState = ConnectionState.RECONNECTING
        connHandler.sendEmptyMessage(MSG_RECONNECT)
    }

    override fun disconnect() {
        if (isReleased) return
        isActiveDisconnect = true
        bluetoothGatt?.disconnect()
    }

    override fun refresh() {
        if (bluetoothGatt != null && !refreshing) {
            refreshing = true
            bluetoothGatt!!.refresh()
        }
    }

    override fun release() {
        connHandler.sendEmptyMessage(MSG_RELEASE)
    }

    override fun releaseNoEvent() {
        isReleased = true
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    override fun clearRequestQueue() {
        requestQueue.clear()
        currentRequest = null
    }

    override fun clearRequestQueueByType(type: RequestType?) {
        // Implementation for clearing specific request types
    }

    override fun getService(service: java.util.UUID?): android.bluetooth.BluetoothGattService? {
        return bluetoothGatt?.getService(service)
    }

    override fun getCharacteristic(
        service: java.util.UUID?,
        characteristic: java.util.UUID?
    ): android.bluetooth.BluetoothGattCharacteristic? {
        return getService(service)?.getCharacteristic(characteristic)
    }

    override fun getDescriptor(
        service: java.util.UUID?,
        characteristic: java.util.UUID?,
        descriptor: java.util.UUID?
    ): android.bluetooth.BluetoothGattDescriptor? {
        return getCharacteristic(service, characteristic)?.getDescriptor(descriptor)
    }

    override fun execute(request: Request?) {
        if (request is GenericRequest && !isReleased) {
            synchronized(requestQueue) {
                if (requestQueue.contains(request)) {
                    return
                }
                requestQueue.add(request)
                Collections.sort(requestQueue)
            }
            // Process the request through the handler
            connHandler.sendEmptyMessage(MSG_PROCESS_REQUEST)
        }
    }

    override fun isNotificationOrIndicationEnabled(characteristic: android.bluetooth.BluetoothGattCharacteristic?): Boolean {
        return false // Simplified implementation
    }

    override fun isNotificationOrIndicationEnabled(service: java.util.UUID?, characteristic: java.util.UUID?): Boolean {
        return isNotificationOrIndicationEnabled(getCharacteristic(service, characteristic))
    }

    override fun setBluetoothGattCallback(callback: BluetoothGattCallback?) {
        originCallback = callback
    }

    override fun hasProperty(service: java.util.UUID?, characteristic: java.util.UUID?, property: Int): Boolean {
        val charac = getCharacteristic(service, characteristic)
        return charac?.let { (it.properties and property) != 0 } ?: false
    }

    private fun doOnConnectionStateChange(status: Int, newState: Int) {
        if (bluetoothGatt != null) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    logD("CONNECTION", "connected! [name: %s, addr: %s]", device.name, device.address)
                    device.connectionState = ConnectionState.CONNECTED
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    logD("CONNECTION", "disconnected! [name: %s, addr: %s]", device.name, device.address)
                    device.connectionState = ConnectionState.DISCONNECTED
                }
            } else {
                logE("CONNECTION", "connection failed with status: %d", status)
                device.connectionState = ConnectionState.DISCONNECTED
            }
        }
    }
}
