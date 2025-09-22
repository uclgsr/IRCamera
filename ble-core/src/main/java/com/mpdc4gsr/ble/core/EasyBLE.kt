package com.mpdc4gsr.ble.core

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.mpdc4gsr.ble.core.ScanListener
import com.mpdc4gsr.ble.core.util.BluetoothPermissionUtils
import com.mpdc4gsr.ble.core.util.DefaultLogger
import com.mpdc4gsr.ble.core.util.Logger
import com.mpdc4gsr.commons.poster.MethodInfo
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import kotlin.concurrent.Volatile

class EasyBLE internal constructor(builder: EasyBLEBuilder) {
    val scanConfiguration: ScanConfiguration
    val executorService: ExecutorService
    val posterDispatcher: PosterDispatcher
    private val bondController: BondController?
    val deviceCreator: DeviceCreator
    val observable: Observable
    val logger: Logger
    private val scannerType: ScannerType?
    private val connectionMap: MutableMap<String?, Connection> = ConcurrentHashMap<String?, Connection>()
    private val addressList: MutableList<String?> = CopyOnWriteArrayList<String?>()
    private val useNordicBleBackend: Boolean
    private val internalObservable: Boolean
    private var unifiedBleManager: UnifiedBleManager? = null
    private var scanner: Scanner? = null
    private var application: Application? = null
    private var isInitialized = false
    var bluetoothAdapter: BluetoothAdapter? = null
        private set
    private var broadcastReceiver: BroadcastReceiver? = null

    private constructor() : this(DEFAULT_BUILDER)

    init {
        tryGetApplication()
        bondController = builder.bondController
        scannerType = builder.scannerType
        useNordicBleBackend = builder.useNordicBleBackend
        deviceCreator = builder.deviceCreator ?: DefaultDeviceCreator()
        scanConfiguration = builder.scanConfiguration ?: ScanConfiguration()
        logger = builder.logger ?: DefaultLogger("EasyBLE")
        if (builder.observable != null) {
            internalObservable = false
            observable = builder.observable!!
            posterDispatcher = observable.getPosterDispatcher()
            executorService = posterDispatcher.executorService
        } else {
            internalObservable = true
            executorService = builder.executorService
            posterDispatcher = DefaultPosterDispatcher()
            observable = DefaultObservable()
        }

        if (application != null) {
            val app = application!!
            unifiedBleManager = UnifiedBleManager.getInstance(app)
        }
    }

    val context: Context?
        get() {
            if (application == null) {
                tryAutoInit()
            }
            return application
        }

