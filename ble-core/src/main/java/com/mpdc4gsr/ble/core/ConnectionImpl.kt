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
import com.mpdc4gsr.ble.core.callback.RequestCallback
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
private fun doOnServicesDiscovered(status: Int) {
    if (bluetoothGatt != null) {
        val services = bluetoothGatt!!.getServices()
        if (status == BluetoothGatt.GATT_SUCCESS) {
            logD(
                Logger.Companion.TYPE_CONNECTION_STATE,
                "services discovered! [name: %s, addr: %s, size: %d]",
                device.name,
                device.address,
                services.size
            )
            if (services.isEmpty()) {
                doClearTaskAndRefresh()
            } else {
                refreshCount = 0
                tryReconnectCount = 0
                reconnectImmediatelyCount = 0
                device.connectionState = ConnectionState.SERVICE_DISCOVERED
                sendConnectionCallback()
            }
        } else {
            doClearTaskAndRefresh()
            logE(
                Logger.Companion.TYPE_CONNECTION_STATE, "GATT error! [status: %d, name: %s, addr: %s]",
                status, device.name, device.address
            )
        }
    }
}


private fun doDiscoverServices() {
    if (bluetoothGatt != null) {
        val context = easyBle.getContext()
        if (!BluetoothPermissionUtils.hasBluetoothConnectPermission(context)) {
            logger.log(
                Log.ERROR, Logger.Companion.TYPE_CONNECTION_STATE,
                "Missing BLUETOOTH_CONNECT permission for discoverServices()"
            )
            notifyDisconnected()
            return
        }

        try {
            bluetoothGatt!!.discoverServices()
            device.connectionState = ConnectionState.SERVICE_DISCOVERING
            sendConnectionCallback()
        } catch (e: SecurityException) {
            logger.log(
                Log.ERROR, Logger.Companion.TYPE_CONNECTION_STATE,
                "SecurityException in discoverServices(): " + e.message
            )
            notifyDisconnected()
        }
    } else {
        notifyDisconnected()
    }
}

private fun doTimer() {
    if (!isReleased) {
        if (device.connectionState != ConnectionState.SERVICE_DISCOVERED && !refreshing && !isActiveDisconnect) {
            if (device.connectionState != ConnectionState.DISCONNECTED) {
                if (System.currentTimeMillis() - connStartTime > configuration!!.connectTimeoutMillis) {
                    connStartTime = System.currentTimeMillis()
                    logE(
                        Logger.Companion.TYPE_CONNECTION_STATE,
                        "connect timeout! [name: %s, addr: %s]",
                        device.name,
                        device.address
                    )
                    val type: Int
                    when (device.connectionState) {
                        ConnectionState.SCANNING_FOR_RECONNECTION -> type =
                            Connection.Companion.TIMEOUT_TYPE_CANNOT_DISCOVER_DEVICE

                        ConnectionState.CONNECTING -> type = Connection.Companion.TIMEOUT_TYPE_CANNOT_CONNECT
                        else -> type = Connection.Companion.TIMEOUT_TYPE_CANNOT_DISCOVER_SERVICES
                    }
                    observable.notifyObservers(MethodInfoGenerator.onConnectTimeout(device, type))
                    if (observer != null) {
                        posterDispatcher.post(observer, MethodInfoGenerator.onConnectTimeout(device, type))
                    }
                    val infinite =
                        configuration.tryReconnectMaxTimes == ConnectionConfiguration.Companion.TRY_RECONNECT_TIMES_INFINITE
                    if (configuration.isAutoReconnect && (infinite || tryReconnectCount < configuration.connectTimeoutMillis)) {
                        doDisconnect(true)
                    } else {
                        doDisconnect(false)
                        if (observer != null) {
                            posterDispatcher.post(
                                observer,
                                MethodInfoGenerator.onConnectFailed(
                                    device,
                                    Connection.Companion.CONNECT_FAIL_TYPE_MAXIMUM_RECONNECTION
                                )
                            )
                        }
                        observable.notifyObservers(
                            MethodInfoGenerator.onConnectFailed(
                                device,
                                Connection.Companion.CONNECT_FAIL_TYPE_MAXIMUM_RECONNECTION
                            )
                        )
                        logE(
                            Logger.Companion.TYPE_CONNECTION_STATE,
                            "connect failed! [type: maximun reconnection, name: %s, addr: %s]",
                            device.name,
                            device.address
                        )
                    }
                }
            } else if (configuration!!.isAutoReconnect) {
                doDisconnect(true)
            }
        }
        connHandler.sendEmptyMessageDelayed(MSG_TIMER, 500)
    }
}

private fun doConnect() {
    cancelRefreshState()
    device.connectionState = ConnectionState.CONNECTING
    sendConnectionCallback()
    logD(Logger.Companion.TYPE_CONNECTION_STATE, "connecting [name: %s, addr: %s]", device.name, device.address)
    connHandler.postDelayed(connectRunnable, 500)
}

private fun doDisconnect(reconnect: Boolean) {
    clearRequestQueueAndNotify()
    connHandler.removeCallbacks(connectRunnable)
    connHandler.removeMessages(MSG_DISCOVER_SERVICES)
    if (bluetoothGatt != null) {
        closeGatt(bluetoothGatt!!)
        bluetoothGatt = null
    }
    device.connectionState = ConnectionState.DISCONNECTED
    if (bluetoothAdapter != null && bluetoothAdapter.isEnabled() && reconnect && !isReleased) {
        if (reconnectImmediatelyCount < configuration!!.reconnectImmediatelyMaxTimes) {
            tryReconnectCount++
            reconnectImmediatelyCount++
            connStartTime = System.currentTimeMillis()
            doConnect()
            return
        } else if (canScanReconnect()) {
            tryScanReconnect()
        }
    }
    sendConnectionCallback()
}

private fun doClearTaskAndRefresh() {
    clearRequestQueueAndNotify()
    doRefresh(true)
}

