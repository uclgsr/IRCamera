package com.mpdc4gsr.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mpdc4gsr.ble.callback.MtuChangeCallback;
import com.mpdc4gsr.ble.callback.NotificationChangeCallback;
import com.mpdc4gsr.ble.callback.ReadCharacteristicCallback;
import com.mpdc4gsr.commons.UUIDManager;
import com.mpdc4gsr.commons.observer.Observable;
import com.mpdc4gsr.commons.observer.Observe;
import com.mpdc4gsr.commons.poster.RunOn;
import com.mpdc4gsr.commons.poster.Tag;
import com.mpdc4gsr.commons.poster.ThreadMode;
import com.mpdc4gsr.commons.util.LLog;
import com.mpdc4gsr.commons.util.StringUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.UUID;

public class BluetoothManager implements EventObserver {
    private static final String TAG = "BluetoothManager";

    public static boolean iSReset = false;
    public static boolean isSending = false;
    public static boolean isClickStopCharging = false;
    public static boolean isReceiveBleData = false;
    private static BluetoothManager instance = null;
    private Device mDevice;
    private Connection connection;
    private BluetoothGattCharacteristic writeCharact = null;

    public BluetoothManager() {
    }

    public static BluetoothManager getInstance() {
        if (instance == null)
            instance = new BluetoothManager();
        return instance;
    }

    public static void setBleData(String message) {


    }

    public Device getDevice() {
        return mDevice;
    }

    private void setMTUValue() {
        if (mDevice.isConnected()) {

            Log.e("bcf_ble", "Connect[CHINESE_TEXT]：" + mDevice.getName() + "");
            RequestBuilder<MtuChangeCallback> builder = null;
            if (mDevice.getName().contains("T-darts") || mDevice.getName().contains("TD")) {
                builder = new RequestBuilderFactory().getChangeMtuBuilder(240);
            } else {
                builder = new RequestBuilderFactory().getChangeMtuBuilder(503);
            }
            Request request = builder.setCallback(new MtuChangeCallback() {
                @Override
                public void onMtuChanged(@NonNull Request request, int mtu) {
                    Log.d("wangchen", "MTUModifySuccess，[CHINESE_TEXT]：" + mtu);
                    setReadCallback();
                }

                @Override
                public void onRequestFailed(@NonNull Request request, int failType, @Nullable Object value) {
                    Log.d("bcf", "MTUModifyFailed");
                }

            }).build();
            connection.execute(request);
        }
    }

    private void setReadCallback() {
        if (mDevice.isConnected()) {
            isSending = false;

            boolean isEnabled = connection.isNotificationOrIndicationEnabled(UUID.fromString(UUIDManager.SERVICE_UUID), UUID.fromString(UUIDManager.NOTIFY_UUID));
            LLog.w("bcf_ble", "[CHINESE_TEXT]Open[CHINESE_TEXT]Notifycation: " + isEnabled);
            RequestBuilder<NotificationChangeCallback> builder = new RequestBuilderFactory().getSetNotificationBuilder(UUID.fromString(UUIDManager.SERVICE_UUID), UUID.fromString(UUIDManager.NOTIFY_UUID), true);
            RequestBuilder<ReadCharacteristicCallback> builder1 = new RequestBuilderFactory().getReadCharacteristicBuilder(UUID.fromString(UUIDManager.SERVICE_UUID), UUID.fromString(UUIDManager.READ_UUID));

            builder.build().execute(connection);
            builder1.build().execute(connection);
        }
    }

    public void setCancelListening() {
        Observable observable = EasyBLE.getInstance().getObservable();
        if (observable != null) {
            EasyBLE.getInstance().unregisterObserver(this);
        }
    }

