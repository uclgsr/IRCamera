package com.mpdc4gsr.lib.core.lms.network;

/**
 * Stub implementation of IResponseCallback interface to resolve circular dependency.
 */
public interface IResponseCallback {
    void onSuccess(Object result);
    void onError(String error);
}