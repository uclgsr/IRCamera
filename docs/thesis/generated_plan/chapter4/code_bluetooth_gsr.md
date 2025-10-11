# Chapter 4: Bluetooth GSR Connection and Reading

        ## Code Snippet 4.1: Shimmer3 Streaming Pipeline

        ```kotlin
            fun startStreaming(macAddress: String) {
    scope.launch {
        devicesMutex.withLock {
            val shimmer = getShimmer(macAddress) ?: return@withLock
            shimmer.startStreaming()
            updateConnectionState(macAddress, ConnectionState.RECORDING)
        }
    }
}

private inner class ShimmerMsgHandler(looper: Looper) : Handler(looper) {
    override fun handleMessage(msg: Message) {
        when (msg.what) {
            ShimmerBluetooth.MSG_IDENTIFIER_STATE_CHANGE -> handleStateChange(msg.obj)
            ShimmerBluetooth.MSG_IDENTIFIER_DATA_PACKET -> handleData(msg.obj as? ObjectCluster)
            ShimmerBluetooth.MSG_IDENTIFIER_NOTIFICATION_MESSAGE -> handleNotification(msg)
        }
    }

    private fun handleStateChange(payload: Any?) {
        val (stateName, mac) =
            when (payload) {
                is ObjectCluster -> payload.mState?.toString() to payload.macAddress
                is CallbackObject -> payload.mState?.toString() to payload.mBluetoothAddress
                else -> return
            }
        val address = mac?.takeIf { it.isNotBlank() } ?: return
        when (stateName?.uppercase()) {
            "CONNECTED" -> {
                getShimmer(address)
                onDeviceConnected(address)
            }
            "CONNECTING" -> updateConnectionState(address, ConnectionState.CONNECTING)
            "DISCONNECTED", "CONNECTION_LOST", "NONE" -> {
                connectedDevices.remove(address)
                onDeviceDisconnected(address)
            }
            "FAILED_TO_CONNECT" -> {
                connectedDevices.remove(address)
                updateConnectionState(address, ConnectionState.ERROR)
            }
            "CONNECTION_TIMEOUT" -> updateConnectionState(address, ConnectionState.ERROR)
            else -> Unit
        }
    }

    private fun handleData(cluster: ObjectCluster?) {
        cluster ?: return
        val mac = cluster.macAddress ?: return
        val sample = mapCluster(mac, cluster)
        if (sample != null) {
            scope.launch { _samples.emit(sample) }
            _telemetry.value =
                _telemetry.value.toMutableMap().apply {
                    this[mac] =
                        TelemetryState(
                            gsrMicrosiemens = sample.gsrMicrosiemens.toFloat(),
                            skinTemperatureCelsius = sample.skinTemperatureCelsius?.toFloat(),
                            thermalSpotCelsius = null,
                            audioLevelDb = null,
                            frameRate = null,
                            droppedFrames = 0,
                            batteryPercent = _devices.value[mac]?.batteryPercent,
                            rssi = sample.connectionRssi,
                        )
                }
        }
    }

    private fun handleNotification(msg: Message) {
        val payload = msg.obj as? CallbackObject ?: return
        val mac = payload.mBluetoothAddress ?: return
        when (msg.arg1) {
            ShimmerBluetooth.NOTIFICATION_SHIMMER_FULLY_INITIALIZED -> onDeviceConnected(mac)
            ShimmerBluetooth.NOTIFICATION_SHIMMER_STOP_STREAMING -> updateConnectionState(mac, ConnectionState.READY)
            ShimmerBluetooth.NOTIFICATION_SHIMMER_START_STREAMING -> updateConnectionState(mac, ConnectionState.RECORDING)
        }
    }
}

private fun mapCluster(deviceId: String, cluster: ObjectCluster): GsrSample? {
    val sensorNames = cluster.mSensorNames ?: return null
    val calibrated = cluster.mCalData ?: return null
    val uncalibrated = cluster.mUncalData

    val gsrIndex = sensorNames.indexOfFirst { it.contains("GSR", ignoreCase = true) }
    if (gsrIndex == -1 || gsrIndex !in calibrated.indices) return null

    val gsrValue = calibrated[gsrIndex]
    val rawValue =
        if (uncalibrated != null && gsrIndex in uncalibrated.indices) uncalibrated[gsrIndex].toInt()
        else null
    val tempIndex = sensorNames.indexOfFirst { it.contains("TEMP", ignoreCase = true) }
    val skinTemperature =
        if (tempIndex != -1 && tempIndex in calibrated.indices) calibrated[tempIndex] else null

    val timestamp = clock.nowInstant().toEpochMilli()
    val resistance = if (gsrValue > 0.0) 1_000_000.0 / gsrValue else null

    return GsrSample(
        deviceId = deviceId,
        timestampMillis = timestamp,
        gsrMicrosiemens = gsrValue,
        gsrRaw = rawValue,
        qualityScore = 0.9,
        connectionRssi = null,
        resistanceOhms = resistance,
        skinTemperatureCelsius = skinTemperature,
        sequenceNumber = sequenceCounter.incrementAndGet(),
    )
}
        ```
