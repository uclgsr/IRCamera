package com.topdon.ble;

import android.bluetooth.BluetoothDevice;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Unified Device interface for all BLE devices in the system.
 * 
 * This interface provides a common API for interacting with both
 * Shimmer and Topdon BLE devices, enabling unified device management
 * and cross-device coordination.
 * 
 * @author IRCamera Unified BLE Integration Team
 */
public interface UnifiedDevice {
    
    /**
     * Get the underlying Bluetooth device
     */
    @NonNull
    BluetoothDevice getBluetoothDevice();
    
    /**
     * Get device type
     */
    @NonNull
    UnifiedBleManager.DeviceType getDeviceType();
    
    /**
     * Get device address
     */
    @NonNull
    String getAddress();
    
    /**
     * Get device name
     */
    @Nullable
    String getName();
    
    /**
     * Check if device is connected
     */
    boolean isConnected();
    
    /**
     * Connect to device
     */
    void connect();
    
    /**
     * Disconnect from device
     */
    void disconnect();
    
    /**
     * Start data streaming
     */
    boolean startDataStreaming();
    
    /**
     * Stop data streaming
     */
    boolean stopDataStreaming();
    
    /**
     * Send command to device
     */
    boolean sendCommand(@NonNull byte[] command);
    
    /**
     * Get connection status
     */
    @NonNull
    ConnectionState getConnectionState();
    
    /**
     * Get signal strength (RSSI)
     */
    int getRssi();
    
    /**
     * Get device information
     */
    @NonNull
    DeviceInfo getDeviceInfo();
    
    /**
     * Set connection listener
     */
    void setConnectionListener(@Nullable UnifiedBleManager.UnifiedConnectionListener listener);
    
    /**
     * Device connection states
     */
    enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
        ERROR
    }
    
    /**
     * Device information container
     */
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
        
        public String getDeviceName() { return deviceName; }
        public String getDeviceAddress() { return deviceAddress; }
        public UnifiedBleManager.DeviceType getDeviceType() { return deviceType; }
        public String getHardwareVersion() { return hardwareVersion; }
        public String getFirmwareVersion() { return firmwareVersion; }
        public String getSerialNumber() { return serialNumber; }
        
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