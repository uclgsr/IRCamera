package com.topdon.ble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;


import androidx.annotation.CallSuper;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.topdon.ble.callback.ScanListener;
import com.topdon.ble.util.Logger;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * date: 2019/10/1 14:44
 * author: bichuanfeng
 */
abstract class AbstractScanner implements Scanner {
    final ScanConfiguration configuration;
    final BluetoothAdapter bluetoothAdapter;
    private final Handler mainHandler;
    private boolean isScanning;
    private final List<ScanListener> scanListeners = new CopyOnWriteArrayList<>();
    private final SparseArray<BluetoothProfile> proxyBluetoothProfiles = new SparseArray<>();
    final Logger logger;
    private final DeviceCreator deviceCreator;

    AbstractScanner(EasyBLE easyBle, BluetoothAdapter bluetoothAdapter) {
        this.bluetoothAdapter = bluetoothAdapter;
        this.configuration = easyBle.scanConfiguration;
        mainHandler = new Handler(Looper.getMainLooper());
        logger = easyBle.getLogger();
        deviceCreator = easyBle.getDeviceCreator();
    }
    
    @Override
    public void addScanListener(ScanListener listener) {
        if (!scanListeners.contains(listener)) {
            scanListeners.add(listener);
        }
    }

    @Override
    public void removeScanListener(ScanListener listener) {
        scanListeners.remove(listener);
    }

