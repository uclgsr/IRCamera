package com.mpdc4gsr.ble.callback;

import android.bluetooth.BluetoothDevice;



import com.mpdc4gsr.ble.Request;

/**
 * date: 2021/8/12 17:43
 * author: bichuanfeng
 */
public interface PhyChangeCallback extends RequestFailedCallback {

    void onPhyChange(Request request, int txPhy, int rxPhy);
}
