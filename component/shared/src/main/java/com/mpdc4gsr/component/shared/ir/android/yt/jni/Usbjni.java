package com.mpdc4gsr.component.shared.ir.android.yt.jni;


public class Usbjni {

    static {
        try {
            System.loadLibrary("usb3803_hub");
        } catch (UnsatisfiedLinkError e) {
        }
    }

    public static native int usb3803_mode_setting(int i);

    public static native int usb3803_read_parameter(int i);
}


