package com.topdon.lms.sdk.xutils.common;

/**
 * Callback stub for LMS SDK
 */
public interface Callback<T> {
    void onSuccess(T result);
    
    void onError(Throwable ex, boolean isOnCallback);
    
    void onCancelled(CancelledException cex);
    
    void onFinished();
    
    class CancelledException extends Exception {
        public CancelledException(String message) {
            super(message);
        }
    }
}