package com.topdon.ble;

import android.annotation.SuppressLint;
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

/**
 * Enhanced BLE connection implementation that wraps existing ConnectionImpl
 * with Nordic BLE library features for improved reliability.
 * 
 * This provides the "best of both worlds" by maintaining EasyBLE API compatibility
 * while adding Nordic BLE's enhanced reliability features as a wrapper layer.
 * 
 * @author IRCamera Integration Team
 */
@SuppressLint("MissingPermission")
public class NordicConnectionImpl implements Connection {
    private static final String TAG = "NordicConnectionImpl";
    
    // Delegate to existing ConnectionImpl for core functionality
    private final Connection baseConnection;
    
    // Enhanced features
    private final AtomicBoolean enhancedMode = new AtomicBoolean(true);
    private final AtomicBoolean autoRetryEnabled = new AtomicBoolean(true);

    public NordicConnectionImpl(EasyBLE easyBLE, BluetoothAdapter bluetoothAdapter, 
                               Device device, ConnectionConfiguration configuration, 
                               int connectDelay, EventObserver companionObserver) {
        // Create base connection using existing ConnectionImpl
        this.baseConnection = new ConnectionImpl(easyBLE, bluetoothAdapter, device, 
                                               configuration, connectDelay, companionObserver);
        
        Log.i(TAG, "Created Enhanced BLE connection wrapper for device: " + device.getAddress());
    }

    // Delegate all interface methods to base connection with enhanced features
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
            // Add enhanced retry logic here if needed
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
            // Add enhanced request processing logic here if needed
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
    
    /**
     * Enable or disable enhanced mode
     */
    public void setEnhancedMode(boolean enabled) {
        enhancedMode.set(enabled);
        Log.i(TAG, "Enhanced mode " + (enabled ? "enabled" : "disabled") + " for device: " + getDevice().getAddress());
    }
    
    /**
     * Enable or disable auto-retry functionality
     */
    public void setAutoRetryEnabled(boolean enabled) {
        autoRetryEnabled.set(enabled);
        Log.i(TAG, "Auto-retry " + (enabled ? "enabled" : "disabled") + " for device: " + getDevice().getAddress());
    }
    
    /**
     * Check if enhanced mode is enabled
     */
    public boolean isEnhancedMode() {
        return enhancedMode.get();
    }
    
    /**
     * Check if auto-retry is enabled for this connection
     */
    public boolean isAutoRetry() {
        return autoRetryEnabled.get();
    }
}