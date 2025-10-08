// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\ir\usbdual\view' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ir\usbdual\view\SurfaceNativeWindow.java =====

package com.mpdc4gsr.libunified.ir.usbdual.view;

public class SurfaceNativeWindow {

    static {
        System.loadLibrary("native-window");
    }

    public native void onCreateSurface(Object surface, int width, int height);

    public native void onDrawFrame(byte[] ARGBdata, int width, int height);

    public native void onReleaseSurface();

    public native void drawBitmap(Object surface, Object bitmap);
}