private fun doRefresh(isAuto: Boolean) {
    logD(Logger.Companion.TYPE_CONNECTION_STATE, "refresh GATT! [name: %s, addr: %s]", device.name, device.address)
    connStartTime = System.currentTimeMillis()
    if (bluetoothGatt != null) {
        val context = easyBle.getContext()
        if (BluetoothPermissionUtils.hasBluetoothConnectPermission(context)) {
            try {
                bluetoothGatt!!.disconnect()
            } catch (e: SecurityException) {
                logE(Logger.Companion.TYPE_CONNECTION_STATE, "SecurityException in disconnect(): %s", e.message)
            } catch (ignore: Exception) {
            }
        } else {
            logger.log(
                Log.WARN,
                Logger.Companion.TYPE_CONNECTION_STATE,
                "Missing BLUETOOTH_CONNECT permission for disconnect()"
            )
        }

        if (isAuto) {
            if (refreshCount <= 5) {
                refreshing = doRefresh()
            }
            refreshCount++
        } else {
            refreshing = doRefresh()
        }
        if (refreshing) {
            connHandler.postDelayed(Runnable { this.cancelRefreshState() }, 2000)
        } else if (bluetoothGatt != null) {
            closeGatt(bluetoothGatt!!)
            bluetoothGatt = null
        }
    }
    notifyDisconnected()
}

private fun cancelRefreshState() {
    if (refreshing) {
        refreshing = false
        if (bluetoothGatt != null) {
            closeGatt(bluetoothGatt!!)
            bluetoothGatt = null
        }
    }
}

private fun tryScanReconnect() {
    if (!isReleased) {
        connStartTime = System.currentTimeMillis()
        easyBle.stopScan()

        device.connectionState = ConnectionState.SCANNING_FOR_RECONNECTION
        logD(
            Logger.Companion.TYPE_CONNECTION_STATE,
            "scanning for reconnection [name: %s, addr: %s]",
            device.name,
            device.address
        )
        easyBle.startScan()
    }
}

private fun canScanReconnect(): Boolean {
    val duration = System.currentTimeMillis() - lastScanStopTime
    val parameters = configuration!!.scanIntervalPairsInAutoReconnection
    Collections.sort<Pair<Int?, Int?>?>(parameters, Comparator { o1: Pair<Int?, Int?>?, o2: Pair<Int?, Int?>? ->
        if (o1 == null || o1.first == null) return@sort 1.toBoolean()
        if (o2 == null || o2.first == null) return@sort -1.toBoolean()
        o2.first!!.compareTo(o1.first!!)
    })
    for (pair in parameters) {
        if (pair.first != null && pair.second != null && tryReconnectCount >= pair.first!! && duration >= pair.second!!) {
            return true
        }
    }
    return false
}

private fun hasBluetoothPermission(): Boolean {
    val context = easyBle.getContext()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        return ContextCompat.checkSelfPermission(
            context!!,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        return ContextCompat.checkSelfPermission(
            context!!,
            Manifest.permission.BLUETOOTH
        ) == PackageManager.PERMISSION_GRANTED
    }
}

private fun closeGatt(gatt: BluetoothGatt) {
    val context = easyBle.getContext()
    if (BluetoothPermissionUtils.hasBluetoothConnectPermission(context)) {
        try {
            gatt.disconnect()
        } catch (e: SecurityException) {
            logger.log(
                Log.WARN,
                Logger.Companion.TYPE_CONNECTION_STATE,
                "SecurityException in disconnect(): " + e.message
            )
        } catch (ignore: Exception) {
        }
        try {
            gatt.close()
        } catch (e: SecurityException) {
            logger.log(
                Log.WARN,
                Logger.Companion.TYPE_CONNECTION_STATE,
                "SecurityException in close(): " + e.message
            )
        } catch (ignore: Exception) {
        }
    } else {
        logger.log(
            Log.WARN,
            Logger.Companion.TYPE_CONNECTION_STATE,
            "Missing BLUETOOTH_CONNECT permission for closeGatt()"
        )
    }
}

private fun notifyDisconnected() {
    device.connectionState = ConnectionState.DISCONNECTED
    sendConnectionCallback()
}

private fun sendConnectionCallback() {
    if (lastConnectionState != device.connectionState) {
        lastConnectionState = device.connectionState
        if (observer != null) {
            posterDispatcher.post(observer, MethodInfoGenerator.onConnectionStateChanged(device))
        }
        observable.notifyObservers(MethodInfoGenerator.onConnectionStateChanged(device))
    }
}

private fun write(
    request: GenericRequest,
    characteristic: BluetoothGattCharacteristic,
    value: ByteArray?
): Boolean {
    if (!hasBluetoothPermission()) {
        handleFailedCallback(request, Connection.Companion.REQUEST_FAIL_TYPE_NO_PERMISSION, true)
        return false
    }
    characteristic.setValue(value)
    val writeType = request.writeOptions.writeType
    if ((writeType == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE || writeType == BluetoothGattCharacteristic.WRITE_TYPE_SIGNED || writeType == BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)) {
        characteristic.setWriteType(writeType)
    }
    if (bluetoothGatt == null) {
        handleFailedCallback(request, Connection.Companion.REQUEST_FAIL_TYPE_GATT_IS_NULL, true)
        return false
    }
    if (!BluetoothPermissionUtils.hasBluetoothConnectPermission(EasyBLE.Companion.getInstance().getContext())) {
        handleFailedCallback(request, ScanListener.Companion.ERROR_LACK_BLUETOOTH_PERMISSION, true)
        return false
    }
    try {
        if (!bluetoothGatt!!.writeCharacteristic(characteristic)) {
            handleWriteFailed(request)
            return false
        }
    } catch (e: SecurityException) {
        handleFailedCallback(request, ScanListener.Companion.ERROR_LACK_BLUETOOTH_PERMISSION, true)
        return false
    }
    return true
}

