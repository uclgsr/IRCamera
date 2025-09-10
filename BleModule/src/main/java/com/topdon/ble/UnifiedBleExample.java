package com.topdon.ble;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.topdon.ble.util.BluetoothPermissionUtils;

import java.util.List;

/**
 * Comprehensive example showing how to use the UnifiedBleManager for both
 * Shimmer Nordic and Topdon BLE device integration.
 * 
 * This example demonstrates:
 * - Device discovery for both Shimmer and Topdon devices
 * - Connection management with configuration
 * - Data streaming and device coordination
 * - Error handling and recovery
 * - Cross-device synchronization
 * 
 * @author IRCamera Unified BLE Integration Team
 */
public class UnifiedBleExample {
    private static final String TAG = "UnifiedBleExample";
    
    private final Context context;
    private UnifiedBleManager unifiedBleManager;
    
    // Connected devices
    private UnifiedDevice shimmerGSRDevice;
    private UnifiedDevice topdonThermalDevice;
    
    public UnifiedBleExample(@NonNull Context context) {
        this.context = context;
    }
    
    /**
     * Initialize and start comprehensive BLE operations
     */
    public void startComprehensiveExample() {
        Log.i(TAG, "Starting comprehensive Shimmer Nordic and Topdon BLE example");
        
        // Initialize unified BLE manager
        unifiedBleManager = UnifiedBleManager.getInstance(context);
        if (!unifiedBleManager.initialize()) {
            Log.e(TAG, "Failed to initialize unified BLE manager");
            return;
        }
        
        // Start device discovery
        startDeviceDiscovery();
    }
    
    /**
     * Start unified device discovery for both Shimmer and Topdon devices
     */
    private void startDeviceDiscovery() {
        Log.i(TAG, "Starting unified device discovery");
        
        unifiedBleManager.startUnifiedDeviceDiscovery(new UnifiedBleManager.UnifiedScanListener() {
            @Override
            public void onShimmerDeviceFound(BluetoothDevice device, UnifiedBleManager.DeviceType type, int rssi, byte[] scanRecord) {
                String deviceName = BluetoothPermissionUtils.getDeviceName(context, device);
                String deviceAddress = BluetoothPermissionUtils.getDeviceAddress(context, device);
                Log.i(TAG, "Found Shimmer device: " + deviceName + " (" + deviceAddress + ") Type: " + type + " RSSI: " + rssi);
                
                // Connect to first GSR device found
                if (type == UnifiedBleManager.DeviceType.SHIMMER_GSR && shimmerGSRDevice == null) {
                    connectToShimmerGSRDevice(device);
                }
            }
            
            @Override
            public void onTopdonDeviceFound(BluetoothDevice device, UnifiedBleManager.DeviceType type, int rssi, byte[] scanRecord) {
                String deviceName = BluetoothPermissionUtils.getDeviceName(context, device);
                String deviceAddress = BluetoothPermissionUtils.getDeviceAddress(context, device);
                Log.i(TAG, "Found Topdon device: " + deviceName + " (" + deviceAddress + ") Type: " + type + " RSSI: " + rssi);
                
                // Connect to first thermal device found
                if (type == UnifiedBleManager.DeviceType.TOPDON_THERMAL && topdonThermalDevice == null) {
                    connectToTopdonThermalDevice(device);
                }
            }
            
            @Override
            public void onUnknownDeviceFound(BluetoothDevice device, int rssi, byte[] scanRecord) {
                String deviceName = BluetoothPermissionUtils.getDeviceName(context, device);
                String deviceAddress = BluetoothPermissionUtils.getDeviceAddress(context, device);
                Log.d(TAG, "Found unknown BLE device: " + deviceName + " (" + deviceAddress + ")");
            }
            
            @Override
            public void onScanError(int errorCode, String message) {
                Log.e(TAG, "Scan error: " + errorCode + " - " + message);
            }
            
            @Override
            public void onScanComplete() {
                Log.i(TAG, "Device discovery completed");
                
                // Check if we have all required devices
                checkDeviceReadiness();
            }
        });
    }
    
    /**
     * Connect to Shimmer GSR device with optimal configuration
     */
    private void connectToShimmerGSRDevice(BluetoothDevice device) {
        Log.i(TAG, "Connecting to Shimmer GSR device: " + device.getAddress());
        
        // Create optimal GSR configuration
        ShimmerDeviceConfig gsrConfig = ShimmerDeviceConfig.createDefaultGSRConfig();
        
        shimmerGSRDevice = unifiedBleManager.connectToShimmerDevice(
            device, 
            gsrConfig, 
            new UnifiedConnectionListener()
        );
        
        if (shimmerGSRDevice != null) {
            Log.i(TAG, "Shimmer GSR device connection initiated");
        }
    }
    
    /**
     * Connect to Topdon thermal device with optimal configuration
     */
    private void connectToTopdonThermalDevice(BluetoothDevice device) {
        Log.i(TAG, "Connecting to Topdon thermal device: " + device.getAddress());
        
        // Create optimal thermal configuration
        TopdonDeviceConfig thermalConfig = TopdonDeviceConfig.createDefaultThermalConfig();
        
        topdonThermalDevice = unifiedBleManager.connectToTopdonDevice(
            device,
            thermalConfig,
            new UnifiedConnectionListener()
        );
        
        if (topdonThermalDevice != null) {
            Log.i(TAG, "Topdon thermal device connection initiated");
        }
    }
    
