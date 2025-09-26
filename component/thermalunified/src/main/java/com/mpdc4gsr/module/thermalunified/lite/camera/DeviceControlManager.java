package com.mpdc4gsr.module.thermalunified.lite.camera;

import android.util.Log;

import com.energy.iruvccamera.usb.USBMonitor;
import com.mpdc4gsr.module.thermalunified.lite.camera.task.DeviceControlWorker;
import com.mpdc4gsr.module.thermalunified.lite.camera.task.IDeviceConnectListener;
import com.mpdc4gsr.module.thermalunified.lite.camera.task.PausePreviewTask;
import com.mpdc4gsr.module.thermalunified.lite.camera.task.ResumePreviewTask;
import com.mpdc4gsr.module.thermalunified.lite.camera.task.StartPreviewTask;
import com.mpdc4gsr.module.thermalunified.lite.camera.task.StopPreviewTask;

import java.util.HashMap;
import java.util.Map;

public class DeviceControlManager implements IDeviceConnectListener {

    private static final String TAG = "DualDeviceControlManager";
    private static DeviceControlManager mInstance;
    private DeviceControlWorker mDeviceControlWorker;
    private HashMap<String, IDeviceConnectListener> mIDeviceConnectListeners;

    private DeviceControlManager() {

    }

    public static synchronized DeviceControlManager getInstance() {
        if (mInstance == null) {
            mInstance = new DeviceControlManager();
        }
        return mInstance;
    }

    public void init() {
        mDeviceControlWorker = new DeviceControlWorker();
        mDeviceControlWorker.setDeviceControlCallback(this);
        mDeviceControlWorker.startWork();
        mIDeviceConnectListeners = new HashMap<>();
    }


    public void addDeviceConnectListener(String key, IDeviceConnectListener iDeviceConnectListener) {
        if (mIDeviceConnectListeners != null) {
            mIDeviceConnectListeners.put(key, iDeviceConnectListener);
        }
    }


    public void removeDeviceConnectListener(String key) {
        if (mIDeviceConnectListeners != null) {
            mIDeviceConnectListeners.remove(key);
        }
    }

    public void release() {
        if (mDeviceControlWorker != null) {
            mDeviceControlWorker.release();
            mDeviceControlWorker = null;
        }
        if (mIDeviceConnectListeners != null) {
            mIDeviceConnectListeners.clear();
            mIDeviceConnectListeners = null;
        }
    }


    public void handleStartPreview(USBMonitor.UsbControlBlock ctrlBlock) {
        if (mDeviceControlWorker != null) {            mDeviceControlWorker.addTask(new StartPreviewTask(ctrlBlock, mDeviceControlWorker.getDeviceState()));
        }
    }

    public void handleStopPreview() {
        if (mDeviceControlWorker != null) {            mDeviceControlWorker.addTask(new StopPreviewTask(mDeviceControlWorker.getDeviceState()));
        }
    }

    public void handlePauseDualPreview() {
        if (mDeviceControlWorker != null) {            mDeviceControlWorker.addTask(new PausePreviewTask(mDeviceControlWorker.getDeviceState()));
        }
    }

    public void handleResumeDualPreview() {
        if (mDeviceControlWorker != null) {            mDeviceControlWorker.addTask(new ResumePreviewTask(mDeviceControlWorker.getDeviceState()));
        }
    }

    @Override
    public void onPrepareConnect() {

        for (Map.Entry<String, IDeviceConnectListener> entry : mIDeviceConnectListeners.entrySet()) {
            entry.getValue().onPrepareConnect();
        }
    }

    @Override
    public void onConnected() {

        for (Map.Entry<String, IDeviceConnectListener> entry : mIDeviceConnectListeners.entrySet()) {
            entry.getValue().onConnected();
        }
    }

    @Override
    public void onDisconnected() {

        for (Map.Entry<String, IDeviceConnectListener> entry : mIDeviceConnectListeners.entrySet()) {
            entry.getValue().onDisconnected();
        }
    }

    @Override
    public void onPaused() {

        for (Map.Entry<String, IDeviceConnectListener> entry : mIDeviceConnectListeners.entrySet()) {
            entry.getValue().onPaused();
        }
    }

    @Override
    public void onResumed() {

        for (Map.Entry<String, IDeviceConnectListener> entry : mIDeviceConnectListeners.entrySet()) {
            entry.getValue().onResumed();
        }
    }
}
