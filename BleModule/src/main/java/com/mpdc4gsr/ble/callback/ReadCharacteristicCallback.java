package com.mpdc4gsr.ble.callback;



import com.mpdc4gsr.ble.Request;

/**
 * date: 2021/8/12 18:42
 * author: bichuanfeng
 */
public interface ReadCharacteristicCallback extends RequestFailedCallback {

    void onCharacteristicRead(Request request, byte[] value);
}
