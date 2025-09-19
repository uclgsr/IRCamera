package com.mpdc4gsr.ble.callback;

import com.mpdc4gsr.ble.Request;

public interface NotificationChangeCallback extends RequestFailedCallback {

    void onNotificationChanged(Request request, boolean isEnabled);
}
