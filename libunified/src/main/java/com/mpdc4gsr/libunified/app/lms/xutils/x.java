package com.mpdc4gsr.libunified.app.lms.xutils;

import com.mpdc4gsr.libunified.app.lms.xutils.common.Callback;
import com.mpdc4gsr.libunified.app.lms.xutils.http.RequestParams;


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