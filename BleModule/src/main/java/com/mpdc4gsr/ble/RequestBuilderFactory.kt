package com.mpdc4gsr.ble

import android.os.Build
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi
import com.mpdc4gsr.ble.callback.MtuChangeCallback
import com.mpdc4gsr.ble.callback.NotificationChangeCallback
import com.mpdc4gsr.ble.callback.PhyChangeCallback
import com.mpdc4gsr.ble.callback.ReadCharacteristicCallback
import com.mpdc4gsr.ble.callback.ReadRssiCallback
import java.util.UUID

class RequestBuilderFactory {
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun getChangeMtuBuilder(@IntRange(from = 23, to = 517) mtu: Int): RequestBuilder<MtuChangeCallback?> {
        var mtu = mtu
        if (mtu < 23) {
            mtu = 23
        } else if (mtu > 517) {
            mtu = 517
        }
        val builder = RequestBuilder<MtuChangeCallback?>(RequestType.CHANGE_MTU)
        builder.value = mtu
        return builder
    }

    fun getReadCharacteristicBuilder(
        service: UUID?,
        characteristic: UUID?
    ): RequestBuilder<ReadCharacteristicCallback?> {
        val builder = RequestBuilder<ReadCharacteristicCallback?>(RequestType.READ_CHARACTERISTIC)
        builder.service = service
        builder.characteristic = characteristic
        return builder
    }

    fun getSetNotificationBuilder(
        service: UUID?, characteristic: UUID?,
        enable: Boolean
    ): RequestBuilder<NotificationChangeCallback?> {
        val builder = RequestBuilder<NotificationChangeCallback?>(RequestType.SET_NOTIFICATION)
        builder.service = service
        builder.characteristic = characteristic
        builder.value = if (enable) 1 else 0
        return builder
    }

    fun getSetIndicationBuilder(
        service: UUID?, characteristic: UUID?,
        enable: Boolean
    ): RequestBuilder<NotificationChangeCallback?> {
        val builder = RequestBuilder<NotificationChangeCallback?>(RequestType.SET_INDICATION)
        builder.service = service
        builder.characteristic = characteristic
        builder.value = if (enable) 1 else 0
        return builder
    }

    fun getReadDescriptorBuilder(
        service: UUID?, characteristic: UUID?,
        descriptor: UUID?
    ): RequestBuilder<NotificationChangeCallback?> {
        val builder = RequestBuilder<NotificationChangeCallback?>(RequestType.READ_DESCRIPTOR)
        builder.service = service
        builder.characteristic = characteristic
        builder.descriptor = descriptor
        return builder
    }

    fun getWriteCharacteristicBuilder(
        service: UUID?, characteristic: UUID?,
        value: ByteArray?
    ): WriteCharacteristicBuilder {
        Inspector.requireNonNull<ByteArray?>(value, "value can't be null")
        val builder = WriteCharacteristicBuilder()
        builder.service = service
        builder.characteristic = characteristic
        builder.value = value
        return builder
    }

    val readRssiBuilder: RequestBuilder<ReadRssiCallback?>
        get() = RequestBuilder<ReadRssiCallback?>(RequestType.READ_RSSI)

    @get:RequiresApi(Build.VERSION_CODES.O)
    val readPhyBuilder: RequestBuilder<PhyChangeCallback?>
        get() = RequestBuilder<PhyChangeCallback?>(RequestType.READ_PHY)

    @RequiresApi(Build.VERSION_CODES.O)
    fun getSetPreferredPhyBuilder(txPhy: Int, rxPhy: Int, phyOptions: Int): RequestBuilder<PhyChangeCallback?> {
        val builder = RequestBuilder<PhyChangeCallback?>(RequestType.SET_PREFERRED_PHY)
        builder.value = intArrayOf(txPhy, rxPhy, phyOptions)
        return builder
    }
}
