package com.mpdc4gsr.component.shared.app.lms;

public class UrlConstants {
    public static volatile String BASE_URL = "https://example.com/";

    public static synchronized void setBaseUrl(String url, boolean useHttps) {
        // The useHttps parameter is ignored in this stub implementation
        BASE_URL = url;
    }

    public static String getBaseUrl() {
        return BASE_URL;
    }
}

