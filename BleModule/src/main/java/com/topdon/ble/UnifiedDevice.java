package com.topdon.ble;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface UnifiedDevice {

    @NonNull
    BluetoothDevice getBluetoothDevice();

    @NonNull
    UnifiedBleManager.DeviceType getDeviceType();

    @NonNull
    String getAddress();

    @Nullable
    String getName();

    boolean isConnected();

    void connect();

    void disconnect();

    boolean startDataStreaming();

    boolean stopDataStreaming();

    boolean sendCommand(@NonNull byte[] command);

    @NonNull
    ConnectionState getConnectionState();

    int getRssi();

    @NonNull
    DeviceInfo getDeviceInfo();

    void setConnectionListener(@Nullable UnifiedBleManager.UnifiedConnectionListener listener);


    @NonNull
    default String getDeviceId() {
        return getAddress();
    }

    @NonNull
    default String getDeviceName() {
        String name = getName();
        return name != null ? name : "Unknown Device";
    }

    default boolean startRecording(long timestamp) {
        return startDataStreaming();
    }

    default boolean stopRecording(long timestamp) {
        return stopDataStreaming();
    }

    default boolean addSyncMark(long timestamp) {

        byte[] syncCommand = new byte[]{0x00, 0x01}; 
        return sendCommand(syncCommand);
    }

    enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
        ERROR
    }

    class DeviceInfo {
        private final String deviceName;
        private final String deviceAddress;
        private final UnifiedBleManager.DeviceType deviceType;
        private final String hardwareVersion;
        private final String firmwareVersion;
        private final String serialNumber;

        public DeviceInfo(String deviceName, String deviceAddress,
                          UnifiedBleManager.DeviceType deviceType,
                          String hardwareVersion, String firmwareVersion,
                          String serialNumber) {
            this.deviceName = deviceName;
            this.deviceAddress = deviceAddress;
            this.deviceType = deviceType;
            this.hardwareVersion = hardwareVersion;
            this.firmwareVersion = firmwareVersion;
            this.serialNumber = serialNumber;
        }

        public String getDeviceName() {
            return deviceName;
        }

        public String getDeviceAddress() {
            return deviceAddress;
        }

        public UnifiedBleManager.DeviceType getDeviceType() {
            return deviceType;
        }

        public String getHardwareVersion() {
            return hardwareVersion;
        }

        public String getFirmwareVersion() {
            return firmwareVersion;
        }

        public String getSerialNumber() {
            return serialNumber;
        }

        @Override
        public String toString() {
            return "DeviceInfo{" +
                    "name='" + deviceName + '\'' +
                    ", address='" + deviceAddress + '\'' +
                    ", type=" + deviceType +
                    ", hw='" + hardwareVersion + '\'' +
                    ", fw='" + firmwareVersion + '\'' +
                    ", sn='" + serialNumber + '\'' +
                    '}';
        }
    }
}
