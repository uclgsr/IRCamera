package com.mpdc4gsr.component.shared.app.lms.utils;

import android.content.Context;

public class SPUtils {
    private static volatile SPUtils instance;

    public static SPUtils getInstance(Context context) {
        // The context parameter is unused in this stub implementation
        if (instance == null) {
            synchronized (SPUtils.class) {
                if (instance == null) {
                    instance = new SPUtils();
                }
            }
        }
        return instance;
    }

    public void put(String key, Object value) {
    }

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

