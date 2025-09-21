package com.mpdc4gsr.lib.core.lms.utils;

/**
 * Stub implementation of StringUtils class to resolve circular dependency.
 */
public class StringUtils {
    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }
    
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
}