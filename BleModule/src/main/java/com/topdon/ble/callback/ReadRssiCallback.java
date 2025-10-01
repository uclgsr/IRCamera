package com.topdon.ble.callback;


import com.topdon.ble.Request;

/**
 * date: 2021/8/12 17:44
 * author: bichuanfeng
 */
public interface ReadRssiCallback extends RequestFailedCallback {
    /**
     * 
     *
     * @param request 
     * @param rssi    
     */
    void onRssiRead(Request request, int rssi);
}
