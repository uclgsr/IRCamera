package com.mpdc4gsr.component.shared.app.lms.network;

public interface IResponseCallback {
    void onResponse(String response);

    default void onFail(Exception e) {
    }

    default void onFail(String failMsg, String errorCode) {
    }
}

