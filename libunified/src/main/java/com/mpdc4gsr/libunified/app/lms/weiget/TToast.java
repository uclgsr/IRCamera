package com.mpdc4gsr.libunified.app.lms.weiget;

import android.content.Context;

public class TToast {
    public static void shortToast(Context context, String message) {
        // Do nothing - stub implementation
    }

    public static void shortToast(Context context, int resId) {
        // Do nothing - stub implementation  
    }

    public static void longToast(Context context, String message) {
        // Do nothing - stub implementation
    }

    public static void longToast(Context context, int resId) {
        // Do nothing - stub implementation
    }

    public static void show(Context context, String message) {
        shortToast(context, message);
    }

    public static void show(Context context, int resId) {
        shortToast(context, resId);
    }
}