package com.infisense.usbdual.camera;

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
import com.energy.iruvc.utils.AutoGainSwitchCallback;
import com.energy.iruvc.utils.AvoidOverexposureCallback;
import com.energy.iruvc.utils.CommonParams;
import com.energy.iruvc.utils.DualCameraParams;
import com.energy.iruvc.utils.IFrameCallback;
import com.energy.iruvc.utils.IIRFrameCallback;
import com.energy.iruvc.uvc.UVCCamera;
import com.infisense.usbdual.Const;
import com.infisense.usbir.utils.OpencvTools;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Locale;

import static com.infisense.usbdual.camera.IFrameData.FRAME_LEN;

/**
 * Created by fengjibo on 2023/9/20.
 */
public class DualViewWithExternalCameraCommonApi extends BaseDualView {

    private final String TAG = "DualViewWithExternalCameraCommonApi";
    private DualUVCCamera dualUVCCamera;
    private final IFrameCallback iFrameCallback;
    private final IIRFrameCallback irFrameCallback;
    public SurfaceView cameraview;
    public boolean isRun = true;
    // 帧率展示
    public int count = 0;
    private long timestart = 0;
    private double fps = 0;
    //开启自动gainswitch将auto_gain_switch和auto_gain_switch_running同时改为true
    public boolean auto_gain_switch = false;
    public boolean auto_gain_switch_running = true;
    public boolean auto_over_protect = false;
    private LibIRProcess.AutoGainSwitchInfo_t auto_gain_switch_info = new LibIRProcess.AutoGainSwitchInfo_t();
    private LibIRProcess.GainSwitchParam_t gain_switch_param = new LibIRProcess.GainSwitchParam_t();
    private CommonParams.GainStatus gainStatus = CommonParams.GainStatus.HIGH_GAIN;
    public Bitmap bitmap;
    public Bitmap supIROlyNoFusionBitmap;
    public Bitmap supMixBitmap;
    public Bitmap supIROlyBitmap;

    private boolean valid = false;
    private Bitmap mScaledBitmap;
    private Handler handler;

    // 是否使用IRISPalgorithm集成
    private boolean isUseIRISP = false;

    private SurfaceNativeWindow mSurfaceNativeWindow;
    private Surface mSurface;

    private DualCameraParams.FusionType mCurrentFusionType;
    private boolean firstFrame = false;
    private byte[] irRGBAData;//原始infrareddata 192 *256
    private byte[] preIrData;//预processinginfrared原始data 192 *256 * 2
    private byte[] preTempData;//预processingtemperature原始data 192 *256 * 2
    private byte[] preIrARGBData;//预processing后infraredARGBdata 192 * 256 * 4
    public byte[] frameData = new byte[FRAME_LEN];//原始全部data

    public byte[] frameIrAndTempData = new byte[192 * 256 * 4];

    public int rotate = 180; //lens颠倒了，所以初始颠倒个180度

    private volatile boolean isOpenAmplify = false;
    private final byte[] amplifyMixRotateArray;//fusion的data 640 * MULTIPLE * 480 * MULTIPLE
    private final byte[] amplifyIRRotateArray;//单infrared的data 256 * MULTIPLE * 192 * MULTIPLE

    public static final int MULTIPLE = 2;

    public boolean isOpenAmplify() {
        return isOpenAmplify;
    }

    public void setOpenAmplify(boolean openAmplify) {
        isOpenAmplify = openAmplify;
    }

