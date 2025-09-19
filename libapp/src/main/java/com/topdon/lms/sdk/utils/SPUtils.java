package com.topdon.lms.sdk.utils;

import android.content.Context;

/**
 * SPUtils stub for LMS SDK
 */
public class SPUtils {
    private static SPUtils instance;
    
    public static SPUtils getInstance(Context context) {
        if (instance == null) {
            instance = new SPUtils();
        }
        return instance;
    }
    
    public void put(String key, Object value) {}
    
    public String getString(String key) {
        return "";
    }
    
    public String getString(String key, String defaultValue) {
        return defaultValue;
    }
    
    public boolean getBoolean(String key) {
        return false;
    }
    
    public boolean getBoolean(String key, boolean defaultValue) {
        return defaultValue;
    }
    
    public int getInt(String key) {
        return 0;
    }
    
    public int getInt(String key, int defaultValue) {
        return defaultValue;
    }
}