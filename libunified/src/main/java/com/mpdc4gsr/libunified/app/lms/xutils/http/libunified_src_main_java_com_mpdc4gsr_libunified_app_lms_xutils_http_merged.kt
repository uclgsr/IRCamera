// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\xutils\http' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:39


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