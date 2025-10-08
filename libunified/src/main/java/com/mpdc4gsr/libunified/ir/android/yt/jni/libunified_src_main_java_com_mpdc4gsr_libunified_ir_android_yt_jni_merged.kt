// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\ir\android\yt\jni' directory and its subdirectories.
// Total files: 2 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ir\android\yt\jni\Usbcontorl.java =====

package com.mpdc4gsr.libunified.ir.android.yt.jni;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Usbcontorl extends Usbjni {

    public static boolean isload = false;

    static {
        File file = new File("/proc/self/maps");
        if (file.exists() && file.isFile()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(file));
                String tempString = null;

                while ((tempString = reader.readLine()) != null) {

                    if (tempString.contains("libusb3803_hub.so")) {
                        isload = true;
                        break;
                    }
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e1) {
                    }
                }
            }
        }
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ir\android\yt\jni\Usbjni.java =====

package com.mpdc4gsr.libunified.ir.android.yt.jni;

import android.util.Log;

public class Usbjni {

    static {
        try {
            System.loadLibrary("usb3803_hub");
        } catch (UnsatisfiedLinkError e) {
            Log.e("Usbjni", "Couldn't load lib:   - " + e.getMessage());
        }
    }

    public static native int usb3803_mode_setting(int i);

    public static native int usb3803_read_parameter(int i);
}