private fun handleWriteFailed(request: GenericRequest) {
    connHandler.removeMessages(MSG_REQUEST_TIMEOUT)
    request.remainQueue = null
    handleFailedCallback(request, Connection.Companion.REQUEST_FAIL_TYPE_REQUEST_FAILED, true)
}

private fun enableNotificationOrIndicationFail(
    enable: Boolean,
    notification: Boolean,
    characteristic: BluetoothGattCharacteristic
): Boolean {
    if (!BluetoothPermissionUtils.hasBluetoothConnectPermission(
            EasyBLE.Companion.getInstance().getContext()
        ) || !bluetoothAdapter!!.isEnabled() || bluetoothGatt == null
    ) {
        return true
    }

    try {
        if (!bluetoothGatt!!.setCharacteristicNotification(characteristic, enable)) {
            return true
        }
    } catch (e: SecurityException) {
        return true
    }
    val descriptor = characteristic.getDescriptor(Connection.Companion.clientCharacteristicConfig)
    if (descriptor == null) {
        return true
    }
    val originValue = descriptor.getValue()
    if (currentRequest != null) {
        if (currentRequest!!.type == RequestType.SET_NOTIFICATION || currentRequest!!.type == RequestType.SET_INDICATION) {
            currentRequest!!.descriptorTemp = originValue
        }
    }
    if (enable) {
        if (notification) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        } else {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
        }
    } else {
        descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
    }


    val writeType = characteristic.getWriteType()
    characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
    if (!BluetoothPermissionUtils.hasBluetoothConnectPermission(EasyBLE.Companion.getInstance().getContext())) {
        return true
    }

    try {
        val result = bluetoothGatt!!.writeDescriptor(descriptor)
        if (!enable) {
            descriptor.setValue(originValue)
        }
        characteristic.setWriteType(writeType)
        return !result
    } catch (e: SecurityException) {
        return true
    }
}

private fun enqueue(request: GenericRequest) {
    if (isReleased) {
        handleFailedCallback(request, Connection.Companion.REQUEST_FAIL_TYPE_CONNECTION_RELEASED, false)
    } else {
        synchronized(this) {
            if (currentRequest == null) {
                executeRequest(request)
            } else {
                var index = -1
                for (i in requestQueue.indices) {
                    val req = requestQueue.get(i)
                    if (req.priority >= request.priority) {
                        if (i < requestQueue.size - 1) {
                            if (requestQueue.get(i + 1).priority < request.priority) {
                                index = i + 1
                                break
                            }
                        } else {
                            index = i + 1
                        }
                    }
                }
                if (index == -1) {
                    requestQueue.add(0, request)
                } else if (index >= requestQueue.size) {
                    requestQueue.add(request)
                } else {
                    requestQueue.add(index, request)
                }
            }
        }
    }
}

private fun executeNextRequest() {
    synchronized(this) {
        connHandler.removeMessages(MSG_REQUEST_TIMEOUT)
        if (requestQueue.isEmpty()) {
            currentRequest = null
        } else {
            executeRequest(requestQueue.removeAt(0))
        }
    }
}

private fun executeRequest(request: GenericRequest) {
    currentRequest = request
    connHandler.sendMessageDelayed(
        Message.obtain(connHandler, MSG_REQUEST_TIMEOUT, request),
        configuration!!.requestTimeoutMillis.toLong()
    )
    if (bluetoothAdapter!!.isEnabled()) {
        if (bluetoothGatt != null) {
            when (request.type) {
                RequestType.READ_RSSI -> {
                    if (!BluetoothPermissionUtils.hasBluetoothConnectPermission(
                            EasyBLE.Companion.getInstance().getContext()
                        )
                    ) {
                        handleFailedCallback(request, ScanListener.Companion.ERROR_LACK_BLUETOOTH_PERMISSION, true)
                        return
                    }
                    try {
                        if (!bluetoothGatt!!.readRemoteRssi()) {
                            handleFailedCallback(
                                request,
                                Connection.Companion.REQUEST_FAIL_TYPE_REQUEST_FAILED,
                                true
                            )
                        }
                    } catch (e: SecurityException) {
                        handleFailedCallback(request, ScanListener.Companion.ERROR_LACK_BLUETOOTH_PERMISSION, true)
                    }
                }

                RequestType.CHANGE_MTU -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (!BluetoothPermissionUtils.hasBluetoothConnectPermission(
                            EasyBLE.Companion.getInstance().getContext()
                        )
                    ) {
                        handleFailedCallback(request, ScanListener.Companion.ERROR_LACK_BLUETOOTH_PERMISSION, true)
                        return
                    }
                    try {
                        if (!bluetoothGatt!!.requestMtu(request.value as Int)) {
                            handleFailedCallback(
                                request,
                                Connection.Companion.REQUEST_FAIL_TYPE_REQUEST_FAILED,
                                true
                            )
                        }
                    } catch (e: SecurityException) {
                        handleFailedCallback(request, ScanListener.Companion.ERROR_LACK_BLUETOOTH_PERMISSION, true)
                    }
                }

                RequestType.READ_PHY -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (!BluetoothPermissionUtils.hasBluetoothConnectPermission(
                            EasyBLE.Companion.getInstance().getContext()
                        )
                    ) {
                        handleFailedCallback(request, ScanListener.Companion.ERROR_LACK_BLUETOOTH_PERMISSION, true)
                        return
                    }
                    try {
                        bluetoothGatt!!.readPhy()
                    } catch (e: SecurityException) {
                        handleFailedCallback(request, ScanListener.Companion.ERROR_LACK_BLUETOOTH_PERMISSION, true)
                    }
                }

                RequestType.SET_PREFERRED_PHY -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (!BluetoothPermissionUtils.hasBluetoothConnectPermission(
                            EasyBLE.Companion.getInstance().getContext()
                        )
                    ) {
                        handleFailedCallback(request, ScanListener.Companion.ERROR_LACK_BLUETOOTH_PERMISSION, true)
                        return
                    }
                    try {
                        val options = request.value as IntArray
                        bluetoothGatt!!.setPreferredPhy(options[0], options[1], options[2])
                    } catch (e: SecurityException) {
                        handleFailedCallback(request, ScanListener.Companion.ERROR_LACK_BLUETOOTH_PERMISSION, true)
                    }
                }

                else -> {
                    val gattService = bluetoothGatt!!.getService(request.service)
                    if (gattService != null) {
                        val characteristic = gattService.getCharacteristic(request.characteristic)
                        if (characteristic != null) {
                            when (request.type) {
                                RequestType.SET_NOTIFICATION, RequestType.SET_INDICATION -> executeIndicationOrNotification(
                                    request,
                                    characteristic
                                )

                                RequestType.READ_CHARACTERISTIC -> executeReadCharacteristic(
                                    request,
                                    characteristic
                                )

                                RequestType.READ_DESCRIPTOR -> executeReadDescriptor(request, characteristic)
                                RequestType.WRITE_CHARACTERISTIC -> executeWriteCharacteristic(
                                    request,
                                    characteristic
                                )
                            }
                        } else {
                            handleFailedCallback(
                                request,
                                Connection.Companion.REQUEST_FAIL_TYPE_CHARACTERISTIC_NOT_EXIST,
                                true
                            )
                        }
                    } else {
                        handleFailedCallback(
                            request,
                            Connection.Companion.REQUEST_FAIL_TYPE_SERVICE_NOT_EXIST,
                            true
                        )
                    }
                }
            }
        } else {
            handleFailedCallback(request, Connection.Companion.REQUEST_FAIL_TYPE_GATT_IS_NULL, true)
        }
    } else {
        handleFailedCallback(request, Connection.Companion.REQUEST_FAIL_TYPE_BLUETOOTH_ADAPTER_DISABLED, true)
    }
}

