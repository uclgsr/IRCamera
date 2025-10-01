package com.topdon.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import com.topdon.commons.observer.Observer;

import java.util.UUID;


/**
 * 。，，，
 * <p>
 * date: 2021/8/12 13:15
 * author: bichuanfeng
 */
public interface EventObserver extends Observer {
    /**
     *
     *
     * @param state {@link BluetoothAdapter#STATE_OFF}
     */
    default void onBluetoothAdapterStateChanged(int state) {
    }

    /**
     *
     *
     * @param request
     * @param value
     */
    default void onCharacteristicRead(Request request, byte[] value) {
    }

    /**
     *
     *
     * @param device
     * @param service        UUID
     * @param characteristic UUID
     * @param value
     */
    default void onCharacteristicChanged(Device device, UUID service, UUID characteristic,
                                         byte[] value) {
    }

    /**
     *
     *
     * @param request
     * @param value
     */
    default void onCharacteristicWrite(Request request, byte[] value) {
    }

    /**
     *
     *
     * @param request
     * @param rssi
     */
    default void onRssiRead(Request request, int rssi) {
    }

    /**
     *
     *
     * @param request
     * @param value
     */
    default void onDescriptorRead(Request request, byte[] value) {
    }

    /**
     * / Indication
     *
     * @param request
     * @param isEnabled
     */
    default void onNotificationChanged(Request request, boolean isEnabled) {
    }

    /**
     *
     *
     * @param request
     * @param mtu
     */
    default void onMtuChanged(Request request, int mtu) {
    }

    /**
     * @param request
     * @param txPhy   。{@link BluetoothDevice#PHY_LE_1M_MASK}
     * @param rxPhy   。{@link BluetoothDevice#PHY_LE_1M_MASK}
     */
    default void onPhyChange(Request request, int txPhy, int rxPhy) {
    }

    /**
     *
     *
     * @param request
     * @param failType 。{@link Connection#REQUEST_FAIL_TYPE_GATT_IS_NULL}
     * @param value    ，null
     */
    default void onRequestFailed(Request request, int failType, Object value) {
    }

    /**
     *
     *
     * @param device 。{@link Device#getConnectionState()}，{@link ConnectionState#CONNECTED}
     */
    default void onConnectionStateChanged(Device device) {
    }

    /**
     *
     *
     * @param device
     * @param failType 。{@link Connection#CONNECT_FAIL_TYPE_MAXIMUM_RECONNECTION}
     */
    default void onConnectFailed(Device device, int failType) {
    }

    /**
     *
     *
     * @param device
     * @param type   。{@link Connection#TIMEOUT_TYPE_CANNOT_CONNECT}
     */
    default void onConnectTimeout(Device device, int type) {
    }
}
