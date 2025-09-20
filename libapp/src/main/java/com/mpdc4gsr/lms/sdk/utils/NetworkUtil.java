package com.mpdc4gsr.lms.sdk.utils;

import android.content.Context;

/**
 * Network Util stub for LMS SDK
 */
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