package com.mpdc4gsr.module.thermal.ir.lite.camera.task;

import android.os.SystemClock;
import android.util.Log;

import com.mpdc4gsr.module.thermal.ir.lite.camera.CameraPreviewManager;

public class StopPreviewTask extends BaseTask {

    public StopPreviewTask(DeviceState deviceState) {
        this.mDeviceState = deviceState;
    }

    @Override
    public void run() {
        if (mDeviceState != DeviceState.CLOSED) {
            Log.d(TAG, "stopPreview start");

            CameraPreviewManager.getInstance().stopPreview();
            SystemClock.sleep(100);
            CameraPreviewManager.getInstance().closePreview();

            SystemClock.sleep(200);
            mDeviceState = DeviceState.CLOSED;
            Log.d(TAG, "stopPreview end33");
        }
    }
}
