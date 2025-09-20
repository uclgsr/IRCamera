package com.mpdc4gsr.lms.sdk.xutils.common;

/**
 * Callback stub for LMS SDK
 */
public interface Callback<T> {
    void onSuccess(T result);

    void onError(Throwable ex, boolean isOnCallback);

    void onCancelled(CancelledException cex);

    void onFinished();

    // Add CommonCallback interface
    interface CommonCallback<T> extends Callback<T> {
        // Default implementation for stub
        default void onSuccess(T result) {
        }

        default void onError(Throwable ex, boolean isOnCallback) {
        }

        default void onCancelled(CancelledException cex) {
        }

        default void onFinished() {
        }
    }

    class CancelledException extends Exception {
        public CancelledException(String message) {
            super(message);
        }
    }
}