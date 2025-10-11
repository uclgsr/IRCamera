package com.mpdc4gsr.component.shared.app.lms.utils;

import android.content.Context;

public class StringUtils {
    public static String getResString(Context context, int resId) {
        return "";
    }

    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
}

