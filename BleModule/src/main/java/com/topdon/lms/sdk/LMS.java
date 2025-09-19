package com.topdon.lms.sdk;

/**
 * Minimal LMS stub for BleModule
 */
public class LMS {
    private static LMS instance;
    
    public static LMS getInstance() {
        if (instance == null) {
            instance = new LMS();
        }
        return instance;
    }
    
    public String getLoginName() {
        return "";
    }
}