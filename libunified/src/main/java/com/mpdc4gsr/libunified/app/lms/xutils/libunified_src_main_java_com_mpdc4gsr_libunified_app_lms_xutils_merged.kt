// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\xutils' directory and its subdirectories.
// Total files: 4 | Generated on: 2025-10-08 01:42:39


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


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\xutils\http\RequestParams.java =====

package com.mpdc4gsr.libunified.app.lms.xutils.http;

import com.mpdc4gsr.libunified.app.lms.xutils.common.task.PriorityExecutor;

public class RequestParams {
    public String uri;
    public boolean isAsJsonContent = false;
    private String saveFilePath;
    private String cacheDirName;
    private boolean autoResume = false;
    private PriorityExecutor executor;

    public RequestParams() {
    }

    public RequestParams(String url) {
        this.uri = url;
    }

    public void addParameter(String key, Object value) {
    }

    public void addBodyParameter(String key, Object value) {
    }

    public void addHeader(String key, String value) {
    }

    public void setSaveFilePath(String path) {
        this.saveFilePath = path;
    }

    public void setCacheDirName(String name) {
        this.cacheDirName = name;
    }

    public void setAutoResume(boolean autoResume) {
        this.autoResume = autoResume;
    }

    public void setExecutor(PriorityExecutor executor) {
        this.executor = executor;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\xutils\x.java =====

package com.mpdc4gsr.libunified.app.lms.xutils;

import com.mpdc4gsr.libunified.app.lms.xutils.common.Callback;
import com.mpdc4gsr.libunified.app.lms.xutils.http.RequestParams;

import java.io.File;

public class x {
    private static HttpManager httpManager = new HttpManager();

    public static HttpManager http() {
        return httpManager;
    }

    public static class HttpManager {
        public void post(RequestParams params, Callback.CommonCallback<String> callback) {
            // Stub implementation - do nothing
        }

        public void get(RequestParams params, Callback.ProgressCallback<File> callback) {
            // Stub implementation - do nothing for now
            // In a real implementation, this would handle HTTP GET requests with progress callbacks
        }
    }
}