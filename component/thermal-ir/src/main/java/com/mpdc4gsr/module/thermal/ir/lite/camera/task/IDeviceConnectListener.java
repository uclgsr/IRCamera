package com.mpdc4gsr.module.thermal.ir.lite.camera.task;

public interface IDeviceConnectListener {
    void onPrepareConnect();

    void onConnected();

    void onDisconnected();

    void onPaused();

    void onResumed();
}
