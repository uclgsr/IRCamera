package com.mpdc4gsr.component.shared.app.lms.network;

public class HttpProxy {
    private static volatile HttpProxy instance;

    public static HttpProxy getInstance() {
        if (instance == null) {
            synchronized (HttpProxy.class) {
                if (instance == null) {
                    instance = new HttpProxy();
                }
            }
        }
        return instance;
    }

    public static HttpProxy getInstant() {
        return getInstance();
    }

    public void post(String url, Object params, IResponseCallback callback) {
        if (callback != null) {
            callback.onResponse("{\"code\":\"2000\",\"message\":\"success\"}");
        }
    }

    public void post(String url, boolean param, Object params, IResponseCallback callback) {
        post(url, params, callback);
    }

    // For backward compatibility
    public static class Companion {
        // Static reference for Kotlin import compatibility
        public static final HttpProxy instant = getInstance();

        public static HttpProxy getInstant() {
            return HttpProxy.getInstance();
        }
    }
}

