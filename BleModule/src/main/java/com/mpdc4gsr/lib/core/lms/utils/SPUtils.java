package com.mpdc4gsr.lib.core.lms.utils;

import android.content.Context;

/**
 * Stub implementation of SPUtils class to resolve circular dependency.
 * This provides minimal shared preferences functionality.
 */
public class SPUtils {
    public static SPUtils getInstance(Context context) {
        return new SPUtils();
    }
    
    public Object get(String key, Object defaultValue) {
        return defaultValue;
    }
    
    public void put(String key, Object value) {
        // Stub implementation - does nothing
    }
}