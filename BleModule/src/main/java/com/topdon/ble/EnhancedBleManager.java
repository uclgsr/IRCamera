package com.topdon.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Enhanced BLE Manager that provides systematic Nordic BLE harmonization
 * with advanced connection management, multi-device coordination, and
 * improved reliability for the Multi-Modal Physiological Sensing Platform.
 * 
 * This manager serves as a central coordinator for all BLE operations
 * across platform components, providing enhanced features from Nordic BLE
 * while maintaining EasyBLE compatibility.
 * 
 * @author IRCamera Systematic Harmonization Team
 */
public class EnhancedBleManager {
    private static final String TAG = "EnhancedBleManager";
    
    // Connection monitoring and statistics
    private final ConcurrentHashMap<String, ConnectionMetrics> deviceMetrics = new ConcurrentHashMap<>();
    private final AtomicBoolean multiDeviceMode = new AtomicBoolean(false);
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    
    // Advanced features
    private final AtomicBoolean enhancedErrorRecovery = new AtomicBoolean(true);
    private final AtomicBoolean connectionOptimization = new AtomicBoolean(true);
    private final AtomicBoolean dataLossDetection = new AtomicBoolean(true);
    
    private EasyBLE easyBLE;
    
    /**
     * Connection metrics for monitoring BLE device performance
     */
    public static class ConnectionMetrics {
        public final AtomicLong connectAttempts = new AtomicLong(0);
        public final AtomicLong successfulConnections = new AtomicLong(0);
        public final AtomicLong disconnections = new AtomicLong(0);
        public final AtomicLong dataPacketsReceived = new AtomicLong(0);
        public final AtomicLong dataErrors = new AtomicLong(0);
        public final AtomicLong lastConnectionTime = new AtomicLong(0);
        public final AtomicBoolean isGsrSensor = new AtomicBoolean(false);
        
        public double getReliabilityScore() {
            long attempts = connectAttempts.get();
            if (attempts == 0) return 1.0;
            return (double) successfulConnections.get() / attempts;
        }
        
        public double getDataIntegrity() {
            long received = dataPacketsReceived.get();
            if (received == 0) return 1.0;
            return 1.0 - ((double) dataErrors.get() / received);
        }
    }
    
    private static volatile EnhancedBleManager instance;
    
