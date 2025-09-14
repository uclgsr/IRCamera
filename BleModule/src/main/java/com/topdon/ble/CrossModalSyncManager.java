package com.topdon.ble;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;


public class CrossModalSyncManager {
    private static final String TAG = "CrossModalSyncManager";
    
    // Singleton instance
    private static volatile CrossModalSyncManager instance;
    private static final Object instanceLock = new Object();
    
    // Context and state
    private final Context context;
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    private final AtomicBoolean isSynchronizing = new AtomicBoolean(false);
    
    // Device management
    private final Map<String, RegisteredDevice> registeredDevices = new ConcurrentHashMap<>();
    private final Map<DeviceCategory, List<RegisteredDevice>> devicesByCategory = new ConcurrentHashMap<>();
    
    // Synchronization state
    private final AtomicLong masterTimestampNanos = new AtomicLong(0);
    private final Map<String, Long> deviceClockOffsets = new ConcurrentHashMap<>();
    private final Map<String, SyncQualityMetrics> syncQualityMap = new ConcurrentHashMap<>();
    
    // Listeners
    private final List<CrossModalSyncListener> listeners = new ArrayList<>();
    
    
    public enum DeviceCategory {
        BLE_SENSOR("BLE Sensors", "Shimmer GSR, Topdon BLE devices"),
        USB_CAMERA("USB Cameras", "Thermal cameras, thermal-lite devices"),
        RGB_CAMERA("RGB Cameras", "Android camera, external cameras"),
        NETWORK_DEVICE("Network Devices", "PC Controllers, hub-spoke systems"),
        AUDIO_DEVICE("Audio Devices", "Microphones, audio recorders");
        
        private final String displayName;
        private final String description;
        
