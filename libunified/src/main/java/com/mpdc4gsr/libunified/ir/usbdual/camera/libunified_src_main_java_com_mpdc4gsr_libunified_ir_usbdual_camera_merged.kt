// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\ir\usbdual\camera' directory and its subdirectories.
// Total files: 8 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ir\usbdual\camera\BaseDualView.java =====

package com.mpdc4gsr.libunified.ir.usbdual.camera;

import com.energy.iruvc.dual.DualUVCCamera;
import com.mpdc4gsr.libunified.ir.usbdual.Const;

import java.util.ArrayList;

public abstract class BaseDualView {

    public DualUVCCamera dualUVCCamera;
    public byte[] vlData;
    public byte[] vlARGBData;
    protected ArrayList<OnFrameCallback> onFrameCallbacks;
    protected int fusionLength;
    protected int irSize;
    protected int vlSize;
    protected int remapTempSize;
    protected byte[] remapTempData;
    protected byte[] mixData;
    protected byte[] normalTempData;
    protected byte[] mixDataRotate;
    protected byte[] irData;

    public BaseDualView() {
        onFrameCallbacks = new ArrayList<>();
        fusionLength = Const.DUAL_WIDTH * Const.DUAL_HEIGHT * 4;
        irSize = Const.IR_WIDTH * Const.IR_HEIGHT;
        vlSize = Const.VL_WIDTH * Const.VL_HEIGHT * 3;
        remapTempSize = Const.DUAL_WIDTH * Const.DUAL_HEIGHT * 2;
        remapTempData = new byte[Const.DUAL_WIDTH * Const.DUAL_HEIGHT * 2];
        mixData = new byte[fusionLength];
        normalTempData = new byte[irSize * 2];
        irData = new byte[irSize * 2];
        vlData = new byte[vlSize];
        vlARGBData = new byte[fusionLength];
    }

    public void addFrameCallback(OnFrameCallback onFrameCallback) {
        onFrameCallbacks.add(onFrameCallback);
    }

    public void removeFrameCallback(OnFrameCallback onFrameCallback) {
        onFrameCallbacks.remove(onFrameCallback);
    }

    public interface OnFrameCallback {
        void onFame(byte[] mixData, byte[] remapTempData, double fpsText);
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ir\usbdual\camera\BaseParamDualView.java =====

package com.mpdc4gsr.libunified.ir.usbdual.camera;

public class BaseParamDualView {
    protected int mIrWidth;
    protected int mIrHeight;
    protected int mVlWidth;
    protected int mVlHeight;
    protected int mDualWidth;
    protected int mDualHeight;

