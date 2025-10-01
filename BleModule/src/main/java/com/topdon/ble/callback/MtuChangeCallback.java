package com.topdon.ble.callback;


import com.topdon.ble.Request;

/**
 * date: 2021/8/12 17:42
 * author: bichuanfeng
 */
public interface MtuChangeCallback extends RequestFailedCallback {
    /**
     *
     *
     * @param request
     * @param mtu
     */
    void onMtuChanged(Request request, int mtu);
}