private fun printWriteLog(request: GenericRequest, progress: Int, total: Int, value: ByteArray?) {
    if (logger.isEnabled()) {
        val t = total.toString()
        val sb = StringBuilder(progress.toString())
        while (sb.length < t.length) {
            sb.insert(0, "0")
        }
        logD(
            Logger.Companion.TYPE_CHARACTERISTIC_WRITE,
            "package [%s/%s] write success! [UUID: %s, addr: %s, value: %s]",
            sb,
            t,
            substringUuid(request.characteristic),
            device.address,
            toHex(value)
        )
    }
}

private fun executeWriteCharacteristic(request: GenericRequest, characteristic: BluetoothGattCharacteristic) {
    try {
        val value = request.value as ByteArray
        val options = request.writeOptions
        val reqDelay =
            if (options.requestWriteDelayMillis > 0) options.requestWriteDelayMillis else options.packageWriteDelayMillis
        if (reqDelay > 0) {
            try {
                Thread.sleep(reqDelay.toLong())
            } catch (ignore: InterruptedException) {
            }
            if (request !== currentRequest) {
                return
            }
        }
        if (options.useMtuAsPackageSize) {
            options.packageSize = mtu - 3
        }
        if (value.size > options.packageSize) {
            val list = MathUtils.splitPackage(value, options.packageSize)
            if (!options.isWaitWriteResult) {
                val delay = options.packageWriteDelayMillis
                for (i in list.indices) {
                    val bytes = list.get(i)
                    if (i > 0 && delay > 0) {
                        try {
                            Thread.sleep(delay.toLong())
                        } catch (ignore: InterruptedException) {
                        }
                        if (request !== currentRequest) {
                            return
                        }
                    }
                    if (!write(request, characteristic, bytes)) {
                        return
                    } else {
                        printWriteLog(request, i + 1, list.size, bytes)
                    }
                }
                printWriteLog(request, list.size, list.size, list.get(list.size - 1))
            } else {
                request.remainQueue = ConcurrentLinkedQueue<ByteArray?>()
                request.remainQueue.addAll(list)
                request.sendingBytes = request.remainQueue.remove()
                write(request, characteristic, request.sendingBytes)
            }
        } else {
            request.sendingBytes = value
            if (write(request, characteristic, value)) {
                if (!options.isWaitWriteResult) {
                    notifyCharacteristicWrite(request, value)
                    printWriteLog(request, 1, 1, value)
                    executeNextRequest()
                }
            }
        }
    } catch (e: Exception) {
        handleWriteFailed(request)
    }
}

private fun executeReadDescriptor(request: GenericRequest, characteristic: BluetoothGattCharacteristic) {
    val gattDescriptor = characteristic.getDescriptor(request.descriptor)
    if (gattDescriptor != null) {
        if (!BluetoothPermissionUtils.hasBluetoothConnectPermission(EasyBLE.Companion.getInstance().getContext())) {
            handleFailedCallback(request, ScanListener.Companion.ERROR_LACK_BLUETOOTH_PERMISSION, true)
            return
        }
        try {
            if (!bluetoothGatt!!.readDescriptor(gattDescriptor)) {
                handleFailedCallback(request, Connection.Companion.REQUEST_FAIL_TYPE_REQUEST_FAILED, true)
            }
        } catch (e: SecurityException) {
            handleFailedCallback(request, ScanListener.Companion.ERROR_LACK_BLUETOOTH_PERMISSION, true)
        }
    } else {
        handleFailedCallback(request, Connection.Companion.REQUEST_FAIL_TYPE_DESCRIPTOR_NOT_EXIST, true)
    }
}

