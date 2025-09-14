package com.topdon.ble;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.topdon.ble.util.BluetoothPermissionUtils;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ShimmerDevice implements UnifiedDevice {
    private static final String TAG = "ShimmerDevice";

    private static final UUID SHIMMER_SERVICE_UUID = UUID.fromString("49535343-FE7D-4AE5-8FA9-9FAFD205E455");
    private static final UUID SHIMMER_DATA_CHAR_UUID = UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616");
    private static final UUID SHIMMER_CMD_CHAR_UUID = UUID.fromString("49535343-8841-43F4-A8D4-ECBE34729BB3");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final byte SHIMMER_START_STREAMING = 0x07;
    private static final byte SHIMMER_STOP_STREAMING = 0x20;

    private final BluetoothDevice bluetoothDevice;
    private final ShimmerDeviceConfig config;
    private final AtomicReference<ConnectionState> connectionState = new AtomicReference<>(ConnectionState.DISCONNECTED);
    private final AtomicBoolean dataStreaming = new AtomicBoolean(false);

    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic dataCharacteristic;
    private BluetoothGattCharacteristic commandCharacteristic;
    private UnifiedBleManager.UnifiedConnectionListener connectionListener;
    private int currentRssi = 0;
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            try {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    Log.i(TAG, "Connected to Shimmer device: " + getAddress());
                    connectionState.set(ConnectionState.CONNECTED);

                    if (BluetoothPermissionUtils.hasBluetoothConnectPermission(EasyBLE.getInstance().getContext())) {
                        try {
                            gatt.discoverServices();
                        } catch (SecurityException e) {
                            Log.e(TAG, "Permission error discovering services: " + e.getMessage());
                            if (connectionListener != null) {
                                connectionListener.onConnectionError(ShimmerDevice.this, 0, "Permission error: " + e.getMessage());
                            }
                        }
                    } else {
                        Log.e(TAG, "Missing Bluetooth permissions for service discovery");
                        if (connectionListener != null) {
                            connectionListener.onConnectionError(ShimmerDevice.this, 0, "Missing Bluetooth permissions");
                        }
                    }

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

                    BluetoothGattService shimmerService = gatt.getService(SHIMMER_SERVICE_UUID);
                    if (shimmerService != null) {
                        dataCharacteristic = shimmerService.getCharacteristic(SHIMMER_DATA_CHAR_UUID);
                        commandCharacteristic = shimmerService.getCharacteristic(SHIMMER_CMD_CHAR_UUID);

                        if (dataCharacteristic != null && commandCharacteristic != null) {

                            if (BluetoothPermissionUtils.hasBluetoothConnectPermission(EasyBLE.getInstance().getContext())) {
                                try {
                                    gatt.setCharacteristicNotification(dataCharacteristic, true);

                                    BluetoothGattDescriptor descriptor = dataCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                                    if (descriptor != null) {
                                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                        gatt.writeDescriptor(descriptor);
                                    }
                                } catch (SecurityException e) {
                                    Log.e(TAG, "Permission error enabling notifications: " + e.getMessage());
                                    if (connectionListener != null) {
                                        connectionListener.onConnectionError(ShimmerDevice.this, 0, "Permission error: " + e.getMessage());
                                    }
                                }
                            } else {
                                Log.e(TAG, "Missing Bluetooth permissions for notifications");
                                if (connectionListener != null) {
                                    connectionListener.onConnectionError(ShimmerDevice.this, 0, "Missing Bluetooth permissions");
                                }
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
    private DeviceInfo deviceInfo;

    public ShimmerDevice(@NonNull BluetoothDevice bluetoothDevice,
                         @NonNull ShimmerDeviceConfig config,
                         @Nullable UnifiedBleManager.UnifiedConnectionListener listener) {
        this.bluetoothDevice = bluetoothDevice;
        this.config = config;
        this.connectionListener = listener;

        Context context = EasyBLE.getInstance().getContext();
        String deviceName = BluetoothPermissionUtils.getDeviceName(context, bluetoothDevice);
        String deviceAddress = BluetoothPermissionUtils.getDeviceAddress(context, bluetoothDevice);

        this.deviceInfo = new DeviceInfo(
                deviceName != null && !deviceName.isEmpty() ? deviceName : "Shimmer Device",
                deviceAddress,
                config.getDeviceType(),
                "3.0", // Hardware version
                "1.0", // Firmware version
                deviceAddress // Serial number based on MAC
        );

        Log.i(TAG, "Created ShimmerDevice: " + bluetoothDevice.getAddress());
    }

    private boolean hasBluetoothPermission(Context context) {
        if (context == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        } else {

            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED;
        }
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
        try {
            if (hasBluetoothPermission(EasyBLE.getInstance().getContext())) {
                return bluetoothDevice.getName();
            } else {
                Log.w(TAG, "Missing Bluetooth permissions to access device name");
                return "Shimmer Device (Name Unavailable)";
            }
        } catch (SecurityException e) {
            Log.w(TAG, "SecurityException accessing device name: " + e.getMessage());
            return "Shimmer Device (Permission Denied)";
        }
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

        Context context = EasyBLE.getInstance().getContext();
        if (!hasBluetoothPermission(context)) {
            Log.e(TAG, "Missing Bluetooth permissions to connect to Shimmer device");
            connectionState.set(ConnectionState.ERROR);
            notifyConnectionError(Connection.CONNECT_FAIL_TYPE_NO_PERMISSION, "Missing Bluetooth permissions");
            return;
        }

        try {
            Log.i(TAG, "Connecting to Shimmer device: " + getAddress());
            connectionState.set(ConnectionState.CONNECTING);

            bluetoothGatt = bluetoothDevice.connectGatt(context, config.isAutoReconnectEnabled(), gattCallback);

        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException during Shimmer connection", e);
            connectionState.set(ConnectionState.ERROR);
            notifyConnectionError(Connection.CONNECT_FAIL_TYPE_NO_PERMISSION, e.getMessage());
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

                Context context = EasyBLE.getInstance().getContext();
                if (hasBluetoothPermission(context)) {
                    bluetoothGatt.disconnect();
                    bluetoothGatt.close();
                } else {
                    Log.w(TAG, "Missing Bluetooth permissions for proper disconnect");
                }
                bluetoothGatt = null;
            }

            connectionState.set(ConnectionState.DISCONNECTED);

        } catch (SecurityException e) {
            Log.w(TAG, "SecurityException during disconnect: " + e.getMessage());
            connectionState.set(ConnectionState.DISCONNECTED);
        } catch (Exception e) {
            Log.e(TAG, "Error disconnecting from Shimmer device", e);
            connectionState.set(ConnectionState.DISCONNECTED);
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

        Context context = EasyBLE.getInstance().getContext();
        if (!hasBluetoothPermission(context)) {
            Log.w(TAG, "Missing Bluetooth permissions to send command");
            return false;
        }

        try {
            commandCharacteristic.setValue(command);
            boolean success = bluetoothGatt.writeCharacteristic(commandCharacteristic);

            Log.d(TAG, "Sent Shimmer command: " + Arrays.toString(command) + " success: " + success);
            return success;

        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException sending Shimmer command: " + e.getMessage());
            return false;
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

    private void notifyConnectionError(int errorCode, String message) {
        if (connectionListener != null) {
            connectionListener.onConnectionError(this, errorCode, message);
        }
    }
}
