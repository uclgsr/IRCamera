package com.mpdc4gsr.component.shared.ir.utils;

public interface USBMonitorCallback {

    void onAttach();

    void onGranted();

    void onConnect();

    void onDisconnect();

    void onDettach();

    void onCancel();

}


