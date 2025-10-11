package com.mpdc4gsr.component.shared.app.lms.weiget;

import android.content.Context;

import com.mpdc4gsr.component.shared.app.compose.components.ComposeToastHelper;

public class TToast {
    private static final long SHORT_DURATION = 2000L;
    private static final long LONG_DURATION = 3500L;

    public static void shortToast(Context context, String message) {
        if (context != null && message != null && !message.isEmpty()) {
            ComposeToastHelper.INSTANCE.show(context, message, SHORT_DURATION);
        }
    }

    public static void shortToast(Context context, int resId) {
        if (context != null) {
            ComposeToastHelper.INSTANCE.show(context, resId, SHORT_DURATION);
        }
    }

    public static void longToast(Context context, String message) {
        if (context != null && message != null && !message.isEmpty()) {
            ComposeToastHelper.INSTANCE.show(context, message, LONG_DURATION);
        }
    }

    public static void longToast(Context context, int resId) {
        if (context != null) {
            ComposeToastHelper.INSTANCE.show(context, resId, LONG_DURATION);
        }
    }

    public static void show(Context context, String message) {
        shortToast(context, message);
    }

    public static void show(Context context, int resId) {
        shortToast(context, resId);
    }
}

