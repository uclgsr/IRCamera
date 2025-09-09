package com.topdon.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Unified BLE Manager that merges all Shimmer Nordic BLE and Topdon BLE functionalities.
 * 
 * This comprehensive manager provides unified access to:
 * - Shimmer Nordic BLE devices (GSR sensors, physiological monitoring)
 * - Topdon BLE devices (thermal cameras, environmental sensors)
 * - Cross-device coordination and synchronization
 * - Unified device discovery and connection management
 * - Enterprise-grade reliability and security
 * 
 * Features:
 * - Unified device discovery supporting both Shimmer and Topdon protocols
 * - Cross-device synchronization for multi-modal sensing
 * - Enhanced connection reliability using Nordic BLE backend
 * - Comprehensive error handling and recovery
 * - Real-time device status monitoring
 * - Unified security layer with device authentication
 * 
 * Usage:
 * UnifiedBleManager manager = UnifiedBleManager.getInstance(context);
 * manager.startUnifiedDeviceDiscovery(listener);
 * manager.connectToShimmerDevice(device, gsrConfig);
 * manager.connectToTopdonDevice(device, thermalConfig);
 * 
 * @author IRCamera Unified BLE Integration Team
 */
@SuppressLint("MissingPermission")
public class UnifiedBleManager {
    private static final String TAG = "UnifiedBleManager";
    
    // Singleton instance
    private static volatile UnifiedBleManager instance;
    
    // Core components
    private final Context context;
    private final BluetoothManager bluetoothManager;
    private final BluetoothAdapter bluetoothAdapter;
    
    // Enhanced BLE managers for different device types
    private final ShimmerBleController shimmerController;
    private final TopdonBleController topdonController;
    
    // Enhanced managers from existing implementation
    private final EnhancedBleManager enhancedBleManager;
    private final SecureBleManager secureBleManager;
    private final PredictiveConnectionManager predictiveManager;
    private final ResearchGradeBleManager researchManager;
    
    // Unified state management
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    private final AtomicBoolean isScanning = new AtomicBoolean(false);
    private final Map<String, UnifiedDevice> connectedDevices = new ConcurrentHashMap<>();
    
    // Device type identification
    public enum DeviceType {
        SHIMMER_GSR,        // Shimmer3 GSR+ sensors
        SHIMMER_PPG,        // Shimmer PPG sensors  
        SHIMMER_IMU,        // Shimmer IMU sensors
        TOPDON_THERMAL,     // Topdon thermal cameras with BLE
        TOPDON_ENV,         // Topdon environmental sensors
        TOPDON_MULTI,       // Topdon multi-sensor devices
        UNKNOWN             // Unknown or generic BLE device
    }
    
    /**
     * Device discovery listener for unified scanning
     */
    public interface UnifiedScanListener {
        void onShimmerDeviceFound(BluetoothDevice device, DeviceType type, int rssi, byte[] scanRecord);
        void onTopdonDeviceFound(BluetoothDevice device, DeviceType type, int rssi, byte[] scanRecord);
        void onUnknownDeviceFound(BluetoothDevice device, int rssi, byte[] scanRecord);
        void onScanError(int errorCode, String message);
        void onScanComplete();
    }
    
    /**
     * Unified connection listener
     */
    public interface UnifiedConnectionListener {
        void onDeviceConnected(UnifiedDevice device);
        void onDeviceDisconnected(UnifiedDevice device, int reason);
        void onConnectionError(UnifiedDevice device, int errorCode, String message);
        void onDataReceived(UnifiedDevice device, byte[] data);
        void onDeviceReady(UnifiedDevice device);
    }
    
    /**
     * Private constructor for singleton pattern
     */
    private UnifiedBleManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;
        
        // Initialize enhanced managers
        this.enhancedBleManager = EnhancedBleManager.getInstance();
        this.secureBleManager = SecureBleManager.getInstance();
        this.predictiveManager = PredictiveConnectionManager.getInstance();
        this.researchManager = ResearchGradeBleManager.getInstance();
        
