package com.topdon.ble;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.topdon.ble.util.BluetoothPermissionUtils;

import java.util.List;
import java.util.Map;


public class CrossModalIntegrationExample {
    private static final String TAG = "CrossModalExample";
    
    private final Context context;
    private UnifiedBleManager bleManager;
    private CrossModalSyncManager syncManager;
    
    public CrossModalIntegrationExample(@NonNull Context context) {
        this.context = context;
    }
    

    public boolean initializeComprehensiveSystem() {
        Log.i(TAG, "=== Initializing Comprehensive Cross-Modal System ===");
        
        try {
            // Step 1: Initialize unified BLE manager
            bleManager = UnifiedBleManager.getInstance(context);
            if (!bleManager.initialize()) {
                Log.e(TAG, "Failed to initialize unified BLE manager");
                return false;
            }
            Log.i(TAG, "✓ Unified BLE Manager initialized");
            
            // Step 2: Initialize cross-modal synchronization manager
            syncManager = CrossModalSyncManager.getInstance(context);
            Log.i(TAG, "✓ Cross-Modal Sync Manager initialized");
            
            // Step 3: Set up comprehensive sync listener
            syncManager.addSyncListener(new ComprehensiveSyncListener());
            Log.i(TAG, "✓ Sync listener registered");
            
            // Step 4: Start device discovery
            if (!startComprehensiveDeviceDiscovery()) {
                Log.e(TAG, "Failed to start device discovery");
                return false;
            }
            Log.i(TAG, "✓ Device discovery started");
            
            Log.i(TAG, "=== Comprehensive Cross-Modal System Ready ===");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize comprehensive system", e);
            return false;
        }
    }
    

