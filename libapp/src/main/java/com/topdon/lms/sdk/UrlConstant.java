package com.topdon.lms.sdk;

/**
 * URL Constant stub for LMS SDK
 */
public class UrlConstant {
    public static String BASE_URL = "https://example.com/";
    
    public static void setBaseUrl(String url, boolean useHttps) {
        BASE_URL = url;
    }
    
    public static String getBaseUrl() {
        return BASE_URL;
    }
}