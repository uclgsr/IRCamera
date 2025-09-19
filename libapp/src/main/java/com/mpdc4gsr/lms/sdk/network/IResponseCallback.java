package com.mpdc4gsr.lms.sdk.network;

/**
 * Response callback interface for LMS SDK
 */
public interface IResponseCallback {
    void onResponse(String response);
    
    default void onFail(Exception e) {}
    
    default void onFail(String failMsg, String errorCode) {}
}