    private boolean startComprehensiveDeviceDiscovery() {
        try {
            // Start BLE device discovery
            boolean bleSuccess = bleManager.startUnifiedDeviceDiscovery(new ComprehensiveDeviceListener());
            
            if (bleSuccess) {
                Log.i(TAG, "BLE device discovery started successfully");
                return true;
            } else {
                Log.w(TAG, "BLE device discovery failed to start");
                return false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting comprehensive device discovery", e);
            return false;
        }
    }
    

    public boolean registerNonBleDevices() {
        Log.i(TAG, "=== Registering Non-BLE Devices for Cross-Modal Sync ===");
        
        try {
            // Example: Register USB thermal camera
            CrossModalSyncManager.DeviceCapabilities thermalCapabilities = 
                new CrossModalSyncManager.DeviceCapabilities(
                    false, // supportsHardwareSync (USB cameras typically don't)
                    true,  // supportsTimestampGeneration
                    30,    // maxSamplingRateHz
                    10000  // syncAccuracyMicros (10ms for USB)
                );
            
            boolean thermalRegistered = syncManager.registerDevice(
                "usb_thermal_001",
                "USB Thermal Camera TC001",
                CrossModalSyncManager.DeviceCategory.USB_CAMERA,
                new Object(), // Placeholder device reference
                thermalCapabilities
            );
            
            // Example: Register RGB camera
            CrossModalSyncManager.DeviceCapabilities rgbCapabilities = 
                new CrossModalSyncManager.DeviceCapabilities(
                    false, // supportsHardwareSync
                    true,  // supportsTimestampGeneration  
                    60,    // maxSamplingRateHz
                    16667  // syncAccuracyMicros (16.67ms for 60fps)
                );
            
            boolean rgbRegistered = syncManager.registerDevice(
                "rgb_camera_001",
                "Android RGB Camera",
                CrossModalSyncManager.DeviceCategory.RGB_CAMERA,
                new Object(), // Placeholder device reference
                rgbCapabilities
            );
            
            // Example: Register network device (PC Controller)
            CrossModalSyncManager.DeviceCapabilities networkCapabilities = 
                new CrossModalSyncManager.DeviceCapabilities(
                    true,  // supportsHardwareSync (network sync)
                    true,  // supportsTimestampGeneration
                    1000,  // maxSamplingRateHz (high network rate)
                    1000   // syncAccuracyMicros (1ms network)
                );
            
            boolean networkRegistered = syncManager.registerDevice(
                "pc_controller_001",
                "PC Controller Hub",
                CrossModalSyncManager.DeviceCategory.NETWORK_DEVICE,
                new Object(), // Placeholder device reference
                networkCapabilities
            );
            
            Log.i(TAG, "Device registration results:");
            Log.i(TAG, "  Thermal Camera: " + (thermalRegistered ? "✓" : "✗"));
            Log.i(TAG, "  RGB Camera: " + (rgbRegistered ? "✓" : "✗"));
            Log.i(TAG, "  PC Controller: " + (networkRegistered ? "✓" : "✗"));
            
            return thermalRegistered && rgbRegistered && networkRegistered;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to register non-BLE devices", e);
            return false;
        }
    }
    

    public boolean startMultiModalRecording() {
        Log.i(TAG, "=== Starting Multi-Modal Recording ===");
        
        try {
            // Register non-BLE devices first
            if (!registerNonBleDevices()) {
                Log.w(TAG, "Some non-BLE device registrations failed, continuing anyway");
            }
            
            // Register BLE devices with cross-modal sync
            if (!bleManager.registerDevicesForCrossModalSync()) {
                Log.w(TAG, "BLE device registration failed, continuing anyway");
            }
            
            // Get all registered devices
            List<CrossModalSyncManager.RegisteredDevice> allDevices = syncManager.getRegisteredDevices();
            Log.i(TAG, "Total registered devices: " + allDevices.size());
            
            for (CrossModalSyncManager.RegisteredDevice device : allDevices) {
                Log.i(TAG, "  - " + device.getDeviceName() + " [" + device.getCategory().getDisplayName() + "]");
            }
            
            // Start synchronized recording
            boolean syncStarted = syncManager.startSynchronizedRecording();
            
            if (syncStarted) {
                Log.i(TAG, "✓ Multi-modal synchronized recording started successfully");
                
                // Also start BLE-specific recording if available
                boolean bleRecordingStarted = bleManager.startCrossModalRecording();
                Log.i(TAG, "BLE cross-modal recording: " + (bleRecordingStarted ? "✓" : "✗"));
                
                return true;
            } else {
                Log.e(TAG, "✗ Failed to start synchronized recording");
                return false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start multi-modal recording", e);
            return false;
        }
    }
    

    public boolean stopMultiModalRecording() {
        Log.i(TAG, "=== Stopping Multi-Modal Recording ===");
        
        try {
            // Stop synchronized recording
            boolean syncStopped = syncManager.stopSynchronizedRecording();
            
            // Stop BLE recording
            boolean bleStopped = bleManager.stopCrossModalRecording();
            
            Log.i(TAG, "Recording stop results:");
            Log.i(TAG, "  Synchronized recording: " + (syncStopped ? "✓" : "✗"));
            Log.i(TAG, "  BLE recording: " + (bleStopped ? "✓" : "✗"));
            
            return syncStopped && bleStopped;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop multi-modal recording", e);
            return false;
        }
    }
    

    public void printSystemStatus() {
        Log.i(TAG, "=== Comprehensive System Status ===");
        
        try {
            // BLE system status
            UnifiedBleManager.SystemBleStatus bleStatus = bleManager.getSystemBleStatus();
            Log.i(TAG, "BLE System:");
            Log.i(TAG, "  Active connections: " + bleStatus.activeConnections);
            Log.i(TAG, "  Multi-device mode: " + bleStatus.multiDeviceMode);
            Log.i(TAG, "  Enhanced error recovery: " + bleStatus.enhancedErrorRecovery);
            Log.i(TAG, "  Total devices connected: " + bleStatus.totalDevicesConnected);
            
            // Cross-modal sync status
            boolean isSynchronizing = syncManager.isSynchronizing();
            Log.i(TAG, "Cross-Modal Sync:");
            Log.i(TAG, "  Synchronization active: " + isSynchronizing);
            
            // Device status by category
            for (CrossModalSyncManager.DeviceCategory category : CrossModalSyncManager.DeviceCategory.values()) {
                List<CrossModalSyncManager.RegisteredDevice> categoryDevices = syncManager.getDevicesByCategory(category);
                int activeCount = 0;
                for (CrossModalSyncManager.RegisteredDevice device : categoryDevices) {
                    if (device.isActive()) activeCount++;
                }
                
                if (!categoryDevices.isEmpty()) {
                    Log.i(TAG, "  " + category.getDisplayName() + ": " + 
                              activeCount + "/" + categoryDevices.size() + " active");
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to get system status", e);
        }
    }
    

    private class ComprehensiveDeviceListener implements UnifiedBleManager.UnifiedScanListener {
        @Override
        public void onShimmerDeviceFound(android.bluetooth.BluetoothDevice device, 
                                       UnifiedBleManager.DeviceType type, int rssi, byte[] scanRecord) {
            String deviceName = BluetoothPermissionUtils.getDeviceName(context, device);
            Log.i(TAG, "Shimmer device discovered: " + deviceName + " [" + type + "] RSSI: " + rssi);
            
            // Auto-register Shimmer devices for cross-modal sync
            try {
                CrossModalSyncManager.DeviceCapabilities capabilities = 
                    new CrossModalSyncManager.DeviceCapabilities(
                        true,  // supportsHardwareSync
                        true,  // supportsTimestampGeneration
                        128,   // maxSamplingRateHz (GSR)
                        1000   // syncAccuracyMicros (1ms)
                    );
                
                syncManager.registerDevice(
                    BluetoothPermissionUtils.getDeviceAddress(context, device),
                    deviceName != null && !deviceName.isEmpty() ? deviceName : "Shimmer Device",
                    CrossModalSyncManager.DeviceCategory.BLE_SENSOR,
                    device,
                    capabilities
                );
                
                Log.i(TAG, "Auto-registered Shimmer device for cross-modal sync");
                
            } catch (Exception e) {
                Log.w(TAG, "Failed to auto-register Shimmer device", e);
            }
        }
        
        @Override
        public void onTopdonDeviceFound(android.bluetooth.BluetoothDevice device, 
                                      UnifiedBleManager.DeviceType type, int rssi, byte[] scanRecord) {
            String deviceName = BluetoothPermissionUtils.getDeviceName(context, device);
            Log.i(TAG, "Topdon device discovered: " + deviceName + " [" + type + "] RSSI: " + rssi);
            
            // Auto-register Topdon devices for cross-modal sync
            try {
                CrossModalSyncManager.DeviceCapabilities capabilities = 
                    new CrossModalSyncManager.DeviceCapabilities(
                        true,  // supportsHardwareSync
                        true,  // supportsTimestampGeneration
                        30,    // maxSamplingRateHz (thermal)
                        5000   // syncAccuracyMicros (5ms)
                    );
                
                syncManager.registerDevice(
                    BluetoothPermissionUtils.getDeviceAddress(context, device),
                    deviceName != null && !deviceName.isEmpty() ? deviceName : "Topdon Device",
                    CrossModalSyncManager.DeviceCategory.BLE_SENSOR,
                    device,
                    capabilities
                );
                
                Log.i(TAG, "Auto-registered Topdon device for cross-modal sync");
                
            } catch (Exception e) {
                Log.w(TAG, "Failed to auto-register Topdon device", e);
            }
        }
        
        @Override
        public void onUnknownDeviceFound(android.bluetooth.BluetoothDevice device, int rssi, byte[] scanRecord) {
            String deviceName = BluetoothPermissionUtils.getDeviceName(context, device);
            Log.d(TAG, "Unknown device discovered: " + deviceName + " RSSI: " + rssi);
        }
        
        @Override
        public void onScanError(int errorCode, String message) {
            Log.e(TAG, "Device discovery error: " + message + " (code: " + errorCode + ")");
        }
        
        @Override
        public void onScanComplete() {
            Log.i(TAG, "Device discovery completed");
        }
    }
    

    private class ComprehensiveSyncListener implements CrossModalSyncManager.CrossModalSyncListener {
        @Override
        public void onDeviceRegistered(@NonNull CrossModalSyncManager.RegisteredDevice device) {
            Log.i(TAG, "Device registered for sync: " + device.getDeviceName() + 
                      " [" + device.getCategory().getDisplayName() + "]");
        }
        
        @Override
        public void onDeviceUnregistered(@NonNull String deviceId) {
            Log.i(TAG, "Device unregistered from sync: " + deviceId);
        }
        
        @Override
        public void onSynchronizationStarted(@NonNull List<CrossModalSyncManager.RegisteredDevice> activeDevices) {
            Log.i(TAG, "Synchronization started with " + activeDevices.size() + " devices:");
            for (CrossModalSyncManager.RegisteredDevice device : activeDevices) {
                Log.i(TAG, "  - " + device.getDeviceName() + " [" + device.getCategory().getDisplayName() + "]");
            }
        }
        
        @Override
        public void onSynchronizationStopped() {
            Log.i(TAG, "Synchronization stopped");
        }
        
        @Override
        public void onSyncEvent(@NonNull String eventType, long masterTimestamp, 
                              @NonNull Map<String, Long> deviceTimestamps) {
            Log.i(TAG, "Sync event: " + eventType + " at " + masterTimestamp);
            Log.d(TAG, "Device timestamps: " + deviceTimestamps.size() + " devices");
        }
        
        @Override
        public void onSyncQualityUpdate(@NonNull String deviceId, 
                                      @NonNull CrossModalSyncManager.SyncQualityMetrics metrics) {
            Log.d(TAG, "Sync quality update for " + deviceId + 
                      ": accuracy=" + metrics.getSyncAccuracyPercent() + "%");
        }
    }
}