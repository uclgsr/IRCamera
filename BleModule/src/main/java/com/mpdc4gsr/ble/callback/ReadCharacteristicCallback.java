package com.mpdc4gsr.ble.callback;

import com.mpdc4gsr.ble.Request;

public interface ReadCharacteristicCallback extends RequestFailedCallback {

    void onCharacteristicRead(Request request, byte[] value);
}
