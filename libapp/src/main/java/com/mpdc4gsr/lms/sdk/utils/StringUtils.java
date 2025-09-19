package com.mpdc4gsr.lms.sdk.utils;

import android.content.Context;

/**
 * String Utils stub for LMS SDK
 */
public class StringUtils {
    public static String getResString(Context context, int resId) {
        // The context parameter is unused in this stub implementation
        return "";
    }
    
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
}