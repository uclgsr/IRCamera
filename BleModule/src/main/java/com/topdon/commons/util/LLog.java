package com.topdon.commons.util;



public class LLog {

    public final static int MAX_LENGTH = 2000;
    private static boolean isDebug = true; // Simplified for now

    public static void d(String tag, String value) {
//        if (isDebug) {
//        }
    }

    public static void i(String tag, String value) {
//        if (isDebug) {
//        }
    }

    public static void w(String tag, String value) {
//        if (isDebug) {
//        }
    }

    public static void e(String tag, String value) {
//        if (isDebug) {
//        }
    }

    public static void LogMaxPrint(String tag, String msg) {
        if (msg.length() > MAX_LENGTH) {
            int length = MAX_LENGTH + 1;
            String remain = msg;
            int index = 0;
            while (length > MAX_LENGTH) {
                index++;
                remain = remain.substring(MAX_LENGTH);
                length = remain.length();
            }
            if (length <= MAX_LENGTH) {
                index++;
            }
        } else {
        }
    }

}
