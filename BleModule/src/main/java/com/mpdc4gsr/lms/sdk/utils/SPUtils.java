package com.mpdc4gsr.lms.sdk.utils;

import android.content.Context;

/**
 * Minimal SPUtils stub for BleModule
 */
public class SPUtils {
    private static volatile SPUtils instance;
    
    public static SPUtils getInstance(Context context) {
        // The context parameter is unused in this stub implementation.
        if (instance == null) {
            synchronized (SPUtils.class) {
                if (instance == null) {
                    instance = new SPUtils();
                }
            }
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