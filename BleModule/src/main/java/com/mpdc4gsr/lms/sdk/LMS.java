package com.mpdc4gsr.lms.sdk;

/**
 * Minimal LMS stub for BleModule
 */
public class LMS {
    private static volatile LMS instance;
    
    public static LMS getInstance() {
        if (instance == null) {
            synchronized (LMS.class) {
                if (instance == null) {
                    instance = new LMS();
                }
            }
        }
        return instance;
    }
    
    public String getLoginName() {
        return "";
    }
}