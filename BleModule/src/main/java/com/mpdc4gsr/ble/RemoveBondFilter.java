package com.mpdc4gsr.ble;

import android.bluetooth.BluetoothDevice;

public interface RemoveBondFilter {
    boolean accept(BluetoothDevice device);
}