    public BaseParamDualView(int mIrWidth, int mIrHeight, int mVlWidth, int mVlHeight, int mDualWidth, int mDualHeight) {
        this.mIrWidth = mIrWidth;
        this.mIrHeight = mIrHeight;
        this.mVlWidth = mVlWidth;
        this.mVlHeight = mVlHeight;
        this.mDualWidth = mDualWidth;
        this.mDualHeight = mDualHeight;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ir\usbdual\camera\DualViewWithExternalCameraCommonApi.java =====

package com.mpdc4gsr.libunified.ir.usbdual.camera;

import static com.mpdc4gsr.libunified.ir.usbdual.camera.IFrameData.FRAME_LEN;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.ViewGroup;

import com.energy.commonlibrary.view.SurfaceNativeWindow;
import com.energy.iruvc.dual.ConcreateDualBuilder;
import com.energy.iruvc.dual.DualType;
import com.energy.iruvc.dual.DualUVCCamera;
import com.energy.iruvc.sdkisp.LibIRParse;
import com.energy.iruvc.sdkisp.LibIRProcess;
import com.energy.iruvc.utils.*;
import com.energy.iruvc.uvc.UVCCamera;
import com.mpdc4gsr.libunified.ir.usbdual.Const;
import com.mpdc4gsr.libunified.ir.utils.OpencvTools;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Locale;

public class DualViewWithExternalCameraCommonApi extends BaseDualView {

    public static final int MULTIPLE = 2;
    private final String TAG = "DualViewWithExternalCameraCommonApi";
    private final IFrameCallback iFrameCallback;
    private final IIRFrameCallback irFrameCallback;
    private final byte[] amplifyMixRotateArray;
    private final byte[] amplifyIRRotateArray;
    public SurfaceView cameraview;
    public boolean isRun = true;
    public int count = 0;
    public boolean auto_gain_switch = false;
    public boolean auto_gain_switch_running = true;
    public boolean auto_over_protect = false;
    public Bitmap bitmap;
    public Bitmap supIROlyNoFusionBitmap;
    public Bitmap supMixBitmap;
    public Bitmap supIROlyBitmap;
    public byte[] frameData = new byte[FRAME_LEN];
    public byte[] frameIrAndTempData = new byte[192 * 256 * 4];
    public int rotate = 180;
    private DualUVCCamera dualUVCCamera;
    private long timestart = 0;
    private double fps = 0;
    private LibIRProcess.AutoGainSwitchInfo_t auto_gain_switch_info = new LibIRProcess.AutoGainSwitchInfo_t();
    private LibIRProcess.GainSwitchParam_t gain_switch_param = new LibIRProcess.GainSwitchParam_t();
    private CommonParams.GainStatus gainStatus = CommonParams.GainStatus.HIGH_GAIN;
    private boolean valid = false;
    private Bitmap mScaledBitmap;
    private Handler handler;
    private boolean isUseIRISP = false;
    private SurfaceNativeWindow mSurfaceNativeWindow;
    private Surface mSurface;
    private DualCameraParams.FusionType mCurrentFusionType;
    private boolean firstFrame = false;
    private byte[] irRGBAData;
    private byte[] preIrData;
    private byte[] preTempData;
    private byte[] preIrARGBData;
    private volatile boolean isOpenAmplify = false;
    private boolean saveData = false;

    public DualViewWithExternalCameraCommonApi(SurfaceView cameraview, UVCCamera irUVCCamera,
                                               CommonParams.DataFlowMode dataFlowMode,
                                               int irCameraWidth, int irCameraHeight, int vlCameraWidth, int vlCameraHeight,
                                               int dualCameraWidth, int dualCameraHeight,
                                               boolean isUseIRISP, int rotate, IIRFrameCallback irFrameCallback) {
        Const.CAMERA_WIDTH = vlCameraWidth;
        Const.CAMERA_HEIGHT = vlCameraHeight;
        Const.IR_WIDTH = irCameraHeight;
        Const.IR_HEIGHT = irCameraWidth;
        Const.VL_WIDTH = vlCameraHeight;
        Const.VL_HEIGHT = vlCameraWidth;
        Const.DUAL_WIDTH = dualCameraWidth;
        Const.DUAL_HEIGHT = dualCameraHeight;
        this.rotate = rotate;
        onFrameCallbacks = new ArrayList<>();
        fusionLength = Const.DUAL_WIDTH * Const.DUAL_HEIGHT * 4;
        irSize = Const.IR_WIDTH * Const.IR_HEIGHT;
        vlSize = Const.VL_WIDTH * Const.VL_HEIGHT * 3;
        remapTempSize = Const.DUAL_WIDTH * Const.DUAL_HEIGHT * 2;
        remapTempData = new byte[Const.DUAL_WIDTH * Const.DUAL_HEIGHT * 2];
        mixData = new byte[fusionLength];
        normalTempData = new byte[irSize * 2];
        irData = new byte[irSize * 2];
        vlData = new byte[vlSize];
        vlARGBData = new byte[fusionLength];
        amplifyMixRotateArray = new byte[fusionLength * MULTIPLE * MULTIPLE];
        amplifyIRRotateArray = new byte[irData.length * MULTIPLE * MULTIPLE];
        this.irFrameCallback = irFrameCallback;

        this.isUseIRISP = isUseIRISP;
        this.cameraview = cameraview;
        bitmap = Bitmap.createBitmap(dualCameraWidth, dualCameraHeight, Bitmap.Config.ARGB_8888);
        supIROlyNoFusionBitmap = Bitmap.createBitmap(irCameraWidth * MULTIPLE,
                irCameraHeight * MULTIPLE, Bitmap.Config.ARGB_8888);
        supIROlyBitmap = Bitmap.createBitmap(irCameraWidth,
                irCameraHeight, Bitmap.Config.ARGB_8888);
        supMixBitmap = Bitmap.createBitmap(Const.DUAL_WIDTH * MULTIPLE,
                Const.DUAL_HEIGHT * MULTIPLE, Bitmap.Config.ARGB_8888);

        ConcreateDualBuilder concreateDualBuilder = new ConcreateDualBuilder();
        dualUVCCamera = concreateDualBuilder
                .setDualType(DualType.USB_DUAL)

                .setIRSize(Const.IR_WIDTH, Const.IR_HEIGHT)
                .setVLSize(Const.VL_WIDTH, Const.VL_HEIGHT)
                .setDualSize(Const.DUAL_HEIGHT, Const.DUAL_WIDTH)
                .setDataFlowMode(dataFlowMode)
                .setPreviewCameraStyle(CommonParams.PreviewCameraStyle.EXTERNAL_CAMERA)
                .setDeviceStyle(CommonParams.DeviceStyle.ALL_IN_ONE)
                .setUseDualGPU(false)

                .setMultiThreadHandleDualEnable(false)
                .build();
        DualCameraParams.TypeLoadParameters rotateT = DualCameraParams.TypeLoadParameters.ROTATE_0;
        if (rotate == 0) {
            rotateT = DualCameraParams.TypeLoadParameters.ROTATE_0;
        } else if (rotate == 90) {
            rotateT = DualCameraParams.TypeLoadParameters.ROTATE_90;
        } else if (rotate == 180) {
            rotateT = DualCameraParams.TypeLoadParameters.ROTATE_180;
        } else if (rotate == 270) {
            rotateT = DualCameraParams.TypeLoadParameters.ROTATE_270;
        }
        dualUVCCamera.setImageRotate(rotateT);
        dualUVCCamera.addIrUVCCamera(irUVCCamera);
        mSurfaceNativeWindow = new SurfaceNativeWindow();

        gain_switch_param.above_pixel_prop = 0.1f;
        gain_switch_param.above_temp_data = (int) ((130 + 273.15) * 16 * 4);
        gain_switch_param.below_pixel_prop = 0.95f;
        gain_switch_param.below_temp_data = (int) ((110 + 273.15) * 16 * 4);
        auto_gain_switch_info.switch_frame_cnt = 5 * 15;
        auto_gain_switch_info.waiting_frame_cnt = 7 * 15;

        int low_gain_over_temp_data = (int) ((550 + 273.15) * 16 * 4);
        int high_gain_over_temp_data = (int) ((110 + 273.15) * 16 * 4);
        float pixel_above_prop = 0.02f;
        int switch_frame_cnt = 7 * 15;
        int close_frame_cnt = 10 * 15;

        LibIRProcess.ImageRes_t imageRes = new LibIRProcess.ImageRes_t();
        imageRes.height = (char) (192);
        imageRes.width = (char) 256;

        irRGBAData = new byte[irSize * 4];
        preIrData = new byte[irSize * 2];
        preTempData = new byte[irSize * 2];
        preIrARGBData = new byte[irSize * 2 * 2];
        ;
        iFrameCallback = new IFrameCallback() {

            @Override
            public void onFrame(byte[] frame) {
                if (frame.length == 1) {
                    if (handler != null) {
                        handler.sendEmptyMessage(Const.RESTART_USB);
                    }
                    Log.d(TAG, "RESTART_USB");
                    return;
                }

                count++;
                if (count == 100) {
                    count = 0;
                    long currentTimeMillis = System.currentTimeMillis();
                    if (timestart != 0) {
                        long timeuse = currentTimeMillis - timestart;
                        fps = 100 * 1000 / (timeuse + 0.0);
                    }
                    timestart = currentTimeMillis;
                    Log.d(TAG, "frame.length = " + frame.length + " fps=" + String.format(Locale.US, "%.1f", fps) +
                            " dataFlowMode = " + dataFlowMode);
                }
                System.arraycopy(frame, 0, mixData, 0, fusionLength);
                System.arraycopy(frame, fusionLength, irData, 0, irSize * 2);
                System.arraycopy(frame, fusionLength + irSize * 2, normalTempData, 0, irSize * 2);

                System.arraycopy(frame, fusionLength + irSize * 4, remapTempData, 0,
                        Const.DUAL_WIDTH * Const.DUAL_HEIGHT * 2);

                System.arraycopy(frame, fusionLength + irSize * 4 + Const.DUAL_WIDTH * Const.DUAL_HEIGHT * 2, vlData,
                        0, vlSize);
                System.arraycopy(frame, 0, frameData, 0, FRAME_LEN);

                System.arraycopy(frame, dualCameraWidth * dualCameraHeight * 4, frameIrAndTempData, 0, frameIrAndTempData.length);

                if (mCurrentFusionType == DualCameraParams.FusionType.IROnlyNoFusion) {
                    for (OnFrameCallback onFrameCallback : onFrameCallbacks) {
                        onFrameCallback.onFame(mixData, normalTempData, fps);
                    }
                } else {
                    for (OnFrameCallback onFrameCallback : onFrameCallbacks) {
                        onFrameCallback.onFame(mixData, remapTempData, fps);
                    }
                }

                mSurface = cameraview.getHolder().getSurface();

                if (mCurrentFusionType == DualCameraParams.FusionType.IROnlyNoFusion) {
                    LibIRParse.converyArrayYuv422ToARGB(irData, Const.IR_WIDTH * Const.IR_HEIGHT, irRGBAData);
                    if (isOpenAmplify) {
                        OpencvTools.supImage(irData, Const.IR_HEIGHT, Const.IR_WIDTH, amplifyIRRotateArray);
                        if (mSurface != null) {
                            mSurfaceNativeWindow.onDrawFrame(mSurface, amplifyIRRotateArray,
                                    Const.IR_WIDTH * MULTIPLE,
                                    Const.IR_HEIGHT * MULTIPLE);
                        }
                    } else {
                        if (mSurface != null) {
                            mSurfaceNativeWindow.onDrawFrame(mSurface, irRGBAData, Const.IR_HEIGHT, Const.IR_WIDTH);
                        }
                    }
                } else {
                    if (isOpenAmplify) {
                        if (mCurrentFusionType == DualCameraParams.FusionType.IROnly) {
                            OpencvTools.supImageMix(mixData, Const.DUAL_HEIGHT, Const.DUAL_WIDTH, mixData);
                            if (mSurface != null) {
                                mSurfaceNativeWindow.onDrawFrame(mSurface, mixData, Const.DUAL_WIDTH, Const.DUAL_HEIGHT);
                            }
                        } else {
                            OpencvTools.supImage(mixData, Const.DUAL_HEIGHT, Const.DUAL_WIDTH, amplifyMixRotateArray);
                            if (mSurface != null) {
                                mSurfaceNativeWindow.onDrawFrame(mSurface, amplifyMixRotateArray,
                                        Const.DUAL_WIDTH * MULTIPLE,
                                        Const.DUAL_HEIGHT * MULTIPLE);
                            }
                        }
                    } else {
                        if (mSurface != null) {
                            mSurfaceNativeWindow.onDrawFrame(mSurface, mixData, Const.DUAL_WIDTH, Const.DUAL_HEIGHT);
                        }
                    }
                }

                if (!isUseIRISP && !firstFrame) {
                    firstFrame = true;
                    if (handler != null) {
                        handler.sendEmptyMessage(Const.HIDE_LOADING);
                    }
                }

                if (dataFlowMode == CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT) {
                    System.arraycopy(frame, fusionLength + irSize * 2, normalTempData, 0, irSize * 2);
                    if (auto_gain_switch && auto_gain_switch_running) {
                        USBMonitorManager.getInstance().getIrcmd().autoGainSwitch(normalTempData, imageRes,
                                auto_gain_switch_info, gain_switch_param, new AutoGainSwitchCallback() {
                                    @Override
                                    public void onAutoGainSwitchState(CommonParams.PropTPDParamsValue.GAINSELStatus gainselStatus) {
                                        Log.d(TAG, "onAutoGainSwitchState = " + gainselStatus.getValue());
                                        auto_gain_switch_running = false;
                                        resetAutoGainInfo();
                                    }

                                    @Override
                                    public void onAutoGainSwitchResult(CommonParams.PropTPDParamsValue.GAINSELStatus gainselStatus, int result) {
                                        Log.d(TAG, "onAutoGainSwitchResult = " + gainselStatus.getValue() + "  result" +
                                                ":" + result);
                                        auto_gain_switch_running = true;
                                    }
                                });
                    }

                    if (auto_over_protect) {
                        USBMonitorManager.getInstance().getIrcmd().avoidOverexposure(false, gainStatus,
                                normalTempData, imageRes, low_gain_over_temp_data, high_gain_over_temp_data,
                                pixel_above_prop, switch_frame_cnt, close_frame_cnt, new AvoidOverexposureCallback() {
                                    @Override
                                    public void onAvoidOverexposureState(boolean avoidOverexpol) {
                                        Log.d(TAG, "onAvoidOverexposureState = " + avoidOverexpol);
                                    }
                                });
                    }
                }

            }
        };
    }

    public boolean isOpenAmplify() {
        return isOpenAmplify;
    }

    public void setOpenAmplify(boolean openAmplify) {
        isOpenAmplify = openAmplify;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public void resetAutoGainInfo() {
        auto_gain_switch_info.switched_flag = 0;
        auto_gain_switch_info.cur_switched_cnt = 0;
        auto_gain_switch_info.cur_detected_low_cnt = 0;
        auto_gain_switch_info.cur_detected_high_cnt = 0;
    }

    public void startPreview() {

        switchIrPreDataHandleEnable(true);
        dualUVCCamera.setFrameCallback(iFrameCallback);
        dualUVCCamera.onStartPreview();
        firstFrame = false;
    }

    public DualUVCCamera getDualUVCCamera() {
        return dualUVCCamera;
    }

    public void stopPreview() {
        dualUVCCamera.setFrameCallback(null);
        dualUVCCamera.onStopPreview();
        SystemClock.sleep(200);
        dualUVCCamera.onDestroy();
    }

    public void switchIrPreDataHandleEnable(boolean enable) {
        dualUVCCamera.setIrDataPreHandleEnable(enable);
        dualUVCCamera.setIrFrameCallback(enable ? irFrameCallback : null);
    }

    public byte[] getRemapTempData() {
        return remapTempData;
    }

    public Bitmap getScaledBitmap() {
        if (isOpenAmplify) {
            if (mCurrentFusionType == DualCameraParams.FusionType.IROnlyNoFusion) {

                supIROlyNoFusionBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(amplifyIRRotateArray, 0,
                        supIROlyNoFusionBitmap.getWidth() * supIROlyNoFusionBitmap.getHeight() * 4));
                mScaledBitmap = Bitmap.createScaledBitmap(supIROlyNoFusionBitmap,
                        ((ViewGroup) cameraview.getParent()).getWidth(),
                        ((ViewGroup) cameraview.getParent()).getHeight(), true);
            } else if (mCurrentFusionType == DualCameraParams.FusionType.IROnly) {
                bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(mixData, 0, bitmap.getWidth() * bitmap.getHeight() * 4));
                mScaledBitmap = Bitmap.createScaledBitmap(bitmap, ((ViewGroup) cameraview.getParent()).getWidth(),
                        ((ViewGroup) cameraview.getParent()).getHeight(), true);
            } else {
                supMixBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(mixData, 0, supMixBitmap.getWidth() * supMixBitmap.getHeight() * 4));
                mScaledBitmap = Bitmap.createScaledBitmap(supMixBitmap, ((ViewGroup) cameraview.getParent()).getWidth(),
                        ((ViewGroup) cameraview.getParent()).getHeight(), true);
            }
        } else {
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(mixData, 0, bitmap.getWidth() * bitmap.getHeight() * 4));
            mScaledBitmap = Bitmap.createScaledBitmap(bitmap, ((ViewGroup) cameraview.getParent()).getWidth(),
                    ((ViewGroup) cameraview.getParent()).getHeight(), true);
        }
        return mScaledBitmap;
    }

