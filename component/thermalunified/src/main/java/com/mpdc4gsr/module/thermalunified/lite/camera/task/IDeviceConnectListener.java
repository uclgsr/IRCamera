package com.mpdc4gsr.module.thermalunified.lite.camera.task;

public interface IDeviceConnectListener {
    void onPrepareConnect();

    void onConnected();

    void onDisconnected();

    void onPaused();

    void onResumed();
}