    //位置服务是否开户
    private boolean isLocationEnabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return locationManager != null && locationManager.isLocationEnabled();
        } else {
            try {
                int locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
                return locationMode != Settings.Secure.LOCATION_MODE_OFF;
            } catch (Settings.SettingNotFoundException e) {
                return false;
            }
        }
    }

    //检查是否有定位权限
    private boolean noLocationPermission(Context context) {
        int sdkVersion = context.getApplicationInfo().targetSdkVersion;
        if (sdkVersion >= 29) {//target sdk版本在29以上的需要精确定位权限才能搜索到蓝牙设备
            return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;
        }
    }

    //检查是否有蓝牙权限
    private boolean noBluetoothPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31+ 需要新的蓝牙权限
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                   ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED;
        } else {
            // API 30及以下使用旧的蓝牙权限
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                   ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED;
        }
    }

    //检查是否有蓝牙连接权限 - 用于访问设备属性
    private boolean hasBluetoothConnectPermission(Context context) {
        if (context == null) return false;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31+ 需要 BLUETOOTH_CONNECT 权限来访问设备属性
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        } else {
            // API 30及以下使用旧的蓝牙权限
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED;
        }
    }

    //处理搜索回调
    void handleScanCallback(final boolean start, final Device device, final boolean isConnectedBySys,
                            final int errorCode, final String errorMsg) {
        mainHandler.post(() -> {
            for (ScanListener listener : scanListeners) {
                if (device != null) {
                    listener.onScanResult(device, isConnectedBySys);
                } else if (start) {
                    listener.onScanStart();
                } else if (errorCode >= 0) {
                    listener.onScanError(errorCode, errorMsg);
                } else {
                    listener.onScanStop();
                }
            }
        });
    }

    //如果系统已配对连接，那么是无法搜索到的，所以尝试获取已连接的设备
    @SuppressWarnings("all")
    private void getSystemConnectedDevices(Context context) {
        try {
            Method method = bluetoothAdapter.getClass().getDeclaredMethod("getConnectionState");
            method.setAccessible(true);
            int state = (int) method.invoke(bluetoothAdapter);
            if (state == BluetoothAdapter.STATE_CONNECTED) {
                Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
                for (BluetoothDevice device : devices) {
                    Method isConnectedMethod = device.getClass().getDeclaredMethod("isConnected");
                    isConnectedMethod.setAccessible(true);
                    boolean isConnected = (boolean) isConnectedMethod.invoke(device);
                    if (isConnected) {
                        parseScanResult(device, true);
                    }
                }
            }
        } catch (Exception ignore) {
        }
        //遍历支持的，获取所有连接的
        for (int i = 1; i <= 21; i++) {
            try {
                getSystemConnectedDevices(context, i);
            } catch (Exception ignore) {
            }
        }
    }

    private void getSystemConnectedDevices(Context context, int profile) {
        bluetoothAdapter.getProfileProxy(context, new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                if (proxy == null) return;
                proxyBluetoothProfiles.put(profile, proxy);
                synchronized (AbstractScanner.this) {
                    if (!isScanning) return;
                }
                try {
                    List<BluetoothDevice> devices = proxy.getConnectedDevices();
                    for (BluetoothDevice device : devices) {
                        parseScanResult(device, true);
                    }
                } catch (Exception ignore) {
                }
            }

            @Override
            public void onServiceDisconnected(int profile) {

            }
        }, profile);
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    void parseScanResult(BluetoothDevice device, ScanResult result) {
        if (result == null) {
            parseScanResult(device, false);
        } else {
            ScanRecord record = result.getScanRecord();
            parseScanResult(device, false, result, result.getRssi(), record == null ? null : record.getBytes());            
        }
    }

    private void parseScanResult(BluetoothDevice device, boolean isConnectedBySys) {
        parseScanResult(device, isConnectedBySys, null, -120, null);
    }
    
    void parseScanResult(BluetoothDevice device, boolean isConnectedBySys, ScanResult result, int rssi, byte[] scanRecord) {
        // Check Bluetooth permissions before accessing device properties
        Context context = EasyBLE.getInstance().getContext();
        if (context != null && noBluetoothPermission(context)) {
            logger.log(Log.WARN, Logger.TYPE_SCAN_STATE, "Missing Bluetooth permissions, skipping device access");
            return;
        }
        
        // Safe device property access with permission checks
        int deviceType = BluetoothDevice.DEVICE_TYPE_UNKNOWN;
        String deviceAddress = "";
        String deviceName = "";
        
        try {
            if (hasBluetoothConnectPermission(context)) {
                deviceType = device.getType();
                deviceAddress = device.getAddress();
                deviceName = device.getName();
            }
        } catch (SecurityException e) {
            logger.log(Log.WARN, Logger.TYPE_SCAN_STATE, "SecurityException accessing device properties: " + e.getMessage());
            return;
        }
        
        if ((configuration.onlyAcceptBleDevice && deviceType != BluetoothDevice.DEVICE_TYPE_LE) ||
                !deviceAddress.matches("^[0-9A-F]{2}(:[0-9A-F]{2}){5}$")) {
            return;
        }
        String name = deviceName == null ? "" : deviceName;
        if (configuration.rssiLowLimit <= rssi) {
            //通过构建器实例化Device
            Device dev = deviceCreator.create(device, result);
            if (dev != null) {
                dev.name = TextUtils.isEmpty(dev.getName()) ? name : dev.getName();
                dev.rssi = rssi;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    dev.scanResult = result;
                }
                dev.scanRecord = scanRecord;
                handleScanCallback(false, dev, isConnectedBySys, -1, "");
            }
        }
        String msg = String.format(Locale.US, "found device! [name: %s, addr: %s]", TextUtils.isEmpty(name) ? "N/A" : name, device.getAddress());
        logger.log(Log.DEBUG, Logger.TYPE_SCAN_STATE, msg);
    }

    @CallSuper
    @Override
    public void startScan(Context context) {
        synchronized (this) {
            if (!isBtEnabled() || (getType() != ScannerType.CLASSIC && isScanning) || !isReady()) {
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!isLocationEnabled(context)) {
                    String errorMsg = "Unable to scan for Bluetooth devices, the phone's location service is not turned on.";
                    handleScanCallback(false, null, false, ScanListener.ERROR_LOCATION_SERVICE_CLOSED, errorMsg);
                    logger.log(Log.ERROR, Logger.TYPE_SCAN_STATE, errorMsg);
                    return;
                } else if (noLocationPermission(context)) {
                    String errorMsg = "Unable to scan for Bluetooth devices, lack location permission.";
                    handleScanCallback(false, null, false, ScanListener.ERROR_LACK_LOCATION_PERMISSION, errorMsg);
                    logger.log(Log.ERROR, Logger.TYPE_SCAN_STATE, errorMsg);
                    return;
                } else if (noBluetoothPermission(context)) {
                    String errorMsg = "Unable to scan for Bluetooth devices, lack Bluetooth permission.";
                    handleScanCallback(false, null, false, ScanListener.ERROR_LACK_BLUETOOTH_PERMISSION, errorMsg);
                    logger.log(Log.ERROR, Logger.TYPE_SCAN_STATE, errorMsg);
                    return;
                }
            }
            if (getType() != ScannerType.CLASSIC) {
                isScanning = true;
            }
        }
        if (getType() != ScannerType.CLASSIC) {
            handleScanCallback(true, null, false, -1, "");
        }
        if (configuration.acceptSysConnectedDevice) {
            getSystemConnectedDevices(context);
        }
        performStartScan();
        if (getType() != ScannerType.CLASSIC) {
            mainHandler.postDelayed(stopScanRunnable, configuration.scanPeriodMillis);
        }
    }

    @Override
    public boolean isScanning() {
        return isScanning;
    }

    @CallSuper
    void setScanning(boolean scanning) {
        synchronized (this) {
            isScanning = scanning;
        }
    }
    
    @CallSuper
    @Override
    public void stopScan(boolean quietly) {
        mainHandler.removeCallbacks(stopScanRunnable);
        int size = proxyBluetoothProfiles.size();
        for (int i = 0; i < size; i++) {
            try {
                bluetoothAdapter.closeProfileProxy(proxyBluetoothProfiles.keyAt(i), proxyBluetoothProfiles.valueAt(i));
            } catch (Exception ignore) {
            }
        }
        proxyBluetoothProfiles.clear();
        if (isBtEnabled()) {
            performStopScan();
        }
        if (getType() != ScannerType.CLASSIC) {
            synchronized (this) {
                if (isScanning) {
                    isScanning = false;
                    if (!quietly) {
                        handleScanCallback(false, null, false, -1, "");
                    }
                }
            }
        }
    }

    private final Runnable stopScanRunnable = () -> stopScan(false);

    //蓝牙是否开启
    private boolean isBtEnabled() {
        if (bluetoothAdapter.isEnabled()) {
            try {
                Method method = bluetoothAdapter.getClass().getDeclaredMethod("isLeEnabled");
                method.setAccessible(true);
                return (boolean) method.invoke(bluetoothAdapter);
            } catch (Exception e) {
                int state = bluetoothAdapter.getState();
                return state == BluetoothAdapter.STATE_ON || state == 15;
            }
        }
        return false;
    }
    
    @Override
    public void onBluetoothOff() {
        synchronized (this) {
            isScanning = false;
        }
        handleScanCallback(false, null, false, -1, "");
    }

    @Override
    public void release() {
        stopScan(false);
        scanListeners.clear();
    }

    /**
     * 是否可搜索
     */
    protected abstract boolean isReady();
    
    /**
     * 执行搜索
     */
    protected abstract void performStartScan();

    /**
     * 执行停止搜索
     */
    protected abstract void performStopScan();
}
