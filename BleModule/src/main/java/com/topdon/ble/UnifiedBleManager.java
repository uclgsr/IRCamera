package com.topdon.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class UnifiedBleManager {
    private static final String TAG = "UnifiedBleManager";

    private static volatile UnifiedBleManager instance;

    private final Context context;
    private final BluetoothManager bluetoothManager;
    private final BluetoothAdapter bluetoothAdapter;

    private final ConcurrentHashMap<String, ConnectionMetrics> deviceMetrics = new ConcurrentHashMap<>();
    private final AtomicBoolean multiDeviceMode = new AtomicBoolean(false);
    private final AtomicInteger activeConnections = new AtomicInteger(0);

    private final AtomicBoolean enhancedErrorRecovery = new AtomicBoolean(true);
    private final AtomicBoolean connectionOptimization = new AtomicBoolean(true);
    private final AtomicBoolean dataLossDetection = new AtomicBoolean(true);

    private final ShimmerBleController shimmerController;
    private final TopdonBleController topdonController;
    private final ConcurrentHashMap<String, UnifiedDevice> connectedDevices = new ConcurrentHashMap<>();
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    private final AtomicBoolean isScanning = new AtomicBoolean(false);
    private final ConcurrentHashMap<String, Boolean> gsrDevices = new ConcurrentHashMap<>();
    private EasyBLE easyBLE;

    private UnifiedBleManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;


        this.shimmerController = new ShimmerBleController(context, this);
        this.topdonController = new TopdonBleController(context, this);

        Log.i(TAG, "UnifiedBleManager initialized with comprehensive BLE support and cross-modal coordination");
    }

    public static UnifiedBleManager getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (UnifiedBleManager.class) {
                if (instance == null) {
                    instance = new UnifiedBleManager(context);
                }
            }
        }
        return instance;
    }

    public boolean initialize() {
        if (isInitialized.get()) {
            return true;
        }

        try {
            if (bluetoothAdapter == null) {
                Log.e(TAG, "Bluetooth adapter not available");
                return false;
            }

            if (!bluetoothAdapter.isEnabled()) {
                Log.w(TAG, "Bluetooth is not enabled");
                return false;
            }

            this.easyBLE = EasyBLE.getBuilder().setUseNordicBleBackend(true).build();

            shimmerController.initialize();
            topdonController.initialize();

            try {
                CrossModalSyncManager syncManager = CrossModalSyncManager.getInstance(context);
                Log.i(TAG, "Cross-modal synchronization manager initialized for unified BLE coordination");
            } catch (Exception e) {
                Log.w(TAG, "Cross-modal sync manager initialization failed, continuing without sync", e);
            }

            isInitialized.set(true);
            Log.i(TAG, "Unified BLE Manager initialized successfully with cross-modal capabilities");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Unified BLE Manager", e);
            return false;
        }
    }

    public boolean startUnifiedDeviceDiscovery(@NonNull UnifiedScanListener listener) {
        if (!isInitialized.get()) {
            Log.e(TAG, "Manager not initialized");
            return false;
        }

        if (isScanning.get()) {
            Log.w(TAG, "Scanning already in progress");
            return false;
        }

        try {
            isScanning.set(true);

            shimmerController.startDeviceDiscovery(new ShimmerScanAdapter(listener));

            topdonController.startDeviceDiscovery(new TopdonScanAdapter(listener));

            Log.i(TAG, "Started unified device discovery for Shimmer and Topdon devices");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Failed to start unified device discovery", e);
            isScanning.set(false);
            return false;
        }
    }

    public void stopUnifiedDeviceDiscovery() {
        if (!isScanning.get()) {
            return;
        }

        try {
            shimmerController.stopDeviceDiscovery();
            topdonController.stopDeviceDiscovery();
            isScanning.set(false);

            Log.i(TAG, "Stopped unified device discovery");

        } catch (Exception e) {
            Log.e(TAG, "Error stopping unified device discovery", e);
        }
    }

    public UnifiedDevice connectToShimmerDevice(@NonNull BluetoothDevice device,
                                                @NonNull ShimmerDeviceConfig config,
                                                @NonNull UnifiedConnectionListener listener) {
        if (!isInitialized.get()) {
            throw new IllegalStateException("Manager not initialized");
        }

        try {
            UnifiedDevice unifiedDevice = shimmerController.connectDevice(device, config, listener);
            if (unifiedDevice != null) {
                connectedDevices.put(device.getAddress(), unifiedDevice);
                Log.i(TAG, "Connected to Shimmer device: " + device.getAddress());
            }
            return unifiedDevice;

        } catch (Exception e) {
            Log.e(TAG, "Failed to connect to Shimmer device", e);
            return null;
        }
    }

    public UnifiedDevice connectToTopdonDevice(@NonNull BluetoothDevice device,
                                               @NonNull TopdonDeviceConfig config,
                                               @NonNull UnifiedConnectionListener listener) {
        if (!isInitialized.get()) {
            throw new IllegalStateException("Manager not initialized");
        }

        try {
            UnifiedDevice unifiedDevice = topdonController.connectDevice(device, config, listener);
            if (unifiedDevice != null) {
                connectedDevices.put(device.getAddress(), unifiedDevice);
                Log.i(TAG, "Connected to Topdon device: " + device.getAddress());
            }
            return unifiedDevice;

        } catch (Exception e) {
            Log.e(TAG, "Failed to connect to Topdon device", e);
            return null;
        }
    }

    public List<UnifiedDevice> getConnectedDevices() {
        return new ArrayList<>(connectedDevices.values());
    }

    public List<UnifiedDevice> getConnectedDevicesByType(DeviceType type) {
        List<UnifiedDevice> devices = new ArrayList<>();
        for (UnifiedDevice device : connectedDevices.values()) {
            if (device.getDeviceType() == type) {
                devices.add(device);
            }
        }
        return devices;
    }

    public void disconnectDevice(@NonNull String address) {
        UnifiedDevice device = connectedDevices.get(address);
        if (device != null) {
            device.disconnect();
            connectedDevices.remove(address);
            Log.i(TAG, "Disconnected device: " + address);
        }
    }

    public void disconnectAllDevices() {
        for (UnifiedDevice device : connectedDevices.values()) {
            device.disconnect();
        }
        connectedDevices.clear();
        Log.i(TAG, "Disconnected all devices");
    }

    public void cleanup() {
        try {
            stopUnifiedDeviceDiscovery();
            disconnectAllDevices();

            shimmerController.cleanup();
            topdonController.cleanup();

            isInitialized.set(false);
            Log.i(TAG, "UnifiedBleManager cleaned up");

        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup", e);
        }
    }

    public boolean initialize(@NonNull Context context, boolean enableMultiDevice) {
        if (isInitialized.get()) {
            return true;
        }

        this.multiDeviceMode.set(enableMultiDevice);
        return initialize();
    }

    public void enableMultiDeviceMode(boolean enabled) {
        this.multiDeviceMode.set(enabled);
        Log.i(TAG, "Multi-device mode " + (enabled ? "enabled" : "disabled"));
    }

    public SystemBleStatus getSystemStatus() {
        return new SystemBleStatus(
                activeConnections.get(),
                multiDeviceMode.get(),
                enhancedErrorRecovery.get(),
                deviceMetrics.size()
        );
    }

    public void markAsGsrSensor(@NonNull String deviceAddress) {
        gsrDevices.put(deviceAddress, true);
        Log.i(TAG, "Device " + deviceAddress + " marked as GSR sensor");
    }

    @NonNull
    public List<UnifiedDevice> getConnectedShimmerDevices() {
        List<UnifiedDevice> shimmerDevices = new ArrayList<>();
        
        // Return actual connected Shimmer devices from shimmerController
        if (shimmerController != null) {
            try {
                shimmerDevices.addAll(shimmerController.getConnectedDevices());
                Log.d(TAG, "Found " + shimmerDevices.size() + " connected Shimmer devices");
            } catch (Exception e) {
                Log.e(TAG, "Error getting connected Shimmer devices", e);
            }
        }
        
        return shimmerDevices;
    }

    /**
     * Scan for nearby Shimmer devices using BLE discovery
     * @param scanDurationMs Duration of scan in milliseconds
     * @param callback Callback to receive discovered devices
     */
    public void scanForShimmerDevices(long scanDurationMs, ShimmerScanCallback callback) {
        if (shimmerController != null) {
            shimmerController.scanForDevices(scanDurationMs, callback);
        } else {
            Log.e(TAG, "Shimmer controller not initialized for scanning");
            if (callback != null) {
                callback.onScanFailed("Shimmer controller not available");
            }
        }
    }

    /**
     * Interface for Shimmer device scan callbacks
     */
    public interface ShimmerScanCallback {
        void onDeviceFound(UnifiedDevice device);
        void onScanComplete(List<UnifiedDevice> foundDevices);
        void onScanFailed(String error);
    }

    @NonNull
    public List<UnifiedDevice> getConnectedTopdonDevices() {
        List<UnifiedDevice> topdonDevices = new ArrayList<>();
        
        // Return actual connected Topdon devices from topdonController  
        if (topdonController != null) {
            try {
                topdonDevices.addAll(topdonController.getConnectedDevices());
                Log.d(TAG, "Found " + topdonDevices.size() + " connected Topdon devices");
            } catch (Exception e) {
                Log.e(TAG, "Error getting connected Topdon devices", e);
            }
        }
        
        return topdonDevices;
    }

    @NonNull
    public SystemBleStatus getSystemBleStatus() {
        return new SystemBleStatus(
                activeConnections.get(),
                true, // multiDeviceMode
                true, // enhancedErrorRecovery
                connectedDevices.size()
        );
    }

    @Nullable
    public Connection connectWithEnhancements(@NonNull String deviceAddress) {
        Log.i(TAG, "Enhanced connection attempt for device: " + deviceAddress);

        ConnectionMetrics metrics = deviceMetrics.computeIfAbsent(deviceAddress,
                k -> new ConnectionMetrics());
        metrics.connectAttempts.incrementAndGet();

        Connection connection = easyBLE.connect(deviceAddress);

        if (connection != null) {
            activeConnections.incrementAndGet();
            metrics.lastConnectionTime.set(System.currentTimeMillis());
            metrics.successfulConnections.incrementAndGet();
            Log.i(TAG, "Enhanced connection successful for device: " + deviceAddress);
        } else {
            Log.w(TAG, "Enhanced connection failed for device: " + deviceAddress);
        }

        return connection;
    }

    public boolean registerDevicesForCrossModalSync() {
        try {
            CrossModalSyncManager syncManager = CrossModalSyncManager.getInstance(context);

            List<UnifiedDevice> shimmerDevices = getConnectedShimmerDevices();
            for (UnifiedDevice device : shimmerDevices) {
                CrossModalSyncManager.DeviceCapabilities capabilities =
                        new CrossModalSyncManager.DeviceCapabilities(
                                true,  // supportsHardwareSync
                                true,  // supportsTimestampGeneration
                                128,   // maxSamplingRateHz (for GSR)
                                1000   // syncAccuracyMicros (1ms)
                        );

                syncManager.registerDevice(
                        device.getDeviceId(),
                        device.getDeviceName(),
                        CrossModalSyncManager.DeviceCategory.BLE_SENSOR,
                        device,
                        capabilities
                );
            }

            List<UnifiedDevice> topdonDevices = getConnectedTopdonDevices();
            for (UnifiedDevice device : topdonDevices) {
                CrossModalSyncManager.DeviceCapabilities capabilities =
                        new CrossModalSyncManager.DeviceCapabilities(
                                true,  // supportsHardwareSync
                                true,  // supportsTimestampGeneration
                                30,    // maxSamplingRateHz (for thermal)
                                5000   // syncAccuracyMicros (5ms)
                        );

                syncManager.registerDevice(
                        device.getDeviceId(),
                        device.getDeviceName(),
                        CrossModalSyncManager.DeviceCategory.BLE_SENSOR,
                        device,
                        capabilities
                );
            }

            Log.i(TAG, "Registered " + (shimmerDevices.size() + topdonDevices.size()) +
                    " BLE devices for cross-modal synchronization");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Failed to register devices for cross-modal sync", e);
            return false;
        }
    }

    public boolean startCrossModalRecording() {
        try {

            registerDevicesForCrossModalSync();

            CrossModalSyncManager syncManager = CrossModalSyncManager.getInstance(context);
            return syncManager.startSynchronizedRecording();

        } catch (Exception e) {
            Log.e(TAG, "Failed to start cross-modal recording", e);
            return false;
        }
    }

    public boolean stopCrossModalRecording() {
        try {
            CrossModalSyncManager syncManager = CrossModalSyncManager.getInstance(context);
            return syncManager.stopSynchronizedRecording();

        } catch (Exception e) {
            Log.e(TAG, "Failed to stop cross-modal recording", e);
            return false;
        }
    }

    @NonNull
    public CrossModalSyncManager getCrossModalSyncManager() {
        return CrossModalSyncManager.getInstance(context);
    }

    public enum DeviceType {
        SHIMMER_GSR,        // Shimmer3 GSR+ sensors
        SHIMMER_PPG,        // Shimmer PPG sensors
        SHIMMER_IMU,        // Shimmer IMU sensors
        TOPDON_THERMAL,     // Topdon thermal cameras with BLE
        TOPDON_ENV,         // Topdon environmental sensors
        TOPDON_MULTI,       // Topdon multi-sensor devices
        UNKNOWN             // Unknown or generic BLE device
    }

    public interface UnifiedScanListener {
        void onShimmerDeviceFound(BluetoothDevice device, DeviceType type, int rssi, byte[] scanRecord);

        void onTopdonDeviceFound(BluetoothDevice device, DeviceType type, int rssi, byte[] scanRecord);

        void onUnknownDeviceFound(BluetoothDevice device, int rssi, byte[] scanRecord);

        void onScanError(int errorCode, String message);

        void onScanComplete();
    }

    public interface UnifiedConnectionListener {
        void onDeviceConnected(UnifiedDevice device);

        void onDeviceDisconnected(UnifiedDevice device, int reason);

        void onConnectionError(UnifiedDevice device, int errorCode, String message);

        void onDataReceived(UnifiedDevice device, byte[] data);

        void onDeviceReady(UnifiedDevice device);
    }

    public static class ConnectionMetrics {
        public final AtomicLong connectAttempts = new AtomicLong(0);
        public final AtomicLong successfulConnections = new AtomicLong(0);
        public final AtomicLong disconnections = new AtomicLong(0);
        public final AtomicLong dataPacketsReceived = new AtomicLong(0);
        public final AtomicLong lastConnectionTime = new AtomicLong(0);

        public double getReliabilityScore() {
            long attempts = connectAttempts.get();
            return attempts > 0 ? (double) successfulConnections.get() / attempts : 0.0;
        }
    }

    public static class SystemBleStatus {
        public final int activeConnections;
        public final boolean multiDeviceMode;
        public final boolean enhancedErrorRecovery;
        public final long totalDevicesConnected;

        public SystemBleStatus(int activeConnections, boolean multiDeviceMode,
                               boolean enhancedErrorRecovery, long totalDevicesConnected) {
            this.activeConnections = activeConnections;
            this.multiDeviceMode = multiDeviceMode;
            this.enhancedErrorRecovery = enhancedErrorRecovery;
            this.totalDevicesConnected = totalDevicesConnected;
        }
    }

    private static class ShimmerScanAdapter implements ShimmerBleController.ShimmerScanListener {
        private final UnifiedScanListener unifiedListener;

        public ShimmerScanAdapter(UnifiedScanListener listener) {
            this.unifiedListener = listener;
        }

        @Override
        public void onShimmerDeviceFound(BluetoothDevice device, DeviceType type, int rssi, byte[] scanRecord) {
            unifiedListener.onShimmerDeviceFound(device, type, rssi, scanRecord);
        }

        @Override
        public void onScanError(int errorCode, String message) {
            unifiedListener.onScanError(errorCode, message);
        }

        @Override
        public void onScanComplete() {
            unifiedListener.onScanComplete();
        }
    }

    private static class TopdonScanAdapter implements TopdonBleController.TopdonScanListener {
        private final UnifiedScanListener unifiedListener;

        public TopdonScanAdapter(UnifiedScanListener listener) {
            this.unifiedListener = listener;
        }

        @Override
        public void onTopdonDeviceFound(BluetoothDevice device, DeviceType type, int rssi, byte[] scanRecord) {
            unifiedListener.onTopdonDeviceFound(device, type, rssi, scanRecord);
        }

        @Override
        public void onScanError(int errorCode, String message) {
            unifiedListener.onScanError(errorCode, message);
        }

        @Override
        public void onScanComplete() {
            unifiedListener.onScanComplete();
        }
    }
}
