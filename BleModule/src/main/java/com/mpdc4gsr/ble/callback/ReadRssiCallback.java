package com.mpdc4gsr.ble.callback;



import com.mpdc4gsr.ble.Request;

/**
 * date: 2021/8/12 17:44
 * author: bichuanfeng
 */
public interface ReadRssiCallback extends RequestFailedCallback {

    void onRssiRead(Request request, int rssi);
}
