package com.topdon.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.topdon.ble.util.BluetoothPermissionUtils;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of UnifiedDevice for Shimmer3 GSR+ devices
 * Provides BLE connectivity and data streaming capabilities
 */
public class ShimmerDevice implements UnifiedDevice {
    private static final String TAG = "ShimmerDevice";

    // Shimmer BLE service and characteristic UUIDs
    private static final UUID SHIMMER_SERVICE_UUID = UUID.fromString("49535343-FE7D-4AE5-8FA9-9FAFD205E455");
    private static final UUID SHIMMER_DATA_CHAR_UUID = UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616");
    private static final UUID SHIMMER_CMD_CHAR_UUID = UUID.fromString("49535343-8841-43F4-A8D4-ECBE34729BB3");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // Shimmer command constants
    private static final byte SHIMMER_START_STREAMING = 0x07;
    private static final byte SHIMMER_STOP_STREAMING = 0x20;
    private static final byte SHIMMER_GET_SAMPLING_RATE = 0x03;

    private final BluetoothDevice bluetoothDevice;
    private final ShimmerDeviceConfig config;
    private final Context context;
    private final Handler mainHandler;
    
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic dataCharacteristic;
    private BluetoothGattCharacteristic commandCharacteristic;
    private UnifiedBleManager.UnifiedConnectionListener connectionListener;
    
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicBoolean isStreaming = new AtomicBoolean(false);
    private ConnectionState connectionState = ConnectionState.DISCONNECTED;
    private int lastRssi = -50; // Default RSSI value

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server for device: " + bluetoothDevice.getAddress());
                connectionState = ConnectionState.CONNECTED;
                isConnected.set(true);
                
                if (connectionListener != null) {
                    mainHandler.post(() -> connectionListener.onDeviceConnected(ShimmerDevice.this));
                }
                
