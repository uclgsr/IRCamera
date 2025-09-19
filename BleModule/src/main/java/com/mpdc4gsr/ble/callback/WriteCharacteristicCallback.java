package com.mpdc4gsr.ble.callback;

import com.mpdc4gsr.ble.Request;

public interface WriteCharacteristicCallback extends RequestFailedCallback {

    void onCharacteristicWrite(Request request, byte[] value);
}
