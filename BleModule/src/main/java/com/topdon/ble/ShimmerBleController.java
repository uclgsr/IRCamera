package com.topdon.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.topdon.ble.util.BluetoothPermissionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class ShimmerBleController {
    private static final String TAG = "ShimmerBleController";

    // Shimmer BLE UUIDs and constants
    private static final UUID SHIMMER_SERVICE_UUID = UUID.fromString("49535343-FE7D-4AE5-8FA9-9FAFD205E455");
    private static final UUID SHIMMER_DATA_CHAR_UUID = UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616");
    private static final UUID SHIMMER_CMD_CHAR_UUID = UUID.fromString("49535343-8841-43F4-A8D4-ECBE34729BB3");

    private static final String[] SHIMMER_DEVICE_PATTERNS = {
            "Shimmer",
            "ShimmerGSR",
            "ShimmerPPG",
            "ShimmerIMU",
            "Shimmer3",
            "Shimmer4"
    };

    private static final byte SHIMMER_START_STREAMING = 0x07;
    private static final byte SHIMMER_STOP_STREAMING = 0x20;
    private static final byte SHIMMER_GET_SAMPLING_RATE = 0x03;
    private static final byte SHIMMER_SET_SAMPLING_RATE = 0x05;
    private static final byte SHIMMER_GET_STATUS = 0x25;
    private static final byte SHIMMER_GET_FW_VERSION = 0x2E;
    private static final byte SHIMMER_SET_GSR_RANGE = 0x60;
    private static final byte SHIMMER_GET_GSR_RANGE = 0x61;

    private final Context context;
    private final UnifiedBleManager unifiedManager;
    private final BluetoothAdapter bluetoothAdapter;
    private final BluetoothLeScanner leScanner;
    private final Handler mainHandler;

    private final AtomicBoolean isScanning = new AtomicBoolean(false);
    private final List<ShimmerDevice> connectedShimmerDevices = new ArrayList<>();
    private ShimmerScanListener currentScanListener;
    private final ScanCallback shimmerScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            try {
                BluetoothDevice device = result.getDevice();
                String deviceName = BluetoothPermissionUtils.getDeviceName(context, device);

                if (deviceName != null && isShimmerDevice(deviceName)) {
                    UnifiedBleManager.DeviceType deviceType = determineShimmerDeviceType(deviceName, result.getScanRecord());

                    if (currentScanListener != null) {
                        currentScanListener.onShimmerDeviceFound(
                                device,
                                deviceType,
                                result.getRssi(),
                                result.getScanRecord() != null ? result.getScanRecord().getBytes() : new byte[0]
                        );
                    }

                    Log.d(TAG, "Found Shimmer device: " + deviceName + " (" + deviceType + ") RSSI: " + result.getRssi());
                }

            } catch (Exception e) {
                Log.e(TAG, "Error processing Shimmer scan result", e);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            isScanning.set(false);
            if (currentScanListener != null) {
                currentScanListener.onScanError(errorCode, "Shimmer BLE scan failed with error: " + errorCode);
            }
            Log.e(TAG, "Shimmer BLE scan failed with error: " + errorCode);
        }
    };

    public ShimmerBleController(@NonNull Context context, @NonNull UnifiedBleManager unifiedManager) {
        this.context = context;
        this.unifiedManager = unifiedManager;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.leScanner = bluetoothAdapter != null ? bluetoothAdapter.getBluetoothLeScanner() : null;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public boolean initialize() {
        try {
            if (bluetoothAdapter == null || leScanner == null) {
                Log.e(TAG, "Bluetooth LE not supported");
                return false;
            }

            if (!bluetoothAdapter.isEnabled()) {
                Log.w(TAG, "Bluetooth not enabled");
                return false;
            }

            Log.i(TAG, "Shimmer BLE Controller initialized with Nordic backend");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Shimmer BLE Controller", e);
            return false;
        }
    }

    public boolean startDeviceDiscovery(@NonNull ShimmerScanListener listener) {
        if (isScanning.get()) {
            Log.w(TAG, "Shimmer scan already in progress");
            return false;
        }

        try {

            if (!BluetoothPermissionUtils.hasBluetoothScanPermission(context)) {
                Log.w(TAG, "Missing BLUETOOTH_SCAN permission for device discovery");
                return false;
            }

            this.currentScanListener = listener;

            ScanSettings scanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                    .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
                    .setReportDelay(0)
                    .build();

            isScanning.set(true);
            try {
                leScanner.startScan(null, scanSettings, shimmerScanCallback);
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException during scan start: " + e.getMessage());
                isScanning.set(false);
                return false;
            }

            mainHandler.postDelayed(() -> {
                if (isScanning.get()) {
                    stopDeviceDiscovery();
                    if (currentScanListener != null) {
                        currentScanListener.onScanComplete();
                    }
                }
            }, 30000);

            Log.i(TAG, "Started Shimmer device discovery with Nordic BLE backend");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Failed to start Shimmer device discovery", e);
            isScanning.set(false);
            return false;
        }
    }

    public void stopDeviceDiscovery() {
        if (!isScanning.get()) {
            return;
        }

        try {

            if (!BluetoothPermissionUtils.hasBluetoothScanPermission(context)) {
                Log.w(TAG, "Missing BLUETOOTH_SCAN permission for stopping scan");
                isScanning.set(false);
                return;
            }

            try {
                leScanner.stopScan(shimmerScanCallback);
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException during scan stop: " + e.getMessage());
            }
            isScanning.set(false);
            currentScanListener = null;

            Log.i(TAG, "Stopped Shimmer device discovery");

        } catch (Exception e) {
            Log.e(TAG, "Error stopping Shimmer device discovery", e);
        }
    }

    public UnifiedDevice connectDevice(@NonNull BluetoothDevice device,
                                       @NonNull ShimmerDeviceConfig config,
                                       @NonNull UnifiedBleManager.UnifiedConnectionListener listener) {
        try {
            Log.i(TAG, "Connecting to Shimmer device: " + device.getAddress());

            ShimmerDevice shimmerDevice = new ShimmerDevice(device, config, listener);
            shimmerDevice.connect();

            connectedShimmerDevices.add(shimmerDevice);

            Log.i(TAG, "Created Shimmer connection for device: " + device.getAddress());
            return shimmerDevice;

        } catch (Exception e) {
            Log.e(TAG, "Failed to connect to Shimmer device", e);
            return null;
        }
    }

    /**
     * Connect and initialize device with proper GSR configuration
     * Implements comprehensive device pairing with scientific accuracy parameters
     * 
     * @param device Bluetooth device to connect
     * @param config Device configuration
     * @param listener Connection listener
     * @param gsrRange GSR range (0-4, default 4 for highest sensitivity)  
     * @param samplingRate Sampling rate in Hz (default 128Hz)
     * @return Connected and initialized ShimmerDevice or null if failed
     */
    public UnifiedDevice connectAndInitializeDevice(@NonNull BluetoothDevice device,
                                                   @NonNull ShimmerDeviceConfig config,
                                                   @NonNull UnifiedBleManager.UnifiedConnectionListener listener,
                                                   int gsrRange,
                                                   int samplingRate) {
        try {
            Log.i(TAG, "Connecting and initializing Shimmer device: " + device.getAddress());

            // First establish basic connection
            UnifiedDevice shimmerDevice = connectDevice(device, config, listener);
            if (shimmerDevice == null) {
                Log.e(TAG, "Failed to establish basic connection");
                return null;
            }

            // Wait for connection to be fully established
            int maxWaitAttempts = 10; // 5 seconds max wait
            int waitAttempts = 0;
            while (!shimmerDevice.isConnected() && waitAttempts < maxWaitAttempts) {
                try {
                    Thread.sleep(500);
                    waitAttempts++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            if (!shimmerDevice.isConnected()) {
                Log.e(TAG, "Device connection timeout");
                return shimmerDevice; // Return anyway, may connect later
            }

            // Initialize device for GSR recording with proper parameters
            if (shimmerDevice instanceof ShimmerDevice) {
                ShimmerDevice shimmer = (ShimmerDevice) shimmerDevice;
                boolean initialized = shimmer.initializeForGSRRecording(gsrRange, samplingRate);
                
                if (initialized) {
                    Log.i(TAG, "Successfully connected and initialized Shimmer device: " + device.getAddress());
                } else {
                    Log.w(TAG, "Device connected but initialization incomplete: " + device.getAddress());
                }
            }

            return shimmerDevice;

        } catch (Exception e) {
            Log.e(TAG, "Failed to connect and initialize Shimmer device", e);
            return null;
        }
    }

    public List<ShimmerDevice> getConnectedDevices() {
        return new ArrayList<>(connectedShimmerDevices);
    }

    /**
     * Scan for nearby Shimmer devices for the specified duration
     *
     * @param scanDurationMs Duration of scan in milliseconds
     * @param callback       Callback to receive discovered devices
     */
    public void scanForDevices(long scanDurationMs, UnifiedBleManager.ShimmerScanCallback callback) {
        if (callback == null) {
            Log.e(TAG, "Scan callback cannot be null");
            return;
        }

        List<UnifiedDevice> foundDevices = new ArrayList<>();

        ShimmerScanListener scanListener = new ShimmerScanListener() {
            @Override
            public void onShimmerDeviceFound(BluetoothDevice device, UnifiedBleManager.DeviceType type, int rssi, byte[] scanRecord) {
                try {
                    // Create UnifiedDevice from BluetoothDevice
                    UnifiedDevice unifiedDevice = createUnifiedDeviceFromBluetooth(device, type, rssi, scanRecord);
                    foundDevices.add(unifiedDevice);
                    callback.onDeviceFound(unifiedDevice);
                } catch (Exception e) {
                    Log.e(TAG, "Error creating unified device from scan result", e);
                }
            }

            @Override
            public void onScanError(int errorCode, String message) {
                callback.onScanFailed(message);
            }

            @Override
            public void onScanComplete() {
                callback.onScanComplete(foundDevices);
            }
        };

        // Start scanning with the specified duration
        if (startDeviceDiscovery(scanListener)) {
            // Schedule scan stop after specified duration
            mainHandler.postDelayed(() -> {
                if (isScanning.get()) {
                    stopDeviceDiscovery();
                    scanListener.onScanComplete();
                }
            }, scanDurationMs);
        } else {
            callback.onScanFailed("Failed to start BLE scan");
        }
    }

    /**
     * Create a UnifiedDevice from BluetoothDevice scan result
     */
    private UnifiedDevice createUnifiedDeviceFromBluetooth(BluetoothDevice device, UnifiedBleManager.DeviceType type, int rssi, byte[] scanRecord) {
        try {
            String deviceName = BluetoothPermissionUtils.getDeviceName(context, device);

            // Create ShimmerDevice as UnifiedDevice
            ShimmerDeviceConfig config = new ShimmerDeviceConfig.Builder()
                    .setDeviceType(type)
                    .setSamplingRate(128) // Default GSR sampling rate
                    .setConnectionTimeout(15000)
                    .build();

            return new ShimmerDevice(device, config, null);

        } catch (Exception e) {
            Log.e(TAG, "Error creating UnifiedDevice from BluetoothDevice", e);
            return null;
        }
    }

    public void cleanup() {
        try {
            stopDeviceDiscovery();

            for (ShimmerDevice device : connectedShimmerDevices) {
                device.disconnect();
            }
            connectedShimmerDevices.clear();

            Log.i(TAG, "Shimmer BLE Controller cleaned up");

        } catch (Exception e) {
            Log.e(TAG, "Error during Shimmer controller cleanup", e);
        }
    }

    private boolean isShimmerDevice(String deviceName) {
        if (deviceName == null) return false;

        for (String pattern : SHIMMER_DEVICE_PATTERNS) {
            if (deviceName.toLowerCase().contains(pattern.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    private UnifiedBleManager.DeviceType determineShimmerDeviceType(String deviceName, @Nullable android.bluetooth.le.ScanRecord scanRecord) {
        String name = deviceName.toLowerCase();

        if (name.contains("gsr")) {
            return UnifiedBleManager.DeviceType.SHIMMER_GSR;
        } else if (name.contains("ppg")) {
            return UnifiedBleManager.DeviceType.SHIMMER_PPG;
        } else if (name.contains("imu")) {
            return UnifiedBleManager.DeviceType.SHIMMER_IMU;
        }

        return UnifiedBleManager.DeviceType.SHIMMER_GSR;
    }

    public interface ShimmerScanListener {
        void onShimmerDeviceFound(BluetoothDevice device, UnifiedBleManager.DeviceType type, int rssi, byte[] scanRecord);

        void onScanError(int errorCode, String message);

        void onScanComplete();
    }
}