    public void saveData() {
        saveData = true;
    }

    public void setGainStatus(CommonParams.GainStatus gainStatus) {
        this.gainStatus = gainStatus;
    }

    public void setCurrentFusionType(DualCameraParams.FusionType currentFusionType) {
        this.mCurrentFusionType = currentFusionType;
        if (dualUVCCamera != null) {
            dualUVCCamera.setFusion(currentFusionType);
        }
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ir\usbdual\camera\DualViewWithManualAlignExternalCamera.java =====

package com.mpdc4gsr.libunified.ir.usbdual.camera;

import static com.mpdc4gsr.libunified.ir.usbdual.Const.HIDE_LOADING;

import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;

import com.energy.commonlibrary.view.SurfaceNativeWindow;
import com.energy.iruvc.dual.ConcreateDualBuilder;
import com.energy.iruvc.dual.DualType;
import com.energy.iruvc.dual.DualUVCCamera;
import com.energy.iruvc.utils.CommonParams;
import com.energy.iruvc.utils.IAlignCallback;
import com.energy.iruvc.utils.IFrameCallback;
import com.energy.iruvc.uvc.UVCCamera;

public class DualViewWithManualAlignExternalCamera extends BaseParamDualView {
    private final String TAG = "DualViewWithManualAlignExternalCamera";
    public SurfaceView cameraview;
    private DualUVCCamera dualUVCCamera;
    private Handler handler;

    private byte[] mixData;
    private int fusionLength;

    private boolean firstFrame = false;
    private SurfaceNativeWindow mSurfaceNativeWindow;
    private Surface mSurface;
    private IFrameCallback iFrameCallback = new IFrameCallback() {

        @Override
        public void onFrame(byte[] frame) {
            Log.d(TAG, "onFrame");
            System.arraycopy(frame, 0, mixData, 0, fusionLength);

            mSurface = cameraview.getHolder().getSurface();
            if (mSurface != null) {
                mSurfaceNativeWindow.onDrawFrame(mSurface, mixData, mDualWidth, mDualHeight);
            }

            if (!firstFrame) {
                firstFrame = true;
                if (handler != null) {
                    handler.sendEmptyMessage(HIDE_LOADING);
                }
            }
        }
    };
    private IAlignCallback iAlignCallback = new IAlignCallback() {
        @Override
        public void onFrame(byte[] frame) {
            System.arraycopy(frame, 0, mixData, 0, fusionLength);
            mSurface = cameraview.getHolder().getSurface();
            if (mSurface != null) {
                mSurfaceNativeWindow.onDrawFrame(mSurface, mixData, mDualWidth, mDualHeight);
            }
        }
    };

    public DualViewWithManualAlignExternalCamera(int irWidth, int irHeight, int vlWidth, int vlHeight, int dualWidth, int dualHeight,
                                                 SurfaceView cameraview, UVCCamera iruvc, CommonParams.DataFlowMode dataFlowMode) {
        super(irWidth, irHeight, vlWidth, vlHeight, dualWidth, dualHeight);
        this.cameraview = cameraview;

        ConcreateDualBuilder concreateDualBuilder = new ConcreateDualBuilder();
        dualUVCCamera = concreateDualBuilder
                .setDualType(DualType.USB_DUAL)
                .setIRSize(mIrWidth, mIrHeight)
                .setVLSize(mVlWidth, mVlHeight)
                .setDualSize(mDualHeight, mDualWidth)
                .setDataFlowMode(dataFlowMode)
                .setPreviewCameraStyle(CommonParams.PreviewCameraStyle.EXTERNAL_CAMERA)
                .setDeviceStyle(CommonParams.DeviceStyle.ALL_IN_ONE)
                .setUseDualGPU(false)
                .setMultiThreadHandleDualEnable(false)
                .build();

        mSurfaceNativeWindow = new SurfaceNativeWindow();

        dualUVCCamera.addIrUVCCamera(iruvc);
        fusionLength = mDualWidth * mDualHeight * 4;
        mixData = new byte[fusionLength];

    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public void startPreview() {
        dualUVCCamera.setFrameCallback(iFrameCallback);
        dualUVCCamera.onStartPreview();
        firstFrame = false;
    }

    public DualUVCCamera getDualUVCCamera() {
        return dualUVCCamera;
    }

    public void stopPreview() {
        dualUVCCamera.setFrameCallback(null);
        dualUVCCamera.onStopPreview();
    }

    public void destroyPreview() {
        dualUVCCamera.onDestroy();
    }

    public void startAlign() {
        dualUVCCamera.setAlignCallback(iAlignCallback);
        dualUVCCamera.startManualAlign();
    }

    public void onDraw() {
        if (dualUVCCamera != null) {
            mSurface = cameraview.getHolder().getSurface();
            if (mSurface != null) {
                mSurfaceNativeWindow.onDrawFrame(mSurface, mixData, mDualWidth, mDualHeight);
            }
        }
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ir\usbdual\camera\IFrameData.java =====

package com.mpdc4gsr.libunified.ir.usbdual.camera;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mpdc4gsr.libunified.ir.usbdual.Const;

public class IFrameData {

    public static int FUSION_LEN = Const.DUAL_WIDTH * Const.DUAL_HEIGHT * 4;

    public static int ORIGINAL_LEN = Const.IR_WIDTH * Const.IR_HEIGHT * 2;

    public static int REMAP_TEMP_LEN = Const.DUAL_WIDTH * Const.DUAL_HEIGHT * 2;

    public static int LIGHT_LEN = Const.VL_WIDTH * Const.VL_HEIGHT * 3;

    public static int P_IN_P_LEN = Const.DUAL_WIDTH * Const.DUAL_HEIGHT * 4;

    public static int FRAME_LEN = FUSION_LEN + ORIGINAL_LEN + ORIGINAL_LEN + REMAP_TEMP_LEN + LIGHT_LEN + P_IN_P_LEN;

    public static byte[] readFusionData(@NonNull byte[] frame, @Nullable byte[] fusionData) {
        if (fusionData == null) {
            fusionData = new byte[FUSION_LEN];
        }
        System.arraycopy(frame, 0, fusionData, 0, fusionData.length);
        return fusionData;
    }

    public static byte[] readNorIRData(@NonNull byte[] frame, @Nullable byte[] irData) {
        if (irData == null) {
            irData = new byte[ORIGINAL_LEN];
        }
        System.arraycopy(frame, FUSION_LEN, irData, 0, irData.length);
        return irData;
    }

    public static byte[] readNorTempData(@NonNull byte[] frame, @Nullable byte[] norTempData) {
        if (norTempData == null) {
            norTempData = new byte[ORIGINAL_LEN];
        }
        System.arraycopy(frame, FUSION_LEN + ORIGINAL_LEN, norTempData, 0, norTempData.length);
        return norTempData;
    }

    public static byte[] readRemapTempData(@NonNull byte[] frame, @Nullable byte[] remapTempData) {
        if (remapTempData == null) {
            remapTempData = new byte[REMAP_TEMP_LEN];
        }
        System.arraycopy(frame, FUSION_LEN + ORIGINAL_LEN + ORIGINAL_LEN, remapTempData, 0, remapTempData.length);
        return remapTempData;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ir\usbdual\camera\IRUVCDual.java =====

package com.mpdc4gsr.libunified.ir.usbdual.camera;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.TextureView;

import com.energy.iruvc.dual.DualUVCCamera;
import com.energy.iruvc.ircmd.ConcreteIRCMDBuilder;
import com.energy.iruvc.ircmd.IRCMD;
import com.energy.iruvc.ircmd.IRCMDType;
import com.energy.iruvc.sdkisp.LibIRProcess;
import com.energy.iruvc.usb.DeviceFilter;
import com.energy.iruvc.usb.USBMonitor;
import com.energy.iruvc.utils.CommonParams;
import com.energy.iruvc.utils.IFrameCallback;
import com.energy.iruvc.utils.SynchronizedBitmap;
import com.energy.iruvc.uvc.*;
import com.mpdc4gsr.libunified.R;
import com.mpdc4gsr.libunified.ir.usbdual.Const;

import java.util.ArrayList;
import java.util.List;

public class IRUVCDual {
    private final Context mContext;
    public String TAG = "IRUVC";
    public DualUVCCamera dualUVCCamera;
    public boolean rotate = false;
    private IFrameCallback iFrameCallback;
    private UVCCamera uvcCamera;
    private IRCMD ircmd;
    private USBMonitor mUSBMonitor;
    private int cameraWidth;
    private int cameraHeight;
    private byte[] image;
    private byte[] temperature;
    private SynchronizedBitmap syncimage;
    private TextureView cameraview;
    private int status = 0;
    private boolean isRequest = false;
    private int mPid = 0;
    private int vid = 0;
    private int mFps;
    private boolean auto_gain_switch = false;
    private boolean auto_over_protect = false;
    private LibIRProcess.AutoGainSwitchInfo_t auto_gain_switch_info = new LibIRProcess.AutoGainSwitchInfo_t();
    private LibIRProcess.GainSwitchParam_t gain_switch_param = new LibIRProcess.GainSwitchParam_t();
    private boolean isUseIRISP;
    private boolean isUseGPU = false;
    private CommonParams.GainStatus gainStatus = CommonParams.GainStatus.HIGH_GAIN;
    private CommonParams.GainMode gainMode = CommonParams.GainMode.GAIN_MODE_HIGH_LOW;
    private short[] nuc_table_high = new short[8192];
    private short[] nuc_table_low = new short[8192];
    private boolean isGetNucFromFlash;
    private byte[] priv_high = new byte[1201];
    private byte[] priv_low = new byte[1201];
    private short[] kt_high = new short[1201];
    private short[] kt_low = new short[1201];
    private short[] bt_high = new short[1201];
    private short[] bt_low = new short[1201];
    private int[] curVtemp = new int[1];
    private ConnectCallback mConnectCallback;
    private CommonParams.PseudoColorType pseudocolorMode;
    private Handler handler;

    public IRUVCDual(int cameraWidth, int cameraHeight, Context context, SynchronizedBitmap syncimage,
                     ConnectCallback connectCallback) {
        this.cameraWidth = cameraWidth;
        this.cameraHeight = cameraHeight;
        this.mContext = context;
        this.syncimage = syncimage;
        this.mConnectCallback = connectCallback;
        initUVCCamera(cameraWidth, cameraHeight);
        mUSBMonitor = new USBMonitor(context, new USBMonitor.OnDeviceConnectListener() {

            @Override
            public void onAttach(UsbDevice device) {
                if (mPid != 0) {
                    return;
                }
                Log.d(TAG, "onAttach");
                if (!isRequest) {
                    isRequest = true;
                    requestPermission(0);
                }

            }

            @Override
            public void onGranted(UsbDevice usbDevice, boolean granted) {

            }

            @Override
            public void onConnect(final UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
                Log.w(TAG, "onConnect");
                if (createNew) {
                    if (mConnectCallback != null && uvcCamera != null) {
                        Log.d(TAG, "onCameraOpened");
                        mConnectCallback.onCameraOpened(uvcCamera);
                    }
                    Const.isDeviceConnected = true;
                    handleUSBConnect(ctrlBlock);
                }
            }

            @Override
            public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
                Log.w(TAG, "onDisconnect");
                Const.isDeviceConnected = false;
            }

            @Override
            public void onDettach(UsbDevice device) {
                Log.w(TAG, "onDettach" + isRequest);
                Const.isDeviceConnected = false;
                if (isRequest) {
                    isRequest = false;
                    stopPreview();
                }
            }

            @Override
            public void onCancel(UsbDevice device) {
                Const.isDeviceConnected = false;
            }
        });

        gain_switch_param.above_pixel_prop = 0.1f;
        gain_switch_param.above_temp_data = (int) ((130 + 273.15) * 16 * 4);
        gain_switch_param.below_pixel_prop = 0.95f;
        gain_switch_param.below_temp_data = (int) ((110 + 273.15) * 16 * 4);
        auto_gain_switch_info.switch_frame_cnt = 5 * 15;
        auto_gain_switch_info.waiting_frame_cnt = 7 * 15;

        int low_gain_over_temp_data = (int) ((550 + 273.15) * 16 * 4);
        int high_gain_over_temp_data = (int) ((100 + 273.15) * 16 * 4);
        float pixel_above_prop = 0.02f;
        int switch_frame_cnt = 7 * 15;
        int close_frame_cnt = 10 * 15;
    }

    public IRUVCDual(int cameraWidth, int cameraHeight, Context context, int pid, int fps,
                     ConnectCallback connectCallback, IFrameCallback iFrameCallback) {
        this.mPid = pid;
        this.mFps = fps;
        this.cameraWidth = cameraWidth;
        this.cameraHeight = cameraHeight;
        this.mContext = context;
        this.mConnectCallback = connectCallback;
        this.iFrameCallback = iFrameCallback;

        initUVCCamera(cameraWidth, cameraHeight);

        mUSBMonitor = new USBMonitor(context, new USBMonitor.OnDeviceConnectListener() {

            @Override
            public void onAttach(UsbDevice device) {
                Log.w(TAG, "USBMonitor-onAttach mPid = " + pid + " getProductId = " + device.getProductId());

                if (device.getProductId() != mPid) {
                    return;
                }
                if (uvcCamera == null || !uvcCamera.getOpenStatus()) {
                    mUSBMonitor.requestPermission(device);
                }
            }

            @Override
            public void onGranted(UsbDevice usbDevice, boolean granted) {
                Log.w(TAG, "USBMonitor-onGranted");
            }

            @Override
            public void onDettach(UsbDevice device) {
                Log.w(TAG, "USBMonitor-onDettach mPid = " + pid);
                Const.isDeviceConnected = false;
                if (uvcCamera != null && uvcCamera.getOpenStatus()) {

                    if (handler != null && status != 2) {
                        handler.sendEmptyMessage(Const.RESTART_USB);
                    }
                    status = 2;
                }
            }

            @Override
            public void onConnect(final UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
                Log.w(TAG, "USBMonitor-onConnect mPid = " + pid);
                Log.w(TAG, "USBMonitor-onConnect createNew = " + createNew);
                if (createNew && device.getProductId() == pid) {
                    if (handler != null) {
                        handler.sendEmptyMessage(Const.SHOW_LOADING);
                    }

                    if (mConnectCallback != null && uvcCamera != null) {
                        Log.w(TAG, "USBMonitor-onCameraOpened");
                        mConnectCallback.onCameraOpened(uvcCamera);
                    }
                    Const.isDeviceConnected = true;
                    handleUSBConnect(ctrlBlock);
                    status = 3;
                }
            }

            @Override
            public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
                Log.w(TAG, "USBMonitor-onDisconnect mPid = " + pid);
                Const.isDeviceConnected = false;
                status = 4;
            }

            @Override
            public void onCancel(UsbDevice device) {
                Log.w(TAG, "USBMonitor-onCancel mPid = " + pid);
                Const.isDeviceConnected = false;
            }
        });
    }

    public IRUVCDual(int cameraWidth, int cameraHeight, Context context, SynchronizedBitmap syncimage, int pid, int fps,
                     ConnectCallback connectCallback) {
        this.mPid = pid;
        this.mFps = fps;
        this.cameraWidth = cameraWidth;
        this.cameraHeight = cameraHeight;
        this.mContext = context;
        this.syncimage = syncimage;
        this.mConnectCallback = connectCallback;

        initUVCCamera(cameraWidth, cameraHeight);

        mUSBMonitor = new USBMonitor(context, new USBMonitor.OnDeviceConnectListener() {

            @Override
            public void onAttach(UsbDevice device) {
                Log.w(TAG, "USBMonitor-onAttach mPid = " + pid + " getProductId = " + device.getProductId());

                if (device.getProductId() != mPid) {
                    return;
                }
                if (uvcCamera == null || !uvcCamera.getOpenStatus()) {
                    mUSBMonitor.requestPermission(device);
                }
            }

            @Override
            public void onGranted(UsbDevice usbDevice, boolean granted) {
                Log.w(TAG, "USBMonitor-onGranted");
            }

            @Override
            public void onDettach(UsbDevice device) {
                Log.w(TAG, "USBMonitor-onDettach mPid = " + pid);
                Const.isDeviceConnected = false;
                if (uvcCamera != null && uvcCamera.getOpenStatus()) {

                    if (handler != null && status != 2) {
                        handler.sendEmptyMessage(Const.RESTART_USB);
                    }
                    status = 2;
                }
            }

            @Override
            public void onConnect(final UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
                Log.w(TAG, "USBMonitor-onConnect mPid = " + pid);
                Log.w(TAG, "USBMonitor-onConnect createNew = " + createNew);
                if (createNew && device.getProductId() == pid) {
                    if (handler != null) {
                        handler.sendEmptyMessage(Const.SHOW_LOADING);
                    }

                    if (mConnectCallback != null && uvcCamera != null) {
                        Log.w(TAG, "USBMonitor-onCameraOpened");
                        mConnectCallback.onCameraOpened(uvcCamera);
                    }
                    Const.isDeviceConnected = true;
                    handleUSBConnect(ctrlBlock);
                    status = 3;
                }
            }

            @Override
            public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
                Log.w(TAG, "USBMonitor-onDisconnect mPid = " + pid);
                Const.isDeviceConnected = false;
                status = 4;
            }

            @Override
            public void onCancel(UsbDevice device) {
                Log.w(TAG, "USBMonitor-onCancel mPid = " + pid);
                Const.isDeviceConnected = false;
            }
        });
    }

    public IRUVCDual(int cameraWidth, int cameraHeight, Context context, SynchronizedBitmap syncimage, int pid,
                     ConnectCallback connectCallback, boolean isUseIRISP) {
        this.mPid = pid;
        this.cameraWidth = cameraWidth;
        this.cameraHeight = cameraHeight;
        this.mContext = context;
        this.syncimage = syncimage;
        this.isUseIRISP = isUseIRISP;
        this.mConnectCallback = connectCallback;

        initUVCCamera(cameraWidth, cameraHeight);

        mUSBMonitor = new USBMonitor(context, new USBMonitor.OnDeviceConnectListener() {

            @Override
            public void onAttach(UsbDevice device) {
                Log.w(TAG, "onAttach" + device.getProductId());
                if (pid != 0) {
                    if (uvcCamera == null || !uvcCamera.getOpenStatus()) {
                        Log.w(TAG, "USBMonitor" + "onAttach requestPermission" + pid);
                        mUSBMonitor.requestPermission(device);
                    }
                }
            }

            @Override
            public void onGranted(UsbDevice usbDevice, boolean granted) {
            }

            @Override
            public void onDettach(UsbDevice device) {
                Log.w(TAG, "onDettach");
                if (pid != 0 && device != null) {
                    Const.isDeviceConnected = false;
                    if (uvcCamera != null && uvcCamera.getOpenStatus()) {
                        status = 2;
                    }
                }
            }

            @Override
            public void onConnect(final UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
                Log.w(TAG, "onConnect");
                if (pid != 0) {
                    if (createNew) {
                        if (handler != null) {
                            handler.sendEmptyMessage(Const.SHOW_LOADING);
                        }

                        if (mConnectCallback != null && uvcCamera != null) {
                            Log.d(TAG, "onCameraOpened");
                            mConnectCallback.onCameraOpened(uvcCamera);
                        }
                        Const.isDeviceConnected = true;
                        handleUSBConnect(ctrlBlock);
                        status = 3;
                    }
                }
            }

            @Override
            public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
                Log.w(TAG, "onDisconnect");
                if (pid != 0 && status != 4) {
                    Const.isDeviceConnected = false;
                    status = 4;
                }
            }

            @Override
            public void onCancel(UsbDevice device) {
                Const.isDeviceConnected = false;
            }

        });
    }

    public void setDualUVCCamera(DualUVCCamera dualUVCCamera) {
        this.dualUVCCamera = dualUVCCamera;
    }

    public void setPseudocolorMode(CommonParams.PseudoColorType pseudocolorMode) {
        this.pseudocolorMode = pseudocolorMode;
    }

    public void setCameraview(TextureView cameraview) {
        this.cameraview = cameraview;
    }

    public void setmPid(int mPid) {
        this.mPid = mPid;
    }

    public void setVid(int vid) {
        this.vid = vid;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public void setRotate(boolean rotate) {
        this.rotate = rotate;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public void setTemperature(byte[] temperature) {
        this.temperature = temperature;
    }

    public void initUVCCamera(int cameraWidth, int cameraHeight) {
        Log.i(TAG, "initUVCCamera->cameraWidth = " + cameraWidth + " cameraHeight = " + cameraHeight);

        ConcreateUVCBuilder concreateUVCBuilder = new ConcreateUVCBuilder();
        uvcCamera = concreateUVCBuilder
                .setUVCType(UVCType.USB_UVC)
                .build();
    }

    public UVCCamera getUvcCamera() {
        return uvcCamera;
    }

    public IRCMD getIrcmd() {
        return ircmd;
    }

    public void registerUSB() {
        Log.i(TAG, "registerUSB");
        if (mUSBMonitor != null) {
            mUSBMonitor.register();
        }
    }

    public void unregisterUSB() {
        Log.i(TAG, "unregisterUSB");
        if (mUSBMonitor != null) {
            mUSBMonitor.destroy();
        }
    }

    public List<UsbDevice> getUsbDeviceList() {
        List<DeviceFilter> deviceFilters = DeviceFilter
                .getDeviceFilters(mContext, R.xml.device_filter);
        if (mUSBMonitor == null || deviceFilters == null) {
            return null;
        }

        return mUSBMonitor.getDeviceList(deviceFilters);
    }

    public boolean requestPermission(int index) {
        Log.i(TAG, "requestPermission");
        List<UsbDevice> devList = getUsbDeviceList();
        if (devList == null || devList.size() == 0) {
            return false;
        }
        int count = devList.size();
        if (index >= count) {
            new IllegalArgumentException("index illegal,should be < devList.size()");
        }
        if (mUSBMonitor != null) {
            if (getUsbDeviceList().get(index).getProductId() == mPid) {
                return mUSBMonitor.requestPermission(getUsbDeviceList().get(index));
            }
        }
        return false;
    }

    public void openUVCCamera(USBMonitor.UsbControlBlock ctrlBlock) {
        Log.i(TAG, "openUVCCamera");
        if (ctrlBlock.getProductId() == 0x3901) {
            if (syncimage != null) {
                syncimage.type = 1;
            }
        }
        if (uvcCamera == null) {
            initUVCCamera(cameraWidth, cameraHeight);
        }

        uvcCamera.openUVCCamera(ctrlBlock);
    }

    public void startPreview() {
        Log.w(TAG, "startPreview mPid = " + mPid + " isUseIRISP = " + isUseIRISP);
        uvcCamera.setOpenStatus(true);

        if (iFrameCallback != null) {
            uvcCamera.setFrameCallback(iFrameCallback);
        }
        uvcCamera.onStartPreview();
        if (mPid == 0x5830 || mPid == 0x5840) {

            ircmd.startPreview(CommonParams.PreviewPathChannel.PREVIEW_PATH0,
                    CommonParams.StartPreviewSource.SOURCE_SENSOR,
                    25, CommonParams.StartPreviewMode.VOC_DVP_MODE,
                    CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT);
            ircmd.setPropImageParams(CommonParams.PropImageParams.IMAGE_PROP_SEL_MIRROR_FLIP,
                    Const.IR_MIRROR_FLIP_TYPE);
        }
    }

    private List<CameraSize> getAllSupportedSize() {
        List<CameraSize> previewList = new ArrayList<>();
        if (uvcCamera != null) {
            previewList = uvcCamera.getSupportedSizeList();
        }
        for (CameraSize size : previewList) {
            Log.i(TAG, "SupportedSize : " + size.width + " * " + size.height);
        }
        return previewList;
    }

    public void initIRCMD(List<CameraSize> previewList) {
        for (CameraSize size : previewList) {

        }

        if (uvcCamera != null) {
            ConcreteIRCMDBuilder concreteIRCMDBuilder = new ConcreteIRCMDBuilder();
            ircmd = concreteIRCMDBuilder
                    .setIrcmdType(IRCMDType.USB_IR_256_384)
                    .setIdCamera(uvcCamera.getNativePtr())
                    .build();

            if (mConnectCallback != null) {
                Log.d(TAG, "onIRCMDCreate");
                mConnectCallback.onIRCMDCreate(ircmd);
            }
        }
    }

    private int setPreviewSize(int cameraWidth, int cameraHeight) {
        if (uvcCamera != null) {
            Log.d(TAG, "setUSBPreviewSize mPid = " + mPid + " cameraWidth = " + cameraWidth +
                    " cameraHeight = " + cameraHeight);
            return uvcCamera.setUSBPreviewSize(cameraWidth, cameraHeight);
        }
        return -1;
    }

    public void stopPreview() {
        Log.i(TAG, "stopPreview");
        if (uvcCamera != null) {
            if (uvcCamera.getOpenStatus()) {
                uvcCamera.onStopPreview();
            }
            uvcCamera.setFrameCallback(null);

            SystemClock.sleep(200);

            uvcCamera.onDestroyPreview();
            uvcCamera = null;
        }
    }

    public void setConnectCallback(ConnectCallback mConnectCallback) {
        Log.d(TAG, "setConnectCallback");
        this.mConnectCallback = mConnectCallback;
    }

    private void handleUSBConnect(USBMonitor.UsbControlBlock ctrlBlock) {
        Log.d(TAG, "handleUSBConnect mPid = " + mPid);
        openUVCCamera(ctrlBlock);

        List<CameraSize> previewList = getAllSupportedSize();

        if (mPid == 0x5830 || mPid == 0x5840) {
            initIRCMD(previewList);

            uvcCamera.setDefaultBandwidth(1.0f);
            uvcCamera.setDefaultPreviewMinFps(1);
            uvcCamera.setDefaultPreviewMaxFps(mFps);
        } else {
            Log.d(TAG, "startVLCamera handleUSBConnect mPid = " + mPid + " setDefaultPreviewMode");

            uvcCamera.setDefaultPreviewMode(CommonParams.FRAMEFORMATType.FRAME_FORMAT_MJPEG);

            uvcCamera.setDefaultBandwidth(0.6f);
            uvcCamera.setDefaultPreviewMinFps(1);
            uvcCamera.setDefaultPreviewMaxFps(mFps);
        }

        int result = setPreviewSize(cameraWidth, cameraHeight);
        if (result == 0) {

            Log.d(TAG, "handleUSBConnect setPreviewSize success = ");
            startPreview();
        } else {
            Log.d(TAG, "handleUSBConnect setPreviewSize fail = ");
            stopPreview();
        }

    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ir\usbdual\camera\USBMonitorDualManager.java =====

package com.mpdc4gsr.libunified.ir.usbdual.camera;

import android.hardware.usb.UsbDevice;
import android.os.SystemClock;
import android.util.Log;

import com.energy.iruvc.ircmd.ConcreteIRCMDBuilder;
import com.energy.iruvc.ircmd.IRCMD;
import com.energy.iruvc.ircmd.IRCMDType;
import com.energy.iruvc.usb.USBMonitor;
import com.energy.iruvc.utils.CommonParams;
import com.energy.iruvc.utils.IFrameCallback;
import com.energy.iruvc.uvc.ConcreateUVCBuilder;
import com.energy.iruvc.uvc.UVCCamera;
import com.energy.iruvc.uvc.UVCType;
import com.mpdc4gsr.libunified.compat.ContextProvider;
import com.mpdc4gsr.libunified.ir.usbdual.Const;
import com.mpdc4gsr.libunified.ir.usbdual.inf.OnUSBConnectListener;

import java.util.ArrayList;
import java.util.List;

public class USBMonitorDualManager {

    public static final String TAG = "USBMonitorDualManager";
    private static USBMonitorDualManager mInstance;
    private USBMonitor mUSBMonitor;
    private IRCMD mIrcmd;
    private Object mSyncs = new Object();
    private List<OnUSBConnectListener> mOnUSBConnectListeners = new ArrayList<>();
    private UVCCamera mIrUvcCamera;
    private UVCCamera mVlUvcCamera;
    private boolean mIrOpened;
    private boolean mVlOpened;
    private IFrameCallback mVlIFrameCallback;

    private USBMonitorDualManager() {
    }

    public static synchronized USBMonitorDualManager getInstance() {
        if (mInstance == null) {
            mInstance = new USBMonitorDualManager();
        }
        return mInstance;
    }

    public void addOnUSBConnectListener(OnUSBConnectListener onUSBConnectListener) {
        mOnUSBConnectListeners.add(onUSBConnectListener);
    }

    public void removeOnUSBConnectListener(OnUSBConnectListener onUSBConnectListener) {
        mOnUSBConnectListeners.remove(onUSBConnectListener);
    }

    public void init(int irPid, int irFPS, int irWidth, int irHeight, float irBandWith,
                     int vlPid, int vlFPS, int vlWidth, int vlHeight, float vlBandWith, IFrameCallback frameCallback) {
        this.mVlIFrameCallback = frameCallback;
        if (mUSBMonitor == null) {
            mUSBMonitor = new USBMonitor(ContextProvider.getContext(),
                    new USBMonitor.OnDeviceConnectListener() {

                        @Override
                        public void onAttach(UsbDevice device) {
                            Log.w(TAG, "USBMonitor-onAttach-getProductId = " + device.getProductId());

                            mUSBMonitor.requestPermission(device);
                            for (OnUSBConnectListener onUSBConnectListener : mOnUSBConnectListeners) {
                                onUSBConnectListener.onAttach(device);
                            }
                        }

                        @Override
                        public void onGranted(UsbDevice usbDevice, boolean granted) {
                            Log.d(TAG, "USBMonitor-onGranted");
                            for (OnUSBConnectListener onUSBConnectListener : mOnUSBConnectListeners) {
                                onUSBConnectListener.onGranted(usbDevice, granted);
                            }
                        }

                        @Override
                        public void onDettach(UsbDevice device) {
                            Log.d(TAG, "USBMonitor-onDettach");
                            Const.isDeviceConnected = false;
                            for (OnUSBConnectListener onUSBConnectListener : mOnUSBConnectListeners) {
                                onUSBConnectListener.onDettach(device);
                            }

                        }

                        @Override
                        public void onConnect(final UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock,
                                              boolean createNew) {
                            Log.w(TAG, "USBMonitor-onConnect createNew " + createNew);
                            Log.w(TAG, "USBMonitor-onConnect Pid " + device.getProductId());
                            if (!mIrOpened) {
                                openIrUVCCamera(irPid, irWidth, irHeight, irFPS, irBandWith, device, ctrlBlock);
                            }
                            if (!mVlOpened) {
                                openVlUVCCamera(vlPid, vlWidth, vlHeight, vlFPS, vlBandWith, device, ctrlBlock);
                            }

                            if (mIrOpened && mVlOpened) {
                                for (OnUSBConnectListener onUSBConnectListener : mOnUSBConnectListeners) {
                                    onUSBConnectListener.onConnect(device, ctrlBlock, createNew);
                                }
                            }
                        }

                        @Override
                        public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
                            Log.w(TAG, "USBMonitor-onDisconnect");
                            Const.isDeviceConnected = false;
                            for (OnUSBConnectListener onUSBConnectListener : mOnUSBConnectListeners) {
                                onUSBConnectListener.onDisconnect(device, ctrlBlock);
                            }
                        }

                        @Override
                        public void onCancel(UsbDevice device) {
                            Log.d(TAG, "USBMonitor-onCancel");
                            Const.isDeviceConnected = false;
                            for (OnUSBConnectListener onUSBConnectListener : mOnUSBConnectListeners) {
                                onUSBConnectListener.onCancel(device);
                            }
                        }
                    });
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

    private void openIrUVCCamera(int pid, int irWidth, int irHeight, int irFps, float irBandWidth, UsbDevice device, USBMonitor.UsbControlBlock controlBlock) {
        Log.w(TAG, "USBMonitor-openIrUVCCamera 1 " + device.getProductId());
        synchronized (mSyncs) {
            if (device.getProductId() != pid) {
                return;
            }
            Log.w(TAG, "USBMonitor-openIrUVCCamera " + device.getProductId());

            if (mIrUvcCamera == null) {
                ConcreateUVCBuilder concreateUVCBuilder = new ConcreateUVCBuilder();
                mIrUvcCamera = concreateUVCBuilder
                        .setUVCType(UVCType.USB_UVC)
                        .build();
                mIrUvcCamera.setDefaultBandwidth(irBandWidth);
                mIrUvcCamera.setDefaultPreviewMaxFps(irFps);

                mIrUvcCamera.openUVCCamera(controlBlock);

                initIRCMD();
                mIrUvcCamera.setUSBPreviewSize(irWidth, irHeight);
                mIrUvcCamera.onStartPreview();
                mIrOpened = true;
                Log.w(TAG, "USBMonitor-openIrUVCCamera complete" + device.getProductId());

            }
        }

    }

    public void initIRCMD() {

        if (mIrUvcCamera != null) {
            ConcreteIRCMDBuilder concreteIRCMDBuilder = new ConcreteIRCMDBuilder();
            mIrcmd = concreteIRCMDBuilder
                    .setIrcmdType(IRCMDType.USB_IR_256_384)
                    .setIdCamera(mIrUvcCamera.getNativePtr())
                    .build();
            for (OnUSBConnectListener onUSBConnectListener : mOnUSBConnectListeners) {
                onUSBConnectListener.onIRCMDInit(mIrcmd);
            }
        }
    }

    private void openVlUVCCamera(int pid, int vlWidth, int vlHeight, int vlFps, float vlBandWidth, UsbDevice device, USBMonitor.UsbControlBlock controlBlock) {
        synchronized (mSyncs) {
            Log.w(TAG, "USBMonitor-openVlUVCCamera 1" + device.getProductId());

            if (device.getProductId() != pid) {
                return;
            }
            Log.w(TAG, "USBMonitor-openVlUVCCamera " + device.getProductId());

            if (mVlUvcCamera == null) {
                ConcreateUVCBuilder concreateUVCBuilder = new ConcreateUVCBuilder();
                mVlUvcCamera = concreateUVCBuilder
                        .setUVCType(UVCType.USB_UVC)
                        .build();

                mVlUvcCamera.setDefaultPreviewMode(CommonParams.FRAMEFORMATType.FRAME_FORMAT_MJPEG);
                mVlUvcCamera.setDefaultBandwidth(vlBandWidth);
                mVlUvcCamera.setDefaultPreviewMaxFps(vlFps);

                mVlUvcCamera.openUVCCamera(controlBlock);

                mVlUvcCamera.setUSBPreviewSize(vlWidth, vlHeight);
                if (mVlIFrameCallback != null) {
                    mVlUvcCamera.setFrameCallback(mVlIFrameCallback);
                }
                mVlUvcCamera.onStartPreview();
                mVlOpened = true;
                Log.w(TAG, "USBMonitor-openVlUVCCamera complete" + device.getProductId());
            }
        }
    }

    public void stopIrUVCCamera() {
        Log.i(TAG, "stopIrUVCCamera");
        if (mIrUvcCamera != null) {
            mIrUvcCamera.onStopPreview();
            SystemClock.sleep(200);
            mIrUvcCamera.onDestroyPreview();
            mIrUvcCamera = null;
            mIrOpened = false;
        }
    }

    public void stopVlUVCCamera() {
        Log.i(TAG, "stopVlUVCCamera");
        if (mVlUvcCamera != null) {
            mVlUvcCamera.onStopPreview();
            SystemClock.sleep(200);
            mVlUvcCamera.onDestroyPreview();
            mVlUvcCamera = null;
            mVlOpened = false;
        }
    }

    public IRCMD getIrcmd() {
        return mIrcmd;
    }

    public UVCCamera getIrUvcCamera() {
        return mIrUvcCamera;
    }

    public UVCCamera getVlUvcCamera() {
        return mVlUvcCamera;
    }

    public void onRelease() {
        mVlIFrameCallback = null;
        mUSBMonitor = null;
        mIrcmd = null;
        mIrUvcCamera = null;
        mVlUvcCamera = null;
        mVlOpened = false;
        mIrOpened = false;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ir\usbdual\camera\USBMonitorManager.java =====

package com.mpdc4gsr.libunified.ir.usbdual.camera;

import android.hardware.usb.UsbDevice;
import android.os.SystemClock;
import android.util.Log;

import com.energy.iruvc.ircmd.ConcreteIRCMDBuilder;
import com.energy.iruvc.ircmd.IRCMD;
import com.energy.iruvc.ircmd.IRCMDType;
import com.energy.iruvc.usb.USBMonitor;
import com.energy.iruvc.utils.CommonParams;
import com.energy.iruvc.utils.DeviceType;
import com.energy.iruvc.uvc.CameraSize;
import com.energy.iruvc.uvc.ConcreateUVCBuilder;
import com.energy.iruvc.uvc.UVCCamera;
import com.energy.iruvc.uvc.UVCType;
import com.mpdc4gsr.libunified.compat.ContextProvider;
import com.mpdc4gsr.libunified.ir.usbdual.Const;
import com.mpdc4gsr.libunified.ir.usbdual.inf.OnUSBConnectListener;

import java.util.ArrayList;
import java.util.List;

public class USBMonitorManager {
    public static final String TAG = "USBMonitorManager";
    private static USBMonitorManager mInstance;
    private USBMonitor mUSBMonitor;
    private UVCCamera mUvcCamera;
    private IRCMD mIrcmd;
    private CommonParams.DataFlowMode mDefaultDataFlowMode = CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT;
    private CommonParams.GainMode gainMode = CommonParams.GainMode.GAIN_MODE_HIGH_LOW;
    private boolean isUseIRISP;
    private boolean isUseGPU = true;
    private int cameraWidth;
    private int cameraHeight;
    private byte[] tau_data_H;
    private byte[] tau_data_L;
    private long tempinfo = 0;
    private short[] nuc_table_high = new short[8192];
    private short[] nuc_table_low = new short[8192];
    private byte[] priv_high = new byte[1201];
    private byte[] priv_low = new byte[1201];
    private short[] kt_high = new short[1201];
    private short[] kt_low = new short[1201];
    private short[] bt_high = new short[1201];
    private short[] bt_low = new short[1201];
    private boolean isGetNucFromFlash;
    private CommonParams.GainStatus gainStatus = CommonParams.GainStatus.HIGH_GAIN;
    private int[] curVtemp = new int[1];
    private List<OnUSBConnectListener> mOnUSBConnectListeners = new ArrayList<>();
    private boolean isTempReplacedWithTNREnabled;
    private boolean isReStart = false;
    private int mPid = 0;

    private USBMonitorManager() {
    }

    public static synchronized USBMonitorManager getInstance() {
        if (mInstance == null) {
            mInstance = new USBMonitorManager();
        }
        return mInstance;
    }

    public void addOnUSBConnectListener(OnUSBConnectListener onUSBConnectListener) {
        mOnUSBConnectListeners.add(onUSBConnectListener);
    }

    public void removeOnUSBConnectListener(OnUSBConnectListener onUSBConnectListener) {
        mOnUSBConnectListeners.remove(onUSBConnectListener);
    }

    public boolean isReStart() {
        return isReStart;
    }

    public void setReStart(boolean reStart) {
        isReStart = reStart;
    }

    public void init(int pid, boolean isUseIRISP, CommonParams.DataFlowMode defaultDataFlowMode) {
        this.mPid = pid;
        this.isUseIRISP = isUseIRISP;
        this.mDefaultDataFlowMode = defaultDataFlowMode;
        if (defaultDataFlowMode == CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT) {

            cameraWidth = 256;
            cameraHeight = 384;
        } else {

            cameraWidth = 256;
            cameraHeight = 192;
        }
        if (mUSBMonitor == null) {
            mUSBMonitor = new USBMonitor(ContextProvider.getContext(),
                    new USBMonitor.OnDeviceConnectListener() {

                        @Override
                        public void onAttach(UsbDevice device) {
                            Log.w(TAG, "USBMonitorManager-onAttach-getProductId = " + device.getProductId());
                            Log.w(TAG, "USBMonitorManager-onAttach-mPid = " + mPid);

                            if (device.getProductId() != mPid) {
                                return;
                            }
                            mUSBMonitor.requestPermission(device);
                            for (OnUSBConnectListener onUSBConnectListener : mOnUSBConnectListeners) {
                                onUSBConnectListener.onAttach(device);
                            }
                        }

                        @Override
                        public void onGranted(UsbDevice usbDevice, boolean granted) {
                            Log.d(TAG, "USBMonitorManager-onGranted");
                            for (OnUSBConnectListener onUSBConnectListener : mOnUSBConnectListeners) {
                                onUSBConnectListener.onGranted(usbDevice, granted);
                            }
                        }

                        @Override
                        public void onDettach(UsbDevice device) {
                            Log.d(TAG, "USBMonitorManager-onDettach");
                            Const.isDeviceConnected = false;
                            for (OnUSBConnectListener onUSBConnectListener : mOnUSBConnectListeners) {
                                onUSBConnectListener.onDettach(device);
                            }
                        }

                        @Override
                        public void onConnect(final UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock,
                                              boolean createNew) {
                            Log.w(TAG, "USBMonitorManager-onConnect");
                            if (createNew) {
                                Const.isDeviceConnected = true;
                                if (isReStart()) {
                                    SystemClock.sleep(2000);
                                }
                                handleUSBConnect(ctrlBlock);
                                for (OnUSBConnectListener onUSBConnectListener : mOnUSBConnectListeners) {
                                    onUSBConnectListener.onConnect(device, ctrlBlock, createNew);
                                }
                            }
                        }

                        @Override
                        public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
                            Log.w(TAG, "USBMonitorManager-onDisconnect");
                            Const.isDeviceConnected = false;
                            for (OnUSBConnectListener onUSBConnectListener : mOnUSBConnectListeners) {
                                onUSBConnectListener.onDisconnect(device, ctrlBlock);
                            }
                        }

                        @Override
                        public void onCancel(UsbDevice device) {
                            Log.d(TAG, "USBMonitorManager-onCancel");
                            Const.isDeviceConnected = false;
                            for (OnUSBConnectListener onUSBConnectListener : mOnUSBConnectListeners) {
                                onUSBConnectListener.onCancel(device);
                            }
                        }
                    });
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

    private void initUVCCamera() {
        Log.d(TAG, "initUVCCamera");

        ConcreateUVCBuilder concreateUVCBuilder = new ConcreateUVCBuilder();
        mUvcCamera = concreateUVCBuilder
                .setUVCType(UVCType.USB_UVC)
                .build();

        mUvcCamera.setDefaultBandwidth(1f);
    }

    public void openUVCCamera(USBMonitor.UsbControlBlock ctrlBlock) {
        if (mUvcCamera == null) {
            initUVCCamera();
        }

        mUvcCamera.openUVCCamera(ctrlBlock);
    }

    public UVCCamera getUvcCamera() {
        return mUvcCamera;
    }

    public IRCMD getIrcmd() {
        return mIrcmd;
    }

    public void handleUSBConnect(USBMonitor.UsbControlBlock ctrlBlock) {
        openUVCCamera(ctrlBlock);

        List<CameraSize> previewList = getAllSupportedSize();

        initIRCMD(previewList);

        if (mDefaultDataFlowMode == CommonParams.DataFlowMode.TNR_OUTPUT) {
            isTempReplacedWithTNREnabled = mIrcmd.isTempReplacedWithTNREnabled(DeviceType.P2);
            Log.i(TAG, "startPreview->isTempReplacedWithTNREnabled = " + isTempReplacedWithTNREnabled);

            if (isTempReplacedWithTNREnabled) {
                cameraWidth = 256;
                cameraHeight = 384;
            } else {
                cameraWidth = 256;
                cameraHeight = 192;
            }
        }
        int result = setPreviewSize(cameraWidth, cameraHeight);
        if (result == 0) {

            startPreview();
        }
    }

    private List<CameraSize> getAllSupportedSize() {
        Log.w(TAG, "getSupportedSize = " + mUvcCamera.getSupportedSize());
        List<CameraSize> previewList = new ArrayList<>();
        if (mUvcCamera != null) {
            previewList = mUvcCamera.getSupportedSizeList();
        }
        for (CameraSize size : previewList) {
            Log.i(TAG, "SupportedSize : " + size.width + " * " + size.height);
        }
        return previewList;
    }

    public void initIRCMD(List<CameraSize> previewList) {
        for (CameraSize size : previewList) {
            Log.i(TAG, "SupportedSize : " + size.width + " * " + size.height);
        }

        if (mUvcCamera != null) {
            ConcreteIRCMDBuilder concreteIRCMDBuilder = new ConcreteIRCMDBuilder();
            mIrcmd = concreteIRCMDBuilder
                    .setIrcmdType(IRCMDType.USB_IR_256_384)
                    .setIdCamera(mUvcCamera.getNativePtr())
                    .build();
            for (OnUSBConnectListener onUSBConnectListener : mOnUSBConnectListeners) {
                onUSBConnectListener.onIRCMDInit(mIrcmd);
            }
        }
    }

    private int setPreviewSize(int cameraWidth, int cameraHeight) {
        int result = -1;

        try {
            if (mUvcCamera != null) {
                result = mUvcCamera.setUSBPreviewSize(cameraWidth, cameraHeight);
            }
        } catch (Exception e) {
            for (OnUSBConnectListener onUSBConnectListener : mOnUSBConnectListeners) {
                onUSBConnectListener.onSetPreviewSizeFail();
            }
        }
        return result;
    }

    private void startPreview() {
        Log.d(TAG, "startPreview");

        if (mUvcCamera == null) {
            return;
        }

        Const.isReadFlashData = true;
        mUvcCamera.setOpenStatus(true);
        mUvcCamera.onStartPreview();
        if (isTempReplacedWithTNREnabled) {

            if (mIrcmd.startPreview(CommonParams.PreviewPathChannel.PREVIEW_PATH0,
                    CommonParams.StartPreviewSource.SOURCE_SENSOR,
                    25, CommonParams.StartPreviewMode.VOC_DVP_MODE,
                    CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT) == 0) {
                if (mIrcmd.startY16ModePreview(CommonParams.PreviewPathChannel.PREVIEW_PATH0,
                        CommonParams.Y16ModePreviewSrcType.Y16_MODE_TEMPERATURE) == 0) {
                    mIrcmd.setPropImageParams(CommonParams.PropImageParams.IMAGE_PROP_SEL_MIRROR_FLIP,
                            Const.IR_MIRROR_FLIP_TYPE);
                }
            }
        } else {

            if (mIrcmd.startPreview(CommonParams.PreviewPathChannel.PREVIEW_PATH0,
                    CommonParams.StartPreviewSource.SOURCE_SENSOR,
                    25, CommonParams.StartPreviewMode.VOC_DVP_MODE,
                    CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT) == 0) {
                mIrcmd.setPropImageParams(CommonParams.PropImageParams.IMAGE_PROP_SEL_MIRROR_FLIP,
                        Const.IR_MIRROR_FLIP_TYPE);
            }
        }
    }

    public void stopPreview() {
        Log.i(TAG, "stopPreview");
        if (mUvcCamera != null) {
            if (mUvcCamera.getOpenStatus()) {
                mUvcCamera.onStopPreview();
            }
            SystemClock.sleep(200);

            mUvcCamera.onDestroyPreview();
            mUvcCamera = null;
        }
    }

    public void onPauseUvcPreview() {
        if (mUvcCamera != null) {
            mUvcCamera.onPausePreview();
        }
    }

    public void onResumeUvcPreview() {
        if (mUvcCamera != null) {
            mUvcCamera.onResumePreview();
        }
    }

}