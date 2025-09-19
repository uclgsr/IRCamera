package com.topdon.lms.sdk.utils;

import android.content.Context;

/**
 * Minimal SPUtils stub for BleModule
 */
public class SPUtils {
    private static SPUtils instance;
    
    public static SPUtils getInstance(Context context) {
        if (instance == null) {
            instance = new SPUtils();
        }
        return instance;
    }
    
    public String get(String key) {
        return "";
    }
    
    public String get(String key, String defaultValue) {
        return defaultValue;
    }
}