private fun executeReadCharacteristic(request: GenericRequest, characteristic: BluetoothGattCharacteristic?) {
    if (!BluetoothPermissionUtils.hasBluetoothConnectPermission(EasyBLE.Companion.getInstance().getContext())) {
        handleFailedCallback(request, ScanListener.Companion.ERROR_LACK_BLUETOOTH_PERMISSION, true)
        return
    }
    try {
        if (!bluetoothGatt!!.readCharacteristic(characteristic)) {
            handleFailedCallback(request, Connection.Companion.REQUEST_FAIL_TYPE_REQUEST_FAILED, true)
        }
    } catch (e: SecurityException) {
        handleFailedCallback(request, ScanListener.Companion.ERROR_LACK_BLUETOOTH_PERMISSION, true)
    }
}

private fun executeIndicationOrNotification(request: GenericRequest, characteristic: BluetoothGattCharacteristic) {
    if (enableNotificationOrIndicationFail(
            (request.value as Int) == 1,
            request.type == RequestType.SET_NOTIFICATION, characteristic
        )
    ) {
        handleGattStatusFailed()
    }
}

private fun handlePhyChange(read: Boolean, txPhy: Int, rxPhy: Int, status: Int) {
    if (currentRequest != null) {
        if ((read && currentRequest!!.type == RequestType.READ_PHY) || ((!read && currentRequest!!.type == RequestType.SET_PREFERRED_PHY))) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                notifyPhyChange(currentRequest!!, txPhy, rxPhy)
            } else {
                handleGattStatusFailed()
            }
            executeNextRequest()
        }
    }
}

private fun handleGattStatusFailed() {
    if (currentRequest != null) {
        handleFailedCallback(currentRequest!!, Connection.Companion.REQUEST_FAIL_TYPE_GATT_STATUS_FAILED, false)
    }
}

private fun handleFailedCallback(request: GenericRequest, failType: Int, executeNext: Boolean) {
    notifyRequestFailed(request, failType)
    if (executeNext) {
        executeNextRequest()
    }
}

private fun toHex(bytes: ByteArray?): String? {
    return StringUtils.toHex(bytes)
}

private fun substringUuid(uuid: UUID?): String {
    return if (uuid == null) "null" else uuid.toString().substring(0, 8)
}

private fun handleCallbacks(callback: RequestCallback?, info: MethodInfo) {
    if (observer != null) {
        posterDispatcher.post(observer, info)
    }
    if (callback != null) {
        posterDispatcher.post(callback, info)
    } else {
        observable.notifyObservers(info)
    }
}

private fun log(priority: Int, type: Int, format: String, vararg args: Any?) {
    logger.log(priority, type, String.format(Locale.US, format, *args))
}

private fun logE(type: Int, format: String, vararg args: Any?) {
    log(Log.ERROR, type, format, *args)
}

private fun logD(type: Int, format: String, vararg args: Any?) {
    log(Log.DEBUG, type, format, *args)
}

private fun notifyRequestFailed(request: GenericRequest, failType: Int) {
    val info = MethodInfoGenerator.onRequestFailed(request, failType, request.value)
    handleCallbacks(request.callback, info)
    logE(
        Logger.Companion.TYPE_REQUEST_FAILED, "request failed! [requestType: %s, addr: %s, failType: %d",
        request.type, device.address, failType
    )
}

private fun notifyCharacteristicRead(request: GenericRequest, value: ByteArray?) {
    val info = MethodInfoGenerator.onCharacteristicRead(request, value)
    handleCallbacks(request.callback, info)
    logD(
        Logger.Companion.TYPE_CHARACTERISTIC_READ, "characteristic read! [UUID: %s, addr: %s, value: %s]",
        substringUuid(request.characteristic), device.address, toHex(value)
    )
}

private fun notifyCharacteristicChanged(characteristic: BluetoothGattCharacteristic) {
    val info = MethodInfoGenerator.onCharacteristicChanged(
        device, characteristic.getService().getUuid(),
        characteristic.getUuid(), characteristic.getValue()
    )
    observable.notifyObservers(info)
    if (observer != null) {
        posterDispatcher.post(observer, info)
    }
    logD(
        Logger.Companion.TYPE_CHARACTERISTIC_CHANGED, "characteristic change! [UUID: %s, addr: %s, value: %s]",
        substringUuid(characteristic.getUuid()), device.address, toHex(characteristic.getValue())
    )
}

private fun notifyRssiRead(request: GenericRequest, rssi: Int) {
    val info = MethodInfoGenerator.onRssiRead(request, rssi)
    handleCallbacks(request.callback, info)
    logD(Logger.Companion.TYPE_READ_REMOTE_RSSI, "rssi read! [addr: %s, rssi: %d]", device.address, rssi)
}

private fun notifyMtuChanged(request: GenericRequest, mtu: Int) {
    val info = MethodInfoGenerator.onMtuChanged(request, mtu)
    handleCallbacks(request.callback, info)
    logD(Logger.Companion.TYPE_MTU_CHANGED, "mtu change! [addr: %s, mtu: %d]", device.address, mtu)
}

private fun notifyDescriptorRead(request: GenericRequest, value: ByteArray?) {
    val info = MethodInfoGenerator.onDescriptorRead(request, value)
    handleCallbacks(request.callback, info)
    logD(
        Logger.Companion.TYPE_DESCRIPTOR_READ, "descriptor read! [UUID: %s, addr: %s, value: %s]",
        substringUuid(request.characteristic), device.address, toHex(value)
    )
}

private fun notifyNotificationChanged(request: GenericRequest, isEnabled: Boolean) {
    val info = MethodInfoGenerator.onNotificationChanged(request, isEnabled)
    handleCallbacks(request.callback, info)
    if (request.type == RequestType.SET_NOTIFICATION) {
        logD(
            Logger.Companion.TYPE_NOTIFICATION_CHANGED,
            "%s [UUID: %s, addr: %s]",
            if (isEnabled) "notification enabled!" else "notification disabled!",
            substringUuid(request.characteristic),
            device.address
        )
    } else {
        logD(
            Logger.Companion.TYPE_INDICATION_CHANGED,
            "%s [UUID: %s, addr: %s]",
            if (isEnabled) "indication enabled!" else "indication disabled!",
            substringUuid(request.characteristic),
            device.address
        )
    }
}

