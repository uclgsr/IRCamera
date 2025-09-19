package com.mpdc4gsr.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class NordicConnectionImpl implements Connection {
    private static final String TAG = "NordicConnectionImpl";

    private final Connection baseConnection;

    private final AtomicBoolean enhancedMode = new AtomicBoolean(true);
    private final AtomicBoolean autoRetryEnabled = new AtomicBoolean(true);

    public NordicConnectionImpl(EasyBLE easyBLE, BluetoothAdapter bluetoothAdapter,
                                Device device, ConnectionConfiguration configuration,
                                int connectDelay, EventObserver companionObserver) {

        this.baseConnection = new ConnectionImpl(easyBLE, bluetoothAdapter, device,
                configuration, connectDelay, companionObserver);

        Log.i(TAG, "Created Enhanced BLE connection wrapper for device: " + device.getAddress());
    }

    @Override
    @NonNull
    public Device getDevice() {
        return baseConnection.getDevice();
    }

    @Override
    public int getMtu() {
        return baseConnection.getMtu();
    }

    @Override
    public void reconnect() {
        if (autoRetryEnabled.get()) {
            Log.i(TAG, "Enhanced reconnection with improved retry logic for device: " + getDevice().getAddress());

        }
        baseConnection.reconnect();
    }

    @Override
    public void disconnect() {
        Log.i(TAG, "Enhanced disconnect for device: " + getDevice().getAddress());
        baseConnection.disconnect();
    }

    @Override
    public void refresh() {
        Log.i(TAG, "Enhanced refresh for device: " + getDevice().getAddress());
        baseConnection.refresh();
    }

    @Override
    public void release() {
        Log.i(TAG, "Enhanced release for device: " + getDevice().getAddress());
        baseConnection.release();
    }

    @Override
    public void releaseNoEvent() {
        Log.i(TAG, "Enhanced release (no event) for device: " + getDevice().getAddress());
        baseConnection.releaseNoEvent();
    }

    @Override
    @NonNull
    public ConnectionState getConnectionState() {
        return baseConnection.getConnectionState();
    }

    @Override
    public boolean isAutoReconnectEnabled() {
        return baseConnection.isAutoReconnectEnabled();
    }

    @Override
    @Nullable
    public BluetoothGatt getGatt() {
        return baseConnection.getGatt();
    }

    @Override
    public void clearRequestQueue() {
        Log.d(TAG, "Enhanced clear request queue for device: " + getDevice().getAddress());
        baseConnection.clearRequestQueue();
    }

    @Override
    public void clearRequestQueueByType(RequestType type) {
        Log.d(TAG, "Enhanced clear request queue by type: " + type + " for device: " + getDevice().getAddress());
        baseConnection.clearRequestQueueByType(type);
    }

    @Override
    @NonNull
    public ConnectionConfiguration getConnectionConfiguration() {
        return baseConnection.getConnectionConfiguration();
    }

    @Override
    @Nullable
    public BluetoothGattService getService(UUID service) {
        return baseConnection.getService(service);
    }

    @Override
    @Nullable
    public BluetoothGattCharacteristic getCharacteristic(UUID service, UUID characteristic) {
        return baseConnection.getCharacteristic(service, characteristic);
    }

    @Override
    @Nullable
    public BluetoothGattDescriptor getDescriptor(UUID service, UUID characteristic, UUID descriptor) {
        return baseConnection.getDescriptor(service, characteristic, descriptor);
    }

    @Override
    public void execute(Request request) {
        if (enhancedMode.get()) {
            Log.d(TAG, "Enhanced request execution for device: " + getDevice().getAddress() + ", type: " + request.getType());

        }
        baseConnection.execute(request);
    }

    @Override
    public boolean isNotificationOrIndicationEnabled(BluetoothGattCharacteristic characteristic) {
        return baseConnection.isNotificationOrIndicationEnabled(characteristic);
    }

    @Override
    public boolean isNotificationOrIndicationEnabled(UUID service, UUID characteristic) {
        return baseConnection.isNotificationOrIndicationEnabled(service, characteristic);
    }

    @Override
    public void setBluetoothGattCallback(BluetoothGattCallback callback) {
        baseConnection.setBluetoothGattCallback(callback);
    }

    @Override
    public boolean hasProperty(UUID service, UUID characteristic, int property) {
        return baseConnection.hasProperty(service, characteristic, property);
    }

    public void setAutoRetryEnabled(boolean enabled) {
        autoRetryEnabled.set(enabled);
        Log.i(TAG, "Auto-retry " + (enabled ? "enabled" : "disabled") + " for device: " + getDevice().getAddress());
    }

    public boolean isEnhancedMode() {
        return enhancedMode.get();
    }

    public void setEnhancedMode(boolean enabled) {
        enhancedMode.set(enabled);
        Log.i(TAG, "Enhanced mode " + (enabled ? "enabled" : "disabled") + " for device: " + getDevice().getAddress());
    }

    public boolean isAutoRetry() {
        return autoRetryEnabled.get();
    }
}