    /**
     * Check if all required devices are ready for synchronized operation
     */
    private void checkDeviceReadiness() {
        List<UnifiedDevice> connectedDevices = unifiedBleManager.getConnectedDevices();
        Log.i(TAG, "Connected devices: " + connectedDevices.size());
        
        boolean hasShimmerGSR = false;
        boolean hasTopdonThermal = false;
        
        for (UnifiedDevice device : connectedDevices) {
            if (device.isConnected()) {
                switch (device.getDeviceType()) {
                    case SHIMMER_GSR:
                        hasShimmerGSR = true;
                        break;
                    case TOPDON_THERMAL:
                        hasTopdonThermal = true;
                        break;
                }
            }
        }
        
        if (hasShimmerGSR && hasTopdonThermal) {
            Log.i(TAG, "All required devices connected - starting synchronized recording");
            startSynchronizedRecording();
        } else {
            Log.w(TAG, "Not all required devices connected. GSR: " + hasShimmerGSR + ", Thermal: " + hasTopdonThermal);
        }
    }
    
    /**
     * Start synchronized data recording from all connected devices
     */
    private void startSynchronizedRecording() {
        Log.i(TAG, "Starting synchronized multi-modal recording");
        
        // Start GSR data streaming
        if (shimmerGSRDevice != null && shimmerGSRDevice.isConnected()) {
            boolean gsrStarted = shimmerGSRDevice.startDataStreaming();
            Log.i(TAG, "GSR streaming started: " + gsrStarted);
        }
        
        // Start thermal data streaming
        if (topdonThermalDevice != null && topdonThermalDevice.isConnected()) {
            boolean thermalStarted = topdonThermalDevice.startDataStreaming();
            Log.i(TAG, "Thermal streaming started: " + thermalStarted);
        }
        
        Log.i(TAG, "Synchronized multi-modal recording active");
    }
    
    /**
     * Stop all recording and disconnect devices
     */
    public void stopAndCleanup() {
        Log.i(TAG, "Stopping recording and cleaning up");
        
        // Stop data streaming
        if (shimmerGSRDevice != null) {
            shimmerGSRDevice.stopDataStreaming();
        }
        
        if (topdonThermalDevice != null) {
            topdonThermalDevice.stopDataStreaming();
        }
        
        // Disconnect all devices
        if (unifiedBleManager != null) {
            unifiedBleManager.disconnectAllDevices();
            unifiedBleManager.cleanup();
        }
        
        Log.i(TAG, "Cleanup completed");
    }
    
    /**
     * Unified connection listener for handling all device events
     */
    private class UnifiedConnectionListener implements UnifiedBleManager.UnifiedConnectionListener {
        @Override
        public void onDeviceConnected(UnifiedDevice device) {
            Log.i(TAG, "Device connected: " + device.getName() + " (" + device.getDeviceType() + ")");
        }
        
        @Override
        public void onDeviceDisconnected(UnifiedDevice device, int reason) {
            Log.i(TAG, "Device disconnected: " + device.getName() + " Reason: " + reason);
        }
        
        @Override
        public void onConnectionError(UnifiedDevice device, int errorCode, String message) {
            Log.e(TAG, "Connection error for " + device.getName() + ": " + errorCode + " - " + message);
        }
        
        @Override
        public void onDataReceived(UnifiedDevice device, byte[] data) {
            switch (device.getDeviceType()) {
                case SHIMMER_GSR:
                    handleGSRData(device, data);
                    break;
                case TOPDON_THERMAL:
                    handleThermalData(device, data);
                    break;
                default:
                    Log.d(TAG, "Data received from " + device.getDeviceType() + ": " + data.length + " bytes");
                    break;
            }
        }
        
        @Override
        public void onDeviceReady(UnifiedDevice device) {
            Log.i(TAG, "Device ready: " + device.getName() + " (" + device.getDeviceType() + ")");
            checkDeviceReadiness();
        }
    }
    
    /**
     * Handle GSR data from Shimmer device
     */
    private void handleGSRData(UnifiedDevice device, byte[] data) {
        // Process GSR data with 12-bit ADC resolution (0-4095 range)
        // Convert to microsiemens and log for research analysis
        Log.d(TAG, "GSR data received: " + data.length + " bytes from " + device.getAddress());
        
        // TODO: Implement GSR data processing with proper 12-bit ADC conversion
        // This is where real GSR values would be calculated and stored
    }
    
    /**
     * Handle thermal data from Topdon device
     */
    private void handleThermalData(UnifiedDevice device, byte[] data) {
        // Process thermal frame data and temperature matrices
        Log.d(TAG, "Thermal data received: " + data.length + " bytes from " + device.getAddress());
        
        // TODO: Implement thermal data processing with temperature calibration
        // This is where thermal frames would be parsed and temperature values extracted
    }
}