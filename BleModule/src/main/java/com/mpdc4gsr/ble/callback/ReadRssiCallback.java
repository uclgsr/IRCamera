package com.mpdc4gsr.ble.callback;

import com.mpdc4gsr.ble.Request;

public interface ReadRssiCallback extends RequestFailedCallback {

    void onRssiRead(Request request, int rssi);
}
