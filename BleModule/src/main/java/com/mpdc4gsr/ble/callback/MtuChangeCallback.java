package com.mpdc4gsr.ble.callback;


import com.mpdc4gsr.ble.Request;

/**
 * date: 2021/8/12 17:42
 * author: bichuanfeng
 */
public interface MtuChangeCallback extends RequestFailedCallback {

    void onMtuChanged(Request request, int mtu);
}
