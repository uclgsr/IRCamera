package com.mpdc4gsr.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.util.Log
import com.mpdc4gsr.ble.callback.MtuChangeCallback
import com.mpdc4gsr.ble.util.BluetoothPermissionUtils
import com.mpdc4gsr.commons.UUIDManager
import com.mpdc4gsr.commons.observer.Observable
import com.mpdc4gsr.commons.observer.Observe
import com.mpdc4gsr.commons.poster.RunOn
import com.mpdc4gsr.commons.poster.Tag
import com.mpdc4gsr.commons.poster.ThreadMode
import com.mpdc4gsr.commons.util.LLog
import com.mpdc4gsr.commons.util.StringUtils
import org.greenrobot.eventbus.EventBus
import java.util.UUID

class BluetoothManager : EventObserver {
    var device: Device? = null
        private set
    private var connection: Connection? = null
    private var writeCharact: BluetoothGattCharacteristic? = null

    private fun setMTUValue() {
        if (device!!.isConnected()) {
            Log.e("bcf_ble", "Connect[CHINESE_TEXT]：" + device!!.getName() + "")
            var builder: RequestBuilder<MtuChangeCallback?>? = null
            if (device!!.getName().contains("T-darts") || device!!.getName().contains("TD")) {
                builder = RequestBuilderFactory().getChangeMtuBuilder(240)
            } else {
                builder = RequestBuilderFactory().getChangeMtuBuilder(503)
            }
            val request = builder.setCallback(object : MtuChangeCallback {
                override fun onMtuChanged(request: Request, mtu: Int) {
                    Log.d("wangchen", "MTU changed successfully, new value: " + mtu)
                    setReadCallback()
                }

                override fun onRequestFailed(request: Request, failType: Int, value: Any?) {
                    Log.d("bcf", "MTUModifyFailed")
                }
            }).build()
            connection!!.execute(request)
        }
    }

    private fun setReadCallback() {
        if (device!!.isConnected()) {
            isSending = false

            val isEnabled = connection!!.isNotificationOrIndicationEnabled(
                UUID.fromString(UUIDManager.SERVICE_UUID),
                UUID.fromString(UUIDManager.NOTIFY_UUID)
            )
            LLog.w("bcf_ble", "[CHINESE_TEXT]Open[CHINESE_TEXT]Notifycation: " + isEnabled)
            val builder = RequestBuilderFactory().getSetNotificationBuilder(
                UUID.fromString(UUIDManager.SERVICE_UUID),
                UUID.fromString(UUIDManager.NOTIFY_UUID),
                true
            )
            val builder1 = RequestBuilderFactory().getReadCharacteristicBuilder(
                UUID.fromString(UUIDManager.SERVICE_UUID),
                UUID.fromString(UUIDManager.READ_UUID)
            )

            builder.build().execute(connection)
            builder1.build().execute(connection)
        }
    }

    fun setCancelListening() {
        val observable: Observable? = EasyBLE.Companion.getInstance().getObservable()
        if (observable != null) {
            EasyBLE.Companion.getInstance().unregisterObserver(this)
        }
    }

    fun connect(device: Device?): Connection? {
        this.device = device
        val config = ConnectionConfiguration()
        config.setConnectTimeoutMillis(10000)
        config.setRequestTimeoutMillis(7000)
        config.setAutoReconnect(false)
        config.setReconnectImmediatelyMaxTimes(3)
        connection = EasyBLE.Companion.getInstance().connect(device, config, this)
        connection!!.setBluetoothGattCallback(object : BluetoothGattCallback() {
            override fun onCharacteristicWrite(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                Log.d(
                    "ble_bcf_data",
                    "[CHINESE_TEXT]Data[CHINESE_TEXT]：status: " + status + "  Content：" + StringUtils.toHex(
                        characteristic.getValue()
                    )
                )
                setBleData(
                    "[CHINESE_TEXT]Data[CHINESE_TEXT]：status: " + status + "  Content：" + StringUtils.toHex(
                        characteristic.getValue()
                    )
                )
            }
        })
        return connection
    }

    fun connect(mac: String?, name: String?): Connection? {
        val configuration = ConnectionConfiguration()
        configuration.setConnectTimeoutMillis(10000)
        configuration.setRequestTimeoutMillis(7000)
        configuration.setAutoReconnect(false)
        configuration.setReconnectImmediatelyMaxTimes(3)
        connection = EasyBLE.Companion.getInstance().connect(mac, configuration, this)
        this.device = connection!!.getDevice()
        device!!.setName(name)
        return connection
    }

