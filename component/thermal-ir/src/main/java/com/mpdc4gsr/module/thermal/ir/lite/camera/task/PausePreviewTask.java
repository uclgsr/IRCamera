package com.mpdc4gsr.module.thermal.ir.lite.camera.task;

import android.util.Log;

import com.mpdc4gsr.module.thermal.ir.lite.camera.CameraPreviewManager;

public class PausePreviewTask extends BaseTask {
    public PausePreviewTask(DeviceState deviceState) {
        this.mDeviceState = deviceState;
    }

    @Override
    public void run() {
        if (mDeviceState != DeviceState.PAUSED) {
            Log.d(TAG, "pausePreview start");
            CameraPreviewManager.getInstance().pausePreview();
            mDeviceState = DeviceState.PAUSED;
        }
    }
}
