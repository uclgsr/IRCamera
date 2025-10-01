package com.topdon.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.UUID;

/**
 * date: 2021/8/12 13:45
 * author: bichuanfeng
 */
public interface Connection {
    UUID clientCharacteristicConfig = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    /**
     *
     */
    int REQUEST_FAIL_TYPE_REQUEST_FAILED = 0;
    int REQUEST_FAIL_TYPE_CHARACTERISTIC_NOT_EXIST = 1;
    int REQUEST_FAIL_TYPE_DESCRIPTOR_NOT_EXIST = 2;
    int REQUEST_FAIL_TYPE_SERVICE_NOT_EXIST = 3;
    /**
     * [BluetoothGatt.GATT_SUCCESS]
     */
    int REQUEST_FAIL_TYPE_GATT_STATUS_FAILED = 4;
    int REQUEST_FAIL_TYPE_GATT_IS_NULL = 5;
    int REQUEST_FAIL_TYPE_BLUETOOTH_ADAPTER_DISABLED = 6;
    int REQUEST_FAIL_TYPE_REQUEST_TIMEOUT = 7;
    int REQUEST_FAIL_TYPE_CONNECTION_DISCONNECTED = 8;
    int REQUEST_FAIL_TYPE_CONNECTION_RELEASED = 9;

    //-------------------
    int TIMEOUT_TYPE_CANNOT_DISCOVER_DEVICE = 0;
    /**
     * ，
     */
    int TIMEOUT_TYPE_CANNOT_CONNECT = 1;
    /**
     * ，，[BluetoothGattCallback.onServicesDiscovered]
     */
    int TIMEOUT_TYPE_CANNOT_DISCOVER_SERVICES = 2;

    //--------------------------------
    /**
     *
     */
    int CONNECT_FAIL_TYPE_MAXIMUM_RECONNECTION = 1;
    /**
     *
     */
    int CONNECT_FAIL_TYPE_CONNECTION_IS_UNSUPPORTED = 2;

    @NonNull
    Device getDevice();

    /**
     *
     */
    int getMtu();

    /**
     *
     */
    void reconnect();

    /**
     *
     */
    void disconnect();

    /**
     *
     */
    void refresh();

    /**
     *
     */
    void release();

    /**
     * ，
     */
    void releaseNoEvent();

    /**
     *
     */
    @NonNull
    ConnectionState getConnectionState();

    /**
     *
     */
    boolean isAutoReconnectEnabled();

    @Nullable
    BluetoothGatt getGatt();

    /**
     * ，
     */
    void clearRequestQueue();

    /**
     * ，null，，
     */
    void clearRequestQueueByType(RequestType type);

    @NonNull
    ConnectionConfiguration getConnectionConfiguration();

    @Nullable
    BluetoothGattService getService(UUID service);

    @Nullable
    BluetoothGattCharacteristic getCharacteristic(UUID service, UUID characteristic);

    @Nullable
    BluetoothGattDescriptor getDescriptor(UUID service, UUID characteristic, UUID descriptor);

    /**
     *
     */
    void execute(Request request);

    /**
     * Indication
     */
    boolean isNotificationOrIndicationEnabled(BluetoothGattCharacteristic characteristic);

    /**
     * Indication
     */
    boolean isNotificationOrIndicationEnabled(UUID service, UUID characteristic);

    /**
     *
     */
    void setBluetoothGattCallback(BluetoothGattCallback callback);

    /**
     *
     *
     * @param service        UUID
     * @param characteristic UUID
     * @param property       。{@link BluetoothGattCharacteristic#PROPERTY_WRITE}
     */
    boolean hasProperty(UUID service, UUID characteristic, int property);
}
