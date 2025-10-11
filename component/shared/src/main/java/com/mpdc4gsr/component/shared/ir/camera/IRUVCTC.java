package com.mpdc4gsr.component.shared.ir.camera;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.os.SystemClock;

import com.energy.iruvc.ircmd.ConcreteIRCMDBuilder;
import com.energy.iruvc.ircmd.IRCMD;
import com.energy.iruvc.ircmd.IRCMDType;
import com.energy.iruvc.sdkisp.LibIRProcess;
import com.energy.iruvc.usb.USBMonitor;
import com.energy.iruvc.utils.*;
import com.energy.iruvc.uvc.*;
import com.mpdc4gsr.component.shared.app.utils.SharedFileUtils;
import com.mpdc4gsr.component.shared.app.utils.SharedScreenUtils;
import com.mpdc4gsr.component.shared.ir.config.MsgCode;
import com.mpdc4gsr.component.shared.ir.utils.USBMonitorCallback;

import java.util.ArrayList;
import java.util.List;

public class IRUVCTC {
    private static final String TAG = "IRUVC_DATA";
    private static final int PID_5840 = 0x5840;
    private static final int PID_3901 = 0x3901;
    private static final int PID_5830 = 0x5830;
    private static final int PID_5838 = 0x5838;
    private static final int ROTATE_270 = 270;
    private static final int ROTATE_90 = 90;
    private static final int ROTATE_180 = 180;
    private final IFrameCallback iFrameCallback;
    private final USBMonitor mUSBMonitor;
    private final ConnectCallback mConnectCallback;
    private final int imageOrTempDataLength = 256 * 192 * 2;
    private final SynchronizedBitmap syncimage;
    private final LibIRProcess.AutoGainSwitchInfo_t auto_gain_switch_info = new LibIRProcess.AutoGainSwitchInfo_t();
    private final LibIRProcess.GainSwitchParam_t gain_switch_param = new LibIRProcess.GainSwitchParam_t();
    private final CommonParams.GainStatus gainStatus = CommonParams.GainStatus.HIGH_GAIN;
    private final byte[] temperatureTemp = new byte[imageOrTempDataLength];
    private final CommonParams.DataFlowMode defaultDataFlowMode;
    private final boolean auto_over_portect = false;
    public UVCCamera uvcCamera;
    public boolean auto_gain_switch = false;
    public byte[] imageEditTemp = null;
    public volatile boolean isFirstFrame;
    private IRCMD ircmd;
    private byte[] imageSrc;
    private byte[] temperatureSrc;
    private int rotateInt = 0;
    private boolean isFrameReady = true;
    private boolean isTempReplacedWithTNREnabled;
    private boolean isRestart;
    private int pids[] = {PID_5840, PID_3901, PID_5830, PID_5838};
    private IFrameCallBackListener iFrameCallBackListener;
    private IFrameReadListener iFrameReadListener;

    public IRUVCTC(int cameraWidth, int cameraHeight, Context context, SynchronizedBitmap syncimage,
                   CommonParams.DataFlowMode dataFlowMode,
                   ConnectCallback connectCallback, USBMonitorCallback usbMonitorCallback) {
        this.syncimage = syncimage;
        mConnectCallback = connectCallback;
        defaultDataFlowMode = dataFlowMode;
        isFirstFrame = true;

        initUVCCamera();

        mUSBMonitor = new USBMonitor(context, new USBMonitor.OnDeviceConnectListener() {

            @Override
            public void onAttach(UsbDevice device) {
                if (uvcCamera == null || !uvcCamera.getOpenStatus()) {
                    mUSBMonitor.requestPermission(device);
                }
                if (usbMonitorCallback != null) {
                    usbMonitorCallback.onAttach();
                }
            }

            @Override
            public void onGranted(UsbDevice usbDevice, boolean granted) {
                if (usbMonitorCallback != null) {
                    usbMonitorCallback.onGranted();
                }
            }

            @Override
            public void onConnect(final UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
                if (isIRpid(device.getProductId())) {
                    if (createNew) {
                        openUVCCamera(ctrlBlock);

                        List<CameraSize> previewList = getAllSupportedSize();
                        for (CameraSize size : previewList) {
                        }

                        initIRCMD();

                        if (ircmd != null) {

                            isTempReplacedWithTNREnabled = ircmd.isTempReplacedWithTNREnabled(DeviceType.P2);
                            if (isTempReplacedWithTNREnabled) {

                                if (uvcCamera != null) {
                                    uvcCamera.setUSBPreviewSize(cameraWidth, cameraHeight * 2);
                                }
                            } else {

                                if (uvcCamera != null) {
                                    uvcCamera.setUSBPreviewSize(cameraWidth, cameraHeight);
                                }
                            }
                            startPreview();
                        }

                        if (usbMonitorCallback != null) {
                            usbMonitorCallback.onConnect();
                        }
                    }
                }
            }

            @Override
            public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
                if (usbMonitorCallback != null) {
                    usbMonitorCallback.onDisconnect();
                }
            }

            @Override
            public void onDettach(UsbDevice device) {
                if (uvcCamera != null && uvcCamera.getOpenStatus()) {
                    if (usbMonitorCallback != null) {
                        usbMonitorCallback.onDettach();
                    }
                }
            }

            @Override
            public void onCancel(UsbDevice device) {
                if (usbMonitorCallback != null) {
                    usbMonitorCallback.onCancel();
                }
            }
        });

