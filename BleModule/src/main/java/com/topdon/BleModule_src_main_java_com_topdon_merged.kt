// Merged ALL .kt and .java files from the 'BleModule\src\main\java\com\topdon' directory and its subdirectories.
// Total files: 76 | Generated on: 2025-10-08 01:42:33


// ===== FROM: BleModule\src\main\java\com\topdon\ble\AbstractScanner.java =====

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

abstract class AbstractScanner implements Scanner {
    final ScanConfiguration configuration;
    final BluetoothAdapter bluetoothAdapter;
    final Logger logger;
    private final Handler mainHandler;
    private final List<ScanListener> scanListeners = new CopyOnWriteArrayList<>();
    private final SparseArray<BluetoothProfile> proxyBluetoothProfiles = new SparseArray<>();
    private final DeviceCreator deviceCreator;
    private final EasyBLE easyBle;
    private boolean isScanning;

    AbstractScanner(EasyBLE easyBle, BluetoothAdapter bluetoothAdapter) {
        this.easyBle = easyBle;
        this.bluetoothAdapter = bluetoothAdapter;
        this.configuration = easyBle.scanConfiguration;
        mainHandler = new Handler (Looper.getMainLooper());
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

    protected EasyBLE getEasyBle() {
        return easyBle;
    }

    // Check if location service is enabled
    private boolean isLocationEnabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            LocationManager locationManager =(LocationManager) context . getSystemService (Context.LOCATION_SERVICE);
            return locationManager != null && locationManager.isLocationEnabled();
        } else {
            try {
                int locationMode = Settings . Secure . getInt (context.getContentResolver(), Settings.Secure.LOCATION_MODE);
                return locationMode != Settings.Secure.LOCATION_MODE_OFF;
            } catch (Settings.SettingNotFoundException e) {
                return false;
            }
        }
    }

    //
    private boolean noLocationPermission(Context context) {
        int sdkVersion = context . getApplicationInfo ().targetSdkVersion;
        if (sdkVersion >= 29) {//target sdk29
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED;
        }
    }

    //
    void handleScanCallback (final boolean start, final Device device, final boolean isConnectedBySys,
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

    //ï¼Œï¼Œ
    @SuppressWarnings("all")
    private void getSystemConnectedDevices(Context context) {
        try {
            Method method = bluetoothAdapter . getClass ().getDeclaredMethod("getConnectionState");
            method.setAccessible(true);
            int state =(int) method . invoke (bluetoothAdapter);
            if (state == BluetoothAdapter.STATE_CONNECTED) {
                Set<BluetoothDevice> devices = bluetoothAdapter . getBondedDevices ();
                for (BluetoothDevice device : devices) {
                    Method isConnectedMethod = device . getClass ().getDeclaredMethod("isConnected");
                    isConnectedMethod.setAccessible(true);
                    boolean isConnected =(boolean) isConnectedMethod . invoke (device);
                    if (isConnected) {
                        parseScanResult(device, true);
                    }
                }
            }
        } catch (Exception ignore) {
        }
        //ï¼Œ
        for (int i = 1; i <= 21; i++) {
        try {
            getSystemConnectedDevices(context, i);
        } catch (Exception ignore) {
        }
    }
    }

    private void getSystemConnectedDevices(Context context, int profile) {
        bluetoothAdapter.getProfileProxy(context, new BluetoothProfile . ServiceListener () {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                if (proxy == null) return;
                proxyBluetoothProfiles.put(profile, proxy);
                synchronized(AbstractScanner.this) {
                    if (!isScanning) return;
                }
                try {
                    List<BluetoothDevice> devices = proxy . getConnectedDevices ();
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
    void parseScanResult (BluetoothDevice device, ScanResult result) {
        if (result == null) {
            parseScanResult(device, false);
        } else {
            ScanRecord record = result . getScanRecord ();
            parseScanResult(device, false, result, result.getRssi(), record == null ? null : record.getBytes());
        }
    }

    private void parseScanResult(BluetoothDevice device, boolean isConnectedBySys) {
        parseScanResult(device, isConnectedBySys, null, -120, null);
    }

    void parseScanResult (BluetoothDevice device, boolean isConnectedBySys, ScanResult result, int rssi, byte[] scanRecord) {
        Context context = easyBle . getContext ();
        if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return;
            }
        }
        if ((configuration.onlyAcceptBleDevice && device.getType() != BluetoothDevice.DEVICE_TYPE_LE) ||
            !device.getAddress().matches("^[0-9A-F]{2}(:[0-9A-F]{2}){5}$")
        ) {
            return;
        }
        String name = device . getName () == null ? "" : device.getName();
        if (configuration.rssiLowLimit <= rssi) {
            //Device
            Device dev = deviceCreator . create (device, result);
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
        String msg = String . format (Locale.US, "found device! [name: %s, addr: %s]", TextUtils.isEmpty(name) ? "N/A" : name, device.getAddress());
        logger.log(Log.DEBUG, Logger.TYPE_SCAN_STATE, msg);
    }

    @CallSuper
    @Override
    public void startScan(Context context) {
        synchronized(this) {
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
    void setScanning (boolean scanning) {
        synchronized(this) {
            isScanning = scanning;
        }
    }

    @CallSuper
    @Override
    public void stopScan(boolean quietly) {
        mainHandler.removeCallbacks(stopScanRunnable);
        int size = proxyBluetoothProfiles . size ();
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
            synchronized(this) {
                if (isScanning) {
                    isScanning = false;
                    if (!quietly) {
                        handleScanCallback(false, null, false, -1, "");
                    }
                }
            }
        }
    }

    //
    private boolean isBtEnabled() {
        if (bluetoothAdapter.isEnabled()) {
            try {
                Method method = bluetoothAdapter . getClass ().getDeclaredMethod("isLeEnabled");
                method.setAccessible(true);
                return (boolean) method . invoke (bluetoothAdapter);
            } catch (Exception e) {
                int state = bluetoothAdapter . getState ();
                return state == BluetoothAdapter.STATE_ON || state == 15;
            }
        }
        return false;
    }

    @Override
    public void onBluetoothOff() {
        synchronized(this) {
            isScanning = false;
        }
        handleScanCallback(false, null, false, -1, "");
    }

    @Override
    public void release() {
        stopScan(false);
        scanListeners.clear();
    }

    protected abstract boolean isReady ();

    protected abstract void performStartScan ();

    protected abstract void performStopScan ();

    private final Runnable stopScanRunnable =() -> stopScan(false);

}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\BondController.java =====

package com.topdon.ble;

public interface BondController {

    boolean accept(Device device);
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\callback\MtuChangeCallback.java =====

package com.topdon.ble.callback;

import com.topdon.ble.Request;

public interface MtuChangeCallback extends RequestFailedCallback {

    void onMtuChanged (Request request, int mtu);
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\callback\NotificationChangeCallback.java =====

package com.topdon.ble.callback;

import com.topdon.ble.Request;

public interface NotificationChangeCallback extends RequestFailedCallback {

    void onNotificationChanged (Request request, boolean isEnabled);
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\callback\PhyChangeCallback.java =====

package com.topdon.ble.callback;

import com.topdon.ble.Request;

public interface PhyChangeCallback extends RequestFailedCallback {

    void onPhyChange (Request request, int txPhy, int rxPhy);
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\callback\ReadCharacteristicCallback.java =====

package com.topdon.ble.callback;

import com.topdon.ble.Request;

public interface ReadCharacteristicCallback extends RequestFailedCallback {

    void onCharacteristicRead (Request request, byte[] value);
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\callback\ReadDescriptorCallback.java =====

package com.topdon.ble.callback;

import com.topdon.ble.Request;

public interface ReadDescriptorCallback extends RequestFailedCallback {

    void onDescriptorRead (Request request, byte[] value);
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\callback\ReadRssiCallback.java =====

package com.topdon.ble.callback;

import com.topdon.ble.Request;

public interface ReadRssiCallback extends RequestFailedCallback {

    void onRssiRead (Request request, int rssi);
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\callback\RequestCallback.java =====

package com.topdon.ble.callback;

public interface RequestCallback {
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\callback\RequestFailedCallback.java =====

package com.topdon.ble.callback;

import com.topdon.ble.Request;

public interface RequestFailedCallback extends RequestCallback {

    void onRequestFailed (Request request, int failType, Object value);
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\callback\ScanListener.java =====

package com.topdon.ble.callback;

import com.topdon.ble.Device;

public interface ScanListener {

    int ERROR_LACK_LOCATION_PERMISSION = 0;

    int ERROR_LOCATION_SERVICE_CLOSED = 1;

    int ERROR_SCAN_FAILED = 2;

    void onScanStart();

    void onScanStop();

    @Deprecated
    default void onScanResult(Device device)
    {
    }

    void onScanResult(Device device, boolean isConnectedBySys);

    void onScanError(int errorCode, String errorMsg);
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\callback\WriteCharacteristicCallback.java =====

package com.topdon.ble.callback;

import com.topdon.ble.Request;

public interface WriteCharacteristicCallback extends RequestFailedCallback {

    void onCharacteristicWrite (Request request, byte[] value);
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\ClassicScanner.java =====

package com.topdon.ble;

import android.bluetooth.BluetoothAdapter;

import androidx.annotation.NonNull;

import com.topdon.ble.util.Logger;

class ClassicScanner extends AbstractScanner {
    private boolean stopQuietly = false;

    ClassicScanner(EasyBLE easyBle, BluetoothAdapter bluetoothAdapter) {
        super(easyBle, bluetoothAdapter);
    }

    @Override
    protected boolean isReady() {
        return true;
    }

    @Override
    protected void performStartScan() {
        try {
            bluetoothAdapter.startDiscovery();
        } catch (SecurityException e) {
            logger.log(
                android.util.Log.ERROR,
                Logger.TYPE_SCAN_STATE,
                "Missing Bluetooth permission for classic scan: " + e.getMessage()
            );
        }
    }

    @Override
    protected void performStopScan() {
        try {
            bluetoothAdapter.cancelDiscovery();
        } catch (SecurityException e) {
            logger.log(
                android.util.Log.ERROR,
                Logger.TYPE_SCAN_STATE,
                "Missing Bluetooth permission to stop classic scan: " + e.getMessage()
            );
        }
    }

    @Override
    void setScanning (boolean scanning) {
        super.setScanning(scanning);
        if (scanning) {
            handleScanCallback(true, null, false, -1, "");
        } else if (!stopQuietly) {
            handleScanCallback(false, null, false, -1, "");
        } else {
            stopQuietly = false;
        }
    }

    @Override
    public void stopScan(boolean quietly) {
        if (isScanning()) {
            stopQuietly = quietly;
        }
        super.stopScan(quietly);
    }

    @NonNull
    @Override
    public ScannerType getType() {
        return ScannerType.CLASSIC;
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\Connection.java =====

package com.topdon.ble;

import android.bluetooth.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.UUID;

public interface Connection {
    UUID clientCharacteristicConfig = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    int REQUEST_FAIL_TYPE_REQUEST_FAILED = 0;
    int REQUEST_FAIL_TYPE_CHARACTERISTIC_NOT_EXIST = 1;
    int REQUEST_FAIL_TYPE_DESCRIPTOR_NOT_EXIST = 2;
    int REQUEST_FAIL_TYPE_SERVICE_NOT_EXIST = 3;

    int REQUEST_FAIL_TYPE_GATT_STATUS_FAILED = 4;
    int REQUEST_FAIL_TYPE_GATT_IS_NULL = 5;
    int REQUEST_FAIL_TYPE_BLUETOOTH_ADAPTER_DISABLED = 6;
    int REQUEST_FAIL_TYPE_REQUEST_TIMEOUT = 7;
    int REQUEST_FAIL_TYPE_CONNECTION_DISCONNECTED = 8;
    int REQUEST_FAIL_TYPE_CONNECTION_RELEASED = 9;

    //-------------------
    int TIMEOUT_TYPE_CANNOT_DISCOVER_DEVICE = 0;

    int TIMEOUT_TYPE_CANNOT_CONNECT = 1;

    int TIMEOUT_TYPE_CANNOT_DISCOVER_SERVICES = 2;

    //--------------------------------

    int CONNECT_FAIL_TYPE_MAXIMUM_RECONNECTION = 1;

    int CONNECT_FAIL_TYPE_CONNECTION_IS_UNSUPPORTED = 2;

    @NonNull
    Device getDevice();

    int getMtu();

    void reconnect();

    void disconnect();

    void refresh();

    void release();

    void releaseNoEvent();

    @NonNull
    ConnectionState getConnectionState();

    boolean isAutoReconnectEnabled();

    @Nullable
    BluetoothGatt getGatt();

    void clearRequestQueue();

    void clearRequestQueueByType(RequestType type);

    @NonNull
    ConnectionConfiguration getConnectionConfiguration();

    @Nullable
    BluetoothGattService getService(UUID service);

    @Nullable
    BluetoothGattCharacteristic getCharacteristic(UUID service, UUID characteristic);

    @Nullable
    BluetoothGattDescriptor getDescriptor(UUID service, UUID characteristic, UUID descriptor);

    void execute(Request request);

    boolean isNotificationOrIndicationEnabled(BluetoothGattCharacteristic characteristic);

    boolean isNotificationOrIndicationEnabled(UUID service, UUID characteristic);

    void setBluetoothGattCallback(BluetoothGattCallback callback);

    boolean hasProperty(UUID service, UUID characteristic, int property);
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\ConnectionConfiguration.java =====

package com.topdon.ble;

import android.bluetooth.BluetoothDevice;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.util.Pair;

import java.util.*;

public class ConnectionConfiguration {

    public static final int TRY_RECONNECT_TIMES_INFINITE = -1;
    @NonNull
    final List<Pair<Integer, Integer>> scanIntervalPairsInAutoReconnection;
    private final Map<String, WriteOptions> defaultWriteOptionsMap = new HashMap<>();
    int discoverServicesDelayMillis = 600;
    int connectTimeoutMillis = 10000;
    int requestTimeoutMillis = 3000;
    int tryReconnectMaxTimes = TRY_RECONNECT_TIMES_INFINITE;
    int reconnectImmediatelyMaxTimes = 3;
    boolean isAutoReconnect = true;
    @RequiresApi(Build.VERSION_CODES.M)
    int transport = BluetoothDevice.TRANSPORT_LE;
    @RequiresApi(Build.VERSION_CODES.O)
    int phy = BluetoothDevice.PHY_LE_1M_MASK;

    public ConnectionConfiguration()
    {
        scanIntervalPairsInAutoReconnection = new ArrayList < > ();
        scanIntervalPairsInAutoReconnection.add(Pair.create(0, 2000));
        scanIntervalPairsInAutoReconnection.add(Pair.create(1, 5000));
        scanIntervalPairsInAutoReconnection.add(Pair.create(3, 10000));
        scanIntervalPairsInAutoReconnection.add(Pair.create(5, 30000));
        scanIntervalPairsInAutoReconnection.add(Pair.create(10, 60000));
    }

    public ConnectionConfiguration setDiscoverServicesDelayMillis(int discoverServicesDelayMillis)
    {
        this.discoverServicesDelayMillis = discoverServicesDelayMillis;
        return this;
    }

    public ConnectionConfiguration setConnectTimeoutMillis(int connectTimeoutMillis)
    {
        if (requestTimeoutMillis >= 1000) {
            this.connectTimeoutMillis = connectTimeoutMillis;
        }
        return this;
    }

    public ConnectionConfiguration setRequestTimeoutMillis(int requestTimeoutMillis)
    {
        if (requestTimeoutMillis >= 1000) {
            this.requestTimeoutMillis = requestTimeoutMillis;
        }
        return this;
    }

    public ConnectionConfiguration setTryReconnectMaxTimes(int tryReconnectMaxTimes)
    {
        this.tryReconnectMaxTimes = tryReconnectMaxTimes;
        return this;
    }

    public ConnectionConfiguration setReconnectImmediatelyMaxTimes(int reconnectImmediatelyMaxTimes)
    {
        this.reconnectImmediatelyMaxTimes = reconnectImmediatelyMaxTimes;
        return this;
    }

    public ConnectionConfiguration setAutoReconnect(boolean autoReconnect)
    {
        isAutoReconnect = autoReconnect;
        return this;
    }

    @RequiresApi(Build.VERSION_CODES.M)
    public ConnectionConfiguration setTransport(int transport)
    {
        this.transport = transport;
        return this;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    public ConnectionConfiguration setPhy(int phy)
    {
        this.phy = phy;
        return this;
    }

    public ConnectionConfiguration setScanIntervalPairsInAutoReconnection(List<Pair<Integer, Integer>> parameters)
    {
        Inspector.requireNonNull(parameters, "parameters can't be null");
        scanIntervalPairsInAutoReconnection.clear();
        scanIntervalPairsInAutoReconnection.addAll(parameters);
        return this;
    }

    public ConnectionConfiguration setDefaultWriteOptions(UUID service, UUID characteristic, WriteOptions options)
    {
        Inspector.requireNonNull(service, "service can't be null");
        Inspector.requireNonNull(characteristic, "characteristic can't be null");
        Inspector.requireNonNull(options, "options can't be null");
        defaultWriteOptionsMap.put(service + ":" + characteristic, options);
        return this;
    }

    @Nullable
    WriteOptions getDefaultWriteOptions(UUID service, UUID characteristic)
    {
        return defaultWriteOptionsMap.get(service + ":" + characteristic);
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\ConnectionImpl.java =====

package com.topdon.ble;

import android.annotation.SuppressLint;
import android.bluetooth.*;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.mpdc4gsr.libunified.app.utils.UnifiedBleUtils;
import com.mpdc4gsr.libunified.app.utils.UnifiedMathUtils;
import com.topdon.ble.callback.RequestCallback;
import com.topdon.ble.callback.ScanListener;
import com.topdon.ble.util.Logger;
import com.topdon.commons.observer.Observable;
import com.topdon.commons.poster.MethodInfo;
import com.topdon.commons.poster.PosterDispatcher;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

// Permission checks are expected to be handled by the calling code before using this connection
@SuppressLint("MissingPermission")
class ConnectionImpl implements Connection, ScanListener {
    private static final int MSG_REQUEST_TIMEOUT = 0;
    private static final int MSG_CONNECT = 1;
    private static final int MSG_DISCONNECT = 2;
    private static final int MSG_REFRESH = 3;
    private static final int MSG_TIMER = 4;
    private static final int MSG_DISCOVER_SERVICES = 6;
    private static final int MSG_ON_CONNECTION_STATE_CHANGE = 7;
    private static final int MSG_ON_SERVICES_DISCOVERED = 8;

    private static final int MSG_ARG_NONE = 0;
    private static final int MSG_ARG_RECONNECT = 1;

    private final BluetoothAdapter bluetoothAdapter;
    private final Device device;
    private final ConnectionConfiguration configuration;//
    private final List<GenericRequest> requestQueue = new ArrayList<>();//
    private final EventObserver observer;//
    private final Handler connHandler;//Handlerï¼Œ
    private final Logger logger;
    private final Observable observable;
    private final PosterDispatcher posterDispatcher;
    private final BluetoothGattCallback gattCallback = new BleGattCallback();
    private final EasyBLE easyBle;
    private BluetoothGatt bluetoothGatt;
    private GenericRequest currentRequest;//
    private boolean isReleased;//
    private long connStartTime; //
    private int refreshCount;//ï¼ˆï¼‰ï¼Œ
    private int tryReconnectCount;//
    private ConnectionState lastConnectionState;//
    private int reconnectImmediatelyCount = 0; //
    private boolean refreshing;//
    private boolean isActiveDisconnect;//
    private long lastScanStopTime;//
    private int mtu = 23;
    private BluetoothGattCallback originCallback;
    private Runnable connectRunnable = new Runnable () {
        @Override
        public void run() {
            if (!isReleased) {
                //
                easyBle.stopScan();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    bluetoothGatt = device.getOriginDevice().connectGatt(
                        easyBle.getContext(), false, gattCallback,
                        configuration.transport, configuration.phy
                    );
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    bluetoothGatt = device.getOriginDevice().connectGatt(
                        easyBle.getContext(), false, gattCallback,
                        configuration.transport
                    );
                } else {
                    bluetoothGatt = device.getOriginDevice().connectGatt(easyBle.getContext(), false, gattCallback);
                }
            }
        }
    };

    ConnectionImpl(
        EasyBLE easyBle, BluetoothAdapter bluetoothAdapter, Device device, ConnectionConfiguration configuration,
        int connectDelay, EventObserver observer
    ) {
        this.easyBle = easyBle;
        this.bluetoothAdapter = bluetoothAdapter;
        this.device = device;
        //
        if (configuration == null) {
            this.configuration = new ConnectionConfiguration ();
        } else {
            this.configuration = configuration;
        }
        this.observer = observer;
        logger = easyBle.getLogger();
        observable = easyBle.getObservable();
        posterDispatcher = easyBle.getPosterDispatcher();
        connHandler = new ConnHandler (this);
        connStartTime = System.currentTimeMillis();
        connHandler.sendEmptyMessageDelayed(MSG_CONNECT, connectDelay); //
        connHandler.sendEmptyMessageDelayed(MSG_TIMER, connectDelay); //
        easyBle.addScanListener(this);
    }

    @Override
    public void onScanStart() {
    }

    @Override
    public void onScanStop() {
        synchronized(this) {
            lastScanStopTime = System.currentTimeMillis();
        }
    }

    @Override
    public void onScanResult(Device device, boolean isConnectedBySys) {
        synchronized(this) {
            if (!isReleased && this.device.equals(device) && this.device.connectionState == ConnectionState.SCANNING_FOR_RECONNECTION) {
                connHandler.sendEmptyMessage(MSG_CONNECT);
            }
        }
    }

    @Override
    public void onScanError(int errorCode, String errorMsg) {

    }

    @Override
    public void setBluetoothGattCallback(BluetoothGattCallback callback) {
        originCallback = callback;
    }

    @Override
    public boolean hasProperty(UUID service, UUID characteristic, int property) {
        BluetoothGattCharacteristic charac = getCharacteristic (service, characteristic);
        if (charac == null) {
            return false;
        }
        return (charac.getProperties() & property) != 0;
    }

    private void doOnConnectionStateChange(int status, int newState) {
        if (bluetoothGatt != null) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    logD(Logger.TYPE_CONNECTION_STATE, "connected! [name: %s, addr: %s]", device.name, device.address);
                    device.connectionState = ConnectionState.CONNECTED;
                    sendConnectionCallback();
                    // 
                    connHandler.sendEmptyMessageDelayed(
                        MSG_DISCOVER_SERVICES,
                        configuration.discoverServicesDelayMillis
                    );
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    logD(
                        Logger.TYPE_CONNECTION_STATE, "disconnected! [name: %s, addr: %s, autoReconnEnable: %s]",
                        device.name, device.address, configuration.isAutoReconnect
                    );
                    clearRequestQueueAndNotify();
                    notifyDisconnected();
                }
            } else {
                logE(
                    Logger.TYPE_CONNECTION_STATE, "GATT error! [status: %d, name: %s, addr: %s]",
                    status, device.name, device.address
                );
                if (status == 133) {
                    doClearTaskAndRefresh();
                } else {
                    clearRequestQueueAndNotify();
                    notifyDisconnected();
                }
            }
        }
    }

    private void doOnServicesDiscovered(int status) {
        if (bluetoothGatt != null) {
            List<BluetoothGattService> services = bluetoothGatt . getServices ();
            if (status == BluetoothGatt.GATT_SUCCESS) {
                logD(
                    Logger.TYPE_CONNECTION_STATE, "services discovered! [name: %s, addr: %s, size: %d]", device.name,
                    device.address, services.size()
                );
                if (services.isEmpty()) {
                    doClearTaskAndRefresh();
                } else {
                    refreshCount = 0;
                    tryReconnectCount = 0;
                    reconnectImmediatelyCount = 0;
                    device.connectionState = ConnectionState.SERVICE_DISCOVERED;
                    sendConnectionCallback();
                }
            } else {
                doClearTaskAndRefresh();
                logE(
                    Logger.TYPE_CONNECTION_STATE, "GATT error! [status: %d, name: %s, addr: %s]",
                    status, device.name, device.address
                );
            }
        }
    }

    private void doDiscoverServices() {
        if (bluetoothGatt != null) {
            bluetoothGatt.discoverServices();
            device.connectionState = ConnectionState.SERVICE_DISCOVERING;
            sendConnectionCallback();
        } else {
            notifyDisconnected();
        }
    }

    private void doTimer() {
        if (!isReleased) {
            //
            if (device.connectionState != ConnectionState.SERVICE_DISCOVERED && !refreshing && !isActiveDisconnect) {
                if (device.connectionState != ConnectionState.DISCONNECTED) {
                    //
                    if (System.currentTimeMillis() - connStartTime > configuration.connectTimeoutMillis) {
                        connStartTime = System.currentTimeMillis();
                        logE(
                            Logger.TYPE_CONNECTION_STATE,
                            "connect timeout! [name: %s, addr: %s]",
                            device.name,
                            device.address
                        );
                        int type;
                        switch(device.connectionState) {
                            case SCANNING_FOR_RECONNECTION :
                            type = TIMEOUT_TYPE_CANNOT_DISCOVER_DEVICE;
                            break;
                            case CONNECTING :
                            type = TIMEOUT_TYPE_CANNOT_CONNECT;
                            break;
                            default:
                            type = TIMEOUT_TYPE_CANNOT_DISCOVER_SERVICES;
                            break;
                        }
                        observable.notifyObservers(MethodInfoGenerator.onConnectTimeout(device, type));
                        if (observer != null) {
                            posterDispatcher.post(observer, MethodInfoGenerator.onConnectTimeout(device, type));
                        }
                        boolean infinite = configuration . tryReconnectMaxTimes == ConnectionConfiguration . TRY_RECONNECT_TIMES_INFINITE;
                        if (configuration.isAutoReconnect && (infinite || tryReconnectCount < configuration.connectTimeoutMillis)) {
                            doDisconnect(true);
                        } else {
                            doDisconnect(false);
                            if (observer != null) {
                                posterDispatcher.post(
                                    observer,
                                    MethodInfoGenerator.onConnectFailed(device, CONNECT_FAIL_TYPE_MAXIMUM_RECONNECTION)
                                );
                            }
                            observable.notifyObservers(
                                MethodInfoGenerator.onConnectFailed(
                                    device,
                                    CONNECT_FAIL_TYPE_MAXIMUM_RECONNECTION
                                )
                            );
                            logE(
                                Logger.TYPE_CONNECTION_STATE,
                                "connect failed! [type: maximun reconnection, name: %s, addr: %s]",
                                device.name,
                                device.address
                            );
                        }
                    }
                } else if (configuration.isAutoReconnect) {
                    doDisconnect(true);
                }
            }
            connHandler.sendEmptyMessageDelayed(MSG_TIMER, 500);
        }
    }

    private void doConnect() {
        cancelRefreshState();
        device.connectionState = ConnectionState.CONNECTING;
        sendConnectionCallback();
        logD(Logger.TYPE_CONNECTION_STATE, "connecting [name: %s, addr: %s]", device.name, device.address);
        connHandler.postDelayed(connectRunnable, 500);
    }

    private void doDisconnect(boolean reconnect) {
        clearRequestQueueAndNotify();
        connHandler.removeCallbacks(connectRunnable);
        connHandler.removeMessages(MSG_DISCOVER_SERVICES);
        if (bluetoothGatt != null) {
            closeGatt(bluetoothGatt);
            bluetoothGatt = null;
        }
        device.connectionState = ConnectionState.DISCONNECTED;
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled() && reconnect && !isReleased) {
            if (reconnectImmediatelyCount < configuration.reconnectImmediatelyMaxTimes) {
                tryReconnectCount++;
                reconnectImmediatelyCount++;
                connStartTime = System.currentTimeMillis();
                doConnect();
                return;
            } else if (canScanReconnect()) {
                tryScanReconnect();
            }
        }
        sendConnectionCallback();
    }

    private void doClearTaskAndRefresh() {
        clearRequestQueueAndNotify();
        doRefresh(true);
    }

    //
    private void doRefresh(boolean isAuto) {
        logD(Logger.TYPE_CONNECTION_STATE, "refresh GATT! [name: %s, addr: %s]", device.name, device.address);
        connStartTime = System.currentTimeMillis();
        if (bluetoothGatt != null) {
            try {
                bluetoothGatt.disconnect();
            } catch (Exception ignore) {
            }

            if (isAuto) {
                if (refreshCount <= 5) {
                    refreshing = doRefresh();
                }
                refreshCount++;
            } else {
                refreshing = doRefresh();
            }
            if (refreshing) {
                connHandler.postDelayed(this::cancelRefreshState, 2000);
            } else if (bluetoothGatt != null) {
                closeGatt(bluetoothGatt);
                bluetoothGatt = null;
            }
        }
        notifyDisconnected();
    }

    private void cancelRefreshState() {
        if (refreshing) {
            refreshing = false;
            if (bluetoothGatt != null) {
                closeGatt(bluetoothGatt);
                bluetoothGatt = null;
            }
        }
    }

    private void tryScanReconnect() {
        if (!isReleased) {
            connStartTime = System.currentTimeMillis();
            easyBle.stopScan();
            //ï¼Œ
            device.connectionState = ConnectionState.SCANNING_FOR_RECONNECTION;
            logD(
                Logger.TYPE_CONNECTION_STATE,
                "scanning for reconnection [name: %s, addr: %s]",
                device.name,
                device.address
            );
            easyBle.startScan();
        }
    }

    private boolean canScanReconnect() {
        long duration = System . currentTimeMillis () - lastScanStopTime;
        List<Pair<Integer, Integer>> parameters = configuration . scanIntervalPairsInAutoReconnection;
        Collections.sort(parameters, (o1, o2) -> {
        if (o1 == null || o1.first == null) return 1;
        if (o2 == null || o2.first == null) return -1;
        return o2.first.compareTo(o1.first);
    });
        for (Pair< Integer, Integer> pair : parameters) {
        if (pair.first != null && pair.second != null && tryReconnectCount >= pair.first && duration >= pair.second) {
            return true;
        }
    }
        return false;
    }

    private void closeGatt(BluetoothGatt gatt) {
        try {
            gatt.disconnect();
        } catch (Exception ignore) {
        }
        try {
            gatt.close();
        } catch (Exception ignore) {
        }
    }

    private void notifyDisconnected() {
        device.connectionState = ConnectionState.DISCONNECTED;
        sendConnectionCallback();
    }

    private void sendConnectionCallback() {
        if (lastConnectionState != device.connectionState) {
            lastConnectionState = device.connectionState;
            if (observer != null) {
                posterDispatcher.post(observer, MethodInfoGenerator.onConnectionStateChanged(device));
            }
            observable.notifyObservers(MethodInfoGenerator.onConnectionStateChanged(device));
        }
    }

    private boolean write(GenericRequest request, BluetoothGattCharacteristic characteristic, byte[] value) {
        characteristic.setValue(value);
        int writeType = request . writeOptions . writeType;
        if ((writeType == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE ||
                    writeType == BluetoothGattCharacteristic.WRITE_TYPE_SIGNED ||
                    writeType == BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        ) {
            characteristic.setWriteType(writeType);
        }
        if (bluetoothGatt == null) {
            handleFailedCallback(request, REQUEST_FAIL_TYPE_GATT_IS_NULL, true);
            return false;
        }
        if (!bluetoothGatt.writeCharacteristic(characteristic)) {
            handleWriteFailed(request);
            return false;
        }
        return true;
    }

    private void handleWriteFailed(GenericRequest request) {
        connHandler.removeMessages(MSG_REQUEST_TIMEOUT);
        request.remainQueue = null;
        handleFailedCallback(request, REQUEST_FAIL_TYPE_REQUEST_FAILED, true);
    }

    private boolean enableNotificationOrIndicationFail(
        boolean enable,
        boolean notification,
        BluetoothGattCharacteristic characteristic
    ) {
        if (!bluetoothAdapter.isEnabled() || bluetoothGatt == null || !bluetoothGatt
                .setCharacteristicNotification(characteristic, enable)
        ) {
            return true;
        }
        BluetoothGattDescriptor descriptor = characteristic . getDescriptor (clientCharacteristicConfig);
        if (descriptor == null) {
            return true;
        }
        byte[] originValue = descriptor . getValue ();
        if (currentRequest != null) {
            if (currentRequest.type == RequestType.SET_NOTIFICATION || currentRequest.type == RequestType.SET_INDICATION) {
                currentRequest.descriptorTemp = originValue;
            }
        }
        if (enable) {
            if (notification) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            } else {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            }
        } else {
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }
        // There was a bug in Android up to 6.0 where the descriptor was written using parent
        // characteristic's write type, instead of always Write With Response, as the spec says.
        int writeType = characteristic . getWriteType ();
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        boolean result = bluetoothGatt . writeDescriptor (descriptor);
        if (!enable) {
            //
            descriptor.setValue(originValue);
        }
        characteristic.setWriteType(writeType);
        return !result;
    }

    private void enqueue(GenericRequest request) {
        if (isReleased) {
            handleFailedCallback(request, REQUEST_FAIL_TYPE_CONNECTION_RELEASED, false);
        } else {
            synchronized(this) {
                if (currentRequest == null) {
                    executeRequest(request);
                } else {
                    //
                    int index = - 1;
                    for (int i = 0; i < requestQueue.size(); i++) {
                        GenericRequest req = requestQueue . get (i);
                        if (req.priority >= request.priority) {
                            if (i < requestQueue.size() - 1) {
                                if (requestQueue.get(i + 1).priority < request.priority) {
                                    index = i + 1;
                                    break;
                                }
                            } else {
                                index = i + 1;
                            }
                        }
                    }
                    if (index == -1) {
                        requestQueue.add(0, request);
                    } else if (index >= requestQueue.size()) {
                        requestQueue.add(request);
                    } else {
                        requestQueue.add(index, request);
                    }
                }
            }
        }
    }

    private void executeNextRequest() {
        synchronized(this) {
            connHandler.removeMessages(MSG_REQUEST_TIMEOUT);
            if (requestQueue.isEmpty()) {
                currentRequest = null;
            } else {
                executeRequest(requestQueue.remove(0));
            }
        }
    }

    private void executeRequest(GenericRequest request) {
        currentRequest = request;
        connHandler.sendMessageDelayed(
            Message.obtain(connHandler, MSG_REQUEST_TIMEOUT, request),
            configuration.requestTimeoutMillis
        );
        if (bluetoothAdapter.isEnabled()) {
            if (bluetoothGatt != null) {
                switch(request.type) {
                    case READ_RSSI :
                    if (!bluetoothGatt.readRemoteRssi()) {
                        handleFailedCallback(request, REQUEST_FAIL_TYPE_REQUEST_FAILED, true);
                    }
                    break;
                    case CHANGE_MTU :
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        if (!bluetoothGatt.requestMtu((int) request . value)) {
                            handleFailedCallback(request, REQUEST_FAIL_TYPE_REQUEST_FAILED, true);
                        }
                    }
                    break;
                    case READ_PHY :
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        bluetoothGatt.readPhy();
                    }
                    break;
                    case SET_PREFERRED_PHY :
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        int[] options =(int[]) request . value;
                        bluetoothGatt.setPreferredPhy(options[0], options[1], options[2]);
                    }
                    break;
                    default:
                    BluetoothGattService gattService = bluetoothGatt . getService (request.service);
                    if (gattService != null) {
                        BluetoothGattCharacteristic characteristic = gattService . getCharacteristic (request.characteristic);
                        if (characteristic != null) {
                            switch(request.type) {
                                case SET_NOTIFICATION :
                                case SET_INDICATION :
                                executeIndicationOrNotification(request, characteristic);
                                break;
                                case READ_CHARACTERISTIC :
                                executeReadCharacteristic(request, characteristic);
                                break;
                                case READ_DESCRIPTOR :
                                executeReadDescriptor(request, characteristic);
                                break;
                                case WRITE_CHARACTERISTIC :
                                executeWriteCharacteristic(request, characteristic);
                                break;
                            }
                        } else {
                            handleFailedCallback(request, REQUEST_FAIL_TYPE_CHARACTERISTIC_NOT_EXIST, true);
                        }
                    } else {
                        handleFailedCallback(request, REQUEST_FAIL_TYPE_SERVICE_NOT_EXIST, true);
                    }
                    break;
                }
            } else {
                handleFailedCallback(request, REQUEST_FAIL_TYPE_GATT_IS_NULL, true);
            }
        } else {
            handleFailedCallback(request, REQUEST_FAIL_TYPE_BLUETOOTH_ADAPTER_DISABLED, true);
        }
    }

    private void printWriteLog(GenericRequest request, int progress, int total, byte[] value) {
        if (logger.isEnabled()) {
            String t = String . valueOf (total);
            StringBuilder sb = new StringBuilder(String.valueOf(progress));
            while (sb.length() < t.length()) {
                sb.insert(0, "0");
            }
            logD(
                Logger.TYPE_CHARACTERISTIC_WRITE, "package [%s/%s] write success! [UUID: %s, addr: %s, value: %s]",
                sb, t, substringUuid(request.characteristic), device.address, toHex(value)
            );
        }
    }

    private void executeWriteCharacteristic(GenericRequest request, BluetoothGattCharacteristic characteristic) {
        try {
            byte[] value =(byte[]) request . value;
            WriteOptions options = request . writeOptions;
            int reqDelay = options . requestWriteDelayMillis > 0 ? options.requestWriteDelayMillis : options.packageWriteDelayMillis;
            if (reqDelay > 0) {
                try {
                    Thread.sleep(reqDelay);
                } catch (InterruptedException ignore) {
                }
                if (request != currentRequest) {
                    return;
                }
            }
            if (options.useMtuAsPackageSize) {
                options.packageSize = mtu - 3;
            }
            if (value.length > options.packageSize) {
                List < byte[] > list = UnifiedMathUtils.INSTANCE.splitPackage(value, options.packageSize);
                if (!options.isWaitWriteResult) { //ï¼Œ
                    int delay = options . packageWriteDelayMillis;
                    for (int i = 0; i < list.size(); i++) {
                        byte[] bytes = list . get (i);
                        if (i > 0 && delay > 0) {
                            try {
                                Thread.sleep(delay);
                            } catch (InterruptedException ignore) {
                            }
                            if (request != currentRequest) {
                                return;
                            }
                        }
                        if (!write(request, characteristic, bytes)) {
                            return;
                        } else {
                            printWriteLog(request, i + 1, list.size(), bytes);
                        }
                    }
                    printWriteLog(request, list.size(), list.size(), list.get(list.size() - 1));
                } else { //ï¼Œ
                    request.remainQueue = new ConcurrentLinkedQueue < > ();
                    request.remainQueue.addAll(list);
                    request.sendingBytes = request.remainQueue.remove();
                    write(request, characteristic, request.sendingBytes);
                }
            } else {
                request.sendingBytes = value;
                if (write(request, characteristic, value)) {
                    if (!options.isWaitWriteResult) {
                        notifyCharacteristicWrite(request, value);
                        printWriteLog(request, 1, 1, value);
                        executeNextRequest();
                    }
                }
            }
        } catch (Exception e) {
            handleWriteFailed(request);
        }
    }

    private void executeReadDescriptor(GenericRequest request, BluetoothGattCharacteristic characteristic) {
        BluetoothGattDescriptor gattDescriptor = characteristic . getDescriptor (request.descriptor);
        if (gattDescriptor != null) {
            if (!bluetoothGatt.readDescriptor(gattDescriptor)) {
                handleFailedCallback(request, REQUEST_FAIL_TYPE_REQUEST_FAILED, true);
            }
        } else {
            handleFailedCallback(request, REQUEST_FAIL_TYPE_DESCRIPTOR_NOT_EXIST, true);
        }
    }

    private void executeReadCharacteristic(GenericRequest request, BluetoothGattCharacteristic characteristic) {
        if (!bluetoothGatt.readCharacteristic(characteristic)) {
            handleFailedCallback(request, REQUEST_FAIL_TYPE_REQUEST_FAILED, true);
        }
    }

    private void executeIndicationOrNotification(GenericRequest request, BluetoothGattCharacteristic characteristic) {
        if (enableNotificationOrIndicationFail(
                ((int) request . value) == 1,
                request.type == RequestType.SET_NOTIFICATION, characteristic
            )
        ) {
            handleGattStatusFailed();
        }
    }

    private void handlePhyChange(boolean read, int txPhy, int rxPhy, int status) {
        if (currentRequest != null) {
            if ((read && currentRequest.type == RequestType.READ_PHY) || ((!read && currentRequest.type == RequestType.SET_PREFERRED_PHY))) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    notifyPhyChange(currentRequest, txPhy, rxPhy);
                } else {
                    handleGattStatusFailed();
                }
                executeNextRequest();
            }
        }
    }

    private void handleGattStatusFailed() {
        if (currentRequest != null) {
            handleFailedCallback(currentRequest, REQUEST_FAIL_TYPE_GATT_STATUS_FAILED, false);
        }
    }

    private void handleFailedCallback(GenericRequest request, int failType, boolean executeNext) {
        notifyRequestFailed(request, failType);
        if (executeNext) {
            executeNextRequest();
        }
    }

    private String toHex(byte[] bytes) {
        return UnifiedBleUtils.INSTANCE.bytesToHexString(bytes);
    }

    private String substringUuid(UUID uuid) {
        return uuid == null ? "null" : uuid.toString().substring(0, 8);
    }

    private void handleCallbacks(RequestCallback callback, MethodInfo info) {
        if (observer != null) {
            posterDispatcher.post(observer, info);//
        }
        if (callback != null) {//
            posterDispatcher.post(callback, info);
        } else {//
            observable.notifyObservers(info);
        }
    }

    private void log(int priority, int type, String format, Object... args) {
        logger.log(priority, type, String.format(Locale.US, format, args));
    }

    private void logE(int type, String format, Object... args) {
        log(Log.ERROR, type, format, args);
    }

    private void logD(int type, String format, Object... args) {
        log(Log.DEBUG, type, format, args);
    }

    private void notifyRequestFailed(GenericRequest request, int failType) {
        MethodInfo info = MethodInfoGenerator . onRequestFailed (request, failType, request.value);
        handleCallbacks(request.callback, info);
        logE(
            Logger.TYPE_REQUEST_FAILED, "request failed! [requestType: %s, addr: %s, failType: %d",
            request.type, device.address, failType
        );
    }

    private void notifyCharacteristicRead(GenericRequest request, byte[] value) {
        MethodInfo info = MethodInfoGenerator . onCharacteristicRead (request, value);
        handleCallbacks(request.callback, info);
        logD(
            Logger.TYPE_CHARACTERISTIC_READ, "characteristic read! [UUID: %s, addr: %s, value: %s]",
            substringUuid(request.characteristic), device.address, toHex(value)
        );
    }

    private void notifyCharacteristicChanged(BluetoothGattCharacteristic characteristic) {
        MethodInfo info = MethodInfoGenerator . onCharacteristicChanged (device, characteristic.getService().getUuid(),
        characteristic.getUuid(), characteristic.getValue());
        observable.notifyObservers(info);
        if (observer != null) {
            posterDispatcher.post(observer, info);
        }
        logD(
            Logger.TYPE_CHARACTERISTIC_CHANGED, "characteristic change! [UUID: %s, addr: %s, value: %s]",
            substringUuid(characteristic.getUuid()), device.address, toHex(characteristic.getValue())
        );
    }

    private void notifyRssiRead(GenericRequest request, int rssi) {
        MethodInfo info = MethodInfoGenerator . onRssiRead (request, rssi);
        handleCallbacks(request.callback, info);
        logD(Logger.TYPE_READ_REMOTE_RSSI, "rssi read! [addr: %s, rssi: %d]", device.address, rssi);
    }

    private void notifyMtuChanged(GenericRequest request, int mtu) {
        MethodInfo info = MethodInfoGenerator . onMtuChanged (request, mtu);
        handleCallbacks(request.callback, info);
        logD(Logger.TYPE_MTU_CHANGED, "mtu change! [addr: %s, mtu: %d]", device.address, mtu);
    }

    private void notifyDescriptorRead(GenericRequest request, byte[] value) {
        MethodInfo info = MethodInfoGenerator . onDescriptorRead (request, value);
        handleCallbacks(request.callback, info);
        logD(
            Logger.TYPE_DESCRIPTOR_READ, "descriptor read! [UUID: %s, addr: %s, value: %s]",
            substringUuid(request.characteristic), device.address, toHex(value)
        );
    }

    private void notifyNotificationChanged(GenericRequest request, boolean isEnabled) {
        MethodInfo info = MethodInfoGenerator . onNotificationChanged (request, isEnabled);
        handleCallbacks(request.callback, info);
        if (request.type == RequestType.SET_NOTIFICATION) {
            logD(Logger.TYPE_NOTIFICATION_CHANGED, "%s [UUID: %s, addr: %s]", isEnabled ? "notification enabled!" :
            "notification disabled!", substringUuid(request.characteristic), device.address);
        } else {
            logD(Logger.TYPE_INDICATION_CHANGED, "%s [UUID: %s, addr: %s]", isEnabled ? "indication enabled!" :
            "indication disabled!", substringUuid(request.characteristic), device.address);
        }
    }

    private void notifyCharacteristicWrite(GenericRequest request, byte[] value) {
        MethodInfo info = MethodInfoGenerator . onCharacteristicWrite (request, value);
        handleCallbacks(request.callback, info);
    }

    private void notifyPhyChange(GenericRequest request, int txPhy, int rxPhy) {
        MethodInfo info = MethodInfoGenerator . onPhyChange (request, txPhy, rxPhy);
        handleCallbacks(request.callback, info);
        String event = request . type == RequestType . READ_PHY ? "phy read!" : "phy update!";
        logD(Logger.TYPE_PHY_CHANGE, "%s [addr: %s, tvPhy: %s, rxPhy: %s]", event, device.address, txPhy, rxPhy);
    }

    @Override
    public int getMtu() {
        return mtu;
    }

    @NonNull
    @Override
    public Device getDevice() {
        return device;
    }

    @Override
    public void reconnect() {
        if (!isReleased) {
            isActiveDisconnect = false;
            tryReconnectCount = 0;
            reconnectImmediatelyCount = 0;
            Message.obtain(connHandler, MSG_DISCONNECT, MSG_ARG_RECONNECT, 0).sendToTarget();
        }
    }

    @Override
    public void disconnect() {
        if (!isReleased) {
            isActiveDisconnect = true;
            Message.obtain(connHandler, MSG_DISCONNECT, MSG_ARG_NONE, 0).sendToTarget();
        }
    }

    //
    @SuppressWarnings("all")
    private boolean doRefresh() {
        try {
            Method localMethod = bluetoothGatt . getClass ().getMethod("refresh");
            return (boolean) localMethod . invoke (bluetoothGatt);
        } catch (Exception ignore) {
        }
        return false;
    }

    @Override
    public void refresh() {
        connHandler.sendEmptyMessage(MSG_REFRESH);
    }

    private void release(boolean noEvent) {
        if (!isReleased) {
            isReleased = true;
            configuration.setAutoReconnect(false); //
            connHandler.removeCallbacksAndMessages(null);
            easyBle.removeScanListener(this);
            clearRequestQueueAndNotify();
            if (bluetoothGatt != null) {
                closeGatt(bluetoothGatt);
                bluetoothGatt = null;
            }
            device.connectionState = ConnectionState.RELEASED;
            logD(
                Logger.TYPE_CONNECTION_STATE,
                "connection released! [name: %s, addr: %s]",
                device.name,
                device.address
            );
            if (!noEvent) {
                sendConnectionCallback();
            }
            easyBle.releaseConnection(device);//
        }
    }

    @Override
    public void release() {
        release(false);
    }

    @Override
    public void releaseNoEvent() {
        release(true);
    }

    @NonNull
    @Override
    public ConnectionState getConnectionState() {
        return device.connectionState;
    }

    @Override
    public boolean isAutoReconnectEnabled() {
        return configuration.isAutoReconnect;
    }

    @Nullable
    @Override
    public BluetoothGatt getGatt() {
        return bluetoothGatt;
    }

    @Override
    public void clearRequestQueue() {
        synchronized(this) {
            requestQueue.clear();
            currentRequest = null;
        }
    }

    @Override
    public void clearRequestQueueByType(RequestType type) {
        synchronized(this) {
            Iterator<GenericRequest> it = requestQueue . iterator ();
            while (it.hasNext()) {
                GenericRequest request = it . next ();
                if (request.type == type) {
                    it.remove();
                }
            }
            if (currentRequest != null && currentRequest.type == type) {
                currentRequest = null;
            }
        }
    }

    private void clearRequestQueueAndNotify() {
        synchronized(this) {
            for (GenericRequest request : requestQueue) {
            handleFailedCallback(request, REQUEST_FAIL_TYPE_CONNECTION_DISCONNECTED, false);
        }
            if (currentRequest != null) {
                handleFailedCallback(currentRequest, REQUEST_FAIL_TYPE_CONNECTION_DISCONNECTED, false);
            }
        }
        clearRequestQueue();
    }

    @NonNull
    @Override
    public ConnectionConfiguration getConnectionConfiguration() {
        return configuration;
    }

    @Nullable
    @Override
    public BluetoothGattService getService(UUID service) {
        if (service != null && bluetoothGatt != null) {
            return bluetoothGatt.getService(service);
        }
        return null;
    }

    @Nullable
    @Override
    public BluetoothGattCharacteristic getCharacteristic(UUID service, UUID characteristic) {
        if (service != null && characteristic != null && bluetoothGatt != null) {
            BluetoothGattService gattService = bluetoothGatt . getService (service);
            if (gattService != null) {
                return gattService.getCharacteristic(characteristic);
            }
        }
        return null;
    }

    @Nullable
    @Override
    public BluetoothGattDescriptor getDescriptor(UUID service, UUID characteristic, UUID descriptor) {
        if (service != null && characteristic != null && descriptor != null && bluetoothGatt != null) {
            BluetoothGattService gattService = bluetoothGatt . getService (service);
            if (gattService != null) {
                BluetoothGattCharacteristic gattCharacteristic = gattService . getCharacteristic (characteristic);
                if (gattCharacteristic != null) {
                    return gattCharacteristic.getDescriptor(descriptor);
                }
            }
        }
        return null;
    }

    //uuidï¼Œï¼Œ
    private void checkUuidExistsAndEnqueue(GenericRequest request, int uuidNum) {
        boolean exists = false;
        if (uuidNum > 2) {
            exists = checkDescriptorExists(request, request.service, request.characteristic, request.descriptor);
        } else if (uuidNum > 1) {
            exists = checkCharacteristicExists(request, request.service, request.characteristic);
        } else if (uuidNum == 1) {
            exists = checkServiceExists(request, request.service);
        }
        if (exists) {
            enqueue(request);
        }
    }

    //
    private boolean checkServiceExists(GenericRequest request, UUID uuid) {
        if (getService(uuid) == null) {
            handleFailedCallback(request, REQUEST_FAIL_TYPE_SERVICE_NOT_EXIST, false);
            return false;
        }
        return true;
    }

    //
    private boolean checkCharacteristicExists(GenericRequest request, UUID service, UUID characteristic) {
        if (checkServiceExists(request, service)) {
            if (getCharacteristic(service, characteristic) == null) {
                handleFailedCallback(request, REQUEST_FAIL_TYPE_CHARACTERISTIC_NOT_EXIST, false);
                return false;
            }
            return true;
        }
        return false;
    }

    //Descriptor
    private boolean checkDescriptorExists(GenericRequest request, UUID service, UUID characteristic, UUID descriptor) {
        if (checkServiceExists(request, service) && checkCharacteristicExists(request, service, characteristic)) {
            if (getDescriptor(service, characteristic, descriptor) == null) {
                handleFailedCallback(request, REQUEST_FAIL_TYPE_DESCRIPTOR_NOT_EXIST, false);
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public void execute(Request request) {
        if (request instanceof GenericRequest) {
            GenericRequest req =(GenericRequest) request;
            req.device = device;
            switch(req.type) {
                case SET_NOTIFICATION :
                case SET_INDICATION :
                case READ_CHARACTERISTIC :
                case WRITE_CHARACTERISTIC :
                if (req.type == RequestType.WRITE_CHARACTERISTIC && req.writeOptions == null) {
                    //
                    req.writeOptions = configuration.getDefaultWriteOptions(req.service, req.characteristic);
                    if (req.writeOptions == null) {
                        //ï¼Œ
                        req.writeOptions = new WriteOptions . Builder ().build();
                    }
                }
                checkUuidExistsAndEnqueue(req, 2);
                break;
                case READ_DESCRIPTOR :
                checkUuidExistsAndEnqueue(req, 3);
                break;
                default:
                enqueue(req);
                break;
            }
        }
    }

    @Override
    public boolean isNotificationOrIndicationEnabled(BluetoothGattCharacteristic characteristic) {
        Inspector.requireNonNull(characteristic, "characteristic can't be null");
        BluetoothGattDescriptor descriptor = characteristic . getDescriptor (clientCharacteristicConfig);
        return descriptor != null && (Arrays.equals(
            descriptor.getValue(),
            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        ) ||
                Arrays.equals(descriptor.getValue(), BluetoothGattDescriptor.ENABLE_INDICATION_VALUE));
    }

    @Override
    public boolean isNotificationOrIndicationEnabled(UUID service, UUID characteristic) {
        BluetoothGattCharacteristic c = getCharacteristic (service, characteristic);
        if (c != null) {
            return isNotificationOrIndicationEnabled(c);
        }
        return false;
    }

    private static class ConnHandler extends Handler {
        private final WeakReference<ConnectionImpl> weakRef;

        ConnHandler(ConnectionImpl connection) {
            super(Looper.getMainLooper());
            weakRef = new WeakReference < > (connection);
        }

        @Override
        public void handleMessage(Message msg) {
            ConnectionImpl connection = weakRef . get ();
            if (connection != null) {
                if (connection.isReleased) {
                    return;
                }
                switch(msg.what) {
                    case MSG_REQUEST_TIMEOUT :
                    GenericRequest request =(GenericRequest) msg . obj;
                    if (connection.currentRequest != null && connection.currentRequest == request) {
                        connection.handleFailedCallback(request, REQUEST_FAIL_TYPE_REQUEST_TIMEOUT, false);
                        connection.executeNextRequest();
                    }
                    break;
                    case MSG_CONNECT ://
                    if (connection.bluetoothAdapter.isEnabled()) {
                        connection.doConnect();
                    }
                    break;
                    case MSG_DISCONNECT ://
                    boolean reconnect = msg . arg1 == MSG_ARG_RECONNECT && connection . bluetoothAdapter . isEnabled ();
                    connection.doDisconnect(reconnect);
                    break;
                    case MSG_REFRESH ://
                    connection.doRefresh(false);
                    break;
                    case MSG_TIMER ://
                    connection.doTimer();
                    break;
                    case MSG_DISCOVER_SERVICES ://
                    case MSG_ON_CONNECTION_STATE_CHANGE ://
                    case MSG_ON_SERVICES_DISCOVERED ://
                    if (connection.bluetoothAdapter.isEnabled()) {
                        if (msg.what == MSG_DISCOVER_SERVICES) {
                            connection.doDiscoverServices();
                        } else if (msg.what == MSG_ON_SERVICES_DISCOVERED) {
                            connection.doOnServicesDiscovered(msg.arg1);
                        } else {
                            connection.doOnConnectionStateChange(msg.arg1, msg.arg2);
                        }
                    }
                    break;
                }
            }
        }
    }

    private class BleGattCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (originCallback != null) {
                easyBle.getExecutorService()
                    .execute(() -> originCallback.onConnectionStateChange(gatt, status, newState));
            }
            if (!isReleased) {
                Message.obtain(connHandler, MSG_ON_CONNECTION_STATE_CHANGE, status, newState).sendToTarget();
            } else {
                closeGatt(gatt);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (originCallback != null) {
                easyBle.getExecutorService().execute(() -> originCallback.onServicesDiscovered(gatt, status));
            }
            if (!isReleased) {
                Message.obtain(connHandler, MSG_ON_SERVICES_DISCOVERED, status, 0).sendToTarget();
            } else {
                closeGatt(gatt);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.e(
                "bcf",
                "onCharacteristicRead  status: " + status + "  value: " + UnifiedBleUtils.INSTANCE.bytesToHexString(
                    characteristic.getValue()
                )
            );
            if (originCallback != null) {
                easyBle.getExecutorService()
                    .execute(() -> originCallback.onCharacteristicRead(gatt, characteristic, status));
            }
            if (currentRequest != null) {
                if (currentRequest.type == RequestType.READ_CHARACTERISTIC) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        notifyCharacteristicRead(currentRequest, characteristic.getValue());
                    } else {
                        handleGattStatusFailed();
                    }
                    executeNextRequest();
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//            Log.e("bcf","onCharacteristicWrite  status: "+status);
            if (originCallback != null) {
                easyBle.getExecutorService()
                    .execute(() -> originCallback.onCharacteristicWrite(gatt, characteristic, status));
            }
            if (currentRequest != null && currentRequest.type == RequestType.WRITE_CHARACTERISTIC &&
                currentRequest.writeOptions.isWaitWriteResult
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (logger.isEnabled()) {
                        byte[] data =(byte[]) currentRequest . value;//
                        int packageSize = currentRequest . writeOptions . packageSize;
                        int total = data . length / packageSize +(data.length % packageSize == 0 ? 0 : 1);
                        int progress;
                        if (currentRequest.remainQueue == null || currentRequest.remainQueue.isEmpty()) {
                            progress = total;
                        } else {
                            progress = data.length / packageSize - currentRequest.remainQueue.size() + 1;
                        }
                        printWriteLog(currentRequest, progress, total, characteristic.getValue());
                    }
                    if (currentRequest.remainQueue == null || currentRequest.remainQueue.isEmpty()) {
                        notifyCharacteristicWrite(currentRequest, (byte[]) currentRequest . value);
                        executeNextRequest();
                    } else {
                        connHandler.removeMessages(MSG_REQUEST_TIMEOUT);
                        connHandler.sendMessageDelayed(
                            Message.obtain(connHandler, MSG_REQUEST_TIMEOUT, currentRequest),
                            configuration.requestTimeoutMillis
                        );
                        GenericRequest req = currentRequest;
                        int delay = currentRequest . writeOptions . packageWriteDelayMillis;
                        if (delay > 0) {
                            try {
                                Thread.sleep(delay);
                            } catch (InterruptedException ignore) {
                            }
                            if (req != currentRequest) {
                                return;
                            }
                        }
                        req.sendingBytes = req.remainQueue.remove();
                        write(req, characteristic, req.sendingBytes);
                    }
                } else {
                    handleFailedCallback(currentRequest, REQUEST_FAIL_TYPE_GATT_STATUS_FAILED, true);
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (originCallback != null) {
                easyBle.getExecutorService()
                    .execute(() -> originCallback.onCharacteristicChanged(gatt, characteristic));
            }
            notifyCharacteristicChanged(characteristic);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            device.setRssi(rssi);
            if (originCallback != null) {
                easyBle.getExecutorService().execute(() -> originCallback.onReadRemoteRssi(gatt, rssi, status));
            }
            if (currentRequest != null) {
                if (currentRequest.type == RequestType.READ_RSSI) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        notifyRssiRead(currentRequest, rssi);
                    } else {
                        handleGattStatusFailed();
                    }
                    executeNextRequest();
                }
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (originCallback != null) {
                easyBle.getExecutorService().execute(() -> originCallback.onDescriptorRead(gatt, descriptor, status));
            }
            if (currentRequest != null) {
                if (currentRequest.type == RequestType.READ_DESCRIPTOR) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        notifyDescriptorRead(currentRequest, descriptor.getValue());
                    } else {
                        handleGattStatusFailed();
                    }
                    executeNextRequest();
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (originCallback != null) {
                easyBle.getExecutorService().execute(() -> originCallback.onDescriptorWrite(gatt, descriptor, status));
            }
            if (currentRequest != null) {
                if (currentRequest.type == RequestType.SET_NOTIFICATION || currentRequest.type == RequestType.SET_INDICATION) {
                    BluetoothGattDescriptor localDescriptor = getDescriptor (descriptor.getCharacteristic().getService()
                        .getUuid(),
                    descriptor.getCharacteristic().getUuid(), clientCharacteristicConfig);
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        handleGattStatusFailed();
                        if (localDescriptor != null) {
                            localDescriptor.setValue(currentRequest.descriptorTemp);
                        }
                    } else {
                        notifyNotificationChanged(currentRequest, ((int) currentRequest . value) == 1);
                    }
                    executeNextRequest();
                }
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            if (originCallback != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    easyBle.getExecutorService().execute(() -> originCallback.onMtuChanged(gatt, mtu, status));
                }
            }
            if (currentRequest != null) {
                if (currentRequest.type == RequestType.CHANGE_MTU) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        ConnectionImpl.this.mtu = mtu;
                        notifyMtuChanged(currentRequest, mtu);
                    } else {
                        handleGattStatusFailed();
                    }
                    executeNextRequest();
                }
            }
        }

        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            if (originCallback != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    easyBle.getExecutorService().execute(() -> originCallback.onPhyRead(gatt, txPhy, rxPhy, status));
                }
            }
            handlePhyChange(true, txPhy, rxPhy, status);
        }

        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            if (originCallback != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    easyBle.getExecutorService().execute(() -> originCallback.onPhyRead(gatt, txPhy, rxPhy, status));
                }
            }
            handlePhyChange(false, txPhy, rxPhy, status);
        }
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\ConnectionState.java =====

package com.topdon.ble;

public enum ConnectionState {

    DISCONNECTED,

    CONNECTING,

    SCANNING_FOR_RECONNECTION,

    CONNECTED,

    SERVICE_DISCOVERING,

    SERVICE_DISCOVERED,

    RELEASED,

    TIMEOUT,

    MTU_SUCCESS
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\DefaultDeviceCreator.java =====

package com.topdon.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;

import androidx.annotation.Nullable;

class DefaultDeviceCreator implements DeviceCreator {
    @Nullable
    @Override
    public Device create(BluetoothDevice device, ScanResult scanResult) {
        return new Device (device);
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\Device.java =====

package com.topdon.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.Objects;

public class Device implements Comparable<Device>, Cloneable, Parcelable {
    public static final Creator < Device > CREATOR = new Creator<Device>() {
        @Override
        public Device createFromParcel(Parcel source) {
            return new Device (source);
        }

        @Override
        public Device [] newArray (int size) {
            return new Device [size];
        }
    };
    private final BluetoothDevice originDevice;
    ConnectionState connectionState = ConnectionState . DISCONNECTED;
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @Nullable
    ScanResult scanResult;
    @Nullable
    byte[] scanRecord;
    String name = "";
    String address = "";
    int rssi = - 120;

    public Device (BluetoothDevice originDevice) {
        this.originDevice = originDevice;
        try {
            this.name = originDevice.getName() == null ? "" : originDevice.getName();
            this.address = originDevice.getAddress();
        } catch (SecurityException e) {
            // Log the error and initialize with default values to prevent crashes
            this.name = "";
            this.address = "";
        }
    }

    protected Device (Parcel in) {
        this.originDevice = in.readParcelable(BluetoothDevice.class. getClassLoader ());
        readFromParcel(in);
    }

    @NonNull
    public BluetoothDevice getOriginDevice() {
        return originDevice;
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @Nullable
    public ScanResult getScanResult() {
        return scanResult;
    }

    @Nullable
    public byte [] getScanRecord () {
        return scanRecord;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @NonNull
    public String getAddress() {
        return address;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    @NonNull
    public ConnectionState getConnectionState() {
        Connection connection = EasyBLE . getInstance ().getConnection(this);
        return connection == null ? connectionState : connection.getConnectionState();
    }

    @Nullable
    public Boolean isConnectable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (scanResult != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    return scanResult.isConnectable();
                }
            }
        }
        return null;
    }

    public boolean isConnected() {
        return getConnectionState() == ConnectionState.SERVICE_DISCOVERED;
    }

    public boolean isDisconnected() {
        ConnectionState state = getConnectionState ();
        return state == ConnectionState.DISCONNECTED || state == ConnectionState.RELEASED;
    }

    public boolean isConnecting() {
        ConnectionState state = getConnectionState ();
        return state != ConnectionState.DISCONNECTED && state != ConnectionState.SERVICE_DISCOVERED &&
                state != ConnectionState.RELEASED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Device)) return false;

        Device device =(Device) o;

        return address.equals(device.address);
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }

    @Override
    public int compareTo(Device other) {
        if (rssi == 0) {
            return -1;
        } else if (other.rssi == 0) {
            return 1;
        } else {
            int result = Integer . compare (other.rssi, rssi);
            if (result == 0) {
                result = name.compareTo(other.name);
            }
            return result;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "Device{" +
                "name='" + name + '\'' +
                ", address='" + address + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.originDevice, flags);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dest.writeParcelable(this.scanResult, flags);
        }
        if (this.scanRecord != null) {
            dest.writeInt(this.scanRecord.length);
            dest.writeByteArray(this.scanRecord);
        } else {
            dest.writeInt(-1);
        }
        dest.writeString(this.name);
        dest.writeString(this.address);
        dest.writeInt(this.rssi);
        for (ConnectionState state : ConnectionState.values()) {
        if (state == connectionState) {
            dest.writeString(this.connectionState.name());
            break;
        }
    }
    }

    public void readFromParcel(Parcel in) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.scanResult = in.readParcelable(ScanResult.class. getClassLoader ());
        }
        int scanRecordLen = in . readInt ();
        if (scanRecordLen > 0) {
            this.scanRecord = new byte [scanRecordLen];
            in.readByteArray(this.scanRecord);
        }
        String inName = in . readString ();
        this.name = inName == null ? "" : inName;
        this.address = Objects.requireNonNull(in.readString());
        this.rssi = in.readInt();
        this.connectionState = ConnectionState.valueOf(in.readString());
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\DeviceCreator.java =====

package com.topdon.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;

import androidx.annotation.Nullable;

public interface DeviceCreator {

    @Nullable
    Device create(BluetoothDevice device, ScanResult scanResult);
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\EasyBLE.java =====

package com.topdon.ble;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.topdon.ble.callback.ScanListener;
import com.topdon.ble.util.DefaultLogger;
import com.topdon.ble.util.Logger;
import com.topdon.commons.observer.Observable;
import com.topdon.commons.poster.MethodInfo;
import com.topdon.commons.poster.PosterDispatcher;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

public class EasyBLE {
    private static final EasyBLEBuilder DEFAULT_BUILDER = new EasyBLEBuilder();
    static volatile EasyBLE instance;
    public final ScanConfiguration scanConfiguration;
    private final ExecutorService executorService;
    private final PosterDispatcher posterDispatcher;
    private final BondController bondController;
    private final DeviceCreator deviceCreator;
    private final Observable observable;
    private final Logger logger;
    private final ScannerType scannerType;
    private final Map<String, Connection> connectionMap = new ConcurrentHashMap<>();
    //MAC
    private final List<String> addressList = new CopyOnWriteArrayList<>();
    private final boolean internalObservable;
    private Scanner scanner;
    private Application application;
    private boolean isInitialized;
    private BluetoothAdapter bluetoothAdapter;
    private BroadcastReceiver broadcastReceiver;

    private EasyBLE()
    {
        this(DEFAULT_BUILDER);
    }

    EasyBLE(EasyBLEBuilder builder)
    {
        tryGetApplication();
        bondController = builder.bondController;
        scannerType = builder.scannerType;
        deviceCreator = builder.deviceCreator == null ? new DefaultDeviceCreator() : builder.deviceCreator;
        scanConfiguration = builder.scanConfiguration == null ? new ScanConfiguration() : builder.scanConfiguration;
        logger = builder.logger == null ? new DefaultLogger("EasyBLE") : builder.logger;
        if (builder.observable != null) {
            internalObservable = false;
            observable = builder.observable;
            posterDispatcher = observable.getPosterDispatcher();
            executorService = posterDispatcher.getExecutorService();
        } else {
            internalObservable = true;
            executorService = builder.executorService;
            posterDispatcher = new PosterDispatcher (executorService, builder.methodDefaultThreadMode);
            observable = new Observable (posterDispatcher, builder.isObserveAnnotationRequired);
        }
    }

    public static EasyBLE getInstance()
    {
        if (instance == null) {
            synchronized(
                EasyBLE.class) {
                if (instance == null)
                {
                    instance = new EasyBLE ();
                }
            }
        }
        return instance;
    }

    public static EasyBLEBuilder getBuilder()
    {
        return new EasyBLEBuilder ();
    }

    @Nullable
    Context getContext()
    {
        if (application == null) {
            tryAutoInit();
        }
        return application;
    }

    @SuppressLint("PrivateApi")
    private void tryGetApplication()
    {
        try {
            Class<?> cls = Class . forName ("android.app.ActivityThread");
            Method method = cls . getMethod ("currentActivityThread");
            method.setAccessible(true);
            Object acThread = method . invoke (null);
            Method appMethod = acThread . getClass ().getMethod("getApplication");
            application = (Application) appMethod . invoke (acThread);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Nullable
    public BluetoothAdapter getBluetoothAdapter()
    {
        return bluetoothAdapter;
    }

    ExecutorService getExecutorService()
    {
        return executorService;
    }

    PosterDispatcher getPosterDispatcher()
    {
        return posterDispatcher;
    }

    DeviceCreator getDeviceCreator()
    {
        return deviceCreator;
    }

    Observable getObservable()
    {
        return observable;
    }

    Logger getLogger()
    {
        return logger;
    }

    public ScannerType getScannerType()
    {
        return scanner == null ? null : scanner.getType();
    }

    public boolean isInitialized()
    {
        return isInitialized && application != null && instance != null;
    }

    public boolean isBluetoothOn()
    {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public synchronized void initialize(Application application)
    {
        if (isInitialized()) {
            return;
        }
        Inspector.requireNonNull(application, "application can't be");
        this.application = application;
        //BLE
        if (!application.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return;
        }
        //
        BluetoothManager bluetoothManager =(BluetoothManager) application . getSystemService (Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null || bluetoothManager.getAdapter() == null) {
            return;
        }
        bluetoothAdapter = bluetoothManager.getAdapter();
        //
        if (broadcastReceiver == null) {
            broadcastReceiver = new InnerBroadcastReceiver ();
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            application.registerReceiver(broadcastReceiver, filter);
        }
        isInitialized = true;
    }

    private synchronized boolean checkStatus()
    {
        Inspector.requireNonNull(instance, "EasyBLE instance has been destroyed!");
        if (!isInitialized) {
            if (!tryAutoInit()) {
                String msg = "The SDK has not been initialized, make sure to call EasyBLE.getInstance().initialize(Application) first.";
                logger.log(Log.ERROR, Logger.TYPE_GENERAL, msg);
                return false;
            }
        } else if (application == null) {
            return tryAutoInit();
        }
        return true;
    }

    private boolean tryAutoInit()
    {
        tryGetApplication();
        if (application != null) {
            initialize(application);
        }
        return isInitialized();
    }

    public void setLogEnabled(boolean isEnabled)
    {
        logger.setEnabled(isEnabled);
    }

    public synchronized void release()
    {
        if (broadcastReceiver != null) {
            application.unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }
        isInitialized = false;
        if (scanner != null) {
            scanner.release();
        }
        releaseAllConnections();
        if (internalObservable) {
            observable.unregisterAll();
            posterDispatcher.shutdown();
        }
    }

    public void destroy()
    {
        release();
        synchronized(
            EasyBLE.class) {
                instance = null;
            }
    }

    public void registerObserver(EventObserver observer)
    {
        if (checkStatus()) {
            observable.registerObserver(observer);
        }
    }

    public boolean isObserverRegistered(EventObserver observer)
    {
        return observable.isRegistered(observer);
    }

    public void unregisterObserver(EventObserver observer)
    {
        observable.unregisterObserver(observer);
    }

    public void notifyObservers(MethodInfo info)
    {
        if (checkStatus()) {
            observable.notifyObservers(info);
        }
    }

    //
    private void checkAndInstanceScanner()
    {
        if (scanner == null) {
            synchronized(this) {
                if (bluetoothAdapter != null && scanner == null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        if (scannerType == ScannerType.LEGACY) {
                            scanner = new LegacyScanner (this, bluetoothAdapter);
                        } else if (scannerType == ScannerType.CLASSIC) {
                            scanner = new ClassicScanner (this, bluetoothAdapter);
                        } else {
                            scanner = new LeScanner (this, bluetoothAdapter);
                        }
                    } else if (scannerType == ScannerType.CLASSIC) {
                        scanner = new ClassicScanner (this, bluetoothAdapter);
                    } else {
                        scanner = new LegacyScanner (this, bluetoothAdapter);
                    }
                }
            }
        }
    }

    public void addScanListener(ScanListener listener)
    {
        checkAndInstanceScanner();
        if (checkStatus() && scanner != null) {
            scanner.addScanListener(listener);
        }
    }

    public void removeScanListener(ScanListener listener)
    {
        if (scanner != null) {
            scanner.removeScanListener(listener);
        }
    }

    public boolean isScanning()
    {
        return scanner != null && scanner.isScanning();
    }

    public void startScan()
    {
        checkAndInstanceScanner();
        if (checkStatus() && scanner != null) {
            scanner.startScan(application);
        }
    }

    public void stopScan()
    {
        if (checkStatus() && scanner != null) {
            scanner.stopScan(false);
        }
    }

    public void stopScanQuietly()
    {
        if (checkStatus() && scanner != null) {
            scanner.stopScan(true);
        }
    }

    @Nullable
    public Connection connect(String address)
    {
        return connect(address, null, null);
    }

    @Nullable
    public Connection connect(String address, ConnectionConfiguration configuration)
    {
        return connect(address, configuration, null);
    }

    @Nullable
    public Connection connect(String address, EventObserver observer)
    {
        return connect(address, null, observer);
    }

    @Nullable
    public Connection connect(String address, ConnectionConfiguration configuration,
    EventObserver observer)
    {
        if (checkStatus()) {
            Inspector.requireNonNull(address, "address can't be null");
            BluetoothDevice remoteDevice = bluetoothAdapter . getRemoteDevice (address);
            if (remoteDevice != null) {
                return connect(new Device (remoteDevice), configuration, observer);
            }
        }
        return null;
    }

    @Nullable
    public Connection connect(Device device)
    {
        return connect(device, null, null);
    }

    @Nullable
    public Connection connect(Device device, ConnectionConfiguration configuration)
    {
        return connect(device, configuration, null);
    }

    @Nullable
    public Connection connect(Device device, EventObserver observer)
    {
        return connect(device, null, observer);
    }

    @Nullable
    public synchronized Connection connect(final Device device, ConnectionConfiguration configuration,
    final EventObserver observer)
    {
        if (checkStatus()) {
            Inspector.requireNonNull(device, "device can't be null");
            Connection connection = connectionMap . remove (device.getAddress());
            //ï¼Œ
            if (connection != null) {
                connection.releaseNoEvent();
            }
            Boolean isConnectable = device . isConnectable ();
            if (isConnectable == null || isConnectable) {
                int connectDelay = 0;
                if (bondController != null && bondController.accept(device)) {
                    BluetoothDevice remoteDevice = bluetoothAdapter . getRemoteDevice (device.getAddress());
                    boolean hasPermission = Build . VERSION . SDK_INT < Build . VERSION_CODES . S ||
                    (application != null && ContextCompat.checkSelfPermission(
                        application,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED);
                    if (hasPermission && remoteDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
                        connectDelay = createBond(device.getAddress()) ? 1500 : 0;
                    }
                }
                connection = new ConnectionImpl (this, bluetoothAdapter, device, configuration, connectDelay, observer);
                connectionMap.put(device.address, connection);
                addressList.add(device.address);
                return connection;
            } else {
                String message = String . format (Locale.US, "connect failed! [type: unconnectable, name: %s, addr: %s]",
                device.getName(), device.getAddress());
                logger.log(Log.ERROR, Logger.TYPE_CONNECTION_STATE, message);
                if (observer != null) {
                    posterDispatcher.post(
                        observer,
                        MethodInfoGenerator.onConnectFailed(
                            device,
                            Connection.CONNECT_FAIL_TYPE_CONNECTION_IS_UNSUPPORTED
                        )
                    );
                }
                observable.notifyObservers(
                    MethodInfoGenerator.onConnectFailed(
                        device,
                        Connection.CONNECT_FAIL_TYPE_CONNECTION_IS_UNSUPPORTED
                    )
                );
            }
        }
        return null;
    }

    @NonNull
    public Collection<Connection> getConnections()
    {
        return connectionMap.values();
    }

    @NonNull
    public List<Connection> getOrderedConnections()
    {
        List<Connection> list = new ArrayList<>();
        for (String address : addressList) {
        Connection connection = connectionMap . get (address);
        if (connection != null) {
            list.add(connection);
        }
    }
        return list;
    }

    @Nullable
    public Connection getFirstConnection()
    {
        return addressList.isEmpty() ? null : connectionMap.get(addressList.get(0));
    }

    @Nullable
    public Connection getLastConnection()
    {
        return addressList.isEmpty() ? null : connectionMap.get(addressList.get(addressList.size()-1));
    }

    @Nullable
    public Connection getConnection(Device device)
    {
        return device == null ? null : connectionMap.get(device.getAddress());
    }

    @Nullable
    public Connection getConnection(String address)
    {
        return address == null ? null : connectionMap.get(address);
    }

    public void disconnectConnection(Device device)
    {
        if (checkStatus() && device != null) {
            Connection connection = connectionMap . get (device.getAddress());
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public void disconnectConnection(String address)
    {
        if (checkStatus() && address != null) {
            Connection connection = connectionMap . get (address);
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public void disconnectAllConnections()
    {
        if (checkStatus()) {
            for (Connection connection : connectionMap.values()) {
                connection.disconnect();
            }
        }
    }

    public void releaseAllConnections()
    {
        if (checkStatus()) {
            for (Connection connection : connectionMap.values()) {
                connection.release();
            }
            connectionMap.clear();
            addressList.clear();
        }
    }

    public void releaseConnection(String address)
    {
        if (checkStatus() && address != null) {
            addressList.remove(address);
            Connection connection = connectionMap . remove (address);
            if (connection != null) {
                connection.release();
            }
        }
    }

    public void releaseConnection(Device device)
    {
        if (checkStatus() && device != null) {
            addressList.remove(device.getAddress());
            Connection connection = connectionMap . remove (device.getAddress());
            if (connection != null) {
                connection.release();
            }
        }
    }

    public void reconnectAll()
    {
        if (checkStatus()) {
            for (Connection connection : connectionMap.values()) {
                if (connection.getConnectionState() != ConnectionState.SERVICE_DISCOVERED) {
                    connection.reconnect();
                }
            }
        }
    }

    public void reconnect(Device device)
    {
        if (checkStatus() && device != null) {
            Connection connection = connectionMap . get (device.getAddress());
            if (connection != null && connection.getConnectionState() != ConnectionState.SERVICE_DISCOVERED) {
                connection.reconnect();
            }
        }
    }

    public int getBondState(String address)
    {
        checkStatus();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                (application == null || ContextCompat.checkSelfPermission(
                    application,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED)
            ) {
                return BluetoothDevice.BOND_NONE;
            }
            return bluetoothAdapter.getRemoteDevice(address).getBondState();
        } catch (Exception e) {
            return BluetoothDevice.BOND_NONE;
        }
    }

    public boolean createBond(String address)
    {
        checkStatus();
        try {
            BluetoothDevice remoteDevice = bluetoothAdapter . getRemoteDevice (address);
            return remoteDevice.getBondState() != BluetoothDevice.BOND_NONE || remoteDevice.createBond();
        } catch (SecurityException e) {
            logger.log(
                android.util.Log.ERROR,
                Logger.TYPE_CONNECTION_STATE,
                "Missing Bluetooth permission for bonding: " + e.getMessage()
            );
            return false;
        } catch (Exception ignore) {
            return false;
        }
    }

    @SuppressWarnings("all")
    public void clearBondDevices(RemoveBondFilter filter)
    {
        checkStatus();
        if (bluetoothAdapter != null) {
            Set<BluetoothDevice> devices = bluetoothAdapter . getBondedDevices ();
            for (BluetoothDevice device : devices) {
                if (filter == null || filter.accept(device)) {
                    try {
                        device.getClass().getMethod("removeBond").invoke(device);
                    } catch (Exception ignore) {
                    }
                }
            }
        }
    }

    @SuppressWarnings("all")
    public void removeBond(String address)
    {
        checkStatus();
        try {
            BluetoothDevice remoteDevice = bluetoothAdapter . getRemoteDevice (address);
            if (remoteDevice.getBondState() != BluetoothDevice.BOND_NONE) {
                remoteDevice.getClass().getMethod("removeBond").invoke(remoteDevice);
            }
        } catch (Exception ignore) {
        }
    }

    private class InnerBroadcastReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent . getAction ();
            if (action != null) {
                switch(action) {
                    case BluetoothAdapter . ACTION_STATE_CHANGED : //
                    if (bluetoothAdapter != null) {
                        //
                        observable.notifyObservers(MethodInfoGenerator.onBluetoothAdapterStateChanged(bluetoothAdapter.getState()));
                        if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF) { //
                            logger.log(Log.DEBUG, Logger.TYPE_GENERAL, "");
                            //
                            if (scanner != null) {
                                scanner.onBluetoothOff();
                            }
                            //
                            disconnectAllConnections();
                        } else if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
                            logger.log(Log.DEBUG, Logger.TYPE_GENERAL, "");
                            //
                            for (Connection connection : connectionMap.values()) {
                                if (connection.isAutoReconnectEnabled()) {
                                    connection.reconnect();
                                }
                            }
                        }
                    }
                    break;
                    case BluetoothAdapter . ACTION_DISCOVERY_STARTED :
                    if (scanner instanceof ClassicScanner) {
                        ClassicScanner scanner =(ClassicScanner) EasyBLE .this.scanner;
                        scanner.setScanning(true);
                    }
                    break;
                    case BluetoothAdapter . ACTION_DISCOVERY_FINISHED :
                    if (scanner instanceof ClassicScanner) {
                        ClassicScanner scanner =(ClassicScanner) EasyBLE .this.scanner;
                        scanner.setScanning(false);
                    }
                    break;
                    case BluetoothDevice . ACTION_FOUND :
                    BluetoothDevice device = intent . getParcelableExtra (BluetoothDevice.EXTRA_DEVICE);
                    if (device != null && scanner instanceof ClassicScanner) {
                        int rssi = - 120;
                        Bundle extras = intent . getExtras ();
                        if (extras != null) {
                            rssi = extras.getShort(BluetoothDevice.EXTRA_RSSI);
                        }
                        ((ClassicScanner) scanner).parseScanResult(device, false, null, rssi, null);
                    }
                    break;
                }
            }
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) { //
                if (bluetoothAdapter != null) {
                    //
                    observable.notifyObservers(MethodInfoGenerator.onBluetoothAdapterStateChanged(bluetoothAdapter.getState()));
                    if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF) { //
                        logger.log(Log.DEBUG, Logger.TYPE_GENERAL, "");
                        //
                        if (scanner != null) {
                            scanner.onBluetoothOff();
                        }
                        //
                        disconnectAllConnections();
                    } else if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
                        logger.log(Log.DEBUG, Logger.TYPE_GENERAL, "");
                        //
                        for (Connection connection : connectionMap.values()) {
                            if (connection.isAutoReconnectEnabled()) {
                                connection.reconnect();
                            }
                        }
                    }
                }
            }
        }
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\EasyBLEBuilder.java =====

package com.topdon.ble;

import com.topdon.ble.util.Logger;
import com.topdon.commons.observer.Observable;
import com.topdon.commons.poster.ThreadMode;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EasyBLEBuilder {
    private final static ExecutorService DEFAULT_EXECUTOR_SERVICE = Executors.newCachedThreadPool();
    BondController bondController;
    DeviceCreator deviceCreator;
    ThreadMode methodDefaultThreadMode = ThreadMode.MAIN;
    ExecutorService executorService = DEFAULT_EXECUTOR_SERVICE;
    ScanConfiguration scanConfiguration;
    Observable observable;
    Logger logger;
    boolean isObserveAnnotationRequired = false;
    ScannerType scannerType;

    EasyBLEBuilder()
    {
    }

    public EasyBLEBuilder setScannerType(ScannerType scannerType)
    {
        Inspector.requireNonNull(scannerType, "scannerType can't be null");
        this.scannerType = scannerType;
        return this;
    }

    public EasyBLEBuilder setExecutorService(ExecutorService executorService)
    {
        Inspector.requireNonNull(executorService, "executorService can't be null");
        this.executorService = executorService;
        return this;
    }

    public EasyBLEBuilder setDeviceCreator(DeviceCreator deviceCreator)
    {
        Inspector.requireNonNull(deviceCreator, "deviceCreator can't be null");
        this.deviceCreator = deviceCreator;
        return this;
    }

    public EasyBLEBuilder setBondController(BondController bondController)
    {
        Inspector.requireNonNull(bondController, "bondController can't be null");
        this.bondController = bondController;
        return this;
    }

    public EasyBLEBuilder setMethodDefaultThreadMode(ThreadMode mode)
    {
        Inspector.requireNonNull(mode, "mode can't be null");
        methodDefaultThreadMode = mode;
        return this;
    }

    public EasyBLEBuilder setScanConfiguration(ScanConfiguration scanConfiguration)
    {
        Inspector.requireNonNull(scanConfiguration, "scanConfiguration can't be null");
        this.scanConfiguration = scanConfiguration;
        return this;
    }

    public EasyBLEBuilder setLogger(Logger logger)
    {
        Inspector.requireNonNull(logger, "logger can't be null");
        this.logger = logger;
        return this;
    }

    public EasyBLEBuilder setObservable(Observable observable)
    {
        Inspector.requireNonNull(observable, "observable can't be null");
        this.observable = observable;
        return this;
    }

    public EasyBLEBuilder setObserveAnnotationRequired(boolean observeAnnotationRequired)
    {
        isObserveAnnotationRequired = observeAnnotationRequired;
        return this;
    }

    public EasyBLE build()
    {
        synchronized(
            EasyBLE.class) {
            if (EasyBLE.instance != null)
            {
                throw new EasyBLEException ("EasyBLE instance already exists. It can only be instantiated once.");
            }
            EasyBLE.instance = new EasyBLE(this);
            return EasyBLE.instance;
        }
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\EasyBLEException.java =====

package com.topdon.ble;

public class EasyBLEException extends RuntimeException {
    private static final long serialVersionUID = -7775315841108791634L;

    public EasyBLEException (String message) {
        super(message);
    }

    public EasyBLEException (String message, Throwable cause) {
        super(message, cause);
    }

    public EasyBLEException (Throwable cause) {
        super(cause);
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\EventObserver.java =====

package com.topdon.ble;

import com.topdon.commons.observer.Observer;

import java.util.UUID;

public interface EventObserver extends Observer {

    default void onBluetoothAdapterStateChanged(int state) {
    }

    default void onCharacteristicRead(Request request, byte[] value) {
    }

    default void onCharacteristicChanged(
        Device device, UUID service, UUID characteristic,
        byte[] value
    ) {
    }

    default void onCharacteristicWrite(Request request, byte[] value) {
    }

    default void onRssiRead(Request request, int rssi) {
    }

    default void onDescriptorRead(Request request, byte[] value) {
    }

    default void onNotificationChanged(Request request, boolean isEnabled) {
    }

    default void onMtuChanged(Request request, int mtu) {
    }

    default void onPhyChange(Request request, int txPhy, int rxPhy) {
    }

    default void onRequestFailed(Request request, int failType, Object value) {
    }

    default void onConnectionStateChanged(Device device) {
    }

    default void onConnectFailed(Device device, int failType) {
    }

    default void onConnectTimeout(Device device, int type) {
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\GenericRequest.java =====

package com.topdon.ble;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.topdon.ble.callback.RequestCallback;

import java.util.Queue;
import java.util.UUID;

class GenericRequest implements Request, Comparable<GenericRequest> {
    private final String tag;
    Device device;
    RequestType type;
    UUID service;
    UUID characteristic;
    UUID descriptor;
    Object value;
    int priority;
    RequestCallback callback;
    WriteOptions writeOptions;
    byte[] descriptorTemp;//
    //---------    ---------
    Queue < byte[] > remainQueue;
    byte[] sendingBytes;
    //--------------------------------

    GenericRequest(RequestBuilder builder) {
        tag = builder.tag;
        type = builder.type;
        service = builder.service;
        characteristic = builder.characteristic;
        descriptor = builder.descriptor;
        priority = builder.priority;
        value = builder.value;
        callback = builder.callback;
        writeOptions = builder.writeOptions;
    }

    @Override
    public int compareTo(GenericRequest other) {
        return Integer.compare(other.priority, priority);
    }

    @NonNull
    public Device getDevice() {
        return device;
    }

    @NonNull
    public RequestType getType() {
        return type;
    }

    @Nullable
    public String getTag() {
        return tag;
    }

    @Nullable
    public UUID getService() {
        return service;
    }

    @Nullable
    public UUID getCharacteristic() {
        return characteristic;
    }

    @Nullable
    public UUID getDescriptor() {
        return descriptor;
    }

    @Override
    public void execute(Connection connection) {
        if (connection != null) {
            connection.execute(this);
        }
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\Inspector.java =====

package com.topdon.ble;

final class Inspector {

    static <T> T requireNonNull(T obj, String message)
    {
        if (obj == null)
            throw new EasyBLEException (message);
        return obj;
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\LegacyScanner.java =====

package com.topdon.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

import com.topdon.ble.util.Logger;

class LegacyScanner extends AbstractScanner implements BluetoothAdapter.LeScanCallback {

    LegacyScanner(EasyBLE easyBle, BluetoothAdapter bluetoothAdapter) {
        super(easyBle, bluetoothAdapter);
    }

    @Override
    protected boolean isReady() {
        return true;
    }

    @Override
    protected void performStartScan() {
        try {
            bluetoothAdapter.startLeScan(this);
        } catch (SecurityException e) {
            logger.log(
                android.util.Log.ERROR,
                Logger.TYPE_SCAN_STATE,
                "Missing Bluetooth permission for legacy scan: " + e.getMessage()
            );
        }
    }

    @Override
    protected void performStopScan() {
        try {
            bluetoothAdapter.stopLeScan(this);
        } catch (SecurityException e) {
            logger.log(
                android.util.Log.ERROR,
                Logger.TYPE_SCAN_STATE,
                "Missing Bluetooth permission to stop legacy scan: " + e.getMessage()
            );
        }
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        parseScanResult(device, false, null, rssi, scanRecord);
    }

    @NonNull
    @Override
    public ScannerType getType() {
        return ScannerType.LEGACY;
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\LeScanner.java =====

package com.topdon.ble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.topdon.ble.callback.ScanListener;
import com.topdon.ble.util.Logger;

class LeScanner extends AbstractScanner {
    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            parseScanResult(result.getDevice(), result);
        }

        @Override
        public void onScanFailed(int errorCode) {
            handleScanCallback(
                false,
                null,
                false,
                ScanListener.ERROR_SCAN_FAILED,
                "onScanFailed. errorCode = " + errorCode
            );
            logger.log(Log.ERROR, Logger.TYPE_SCAN_STATE, "onScanFailed. errorCode = " + errorCode);
            stopScan(true);
        }
    };
    private BluetoothLeScanner bleScanner;

    LeScanner(EasyBLE easyBle, BluetoothAdapter bluetoothAdapter) {
        super(easyBle, bluetoothAdapter);
    }

    private BluetoothLeScanner getLeScanner() {
        if (bleScanner == null) {
            //ï¼Œnull
            bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        }
        return bleScanner;
    }

    @Override
    protected boolean isReady() {
        return getLeScanner() != null;
    }

    @Override
    protected void performStartScan() {
        ScanSettings settings;
        if (configuration.scanSettings == null) {
            settings = new ScanSettings . Builder ()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .build();
        } else {
            settings = configuration.scanSettings;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Context context = getEasyBle ().getContext();
            if (context == null || ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                logger.log(Log.ERROR, Logger.TYPE_SCAN_STATE, "Missing BLUETOOTH_SCAN permission for LE scan");
                return;
            }
        }

        try {
            bleScanner.startScan(configuration.filters, settings, scanCallback);
        } catch (SecurityException e) {
            logger.log(
                Log.ERROR,
                Logger.TYPE_SCAN_STATE,
                "Missing Bluetooth permission to start LE scan: " + e.getMessage()
            );
        }
    }

    @Override
    protected void performStopScan() {
        if (bleScanner != null) {
            try {
                bleScanner.stopScan(scanCallback);
            } catch (SecurityException e) {
                logger.log(
                    android.util.Log.ERROR,
                    Logger.TYPE_SCAN_STATE,
                    "Missing Bluetooth permission to stop LE scan: " + e.getMessage()
                );
            }
        }
    }

    @NonNull
    @Override
    public ScannerType getType() {
        return ScannerType.LE;
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\MethodInfoGenerator.java =====

package com.topdon.ble;

import com.topdon.commons.poster.MethodInfo;

import java.util.UUID;

class MethodInfoGenerator {
    static MethodInfo onBluetoothAdapterStateChanged(int state)
    {
        return new MethodInfo ("onBluetoothAdapterStateChanged", new MethodInfo.Parameter(int.class, state));
    }

    static MethodInfo onConnectionStateChanged(Device device)
    {
        return new MethodInfo ("onConnectionStateChanged", new MethodInfo.Parameter(Device.class, device));
    }

    static MethodInfo onConnectFailed(Device device, int failType)
    {
        return new MethodInfo ("onConnectFailed", new MethodInfo.Parameter(Device.class, device),
        new MethodInfo . Parameter (int.class, failType));
    }

    static MethodInfo onConnectTimeout(Device device, int type)
    {
        return new MethodInfo ("onConnectTimeout", new MethodInfo.Parameter(Device.class, device),
        new MethodInfo . Parameter (int.class, type));
    }

    static MethodInfo onCharacteristicChanged(Device device, UUID service, UUID characteristic, byte[] value )
    {
        return new MethodInfo ("onCharacteristicChanged", new MethodInfo.Parameter(Device.class, device),
        new MethodInfo . Parameter (UUID.class, service), new MethodInfo.Parameter(UUID.class, characteristic),
        new MethodInfo . Parameter (byte[].class, value));
    }

    static MethodInfo onCharacteristicRead(Request request, byte[] value )
    {
        return new MethodInfo ("onCharacteristicRead", new MethodInfo.Parameter(Request.class, request),
        new MethodInfo . Parameter (byte[].class, value));
    }

    static MethodInfo onCharacteristicWrite(Request request, byte[] value )
    {
        return new MethodInfo ("onCharacteristicWrite", new MethodInfo.Parameter(Request.class, request),
        new MethodInfo . Parameter (byte[].class, value));
    }

    static MethodInfo onRssiRead(Request request, int rssi)
    {
        return new MethodInfo ("onRssiRead", new MethodInfo.Parameter(Request.class, request),
        new MethodInfo . Parameter (int.class, rssi));
    }

    static MethodInfo onDescriptorRead(Request request, byte[] value )
    {
        return new MethodInfo ("onDescriptorRead", new MethodInfo.Parameter(Request.class, request),
        new MethodInfo . Parameter (byte[].class, value));
    }

    static MethodInfo onNotificationChanged(Request request, boolean isEnabled)
    {
        return new MethodInfo ("onNotificationChanged", new MethodInfo.Parameter(Request.class, request),
        new MethodInfo . Parameter (boolean.class, isEnabled));
    }

    static MethodInfo onMtuChanged(Request request, int mtu)
    {
        return new MethodInfo ("onMtuChanged", new MethodInfo.Parameter(Request.class, request),
        new MethodInfo . Parameter (int.class, mtu));
    }

    static MethodInfo onPhyChange(Request request, int txPhy, int rxPhy)
    {
        return new MethodInfo ("onPhyChange", new MethodInfo.Parameter(Request.class, request),
        new MethodInfo . Parameter (int.class, txPhy), new MethodInfo.Parameter(int.class, rxPhy));
    }

    static MethodInfo onRequestFailed(Request request, int failType, Object value )
    {
        return new MethodInfo ("onRequestFailed", new MethodInfo.Parameter(Request.class, request),
        new MethodInfo . Parameter (int.class, failType), new MethodInfo.Parameter(Object.class, value));
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\RemoveBondFilter.java =====

package com.topdon.ble;

import android.bluetooth.BluetoothDevice;

public interface RemoveBondFilter {
    boolean accept(BluetoothDevice device);
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\Request.java =====

package com.topdon.ble;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.UUID;

public interface Request {

    @NonNull
    Device getDevice();

    @NonNull
    RequestType getType();

    @Nullable
    String getTag();

    @Nullable
    UUID getService();

    @Nullable
    UUID getCharacteristic();

    @Nullable
    UUID getDescriptor();

    void execute(Connection connection);
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\RequestBuilder.java =====

package com.topdon.ble;

import com.topdon.ble.callback.RequestCallback;

import java.util.UUID;

public class RequestBuilder<T extends RequestCallback> {
    String tag;
    RequestType type;
    UUID service;
    UUID characteristic;
    UUID descriptor;
    Object value;
    int priority;
    RequestCallback callback;
    WriteOptions writeOptions;

    RequestBuilder(RequestType type) {
        this.type = type;
    }

    public RequestBuilder < T > setTag (String tag) {
        this.tag = tag;
        return this;
    }

    public RequestBuilder < T > setPriority (int priority) {
        this.priority = priority;
        return this;
    }

    public RequestBuilder < T > setCallback (T callback) {
        this.callback = callback;
        return this;
    }

    public Request build() {
        return new GenericRequest (this);
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\RequestBuilderFactory.java =====

package com.topdon.ble;

import android.os.Build;

import androidx.annotation.IntRange;
import androidx.annotation.RequiresApi;

import com.topdon.ble.callback.*;

import java.util.UUID;

public class RequestBuilderFactory {

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public RequestBuilder<MtuChangeCallback> getChangeMtuBuilder(@IntRange(from = 23, to = 517) int mtu)
    {
        if (mtu < 23) {
            mtu = 23;
        } else if (mtu > 517) {
            mtu = 517;
        }
        RequestBuilder<MtuChangeCallback> builder = new RequestBuilder<>(RequestType.CHANGE_MTU);
        builder.value = mtu;
        return builder;
    }

    public RequestBuilder<ReadCharacteristicCallback> getReadCharacteristicBuilder(UUID service, UUID characteristic)
    {
        RequestBuilder<ReadCharacteristicCallback> builder = new RequestBuilder<>(RequestType.READ_CHARACTERISTIC);
        builder.service = service;
        builder.characteristic = characteristic;
        return builder;
    }

    public RequestBuilder<NotificationChangeCallback> getSetNotificationBuilder(UUID service, UUID characteristic,
    boolean enable)
    {
        RequestBuilder<NotificationChangeCallback> builder = new RequestBuilder<>(RequestType.SET_NOTIFICATION);
        builder.service = service;
        builder.characteristic = characteristic;
        builder.value = enable ? 1 : 0;
        return builder;
    }

    public RequestBuilder<NotificationChangeCallback> getSetIndicationBuilder(UUID service, UUID characteristic,
    boolean enable)
    {
        RequestBuilder<NotificationChangeCallback> builder = new RequestBuilder<>(RequestType.SET_INDICATION);
        builder.service = service;
        builder.characteristic = characteristic;
        builder.value = enable ? 1 : 0;
        return builder;
    }

    public RequestBuilder<NotificationChangeCallback> getReadDescriptorBuilder(UUID service, UUID characteristic,
    UUID descriptor)
    {
        RequestBuilder<NotificationChangeCallback> builder = new RequestBuilder<>(RequestType.READ_DESCRIPTOR);
        builder.service = service;
        builder.characteristic = characteristic;
        builder.descriptor = descriptor;
        return builder;
    }

    public WriteCharacteristicBuilder getWriteCharacteristicBuilder(UUID service, UUID characteristic,
    byte[] value )
    {
        Inspector.requireNonNull(value, "value can't be null");
        WriteCharacteristicBuilder builder = new WriteCharacteristicBuilder();
        builder.service = service;
        builder.characteristic = characteristic;
        builder.value = value;
        return builder;
    }

    public RequestBuilder<ReadRssiCallback> getReadRssiBuilder()
    {
        return new RequestBuilder < > (RequestType.READ_RSSI);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    public RequestBuilder<PhyChangeCallback> getReadPhyBuilder()
    {
        return new RequestBuilder < > (RequestType.READ_PHY);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    public RequestBuilder<PhyChangeCallback> getSetPreferredPhyBuilder(int txPhy, int rxPhy, int phyOptions)
    {
        RequestBuilder<PhyChangeCallback> builder = new RequestBuilder<>(RequestType.SET_PREFERRED_PHY);
        builder.value = new int []{ txPhy, rxPhy, phyOptions };
        return builder;
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\RequestType.java =====

package com.topdon.ble;

public enum RequestType {

    SET_NOTIFICATION,

    SET_INDICATION,

    READ_CHARACTERISTIC,

    READ_DESCRIPTOR,

    READ_RSSI,

    WRITE_CHARACTERISTIC,

    CHANGE_MTU,

    READ_PHY,

    SET_PREFERRED_PHY
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\ScanConfiguration.java =====

package com.topdon.ble;

import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.List;

public class ScanConfiguration {
    int scanPeriodMillis = 10000;
    boolean acceptSysConnectedDevice;
    ScanSettings scanSettings;
    boolean onlyAcceptBleDevice;
    int rssiLowLimit = -120;
    List<ScanFilter> filters;

    public int getScanPeriodMillis()
    {
        return scanPeriodMillis;
    }

    public ScanConfiguration setScanPeriodMillis(int scanPeriodMillis)
    {
        //1
        if (scanPeriodMillis >= 1000) {
            this.scanPeriodMillis = scanPeriodMillis;
        }
        return this;
    }

    public boolean isAcceptSysConnectedDevice()
    {
        return acceptSysConnectedDevice;
    }

    public ScanConfiguration setAcceptSysConnectedDevice(boolean acceptSysConnectedDevice)
    {
        this.acceptSysConnectedDevice = acceptSysConnectedDevice;
        return this;
    }

    public ScanSettings getScanSettings()
    {
        return scanSettings;
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public ScanConfiguration setScanSettings(ScanSettings scanSettings)
    {
        Inspector.requireNonNull(scanSettings, "scanSettings can't be null");
        this.scanSettings = scanSettings;
        return this;
    }

    public boolean isOnlyAcceptBleDevice()
    {
        return onlyAcceptBleDevice;
    }

    public ScanConfiguration setOnlyAcceptBleDevice(boolean onlyAcceptBleDevice)
    {
        this.onlyAcceptBleDevice = onlyAcceptBleDevice;
        return this;
    }

    public int getRssiLowLimit()
    {
        return rssiLowLimit;
    }

    public ScanConfiguration setRssiLowLimit(int rssiLowLimit)
    {
        this.rssiLowLimit = rssiLowLimit;
        return this;
    }

    public List<ScanFilter> getFilters()
    {
        return filters;
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public ScanConfiguration setFilters(List<ScanFilter> filters)
    {
        this.filters = filters;
        return this;
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\Scanner.java =====

package com.topdon.ble;

import android.content.Context;

import com.topdon.ble.callback.ScanListener;

interface Scanner {

    void addScanListener(ScanListener listener);

    void removeScanListener(ScanListener listener);

    void startScan(Context context);

    void stopScan(boolean quietly);

    boolean isScanning();

    void onBluetoothOff();

    void release();

    ScannerType getType();
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\ScannerType.java =====

package com.topdon.ble;

public enum ScannerType {

    LE,

    LEGACY,

    CLASSIC
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\util\DefaultLogger.java =====

package com.topdon.ble.util;

import android.util.Log;

public class DefaultLogger implements Logger {
    private final String tag;
    private boolean isEnabled;

    public DefaultLogger (String tag) {
        this.tag = tag;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    @Override
    public void log(int priority, int type, String msg) {
        if (isEnabled) {
            Log.println(priority, tag, msg);
        }
    }

    @Override
    public void log(int priority, int type, String msg, Throwable th) {
        if (isEnabled) {
            if (msg != null) {
                log(priority, type, msg + "\n" + Log.getStackTraceString(th));
            } else {
                log(priority, type, Log.getStackTraceString(th));
            }
        }
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\util\Logger.java =====

package com.topdon.ble.util;

public interface Logger {

    int TYPE_GENERAL = 0;

    int TYPE_SCAN_STATE = 1;

    int TYPE_CONNECTION_STATE = 2;

    int TYPE_CHARACTERISTIC_READ = 3;

    int TYPE_CHARACTERISTIC_CHANGED = 4;

    int TYPE_READ_REMOTE_RSSI = 5;

    int TYPE_MTU_CHANGED = 6;

    int TYPE_REQUEST_FAILED = 7;
    int TYPE_DESCRIPTOR_READ = 8;
    int TYPE_NOTIFICATION_CHANGED = 9;
    int TYPE_INDICATION_CHANGED = 10;
    int TYPE_CHARACTERISTIC_WRITE = 11;
    int TYPE_PHY_CHANGE = 12;

    void log(int priority, int type, String msg);

    void log(int priority, int type, String msg, Throwable th);

    boolean isEnabled();

    void setEnabled(boolean isEnabled);
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\WriteCharacteristicBuilder.java =====

package com.topdon.ble;

import com.topdon.ble.callback.WriteCharacteristicCallback;

public final class WriteCharacteristicBuilder extends RequestBuilder<WriteCharacteristicCallback> {
    WriteCharacteristicBuilder() {
        super(RequestType.WRITE_CHARACTERISTIC);
    }

    @Override
    public WriteCharacteristicBuilder setTag(String tag) {
        super.setTag(tag);
        return this;
    }

    @Override
    public WriteCharacteristicBuilder setPriority(int priority) {
        super.setPriority(priority);
        return this;
    }

    @Override
    public WriteCharacteristicBuilder setCallback(WriteCharacteristicCallback callback) {
        super.setCallback(callback);
        return this;
    }

    public WriteCharacteristicBuilder setWriteOptions(WriteOptions writeOptions) {
        this.writeOptions = writeOptions;
        return this;
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\WriteOptions.java =====

package com.topdon.ble;

import android.bluetooth.BluetoothGattCharacteristic;

public class WriteOptions {
    final int packageWriteDelayMillis;
    final int requestWriteDelayMillis;
    final boolean isWaitWriteResult;
    final int writeType;
    final boolean useMtuAsPackageSize;
    int packageSize;

    private WriteOptions(Builder builder)
    {
        packageWriteDelayMillis = builder.packageWriteDelayMillis;
        requestWriteDelayMillis = builder.requestWriteDelayMillis;
        packageSize = builder.packageSize;
        isWaitWriteResult = builder.isWaitWriteResult;
        writeType = builder.writeType;
        useMtuAsPackageSize = builder.useMtuAsPackageSize;
    }

    public int getPackageWriteDelayMillis()
    {
        return packageWriteDelayMillis;
    }

    public int getRequestWriteDelayMillis()
    {
        return requestWriteDelayMillis;
    }

    public int getPackageSize()
    {
        return packageSize;
    }

    public boolean isWaitWriteResult()
    {
        return isWaitWriteResult;
    }

    public int getWriteType()
    {
        return writeType;
    }

    public static
    class Builder {
        private int packageWriteDelayMillis = 0;
        private int requestWriteDelayMillis = -1;
        private int packageSize = 20;
        private boolean isWaitWriteResult = true;
        private int writeType = -1;
        private boolean useMtuAsPackageSize = false;

        public Builder setPackageWriteDelayMillis(int packageWriteDelayMillis)
        {
            this.packageWriteDelayMillis = packageWriteDelayMillis;
            return this;
        }

        public Builder setRequestWriteDelayMillis(int requestWriteDelayMillis)
        {
            this.requestWriteDelayMillis = requestWriteDelayMillis;
            return this;
        }

        public Builder setPackageSize(int packageSize)
        {
            if (packageSize > 0) {
                this.packageSize = packageSize;
            }
            return this;
        }

        public Builder setWaitWriteResult(boolean waitWriteResult)
        {
            isWaitWriteResult = waitWriteResult;
            return this;
        }

        public Builder setWriteType(int writeType)
        {
            if (writeType == BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT ||
                writeType == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE ||
                writeType == BluetoothGattCharacteristic.WRITE_TYPE_SIGNED
            ) {
                this.writeType = writeType;
            }
            return this;
        }

        public Builder setMtuAsPackageSize()
        {
            useMtuAsPackageSize = true;
            return this;
        }

        public WriteOptions build()
        {
            return new WriteOptions (this);
        }
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\base\AppHolder.java =====

package com.topdon.commons.base;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class AppHolder implements Application.ActivityLifecycleCallbacks {
    //Activity
    private final List<RunningActivity> runningActivities = new CopyOnWriteArrayList<>();
    //
    private boolean isCompleteExit = false;
    private Application application;
    private Looper mainLooper;
    private RunningActivity topActivity;

    private AppHolder () {
        mainLooper = Looper.getMainLooper();
        //application
        application = tryGetApplication();
        if (application != null) {
            application.registerActivityLifecycleCallbacks(this);
        }
    }

    @NonNull
    public static AppHolder getInstance () {
        return Holder.INSTANCE;
    }

    public static void initialize (@NonNull Application application) {
        Objects.requireNonNull(application, "application is null");
        //Applicationï¼Œ
        if (Holder.INSTANCE.application != null && Holder.INSTANCE.application != application) {
            Holder.INSTANCE.application.unregisterActivityLifecycleCallbacks(Holder.INSTANCE);
            application.registerActivityLifecycleCallbacks(Holder.INSTANCE);
        }
        Holder.INSTANCE.application = application;
    }

    @SuppressLint("PrivateApi")
    @Nullable
    private Application tryGetApplication() {
        try {
            Class<?> cls = Class . forName ("android.app.ActivityThread");
            Method catMethod = cls . getMethod ("currentActivityThread");
            catMethod.setAccessible(true);
            Object aThread = catMethod . invoke (null);
            Method method = aThread . getClass ().getMethod("getApplication");
            return (Application) method . invoke (aThread);
        } catch (Exception e) {
            return null;
        }
    }

    @CallSuper
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        RunningActivity a = new RunningActivity(activity.getClass().getName(), new WeakReference < > (activity));
        if (!runningActivities.contains(a)) {
            runningActivities.add(a);
        }
        topActivity = a;
    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @CallSuper
    @Override
    public void onActivityDestroyed(Activity activity) {
        if (runningActivities.isEmpty()) {
            topActivity = null;
        }
        RunningActivity a = new RunningActivity(activity.getClass().getName(), new WeakReference < > (activity));
        runningActivities.remove(a);
        if (isCompleteExit && runningActivities.isEmpty()) {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        }
    }

    public boolean isMainThread() {
        return Looper.myLooper() == mainLooper;
    }

    @NonNull
    public Looper getMainLooper() {
        if (mainLooper == null) {
            mainLooper = Looper.getMainLooper();
        }
        return mainLooper;
    }

    @NonNull
    public Context getContext() {
        Objects.requireNonNull(
            application,
            "The AppHolder has not been initialized, make sure to call AppHolder.initialize(app) first."
        );
        return application;
    }

    @Nullable
    public PackageInfo getPackageInfo() {
        try {
            PackageManager pm = application . getPackageManager ();
            return pm.getPackageInfo(application.getPackageName(), 0);
        } catch (Exception ignore) {
        }
        return null;
    }

    public boolean isAppOnForeground() {
        ActivityManager am =(ActivityManager) application . getSystemService (Context.ACTIVITY_SERVICE);
        if (am != null) {
            List<ActivityManager.RunningAppProcessInfo> processes = am . getRunningAppProcesses ();
            if (processes != null) {
                for (ActivityManager. RunningAppProcessInfo process : processes) {
                    if (application.getPackageName().equals(process.processName) &&
                        ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND == process.importance
                    ) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    //
    private boolean contains(Object[] array, Object obj) {
        if (array != null && array.length > 0) {
            for (Object o : array) {
                if (o.equals(obj)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void finish(String className, String... classNames) {
        List<RunningActivity> list = new ArrayList<>(runningActivities);
        Collections.reverse(list);//ï¼Œfinish
        for (RunningActivity runningActivity : list) {
        Activity activity = runningActivity . weakActivity . get ();
        if (activity != null) {
            String name = activity . getClass ().getName();
            if (name.equals(className) || contains(classNames, name)) {
                activity.finish();
            }
        }
    }
    }

    public void finishAllWithout(@Nullable String className, String... classNames) {
        List<RunningActivity> list = new ArrayList<>(runningActivities);
        Collections.reverse(list);//ï¼Œfinish
        for (RunningActivity runningActivity : list) {
        Activity activity = runningActivity . weakActivity . get ();
        if (activity != null) {
            String name = activity . getClass ().getName();
            if (!name.equals(className) && !contains(classNames, name)) {
                activity.finish();
            }
        }
    }
    }

    public void finishAll() {
        finishAllWithout(null);
    }

    public void backTo(String className) {
        List<RunningActivity> list = new ArrayList<>(runningActivities);
        Collections.reverse(list);//ï¼Œfinish
        for (RunningActivity runningActivity : list) {
        Activity activity = runningActivity . weakActivity . get ();
        if (activity != null) {
            String name = activity . getClass ().getName();
            if (name.equals(className)) {
                activity.finish();
                return;
            }
        }
    }
    }

    @Nullable
    public Activity getActivity(String className) {
        for (RunningActivity runningActivity : runningActivities) {
        if (runningActivity.name.equals(className)) {
            return runningActivity.weakActivity.get();
        }
    }
        return null;
    }

    public boolean isAllFinished() {
        return runningActivities.isEmpty();
    }

    public List < Activity > getAllActivities () {
        List<Activity> activities = new ArrayList<>();
        for (RunningActivity runningActivity : runningActivities) {
        Activity activity = runningActivity . weakActivity . get ();
        if (activity != null) {
            activities.add(activity);
        }
    }
        return activities;
    }

    public void completeExit() {
        isCompleteExit = true;
        List<RunningActivity> list = new ArrayList<>(runningActivities);
        Collections.reverse(list);//ï¼Œfinish
        for (RunningActivity runningActivity : list) {
        Activity activity = runningActivity . weakActivity . get ();
        if (activity != null) {
            activity.finish();
        }
    }
    }

    public Activity getTopActivity() {
        return topActivity == null ? null : topActivity.weakActivity.get();
    }

    private static final class Holder {
        private static final AppHolder INSTANCE = new AppHolder();
    }

    private static class RunningActivity {
        String name;
        WeakReference<Activity> weakActivity;

        RunningActivity(String name, WeakReference<Activity> weakActivity)
        {
            this.name = name;
            this.weakActivity = weakActivity;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (!(o instanceof RunningActivity)) return false;
            RunningActivity runningActivity =(RunningActivity) o;
            return name.equals(runningActivity.name);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(name);
        }
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\base\entity\AbstractTimer.java =====

package com.topdon.commons.base.entity;

import android.os.Handler;
import android.os.Looper;

import java.util.Timer;
import java.util.TimerTask;

public abstract class AbstractTimer {
    private final Handler handler;
    private final boolean callbackOnMainThread;
    private Timer timer;

    public AbstractTimer(boolean callbackOnMainThread)
    {
        handler = new Handler (Looper.getMainLooper());
        this.callbackOnMainThread = callbackOnMainThread;
    }

    public abstract void onTick();

    public synchronized final void start(long delay, long period)
    {
        if (timer == null) {
            timer = new Timer ();
            timer.schedule(new TimerTask () {
                @Override
                public void run() {
                    if (callbackOnMainThread) {
                        handler.post(new Runnable () {
                            @Override
                            public void run() {
                                onTick();
                            }
                        });
                    } else {
                        onTick();
                    }
                }
            }, delay, period);
        }
    }

    public synchronized final void stop()
    {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public boolean isRunning()
    {
        return timer != null;
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\base\entity\CheckableItem.java =====

package com.topdon.commons.base.entity;

import com.topdon.commons.base.interfaces.Checkable;

public class CheckableItem<T> implements Checkable<CheckableItem<T>> {
    private T data;
    private boolean isChecked;

    public CheckableItem () {
    }

    public CheckableItem (T data) {
        this.data = data;
    }

    public CheckableItem (T data, boolean isChecked) {
        this.data = data;
        this.isChecked = isChecked;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public boolean isChecked() {
        return isChecked;
    }

    @Override
    public CheckableItem < T > setChecked (boolean isChecked) {
        this.isChecked = isChecked;
        return this;
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\base\entity\CheckableParcelable.java =====

package com.topdon.commons.base.entity;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class CheckableParcelable<T extends Parcelable> extends CheckableItem<T> implements Parcelable {
    public static final Creator < CheckableParcelable > CREATOR = new Creator<CheckableParcelable>() {
        @Override
        public CheckableParcelable createFromParcel(Parcel source) {
            return new CheckableParcelable (source);
        }

        @Override
        public CheckableParcelable [] newArray (int size) {
            return new CheckableParcelable [size];
        }
    };

    public CheckableParcelable () {
    }

    public CheckableParcelable (T data) {
        super(data);
    }

    public CheckableParcelable (T data, boolean isChecked) {
        super(data, isChecked);
    }

    @SuppressWarnings("unchecked")
    protected CheckableParcelable (Parcel in) {
        Bundle bundle = in . readBundle (getClass().getClassLoader());
        if (bundle != null) {
            setData((T) bundle . getParcelable ("items"));
        }
        setChecked(in.readByte() != 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Bundle bundle = new Bundle();
        bundle.putParcelable("items", getData());
        dest.writeBundle(bundle);
        dest.writeByte(isChecked() ?(byte) 1 : (byte) 0);
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\base\entity\DatabaseContext.java =====

package com.topdon.commons.base.entity;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.Objects;

public class DatabaseContext extends ContextWrapper {
    private File dbDir;

    public DatabaseContext (Context base, @NonNull File dbDir) {
        super(base);
        Objects.requireNonNull(dbDir, "dbDir is null");
        this.dbDir = dbDir;
    }

    @Override
    public File getDatabasePath(String name) {
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }
        return new File (dbDir, name);
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(
        String name,
        int mode,
        SQLiteDatabase.CursorFactory factory,
        DatabaseErrorHandler errorHandler
    ) {
        return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), factory);
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
        return super.openOrCreateDatabase(getDatabasePath(name).getName(), mode, factory);
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\base\entity\UnitDBBean.java =====

package com.topdon.commons.base.entity;

import java.io.Serializable;

public class UnitDBBean implements Serializable {
//    {
//        "": "",
//            "": "m",
//            "": "",
//            "": "yd.",
//            "": "",
//            "": "1  = 1.094",
//            "": "1.094"
//    },

    private static final long serialVersionUID = -1L;
    public Long dbid;
    String LoginName;//
    int unitType;//0   1 
    String conversionRelation;//
    String preUnit;//
    String preName;//
    String afterUnit;//
    String afterName;//
    String conversionFormula;//
    String calcFactor;//

    public Long getDbid() {
        return dbid;
    }

    public void setDbid(Long dbid) {
        this.dbid = dbid;
    }

    public String getLoginName() {
        return LoginName;
    }

    public void setLoginName(String loginName) {
        LoginName = loginName;
    }

    public int getUnitType() {
        return unitType;
    }

    public void setUnitType(int unitType) {
        this.unitType = unitType;
    }

    public String getConversionRelation() {
        return conversionRelation;
    }

    public void setConversionRelation(String conversionRelation) {
        this.conversionRelation = conversionRelation;
    }

    public String getPreUnit() {
        return preUnit;
    }

    public void setPreUnit(String preUnit) {
        this.preUnit = preUnit;
    }

    public String getPreName() {
        return preName;
    }

    public void setPreName(String preName) {
        this.preName = preName;
    }

    public String getAfterUnit() {
        return afterUnit;
    }

    public void setAfterUnit(String afterUnit) {
        this.afterUnit = afterUnit;
    }

    public String getAfterName() {
        return afterName;
    }

    public void setAfterName(String afterName) {
        this.afterName = afterName;
    }

    public String getConversionFormula() {
        return conversionFormula;
    }

    public void setConversionFormula(String conversionFormula) {
        this.conversionFormula = conversionFormula;
    }

    public String getCalcFactor() {
        return calcFactor;
    }

    public void setCalcFactor(String calcFactor) {
        this.calcFactor = calcFactor;
    }

}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\base\interfaces\Callback.java =====

package com.topdon.commons.base.interfaces;

public interface Callback<T> {
    void onCallback(T obj);
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\base\interfaces\Checkable.java =====

package com.topdon.commons.base.interfaces;

public interface Checkable<T> {
    boolean isChecked();

    T setChecked(boolean isChecked);
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\base\interfaces\DrawableBuilder.java =====

package com.topdon.commons.base.interfaces;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

public interface DrawableBuilder {
    @NonNull
    Drawable build();
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\base\interfaces\IText.java =====

package com.topdon.commons.base.interfaces;

import androidx.annotation.NonNull;

public interface IText {
    @NonNull
    String getText();
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\base\interfaces\IWeight.java =====

package com.topdon.commons.base.interfaces;

public interface IWeight {

    Integer getWeight();
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\base\SaveBean.java =====

package com.topdon.commons.base;

public class SaveBean {

    public String type;
    public String mac;
    public String name;

    public SaveBean(String type, String mac, String name)
    {
        this.type = type;
        this.mac = mac;
        this.name = name;
    }

    public SaveBean()
    {
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getMac()
    {
        return mac;
    }

    public void setMac(String mac)
    {
        this.mac = mac;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\BleObserver.java =====

package com.topdon.commons;

import com.topdon.ble.EventObserver;

public interface BleObserver extends EventObserver {
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\helper\PermissionsRequester.java =====

package com.topdon.commons.helper;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class PermissionsRequester {
    private static final int PERMISSION_REQUEST_CODE = 10;
    private static final int REQUEST_CODE_WRITE_SETTINGS = 11;
    private static final int REQUEST_CODE_UNKNOWN_APP_SOURCES = 12;

    private final List<String> allPermissions = new ArrayList<>();
    private final List<String> refusedPermissions = new ArrayList<>();
    private Callback callback;
    private Activity activity;
    private Fragment fragment;
    private boolean checking;

    public PermissionsRequester(@NonNull Activity activity)
    {
        this.activity = activity;
    }

    public PermissionsRequester(@NonNull Fragment fragment)
    {
        this.fragment = fragment;
    }

    public void setCallback(Callback callback)
    {
        this.callback = callback;
    }

    public void checkAndRequest(@NonNull List<String> permissions)
    {
        if (checking) {
            return;
        }
        refusedPermissions.clear();
        allPermissions.clear();
        allPermissions.addAll(permissions);
        checkPermissions(allPermissions, false);
    }

    public boolean hasPermissions(@NonNull List<String> permissions)
    {
        return checkPermissions(permissions, true);
    }

    @SuppressWarnings("all")
    private boolean checkPermissions(List<String> permissions, boolean onlyCheck)
    {
        Context context = activity != null ? activity : fragment.getContext();
        if (context == null) return false;
        if (permissions.remove(Manifest.permission.WRITE_SETTINGS) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(context)) {
                if (!onlyCheck) {
                    Intent intent = new Intent(
                        Settings.ACTION_MANAGE_WRITE_SETTINGS,
                        Uri.parse("package:" + context.getPackageName())
                    );
                    if (activity != null) {
                        activity.startActivityForResult(intent, REQUEST_CODE_WRITE_SETTINGS);
                    } else {
                        fragment.startActivityForResult(intent, REQUEST_CODE_WRITE_SETTINGS);
                    }
                    checking = true;
                }
                return false;
            }
        }
        if (permissions.remove(Manifest.permission.REQUEST_INSTALL_PACKAGES) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.getPackageManager().canRequestPackageInstalls()) {
                if (!onlyCheck) {
                    Intent intent = new Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + context.getPackageName())
                    );
                    if (activity != null) {
                        activity.startActivityForResult(intent, REQUEST_CODE_UNKNOWN_APP_SOURCES);
                    } else {
                        fragment.startActivityForResult(intent, REQUEST_CODE_UNKNOWN_APP_SOURCES);
                    }
                    checking = true;
                }
                return false;
            }
        }
        List<String> needRequestPermissonList = findDeniedPermissions (permissions);
        if (onlyCheck) {
            return needRequestPermissonList.isEmpty();
        } else if (!needRequestPermissonList.isEmpty()) {
            if (activity != null) {
                ActivityCompat.requestPermissions(
                    activity,
                    needRequestPermissonList.toArray(new String [0]),
                    PERMISSION_REQUEST_CODE
                );
            } else {
                fragment.requestPermissions(needRequestPermissonList.toArray(new String [0]), PERMISSION_REQUEST_CODE);
            }
            checking = true;
            return false;
        } else {
            if (callback != null) {
                callback.onRequestResult(refusedPermissions);
            }
            checking = false;
            return true;
        }
    }

    //
    private List<String> findDeniedPermissions(List<String> permissions)
    {
        List<String> needRequestPermissionList = new ArrayList<>();
        Activity activity = this.activity != null ? this.activity : fragment.getActivity();
        if (activity != null) {
            for (String perm : permissions) {
                if (ContextCompat.checkSelfPermission(activity, perm) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, perm)
                ) {
                    needRequestPermissionList.add(perm);
                }
            }
        }
        return needRequestPermissionList;
    }

    public void onActivityResult(int requestCode)
    {
        Context context = activity != null ? activity : fragment.getContext();
        if (context == null) return;
        if (requestCode == REQUEST_CODE_WRITE_SETTINGS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(context)) {
                refusedPermissions.add(Manifest.permission.WRITE_SETTINGS);
            }
            checkPermissions(allPermissions, false);
        }
        if (requestCode == REQUEST_CODE_UNKNOWN_APP_SOURCES && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.getPackageManager().canRequestPackageInstalls()) {
                refusedPermissions.add(Manifest.permission.REQUEST_INSTALL_PACKAGES);
            }
            checkPermissions(allPermissions, false);
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions [i];
                if (allPermissions.remove(permission) && grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    refusedPermissions.add(permission);
                }
            }
            if (callback != null) {
                callback.onRequestResult(refusedPermissions);
            }
            checking = false;
        }
    }

    public interface Callback {

        void onRequestResult(List<String> refusedPermissions);
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\MyEvent.java =====

package com.topdon.commons;

public class MyEvent {
    public String msg;

    public MyEvent(String msg)
    {
        this.msg = msg;
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\observer\Observable.java =====

package com.topdon.commons.observer;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.topdon.commons.poster.MethodInfo;
import com.topdon.commons.poster.PosterDispatcher;

import java.lang.reflect.Method;
import java.util.*;

public final class Observable {
    private final List<ObserverInfo> observerInfos = new ArrayList<>();
    private final PosterDispatcher posterDispatcher;
    private final ObserverMethodHelper helper;

    public Observable(@NonNull PosterDispatcher posterDispatcher, boolean isObserveAnnotationRequired)
    {
        this.posterDispatcher = posterDispatcher;
        helper = new ObserverMethodHelper (isObserveAnnotationRequired);
    }

    public PosterDispatcher getPosterDispatcher()
    {
        return posterDispatcher;
    }

    public void registerObserver(@NonNull Observer observer)
    {
        Objects.requireNonNull(observer, "observer can't be null");
        synchronized(observerInfos) {
            boolean registered = false;
            for (Iterator< ObserverInfo > it = observerInfos.iterator(); it.hasNext(); ) {
            ObserverInfo info = it . next ();
            Observer o = info . weakObserver . get ();
            if (o == null) {
                it.remove();
            } else if (o == observer) {
                registered = true;
            }
        }
            if (registered) {
                Log.e("Observable", "", new Error ("Observer " + observer + " is already registered."));
                return;
            }
            Map<String, Method> methodMap = helper . findObserverMethod (observer);
            observerInfos.add(new ObserverInfo (observer, methodMap));
        }
    }

    public boolean isRegistered(@NonNull Observer observer)
    {
        synchronized(observerInfos) {
            for (ObserverInfo info : observerInfos) {
            if (info.weakObserver.get() == observer) {
                return true;
            }
        }
            return false;
        }
    }

    public void unregisterObserver(@NonNull Observer observer)
    {
        synchronized(observerInfos) {
            for (Iterator< ObserverInfo > it = observerInfos.iterator(); it.hasNext(); ) {
            ObserverInfo info = it . next ();
            Observer o = info . weakObserver . get ();
            if (o == null || observer == o) {
                it.remove();
            }
        }
        }
    }

    public void unregisterAll()
    {
        synchronized(observerInfos) {
            observerInfos.clear();
        }
        helper.clearCache();
    }

    private List<ObserverInfo> getObserverInfos()
    {
        synchronized(observerInfos) {
            ArrayList<ObserverInfo> infos = new ArrayList<>();
            for (ObserverInfo info : observerInfos) {
            Observer observer = info . weakObserver . get ();
            if (observer != null) {
                infos.add(info);
            }
        }
            return infos;
        }
    }

    public void notifyObservers(@NonNull String methodName, @Nullable MethodInfo.Parameter... parameters)
    {
        notifyObservers(new MethodInfo (methodName, parameters));
    }

    public void notifyObservers(@NonNull MethodInfo info)
    {
        List<ObserverInfo> infos = getObserverInfos ();
        for (ObserverInfo oi : infos) {
        Observer observer = oi . weakObserver . get ();
        if (observer != null) {
            String key = helper . generateKey (info.getTag(), info.getName(), info.getParameterTypes());
            Method method = oi . methodMap . get (key);
            if (method != null) {
                Runnable runnable = helper . generateRunnable (observer, method, info);
                posterDispatcher.post(method, runnable);
            }
        }
    }
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\observer\Observe.java =====

package com.topdon.commons.observer;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @ interface Observe {
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\observer\Observer.java =====

package com.topdon.commons.observer;

public interface Observer {

    @Observe
    default void onChanged(Object o)
    {
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\observer\ObserverInfo.java =====

package com.topdon.commons.observer;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Map;

class ObserverInfo {
    final WeakReference<Observer> weakObserver;
    final Map<String, Method> methodMap;

    ObserverInfo(Observer observer, Map<String, Method> methodMap)
    {
        weakObserver = new WeakReference < > (observer);
        this.methodMap = methodMap;
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\observer\ObserverMethodHelper.java =====

package com.topdon.commons.observer;

import com.topdon.commons.poster.MethodInfo;
import com.topdon.commons.poster.Tag;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class ObserverMethodHelper {
    private static final Map<Class<?>, Map<String, Method>> METHOD_CACHE = new ConcurrentHashMap<>();
    private boolean isObserveAnnotationRequired;

    ObserverMethodHelper(boolean isObserveAnnotationRequired)
    {
        this.isObserveAnnotationRequired = isObserveAnnotationRequired;
    }

    private static boolean contains(List<Method> methods, Method method)
    {
        for (Method m : methods) {
        if (m.getName().equals(method.getName()) && m.getReturnType().equals(method.getReturnType()) &&
            equalParamTypes(m.getParameterTypes(), method.getParameterTypes())
        ) {
            return true;
        }
    }
        return false;
    }

    private static boolean equalParamTypes(Class<?>[] params1, Class<?>[] params2)
    {
        if (params1.length == params2.length) {
            for (int i = 0; i < params1.length; i++) {
                if (params1[i] != params2[i])
                    return false;
            }
            return true;
        }
        return false;
    }

    void clearCache()
    {
        METHOD_CACHE.clear();
    }

    Runnable generateRunnable(Observer observer, Method method, MethodInfo info)
    {
        MethodInfo.Parameter[] parameters = info . getParameters ();
        if (parameters == null || parameters.length == 0) {
            return () -> {
                try {
                    method.invoke(observer);
                } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
            };
        } else {
            final Object [] params = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                MethodInfo.Parameter parameter = parameters [i];
                params[i] = parameter.getValue();
            }
            return () -> {
                try {
                    method.invoke(observer, params);
                } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
            };
        }
    }

    String generateKey(String tag, String name, Class<?>[] paramTypes)
    {
        StringBuilder sb = new StringBuilder();
        if (tag.isEmpty()) {
            sb.append(name);
        } else {
            sb.append(tag);
        }
        for (Class<?> type : paramTypes) {
        sb.append(",").append(type);
    }
        return sb.toString();
    }

    Map<String, Method> findObserverMethod(Observer observer)
    {
        Map<String, Method> map = METHOD_CACHE . get (observer.getClass());
        if (map != null) {
            return map;
        }
        map = new HashMap < > ();
        List<Method> methods = new ArrayList<>();
        Class<?> cls = observer . getClass ();
        while (cls != null && !cls.isInterface() && Observer.class. isAssignableFrom (cls)) {
            Method[] ms = null;
            try {
                ms = cls.getDeclaredMethods();
            } catch (Throwable ignore) {
            }
            if (ms != null) {
                for (Method m : ms) {
                    int ignore = Modifier . ABSTRACT | Modifier . STATIC | 0x40 | 0x1000;
                    if ((m.getModifiers() & Modifier.PUBLIC) != 0 && (m.getModifiers() & ignore) == 0 && !contains(methods, m)) {
                    methods.add(m);
                }
                }
            }
            cls = cls.getSuperclass();
        }
        for (Method method : methods) {
        Observe anno = method . getAnnotation (Observe.class);
        if (anno != null || !isObserveAnnotationRequired) {
            Tag tagAnno = method . getAnnotation (Tag.class);
            String tag = tagAnno == null ? "" : tagAnno.value();
            String key = generateKey (tag, method.getName(), method.getParameterTypes());
            map.put(key, method);
        }
    }
        if (!map.isEmpty()) {
            METHOD_CACHE.put(observer.getClass(), map);
        }
        return map;
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\poster\AsyncPoster.java =====

package com.topdon.commons.poster;

import androidx.annotation.NonNull;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

final class AsyncPoster implements Runnable, Poster {
    private final ExecutorService executorService;
    private final Queue<Runnable> queue;

    AsyncPoster(@NonNull ExecutorService executorService) {
        this.executorService = executorService;
        queue = new ConcurrentLinkedQueue < > ();
    }

    @Override
    public void enqueue(@NonNull Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable is null, cannot be enqueued");
        queue.add(runnable);
        executorService.execute(this);
    }

    @Override
    public void clear() {
        synchronized(this) {
            queue.clear();
        }
    }

    @Override
    public void run() {
        Runnable runnable = queue . poll ();
        if (runnable != null) {
            runnable.run();
        }
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\poster\BackgroundPoster.java =====

package com.topdon.commons.poster;

import androidx.annotation.NonNull;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

final class BackgroundPoster implements Runnable, Poster {
    private final ExecutorService executorService;
    private final Queue<Runnable> queue;
    private volatile boolean executorRunning;

    BackgroundPoster(@NonNull ExecutorService executorService) {
        this.executorService = executorService;
        queue = new ConcurrentLinkedQueue < > ();
    }

    @Override
    public void enqueue(@NonNull Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable is null, cannot be enqueued");
        synchronized(this) {
            queue.add(runnable);
            if (!executorRunning) {
                executorRunning = true;
                executorService.execute(this);
            }
        }
    }

    @Override
    public void clear() {
        synchronized(this) {
            queue.clear();
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                Runnable runnable = queue . poll ();
                if (runnable == null) {
                    synchronized(this) {
                        runnable = queue.poll();
                        if (runnable == null) {
                            executorRunning = false;
                            return;
                        }
                    }
                }
                runnable.run();
            }
        } finally {
            executorRunning = false;
        }
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\poster\MainThreadPoster.java =====

package com.topdon.commons.poster;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

final class MainThreadPoster extends Handler implements Poster {
    private final Queue<Runnable> queue;
    private boolean handlerActive;

    MainThreadPoster() {
        super(Looper.getMainLooper());
        queue = new ConcurrentLinkedQueue < > ();
    }

    @Override
    public void enqueue(@NonNull Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable is null, cannot be enqueued");
        synchronized(this) {
            queue.add(runnable);
            if (!handlerActive) {
                handlerActive = true;
                if (!sendMessage(obtainMessage())) {
                    throw new RuntimeException ("Could not send handler message");
                }
            }
        }
    }

    @Override
    public void clear() {
        synchronized(this) {
            queue.clear();
        }
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            while (true) {
                Runnable runnable = queue . poll ();
                if (runnable == null) {
                    synchronized(this) {
                        runnable = queue.poll();
                        if (runnable == null) {
                            handlerActive = false;
                            return;
                        }
                    }
                }
                runnable.run();
            }
        } finally {
            handlerActive = false;
        }
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\poster\MethodInfo.java =====

package com.topdon.commons.poster;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Method;

public class MethodInfo {
    @NonNull
    private String name;
    @Nullable
    private Parameter[] parameters;
    @NonNull
    private String tag;

    public MethodInfo(@NonNull String name, @Nullable Parameter... parameters)
    {
        this(name, name, parameters);
    }

    public MethodInfo(@NonNull String name, @NonNull String tag, @Nullable Parameter... parameters)
    {
        this.name = name;
        this.tag = tag;
        this.parameters = parameters;
    }

    public MethodInfo(@NonNull String name, @Nullable Class<?>[] parameterTypes)
    {
        this(name, name, parameterTypes);
    }

    public MethodInfo(@NonNull String name, @NonNull String tag, @Nullable Class<?>[] parameterTypes)
    {
        this(name, tag, toParameters(parameterTypes));
    }

    public static MethodInfo valueOf(@NonNull Method method)
    {
        Tag annotation = method . getAnnotation (Tag.class);
        return new MethodInfo (method.getName(), annotation == null ? method.getName() : annotation.value(),
        method.getParameterTypes());
    }

    private static Parameter[] toParameters(Class<?>[] parameterTypes)
    {
        Parameter[] parameters = null;
        if (parameterTypes != null) {
            parameters = new Parameter [parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                parameters[i] = new Parameter (parameterTypes[i], null);
            }
        }
        return parameters;
    }

    @NonNull
    public String getName()
    {
        return name;
    }

    public void setName(@NonNull String name)
    {
        this.name = name;
    }

    @NonNull
    public String getTag()
    {
        return tag;
    }

    public void setTag(@NonNull String tag)
    {
        this.tag = tag;
    }

    @Nullable
    public Parameter[] getParameters()
    {
        return parameters;
    }

    public void setParameters(@Nullable Parameter[] parameters)
    {
        this.parameters = parameters;
    }

    @Nullable
    public Class<?>[] getParameterTypes()
    {
        if (parameters == null) {
            return null;
        } else {
            Class<?>[] types = new Class[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                types[i] = parameters[i].type;
            }
            return types;
        }
    }

    @Nullable
    public Object[] getParameterValues()
    {
        if (parameters == null) {
            return null;
        } else {
            Object[] values = new Class[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                values[i] = parameters[i].value;
            }
            return values;
        }
    }

    public static
    class Parameter {
        @Nullable
        private Object value ;
        @NonNull
        private Class<?> type;

        public Parameter(@NonNull Class<?> type, @Nullable Object value )
        {
            this.type = type;
            this.value = value;
        }

        @Nullable
        public Object getValue()
        {
            return value;
        }

        public void setValue(@Nullable Object value )
        {
            this.value = value;
        }

        @NonNull
        public Class<?> getType()
        {
            return type;
        }

        public void setType(@NonNull Class<?> type)
        {
            this.type = type;
        }
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\poster\Poster.java =====

package com.topdon.commons.poster;

import androidx.annotation.NonNull;

interface Poster {

    void enqueue(@NonNull Runnable runnable);

    void clear();
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\poster\PosterDispatcher.java =====

package com.topdon.commons.poster;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;

public class PosterDispatcher {
    private final ThreadMode defaultMode;
    private final Poster backgroundPoster;
    private final Poster mainThreadPoster;
    private final ExecutorService executorService;
    private final Poster asyncPoster;

    public PosterDispatcher(@NonNull ExecutorService executorService, @NonNull ThreadMode defaultMode)
    {
        this.defaultMode = defaultMode;
        this.executorService = executorService;
        backgroundPoster = new BackgroundPoster (executorService);
        mainThreadPoster = new MainThreadPoster ();
        asyncPoster = new AsyncPoster (executorService);
    }

    public ThreadMode getDefaultMode()
    {
        return defaultMode;
    }

    public ExecutorService getExecutorService()
    {
        return executorService;
    }

    public void clearTasks()
    {
        backgroundPoster.clear();
        mainThreadPoster.clear();
        asyncPoster.clear();
    }

    public void shutdown()
    {
        clearTasks();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public void post(@Nullable Method method, @NonNull Runnable runnable)
    {
        if (method != null) {
            RunOn annotation = method . getAnnotation (RunOn.class);
            ThreadMode mode = defaultMode;
            if (annotation != null) {
                mode = annotation.value();
            }
            post(mode, runnable);
        }
    }

    public void post(@NonNull ThreadMode mode, @NonNull Runnable runnable)
    {
        if (mode == ThreadMode.UNSPECIFIED) {
            mode = defaultMode;
        }
        switch(mode) {
            case MAIN :
            mainThreadPoster.enqueue(runnable);
            break;
            case POSTING :
            runnable.run();
            break;
            case BACKGROUND :
            backgroundPoster.enqueue(runnable);
            break;
            case ASYNC :
            asyncPoster.enqueue(runnable);
            break;
        }
    }

    public void post(@NonNull Object owner, @NonNull String methodName, @NonNull String tag,
    @Nullable MethodInfo.Parameter... parameters)
    {
        Class<?>[] classes = new Class[0];
        Object[] params = new Object[0];
        if (parameters != null) {
            params = new Object [parameters.length];
            classes = new Class [parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                MethodInfo.Parameter parameter = parameters [i];
                classes[i] = parameter.getType();
                params[i] = parameter.getValue();
            }
        }
        Method[] methods = owner . getClass ().getDeclaredMethods();
        Method tm = null;
        Method mm = null;
        for (Method method : methods) {
        Tag annotation = method . getAnnotation (Tag.class);
        if (annotation != null && !annotation.value().isEmpty() && annotation.value().equals(tag) &&
            equalParamTypes(method.getParameterTypes(), classes)
        ) {
            tm = method;
        }
        if (tm == null) {
            if (method.getName().equals(methodName) && equalParamTypes(method.getParameterTypes(), classes)) {
                mm = method;
            }
        } else {
            break;
        }
    }
        Method method = tm == null ? mm : tm;
        if (method == null) {
            return;
        }
        try {
            Object[] finalParams = params;
            post(method, () -> {
                try {
                    method.invoke(owner, finalParams);
                } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
            });
        } catch (Exception ignore) {
        }
    }

    private boolean equalParamTypes(Class<?>[] params1, Class<?>[] params2)
    {
        if (params1.length == params2.length) {
            for (int i = 0; i < params1.length; i++) {
                if (params1[i] != params2[i])
                    return false;
            }
            return true;
        }
        return false;
    }

    public void post(@NonNull final Object owner, @NonNull String methodName, @Nullable MethodInfo.Parameter... parameters)
    {
        post(owner, methodName, "", parameters);
    }

    public void post(@NonNull Object owner, @NonNull MethodInfo methodInfo)
    {
        post(owner, methodInfo.getName(), methodInfo.getTag(), methodInfo.getParameters());
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\poster\RunOn.java =====

package com.topdon.commons.poster;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @ interface RunOn {

    ThreadMode value () default ThreadMode.UNSPECIFIED;
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\poster\Tag.java =====

package com.topdon.commons.poster;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @ interface Tag {
    String value () default "";
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\poster\ThreadMode.java =====

package com.topdon.commons.poster;

public enum ThreadMode {

    POSTING,

    MAIN,

    BACKGROUND,

    ASYNC,

    UNSPECIFIED
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\util\DiagnoseEventBusBean.java =====

package com.topdon.commons.util;

public class DiagnoseEventBusBean {
    private int what;//1   2 sn  3 4   5 Folder sn   6 diagMenuMask
    private String language;
    private boolean snConnection;// true sn  false
    private boolean isDiagnose;// true   false
    private long mDiagEntryType;//
    private long mDiagMenuMask;//
    private String snPath;//sn

    public String getSnPath()
    {
        return snPath;
    }

    public void setSnPath(String snPath)
    {
        this.snPath = snPath;
    }

    public int getWhat()
    {
        return what;
    }

    public void setWhat(int what)
    {
        this.what = what;
    }

    public String getLanguage()
    {
        return language;
    }

    public void setLanguage(String language)
    {
        this.language = language;
    }

    public boolean isSnConnection()
    {
        return snConnection;
    }

    public void setSnConnection(boolean snConnection)
    {
        this.snConnection = snConnection;
    }

    public boolean isDiagnose()
    {
        return isDiagnose;
    }

    public void setDiagnose(boolean diagnose)
    {
        isDiagnose = diagnose;
    }

    public long getmDiagEntryType()
    {
        return mDiagEntryType;
    }

    public void setmDiagEntryType(long mDiagEntryType)
    {
        this.mDiagEntryType = mDiagEntryType;
    }

    public long getDiagMenuMask()
    {
        return mDiagMenuMask;
    }

    public void setDiagMenuMask(long diagMenuMask)
    {
        mDiagMenuMask = diagMenuMask;
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\util\LLog.java =====

package com.topdon.commons.util;

import android.util.Log;

import com.elvishew.xlog.XLog;

public class LLog {

    public final static int MAX_LENGTH = 2000;
    private static boolean isDebug = true; // Simplified for now

    public static void d(String tag, String value )
    {
        XLog.tag(tag).d(value);
//        if (isDebug) {
//            Log.d(tag, value);
//        }
    }

    public static void i(String tag, String value )
    {
        XLog.tag(tag).i(value);
//        if (isDebug) {
//            Log.i(tag, value);
//        }
    }

    public static void w(String tag, String value )
    {
        XLog.tag(tag).w(value);
//        if (isDebug) {
//            Log.w(tag, value);
//        }
    }

    public static void e(String tag, String value )
    {
        XLog.tag(tag).e(value);
//        if (isDebug) {
//            Log.e(tag, value);
//        }
    }

    public static void LogMaxPrint(String tag, String msg)
    {
        if (msg.length() > MAX_LENGTH) {
            int length = MAX_LENGTH +1;
            String remain = msg;
            int index = 0;
            while (length > MAX_LENGTH) {
                index++;
                Log.v(tag + "[" + index + "]", " \n" + remain.substring(0, MAX_LENGTH));
                remain = remain.substring(MAX_LENGTH);
                length = remain.length();
            }
            if (length <= MAX_LENGTH) {
                index++;
                Log.v(tag + "[" + index + "]", " \n" + remain);
            }
        } else {
            Log.v(tag, msg);
        }
    }

}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\util\Topdon.java =====

package com.topdon.commons.util;

import android.content.Context;

public class Topdon {
    private static Context app;

    public static void init(Context context)
    {
        app = context;
    }

    public static Context getApp()
    {
        return app;
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\util\UnicodeReader.java =====

package com.topdon.commons.util;

import java.io.*;

public class UnicodeReader extends Reader {
    private static final int BOM_SIZE = 4;
    PushbackInputStream internalIn;
    InputStreamReader internalIn2 = null;
    String defaultEnc;

    UnicodeReader(InputStream in, String defaultEnc) {
        internalIn = new PushbackInputStream ( in, BOM_SIZE);
        this.defaultEnc = defaultEnc;
    }

    public String getDefaultEncoding() {
        return defaultEnc;
    }

    public String getEncoding() {
        if (internalIn2 == null)
            return null;
        return internalIn2.getEncoding();
    }

    protected void init() throws IOException {
        if (internalIn2 != null)
            return;

        String encoding;
        byte bom [] = new byte [BOM_SIZE];
        int n, unread;
        n = internalIn.read(bom, 0, bom.length);

        if ((bom[0] == (byte) 0x00) && (bom[1] == (byte) 0x00)
        && (bom[2] == (byte) 0xFE) && (bom[3] == (byte) 0xFF)) {
        encoding = "UTF-32BE";
        unread = n - 4;
    } else if ((bom[0] == (byte) 0xFF) && (bom[1] == (byte) 0xFE)
        && (bom[2] == (byte) 0x00) && (bom[3] == (byte) 0x00)) {
        encoding = "UTF-32LE";
        unread = n - 4;
    } else if ((bom[0] == (byte) 0xEF) && (bom[1] == (byte) 0xBB)
        && (bom[2] == (byte) 0xBF)) {
        encoding = "UTF-8";
        unread = n - 3;
    } else if ((bom[0] == (byte) 0xFE) && (bom[1] == (byte) 0xFF)) {
        encoding = "UTF-16BE";
        unread = n - 2;
    } else if ((bom[0] == (byte) 0xFF) && (bom[1] == (byte) 0xFE)) {
        encoding = "UTF-16LE";
        unread = n - 2;
    } else {
        // Unicode BOM mark not found, unread all bytes
        encoding = defaultEnc;
        unread = n;
    }
        // System.out.println("read=" + n + ", unread=" + unread);

        if (unread > 0)
            internalIn.unread(bom, (n - unread), unread);

        // Use given encoding
        if (encoding == null) {
            internalIn2 = new InputStreamReader (internalIn);
        } else {
            internalIn2 = new InputStreamReader (internalIn, encoding);
        }
    }

    public void close() throws IOException {
        init();
        internalIn2.close();
    }

    public int read(char[] cbuf, int off, int len) throws IOException {
        init();
        return internalIn2.read(cbuf, off, len);
    }

}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\util\WeakReferenceHandler.java =====

package com.topdon.commons.util;

import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;

public class WeakReferenceHandler<T> extends Handler {

    private final WeakReference<T> mReference;

    public WeakReferenceHandler (T referencedObject) {
        mReference = new WeakReference < T >(referencedObject);
    }

    public WeakReferenceHandler (Looper looper, T referencedObject) {
        super(looper);
        mReference = new WeakReference < T >(referencedObject);
    }

    protected T getReferencedObject() {
        return mReference.get();
    }

}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\UUIDManager.java =====

package com.topdon.commons;

public class UUIDManager {

    public static final String SERVICE_UUID = "00010203-0405-0607-0809-0a0b0c0d1910";//"00010203-0405-0607-0809-0A0B0C0D1910";//

    public static final String NOTIFY_UUID = "00010203-0405-0607-0809-0a0b0c0d2b10";

    public static final String WRITE_UUID = "00010203-0405-0607-0809-0a0b0c0d2b11";//"00010203-0405-0607-0809-0A0B0C0D2B11";//

    public static final String READ_UUID = "00010203-0405-0607-0809-0a0b0c0d2b10";//"00010203-0405-0607-0809-0A0B0C0D2B10";//

    public static final String NOTIFY_DESCRIPTOR = "00010203-0405-0607-0809-0a0b0c0d2b10";
}