package com.topdon.lms.sdk.network;

/**
 * Http Proxy stub for LMS SDK
 */
public class HttpProxy {
    private static HttpProxy instance;
    
    public static HttpProxy getInstant() {
        if (instance == null) {
            instance = new HttpProxy();
        }
        return instance;
    }
    
    // For backward compatibility
    public static class Companion {
        public static HttpProxy getInstant() {
            return HttpProxy.getInstant();
        }
    }
    
    public void post(String url, Object params, IResponseCallback callback) {
        if (callback != null) {
            callback.onResponse("{\"code\":\"2000\",\"message\":\"success\"}");
        }
    }
}