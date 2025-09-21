package com.mpdc4gsr.module.thermal.ir.lite.camera.task;

import android.util.Log;

import com.mpdc4gsr.module.thermal.ir.lite.camera.CameraPreviewManager;

public class ResumePreviewTask extends BaseTask {
    public ResumePreviewTask(DeviceState deviceState) {
        this.mDeviceState = deviceState;
    }

    @Override
    public void run() {
        if (mDeviceState != DeviceState.RESUMED) {
            Log.d(TAG, "resumePreview start");
            CameraPreviewManager.getInstance().resumePreview();
            mDeviceState = DeviceState.RESUMED;
        }
    }
}
