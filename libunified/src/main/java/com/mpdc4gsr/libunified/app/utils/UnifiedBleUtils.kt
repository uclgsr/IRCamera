package com.mpdc4gsr.libunified.app.utils

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import java.util.*

object UnifiedBleUtils {
    private const val TAG = "UnifiedBleUtils"
    fun bytesToHexString(byteArray: ByteArray?): String {
        if (byteArray == null || byteArray.isEmpty()) {
            return "BYTE IS NULL"
        }
        val sb = StringBuilder(byteArray.size * 2)
        for (byte in byteArray) {
            val hex = Integer.toHexString(0xFF and byte.toInt())
            if (hex.length < 2) {
                sb.append('0')
            }
            sb.append(hex)
        }
        return sb.toString().uppercase(Locale.getDefault())
    }

    fun hexStringToBytes(hexString: String?): ByteArray {
        if (hexString.isNullOrEmpty()) {
            return ByteArray(0)
        }
        val cleanHex = hexString.replace(" ", "").replace("-", "").replace(":", "")
        val length = cleanHex.length
        val data = ByteArray(length / 2)
        var i = 0
        while (i < length) {
            data[i / 2] = ((Character.digit(cleanHex[i], 16) shl 4) +
                    Character.digit(cleanHex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    fun isBleSupported(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    }

    fun isBluetoothEnabled(): Boolean {
        @Suppress("DEPRECATION")
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        return bluetoothAdapter?.isEnabled == true
    }

    fun getBluetoothAdapter(): BluetoothAdapter? {
        @Suppress("DEPRECATION")
        return BluetoothAdapter.getDefaultAdapter()
    }

    fun hasBluetoothLowEnergyCapabilities(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) &&
                getBluetoothAdapter() != null
    }

    fun formatDeviceName(device: BluetoothDevice?): String {
        if (device == null) return "Unknown Device"
        val name = device.name
        val address = device.address
        return when {
            !name.isNullOrBlank() -> "$name ($address)"
            !address.isNullOrBlank() -> address
            else -> "Unknown Device"
        }
    }

    fun getRssiDescription(rssi: Int): String {
        return when {
            rssi >= -50 -> "Excellent"
            rssi >= -60 -> "Good"
            rssi >= -70 -> "Fair"
            rssi >= -80 -> "Weak"
            else -> "Very Weak"
        }
    }

    fun getServiceName(uuid: UUID?): String {
        if (uuid == null) return "Unknown Service"
        return when (uuid.toString().uppercase()) {
            "00001800-0000-1000-8000-00805F9B34FB" -> "Generic Access"
            "00001801-0000-1000-8000-00805F9B34FB" -> "Generic Attribute"
            "0000180F-0000-1000-8000-00805F9B34FB" -> "Battery Service"
            "0000180A-0000-1000-8000-00805F9B34FB" -> "Device Information"
            "00001802-0000-1000-8000-00805F9B34FB" -> "Immediate Alert"
            "00001803-0000-1000-8000-00805F9B34FB" -> "Link Loss"
            "00001804-0000-1000-8000-00805F9B34FB" -> "Tx Power"
            else -> "Custom Service"
        }
    }

    fun getCharacteristicName(uuid: UUID?): String {
        if (uuid == null) return "Unknown Characteristic"
        return when (uuid.toString().uppercase()) {
            "00002A00-0000-1000-8000-00805F9B34FB" -> "Device Name"
            "00002A01-0000-1000-8000-00805F9B34FB" -> "Appearance"
            "00002A04-0000-1000-8000-00805F9B34FB" -> "Peripheral Preferred Connection Parameters"
            "00002A19-0000-1000-8000-00805F9B34FB" -> "Battery Level"
            "00002A29-0000-1000-8000-00805F9B34FB" -> "Manufacturer Name String"
            "00002A24-0000-1000-8000-00805F9B34FB" -> "Model Number String"
            "00002A25-0000-1000-8000-00805F9B34FB" -> "Serial Number String"
            "00002A27-0000-1000-8000-00805F9B34FB" -> "Hardware Revision String"
            "00002A26-0000-1000-8000-00805F9B34FB" -> "Firmware Revision String"
            "00002A28-0000-1000-8000-00805F9B34FB" -> "Software Revision String"
            else -> "Custom Characteristic"
        }
    }

    fun getCharacteristicProperties(characteristic: BluetoothGattCharacteristic): String {
        val properties = mutableListOf<String>()
        val props = characteristic.properties
        if (props and BluetoothGattCharacteristic.PROPERTY_READ != 0) properties.add("READ")
        if (props and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) properties.add("WRITE")
        if (props and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) properties.add("WRITE_NO_RESPONSE")
        if (props and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) properties.add("NOTIFY")
        if (props and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) properties.add("INDICATE")
        if (props and BluetoothGattCharacteristic.PROPERTY_BROADCAST != 0) properties.add("BROADCAST")
        if (props and BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS != 0) properties.add("EXTENDED_PROPS")
        if (props and BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE != 0) properties.add("SIGNED_WRITE")
        return if (properties.isNotEmpty()) properties.joinToString(", ") else "NONE"
    }

    fun parseScanRecord(scanRecord: ByteArray?): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        if (scanRecord == null || scanRecord.isEmpty()) {
            return result
        }
        var index = 0
        while (index < scanRecord.size) {
            val length = scanRecord[index].toInt() and 0xFF
            if (length == 0) break
            if (index + length >= scanRecord.size) break
            val type = scanRecord[index + 1].toInt() and 0xFF
            val data = scanRecord.sliceArray((index + 2)..(index + length))
            when (type) {
                0x01 -> result["flags"] = data
                0x02, 0x03 -> result["serviceUuids"] = data
                0x08, 0x09 -> result["deviceName"] = String(data)
                0x0A -> result["txPowerLevel"] = data[0]
                0xFF -> result["manufacturerData"] = data
            }
            index += length + 1
        }
        return result
    }

    fun supportsNotifications(characteristic: BluetoothGattCharacteristic): Boolean {
        return (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
    }

    fun supportsIndications(characteristic: BluetoothGattCharacteristic): Boolean {
        return (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
    }

    fun isReadable(characteristic: BluetoothGattCharacteristic): Boolean {
        return (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ) != 0
    }

    fun isWritable(characteristic: BluetoothGattCharacteristic): Boolean {
        return (characteristic.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0
    }

    fun calculateConnectionTimeout(rssi: Int): Long {
        return when {
            rssi >= -50 -> 5000L      // 5 seconds for strong signal
            rssi >= -70 -> 10000L     // 10 seconds for medium signal
            else -> 15000L            // 15 seconds for weak signal
        }
    }

    fun formatByteValue(value: Byte, signed: Boolean = false): String {
        return if (signed) {
            value.toString()
        } else {
            (value.toInt() and 0xFF).toString()
        }
    }

    fun logBleOperation(
        operation: String,
        device: BluetoothDevice?,
        success: Boolean,
        details: String = ""
    ) {
        val deviceInfo = formatDeviceName(device)
        val status = if (success) "SUCCESS" else "FAILED"
        val message =
            "BLE $operation: $status for $deviceInfo${if (details.isNotEmpty()) " - $details" else ""}"
        if (success) {
            Log.d(TAG, message)
        } else {
            Log.w(TAG, message)
        }
    }
}