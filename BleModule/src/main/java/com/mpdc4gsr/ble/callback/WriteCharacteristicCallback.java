package com.mpdc4gsr.ble.callback;



import com.mpdc4gsr.ble.Request;

/**
 * date: 2021/8/12 17:40
 * author: bichuanfeng
 */
public interface WriteCharacteristicCallback extends RequestFailedCallback {

    void onCharacteristicWrite(Request request, byte[] value);
}