private fun notifyCharacteristicWrite(request: GenericRequest, value: ByteArray?) {
    val info = MethodInfoGenerator.onCharacteristicWrite(request, value)
    handleCallbacks(request.callback, info)
}

private fun notifyPhyChange(request: GenericRequest, txPhy: Int, rxPhy: Int) {
    val info = MethodInfoGenerator.onPhyChange(request, txPhy, rxPhy)
    handleCallbacks(request.callback, info)
    val event = if (request.type == RequestType.READ_PHY) "phy read!" else "phy update!"
    logD(
        Logger.Companion.TYPE_PHY_CHANGE,
        "%s [addr: %s, tvPhy: %s, rxPhy: %s]",
        event,
        device.address,
        txPhy,
        rxPhy
    )
}

override fun getMtu(): Int {
    return mtu
}

override fun getDevice(): Device {
    return device
}

override fun reconnect() {
    if (!isReleased) {
        isActiveDisconnect = false
        tryReconnectCount = 0
        reconnectImmediatelyCount = 0
        Message.obtain(connHandler, MSG_DISCONNECT, MSG_ARG_RECONNECT, 0).sendToTarget()
    }
}

override fun disconnect() {
    if (!isReleased) {
        isActiveDisconnect = true
        Message.obtain(connHandler, MSG_DISCONNECT, MSG_ARG_NONE, 0).sendToTarget()
    }
}

private fun doRefresh(): Boolean {
    try {
        val localMethod = bluetoothGatt!!.javaClass.getMethod("refresh")
        return localMethod.invoke(bluetoothGatt) as Boolean
    } catch (ignore: Exception) {
    }
    return false
}

override fun refresh() {
    connHandler.sendEmptyMessage(MSG_REFRESH)
}

private fun release(noEvent: Boolean) {
    if (!isReleased) {
        isReleased = true
        configuration!!.setAutoReconnect(false)
        connHandler.removeCallbacksAndMessages(null)
        easyBle.removeScanListener(this)
        clearRequestQueueAndNotify()
        if (bluetoothGatt != null) {
            closeGatt(bluetoothGatt!!)
            bluetoothGatt = null
        }
        device.connectionState = ConnectionState.RELEASED
        logD(
            Logger.Companion.TYPE_CONNECTION_STATE,
            "connection released! [name: %s, addr: %s]",
            device.name,
            device.address
        )
        if (!noEvent) {
            sendConnectionCallback()
        }
        easyBle.releaseConnection(device)
    }
}

override fun release() {
    release(false)
}

override fun releaseNoEvent() {
    release(true)
}

override fun getConnectionState(): ConnectionState {
    return device.connectionState
}

override fun isAutoReconnectEnabled(): Boolean {
    return configuration!!.isAutoReconnect
}

override fun getGatt(): BluetoothGatt? {
    return bluetoothGatt
}

override fun clearRequestQueue() {
    synchronized(this) {
        requestQueue.clear()
        currentRequest = null
    }
}

override fun clearRequestQueueByType(type: RequestType?) {
    synchronized(this) {
        val it = requestQueue.iterator()
        while (it.hasNext()) {
            val request = it.next()
            if (request.type == type) {
                it.remove()
            }
        }
        if (currentRequest != null && currentRequest!!.type == type) {
            currentRequest = null
        }
    }
}

private fun clearRequestQueueAndNotify() {
    synchronized(this) {
        for (request in requestQueue) {
            handleFailedCallback(request, Connection.Companion.REQUEST_FAIL_TYPE_CONNECTION_DISCONNECTED, false)
        }
        if (currentRequest != null) {
            handleFailedCallback(
                currentRequest!!,
                Connection.Companion.REQUEST_FAIL_TYPE_CONNECTION_DISCONNECTED,
                false
            )
        }
    }
    clearRequestQueue()
}

override fun getConnectionConfiguration(): ConnectionConfiguration {
    return configuration!!
}

override fun getService(service: UUID?): BluetoothGattService? {
    if (service != null && bluetoothGatt != null) {
        return bluetoothGatt!!.getService(service)
    }
    return null
}

override fun getCharacteristic(service: UUID?, characteristic: UUID?): BluetoothGattCharacteristic? {
    if (service != null && characteristic != null && bluetoothGatt != null) {
        val gattService = bluetoothGatt!!.getService(service)
        if (gattService != null) {
            return gattService.getCharacteristic(characteristic)
        }
    }
    return null
}

override fun getDescriptor(service: UUID?, characteristic: UUID?, descriptor: UUID?): BluetoothGattDescriptor? {
    if (service != null && characteristic != null && descriptor != null && bluetoothGatt != null) {
        val gattService = bluetoothGatt!!.getService(service)
        if (gattService != null) {
            val gattCharacteristic = gattService.getCharacteristic(characteristic)
            if (gattCharacteristic != null) {
                return gattCharacteristic.getDescriptor(descriptor)
            }
        }
    }
    return null
}

private fun checkUuidExistsAndEnqueue(request: GenericRequest, uuidNum: Int) {
    var exists = false
    if (uuidNum > 2) {
        exists = checkDescriptorExists(request, request.service, request.characteristic, request.descriptor)
    } else if (uuidNum > 1) {
        exists = checkCharacteristicExists(request, request.service, request.characteristic)
    } else if (uuidNum == 1) {
        exists = checkServiceExists(request, request.service)
    }
    if (exists) {
        enqueue(request)
    }
}

private fun checkServiceExists(request: GenericRequest, uuid: UUID?): Boolean {
    if (getService(uuid) == null) {
        handleFailedCallback(request, Connection.Companion.REQUEST_FAIL_TYPE_SERVICE_NOT_EXIST, false)
        return false
    }
    return true
}

