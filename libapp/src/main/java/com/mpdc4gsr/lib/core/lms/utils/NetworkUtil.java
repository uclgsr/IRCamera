package com.mpdc4gsr.lib.core.lms.utils;

import android.content.Context;

public class NetworkUtil {
    public static boolean isNetworkAvailable() {
        return true;
    }

    public static boolean isWifiConnected() {
        return false;
    }
    
    public static boolean isConnected(Context context) {
        return true;
    }
}