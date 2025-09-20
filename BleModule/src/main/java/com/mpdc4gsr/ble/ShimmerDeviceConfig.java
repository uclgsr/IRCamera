package com.mpdc4gsr.ble;

public class ShimmerDeviceConfig {

    public static final int SAMPLING_RATE_32HZ = 32;
    public static final int SAMPLING_RATE_64HZ = 64;
    public static final int SAMPLING_RATE_128HZ = 128;
    public static final int SAMPLING_RATE_256HZ = 256;
    public static final int SAMPLING_RATE_512HZ = 512;

    public static final int GSR_RANGE_AUTO = 0;
    public static final int GSR_RANGE_10KOHM_56KOHM = 1;
    public static final int GSR_RANGE_56KOHM_220KOHM = 2;
    public static final int GSR_RANGE_220KOHM_680KOHM = 3;
    public static final int GSR_RANGE_680KOHM_4_7MOHM = 4;

    public static final int PPG_GREEN = 0;
    public static final int PPG_RED = 1;
    public static final int PPG_INFRARED = 2;

    private final UnifiedBleManager.DeviceType deviceType;
    private final int samplingRate;
    private final boolean enableGSR;
    private final boolean enablePPG;
    private final boolean enableIMU;
    private final int gsrRange;
    private final int ppgMode;
    private final boolean enableTimestamp;
    private final boolean enableAutoReconnect;
    private final int connectionTimeout;
    private final String sessionId;

    private ShimmerDeviceConfig(Builder builder) {
        this.deviceType = builder.deviceType;
        this.samplingRate = builder.samplingRate;
        this.enableGSR = builder.enableGSR;
        this.enablePPG = builder.enablePPG;
        this.enableIMU = builder.enableIMU;
        this.gsrRange = builder.gsrRange;
        this.ppgMode = builder.ppgMode;
        this.enableTimestamp = builder.enableTimestamp;
        this.enableAutoReconnect = builder.enableAutoReconnect;
        this.connectionTimeout = builder.connectionTimeout;
        this.sessionId = builder.sessionId;
    }

    public static ShimmerDeviceConfig createDefaultGSRConfig() {
        return new Builder()
                .setDeviceType(UnifiedBleManager.DeviceType.SHIMMER_GSR)
                .setSamplingRate(SAMPLING_RATE_128HZ)
                .enableGSR(true)
                .enablePPG(false)
                .enableIMU(false)
                .setGSRRange(GSR_RANGE_AUTO)
                .enableTimestamp(true)
                .enableAutoReconnect(true)
                .build();
    }

    public static ShimmerDeviceConfig createDefaultPPGConfig() {
        return new Builder()
                .setDeviceType(UnifiedBleManager.DeviceType.SHIMMER_PPG)
                .setSamplingRate(SAMPLING_RATE_64HZ)
                .enableGSR(false)
                .enablePPG(true)
                .enableIMU(false)
                .setPPGMode(PPG_GREEN)
                .enableTimestamp(true)
                .enableAutoReconnect(true)
                .build();
    }

    public static ShimmerDeviceConfig createMultiSensorConfig() {
        return new Builder()
                .setDeviceType(UnifiedBleManager.DeviceType.SHIMMER_GSR)
                .setSamplingRate(SAMPLING_RATE_128HZ)
                .enableGSR(true)
                .enablePPG(true)
                .enableIMU(true)
                .setGSRRange(GSR_RANGE_AUTO)
                .setPPGMode(PPG_GREEN)
                .enableTimestamp(true)
                .enableAutoReconnect(true)
                .build();
    }

    public UnifiedBleManager.DeviceType getDeviceType() {
        return deviceType;
    }

    public int getSamplingRate() {
        return samplingRate;
    }

    public boolean isGSREnabled() {
        return enableGSR;
    }

    public boolean isPPGEnabled() {
        return enablePPG;
    }

    public boolean isIMUEnabled() {
        return enableIMU;
    }

    public int getGSRRange() {
        return gsrRange;
    }

    public int getPPGMode() {
        return ppgMode;
    }

    public boolean isTimestampEnabled() {
        return enableTimestamp;
    }

    public boolean isAutoReconnectEnabled() {
        return enableAutoReconnect;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public String getSessionId() {
        return sessionId;
    }

    @Override
    public String toString() {
        return "ShimmerDeviceConfig{" +
                "deviceType=" + deviceType +
                ", samplingRate=" + samplingRate +
                ", enableGSR=" + enableGSR +
                ", enablePPG=" + enablePPG +
                ", enableIMU=" + enableIMU +
                ", gsrRange=" + gsrRange +
                ", ppgMode=" + ppgMode +
                ", enableTimestamp=" + enableTimestamp +
                ", enableAutoReconnect=" + enableAutoReconnect +
                ", connectionTimeout=" + connectionTimeout +
                ", sessionId='" + sessionId + '\'' +
                '}';
    }

    public static class Builder {
        private UnifiedBleManager.DeviceType deviceType = UnifiedBleManager.DeviceType.SHIMMER_GSR;
        private int samplingRate = SAMPLING_RATE_128HZ;
        private boolean enableGSR = true;
        private boolean enablePPG = false;
        private boolean enableIMU = false;
        private int gsrRange = GSR_RANGE_AUTO;
        private int ppgMode = PPG_GREEN;
        private boolean enableTimestamp = true;
        private boolean enableAutoReconnect = true;
        private int connectionTimeout = 10000;
        private String sessionId = null;

        public Builder setDeviceType(UnifiedBleManager.DeviceType deviceType) {
            this.deviceType = deviceType;
            return this;
        }

        public Builder setSamplingRate(int samplingRate) {
            this.samplingRate = samplingRate;
            return this;
        }

        public Builder enableGSR(boolean enable) {
            this.enableGSR = enable;
            return this;
        }

        public Builder enablePPG(boolean enable) {
            this.enablePPG = enable;
            return this;
        }

        public Builder enableIMU(boolean enable) {
            this.enableIMU = enable;
            return this;
        }

        public Builder setGSRRange(int range) {
            this.gsrRange = range;
            return this;
        }

        public Builder setPPGMode(int mode) {
            this.ppgMode = mode;
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

        public ShimmerDeviceConfig build() {
            return new ShimmerDeviceConfig(this);
        }
    }
}