                // Discover services
                if (bluetoothGatt != null) {
                    try {
                        bluetoothGatt.discoverServices();
                    } catch (SecurityException e) {
                        Log.e(TAG, "Permission denied for service discovery", e);
                    }
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server for device: " + bluetoothDevice.getAddress());
                connectionState = ConnectionState.DISCONNECTED;
                isConnected.set(false);
                isStreaming.set(false);
                
                if (connectionListener != null) {
                    mainHandler.post(() -> connectionListener.onDeviceDisconnected(ShimmerDevice.this, 0));
                }
                
                cleanup();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Services discovered for device: " + bluetoothDevice.getAddress());
                initializeCharacteristics();
                
                // Notify that device is ready
                if (connectionListener != null) {
                    mainHandler.post(() -> connectionListener.onDeviceReady(ShimmerDevice.this));
                }
            } else {
                Log.w(TAG, "Service discovery failed with status: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (SHIMMER_DATA_CHAR_UUID.equals(characteristic.getUuid())) {
                byte[] data = characteristic.getValue();
                if (data != null && connectionListener != null) {
                    mainHandler.post(() -> connectionListener.onDataReceived(ShimmerDevice.this, data));
                }
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                lastRssi = rssi;
            }
        }
    };

    public ShimmerDevice(@NonNull BluetoothDevice bluetoothDevice, 
                        @NonNull ShimmerDeviceConfig config,
                        @Nullable UnifiedBleManager.UnifiedConnectionListener listener) {
        this.bluetoothDevice = bluetoothDevice;
        this.config = config;
        this.connectionListener = listener;
        // Try to get context from EasyBLE instance, fallback to null
        Context tmpContext = null;
        try {
            if (EasyBLE.getInstance() != null) {
                tmpContext = EasyBLE.getInstance().getContext();
            }
        } catch (Exception e) {
            tmpContext = null;
        }
        this.context = tmpContext;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @NonNull
    @Override
    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    @NonNull
    @Override
    public UnifiedBleManager.DeviceType getDeviceType() {
        return config.getDeviceType();
    }

    @NonNull
    @Override
    public String getAddress() {
        return bluetoothDevice.getAddress();
    }

    @Nullable
    @Override
    public String getName() {
        if (context != null) {
            return BluetoothPermissionUtils.getDeviceName(context, bluetoothDevice);
        }
        try {
            return bluetoothDevice.getName();
        } catch (SecurityException e) {
            Log.w(TAG, "Permission denied for device name access", e);
            return "Shimmer Device";
        }
    }

    @Override
    public boolean isConnected() {
        return isConnected.get();
    }

    @Override
    public void connect() {
        if (isConnected.get()) {
            Log.w(TAG, "Device already connected: " + getAddress());
            return;
        }

        try {
            connectionState = ConnectionState.CONNECTING;
            
            if (connectionListener != null) {
                mainHandler.post(() -> connectionListener.onDeviceConnected(this));
            }

            // Connect to GATT server
            bluetoothGatt = bluetoothDevice.connectGatt(context, false, gattCallback);
            
            Log.i(TAG, "Initiated GATT connection to device: " + getAddress());
            
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for GATT connection", e);
            connectionState = ConnectionState.ERROR;
            if (connectionListener != null) {
                mainHandler.post(() -> connectionListener.onConnectionError(this, -1, "Permission denied"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to connect to GATT server", e);
            connectionState = ConnectionState.ERROR;
            if (connectionListener != null) {
                mainHandler.post(() -> connectionListener.onConnectionError(this, -1, e.getMessage()));
            }
        }
    }

    @Override
    public void disconnect() {
        try {
            connectionState = ConnectionState.DISCONNECTING;
            
            if (isStreaming.get()) {
                stopDataStreaming();
            }
            
            if (bluetoothGatt != null) {
                try {
                    bluetoothGatt.disconnect();
                } catch (SecurityException e) {
                    Log.w(TAG, "Permission denied during disconnect", e);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error during disconnect", e);
        } finally {
            cleanup();
        }
    }

    @Override
    public boolean startDataStreaming() {
        if (!isConnected.get()) {
            Log.w(TAG, "Device not connected, cannot start streaming");
            return false;
        }

        if (isStreaming.get()) {
            Log.w(TAG, "Already streaming data");
            return true;
        }

        try {
            // Enable notifications on data characteristic
            if (dataCharacteristic != null) {
                boolean notificationEnabled = enableNotifications(dataCharacteristic);
                if (!notificationEnabled) {
                    Log.e(TAG, "Failed to enable notifications");
                    return false;
                }
            }

            // Send start streaming command
            if (commandCharacteristic != null) {
                byte[] startCommand = {SHIMMER_START_STREAMING};
                boolean commandSent = sendCommand(startCommand);
                if (commandSent) {
                    isStreaming.set(true);
                    Log.i(TAG, "Started data streaming for device: " + getAddress());
                    return true;
                } else {
                    Log.e(TAG, "Failed to send start streaming command");
                    return false;
                }
            }

            Log.e(TAG, "Command characteristic not available");
            return false;

        } catch (Exception e) {
            Log.e(TAG, "Error starting data streaming", e);
            return false;
        }
    }

    @Override
    public boolean stopDataStreaming() {
        if (!isStreaming.get()) {
            Log.w(TAG, "Not currently streaming");
            return true;
        }

        try {
            // Send stop streaming command
            if (commandCharacteristic != null) {
                byte[] stopCommand = {SHIMMER_STOP_STREAMING};
                boolean commandSent = sendCommand(stopCommand);
                if (commandSent) {
                    isStreaming.set(false);
                    Log.i(TAG, "Stopped data streaming for device: " + getAddress());
                    return true;
                } else {
                    Log.e(TAG, "Failed to send stop streaming command");
                    return false;
                }
            }

            Log.e(TAG, "Command characteristic not available");
            return false;

        } catch (Exception e) {
            Log.e(TAG, "Error stopping data streaming", e);
            return false;
        }
    }

    @Override
    public boolean sendCommand(@NonNull byte[] command) {
        if (!isConnected.get() || commandCharacteristic == null) {
            Log.w(TAG, "Cannot send command - device not ready");
            return false;
        }

        try {
            commandCharacteristic.setValue(command);
            return bluetoothGatt.writeCharacteristic(commandCharacteristic);
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for characteristic write", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error sending command", e);
            return false;
        }
    }

    @NonNull
    @Override
    public ConnectionState getConnectionState() {
        return connectionState;
    }

    @Override
    public int getRssi() {
        return lastRssi;
    }

    @NonNull
    @Override
    public UnifiedDevice.DeviceInfo getDeviceInfo() {
        return new UnifiedDevice.DeviceInfo(
            getDeviceName(),
            getAddress(),
            getDeviceType(),
            "Unknown", // Hardware version
            "Unknown", // Firmware version  
            "Unknown"  // Serial number
        );
    }

    @Override
    public void setConnectionListener(@Nullable UnifiedBleManager.UnifiedConnectionListener listener) {
        this.connectionListener = listener;
    }

    private void initializeCharacteristics() {
        try {
            BluetoothGattService shimmerService = bluetoothGatt.getService(SHIMMER_SERVICE_UUID);
            if (shimmerService == null) {
                Log.e(TAG, "Shimmer service not found");
                return;
            }

            dataCharacteristic = shimmerService.getCharacteristic(SHIMMER_DATA_CHAR_UUID);
            commandCharacteristic = shimmerService.getCharacteristic(SHIMMER_CMD_CHAR_UUID);

            if (dataCharacteristic == null) {
                Log.e(TAG, "Data characteristic not found");
            }

            if (commandCharacteristic == null) {
                Log.e(TAG, "Command characteristic not found");
            }

            Log.i(TAG, "Characteristics initialized for device: " + getAddress());

        } catch (Exception e) {
            Log.e(TAG, "Error initializing characteristics", e);
        }
    }

    private boolean enableNotifications(BluetoothGattCharacteristic characteristic) {
        try {
            boolean result = bluetoothGatt.setCharacteristicNotification(characteristic, true);
            
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
            if (descriptor != null) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                result &= bluetoothGatt.writeDescriptor(descriptor);
            }
            
            return result;
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for enabling notifications", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error enabling notifications", e);
            return false;
        }
    }

    private void cleanup() {
        try {
            if (bluetoothGatt != null) {
                bluetoothGatt.close();
                bluetoothGatt = null;
            }
            
            dataCharacteristic = null;
            commandCharacteristic = null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup", e);
        }
    }
}