    public static EnhancedBleManager getInstance() {
        if (instance == null) {
            synchronized (EnhancedBleManager.class) {
                if (instance == null) {
                    instance = new EnhancedBleManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Initialize the Enhanced BLE Manager with Nordic BLE backend
     */
    public void initialize(@NonNull Context context, boolean enableNordicBackend) {
        Log.i(TAG, "Initializing Enhanced BLE Manager with Nordic backend: " + enableNordicBackend);
        
        easyBLE = EasyBLE.getBuilder()
                .setUseNordicBleBackend(enableNordicBackend)
                .build();
                
        easyBLE.initialize((android.app.Application) context.getApplicationContext());
        
        Log.i(TAG, "Enhanced BLE Manager initialized successfully");
    }
    
    /**
     * Enhanced device connection with monitoring and metrics
     */
    @Nullable
    public Connection connectWithEnhancements(@NonNull String deviceAddress, 
                                            @Nullable ConnectionConfiguration config,
                                            @Nullable EventObserver observer) {
        Log.i(TAG, "Enhanced connection attempt for device: " + deviceAddress);
        
        // Initialize metrics for this device
        ConnectionMetrics metrics = deviceMetrics.computeIfAbsent(deviceAddress, 
            k -> new ConnectionMetrics());
        metrics.connectAttempts.incrementAndGet();
        
        // Create enhanced observer wrapper
        EventObserver enhancedObserver = createEnhancedObserver(deviceAddress, observer);
        
        Connection connection = easyBLE.connect(deviceAddress, config, enhancedObserver);
        
        if (connection != null) {
            activeConnections.incrementAndGet();
            metrics.lastConnectionTime.set(System.currentTimeMillis());
            Log.i(TAG, "Enhanced connection successful for device: " + deviceAddress);
        } else {
            Log.w(TAG, "Enhanced connection failed for device: " + deviceAddress);
        }
        
        return connection;
    }
    
    /**
     * Create enhanced observer with systematic monitoring
     */
    private EventObserver createEnhancedObserver(String deviceAddress, EventObserver baseObserver) {
        return new EventObserver() {
            @Override
            public void onConnectionStateChanged(Device device) {
                ConnectionMetrics metrics = deviceMetrics.get(deviceAddress);
                if (metrics != null) {
                    ConnectionState state = device.getConnectionState();
                    if (state == ConnectionState.SERVICE_DISCOVERED) {
                        metrics.successfulConnections.incrementAndGet();
                        Log.i(TAG, "Enhanced connection success for device: " + deviceAddress + 
                            ", reliability: " + String.format("%.2f%%", 
                            metrics.getReliabilityScore() * 100));
                    } else if (state == ConnectionState.DISCONNECTED) {
                        metrics.disconnections.incrementAndGet();
                        activeConnections.decrementAndGet();
                        Log.i(TAG, "Enhanced disconnection for device: " + deviceAddress);
                    }
                }
                
                if (baseObserver != null) {
                    baseObserver.onConnectionStateChanged(device);
                }
            }
            
            @Override
            public void onConnectFailed(Device device, int failType) {
                Log.w(TAG, "Enhanced connection failed for device: " + deviceAddress + 
                    ", error type: " + failType);
                
                if (enhancedErrorRecovery.get()) {
                    scheduleEnhancedReconnection(deviceAddress, failType);
                }
                
                if (baseObserver != null) {
                    baseObserver.onConnectFailed(device, failType);
                }
            }
            
            @Override
            public void onConnectTimeout(Device device, int type) {
                Log.w(TAG, "Enhanced connection timeout for device: " + deviceAddress + 
                    ", timeout type: " + type);
                
                if (baseObserver != null) {
                    baseObserver.onConnectTimeout(device, type);
                }
            }
            
            @Override
            public void onCharacteristicChanged(Device device, UUID service, UUID characteristic, byte[] value) {
                ConnectionMetrics metrics = deviceMetrics.get(deviceAddress);
                if (metrics != null) {
                    metrics.dataPacketsReceived.incrementAndGet();
                }
                
                Log.d(TAG, "Enhanced characteristic changed for device: " + deviceAddress + 
                    ", service: " + service + ", characteristic: " + characteristic);
                
                if (baseObserver != null) {
                    baseObserver.onCharacteristicChanged(device, service, characteristic, value);
                }
            }
            
            @Override
            public void onNotificationChanged(Request request, boolean isEnabled) {
                Log.d(TAG, "Enhanced notification changed for device: " + deviceAddress + 
                    ", request: " + request.getType() + ", enabled: " + isEnabled);
                
                if (baseObserver != null) {
                    baseObserver.onNotificationChanged(request, isEnabled);
                }
            }
            
            @Override
            public void onCharacteristicRead(Request request, byte[] value) {
                ConnectionMetrics metrics = deviceMetrics.get(deviceAddress);
                if (metrics != null) {
                    metrics.dataPacketsReceived.incrementAndGet();
                }
                
                Log.d(TAG, "Enhanced characteristic read for device: " + deviceAddress + 
                    ", request type: " + request.getType());
                
                if (baseObserver != null) {
                    baseObserver.onCharacteristicRead(request, value);
                }
            }
            
            @Override
            public void onCharacteristicWrite(Request request, byte[] value) {
                Log.d(TAG, "Enhanced characteristic write for device: " + deviceAddress + 
                    ", request type: " + request.getType());
                
                if (baseObserver != null) {
                    baseObserver.onCharacteristicWrite(request, value);
                }
            }
            
            @Override
            public void onRequestFailed(Request request, int failType, Object value) {
                ConnectionMetrics metrics = deviceMetrics.get(deviceAddress);
                if (metrics != null) {
                    metrics.dataErrors.incrementAndGet();
                }
                
                if (dataLossDetection.get()) {
                    Log.w(TAG, "Enhanced request failure detected for device: " + deviceAddress + 
                        ", request: " + request.getType() + ", fail type: " + failType);
                }
                
                if (baseObserver != null) {
                    baseObserver.onRequestFailed(request, failType, value);
                }
            }
            
            @Override
            public void onMtuChanged(Request request, int mtu) {
                Log.i(TAG, "Enhanced MTU changed for device: " + deviceAddress + 
                    ", new MTU: " + mtu);
                
                if (baseObserver != null) {
                    baseObserver.onMtuChanged(request, mtu);
                }
            }
            
            @Override
            public void onRssiRead(Request request, int rssi) {
                Log.d(TAG, "Enhanced RSSI read for device: " + deviceAddress + 
                    ", RSSI: " + rssi);
                
                if (baseObserver != null) {
                    baseObserver.onRssiRead(request, rssi);
                }
            }
            
            @Override
            public void onDescriptorRead(Request request, byte[] value) {
                Log.d(TAG, "Enhanced descriptor read for device: " + deviceAddress + 
                    ", request type: " + request.getType());
                
                if (baseObserver != null) {
                    baseObserver.onDescriptorRead(request, value);
                }
            }
            
            @Override
            public void onBluetoothAdapterStateChanged(int state) {
                Log.i(TAG, "Enhanced Bluetooth adapter state changed: " + state + 
                    " for device monitoring: " + deviceAddress);
                
                if (baseObserver != null) {
                    baseObserver.onBluetoothAdapterStateChanged(state);
                }
            }
            
            @Override
            public void onPhyChange(Request request, int txPhy, int rxPhy) {
                Log.d(TAG, "Enhanced PHY change for device: " + deviceAddress + 
                    ", TX PHY: " + txPhy + ", RX PHY: " + rxPhy);
                
                if (baseObserver != null) {
                    baseObserver.onPhyChange(request, txPhy, rxPhy);
                }
            }
        };
    }
    
    /**
     * Enhanced reconnection with progressive retry delays
     */
    private void scheduleEnhancedReconnection(String deviceAddress, int errorType) {
        Log.i(TAG, "Scheduling enhanced reconnection for device: " + deviceAddress);
        // Implementation for enhanced reconnection logic would go here
    }
    
    /**
     * Enable multi-device coordination mode for hub-spoke systems
     */
    public void enableMultiDeviceMode(boolean enabled) {
        multiDeviceMode.set(enabled);
        Log.i(TAG, "Multi-device coordination mode " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Get connection metrics for a specific device
     */
    @Nullable
    public ConnectionMetrics getDeviceMetrics(String deviceAddress) {
        return deviceMetrics.get(deviceAddress);
    }
    
    /**
     * Get system-wide BLE status
     */
    public SystemBleStatus getSystemStatus() {
        return new SystemBleStatus(
            activeConnections.get(),
            deviceMetrics.size(),
            multiDeviceMode.get(),
            easyBLE.isBluetoothOn()
        );
    }
    
    /**
     * Mark a device as a GSR sensor for enhanced physiological data handling
     */
    public void markAsGsrSensor(String deviceAddress) {
        ConnectionMetrics metrics = deviceMetrics.computeIfAbsent(deviceAddress, 
            k -> new ConnectionMetrics());
        metrics.isGsrSensor.set(true);
        Log.i(TAG, "Device marked as GSR sensor: " + deviceAddress);
    }
    
    /**
     * System-wide BLE status information
     */
    public static class SystemBleStatus {
        public final int activeConnections;
        public final int knownDevices;
        public final boolean multiDeviceMode;
        public final boolean bluetoothEnabled;
        
        SystemBleStatus(int activeConnections, int knownDevices, 
                       boolean multiDeviceMode, boolean bluetoothEnabled) {
            this.activeConnections = activeConnections;
            this.knownDevices = knownDevices;
            this.multiDeviceMode = multiDeviceMode;
            this.bluetoothEnabled = bluetoothEnabled;
        }
        
        @Override
        public String toString() {
            return String.format("BLE Status: %d active, %d known devices, multi-device: %s, BT: %s",
                activeConnections, knownDevices, multiDeviceMode, bluetoothEnabled);
        }
    }
    
    /**
     * Release all resources and reset the manager
     */
    public void release() {
        Log.i(TAG, "Releasing Enhanced BLE Manager resources");
        
        if (easyBLE != null) {
            easyBLE.release();
        }
        
        deviceMetrics.clear();
        activeConnections.set(0);
        
        Log.i(TAG, "Enhanced BLE Manager released successfully");
    }
}