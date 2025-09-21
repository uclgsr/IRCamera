package com.mpdc4gsr.lib.core.lms;

/**
 * Stub implementation of LMS class to resolve circular dependency.
 * This provides minimal functionality to maintain compilation compatibility.
 */
public class LMS {
    private static LMS instance = new LMS();
    
    public static LMS getInstance() {
        return instance;
    }
    
    public String getLoginName() {
        return "stub_user";
    }
}