        gain_switch_param.above_pixel_prop = 0.1f;
        gain_switch_param.above_temp_data = (int) ((130 + 273.15) * 16 * 4);
        gain_switch_param.below_pixel_prop = 0.95f;
        gain_switch_param.below_temp_data = (int) ((110 + 273.15) * 16 * 4);
        auto_gain_switch_info.switch_frame_cnt = 5 * 15;
        auto_gain_switch_info.waiting_frame_cnt = 7 * 15;

        int low_gain_over_temp_data = (int) ((550 + 273.15) * 16 * 4);
        int high_gain_over_temp_data = (int) ((150 + 273.15) * 16 * 4);
        float pixel_above_prop = 0.02f;
        int switch_frame_cnt = 7 * 15;
        int close_frame_cnt = 10 * 15;

        LibIRProcess.ImageRes_t imageRes = new LibIRProcess.ImageRes_t();
        imageRes.height = (char) (dataFlowMode == CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT ? cameraHeight / 2
                : cameraHeight);
        imageRes.width = (char) cameraWidth;

        iFrameCallback = new IFrameCallback() {
            @Override
            public void onFrame(byte[] frame) {
                if (!isFrameReady) {
                    return;
                }
                if (syncimage == null) {
                    return;
                }
                syncimage.start = true;

                synchronized (syncimage.dataLock) {

                    int length = frame.length - 1;
                    if (frame[length] == 1) {

                        return;
                    }
                    if (imageEditTemp != null && imageEditTemp.length >= length) {

                        System.arraycopy(frame, 0, imageEditTemp, 0, length);
                    }

                    if (dataFlowMode == CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT) {

                        System.arraycopy(frame, 0, imageSrc, 0, imageOrTempDataLength);

                        if (length >= imageOrTempDataLength * 2) {

                            if (rotateInt == ROTATE_270) {

                                System.arraycopy(frame, imageOrTempDataLength, temperatureTemp, 0,
                                        imageOrTempDataLength);
                                LibIRProcess.rotateRight90(temperatureTemp, imageRes,
                                        CommonParams.IRPROCSRCFMTType.IRPROC_SRC_FMT_Y14, temperatureSrc);
                            } else if (rotateInt == ROTATE_90) {

                                System.arraycopy(frame, imageOrTempDataLength, temperatureTemp, 0,
                                        imageOrTempDataLength);
                                LibIRProcess.rotateLeft90(temperatureTemp, imageRes,
                                        CommonParams.IRPROCSRCFMTType.IRPROC_SRC_FMT_Y14, temperatureSrc);
                            } else if (rotateInt == ROTATE_180) {

                                System.arraycopy(frame, imageOrTempDataLength, temperatureTemp, 0,
                                        imageOrTempDataLength);
                                LibIRProcess.rotate180(temperatureTemp, imageRes,
                                        CommonParams.IRPROCSRCFMTType.IRPROC_SRC_FMT_Y14, temperatureSrc);
                            } else {

                                System.arraycopy(frame, imageOrTempDataLength, temperatureSrc, 0,
                                        imageOrTempDataLength);

                            }
                            if (ircmd != null) {

                                if (auto_gain_switch) {
                                    ircmd.autoGainSwitch(temperatureSrc, imageRes, auto_gain_switch_info,
                                            gain_switch_param, new AutoGainSwitchCallback() {
                                                @Override
                                                public void onAutoGainSwitchState(CommonParams.PropTPDParamsValue.GAINSELStatus gainselStatus) {
                                                }

                                                @Override
                                                public void onAutoGainSwitchResult(CommonParams.PropTPDParamsValue.GAINSELStatus gainselStatus, int result) {
                                                }
                                            });
                                }

                                if (auto_over_portect) {
                                    ircmd.avoidOverexposure(false, gainStatus, temperatureSrc, imageRes,
                                            low_gain_over_temp_data,
                                            high_gain_over_temp_data, pixel_above_prop, switch_frame_cnt,
                                            close_frame_cnt,
                                            new AvoidOverexposureCallback() {
                                                @Override
                                                public void onAvoidOverexposureState(boolean avoidOverexpol) {
                                                }
                                            });
                                }
                            }
                        }
                    } else {

                        System.arraycopy(frame, 0, imageSrc, 0, imageOrTempDataLength);
                    }
                    if (iFrameCallBackListener != null) {
                        iFrameCallBackListener.updateData();
                    }
                }
                if (isFirstFrame && iFrameReadListener != null) {
                    iFrameReadListener.frameRead();
                    isFirstFrame = false;
                }
            }

        };
    }

    public void setIFrameCallBackListener(IFrameCallBackListener iFrameCallBackListener) {
        this.iFrameCallBackListener = iFrameCallBackListener;
    }

    public void setiFirstFrameListener(IFrameReadListener iFrameReadListener) {
        this.iFrameReadListener = iFrameReadListener;
    }

    public void setRotate(int rotateInt) {
        this.rotateInt = rotateInt;
    }

    public void setImageSrc(byte[] image) {
        this.imageSrc = image;
    }

    public void setTemperatureSrc(byte[] temperatureSrc) {
        this.temperatureSrc = temperatureSrc;
    }

    public void setFrameReady(boolean frameReady) {
        isFrameReady = frameReady;
    }

    public boolean isRestart() {
        return isRestart;
    }

    public void setRestart(boolean restart) {
        isRestart = restart;
    }

    private void initUVCCamera() {
        uvcCamera = new ConcreateUVCBuilder()
                .setUVCType(UVCType.USB_UVC)
                .build();

        uvcCamera.setDefaultBandwidth(0.5F);
    }

    private void initIRCMD() {
        if (uvcCamera != null) {
            ircmd = new ConcreteIRCMDBuilder()
                    .setIrcmdType(IRCMDType.USB_IR_256_384)
                    .setIdCamera(uvcCamera.getNativePtr())
                    .build();

            if (ircmd == null) {
                return;
            }
            if (mConnectCallback != null) {
                mConnectCallback.onIRCMDCreate(ircmd);
            }
        }
    }

    public void registerUSB() {
        if (mUSBMonitor != null) {
            mUSBMonitor.register();
        }
    }

    public void unregisterUSB() {
        if (mUSBMonitor != null) {
            mUSBMonitor.unregister();
        }
    }

    private void openUVCCamera(USBMonitor.UsbControlBlock ctrlBlock) {
        if (ctrlBlock.getProductId() == PID_3901) {
            if (syncimage != null) {
                syncimage.type = 1;
            }
        }
        if (uvcCamera == null) {
            initUVCCamera();
        }

        if (uvcCamera.openUVCCamera(ctrlBlock) == 0) {

            if (mConnectCallback != null && uvcCamera != null) {
                mConnectCallback.onCameraOpened(uvcCamera);
            }
        }
    }

    private List<CameraSize> getAllSupportedSize() {
        List<CameraSize> previewList = new ArrayList<>();
        if (uvcCamera != null) {
            previewList = uvcCamera.getSupportedSizeList();
        }
        for (CameraSize size : previewList) {
        }
        return previewList;
    }

    private boolean isIRpid(int devpid) {
        for (int x : pids) {
            if (x == devpid) return true;
        }
        return false;
    }

    private void startPreview() {
        if (ircmd == null) {
            return;
        }
        uvcCamera.setOpenStatus(true);
        uvcCamera.setFrameCallback(iFrameCallback);
        uvcCamera.onStartPreview();

        if (CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT == defaultDataFlowMode ||
                CommonParams.DataFlowMode.IMAGE_OUTPUT == defaultDataFlowMode) {


            setFrameReady(false);
            if (isRestart) {

                if (ircmd.stopPreview(CommonParams.PreviewPathChannel.PREVIEW_PATH0) == 0) {

                    if (ircmd.startPreview(CommonParams.PreviewPathChannel.PREVIEW_PATH0,
                            CommonParams.StartPreviewSource.SOURCE_SENSOR,
                            SharedScreenUtils.getPreviewFPSByDataFlowMode(defaultDataFlowMode),
                            CommonParams.StartPreviewMode.VOC_DVP_MODE,
                            defaultDataFlowMode) == 0) {
                        handleStartPreviewComplete();
                    }
                } else {
                }
            } else {
                handleStartPreviewComplete();
            }
        } else {

            setFrameReady(false);
            if (isRestart) {
                if (ircmd.stopPreview(CommonParams.PreviewPathChannel.PREVIEW_PATH0) == 0) {
                    if (ircmd.startPreview(CommonParams.PreviewPathChannel.PREVIEW_PATH0,
                            CommonParams.StartPreviewSource.SOURCE_SENSOR,
                            SharedScreenUtils.getPreviewFPSByDataFlowMode(defaultDataFlowMode),
                            CommonParams.StartPreviewMode.VOC_DVP_MODE, defaultDataFlowMode) == 0) {
                        try {

                            Thread.sleep(1500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (ircmd.startY16ModePreview(CommonParams.PreviewPathChannel.PREVIEW_PATH0,
                                SharedFileUtils.getY16SrcTypeByDataFlowMode(defaultDataFlowMode)) == 0) {
                            handleStartPreviewComplete();
                        } else {
                        }
                    } else {
                    }
                } else {
                }
            } else {

                boolean isTempReplacedWithTNREnabled = ircmd.isTempReplacedWithTNREnabled(DeviceType.P2);
                if (isTempReplacedWithTNREnabled) {

                    if (ircmd.stopPreview(CommonParams.PreviewPathChannel.PREVIEW_PATH0) == 0) {
                        if (ircmd.startPreview(CommonParams.PreviewPathChannel.PREVIEW_PATH0,
                                CommonParams.StartPreviewSource.SOURCE_SENSOR,
                                SharedScreenUtils.getPreviewFPSByDataFlowMode(CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT),
                                CommonParams.StartPreviewMode.VOC_DVP_MODE,
                                CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT) == 0) {
                            try {

                                Thread.sleep(1500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (ircmd.startY16ModePreview(CommonParams.PreviewPathChannel.PREVIEW_PATH0,
                                    SharedFileUtils.getY16SrcTypeByDataFlowMode(CommonParams.DataFlowMode.TNR_OUTPUT)) == 0) {
                                handleStartPreviewComplete();
                            } else {
                            }
                        } else {
                        }
                    } else {
                    }
                } else {

                    if (ircmd.stopPreview(CommonParams.PreviewPathChannel.PREVIEW_PATH0) == 0) {
                        if (ircmd.startPreview(CommonParams.PreviewPathChannel.PREVIEW_PATH0,
                                CommonParams.StartPreviewSource.SOURCE_SENSOR,
                                SharedScreenUtils.getPreviewFPSByDataFlowMode(defaultDataFlowMode),
                                CommonParams.StartPreviewMode.VOC_DVP_MODE, defaultDataFlowMode) == 0) {
                            try {

                                Thread.sleep(1500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (ircmd.startY16ModePreview(CommonParams.PreviewPathChannel.PREVIEW_PATH0,
                                    SharedFileUtils.getY16SrcTypeByDataFlowMode(defaultDataFlowMode)) == 0) {
                                handleStartPreviewComplete();
                            } else {
                            }
                        } else {
                        }
                    } else {
                    }
                }
            }
        }
    }

    public void stopPreview() {
        if (uvcCamera != null) {
            if (uvcCamera.getOpenStatus()) {
                uvcCamera.onStopPreview();
            }
            uvcCamera.setFrameCallback(null);
            final UVCCamera camera;
            camera = uvcCamera;
            uvcCamera = null;

            if (ircmd != null) {
                ircmd.onDestroy();
                ircmd = null;
            }

            SystemClock.sleep(200);

            camera.onDestroyPreview();

        }
    }

    private void handleStartPreviewComplete() {

    }

    public interface IFrameCallBackListener {
        void updateData();
    }

    public interface IFrameReadListener {
        void frameRead();
    }

}


