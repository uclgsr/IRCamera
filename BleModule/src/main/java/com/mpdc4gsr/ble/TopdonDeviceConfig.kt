package com.mpdc4gsr.ble

class TopdonDeviceConfig private constructor(builder: Builder) {
    val deviceType: UnifiedBleManager.DeviceType?
    val thermalResolution: String?
    val thermalFrameRate: Double
    val temperatureRange: Int
    val minTemperature: Double
    val maxTemperature: Double
    val colorPalette: Int
    val environmentalSensors: Int
    val isTimestampEnabled: Boolean
    val isAutoReconnectEnabled: Boolean
    val connectionTimeout: Int
    val sessionId: String?
    val isCalibrationEnabled: Boolean
    val dataOutputFormat: Int

    init {
        this.deviceType = builder.deviceType
        this.thermalResolution = builder.thermalResolution
        this.thermalFrameRate = builder.thermalFrameRate
        this.temperatureRange = builder.temperatureRange
        this.minTemperature = builder.minTemperature
        this.maxTemperature = builder.maxTemperature
        this.colorPalette = builder.colorPalette
        this.environmentalSensors = builder.environmentalSensors
        this.isTimestampEnabled = builder.enableTimestamp
        this.isAutoReconnectEnabled = builder.enableAutoReconnect
        this.connectionTimeout = builder.connectionTimeout
        this.sessionId = builder.sessionId
        this.isCalibrationEnabled = builder.enableCalibration
        this.dataOutputFormat = builder.dataOutputFormat
    }

    override fun toString(): String {
        return "TopdonDeviceConfig{" +
                "deviceType=" + deviceType +
                ", thermalResolution='" + thermalResolution + '\'' +
                ", thermalFrameRate=" + thermalFrameRate +
                ", temperatureRange=" + temperatureRange +
                ", minTemperature=" + minTemperature +
                ", maxTemperature=" + maxTemperature +
                ", colorPalette=" + colorPalette +
                ", environmentalSensors=" + environmentalSensors +
                ", enableTimestamp=" + this.isTimestampEnabled +
                ", enableAutoReconnect=" + this.isAutoReconnectEnabled +
                ", connectionTimeout=" + connectionTimeout +
                ", sessionId='" + sessionId + '\'' +
                ", enableCalibration=" + this.isCalibrationEnabled +
                ", dataOutputFormat=" + dataOutputFormat +
                '}'
    }

    class Builder {
        private var deviceType: UnifiedBleManager.DeviceType? = UnifiedBleManager.DeviceType.MPDC4GSR_THERMAL
        private var thermalResolution: String? = RESOLUTION_256x192
        private var thermalFrameRate: Double = FRAME_RATE_9HZ
        private var temperatureRange: Int = TEMP_RANGE_MINUS_20_TO_120
        private var minTemperature = -20.0
        private var maxTemperature = 120.0
        private var colorPalette: Int = PALETTE_IRON
        private var environmentalSensors = 0
        private var enableTimestamp = true
        private var enableAutoReconnect = true
        private var connectionTimeout = 10000
        private var sessionId: String? = null
        private var enableCalibration = true
        private var dataOutputFormat = 0

        fun setDeviceType(deviceType: UnifiedBleManager.DeviceType?): Builder {
            this.deviceType = deviceType
            return this
        }

        fun setThermalResolution(resolution: String?): Builder {
            this.thermalResolution = resolution
            return this
        }

        fun setThermalFrameRate(frameRate: Double): Builder {
            this.thermalFrameRate = frameRate
            return this
        }

        fun setTemperatureRange(range: Int): Builder {
            this.temperatureRange = range
            return this
        }

        fun setCustomTemperatureRange(minTemp: Double, maxTemp: Double): Builder {
            this.temperatureRange = TEMP_RANGE_CUSTOM
            this.minTemperature = minTemp
            this.maxTemperature = maxTemp
            return this
        }

