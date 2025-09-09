package com.topdon.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Shimmer device implementation using Nordic BLE backend.
 * 
 * Provides enhanced reliability and comprehensive Shimmer device support
 * for GSR, PPG, IMU, and multi-sensor configurations.
 * 
 * @author IRCamera Shimmer Integration Team
 */
@SuppressLint("MissingPermission")
public class ShimmerDevice implements UnifiedDevice {
    private static final String TAG = "ShimmerDevice";
    
    // Shimmer BLE service and characteristic UUIDs
    private static final UUID SHIMMER_SERVICE_UUID = UUID.fromString("49535343-FE7D-4AE5-8FA9-9FAFD205E455");
    private static final UUID SHIMMER_DATA_CHAR_UUID = UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616");
    private static final UUID SHIMMER_CMD_CHAR_UUID = UUID.fromString("49535343-8841-43F4-A8D4-ECBE34729BB3");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    
    // Shimmer commands
    private static final byte SHIMMER_START_STREAMING = 0x07;
    private static final byte SHIMMER_STOP_STREAMING = 0x20;
    
    // Device state
    private final BluetoothDevice bluetoothDevice;
    private final ShimmerDeviceConfig config;
    private final AtomicReference<ConnectionState> connectionState = new AtomicReference<>(ConnectionState.DISCONNECTED);
    private final AtomicBoolean dataStreaming = new AtomicBoolean(false);
    
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic dataCharacteristic;
    private BluetoothGattCharacteristic commandCharacteristic;
    private UnifiedBleManager.UnifiedConnectionListener connectionListener;
    private int currentRssi = 0;
    
    // Device info
    private DeviceInfo deviceInfo;
    
    /**
     * Constructor
     */
    public ShimmerDevice(@NonNull BluetoothDevice bluetoothDevice, 
                        @NonNull ShimmerDeviceConfig config,
                        @Nullable UnifiedBleManager.UnifiedConnectionListener listener) {
        this.bluetoothDevice = bluetoothDevice;
        this.config = config;
        this.connectionListener = listener;
        
        // Create device info
        this.deviceInfo = new DeviceInfo(
            bluetoothDevice.getName() != null ? bluetoothDevice.getName() : "Shimmer Device",
            bluetoothDevice.getAddress(),
            config.getDeviceType(),
            "3.0", // Hardware version
            "1.0", // Firmware version
            bluetoothDevice.getAddress() // Serial number based on MAC
        );
        
        Log.i(TAG, "Created ShimmerDevice: " + bluetoothDevice.getAddress());
    }
    
