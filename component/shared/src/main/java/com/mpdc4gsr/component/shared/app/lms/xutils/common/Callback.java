package com.mpdc4gsr.component.shared.app.lms.xutils.common;

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

    // Add ProgressCallback interface
    interface ProgressCallback<T> extends Callback<T> {
        void onWaiting();

        void onStarted();

        void onLoading(long total, long current, boolean isDownloading);
    }

    class CancelledException extends Exception {
        public CancelledException(String message) {
            super(message);
        }
    }
}

