package com.umeng.analytics;

import android.content.Context;

/**
 * Stub implementation of MobclickAgent to resolve circular dependency.
 * This provides minimal functionality to maintain compilation compatibility.
 */
public class MobclickAgent {
    public static void onEvent(Context context, String eventId) {
        // Stub implementation - does nothing
    }
    
    public static void onEvent(Context context, String eventId, String label) {
        // Stub implementation - does nothing
    }
}