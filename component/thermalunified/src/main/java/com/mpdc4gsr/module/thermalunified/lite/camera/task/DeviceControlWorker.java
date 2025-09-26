package com.mpdc4gsr.module.thermalunified.lite.camera.task;

import android.util.Log;

import java.util.concurrent.ArrayBlockingQueue;

public class DeviceControlWorker {
    private static final String TAG = "DeviceControlWorker";
    private Thread mThread;
    private ArrayBlockingQueue<BaseTask> mEventQueue = new ArrayBlockingQueue<>(2);
    private DeviceState mDeviceState = DeviceState.NONE;
    private IDeviceConnectListener mDeviceControlCallback;
    private boolean isStartPreviewing = false;

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            BaseTask task;
            DeviceState previousState;

            try {
                while (!Thread.currentThread().isInterrupted()) {                    synchronized (mEventQueue) {
                        while (mEventQueue.isEmpty()) {                            try {
                                mEventQueue.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        task = mEventQueue.peek();
                    }
                    previousState = mDeviceState;
                    task.setDeviceState(mDeviceState);
                    if (task instanceof StartPreviewTask) {
                        ((StartPreviewTask) task).setDeviceControlCallback(mDeviceControlCallback);
                    }
                    task.run();
                    mEventQueue.poll();

                    mDeviceState = task.getDeviceState();                    if (mDeviceControlCallback != null) {

                        if (mDeviceState != previousState) {
                            if (mDeviceState == DeviceState.OPEN) {
                                mDeviceControlCallback.onConnected();
                            } else if (mDeviceState == DeviceState.CLOSED) {
                                mDeviceControlCallback.onDisconnected();
                            } else if (mDeviceState == DeviceState.RESUMED) {
                                mDeviceState = DeviceState.OPEN;
                                mDeviceControlCallback.onResumed();
                            } else if (mDeviceState == DeviceState.PAUSED) {
                                mDeviceControlCallback.onPaused();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public void startWork() {        if (mThread == null) {
            mThread = new Thread(mRunnable);
            mThread.start();
        }
    }

    public void stopWork() {        if (mThread != null) {
            try {
                mThread.interrupt();

                mThread = null;
            } catch (Exception e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }
    }

    public void addTask(BaseTask task) {
        synchronized (mEventQueue) {
            if (mEventQueue.size() < 2) {.getSimpleName());
                mEventQueue.add(task);
            } else {                BaseTask task1 = mEventQueue.poll();
                if (task1 instanceof StartPreviewTask) {                } else if (task1 instanceof StopPreviewTask) {                }
                mEventQueue.add(task);
            }
            mEventQueue.notify();
        }
    }

    public void setDeviceControlCallback(IDeviceConnectListener mDeviceControlCallback) {
        this.mDeviceControlCallback = mDeviceControlCallback;
    }

    public void release() {
        mDeviceControlCallback = null;
        stopWork();
    }

    public DeviceState getDeviceState() {
        return mDeviceState;
    }

    public boolean isStartPreviewing() {
        return isStartPreviewing;
    }
}
