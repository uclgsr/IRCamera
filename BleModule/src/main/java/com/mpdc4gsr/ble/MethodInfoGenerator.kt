package com.mpdc4gsr.ble

import com.mpdc4gsr.commons.poster.MethodInfo
import java.util.UUID

internal object MethodInfoGenerator {
    fun onBluetoothAdapterStateChanged(state: Int): MethodInfo {
        return MethodInfo("onBluetoothAdapterStateChanged", MethodInfo.Parameter(Int::class.javaPrimitiveType!!, state))
    }

    fun onConnectionStateChanged(device: Device?): MethodInfo {
        return MethodInfo("onConnectionStateChanged", MethodInfo.Parameter(Device::class.java, device))
    }

    fun onConnectFailed(device: Device?, failType: Int): MethodInfo {
        return MethodInfo(
            "onConnectFailed", MethodInfo.Parameter(Device::class.java, device),
            MethodInfo.Parameter(Int::class.javaPrimitiveType!!, failType)
        )
    }

    fun onConnectTimeout(device: Device?, type: Int): MethodInfo {
        return MethodInfo(
            "onConnectTimeout", MethodInfo.Parameter(Device::class.java, device),
            MethodInfo.Parameter(Int::class.javaPrimitiveType!!, type)
        )
    }

    fun onCharacteristicChanged(device: Device?, service: UUID?, characteristic: UUID?, value: ByteArray?): MethodInfo {
        return MethodInfo(
            "onCharacteristicChanged", MethodInfo.Parameter(Device::class.java, device),
            MethodInfo.Parameter(UUID::class.java, service), MethodInfo.Parameter(UUID::class.java, characteristic),
            MethodInfo.Parameter(ByteArray::class.java, value)
        )
    }

    fun onCharacteristicRead(request: Request?, value: ByteArray?): MethodInfo {
        return MethodInfo(
            "onCharacteristicRead", MethodInfo.Parameter(Request::class.java, request),
            MethodInfo.Parameter(ByteArray::class.java, value)
        )
    }

    fun onCharacteristicWrite(request: Request?, value: ByteArray?): MethodInfo {
        return MethodInfo(
            "onCharacteristicWrite", MethodInfo.Parameter(Request::class.java, request),
            MethodInfo.Parameter(ByteArray::class.java, value)
        )
    }

    fun onRssiRead(request: Request?, rssi: Int): MethodInfo {
        return MethodInfo(
            "onRssiRead", MethodInfo.Parameter(Request::class.java, request),
            MethodInfo.Parameter(Int::class.javaPrimitiveType!!, rssi)
        )
    }

    fun onDescriptorRead(request: Request?, value: ByteArray?): MethodInfo {
        return MethodInfo(
            "onDescriptorRead", MethodInfo.Parameter(Request::class.java, request),
            MethodInfo.Parameter(ByteArray::class.java, value)
        )
    }

    fun onNotificationChanged(request: Request?, isEnabled: Boolean): MethodInfo {
        return MethodInfo(
            "onNotificationChanged", MethodInfo.Parameter(Request::class.java, request),
            MethodInfo.Parameter(Boolean::class.javaPrimitiveType!!, isEnabled)
        )
    }

    fun onMtuChanged(request: Request?, mtu: Int): MethodInfo {
        return MethodInfo(
            "onMtuChanged", MethodInfo.Parameter(Request::class.java, request),
            MethodInfo.Parameter(Int::class.javaPrimitiveType!!, mtu)
        )
    }

    fun onPhyChange(request: Request?, txPhy: Int, rxPhy: Int): MethodInfo {
        return MethodInfo(
            "onPhyChange",
            MethodInfo.Parameter(Request::class.java, request),
            MethodInfo.Parameter(Int::class.javaPrimitiveType!!, txPhy),
            MethodInfo.Parameter(Int::class.javaPrimitiveType!!, rxPhy)
        )
    }

    fun onRequestFailed(request: Request?, failType: Int, value: Any?): MethodInfo {
        return MethodInfo(
            "onRequestFailed", MethodInfo.Parameter(Request::class.java, request),
            MethodInfo.Parameter(Int::class.javaPrimitiveType!!, failType), MethodInfo.Parameter(Any::class.java, value)
        )
    }
}
