package com.topdon.ble.callback;

import android.bluetooth.BluetoothDevice;

import com.topdon.ble.Request;

/**
 * date: 2021/8/12 17:43
 * author: bichuanfeng
 */
public interface PhyChangeCallback extends RequestFailedCallback {
    /**
     * @param request 
     * @param txPhy   。{@link BluetoothDevice#PHY_LE_1M_MASK}
     * @param rxPhy   。{@link BluetoothDevice#PHY_LE_1M_MASK}
     */
    void onPhyChange(Request request, int txPhy, int rxPhy);
}
