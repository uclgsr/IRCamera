package com.mpdc4gsr.ble.callback;

import android.bluetooth.BluetoothDevice;

import com.mpdc4gsr.ble.Request;


public interface PhyChangeCallback extends RequestFailedCallback {

    void onPhyChange(Request request, int txPhy, int rxPhy);
}