        DeviceCategory(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    
    public static class RegisteredDevice {
        private final String deviceId;
        private final String deviceName;
        private final DeviceCategory category;
        private final Object deviceRef; // Actual device object
        private final DeviceCapabilities capabilities;
        private volatile boolean isActive = false;
        private volatile long lastSyncTimestamp = 0;
        
        public RegisteredDevice(String deviceId, String deviceName, DeviceCategory category, 
                              Object deviceRef, DeviceCapabilities capabilities) {
            this.deviceId = deviceId;
            this.deviceName = deviceName;
            this.category = category;
            this.deviceRef = deviceRef;
            this.capabilities = capabilities;
        }
        
        // Getters
        public String getDeviceId() { return deviceId; }
        public String getDeviceName() { return deviceName; }
        public DeviceCategory getCategory() { return category; }
        public Object getDeviceRef() { return deviceRef; }
        public DeviceCapabilities getCapabilities() { return capabilities; }
        public boolean isActive() { return isActive; }
        public long getLastSyncTimestamp() { return lastSyncTimestamp; }
        
        // Internal setters
        void setActive(boolean active) { this.isActive = active; }
        void setLastSyncTimestamp(long timestamp) { this.lastSyncTimestamp = timestamp; }
    }
    
    
    public static class DeviceCapabilities {
        private final boolean supportsHardwareSync;
        private final boolean supportsTimestampGeneration;
        private final int maxSamplingRateHz;
        private final long syncAccuracyMicros;
        
        public DeviceCapabilities(boolean supportsHardwareSync, boolean supportsTimestampGeneration,
                                int maxSamplingRateHz, long syncAccuracyMicros) {
            this.supportsHardwareSync = supportsHardwareSync;
            this.supportsTimestampGeneration = supportsTimestampGeneration;
            this.maxSamplingRateHz = maxSamplingRateHz;
            this.syncAccuracyMicros = syncAccuracyMicros;
        }
        
        // Getters
        public boolean supportsHardwareSync() { return supportsHardwareSync; }
        public boolean supportsTimestampGeneration() { return supportsTimestampGeneration; }
        public int getMaxSamplingRateHz() { return maxSamplingRateHz; }
        public long getSyncAccuracyMicros() { return syncAccuracyMicros; }
    }
    
    
    public static class SyncQualityMetrics {
        private final long avgClockDriftNanos;
        private final long maxClockDriftNanos;
        private final double syncAccuracyPercent;
        private final int missedSyncEvents;
        private final long lastUpdateTimestamp;
        
        public SyncQualityMetrics(long avgClockDriftNanos, long maxClockDriftNanos,
                                double syncAccuracyPercent, int missedSyncEvents, long lastUpdateTimestamp) {
            this.avgClockDriftNanos = avgClockDriftNanos;
            this.maxClockDriftNanos = maxClockDriftNanos;
            this.syncAccuracyPercent = syncAccuracyPercent;
            this.missedSyncEvents = missedSyncEvents;
            this.lastUpdateTimestamp = lastUpdateTimestamp;
        }
        
        // Getters
        public long getAvgClockDriftNanos() { return avgClockDriftNanos; }
        public long getMaxClockDriftNanos() { return maxClockDriftNanos; }
        public double getSyncAccuracyPercent() { return syncAccuracyPercent; }
        public int getMissedSyncEvents() { return missedSyncEvents; }
        public long getLastUpdateTimestamp() { return lastUpdateTimestamp; }
    }
    
    
    public interface CrossModalSyncListener {
        void onDeviceRegistered(@NonNull RegisteredDevice device);
        void onDeviceUnregistered(@NonNull String deviceId);
        void onSynchronizationStarted(@NonNull List<RegisteredDevice> activeDevices);
        void onSynchronizationStopped();
        void onSyncEvent(@NonNull String eventType, long masterTimestamp, @NonNull Map<String, Long> deviceTimestamps);
        void onSyncQualityUpdate(@NonNull String deviceId, @NonNull SyncQualityMetrics metrics);
    }
    
    
    private CrossModalSyncManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
        initializeDeviceCategories();
    }
    
    
    public static CrossModalSyncManager getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (instanceLock) {
                if (instance == null) {
                    instance = new CrossModalSyncManager(context);
                }
            }
        }
        return instance;
    }
    
    
    private void initializeDeviceCategories() {
        for (DeviceCategory category : DeviceCategory.values()) {
            devicesByCategory.put(category, new ArrayList<>());
        }
        isInitialized.set(true);
        Log.i(TAG, "CrossModalSyncManager initialized with " + DeviceCategory.values().length + " device categories");
    }
    
    
    public boolean registerDevice(@NonNull String deviceId, @NonNull String deviceName,
                                @NonNull DeviceCategory category, @NonNull Object deviceRef,
                                @NonNull DeviceCapabilities capabilities) {
        if (!isInitialized.get()) {
            Log.e(TAG, "CrossModalSyncManager not initialized");
            return false;
        }
        
        try {
            RegisteredDevice device = new RegisteredDevice(deviceId, deviceName, category, deviceRef, capabilities);
            
            // Add to main registry
            registeredDevices.put(deviceId, device);
            
            // Add to category list
            List<RegisteredDevice> categoryDevices = devicesByCategory.get(category);
            if (categoryDevices != null) {
                categoryDevices.add(device);
            }
            
            // Initialize clock offset tracking
            deviceClockOffsets.put(deviceId, 0L);
            
            // Initialize sync quality metrics
            syncQualityMap.put(deviceId, new SyncQualityMetrics(0, 0, 100.0, 0, System.nanoTime()));
            
            Log.i(TAG, "Registered device: " + deviceName + " [" + category.getDisplayName() + "] ID: " + deviceId);
            
            // Notify listeners
            notifyDeviceRegistered(device);
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to register device: " + deviceId, e);
            return false;
        }
    }
    
    
    public boolean unregisterDevice(@NonNull String deviceId) {
        RegisteredDevice device = registeredDevices.remove(deviceId);
        if (device != null) {
            // Remove from category list
            List<RegisteredDevice> categoryDevices = devicesByCategory.get(device.getCategory());
            if (categoryDevices != null) {
                categoryDevices.remove(device);
            }
            
            // Clean up tracking data
            deviceClockOffsets.remove(deviceId);
            syncQualityMap.remove(deviceId);
            
            Log.i(TAG, "Unregistered device: " + device.getDeviceName() + " ID: " + deviceId);
            
            // Notify listeners
            notifyDeviceUnregistered(deviceId);
            
            return true;
        }
        return false;
    }
    
    
    public boolean startSynchronizedRecording() {
        if (!isInitialized.get()) {
            Log.e(TAG, "Cannot start recording - manager not initialized");
            return false;
        }
        
        if (isSynchronizing.get()) {
            Log.w(TAG, "Synchronization already in progress");
            return false;
        }
        
        try {
            // Get all active devices
            List<RegisteredDevice> activeDevices = new ArrayList<>();
            for (RegisteredDevice device : registeredDevices.values()) {
                if (device.isActive()) {
                    activeDevices.add(device);
                }
            }
            
            if (activeDevices.isEmpty()) {
                Log.w(TAG, "No active devices available for synchronized recording");
                return false;
            }
            
            // Set master timestamp
            masterTimestampNanos.set(System.nanoTime());
            
            // Start synchronization
            isSynchronizing.set(true);
            
            Log.i(TAG, "Started synchronized recording with " + activeDevices.size() + " devices");
            
            // Notify listeners
            notifySynchronizationStarted(activeDevices);
            
            // Send start command to all devices
            return sendSyncCommand("START_RECORDING", masterTimestampNanos.get());
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start synchronized recording", e);
            isSynchronizing.set(false);
            return false;
        }
    }
    
    
    public boolean stopSynchronizedRecording() {
        if (!isSynchronizing.get()) {
            Log.w(TAG, "No synchronization in progress");
            return false;
        }
        
        try {
            // Send stop command to all devices
            boolean success = sendSyncCommand("STOP_RECORDING", System.nanoTime());
            
            // Stop synchronization
            isSynchronizing.set(false);
            masterTimestampNanos.set(0);
            
            Log.i(TAG, "Stopped synchronized recording");
            
            // Notify listeners
            notifySynchronizationStopped();
            
            return success;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop synchronized recording", e);
            return false;
        }
    }
    
    
    private boolean sendSyncCommand(@NonNull String command, long masterTimestamp) {
        boolean overallSuccess = true;
        Map<String, Long> deviceTimestamps = new ConcurrentHashMap<>();
        
        for (RegisteredDevice device : registeredDevices.values()) {
            if (!device.isActive()) continue;
            
            try {
                long deviceTimestamp = masterTimestamp + deviceClockOffsets.getOrDefault(device.getDeviceId(), 0L);
                deviceTimestamps.put(device.getDeviceId(), deviceTimestamp);
                
                // Send command based on device category
                boolean deviceSuccess = sendCommandToDevice(device, command, deviceTimestamp);
                if (!deviceSuccess) {
                    Log.w(TAG, "Failed to send command " + command + " to device: " + device.getDeviceName());
                    overallSuccess = false;
                }
                
                device.setLastSyncTimestamp(deviceTimestamp);
                
            } catch (Exception e) {
                Log.e(TAG, "Error sending command to device: " + device.getDeviceName(), e);
                overallSuccess = false;
            }
        }
        
        // Notify listeners of sync event
        notifySyncEvent(command, masterTimestamp, deviceTimestamps);
        
        return overallSuccess;
    }
    
    
    private boolean sendCommandToDevice(@NonNull RegisteredDevice device, @NonNull String command, long timestamp) {
        try {
            switch (device.getCategory()) {
                case BLE_SENSOR:
                    return sendBleDeviceCommand(device, command, timestamp);
                case USB_CAMERA:
                    return sendUsbCameraCommand(device, command, timestamp);
                case RGB_CAMERA:
                    return sendRgbCameraCommand(device, command, timestamp);
                case NETWORK_DEVICE:
                    return sendNetworkDeviceCommand(device, command, timestamp);
                case AUDIO_DEVICE:
                    return sendAudioDeviceCommand(device, command, timestamp);
                default:
                    Log.w(TAG, "Unknown device category: " + device.getCategory());
                    return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to send command to device: " + device.getDeviceName(), e);
            return false;
        }
    }
    
    
    private boolean sendBleDeviceCommand(@NonNull RegisteredDevice device, @NonNull String command, long timestamp) {
        Object deviceRef = device.getDeviceRef();
        
        if (deviceRef instanceof UnifiedDevice) {
            UnifiedDevice unifiedDevice = (UnifiedDevice) deviceRef;
            // Implementation would depend on UnifiedDevice interface
            return sendUnifiedDeviceCommand(unifiedDevice, command, timestamp);
        }
        
        // Add support for other BLE device types as needed
        Log.w(TAG, "Unsupported BLE device type: " + deviceRef.getClass().getSimpleName());
        return false;
    }
    
    
    private boolean sendUnifiedDeviceCommand(@NonNull UnifiedDevice device, @NonNull String command, long timestamp) {
        try {
            switch (command) {
                case "START_RECORDING":
                    return device.startRecording(timestamp);
                case "STOP_RECORDING":
                    return device.stopRecording(timestamp);
                case "SYNC_MARK":
                    return device.addSyncMark(timestamp);
                default:
                    Log.w(TAG, "Unknown command for unified device: " + command);
                    return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to send command to unified device", e);
            return false;
        }
    }
    
    
    private boolean sendUsbCameraCommand(@NonNull RegisteredDevice device, @NonNull String command, long timestamp) {
        // Placeholder for USB camera command implementation
        Log.d(TAG, "USB Camera command: " + command + " for device: " + device.getDeviceName());
        return true;
    }
    
    
    private boolean sendRgbCameraCommand(@NonNull RegisteredDevice device, @NonNull String command, long timestamp) {
        // Placeholder for RGB camera command implementation
        Log.d(TAG, "RGB Camera command: " + command + " for device: " + device.getDeviceName());
        return true;
    }
    
    
    private boolean sendNetworkDeviceCommand(@NonNull RegisteredDevice device, @NonNull String command, long timestamp) {
        // Placeholder for network device command implementation  
        Log.d(TAG, "Network Device command: " + command + " for device: " + device.getDeviceName());
        return true;
    }
    
    
    private boolean sendAudioDeviceCommand(@NonNull RegisteredDevice device, @NonNull String command, long timestamp) {
        // Placeholder for audio device command implementation
        Log.d(TAG, "Audio Device command: " + command + " for device: " + device.getDeviceName());
        return true;
    }
    
    
    @NonNull
    public List<RegisteredDevice> getRegisteredDevices() {
        return new ArrayList<>(registeredDevices.values());
    }
    
    
    @NonNull
    public List<RegisteredDevice> getDevicesByCategory(@NonNull DeviceCategory category) {
        List<RegisteredDevice> categoryDevices = devicesByCategory.get(category);
        return categoryDevices != null ? new ArrayList<>(categoryDevices) : new ArrayList<>();
    }
    
    
    @Nullable
    public SyncQualityMetrics getSyncQualityMetrics(@NonNull String deviceId) {
        return syncQualityMap.get(deviceId);
    }
    
    
    public boolean isSynchronizing() {
        return isSynchronizing.get();
    }
    
    
    public void addSyncListener(@NonNull CrossModalSyncListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }
    
    
    public void removeSyncListener(@NonNull CrossModalSyncListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
    
    // Notification methods
    private void notifyDeviceRegistered(@NonNull RegisteredDevice device) {
        synchronized (listeners) {
            for (CrossModalSyncListener listener : listeners) {
                try {
                    listener.onDeviceRegistered(device);
                } catch (Exception e) {
                    Log.e(TAG, "Error notifying device registration", e);
                }
            }
        }
    }
    
    private void notifyDeviceUnregistered(@NonNull String deviceId) {
        synchronized (listeners) {
            for (CrossModalSyncListener listener : listeners) {
                try {
                    listener.onDeviceUnregistered(deviceId);
                } catch (Exception e) {
                    Log.e(TAG, "Error notifying device unregistration", e);
                }
            }
        }
    }
    
    private void notifySynchronizationStarted(@NonNull List<RegisteredDevice> activeDevices) {
        synchronized (listeners) {
            for (CrossModalSyncListener listener : listeners) {
                try {
                    listener.onSynchronizationStarted(new ArrayList<>(activeDevices));
                } catch (Exception e) {
                    Log.e(TAG, "Error notifying synchronization start", e);
                }
            }
        }
    }
    
    private void notifySynchronizationStopped() {
        synchronized (listeners) {
            for (CrossModalSyncListener listener : listeners) {
                try {
                    listener.onSynchronizationStopped();
                } catch (Exception e) {
                    Log.e(TAG, "Error notifying synchronization stop", e);
                }
            }
        }
    }
    
    private void notifySyncEvent(@NonNull String eventType, long masterTimestamp, @NonNull Map<String, Long> deviceTimestamps) {
        synchronized (listeners) {
            for (CrossModalSyncListener listener : listeners) {
                try {
                    listener.onSyncEvent(eventType, masterTimestamp, new ConcurrentHashMap<>(deviceTimestamps));
                } catch (Exception e) {
                    Log.e(TAG, "Error notifying sync event", e);
                }
            }
        }
    }
}