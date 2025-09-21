package com.mpdc4gsr.ble.callback;



import com.mpdc4gsr.ble.Request;

/**
 * date: 2021/8/12 17:43
 * author: bichuanfeng
 */
public interface NotificationChangeCallback extends RequestFailedCallback {

    void onNotificationChanged(Request request, boolean isEnabled);
}