private fun checkCharacteristicExists(request: GenericRequest, service: UUID?, characteristic: UUID?): Boolean {
    if (checkServiceExists(request, service)) {
        if (getCharacteristic(service, characteristic) == null) {
            handleFailedCallback(request, Connection.Companion.REQUEST_FAIL_TYPE_CHARACTERISTIC_NOT_EXIST, false)
            return false
        }
        return true
    }
    return false
}

private fun checkDescriptorExists(
    request: GenericRequest,
    service: UUID?,
    characteristic: UUID?,
    descriptor: UUID?
): Boolean {
    if (checkServiceExists(request, service) && checkCharacteristicExists(request, service, characteristic)) {
        if (getDescriptor(service, characteristic, descriptor) == null) {
            handleFailedCallback(request, Connection.Companion.REQUEST_FAIL_TYPE_DESCRIPTOR_NOT_EXIST, false)
            return false
        }
        return true
    }
    return false
}

override fun execute(request: Request?) {
    if (request is GenericRequest) {
        val req = request
        req.setDevice(device)
        when (req.type) {
            RequestType.SET_NOTIFICATION, RequestType.SET_INDICATION, RequestType.READ_CHARACTERISTIC, RequestType.WRITE_CHARACTERISTIC -> {
                if (req.type == RequestType.WRITE_CHARACTERISTIC && req.writeOptions == null) {
                    req.writeOptions = configuration!!.getDefaultWriteOptions(req.service, req.characteristic)
                    if (req.writeOptions == null) {
                        req.writeOptions = WriteOptions.Builder().build()
                    }
                }
                checkUuidExistsAndEnqueue(req, 2)
            }

            RequestType.READ_DESCRIPTOR -> checkUuidExistsAndEnqueue(req, 3)
            else -> enqueue(req)
        }
    }
}

override fun isNotificationOrIndicationEnabled(characteristic: BluetoothGattCharacteristic?): Boolean {
    Inspector.requireNonNull<BluetoothGattCharacteristic?>(characteristic, "characteristic can't be null")
    val descriptor = characteristic!!.getDescriptor(Connection.Companion.clientCharacteristicConfig)
    return descriptor != null && (descriptor.getValue()
        .contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) || descriptor.getValue()
        .contentEquals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE))
}

override fun isNotificationOrIndicationEnabled(service: UUID?, characteristic: UUID?): Boolean {
    val c = getCharacteristic(service, characteristic)
    if (c != null) {
        return isNotificationOrIndicationEnabled(c)
    }
    return false
}

private class ConnHandler(connection: ConnectionImpl?) : Handler(Looper.getMainLooper()) {
    private val weakRef: WeakReference<ConnectionImpl?>

    init {
        weakRef = WeakReference<ConnectionImpl?>(connection)
    }

    override fun handleMessage(msg: Message) {
        val connection = weakRef.get()
        if (connection != null) {
            if (connection.isReleased) {
                return
            }
            when (msg.what) {
                MSG_REQUEST_TIMEOUT -> {
                    val request = msg.obj as GenericRequest?
                    if (connection.currentRequest != null && connection.currentRequest === request) {
                        connection.handleFailedCallback(
                            request!!,
                            Connection.Companion.REQUEST_FAIL_TYPE_REQUEST_TIMEOUT,
                            false
                        )
                        connection.executeNextRequest()
                    }
                }

                MSG_CONNECT -> if (connection.bluetoothAdapter!!.isEnabled()) {
                    connection.doConnect()
                }

                MSG_DISCONNECT -> {
                    val reconnect = msg.arg1 == MSG_ARG_RECONNECT && connection.bluetoothAdapter!!.isEnabled()
                    connection.doDisconnect(reconnect)
                }

                MSG_REFRESH -> connection.doRefresh(false)
                MSG_TIMER -> connection.doTimer()
                MSG_DISCOVER_SERVICES, MSG_ON_CONNECTION_STATE_CHANGE, MSG_ON_SERVICES_DISCOVERED -> if (connection.bluetoothAdapter!!.isEnabled()) {
                    if (msg.what == MSG_DISCOVER_SERVICES) {
                        connection.doDiscoverServices()
                    } else if (msg.what == MSG_ON_SERVICES_DISCOVERED) {
                        connection.doOnServicesDiscovered(msg.arg1)
                    } else {
                        connection.doOnConnectionStateChange(msg.arg1, msg.arg2)
                    }
                }
            }
        }
    }
}