    @Override
    @NonNull
    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }
    
    @Override
    @NonNull
    public UnifiedBleManager.DeviceType getDeviceType() {
        return config.getDeviceType();
    }
    
    @Override
    @NonNull
    public String getAddress() {
        return bluetoothDevice.getAddress();
    }
    
    @Override
    @Nullable
    public String getName() {
        return bluetoothDevice.getName();
    }
    
    @Override
    public boolean isConnected() {
        return connectionState.get() == ConnectionState.CONNECTED;
    }
    
    @Override
    public void connect() {
        if (connectionState.get() == ConnectionState.CONNECTING || 
            connectionState.get() == ConnectionState.CONNECTED) {
            Log.w(TAG, "Already connecting or connected to " + getAddress());
            return;
        }
        
        try {
            Log.i(TAG, "Connecting to Shimmer device: " + getAddress());
            connectionState.set(ConnectionState.CONNECTING);
            
            bluetoothGatt = bluetoothDevice.connectGatt(null, config.isAutoReconnectEnabled(), gattCallback);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to connect to Shimmer device", e);
            connectionState.set(ConnectionState.ERROR);
            notifyConnectionError(0, e.getMessage());
        }
    }
    
    @Override
    public void disconnect() {
        try {
            Log.i(TAG, "Disconnecting from Shimmer device: " + getAddress());
            
            if (dataStreaming.get()) {
                stopDataStreaming();
            }
            
            connectionState.set(ConnectionState.DISCONNECTING);
            
            if (bluetoothGatt != null) {
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
                bluetoothGatt = null;
            }
            
            connectionState.set(ConnectionState.DISCONNECTED);
            
        } catch (Exception e) {
            Log.e(TAG, "Error disconnecting from Shimmer device", e);
        }
    }
    
    @Override
    public boolean startDataStreaming() {
        if (!isConnected()) {
            Log.w(TAG, "Device not connected");
            return false;
        }
        
        if (dataStreaming.get()) {
            Log.w(TAG, "Data streaming already active");
            return true;
        }
        
        try {
            Log.i(TAG, "Starting Shimmer data streaming");
            
            // Send start streaming command
            byte[] startCommand = {SHIMMER_START_STREAMING};
            boolean success = sendCommand(startCommand);
            
            if (success) {
                dataStreaming.set(true);
                Log.i(TAG, "Shimmer data streaming started");
            }
            
            return success;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start Shimmer data streaming", e);
            return false;
        }
    }
    
    @Override
    public boolean stopDataStreaming() {
        if (!dataStreaming.get()) {
            Log.w(TAG, "Data streaming not active");
            return true;
        }
        
        try {
            Log.i(TAG, "Stopping Shimmer data streaming");
            
            // Send stop streaming command
            byte[] stopCommand = {SHIMMER_STOP_STREAMING};
            boolean success = sendCommand(stopCommand);
            
            if (success) {
                dataStreaming.set(false);
                Log.i(TAG, "Shimmer data streaming stopped");
            }
            
            return success;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop Shimmer data streaming", e);
            return false;
        }
    }
    
    @Override
    public boolean sendCommand(@NonNull byte[] command) {
        if (!isConnected() || commandCharacteristic == null) {
            Log.w(TAG, "Cannot send command - device not ready");
            return false;
        }
        
        try {
            commandCharacteristic.setValue(command);
            boolean success = bluetoothGatt.writeCharacteristic(commandCharacteristic);
            
            Log.d(TAG, "Sent Shimmer command: " + Arrays.toString(command) + " success: " + success);
            return success;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to send Shimmer command", e);
            return false;
        }
    }
    
    @Override
    @NonNull
    public ConnectionState getConnectionState() {
        return connectionState.get();
    }
    
    @Override
    public int getRssi() {
        return currentRssi;
    }
    
    @Override
    @NonNull
    public DeviceInfo getDeviceInfo() {
        return deviceInfo;
    }
    
    @Override
    public void setConnectionListener(@Nullable UnifiedBleManager.UnifiedConnectionListener listener) {
        this.connectionListener = listener;
    }
    
    /**
     * GATT callback for handling Shimmer BLE communication
     */
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            try {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    Log.i(TAG, "Connected to Shimmer device: " + getAddress());
                    connectionState.set(ConnectionState.CONNECTED);
                    
                    // Discover services
                    gatt.discoverServices();
                    
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    Log.i(TAG, "Disconnected from Shimmer device: " + getAddress());
                    connectionState.set(ConnectionState.DISCONNECTED);
                    
                    if (connectionListener != null) {
                        connectionListener.onDeviceDisconnected(ShimmerDevice.this, status);
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error in connection state change", e);
            }
        }
        
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            try {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i(TAG, "Services discovered for Shimmer device: " + getAddress());
                    
                    // Find Shimmer service and characteristics
                    BluetoothGattService shimmerService = gatt.getService(SHIMMER_SERVICE_UUID);
                    if (shimmerService != null) {
                        dataCharacteristic = shimmerService.getCharacteristic(SHIMMER_DATA_CHAR_UUID);
                        commandCharacteristic = shimmerService.getCharacteristic(SHIMMER_CMD_CHAR_UUID);
                        
                        if (dataCharacteristic != null && commandCharacteristic != null) {
                            // Enable notifications for data characteristic
                            gatt.setCharacteristicNotification(dataCharacteristic, true);
                            
                            BluetoothGattDescriptor descriptor = dataCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                            if (descriptor != null) {
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                gatt.writeDescriptor(descriptor);
                            }
                            
                            Log.i(TAG, "Shimmer device ready: " + getAddress());
                            
                            if (connectionListener != null) {
                                connectionListener.onDeviceConnected(ShimmerDevice.this);
                                connectionListener.onDeviceReady(ShimmerDevice.this);
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "Service discovery failed for Shimmer device: " + status);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error in service discovery", e);
            }
        }
        
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            try {
                if (characteristic.getUuid().equals(SHIMMER_DATA_CHAR_UUID)) {
                    byte[] data = characteristic.getValue();
                    
                    if (data != null && data.length > 0) {
                        Log.d(TAG, "Received Shimmer data: " + data.length + " bytes");
                        
                        if (connectionListener != null) {
                            connectionListener.onDataReceived(ShimmerDevice.this, data);
                        }
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error processing characteristic change", e);
            }
        }
        
        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                currentRssi = rssi;
                Log.d(TAG, "RSSI updated: " + rssi);
            }
        }
    };
    
    /**
     * Notify connection error
     */
    private void notifyConnectionError(int errorCode, String message) {
        if (connectionListener != null) {
            connectionListener.onConnectionError(this, errorCode, message);
        }
    }
}