        // Initialize device controllers
        this.shimmerController = new ShimmerBleController(context, this);
        this.topdonController = new TopdonBleController(context, this);
        
        Log.i(TAG, "UnifiedBleManager initialized with comprehensive BLE support");
    }
    
    /**
     * Get singleton instance
     */
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
    
    /**
     * Initialize the unified BLE manager
     */
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
            
            // Initialize enhanced managers
            enhancedBleManager.initialize(context, true);
            // Note: Other managers may not have public initialize/cleanup methods
            // Initialize device controllers
            shimmerController.initialize();
            topdonController.initialize();
            
            isInitialized.set(true);
            Log.i(TAG, "Unified BLE Manager initialized successfully");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Unified BLE Manager", e);
            return false;
        }
    }
    
    /**
     * Start unified device discovery for both Shimmer and Topdon devices
     */
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
            
            // Start Shimmer device discovery
            shimmerController.startDeviceDiscovery(new ShimmerScanAdapter(listener));
            
            // Start Topdon device discovery  
            topdonController.startDeviceDiscovery(new TopdonScanAdapter(listener));
            
            Log.i(TAG, "Started unified device discovery for Shimmer and Topdon devices");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start unified device discovery", e);
            isScanning.set(false);
            return false;
        }
    }
    
    /**
     * Stop unified device discovery
     */
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
    
    /**
     * Connect to Shimmer device with unified configuration
     */
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
    
    /**
     * Connect to Topdon device with unified configuration
     */
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
    
    /**
     * Get all connected devices
     */
    public List<UnifiedDevice> getConnectedDevices() {
        return new ArrayList<>(connectedDevices.values());
    }
    
    /**
     * Get connected devices by type
     */
    public List<UnifiedDevice> getConnectedDevicesByType(DeviceType type) {
        List<UnifiedDevice> devices = new ArrayList<>();
        for (UnifiedDevice device : connectedDevices.values()) {
            if (device.getDeviceType() == type) {
                devices.add(device);
            }
        }
        return devices;
    }
    
    /**
     * Disconnect device
     */
    public void disconnectDevice(@NonNull String address) {
        UnifiedDevice device = connectedDevices.get(address);
        if (device != null) {
            device.disconnect();
            connectedDevices.remove(address);
            Log.i(TAG, "Disconnected device: " + address);
        }
    }
    
    /**
     * Disconnect all devices
     */
    public void disconnectAllDevices() {
        for (UnifiedDevice device : connectedDevices.values()) {
            device.disconnect();
        }
        connectedDevices.clear();
        Log.i(TAG, "Disconnected all devices");
    }
    
    /**
     * Get enhanced BLE manager for advanced features
     */
    public EnhancedBleManager getEnhancedBleManager() {
        return enhancedBleManager;
    }
    
    /**
     * Get secure BLE manager for security features
     */
    public SecureBleManager getSecureBleManager() {
        return secureBleManager;
    }
    
    /**
     * Get predictive connection manager for AI features
     */
    public PredictiveConnectionManager getPredictiveManager() {
        return predictiveManager;
    }
    
    /**
     * Get research-grade BLE manager for scientific features
     */
    public ResearchGradeBleManager getResearchManager() {
        return researchManager;
    }
    
    /**
     * Cleanup and release resources
     */
    public void cleanup() {
        try {
            stopUnifiedDeviceDiscovery();
            disconnectAllDevices();
            
            shimmerController.cleanup();
            topdonController.cleanup();
            
            // Note: Enhanced managers are singletons and may not need cleanup
            // enhancedBleManager.cleanup();
            // secureBleManager.cleanup(); 
            // predictiveManager.cleanup();
            // researchManager.cleanup();
            
            isInitialized.set(false);
            Log.i(TAG, "UnifiedBleManager cleaned up");
            
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup", e);
        }
    }
    
    // Adapter classes for scan listeners
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