    public Connection connect(Device device) {
        mDevice = device;
        ConnectionConfiguration config = new ConnectionConfiguration();
        config.setConnectTimeoutMillis(10000);
        config.setRequestTimeoutMillis(7000);
        config.setAutoReconnect(false);
        config.setReconnectImmediatelyMaxTimes(3);
        connection = EasyBLE.getInstance().connect(device, config, this);
        connection.setBluetoothGattCallback(new BluetoothGattCallback() {
            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                Log.d("ble_bcf_data", "[CHINESE_TEXT]Data[CHINESE_TEXT]：status: " + status + "  Content：" + StringUtils.toHex(characteristic.getValue()));
                setBleData("[CHINESE_TEXT]Data[CHINESE_TEXT]：status: " + status + "  Content：" + StringUtils.toHex(characteristic.getValue()));
            }
        });
        return connection;
    }

    public Connection connect(String mac, String name) {
        ConnectionConfiguration configuration = new ConnectionConfiguration();
        configuration.setConnectTimeoutMillis(10000);
        configuration.setRequestTimeoutMillis(7000);
        configuration.setAutoReconnect(false);
        configuration.setReconnectImmediatelyMaxTimes(3);
        connection = EasyBLE.getInstance().connect(mac, configuration, this);
        mDevice = connection.getDevice();
        mDevice.setName(name);
        return connection;
    }

    public void release() {
        Log.d("bcf", "[CHINESE_TEXT]BLEConnect");
        EasyBLE.getInstance().disconnectConnection(mDevice);
        EasyBLE.getInstance().release();
        EasyBLE.getInstance().releaseConnection(mDevice);
    }

    public boolean isConnected() {
        if (mDevice == null)
            return false;
        return mDevice.isConnected();
    }

    @Tag("onConnectionStateChanged")
    @Observe
    @RunOn(ThreadMode.MAIN)
    @Override
    public void onConnectionStateChanged(@NonNull Device device) {
        if (device.getConnectionState() != ConnectionState.SERVICE_DISCOVERED || device.getConnectionState() != ConnectionState.DISCONNECTED) {
            EventBus.getDefault().post(device.getConnectionState());
            Log.e("wangchen", "Send[CHINESE_TEXT]--" + device.getConnectionState());
        }
        Log.d("ywq", "MyObserver Connect[CHINESE_TEXT]：" + device.getConnectionState() + " [CHINESE_TEXT]Connected： " + device.isConnected() + "-----[CHINESE_TEXT]：" + device.getName() + "-------mac: " + device.getAddress());
        switch (device.getConnectionState()) {
            case SCANNING_FOR_RECONNECTION:
                break;
            case CONNECTING:
                break;
            case CONNECTED:
                break;
            case DISCONNECTED:
                EventBus.getDefault().post(ConnectionState.DISCONNECTED.name());
                break;
            case RELEASED:
                EventBus.getDefault().post(ConnectionState.RELEASED.name());
                break;
            case SERVICE_DISCOVERED:
                setMTUValue();

                if (device.isConnected()) {
                    EventBus.getDefault().post(ConnectionState.SERVICE_DISCOVERED.name());
                }
                break;
        }
    }

    @Override
    public void onConnectFailed(Device device, int failType) {
        Log.e("bcf_ble", "ConnectFailed" + device.getName());
        EventBus.getDefault().post(device.getConnectionState());
    }

    @Override
    public void onConnectTimeout(Device device, int type) {
        Log.e("bcf_ble", "Connect[CHINESE_TEXT]Hour");
    }

    @Observe
    @Override
    public void onNotificationChanged(@NonNull Request request, boolean isEnabled) {
        String typeTag = "";
        if (request.getType() == RequestType.SET_NOTIFICATION) {
            typeTag = "Notification";
            EventBus.getDefault().post(ConnectionState.MTU_SUCCESS);
        } else {
            typeTag = "Indication";
        }
        Log.d("bcf_ble", "onNotificationChanged ：" + typeTag + "：" + (isEnabled ? "[CHINESE_TEXT]" : "Close"));
    }

    public boolean writeBuletoothData(byte[] data) {
        if (mDevice == null || !mDevice.isConnected()) {
            return false;
        }

        if (!com.mpdc4gsr.ble.util.BluetoothPermissionUtils.hasBluetoothConnectPermission(EasyBLE.getInstance().getContext())) {
            Log.w(TAG, "Missing BLUETOOTH_CONNECT permission for GATT operations");
            return false;
        }

        try {
            writeCharact = connection.getCharacteristic(UUID.fromString(UUIDManager.SERVICE_UUID), UUID.fromString(UUIDManager.WRITE_UUID));
            connection.getGatt().setCharacteristicNotification(writeCharact, true); 

            writeCharact.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            writeCharact.setValue(data);

            return connection.getGatt().writeCharacteristic(writeCharact);
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException during GATT write operation: " + e.getMessage());
            return false;
        }
    }

    @Observe
    @Override
    public void onCharacteristicRead(Request request, byte[] value) {

        String data = StringUtils.toHex(value); 

    }

    @Observe
    @Override
    public void onCharacteristicChanged(Device device, UUID service, UUID characteristic, byte[] value) {
        Log.e("ble_bcf_data", "Receive[CHINESE_TEXT]Data：" + StringUtils.toHex(value));
        EventBus.getDefault().post(value);
    }

}