        fun setColorPalette(palette: Int): Builder {
            this.colorPalette = palette
            return this
        }

        fun setEnvironmentalSensors(sensors: Int): Builder {
            this.environmentalSensors = sensors
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

        fun enableCalibration(enable: Boolean): Builder {
            this.enableCalibration = enable
            return this
        }

        fun setDataOutputFormat(format: Int): Builder {
            this.dataOutputFormat = format
            return this
        }

        fun build(): TopdonDeviceConfig {
            return TopdonDeviceConfig(this)
        }
    }

    companion object {
        const val RESOLUTION_256x192: String = "256x192"
        const val RESOLUTION_384x288: String = "384x288"
        const val RESOLUTION_640x480: String = "640x480"

        const val FRAME_RATE_9HZ: Double = 9.0
        const val FRAME_RATE_15HZ: Double = 15.0
        const val FRAME_RATE_25HZ: Double = 25.0
        const val FRAME_RATE_30HZ: Double = 30.0

        const val TEMP_RANGE_MINUS_20_TO_120: Int = 0
        const val TEMP_RANGE_0_TO_250: Int = 1
        const val TEMP_RANGE_100_TO_450: Int = 2
        const val TEMP_RANGE_CUSTOM: Int = 3

        const val PALETTE_IRON: Int = 0
        const val PALETTE_RAINBOW: Int = 1
        const val PALETTE_GRAYSCALE: Int = 2
        const val PALETTE_HOT: Int = 3

        const val ENV_TEMPERATURE: Int = 1
        const val ENV_HUMIDITY: Int = 2
        const val ENV_PRESSURE: Int = 4
        val ENV_ALL: Int = ENV_TEMPERATURE or ENV_HUMIDITY or ENV_PRESSURE

        fun createDefaultThermalConfig(): TopdonDeviceConfig {
            return Builder()
                .setDeviceType(UnifiedBleManager.DeviceType.MPDC4GSR_THERMAL)
                .setThermalResolution(RESOLUTION_256x192)
                .setThermalFrameRate(FRAME_RATE_9HZ)
                .setTemperatureRange(TEMP_RANGE_MINUS_20_TO_120)
                .setColorPalette(PALETTE_IRON)
                .enableTimestamp(true)
                .enableAutoReconnect(true)
                .enableCalibration(true)
                .build()
        }

        fun createDefaultEnvironmentalConfig(): TopdonDeviceConfig {
            return Builder()
                .setDeviceType(UnifiedBleManager.DeviceType.MPDC4GSR_ENV)
                .setEnvironmentalSensors(ENV_ALL)
                .enableTimestamp(true)
                .enableAutoReconnect(true)
                .build()
        }

        fun createHighResThermalConfig(): TopdonDeviceConfig {
            return Builder()
                .setDeviceType(UnifiedBleManager.DeviceType.MPDC4GSR_THERMAL)
                .setThermalResolution(RESOLUTION_640x480)
                .setThermalFrameRate(FRAME_RATE_15HZ)
                .setTemperatureRange(TEMP_RANGE_0_TO_250)
                .setColorPalette(PALETTE_RAINBOW)
                .enableTimestamp(true)
                .enableAutoReconnect(true)
                .enableCalibration(true)
                .build()
        }

        fun createMultiSensorConfig(): TopdonDeviceConfig {
            return Builder()
                .setDeviceType(UnifiedBleManager.DeviceType.MPDC4GSR_MULTI)
                .setThermalResolution(RESOLUTION_384x288)
                .setThermalFrameRate(FRAME_RATE_15HZ)
                .setTemperatureRange(TEMP_RANGE_MINUS_20_TO_120)
                .setColorPalette(PALETTE_HOT)
                .setEnvironmentalSensors(ENV_ALL)
                .enableTimestamp(true)
                .enableAutoReconnect(true)
                .enableCalibration(true)
                .build()
        }
    }
}
