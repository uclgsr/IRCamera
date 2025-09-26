package com.topdon.ble.util;


/**
 * date: 2021/8/12 16:24
 * author: bichuanfeng
 */
public class DefaultLogger implements Logger {
    private final String tag;
    private boolean isEnabled;

    public DefaultLogger(String tag) {
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
    }

    @Override
    public void log(int priority, int type, String msg, Throwable th) {
    }
}
