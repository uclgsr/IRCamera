package com.mpdc4gsr.lms.sdk.xutils.http;

/**
 * Request Params stub for LMS SDK
 */
public class RequestParams {
    public String uri;
    public boolean isAsJsonContent = false;

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
}