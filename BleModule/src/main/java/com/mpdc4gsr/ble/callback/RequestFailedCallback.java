package com.mpdc4gsr.ble.callback;

import com.mpdc4gsr.ble.Request;

public interface RequestFailedCallback extends RequestCallback {

    void onRequestFailed(Request request, int failType, Object value);
}