private inner class BleGattCallback : BluetoothGattCallback() {
    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        if (originCallback != null) {
            easyBle.getExecutorService()
                .execute(Runnable { originCallback!!.onConnectionStateChange(gatt, status, newState) })
        }
        if (!isReleased) {
            Message.obtain(connHandler, MSG_ON_CONNECTION_STATE_CHANGE, status, newState).sendToTarget()
        } else {
            closeGatt(gatt)
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        if (originCallback != null) {
            easyBle.getExecutorService().execute(Runnable { originCallback!!.onServicesDiscovered(gatt, status) })
        }
        if (!isReleased) {
            Message.obtain(connHandler, MSG_ON_SERVICES_DISCOVERED, status, 0).sendToTarget()
        } else {
            closeGatt(gatt)
        }
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        Log.e(
            "bcf",
            "onCharacteristicRead  status: " + status + "  value: " + HexUtil.bytesToHexString(characteristic.getValue())
        )
        if (originCallback != null) {
            easyBle.getExecutorService()
                .execute(Runnable { originCallback!!.onCharacteristicRead(gatt, characteristic, status) })
        }
        if (currentRequest != null) {
            if (currentRequest!!.type == RequestType.READ_CHARACTERISTIC) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    notifyCharacteristicRead(currentRequest!!, characteristic.getValue())
                } else {
                    handleGattStatusFailed()
                }
                executeNextRequest()
            }
        }
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        if (originCallback != null) {
            easyBle.getExecutorService()
                .execute(Runnable { originCallback!!.onCharacteristicWrite(gatt, characteristic, status) })
        }
        if (currentRequest != null && currentRequest!!.type == RequestType.WRITE_CHARACTERISTIC &&
            currentRequest!!.writeOptions.isWaitWriteResult
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (logger.isEnabled()) {
                    val data = currentRequest!!.value as ByteArray
                    val packageSize = currentRequest!!.writeOptions.packageSize
                    val total = data.size / packageSize + (if (data.size % packageSize == 0) 0 else 1)
                    val progress: Int
                    if (currentRequest!!.remainQueue == null || currentRequest!!.remainQueue.isEmpty()) {
                        progress = total
                    } else {
                        progress = data.size / packageSize - currentRequest!!.remainQueue.size + 1
                    }
                    printWriteLog(currentRequest!!, progress, total, characteristic.getValue())
                }
                if (currentRequest!!.remainQueue == null || currentRequest!!.remainQueue.isEmpty()) {
                    notifyCharacteristicWrite(currentRequest!!, currentRequest!!.value as ByteArray?)
                    executeNextRequest()
                } else {
                    connHandler.removeMessages(MSG_REQUEST_TIMEOUT)
                    connHandler.sendMessageDelayed(
                        Message.obtain(connHandler, MSG_REQUEST_TIMEOUT, currentRequest),
                        configuration!!.requestTimeoutMillis.toLong()
                    )
                    val req = currentRequest
                    val delay = currentRequest!!.writeOptions.packageWriteDelayMillis
                    if (delay > 0) {
                        try {
                            Thread.sleep(delay.toLong())
                        } catch (ignore: InterruptedException) {
                        }
                        if (req !== currentRequest) {
                            return
                        }
                    }
                    req!!.sendingBytes = req.remainQueue.remove()
                    write(req, characteristic, req.sendingBytes)
                }
            } else {
                handleFailedCallback(
                    currentRequest!!,
                    Connection.Companion.REQUEST_FAIL_TYPE_GATT_STATUS_FAILED,
                    true
                )
            }
        }
    }

    override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic) {
        if (originCallback != null) {
            easyBle.getExecutorService()
                .execute(Runnable { originCallback!!.onCharacteristicChanged(gatt, characteristic) })
        }
        notifyCharacteristicChanged(characteristic)
    }

    override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
        device.setRssi(rssi)
        if (originCallback != null) {
            easyBle.getExecutorService().execute(Runnable { originCallback!!.onReadRemoteRssi(gatt, rssi, status) })
        }
        if (currentRequest != null) {
            if (currentRequest!!.type == RequestType.READ_RSSI) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    notifyRssiRead(currentRequest!!, rssi)
                } else {
                    handleGattStatusFailed()
                }
                executeNextRequest()
            }
        }
    }

    override fun onDescriptorRead(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor, status: Int) {
        if (originCallback != null) {
            easyBle.getExecutorService()
                .execute(Runnable { originCallback!!.onDescriptorRead(gatt, descriptor, status) })
        }
        if (currentRequest != null) {
            if (currentRequest!!.type == RequestType.READ_DESCRIPTOR) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    notifyDescriptorRead(currentRequest!!, descriptor.getValue())
                } else {
                    handleGattStatusFailed()
                }
                executeNextRequest()
            }
        }
    }

    override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor, status: Int) {
        if (originCallback != null) {
            easyBle.getExecutorService()
                .execute(Runnable { originCallback!!.onDescriptorWrite(gatt, descriptor, status) })
        }
        if (currentRequest != null) {
            if (currentRequest!!.type == RequestType.SET_NOTIFICATION || currentRequest!!.type == RequestType.SET_INDICATION) {
                val localDescriptor = getDescriptor(
                    descriptor.getCharacteristic().getService().getUuid(),
                    descriptor.getCharacteristic().getUuid(), Connection.Companion.clientCharacteristicConfig
                )
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    handleGattStatusFailed()
                    if (localDescriptor != null) {
                        localDescriptor.setValue(currentRequest!!.descriptorTemp)
                    }
                } else {
                    notifyNotificationChanged(currentRequest!!, (currentRequest!!.value as Int) == 1)
                }
                executeNextRequest()
            }
        }
    }

    override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
        if (originCallback != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                easyBle.getExecutorService().execute(Runnable { originCallback!!.onMtuChanged(gatt, mtu, status) })
            }
        }
        if (currentRequest != null) {
            if (currentRequest!!.type == RequestType.CHANGE_MTU) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    this@ConnectionImpl.mtu = mtu
                    notifyMtuChanged(currentRequest!!, mtu)
                } else {
                    handleGattStatusFailed()
                }
                executeNextRequest()
            }
        }
    }

    override fun onPhyRead(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
        if (originCallback != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                easyBle.getExecutorService()
                    .execute(Runnable { originCallback!!.onPhyRead(gatt, txPhy, rxPhy, status) })
            }
        }
        handlePhyChange(true, txPhy, rxPhy, status)
    }

    override fun onPhyUpdate(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
        if (originCallback != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                easyBle.getExecutorService()
                    .execute(Runnable { originCallback!!.onPhyRead(gatt, txPhy, rxPhy, status) })
            }
        }
        handlePhyChange(false, txPhy, rxPhy, status)
    }

    companion object {
        private const val MSG_REQUEST_TIMEOUT = 0
        private const val MSG_CONNECT = 1
        private const val MSG_DISCONNECT = 2
        private const val MSG_REFRESH = 3
        private const val MSG_TIMER = 4
        private const val MSG_DISCOVER_SERVICES = 6
        private const val MSG_ON_CONNECTION_STATE_CHANGE = 7
        private const val MSG_ON_SERVICES_DISCOVERED = 8

        private const val MSG_ARG_NONE = 0
        private const val MSG_ARG_RECONNECT = 1
    }
}
