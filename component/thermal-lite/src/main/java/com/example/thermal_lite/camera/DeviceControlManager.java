package com.example.thermal_lite.camera;

import android.util.Log;

import com.energy.iruvccamera.usb.USBMonitor;
import com.example.thermal_lite.camera.task.DeviceControlWorker;
import com.example.thermal_lite.camera.task.IDeviceConnectListener;
import com.example.thermal_lite.camera.task.PausePreviewTask;
import com.example.thermal_lite.camera.task.ResumePreviewTask;
import com.example.thermal_lite.camera.task.StartPreviewTask;
import com.example.thermal_lite.camera.task.StopPreviewTask;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by fengjibo on 2024/5/17.
 */
public class DeviceControlManager implements IDeviceConnectListener {

    private static final String TAG = "DualDeviceControlManager";

    private DeviceControlWorker mDeviceControlWorker;

    private HashMap<String, IDeviceConnectListener> mIDeviceConnectListeners;

    private DeviceControlManager() {

    }

    private static DeviceControlManager mInstance;

    public static synchronized DeviceControlManager getInstance() {
        if (mInstance == null) {
            mInstance = new DeviceControlManager();
        }
        return mInstance;
    }

    /**
initialize
     */
    public void init() {
        mDeviceControlWorker = new DeviceControlWorker();
        mDeviceControlWorker.setDeviceControlCallback(this);
        mDeviceControlWorker.startWork();
        mIDeviceConnectListeners = new HashMap<>();
    }

    /**
RegisterdevicestateCallback，可在activity或fragment中Register，用于UI的改变
@param key 唯一标识
     * @param iDeviceConnectListener
     */
    public void addDeviceConnectListener(String key, IDeviceConnectListener iDeviceConnectListener) {
        if (mIDeviceConnectListeners != null) {
            mIDeviceConnectListeners.put(key, iDeviceConnectListener);
        }
    }

    /**
CancelRegisterdevicestateCallback
     * @param key
     */
    public void removeDeviceConnectListener(String key) {
        if (mIDeviceConnectListeners != null) {
            mIDeviceConnectListeners.remove(key);
        }
    }

    /**
recycle资源
     */
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

    /**
dual lightdata流出图
     * @param ctrlBlock
     */
    public void handleStartPreview(USBMonitor.UsbControlBlock ctrlBlock) {
        if (mDeviceControlWorker != null) {
            Log.d(TAG, "handleStartPreview");
            mDeviceControlWorker.addTask(new StartPreviewTask(ctrlBlock, mDeviceControlWorker.getDeviceState()));
        }
    }

    /**
dual lightdata流停图
     */
    public void handleStopPreview() {
        if (mDeviceControlWorker != null) {
            Log.d(TAG, "handleStopPreview");
            mDeviceControlWorker.addTask(new StopPreviewTask(mDeviceControlWorker.getDeviceState()));
        }
    }

    /**
dual lightdata流pause
     */
    public void handlePauseDualPreview() {
        if (mDeviceControlWorker != null) {
            Log.d(TAG, "handlePausePreview");
            mDeviceControlWorker.addTask(new PausePreviewTask(mDeviceControlWorker.getDeviceState()));
        }
    }

    /**
dual lightdata流Restore
     */
    public void handleResumeDualPreview() {
        if (mDeviceControlWorker != null) {
            Log.d(TAG, "handleResumePreview");
            mDeviceControlWorker.addTask(new ResumePreviewTask(mDeviceControlWorker.getDeviceState()));
        }
    }

    @Override
    public void onPrepareConnect() {
StartPreview前Callback
        for (Map.Entry<String, IDeviceConnectListener> entry: mIDeviceConnectListeners.entrySet()) {
            entry.getValue().onPrepareConnect();
        }
    }

    @Override
    public void onConnected() {
StartPreviewsuccessful前后Callback，注意是子line程
        for (Map.Entry<String, IDeviceConnectListener> entry: mIDeviceConnectListeners.entrySet()) {
            entry.getValue().onConnected();
        }
    }

    @Override
    public void onDisconnected() {
StopPreviewsuccessful前后Callback，注意是子line程
        for (Map.Entry<String, IDeviceConnectListener> entry: mIDeviceConnectListeners.entrySet()) {
            entry.getValue().onDisconnected();
        }
    }

    @Override
    public void onPaused() {
todo 自行定义Paused Task来implementation
        for (Map.Entry<String, IDeviceConnectListener> entry: mIDeviceConnectListeners.entrySet()) {
            entry.getValue().onPaused();
        }
    }

    @Override
    public void onResumed() {
todo 自行定义Resumed Task来implementation
        for (Map.Entry<String, IDeviceConnectListener> entry: mIDeviceConnectListeners.entrySet()) {
            entry.getValue().onResumed();
        }
    }
}
