// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\xutils\common' directory and its subdirectories.
// Total files: 2 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\xutils\common\Callback.java =====

package com.mpdc4gsr.libunified.app.lms.xutils.common;

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


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\xutils\common\task\PriorityExecutor.java =====

package com.mpdc4gsr.libunified.app.lms.xutils.common.task;

public class PriorityExecutor {
    private int corePoolSize;
    private boolean allowCoreThreadTimeOut;

    public PriorityExecutor() {
        this.corePoolSize = 1;
        this.allowCoreThreadTimeOut = false;
    }

    public PriorityExecutor(int corePoolSize, boolean allowCoreThreadTimeOut) {
        this.corePoolSize = corePoolSize;
        this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
    }

    public static void execute(Runnable task) {
        // Execute immediately - stub implementation
        if (task != null) {
            task.run();
        }
    }
}