package com.mpdc4gsr.ble.callback;

import com.mpdc4gsr.ble.Request;

public interface ReadDescriptorCallback extends RequestFailedCallback {

    void onDescriptorRead(Request request, byte[] value);
}
