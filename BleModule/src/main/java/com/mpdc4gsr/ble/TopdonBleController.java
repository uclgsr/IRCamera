package com.mpdc4gsr.ble;

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

import com.mpdc4gsr.ble.util.BluetoothPermissionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class TopdonBleController {
    private static final String TAG = "TopdonBleController";

    private static final UUID TOPDON_SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID TOPDON_DATA_CHAR_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID TOPDON_CMD_CHAR_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");

    private static final UUID TOPDON_THERMAL_SERVICE_UUID = UUID.fromString("12345678-1234-5678-9012-123456789ABC");
    private static final UUID TOPDON_THERMAL_DATA_CHAR_UUID = UUID.fromString("12345678-1234-5678-9012-123456789ABD");

    private static final String[] TOPDON_DEVICE_PATTERNS = {
            "Topdon",
            "TC001",
            "TC-001",
            "TOPDON",
            "TopdonThermal",
            "TopdonEnv",
            "TopdonSensor"
    };

    private static final byte TOPDON_START_THERMAL = 0x01;
    private static final byte TOPDON_STOP_THERMAL = 0x02;
    private static final byte TOPDON_GET_TEMP_RANGE = 0x03;
    private static final byte TOPDON_SET_TEMP_RANGE = 0x04;
    private static final byte TOPDON_CALIBRATE = 0x05;

    private final Context context;
    private final UnifiedBleManager unifiedManager;
    private final BluetoothAdapter bluetoothAdapter;
    private final BluetoothLeScanner leScanner;
    private final Handler mainHandler;

    private final AtomicBoolean isScanning = new AtomicBoolean(false);
    private final List<TopdonDevice> connectedTopdonDevices = new ArrayList<>();
    private TopdonScanListener currentScanListener;
    private final ScanCallback topdonScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            try {
                BluetoothDevice device = result.getDevice();
                String deviceName = BluetoothPermissionUtils.getDeviceName(context, device);

                if (deviceName != null && isTopdonDevice(deviceName)) {
                    UnifiedBleManager.DeviceType deviceType = determineTopdonDeviceType(deviceName, result.getScanRecord());

                    if (currentScanListener != null) {
                        currentScanListener.onTopdonDeviceFound(
                                device,
                                deviceType,
                                result.getRssi(),
                                result.getScanRecord() != null ? result.getScanRecord().getBytes() : new byte[0]
                        );
                    }

                    Log.d(TAG, "Found Topdon device: " + deviceName + " (" + deviceType + ") RSSI: " + result.getRssi());
                }

            } catch (Exception e) {
                Log.e(TAG, "Error processing Topdon scan result", e);
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
                currentScanListener.onScanError(errorCode, "Topdon BLE scan failed with error: " + errorCode);
            }
            Log.e(TAG, "Topdon BLE scan failed with error: " + errorCode);
        }
    };

    public TopdonBleController(@NonNull Context context, @NonNull UnifiedBleManager unifiedManager) {
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

            Log.i(TAG, "Topdon BLE Controller initialized with Nordic backend");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Topdon BLE Controller", e);
            return false;
        }
    }

    public boolean startDeviceDiscovery(@NonNull TopdonScanListener listener) {
        if (isScanning.get()) {
            Log.w(TAG, "Topdon scan already in progress");
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
                leScanner.startScan(null, scanSettings, topdonScanCallback);
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

            Log.i(TAG, "Started Topdon device discovery with Nordic BLE backend");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Failed to start Topdon device discovery", e);
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
                leScanner.stopScan(topdonScanCallback);
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException during scan stop: " + e.getMessage());
            }
            isScanning.set(false);
            currentScanListener = null;

            Log.i(TAG, "Stopped Topdon device discovery");

        } catch (Exception e) {
            Log.e(TAG, "Error stopping Topdon device discovery", e);
        }
    }

    public UnifiedDevice connectDevice(@NonNull BluetoothDevice device,
                                       @NonNull TopdonDeviceConfig config,
                                       @NonNull UnifiedBleManager.UnifiedConnectionListener listener) {
        try {
            Log.i(TAG, "Connecting to Topdon device: " + device.getAddress());

            TopdonDevice topdonDevice = new TopdonDevice(device, config, listener);
            topdonDevice.connect();

            connectedTopdonDevices.add(topdonDevice);

            Log.i(TAG, "Created Topdon connection for device: " + device.getAddress());
            return topdonDevice;

        } catch (Exception e) {
            Log.e(TAG, "Failed to connect to Topdon device", e);
            return null;
        }
    }

    public List<TopdonDevice> getConnectedDevices() {
        return new ArrayList<>(connectedTopdonDevices);
    }

    public void cleanup() {
        try {
            stopDeviceDiscovery();

            for (TopdonDevice device : connectedTopdonDevices) {
                device.disconnect();
            }
            connectedTopdonDevices.clear();

            Log.i(TAG, "Topdon BLE Controller cleaned up");

        } catch (Exception e) {
            Log.e(TAG, "Error during Topdon controller cleanup", e);
        }
    }

    private boolean isTopdonDevice(String deviceName) {
        if (deviceName == null) return false;

        for (String pattern : TOPDON_DEVICE_PATTERNS) {
            if (deviceName.toLowerCase().contains(pattern.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private UnifiedBleManager.DeviceType determineTopdonDeviceType(String deviceName, @Nullable android.bluetooth.le.ScanRecord scanRecord) {
        String name = deviceName.toLowerCase();

        if (name.contains("tc001") || name.contains("tc-001") || name.contains("thermal")) {
            return UnifiedBleManager.DeviceType.TOPDON_THERMAL;
        } else if (name.contains("env") || name.contains("sensor")) {
            return UnifiedBleManager.DeviceType.TOPDON_ENV;
        } else if (name.contains("multi")) {
            return UnifiedBleManager.DeviceType.TOPDON_MULTI;
        }

        return UnifiedBleManager.DeviceType.TOPDON_THERMAL;
    }

    public interface TopdonScanListener {
        void onTopdonDeviceFound(BluetoothDevice device, UnifiedBleManager.DeviceType type, int rssi, byte[] scanRecord);

        void onScanError(int errorCode, String message);

        void onScanComplete();
    }
}
