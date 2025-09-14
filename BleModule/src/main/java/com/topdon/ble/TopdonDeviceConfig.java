package com.topdon.ble;


public class TopdonDeviceConfig {
    
    // Thermal camera resolutions
    public static final String RESOLUTION_256x192 = "256x192";
    public static final String RESOLUTION_384x288 = "384x288";
    public static final String RESOLUTION_640x480 = "640x480";
    
    // Thermal frame rates (Hz)
    public static final double FRAME_RATE_9HZ = 9.0;
    public static final double FRAME_RATE_15HZ = 15.0;
    public static final double FRAME_RATE_25HZ = 25.0;
    public static final double FRAME_RATE_30HZ = 30.0;
    
    // Temperature ranges (Celsius)
    public static final int TEMP_RANGE_MINUS_20_TO_120 = 0;
    public static final int TEMP_RANGE_0_TO_250 = 1;
    public static final int TEMP_RANGE_100_TO_450 = 2;
    public static final int TEMP_RANGE_CUSTOM = 3;
    
    // Thermal color palettes
    public static final int PALETTE_IRON = 0;
    public static final int PALETTE_RAINBOW = 1;
    public static final int PALETTE_GRAYSCALE = 2;
    public static final int PALETTE_HOT = 3;
    
    // Environmental sensor types
    public static final int ENV_TEMPERATURE = 1;
    public static final int ENV_HUMIDITY = 2;
    public static final int ENV_PRESSURE = 4;
    public static final int ENV_ALL = ENV_TEMPERATURE | ENV_HUMIDITY | ENV_PRESSURE;
    
    // Configuration parameters
    private final UnifiedBleManager.DeviceType deviceType;
    private final String thermalResolution;
    private final double thermalFrameRate;
    private final int temperatureRange;
    private final double minTemperature;
    private final double maxTemperature;
    private final int colorPalette;
    private final int environmentalSensors;
    private final boolean enableTimestamp;
    private final boolean enableAutoReconnect;
    private final int connectionTimeout;
    private final String sessionId;
    private final boolean enableCalibration;
    private final int dataOutputFormat;
    
    private TopdonDeviceConfig(Builder builder) {
        this.deviceType = builder.deviceType;
        this.thermalResolution = builder.thermalResolution;
        this.thermalFrameRate = builder.thermalFrameRate;
        this.temperatureRange = builder.temperatureRange;
        this.minTemperature = builder.minTemperature;
        this.maxTemperature = builder.maxTemperature;
        this.colorPalette = builder.colorPalette;
        this.environmentalSensors = builder.environmentalSensors;
        this.enableTimestamp = builder.enableTimestamp;
        this.enableAutoReconnect = builder.enableAutoReconnect;
        this.connectionTimeout = builder.connectionTimeout;
        this.sessionId = builder.sessionId;
        this.enableCalibration = builder.enableCalibration;
        this.dataOutputFormat = builder.dataOutputFormat;
    }
    
    // Getters
    public UnifiedBleManager.DeviceType getDeviceType() { return deviceType; }
    public String getThermalResolution() { return thermalResolution; }
    public double getThermalFrameRate() { return thermalFrameRate; }
    public int getTemperatureRange() { return temperatureRange; }
    public double getMinTemperature() { return minTemperature; }
    public double getMaxTemperature() { return maxTemperature; }
    public int getColorPalette() { return colorPalette; }
    public int getEnvironmentalSensors() { return environmentalSensors; }
    public boolean isTimestampEnabled() { return enableTimestamp; }
    public boolean isAutoReconnectEnabled() { return enableAutoReconnect; }
    public int getConnectionTimeout() { return connectionTimeout; }
    public String getSessionId() { return sessionId; }
    public boolean isCalibrationEnabled() { return enableCalibration; }
    public int getDataOutputFormat() { return dataOutputFormat; }
    

    public static class Builder {
        private UnifiedBleManager.DeviceType deviceType = UnifiedBleManager.DeviceType.TOPDON_THERMAL;
        private String thermalResolution = RESOLUTION_256x192;
        private double thermalFrameRate = FRAME_RATE_9HZ;
        private int temperatureRange = TEMP_RANGE_MINUS_20_TO_120;
        private double minTemperature = -20.0;
        private double maxTemperature = 120.0;
        private int colorPalette = PALETTE_IRON;
        private int environmentalSensors = 0;
        private boolean enableTimestamp = true;
        private boolean enableAutoReconnect = true;
        private int connectionTimeout = 10000; // 10 seconds
        private String sessionId = null;
        private boolean enableCalibration = true;
        private int dataOutputFormat = 0; // 0=CSV, 1=Binary, 2=JSON
        