    /**
     * @param handler
     */
    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    /**
     * @param cameraview
     * @param irUVCCamera
     * @param dataFlowMode
     * @param vlCameraWidth
     * @param vlCameraHeight
     * @param irCameraWidth
     * @param irCameraHeight
     * @param dualCameraWidth
     * @param dualCameraHeight
     */
    public DualViewWithExternalCameraCommonApi(SurfaceView cameraview, UVCCamera irUVCCamera,
                                               CommonParams.DataFlowMode dataFlowMode,
                                               int irCameraWidth, int irCameraHeight, int vlCameraWidth, int vlCameraHeight,
                                               int dualCameraWidth, int dualCameraHeight,
                                               boolean isUseIRISP,int rotate,IIRFrameCallback irFrameCallback) {
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
        // DualUVCCamera initialize
        ConcreateDualBuilder concreateDualBuilder = new ConcreateDualBuilder();
        dualUVCCamera = concreateDualBuilder
                .setDualType(DualType.USB_DUAL)
                // 需要注意宽高的顺序
                .setIRSize(Const.IR_WIDTH, Const.IR_HEIGHT)
                .setVLSize(Const.VL_WIDTH, Const.VL_HEIGHT)
                .setDualSize(Const.DUAL_HEIGHT, Const.DUAL_WIDTH)
                .setDataFlowMode(dataFlowMode)
                .setPreviewCameraStyle(CommonParams.PreviewCameraStyle.EXTERNAL_CAMERA)
                .setDeviceStyle(CommonParams.DeviceStyle.ALL_IN_ONE)
                .setUseDualGPU(false)
                /**
                 * start多line程processingfusion
                 * 在CPU低性能平台上，建议close多line程
                 * setUseDualGPU 为true GPU无效
                 */
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

        /**
         * 同时Open防灼烧和自动gainswitch后，如果想modify防灼烧和自动gainswitch的触发优先级，可以通过modify下area的触发parameterimplementation
         */
        // 自动gainswitchparameterauto gain switch parameter
        gain_switch_param.above_pixel_prop = 0.1f;    //用于high -> low gain,device像素总area积的百分比
        gain_switch_param.above_temp_data = (int) ((130 + 273.15) * 16 * 4); //用于high -> low gain,高gain向低gainswitch的触发temperature
        gain_switch_param.below_pixel_prop = 0.95f;   //用于low -> high gain,device像素总area积的百分比
        gain_switch_param.below_temp_data = (int) ((110 + 273.15) * 16 * 4);//用于low -> high gain,低gain向高gainswitch的触发temperature
        auto_gain_switch_info.switch_frame_cnt = 5 * 15; //continuous满足触发条件帧数超过该阈值会触发自动gainswitch(假设出图速度为15帧每秒，则5 * 15大概为5秒)
        auto_gain_switch_info.waiting_frame_cnt = 7 * 15;//触发自动gainswitch之后，会间隔该阈值的帧数不进行gainswitch监测(假设出图速度为15帧每秒，则7 * 15大概为7秒)
        // 防灼烧parameterover_portect parameter
        int low_gain_over_temp_data = (int) ((550 + 273.15) * 16 * 4); //低gain下触发防灼烧的temperature
        int high_gain_over_temp_data = (int) ((110 + 273.15) * 16 * 4); //高gain下触发防灼烧的temperature
        float pixel_above_prop = 0.02f;//device像素总area积的百分比
        int switch_frame_cnt = 7 * 15;//continuous满足触发条件超过该阈值会触发防灼烧(假设出图速度为15帧每秒，则7 * 15大概为7秒)
        int close_frame_cnt = 10 * 15;//触发防灼烧之后，经过该阈值的帧数之后会解除防灼烧(假设出图速度为15帧每秒，则10 * 15大概为10秒)

        LibIRProcess.ImageRes_t imageRes = new LibIRProcess.ImageRes_t();
        imageRes.height = (char) (192);
        imageRes.width = (char) 256;

        irRGBAData = new byte[irSize * 4];
        preIrData = new byte[irSize * 2];//预processinginfrared原始data 192 *256 * 2
        preTempData = new byte[irSize * 2];//预processingtemperature原始data 192 *256 * 2
        preIrARGBData = new byte[irSize * 2 * 2];;//预processing后infraredARGBdata 192 * 256 * 4
        iFrameCallback = new IFrameCallback() {
            /**
             * frame 所有data集合 (CPU)
             * frame 长度 dualwidth * dualHeight * 4 + irWidth * irHeight * 2 + irWidth * irHeight * 2 + dualwidth *
             * dualHeight * 2 + vlWidth * vlHeight * 3 + dualwidth * dualHeight * 4
             * data流按顺序依次为
             * mixData fusiondata，formatARGB，长度dualwidth * dualHeight * 4
             * irData 原始infrareddata，formatY16，长度irWidth * irHeight * 2
             * tempData 原始temperaturedata，formatY16，长度irWidth * irHeight * 2
             * remapTempData fusionimage尺寸一致的temperaturedata formatYUV422 dualwidth * dualHeight * 2
             * vlData 原始visible lightdata formatRGB24 vlWidth * vlHeight * 3
             * vlARGBData fusionimage尺寸一致的visible lightdata dualwidth * dualHeight * 4（仅picture-in-picturemodeCallbackdata）
             * picture-in-picturemodeScreenFusion:mixData 为单infrareddata，formatARGB，长度dualwidth * dualHeight * 4
             * fusionmode为IROnlyNoFusion, 只会ReturnirData和tempData,data位置不变
             */
            /**
             * frame 所有data集合 (GPU)
             * frame 长度 dualwidth * dualHeight * 4 + irWidth * irHeight * 2 + irWidth * irHeight * 2 + dualwidth *
             * dualHeight * 2 + vlWidth * vlHeight * 4
             * data流按顺序依次为
             * mixData fusiondata，formatARGB，长度dualwidth * dualHeight * 4
             * irData 原始infrareddata，formatY16，长度irWidth * irHeight * 2
             * tempData 原始temperaturedata，formatY16，长度irWidth * irHeight * 2
             * remapTempData fusionimage尺寸一致的temperaturedata formatYUV422 dualwidth * dualHeight * 2
             * vlData 原始visible lightdata formatARGB vlWidth * vlHeight * 4
             * picture-in-picturemodeScreenFusion:mixData 为单infrareddata，formatARGB，长度dualwidth * dualHeight * 4
             * fusionmode为IROnlyNoFusion, 只会ReturnirData和tempData,data位置不变
             */
            @Override
            public void onFrame(byte[] frame) {
                if (frame.length == 1) {
                    if (handler != null) {
                        handler.sendEmptyMessage(Const.RESTART_USB);
                    }
                    Log.d(TAG, "RESTART_USB");
                    return;
                }
                // 帧率展示
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
                System.arraycopy(frame, 0, frameData, 0, FRAME_LEN); //无损data
                //Merge原始infrareddata和原始temperaturedata
                System.arraycopy(frame, dualCameraWidth*dualCameraHeight*4, frameIrAndTempData, 0, frameIrAndTempData.length);

                //picture-in-picturemodeScreenFusion:mixData 为单infrareddata，formatARGB，长度dualwidth * dualHeight * 4
//                if (mCurrentFusionType == DualCameraParams.FusionType.ScreenFusion) {
//                    System.arraycopy(frame, fusionLength + irSize * 4 + remapTempSize + vlSize, vlARGBData, 0,
//                            fusionLength);
//                }

                //如果是IROnlyNoFusionmode, 此时infrareddata和temperature为原始data，长度都为256*192
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
                    if (isOpenAmplify){
                        OpencvTools.supImage(irData,Const.IR_HEIGHT,Const.IR_WIDTH, amplifyIRRotateArray);
                        if (mSurface != null) {
                            mSurfaceNativeWindow.onDrawFrame(mSurface, amplifyIRRotateArray,
                                    Const.IR_WIDTH * MULTIPLE,
                                    Const.IR_HEIGHT * MULTIPLE);
                        }
                    }else {
                        if (mSurface != null) {
                            mSurfaceNativeWindow.onDrawFrame(mSurface, irRGBAData, Const.IR_HEIGHT, Const.IR_WIDTH);
                        }
                    }
                }else {
                    if (isOpenAmplify){
                        if (mCurrentFusionType == DualCameraParams.FusionType.IROnly){
                            OpencvTools.supImageMix(mixData,Const.DUAL_HEIGHT,Const.DUAL_WIDTH, mixData);
                            if (mSurface != null) {
                                mSurfaceNativeWindow.onDrawFrame(mSurface, mixData, Const.DUAL_WIDTH, Const.DUAL_HEIGHT);
                            }
                        }else {
                            OpencvTools.supImage(mixData,Const.DUAL_HEIGHT,Const.DUAL_WIDTH, amplifyMixRotateArray);
                            if (mSurface != null) {
                                mSurfaceNativeWindow.onDrawFrame(mSurface, amplifyMixRotateArray,
                                        Const.DUAL_WIDTH * MULTIPLE,
                                        Const.DUAL_HEIGHT * MULTIPLE);
                            }
                        }
                    }else {
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

//                if (saveData) {
//                    saveData = false;
//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            FileUtil.saveByteFile(cameraview.getContext(), mixData, "mix");
//                            FileUtil.saveByteFile(cameraview.getContext(), remapTempData, "remap_temp");
//                            FileUtil.saveByteFile(cameraview.getContext(), irData, "ir_data");
//                            FileUtil.saveByteFile(cameraview.getContext(), normalTempData, "temp_data");
//                            FileUtil.saveByteFile(cameraview.getContext(), vlData, "vl_data");
//                            FileUtil.saveByteFile(cameraview.getContext(), vlARGBData, "vl_argb_data");
//                        }
//                    }).start();
//
//                }
                //请不要旋转imageTest
                // 自动gainswitch，不effective的话请您的device是否支持自动gainswitch
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
                    // 防灼烧保护
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

    public void resetAutoGainInfo() {
        auto_gain_switch_info.switched_flag = 0;
        auto_gain_switch_info.cur_switched_cnt = 0;
        auto_gain_switch_info.cur_detected_low_cnt = 0;
        auto_gain_switch_info.cur_detected_high_cnt = 0;
    }

    
    public void startPreview() {
        /**
         * setIrDataPreHandleEnable 开启后
         * 必须settingssetIrFrameCallback
         * 同时setFusion(HSLFusion)mode不effective, 等温尺相关interfacesetIsothermal,pseudo color，自定义pseudo color相关interfacesetPseudocolor, setCustomPseudocolor不effective
         */
        switchIrPreDataHandleEnable(true);
        dualUVCCamera.setFrameCallback(iFrameCallback);
        dualUVCCamera.onStartPreview();
        firstFrame = false;
    }

    /**
     * @return
     */
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
        dualUVCCamera.setIrFrameCallback(enable? irFrameCallback : null);
    }

    public byte[] getRemapTempData() {
        return remapTempData;
    }

    public Bitmap getScaledBitmap() {
        if (isOpenAmplify){
            if (mCurrentFusionType == DualCameraParams.FusionType.IROnlyNoFusion){
                //单infraredmode
                supIROlyNoFusionBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(amplifyIRRotateArray, 0,
                        supIROlyNoFusionBitmap.getWidth() * supIROlyNoFusionBitmap.getHeight() * 4));
                mScaledBitmap = Bitmap.createScaledBitmap(supIROlyNoFusionBitmap,
                        ((ViewGroup)cameraview.getParent()).getWidth(),
                        ((ViewGroup)cameraview.getParent()).getHeight(), true);
            }else if (mCurrentFusionType == DualCameraParams.FusionType.IROnly){
                bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(mixData, 0, bitmap.getWidth() * bitmap.getHeight() * 4));
                mScaledBitmap = Bitmap.createScaledBitmap(bitmap, ((ViewGroup)cameraview.getParent()).getWidth(),
                        ((ViewGroup)cameraview.getParent()).getHeight(), true);
            } else {
                supMixBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(mixData, 0, supMixBitmap.getWidth() * supMixBitmap.getHeight() * 4));
                mScaledBitmap = Bitmap.createScaledBitmap(supMixBitmap, ((ViewGroup)cameraview.getParent()).getWidth(),
                        ((ViewGroup)cameraview.getParent()).getHeight(), true);
            }
        }else {
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(mixData, 0, bitmap.getWidth() * bitmap.getHeight() * 4));
            mScaledBitmap = Bitmap.createScaledBitmap(bitmap, ((ViewGroup)cameraview.getParent()).getWidth(),
                    ((ViewGroup)cameraview.getParent()).getHeight(), true);
        }
        return mScaledBitmap;
    }

    private boolean saveData = false;

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
