package com.topdon.ble.callback;


import com.topdon.ble.Request;

/**
 * date: 2021/8/12 17:43
 * author: bichuanfeng
 */
public interface NotificationChangeCallback extends RequestFailedCallback {
    /**
     * / Indication
     *
     * @param request
     * @param isEnabled
     */
    void onNotificationChanged(Request request, boolean isEnabled);
}