        public Builder setDeviceType(UnifiedBleManager.DeviceType deviceType) {
            this.deviceType = deviceType;
            return this;
        }
        
        public Builder setThermalResolution(String resolution) {
            this.thermalResolution = resolution;
            return this;
        }
        
        public Builder setThermalFrameRate(double frameRate) {
            this.thermalFrameRate = frameRate;
            return this;
        }
        
        public Builder setTemperatureRange(int range) {
            this.temperatureRange = range;
            return this;
        }
        
        public Builder setCustomTemperatureRange(double minTemp, double maxTemp) {
            this.temperatureRange = TEMP_RANGE_CUSTOM;
            this.minTemperature = minTemp;
            this.maxTemperature = maxTemp;
            return this;
        }
        
        public Builder setColorPalette(int palette) {
            this.colorPalette = palette;
            return this;
        }
        
        public Builder setEnvironmentalSensors(int sensors) {
            this.environmentalSensors = sensors;
            return this;
        }
        
        public Builder enableTimestamp(boolean enable) {
            this.enableTimestamp = enable;
            return this;
        }
        
        public Builder enableAutoReconnect(boolean enable) {
            this.enableAutoReconnect = enable;
            return this;
        }
        
        public Builder setConnectionTimeout(int timeout) {
            this.connectionTimeout = timeout;
            return this;
        }
        
        public Builder setSessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }
        
        public Builder enableCalibration(boolean enable) {
            this.enableCalibration = enable;
            return this;
        }
        
        public Builder setDataOutputFormat(int format) {
            this.dataOutputFormat = format;
            return this;
        }
        
        public TopdonDeviceConfig build() {
            return new TopdonDeviceConfig(this);
        }
    }
    

    public static TopdonDeviceConfig createDefaultThermalConfig() {
        return new Builder()
            .setDeviceType(UnifiedBleManager.DeviceType.TOPDON_THERMAL)
            .setThermalResolution(RESOLUTION_256x192)
            .setThermalFrameRate(FRAME_RATE_9HZ)
            .setTemperatureRange(TEMP_RANGE_MINUS_20_TO_120)
            .setColorPalette(PALETTE_IRON)
            .enableTimestamp(true)
            .enableAutoReconnect(true)
            .enableCalibration(true)
            .build();
    }
    

    public static TopdonDeviceConfig createDefaultEnvironmentalConfig() {
        return new Builder()
            .setDeviceType(UnifiedBleManager.DeviceType.TOPDON_ENV)
            .setEnvironmentalSensors(ENV_ALL)
            .enableTimestamp(true)
            .enableAutoReconnect(true)
            .build();
    }
    

    public static TopdonDeviceConfig createHighResThermalConfig() {
        return new Builder()
            .setDeviceType(UnifiedBleManager.DeviceType.TOPDON_THERMAL)
            .setThermalResolution(RESOLUTION_640x480)
            .setThermalFrameRate(FRAME_RATE_15HZ)
            .setTemperatureRange(TEMP_RANGE_0_TO_250)
            .setColorPalette(PALETTE_RAINBOW)
            .enableTimestamp(true)
            .enableAutoReconnect(true)
            .enableCalibration(true)
            .build();
    }
    

    public static TopdonDeviceConfig createMultiSensorConfig() {
        return new Builder()
            .setDeviceType(UnifiedBleManager.DeviceType.TOPDON_MULTI)
            .setThermalResolution(RESOLUTION_384x288)
            .setThermalFrameRate(FRAME_RATE_15HZ)
            .setTemperatureRange(TEMP_RANGE_MINUS_20_TO_120)
            .setColorPalette(PALETTE_HOT)
            .setEnvironmentalSensors(ENV_ALL)
            .enableTimestamp(true)
            .enableAutoReconnect(true)
            .enableCalibration(true)
            .build();
    }
    
    @Override
    public String toString() {
        return "TopdonDeviceConfig{" +
               "deviceType=" + deviceType +
               ", thermalResolution='" + thermalResolution + '\'' +
               ", thermalFrameRate=" + thermalFrameRate +
               ", temperatureRange=" + temperatureRange +
               ", minTemperature=" + minTemperature +
               ", maxTemperature=" + maxTemperature +
               ", colorPalette=" + colorPalette +
               ", environmentalSensors=" + environmentalSensors +
               ", enableTimestamp=" + enableTimestamp +
               ", enableAutoReconnect=" + enableAutoReconnect +
               ", connectionTimeout=" + connectionTimeout +
               ", sessionId='" + sessionId + '\'' +
               ", enableCalibration=" + enableCalibration +
               ", dataOutputFormat=" + dataOutputFormat +
               '}';
    }
}