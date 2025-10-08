// Merged ALL .kt and .java files from the 'BleModule\src\main\java\com\topdon\ble\util' directory and its subdirectories.
// Total files: 2 | Generated on: 2025-10-08 01:42:33


// ===== FROM: BleModule\src\main\java\com\topdon\ble\util\DefaultLogger.java =====

package com.topdon.ble.util;

import android.util.Log;

public class DefaultLogger implements Logger {
    private final String tag;
    private boolean isEnabled;

    public DefaultLogger (String tag) {
        this.tag = tag;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    @Override
    public void log(int priority, int type, String msg) {
        if (isEnabled) {
            Log.println(priority, tag, msg);
        }
    }

    @Override
    public void log(int priority, int type, String msg, Throwable th) {
        if (isEnabled) {
            if (msg != null) {
                log(priority, type, msg + "\n" + Log.getStackTraceString(th));
            } else {
                log(priority, type, Log.getStackTraceString(th));
            }
        }
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\util\Logger.java =====

package com.topdon.ble.util;

public interface Logger {

    int TYPE_GENERAL = 0;

    int TYPE_SCAN_STATE = 1;

    int TYPE_CONNECTION_STATE = 2;

    int TYPE_CHARACTERISTIC_READ = 3;

    int TYPE_CHARACTERISTIC_CHANGED = 4;

    int TYPE_READ_REMOTE_RSSI = 5;

    int TYPE_MTU_CHANGED = 6;

    int TYPE_REQUEST_FAILED = 7;
    int TYPE_DESCRIPTOR_READ = 8;
    int TYPE_NOTIFICATION_CHANGED = 9;
    int TYPE_INDICATION_CHANGED = 10;
    int TYPE_CHARACTERISTIC_WRITE = 11;
    int TYPE_PHY_CHANGE = 12;

    void log(int priority, int type, String msg);

    void log(int priority, int type, String msg, Throwable th);

    boolean isEnabled();

    void setEnabled(boolean isEnabled);
}