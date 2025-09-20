package com.mpdc4gsr.lms.sdk.xutils;

import com.mpdc4gsr.lms.sdk.xutils.common.Callback;
import com.mpdc4gsr.lms.sdk.xutils.http.RequestParams;

/**
 * X Utils stub for LMS SDK
 */
public class x {
    private static HttpManager httpManager = new HttpManager();

    public static HttpManager http() {
        return httpManager;
    }

    public static class HttpManager {
        public void post(RequestParams params, Callback.CommonCallback<String> callback) {
            // Stub implementation - do nothing
        }
    }
}