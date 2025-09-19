package com.mpdc4gsr.ble;

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

import com.mpdc4gsr.ble.util.BluetoothPermissionUtils;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class TopdonDevice implements UnifiedDevice {
    private static final String TAG = "MPDC4GSRDevice";

    private static final UUID MPDC4GSR_SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID MPDC4GSR_DATA_CHAR_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID MPDC4GSR_CMD_CHAR_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final UUID MPDC4GSR_THERMAL_SERVICE_UUID = UUID.fromString("12345678-1234-5678-9012-123456789ABC");
    private static final UUID MPDC4GSR_THERMAL_DATA_CHAR_UUID = UUID.fromString("12345678-1234-5678-9012-123456789ABD");

    private static final byte MPDC4GSR_START_THERMAL = 0x01;
    private static final byte MPDC4GSR_STOP_THERMAL = 0x02;
    private static final byte MPDC4GSR_START_ENV = 0x03;
    private static final byte MPDC4GSR_STOP_ENV = 0x04;

    private final BluetoothDevice bluetoothDevice;
    private final TopdonDeviceConfig config;
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
                    Log.i(TAG, "Connected to Topdon device: " + getAddress());
                    connectionState.set(ConnectionState.CONNECTED);

                    if (BluetoothPermissionUtils.hasBluetoothConnectPermission(EasyBLE.getInstance().getContext())) {
                        try {
                            gatt.discoverServices();
                        } catch (SecurityException e) {
                            Log.e(TAG, "Permission error discovering services: " + e.getMessage());
                            notifyConnectionError(0, "Permission error: " + e.getMessage());
                        }
                    } else {
                        Log.e(TAG, "Missing Bluetooth permissions for service discovery");
                        notifyConnectionError(0, "Missing Bluetooth permissions");
                    }

                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    Log.i(TAG, "Disconnected from Topdon device: " + getAddress());
                    connectionState.set(ConnectionState.DISCONNECTED);

                    if (connectionListener != null) {
                        connectionListener.onDeviceDisconnected(TopdonDevice.this, status);
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
                    Log.i(TAG, "Services discovered for MPDC4GSR device: " + getAddress());

                    BluetoothGattService topdonService = gatt.getService(MPDC4GSR_SERVICE_UUID);
                    if (topdonService == null) {

                        topdonService = gatt.getService(MPDC4GSR_THERMAL_SERVICE_UUID);
                    }

                    if (topdonService != null) {
                        dataCharacteristic = topdonService.getCharacteristic(MPDC4GSR_DATA_CHAR_UUID);
                        if (dataCharacteristic == null) {
                            dataCharacteristic = topdonService.getCharacteristic(MPDC4GSR_THERMAL_DATA_CHAR_UUID);
                        }

                        commandCharacteristic = topdonService.getCharacteristic(MPDC4GSR_CMD_CHAR_UUID);

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
                                    notifyConnectionError(0, "Permission error: " + e.getMessage());
                                    return;
                                }
                            } else {
                                Log.e(TAG, "Missing Bluetooth permissions for notifications");
                                notifyConnectionError(0, "Missing Bluetooth permissions");
                                return;
                            }

                            Log.i(TAG, "Topdon device ready: " + getAddress());

                            if (connectionListener != null) {
                                connectionListener.onDeviceConnected(TopdonDevice.this);
                                connectionListener.onDeviceReady(TopdonDevice.this);
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "Service discovery failed for Topdon device: " + status);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error in service discovery", e);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            try {
                if (characteristic.getUuid().equals(MPDC4GSR_DATA_CHAR_UUID) ||
                        characteristic.getUuid().equals(MPDC4GSR_THERMAL_DATA_CHAR_UUID)) {
                    byte[] data = characteristic.getValue();

                    if (data != null && data.length > 0) {
                        Log.d(TAG, "Received Topdon data: " + data.length + " bytes");

                        if (connectionListener != null) {
                            connectionListener.onDataReceived(TopdonDevice.this, data);
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

    public TopdonDevice(@NonNull BluetoothDevice bluetoothDevice,
                        @NonNull TopdonDeviceConfig config,
                        @Nullable UnifiedBleManager.UnifiedConnectionListener listener) {
        this.bluetoothDevice = bluetoothDevice;
        this.config = config;
        this.connectionListener = listener;

        Context context = EasyBLE.getInstance().getContext();
        String deviceName = BluetoothPermissionUtils.getDeviceName(context, bluetoothDevice);
        String deviceAddress = BluetoothPermissionUtils.getDeviceAddress(context, bluetoothDevice);

        this.deviceInfo = new DeviceInfo(
                deviceName != null && !deviceName.isEmpty() ? deviceName : "Topdon Device",
                deviceAddress,
                config.getDeviceType(),
                "1.0", 
                "1.0", 
                deviceAddress 
        );

        Log.i(TAG, "Created TopdonDevice: " + deviceAddress);
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
        Context context = EasyBLE.getInstance().getContext();
        return BluetoothPermissionUtils.getDeviceName(context, bluetoothDevice);
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
            Log.i(TAG, "Connecting to Topdon device: " + getAddress());
            connectionState.set(ConnectionState.CONNECTING);

            if (!BluetoothPermissionUtils.hasBluetoothConnectPermission(EasyBLE.getInstance().getContext())) {
                Log.e(TAG, "Missing Bluetooth permissions for connection");
                connectionState.set(ConnectionState.ERROR);
                notifyConnectionError(0, "Missing Bluetooth permissions");
                return;
            }

            try {
                bluetoothGatt = bluetoothDevice.connectGatt(null, config.isAutoReconnectEnabled(), gattCallback);
            } catch (SecurityException e) {
                Log.e(TAG, "Permission error connecting to device: " + e.getMessage());
                connectionState.set(ConnectionState.ERROR);
                notifyConnectionError(0, "Permission error: " + e.getMessage());
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to connect to Topdon device", e);
            connectionState.set(ConnectionState.ERROR);
            notifyConnectionError(0, e.getMessage());
        }
    }

    @Override
    public void disconnect() {
        try {
            Log.i(TAG, "Disconnecting from Topdon device: " + getAddress());

            if (dataStreaming.get()) {
                stopDataStreaming();
            }

            connectionState.set(ConnectionState.DISCONNECTING);

            if (bluetoothGatt != null) {
                if (BluetoothPermissionUtils.hasBluetoothConnectPermission(EasyBLE.getInstance().getContext())) {
                    try {
                        bluetoothGatt.disconnect();
                        bluetoothGatt.close();
                    } catch (SecurityException e) {
                        Log.e(TAG, "Permission error during disconnect: " + e.getMessage());
                    }
                }
                bluetoothGatt = null;
            }

            connectionState.set(ConnectionState.DISCONNECTED);

        } catch (Exception e) {
            Log.e(TAG, "Error disconnecting from Topdon device", e);
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
            Log.i(TAG, "Starting Topdon data streaming for type: " + config.getDeviceType());

            byte[] startCommand;
            switch (config.getDeviceType()) {
                case MPDC4GSR_THERMAL:
                    startCommand = new byte[]{MPDC4GSR_START_THERMAL};
                    break;
                case MPDC4GSR_ENV:
                    startCommand = new byte[]{MPDC4GSR_START_ENV};
                    break;
                case MPDC4GSR_MULTI:

                    sendCommand(new byte[]{MPDC4GSR_START_THERMAL});
                    startCommand = new byte[]{MPDC4GSR_START_ENV};
                    break;
                default:
                    startCommand = new byte[]{MPDC4GSR_START_THERMAL};
                    break;
            }

            boolean success = sendCommand(startCommand);

            if (success) {
                dataStreaming.set(true);
                Log.i(TAG, "Topdon data streaming started");
            }

            return success;

        } catch (Exception e) {
            Log.e(TAG, "Failed to start Topdon data streaming", e);
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
            Log.i(TAG, "Stopping Topdon data streaming");

            byte[] stopCommand;
            switch (config.getDeviceType()) {
                case MPDC4GSR_THERMAL:
                    stopCommand = new byte[]{MPDC4GSR_STOP_THERMAL};
                    break;
                case MPDC4GSR_ENV:
                    stopCommand = new byte[]{MPDC4GSR_STOP_ENV};
                    break;
                case MPDC4GSR_MULTI:

                    sendCommand(new byte[]{MPDC4GSR_STOP_THERMAL});
                    stopCommand = new byte[]{MPDC4GSR_STOP_ENV};
                    break;
                default:
                    stopCommand = new byte[]{MPDC4GSR_STOP_THERMAL};
                    break;
            }

            boolean success = sendCommand(stopCommand);

            if (success) {
                dataStreaming.set(false);
                Log.i(TAG, "Topdon data streaming stopped");
            }

            return success;

        } catch (Exception e) {
            Log.e(TAG, "Failed to stop Topdon data streaming", e);
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
            if (!BluetoothPermissionUtils.hasBluetoothConnectPermission(EasyBLE.getInstance().getContext())) {
                Log.e(TAG, "Missing Bluetooth permissions for command write");
                return false;
            }

            commandCharacteristic.setValue(command);
            boolean success;
            try {
                success = bluetoothGatt.writeCharacteristic(commandCharacteristic);
            } catch (SecurityException e) {
                Log.e(TAG, "Permission error writing command: " + e.getMessage());
                return false;
            }

            Log.d(TAG, "Sent Topdon command: " + Arrays.toString(command) + " success: " + success);
            return success;

        } catch (Exception e) {
            Log.e(TAG, "Failed to send Topdon command", e);
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