    @SuppressLint("PrivateApi")
    private fun tryGetApplication() {
        try {
            val cls = Class.forName("android.app.ActivityThread")
            val method = cls.getMethod("currentActivityThread")
            method.setAccessible(true)
            val acThread = method.invoke(null)
            val appMethod = acThread!!.javaClass.getMethod("getApplication")
            application = appMethod.invoke(acThread) as Application?
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getScannerType(): ScannerType? {
        return if (scanner == null) null else scanner!!.getType()
    }

    fun isInitialized(): Boolean {
        return isInitialized && application != null && instance != null
    }

    val isBluetoothOn: Boolean
        get() = bluetoothAdapter != null && bluetoothAdapter!!.isEnabled()

    fun getUnifiedBleManager(): UnifiedBleManager? {
        if (unifiedBleManager == null && application != null) {
            val app = application!!
            unifiedBleManager = UnifiedBleManager.getInstance(app)
        }
        return unifiedBleManager
    }

    val isUnifiedBleManagerReady: Boolean
        get() {
            val manager = getUnifiedBleManager()
            return manager != null
        }

    @Synchronized
    fun initialize(application: Application?) {
        if (isInitialized()) {
            return
        }
        Inspector.requireNonNull<Application?>(application, "application can't be")
        this.application = application

        if (!application!!.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return
        }

        val bluetoothManager = application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        if (bluetoothManager == null || bluetoothManager.getAdapter() == null) {
            return
        }
        bluetoothAdapter = bluetoothManager.getAdapter()

        if (broadcastReceiver == null) {
            broadcastReceiver = InnerBroadcastReceiver()
            val filter = IntentFilter()
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            filter.addAction(BluetoothDevice.ACTION_FOUND)
            application.registerReceiver(broadcastReceiver, filter)
        }
        isInitialized = true
    }

    @Synchronized
    private fun checkStatus(): Boolean {
        Inspector.requireNonNull<EasyBLE?>(instance, "EasyBLE instance has been destroyed!")
        if (!isInitialized) {
            if (!tryAutoInit()) {
                val msg =
                    "The SDK has not been initialized, make sure to call EasyBLE.getInstance().initialize(Application) first."
                logger.log(Log.ERROR, Logger.Companion.TYPE_GENERAL, msg)
                return false
            }
        } else if (application == null) {
            return tryAutoInit()
        }
        return true
    }

    private fun tryAutoInit(): Boolean {
        tryGetApplication()
        if (application != null) {
            initialize(application)
        }
        return isInitialized()
    }

    fun setLogEnabled(isEnabled: Boolean) {
        logger.isEnabled = isEnabled
    }

    @Synchronized
    fun release() {
        if (broadcastReceiver != null) {
            application!!.unregisterReceiver(broadcastReceiver)
            broadcastReceiver = null
        }
        isInitialized = false
        if (scanner != null) {
            scanner!!.release()
        }
        releaseAllConnections()
        if (internalObservable) {
            observable.unregisterAll()
            posterDispatcher.clearTasks()
        }
    }

    fun destroy() {
        release()
        synchronized(EasyBLE::class.java) {
            instance = null
        }
    }

    fun registerObserver(observer: EventObserver) {
        if (checkStatus()) {
            observable.registerObserver(observer)
        }
    }

    fun isObserverRegistered(observer: EventObserver): Boolean {
        return observable.isRegistered(observer)
    }

    fun unregisterObserver(observer: EventObserver) {
        observable.unregisterObserver(observer)
    }

    fun notifyObservers(info: MethodInfo) {
        if (checkStatus()) {
            observable.notifyObservers(info)
        }
    }

    private fun checkAndInstanceScanner() {
        if (scanner == null) {
            synchronized(this) {
                if (bluetoothAdapter != null && scanner == null) {
                    val adapter = bluetoothAdapter!!
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        if (scannerType == ScannerType.LEGACY) {
                            scanner = LegacyScanner(this, adapter)
                        } else if (scannerType == ScannerType.CLASSIC) {
                            scanner = ClassicScanner(this, adapter)
                        } else {
                            scanner = LeScanner(this, adapter)
                        }
                    } else if (scannerType == ScannerType.CLASSIC) {
                        scanner = ClassicScanner(this, adapter)
                    } else {
                        scanner = LegacyScanner(this, adapter)
                    }
                }
            }
        }
    }

    fun addScanListener(listener: ScanListener?) {
        checkAndInstanceScanner()
        if (checkStatus() && scanner != null && listener != null) {
            scanner!!.addScanListener(listener)
        }
    }

    fun removeScanListener(listener: ScanListener?) {
        if (scanner != null && listener != null) {
            scanner!!.removeScanListener(listener)
        }
    }

    val isScanning: Boolean
        get() = scanner != null && scanner!!.isScanning()

    fun startScan() {
        checkAndInstanceScanner()
        if (checkStatus() && scanner != null) {
            // Create a default scan listener for internal use
            val defaultListener = object : ScanListener {
                override fun onScanStart() {}
                override fun onScanStop() {}
                override fun onScanResult(device: Device, rssi: Int, data: ByteArray) {}
                override fun onScanFailed(reason: Int) {}
                override fun onScanComplete() {}
                override fun onScanError(errorCode: Int, errorMessage: String) {}
            }
            scanner!!.startScan(defaultListener)
        }
    }

    fun stopScan() {
        if (checkStatus() && scanner != null) {
            scanner!!.stopScan()
        }
    }

    fun stopScanQuietly() {
        if (checkStatus() && scanner != null) {
            scanner!!.stopScan()
        }
    }

    fun connect(address: String?, observer: EventObserver?): Connection? {
        return connect(address, null, observer)
    }

    @JvmOverloads
    fun connect(
        address: String?, configuration: ConnectionConfiguration? = null,
        observer: EventObserver? = null
    ): Connection? {
        if (checkStatus()) {
            Inspector.requireNonNull<String?>(address, "address can't be null")
            val remoteDevice = bluetoothAdapter!!.getRemoteDevice(address)
            if (remoteDevice != null) {
                return connect(Device(remoteDevice), configuration, observer)
            }
        }
        return null
    }

    fun connect(device: Device?): Connection? {
        return connect(device, null, null)
    }

    fun connect(device: Device?, configuration: ConnectionConfiguration?): Connection? {
        return connect(device, configuration, null)
    }

    fun connect(device: Device?, observer: EventObserver?): Connection? {
        return connect(device, null, observer)
    }

    @Synchronized
    fun connect(
        device: Device?, configuration: ConnectionConfiguration?,
        observer: EventObserver?
    ): Connection? {
        if (checkStatus()) {
            Inspector.requireNonNull<Device?>(device, "device can't be null")
            var connection = connectionMap.remove(device!!.getAddress())

            if (connection != null) {
                connection.releaseNoEvent()
            }
            val isConnectable = device.isConnectable
            if (isConnectable == null || isConnectable) {
                var connectDelay = 0
                if (bondController != null && bondController.accept(device)) {
                    val remoteDevice = bluetoothAdapter!!.getRemoteDevice(device.getAddress())
                    if (BluetoothPermissionUtils.hasBluetoothConnectPermission(this.context)) {
                        try {
                            if (remoteDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
                                connectDelay = if (createBond(device.getAddress())) 1500 else 0
                            }
                        } catch (e: SecurityException) {
                            logger.log(
                                Log.WARN, Logger.Companion.TYPE_CONNECTION_STATE,
                                "SecurityException checking bond state: " + e.message
                            )
                        }
                    } else {
                        logger.log(
                            Log.WARN, Logger.Companion.TYPE_CONNECTION_STATE,
                            "Missing BLUETOOTH_CONNECT permission for bonding operations"
                        )
                    }
                }

                if (useNordicBleBackend) {
                    logger.log(
                        Log.INFO, Logger.Companion.TYPE_CONNECTION_STATE,
                        "Creating Nordic BLE-enhanced connection for improved reliability: " + device.getAddress()
                    )
                    connection =
                        NordicConnectionImpl(device, this, bluetoothAdapter, configuration, connectDelay, observer)
                } else {
                    logger.log(
                        Log.DEBUG, Logger.Companion.TYPE_CONNECTION_STATE,
                        "Creating standard EasyBLE connection: " + device.getAddress()
                    )
                    connection = ConnectionImpl(this, bluetoothAdapter, device, configuration, connectDelay, observer)
                }
                connectionMap.put(device.address, connection)
                addressList.add(device.address)
                return connection
            } else {
                val message = String.format(
                    Locale.US, "connect failed! [type: unconnectable, name: %s, addr: %s]",
                    device.getName(), device.getAddress()
                )
                logger.log(Log.ERROR, Logger.Companion.TYPE_CONNECTION_STATE, message)
                if (observer != null) {
                    posterDispatcher.post(
                        observer,
                        MethodInfoGenerator.onConnectFailed(
                            device,
                            Connection.Companion.CONNECT_FAIL_TYPE_CONNECTION_IS_UNSUPPORTED
                        )
                    )
                }
                observable.notifyObservers(
                    MethodInfoGenerator.onConnectFailed(
                        device,
                        Connection.Companion.CONNECT_FAIL_TYPE_CONNECTION_IS_UNSUPPORTED
                    )
                )
            }
        }
        return null
    }

    val connections: MutableCollection<Connection?>
        get() = connectionMap.values as MutableCollection<Connection?>

    val orderedConnections: MutableList<Connection?>
        get() {
            val list: MutableList<Connection?> =
                ArrayList<Connection?>()
            for (address in addressList) {
                val connection = connectionMap.get(address)
                if (connection != null) {
                    list.add(connection)
                }
            }
            return list
        }

    val firstConnection: Connection?
        get() = if (addressList.isEmpty()) null else connectionMap.get(addressList.get(0))

    val lastConnection: Connection?
        get() = if (addressList.isEmpty()) null else connectionMap.get(addressList.get(addressList.size - 1))

    fun getConnection(device: Device?): Connection? {
        return if (device == null) null else connectionMap.get(device.getAddress())
    }

    fun getConnection(address: String?): Connection? {
        return if (address == null) null else connectionMap.get(address)
    }

    fun disconnectConnection(device: Device?) {
        if (checkStatus() && device != null) {
            val connection = connectionMap.get(device.getAddress())
            if (connection != null) {
                connection.disconnect()
            }
        }
    }

    fun disconnectConnection(address: String?) {
        if (checkStatus() && address != null) {
            val connection = connectionMap.get(address)
            if (connection != null) {
                connection.disconnect()
            }
        }
    }

    fun disconnectAllConnections() {
        if (checkStatus()) {
            for (connection in connectionMap.values) {
                connection.disconnect()
            }
        }
    }

    fun releaseAllConnections() {
        if (checkStatus()) {
            for (connection in connectionMap.values) {
                connection.release()
            }
            connectionMap.clear()
            addressList.clear()
        }
    }

    fun releaseConnection(address: String?) {
        if (checkStatus() && address != null) {
            addressList.remove(address)
            val connection = connectionMap.remove(address)
            if (connection != null) {
                connection.release()
            }
        }
    }

    fun releaseConnection(device: Device?) {
        if (checkStatus() && device != null) {
            addressList.remove(device.getAddress())
            val connection = connectionMap.remove(device.getAddress())
            if (connection != null) {
                connection.release()
            }
        }
    }

    fun reconnectAll() {
        if (checkStatus()) {
            for (connection in connectionMap.values) {
                if (connection.getConnectionState() != ConnectionState.SERVICE_DISCOVERED) {
                    connection.reconnect()
                }
            }
        }
    }

    fun reconnect(device: Device?) {
        if (checkStatus() && device != null) {
            val connection = connectionMap.get(device.getAddress())
            if (connection != null && connection.getConnectionState() != ConnectionState.SERVICE_DISCOVERED) {
                connection.reconnect()
            }
        }
    }

    fun getBondState(address: String?): Int {
        checkStatus()
        if (!BluetoothPermissionUtils.hasBluetoothConnectPermission(this.context)) {
            logger.log(
                Log.WARN, Logger.Companion.TYPE_CONNECTION_STATE,
                "Missing BLUETOOTH_CONNECT permission for getBondState()"
            )
            return BluetoothDevice.BOND_NONE
        }

        try {
            return bluetoothAdapter!!.getRemoteDevice(address).getBondState()
        } catch (e: SecurityException) {
            logger.log(
                Log.WARN, Logger.Companion.TYPE_CONNECTION_STATE,
                "SecurityException getting bond state: " + e.message
            )
            return BluetoothDevice.BOND_NONE
        } catch (e: Exception) {
            return BluetoothDevice.BOND_NONE
        }
    }

    fun createBond(address: String?): Boolean {
        checkStatus()
        if (!BluetoothPermissionUtils.hasBluetoothConnectPermission(this.context)) {
            logger.log(
                Log.WARN, Logger.Companion.TYPE_CONNECTION_STATE,
                "Missing BLUETOOTH_CONNECT permission for createBond()"
            )
            return false
        }

        try {
            val remoteDevice = bluetoothAdapter!!.getRemoteDevice(address)
            return remoteDevice.getBondState() != BluetoothDevice.BOND_NONE || remoteDevice.createBond()
        } catch (e: SecurityException) {
            logger.log(
                Log.WARN, Logger.Companion.TYPE_CONNECTION_STATE,
                "SecurityException creating bond: " + e.message
            )
            return false
        } catch (ignore: Exception) {
            return false
        }
    }

    fun clearBondDevices(filter: RemoveBondFilter?) {
        checkStatus()
        if (bluetoothAdapter != null) {
            val devices = bluetoothAdapter!!.getBondedDevices()
            for (device in devices) {
                if (filter == null || filter.accept(device)) {
                    try {
                        device.javaClass.getMethod("removeBond").invoke(device)
                    } catch (ignore: Exception) {
                    }
                }
            }
        }
    }

    fun removeBond(address: String?) {
        checkStatus()
        try {
            val remoteDevice = bluetoothAdapter!!.getRemoteDevice(address)
            if (remoteDevice.getBondState() != BluetoothDevice.BOND_NONE) {
                remoteDevice.javaClass.getMethod("removeBond").invoke(remoteDevice)
            }
        } catch (ignore: Exception) {
        }
    }

    private inner class InnerBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.getAction()
            if (action != null) {
                when (action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED -> if (bluetoothAdapter != null) {
                        observable.notifyObservers(MethodInfoGenerator.onBluetoothAdapterStateChanged(bluetoothAdapter!!.getState()))
                        if (bluetoothAdapter!!.getState() == BluetoothAdapter.STATE_OFF) {
                            logger.log(Log.DEBUG, Logger.Companion.TYPE_GENERAL, "[CHINESE_TEXT]Close[CHINESE_TEXT]")

                            if (scanner != null) {
                                scanner!!.onBluetoothOff()
                            }

                            disconnectAllConnections()
                        } else if (bluetoothAdapter!!.getState() == BluetoothAdapter.STATE_ON) {
                            logger.log(Log.DEBUG, Logger.Companion.TYPE_GENERAL, "[CHINESE_TEXT]")

                            for (connection in connectionMap.values) {
                                if (connection.isAutoReconnectEnabled) {
                                    connection.reconnect()
                                }
                            }
                        }
                    }

                    BluetoothAdapter.ACTION_DISCOVERY_STARTED -> if (scanner is ClassicScanner) {
                        val scanner = this@EasyBLE.scanner as ClassicScanner
                        scanner.setScanning(true)
                    }

                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> if (scanner is ClassicScanner) {
                        val scanner = this@EasyBLE.scanner as ClassicScanner
                        scanner.setScanning(false)
                    }

                    BluetoothDevice.ACTION_FOUND -> {
                        val device = intent.getParcelableExtra<BluetoothDevice?>(BluetoothDevice.EXTRA_DEVICE)
                        if (device != null && scanner is ClassicScanner) {
                            var rssi = -120
                            val extras = intent.getExtras()
                            if (extras != null) {
                                rssi = extras.getShort(BluetoothDevice.EXTRA_RSSI).toInt()
                            }
                            ClassicScanner.parseScanResult(device)
                        }
                    }
                }
            }
            if (BluetoothAdapter.ACTION_STATE_CHANGED == intent.getAction()) {
                if (bluetoothAdapter != null) {
                    observable.notifyObservers(MethodInfoGenerator.onBluetoothAdapterStateChanged(bluetoothAdapter!!.getState()))
                    if (bluetoothAdapter!!.getState() == BluetoothAdapter.STATE_OFF) {
                        logger.log(Log.DEBUG, Logger.Companion.TYPE_GENERAL, "[CHINESE_TEXT]Close[CHINESE_TEXT]")

                        if (scanner != null) {
                            scanner!!.onBluetoothOff()
                        }

                        disconnectAllConnections()
                    } else if (bluetoothAdapter!!.getState() == BluetoothAdapter.STATE_ON) {
                        logger.log(Log.DEBUG, Logger.Companion.TYPE_GENERAL, "[CHINESE_TEXT]")

                        for (connection in connectionMap.values) {
                            if (connection.isAutoReconnectEnabled) {
                                connection.reconnect()
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private val DEFAULT_BUILDER = EasyBLEBuilder()

        @Volatile
        var instance: EasyBLE? = null
        fun getInstance(): EasyBLE? {
            if (instance == null) {
                synchronized(EasyBLE::class.java) {
                    if (instance == null) {
                        instance = EasyBLE()
                    }
                }
            }
            return instance
        }

        val builder: EasyBLEBuilder
            get() = EasyBLEBuilder()
    }
}
