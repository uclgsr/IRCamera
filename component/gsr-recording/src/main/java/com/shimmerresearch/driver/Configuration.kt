package com.shimmerresearch.driver

class Configuration {
    companion object {

        const val SHIMMER3 = 2

        const val SENSOR_GSR = 0x10
        const val SENSOR_ACCEL = 0x80
        const val SENSOR_GYRO = 0x40
        const val SENSOR_MAG = 0x20
        const val SENSOR_BATTERY = 0x01

        const val GSR_RANGE_10KOHM_56KOHM = 0
        const val GSR_RANGE_56KOHM_220KOHM = 1
        const val GSR_RANGE_220KOHM_680KOHM = 2
        const val GSR_RANGE_680KOHM_4_7MOHM = 3
        const val GSR_RANGE_AUTO = 4

        val SAMPLING_RATES =
            doubleArrayOf(
                32.768,
                65.536,
                128.0,
                256.0,
                512.0,
                1024.0,
            )

        fun getDefaultGSRConfiguration(): Configuration {
            return Configuration().apply {
                samplingRate = 128.0
                enabledSensors = SENSOR_GSR.toLong()
                gsrRange = GSR_RANGE_AUTO
                deviceType = SHIMMER3
            }
        }
    }

    var samplingRate: Double = 128.0
    var enabledSensors: Long = SENSOR_GSR.toLong()
    var gsrRange: Int = GSR_RANGE_AUTO
    var deviceType: Int = SHIMMER3
    var firmwareVersion: String = "1.0.0"
    var hardwareVersion: String = "3.0"

    fun toConfigurationBytes(): ByteArray {
        val config = ByteArray(12)

        val rateIndex = SAMPLING_RATES.indexOfFirst { it == samplingRate }
        config[0] = if (rateIndex >= 0) rateIndex.toByte() else 2.toByte() // Default to 128Hz

        config[1] = enabledSensors.toByte()
        config[2] = (enabledSensors shr 8).toByte()

        config[3] = gsrRange.toByte()

        config[4] = deviceType.toByte()

        return config
    }

    fun fromConfigurationBytes(config: ByteArray) {
        if (config.size >= 5) {
            val rateIndex = config[0].toInt()
            if (rateIndex >= 0 && rateIndex < SAMPLING_RATES.size) {
                samplingRate = SAMPLING_RATES[rateIndex]
            }

            enabledSensors = config[1].toLong() or (config[2].toLong() shl 8)
            gsrRange = config[3].toInt()
            deviceType = config[4].toInt()
        }
    }

    fun isValid(): Boolean {
        val validSamplingRate = SAMPLING_RATES.any { it == samplingRate }
        val validGSRRange = gsrRange >= GSR_RANGE_10KOHM_56KOHM && gsrRange <= GSR_RANGE_AUTO
        val validDeviceType = deviceType == SHIMMER3

        return validSamplingRate && validGSRRange && validDeviceType
    }

    override fun toString(): String {
        return "Configuration(samplingRate=${samplingRate}Hz, " +
                "enabledSensors=0x${enabledSensors.toString(16)}, " +
                "gsrRange=$gsrRange, deviceType=$deviceType)"
    }
}
