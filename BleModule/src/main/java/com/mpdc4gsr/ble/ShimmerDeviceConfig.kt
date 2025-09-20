package com.mpdc4gsr.ble

class ShimmerDeviceConfig private constructor(builder: Builder) {
    val deviceType: UnifiedBleManager.DeviceType?
    val samplingRate: Int
    val isGSREnabled: Boolean
    val isPPGEnabled: Boolean
    val isIMUEnabled: Boolean
    val gSRRange: Int
    val pPGMode: Int
    val isTimestampEnabled: Boolean
    val isAutoReconnectEnabled: Boolean
    val connectionTimeout: Int
    val sessionId: String?

    init {
        this.deviceType = builder.deviceType
        this.samplingRate = builder.samplingRate
        this.isGSREnabled = builder.enableGSR
        this.isPPGEnabled = builder.enablePPG
        this.isIMUEnabled = builder.enableIMU
        this.gSRRange = builder.gsrRange
        this.pPGMode = builder.ppgMode
        this.isTimestampEnabled = builder.enableTimestamp
        this.isAutoReconnectEnabled = builder.enableAutoReconnect
        this.connectionTimeout = builder.connectionTimeout
        this.sessionId = builder.sessionId
    }

    override fun toString(): String {
        return "ShimmerDeviceConfig{" +
                "deviceType=" + deviceType +
                ", samplingRate=" + samplingRate +
                ", enableGSR=" + this.isGSREnabled +
                ", enablePPG=" + this.isPPGEnabled +
                ", enableIMU=" + this.isIMUEnabled +
                ", gsrRange=" + this.gSRRange +
                ", ppgMode=" + this.pPGMode +
                ", enableTimestamp=" + this.isTimestampEnabled +
                ", enableAutoReconnect=" + this.isAutoReconnectEnabled +
                ", connectionTimeout=" + connectionTimeout +
                ", sessionId='" + sessionId + '\'' +
                '}'
    }

    class Builder {
        private var deviceType: UnifiedBleManager.DeviceType? = UnifiedBleManager.DeviceType.SHIMMER_GSR
        private var samplingRate: Int = SAMPLING_RATE_128HZ
        private var enableGSR = true
        private var enablePPG = false
        private var enableIMU = false
        private var gsrRange: Int = GSR_RANGE_AUTO
        private var ppgMode: Int = PPG_GREEN
        private var enableTimestamp = true
        private var enableAutoReconnect = true
        private var connectionTimeout = 10000
        private var sessionId: String? = null

        fun setDeviceType(deviceType: UnifiedBleManager.DeviceType?): Builder {
            this.deviceType = deviceType
            return this
        }

        fun setSamplingRate(samplingRate: Int): Builder {
            this.samplingRate = samplingRate
            return this
        }

        fun enableGSR(enable: Boolean): Builder {
            this.enableGSR = enable
            return this
        }

        fun enablePPG(enable: Boolean): Builder {
            this.enablePPG = enable
            return this
        }

        fun enableIMU(enable: Boolean): Builder {
            this.enableIMU = enable
            return this
        }

        fun setGSRRange(range: Int): Builder {
            this.gsrRange = range
            return this
        }

        fun setPPGMode(mode: Int): Builder {
            this.ppgMode = mode
            return this
        }

        fun enableTimestamp(enable: Boolean): Builder {
            this.enableTimestamp = enable
            return this
        }

        fun enableAutoReconnect(enable: Boolean): Builder {
            this.enableAutoReconnect = enable
            return this
        }

        fun setConnectionTimeout(timeout: Int): Builder {
            this.connectionTimeout = timeout
            return this
        }

        fun setSessionId(sessionId: String?): Builder {
            this.sessionId = sessionId
            return this
        }

        fun build(): ShimmerDeviceConfig {
            return ShimmerDeviceConfig(this)
        }
    }

    companion object {
        const val SAMPLING_RATE_32HZ: Int = 32
        const val SAMPLING_RATE_64HZ: Int = 64
        const val SAMPLING_RATE_128HZ: Int = 128
        const val SAMPLING_RATE_256HZ: Int = 256
        const val SAMPLING_RATE_512HZ: Int = 512

        const val GSR_RANGE_AUTO: Int = 0
        const val GSR_RANGE_10KOHM_56KOHM: Int = 1
        const val GSR_RANGE_56KOHM_220KOHM: Int = 2
        const val GSR_RANGE_220KOHM_680KOHM: Int = 3
        const val GSR_RANGE_680KOHM_4_7MOHM: Int = 4

        const val PPG_GREEN: Int = 0
        const val PPG_RED: Int = 1
        const val PPG_INFRARED: Int = 2

        fun createDefaultGSRConfig(): ShimmerDeviceConfig {
            return Builder()
                .setDeviceType(UnifiedBleManager.DeviceType.SHIMMER_GSR)
                .setSamplingRate(SAMPLING_RATE_128HZ)
                .enableGSR(true)
                .enablePPG(false)
                .enableIMU(false)
                .setGSRRange(GSR_RANGE_AUTO)
                .enableTimestamp(true)
                .enableAutoReconnect(true)
                .build()
        }

        fun createDefaultPPGConfig(): ShimmerDeviceConfig {
            return Builder()
                .setDeviceType(UnifiedBleManager.DeviceType.SHIMMER_PPG)
                .setSamplingRate(SAMPLING_RATE_64HZ)
                .enableGSR(false)
                .enablePPG(true)
                .enableIMU(false)
                .setPPGMode(PPG_GREEN)
                .enableTimestamp(true)
                .enableAutoReconnect(true)
                .build()
        }

        fun createMultiSensorConfig(): ShimmerDeviceConfig {
            return Builder()
                .setDeviceType(UnifiedBleManager.DeviceType.SHIMMER_GSR)
                .setSamplingRate(SAMPLING_RATE_128HZ)
                .enableGSR(true)
                .enablePPG(true)
                .enableIMU(true)
                .setGSRRange(GSR_RANGE_AUTO)
                .setPPGMode(PPG_GREEN)
                .enableTimestamp(true)
                .enableAutoReconnect(true)
                .build()
        }
    }
}
