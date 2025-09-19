package com.mpdc4gsr.ble.callback;

import com.mpdc4gsr.ble.Request;

public interface MtuChangeCallback extends RequestFailedCallback {

    void onMtuChanged(Request request, int mtu);
}
