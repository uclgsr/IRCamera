package com.mpdc4gsr.ble.core

import android.bluetooth.BluetoothDevice
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.util.Pair
import java.util.UUID

class ConnectionConfiguration {
    val scanIntervalPairsInAutoReconnection: MutableList<Pair<Int?, Int?>?>
    private val defaultWriteOptionsMap: MutableMap<String?, WriteOptions?> = HashMap<String?, WriteOptions?>()
    var discoverServicesDelayMillis: Int = 600
    var connectTimeoutMillis: Int = 10000
    var requestTimeoutMillis: Int = 3000
    var tryReconnectMaxTimes: Int = TRY_RECONNECT_TIMES_INFINITE
    var reconnectImmediatelyMaxTimes: Int = 3
    var isAutoReconnect: Boolean = true

    @RequiresApi(Build.VERSION_CODES.M)
    var transport: Int = BluetoothDevice.TRANSPORT_LE

    @RequiresApi(Build.VERSION_CODES.O)
    var phy: Int = BluetoothDevice.PHY_LE_1M_MASK

    init {
        scanIntervalPairsInAutoReconnection = ArrayList<Pair<Int?, Int?>?>()
        scanIntervalPairsInAutoReconnection.add(Pair.create<Int?, Int?>(0, 2000))
        scanIntervalPairsInAutoReconnection.add(Pair.create<Int?, Int?>(1, 5000))
        scanIntervalPairsInAutoReconnection.add(Pair.create<Int?, Int?>(3, 10000))
        scanIntervalPairsInAutoReconnection.add(Pair.create<Int?, Int?>(5, 30000))
        scanIntervalPairsInAutoReconnection.add(Pair.create<Int?, Int?>(10, 60000))
    }

    fun setDiscoverServicesDelayMillis(discoverServicesDelayMillis: Int): ConnectionConfiguration {
        this.discoverServicesDelayMillis = discoverServicesDelayMillis
        return this
    }

    fun setConnectTimeoutMillis(connectTimeoutMillis: Int): ConnectionConfiguration {
        if (requestTimeoutMillis >= 1000) {
            this.connectTimeoutMillis = connectTimeoutMillis
        }
        return this
    }

    fun setRequestTimeoutMillis(requestTimeoutMillis: Int): ConnectionConfiguration {
        if (requestTimeoutMillis >= 1000) {
            this.requestTimeoutMillis = requestTimeoutMillis
        }
        return this
    }

    fun setTryReconnectMaxTimes(tryReconnectMaxTimes: Int): ConnectionConfiguration {
        this.tryReconnectMaxTimes = tryReconnectMaxTimes
        return this
    }

    fun setReconnectImmediatelyMaxTimes(reconnectImmediatelyMaxTimes: Int): ConnectionConfiguration {
        this.reconnectImmediatelyMaxTimes = reconnectImmediatelyMaxTimes
        return this
    }

    fun setAutoReconnect(autoReconnect: Boolean): ConnectionConfiguration {
        isAutoReconnect = autoReconnect
        return this
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun setTransport(transport: Int): ConnectionConfiguration {
        this.transport = transport
        return this
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun setPhy(phy: Int): ConnectionConfiguration {
        this.phy = phy
        return this
    }

    fun setScanIntervalPairsInAutoReconnection(parameters: MutableList<Pair<Int?, Int?>?>?): ConnectionConfiguration {
        Inspector.requireNonNull<MutableList<Pair<Int?, Int?>?>?>(parameters, "parameters can't be null")
        scanIntervalPairsInAutoReconnection.clear()
        scanIntervalPairsInAutoReconnection.addAll(parameters!!)
        return this
    }

    fun setDefaultWriteOptions(service: UUID?, characteristic: UUID?, options: WriteOptions?): ConnectionConfiguration {
        Inspector.requireNonNull<UUID?>(service, "service can't be null")
        Inspector.requireNonNull<UUID?>(characteristic, "characteristic can't be null")
        Inspector.requireNonNull<WriteOptions?>(options, "options can't be null")
        defaultWriteOptionsMap.put(service.toString() + ":" + characteristic, options)
        return this
    }

    fun getDefaultWriteOptions(service: UUID?, characteristic: UUID?): WriteOptions? {
        return defaultWriteOptionsMap.get(service.toString() + ":" + characteristic)
    }

    companion object {
        val TRY_RECONNECT_TIMES_INFINITE: Int = -1
    }
}