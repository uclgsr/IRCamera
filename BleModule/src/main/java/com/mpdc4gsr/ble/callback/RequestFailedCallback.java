package com.mpdc4gsr.ble.callback;




import com.mpdc4gsr.ble.Request;

/**
 * date: 2021/8/12 17:39
 * author: bichuanfeng
 */
public interface RequestFailedCallback extends RequestCallback {

    void onRequestFailed(Request request, int failType, Object value);
}