    fun release() {
        Log.d("bcf", "[CHINESE_TEXT]BLEConnect")
        EasyBLE.Companion.getInstance().disconnectConnection(this.device)
        EasyBLE.Companion.getInstance().release()
        EasyBLE.Companion.getInstance().releaseConnection(this.device)
    }

    val isConnected: Boolean
        get() {
            if (this.device == null) return false
            return device!!.isConnected()
        }

    @Tag("onConnectionStateChanged")
    @Observe
    @RunOn(ThreadMode.MAIN)
    override fun onConnectionStateChanged(device: Device) {
        if (device.getConnectionState() != ConnectionState.SERVICE_DISCOVERED || device.getConnectionState() != ConnectionState.DISCONNECTED) {
            EventBus.getDefault().post(device.getConnectionState())
            Log.e("wangchen", "Send[CHINESE_TEXT]--" + device.getConnectionState())
        }
        Log.d(
            "ywq",
            "MyObserver Connection state: " + device.getConnectionState() + " Is connected: " + device.isConnected() + "-----Name: " + device.getName() + "-------mac: " + device.getAddress()
        )
        when (device.getConnectionState()) {
            ConnectionState.SCANNING_FOR_RECONNECTION -> {}
            ConnectionState.CONNECTING -> {}
            ConnectionState.CONNECTED -> {}
            ConnectionState.DISCONNECTED -> EventBus.getDefault().post(ConnectionState.DISCONNECTED.name)
            ConnectionState.RELEASED -> EventBus.getDefault().post(ConnectionState.RELEASED.name)
            ConnectionState.SERVICE_DISCOVERED -> {
                setMTUValue()

                if (device.isConnected()) {
                    EventBus.getDefault().post(ConnectionState.SERVICE_DISCOVERED.name)
                }
            }
        }
    }

    override fun onConnectFailed(device: Device, failType: Int) {
        Log.e("bcf_ble", "ConnectFailed" + device.getName())
        EventBus.getDefault().post(device.getConnectionState())
    }

    override fun onConnectTimeout(device: Device?, type: Int) {
        Log.e("bcf_ble", "Connection timeout")
    }

    @Observe
    override fun onNotificationChanged(request: Request, isEnabled: Boolean) {
        var typeTag = ""
        if (request.getType() == RequestType.SET_NOTIFICATION) {
            typeTag = "Notification"
            EventBus.getDefault().post(ConnectionState.MTU_SUCCESS)
        } else {
            typeTag = "Indication"
        }
        Log.d("bcf_ble", "onNotificationChanged ：" + typeTag + "：" + (if (isEnabled) "[CHINESE_TEXT]" else "Close"))
    }

    fun writeBuletoothData(data: ByteArray?): Boolean {
        if (this.device == null || !device!!.isConnected()) {
            return false
        }

        if (!BluetoothPermissionUtils.hasBluetoothConnectPermission(EasyBLE.Companion.getInstance().getContext())) {
            Log.w(TAG, "Missing BLUETOOTH_CONNECT permission for GATT operations")
            return false
        }

        try {
            writeCharact = connection!!.getCharacteristic(
                UUID.fromString(UUIDManager.SERVICE_UUID),
                UUID.fromString(UUIDManager.WRITE_UUID)
            )
            connection!!.getGatt()!!.setCharacteristicNotification(writeCharact, true)

            writeCharact!!.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
            writeCharact!!.setValue(data)

            return connection!!.getGatt()!!.writeCharacteristic(writeCharact)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException during GATT write operation: " + e.message)
            return false
        }
    }

    @Observe
    override fun onCharacteristicRead(request: Request?, value: ByteArray?) {
        val data = StringUtils.toHex(value)
    }

    @Observe
    override fun onCharacteristicChanged(device: Device?, service: UUID?, characteristic: UUID?, value: ByteArray?) {
        Log.e("ble_bcf_data", "Receive[CHINESE_TEXT]Data：" + StringUtils.toHex(value))
        EventBus.getDefault().post(value)
    }

    companion object {
        private const val TAG = "BluetoothManager"

        var iSReset: Boolean = false
        var isSending: Boolean = false
        var isClickStopCharging: Boolean = false
        var isReceiveBleData: Boolean = false
        var instance: BluetoothManager? = null
            get() {
                if (field == null) field = BluetoothManager()
                return field
            }
            private set

        fun setBleData(message: String?) {
        }
    }
}
