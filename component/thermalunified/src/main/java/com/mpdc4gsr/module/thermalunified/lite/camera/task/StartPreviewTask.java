package com.mpdc4gsr.module.thermalunified.lite.camera.task;

import android.util.Log;

import com.energy.iruvccamera.usb.USBMonitor;
import com.mpdc4gsr.module.thermalunified.lite.camera.CameraPreviewManager;

public class StartPreviewTask extends BaseTask {
    private IDeviceConnectListener mDeviceControlCallback;
    private USBMonitor.UsbControlBlock mUsbControlBlock;

    public StartPreviewTask(USBMonitor.UsbControlBlock usbControlBlock, DeviceState deviceState) {
        this.mDeviceState = deviceState;
        this.mUsbControlBlock = usbControlBlock;
    }

    @Override
    public void run() {
        if (mDeviceState != DeviceState.OPEN) {            if (mDeviceControlCallback != null) {
                mDeviceControlCallback.onPrepareConnect();
            }

            CameraPreviewManager.getInstance().handleUSBConnect(mUsbControlBlock);

            mDeviceState = DeviceState.OPEN;        } else {
            mDeviceState = DeviceState.OPEN;
        }
    }

    public void setDeviceControlCallback(IDeviceConnectListener deviceControlCallback) {
        this.mDeviceControlCallback = deviceControlCallback;
    }
}
