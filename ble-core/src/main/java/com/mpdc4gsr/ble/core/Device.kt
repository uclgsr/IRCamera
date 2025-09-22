package com.mpdc4gsr.ble.core

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RequiresApi
import com.mpdc4gsr.ble.core.util.BluetoothPermissionUtils
import java.util.Objects

class Device : Comparable<Device>, Cloneable, Parcelable {
    val originDevice: BluetoothDevice
    var connectionState: ConnectionState = ConnectionState.DISCONNECTED

    @get:RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    var scanResult: ScanResult? = null
    var scanRecord: ByteArray? = null
    var name: String = ""
    var address: String = ""
    var rssi: Int = -120

    constructor(originDevice: BluetoothDevice) {
        this.originDevice = originDevice
        val context: Context? = EasyBLE.getInstance()?.context
        if (context != null) {
            this.name = BluetoothPermissionUtils.getDeviceName(context, originDevice) ?: ""
            this.address = BluetoothPermissionUtils.getDeviceAddress(context, originDevice) ?: ""
        } else {
            this.name = originDevice.name ?: "Unknown Device"
            this.address = originDevice.address ?: ""
        }
    }

    protected constructor(`in`: Parcel) {
        this.originDevice = `in`.readParcelable<BluetoothDevice>(BluetoothDevice::class.java.getClassLoader())!!
        readFromParcel(`in`)
    }

    val currentConnectionState: ConnectionState
        get() {
            val connection: Connection? = EasyBLE.getInstance()?.getConnection(this)
            return if (connection == null) connectionState else connection.connectionState
        }

    val isConnectable: Boolean?
        get() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (scanResult != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        return scanResult!!.isConnectable()
                    }
                }
            }
            return null
        }

    val isConnected: Boolean
        get() = currentConnectionState == ConnectionState.SERVICE_DISCOVERED

    val isDisconnected: Boolean
        get() {
            val state = currentConnectionState
            return state == ConnectionState.DISCONNECTED || state == ConnectionState.RELEASED
        }

    val isConnecting: Boolean
        get() {
            val state = currentConnectionState
            return state != ConnectionState.DISCONNECTED && state != ConnectionState.SERVICE_DISCOVERED && state != ConnectionState.RELEASED
        }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o !is Device) return false

        val device = o

        return address == device.address
    }

    override fun hashCode(): Int {
        return address.hashCode()
    }

    override fun compareTo(other: Device): Int {
        if (rssi == 0) {
            return -1
        } else if (other.rssi == 0) {
            return 1
        } else {
            var result = Integer.compare(other.rssi, rssi)
            if (result == 0) {
                result = name.compareTo(other.name)
            }
            return result
        }
    }

    override fun toString(): String {
        return "Device{" +
                "name='" + name + '\'' +
                ", address='" + address + '\'' +
                '}'
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(this.originDevice, flags)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dest.writeParcelable(this.scanResult, flags)
        }
        if (this.scanRecord != null) {
            dest.writeInt(this.scanRecord!!.size)
            dest.writeByteArray(this.scanRecord)
        } else {
            dest.writeInt(-1)
        }
        dest.writeString(this.name)
        dest.writeString(this.address)
        dest.writeInt(this.rssi)
        for (state in ConnectionState.entries) {
            if (state == connectionState) {
                dest.writeString(this.connectionState.name)
                break
            }
        }
    }

    fun readFromParcel(`in`: Parcel) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.scanResult = `in`.readParcelable<ScanResult?>(ScanResult::class.java.getClassLoader())
        }
        val scanRecordLen = `in`.readInt()
        if (scanRecordLen > 0) {
            this.scanRecord = ByteArray(scanRecordLen)
            `in`.readByteArray(this.scanRecord!!)
        }
        val inName = `in`.readString()
        this.name = if (inName == null) "" else inName
        this.address = Objects.requireNonNull<String>(`in`.readString())
        this.rssi = `in`.readInt()
        this.connectionState = ConnectionState.valueOf(`in`.readString()!!)
    }

    /**
     * Get device type
     */
    fun getType(): Int {
        return BluetoothPermissionUtils.getDeviceType(EasyBLE.getInstance()?.context, originDevice)
    }

    /**
     * Get bond state
     */
    fun getBondState(): Int {
        return BluetoothPermissionUtils.getDeviceBondState(EasyBLE.getInstance()?.context, originDevice)
    }

    companion object {
        val CREATOR: Parcelable.Creator<Device?> = object : Parcelable.Creator<Device?> {
            override fun createFromParcel(source: Parcel): Device {
                return Device(source)
            }

            override fun newArray(size: Int): Array<Device?> {
                return arrayOfNulls<Device>(size)
            }
        }
    }
}
