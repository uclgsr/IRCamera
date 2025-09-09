package com.umeng.analytics;

import android.content.Context;

/**
 * Stub implementation of MobclickAgent for UMeng Analytics
 * This provides the basic API without actual analytics functionality
 * Replace with real implementation when UMeng Analytics dependency is available
 */
public class MobclickAgent {
    
    /**
     * Log custom event with parameters
     * @param context Application context
     * @param eventName Event name
     * @param eventData Event parameter
     */
    public static void onEvent(Context context, String eventName, String eventData) {
        // Stub implementation - replace with real analytics when available
        android.util.Log.d("MobclickAgent", "Event: " + eventName + " Data: " + eventData);
    }
    
    /**
     * Log custom event
     * @param context Application context 
     * @param eventName Event name
     */
    public static void onEvent(Context context, String eventName) {
        // Stub implementation - replace with real analytics when available
        android.util.Log.d("MobclickAgent", "Event: " + eventName);
    }
}