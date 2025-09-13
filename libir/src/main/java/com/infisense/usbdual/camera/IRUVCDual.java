package com.infisense.usbdual.camera;

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
import com.energy.iruvc.uvc.CameraSize;
import com.energy.iruvc.uvc.ConcreateUVCBuilder;
import com.energy.iruvc.uvc.ConnectCallback;
import com.energy.iruvc.uvc.UVCCamera;
import com.energy.iruvc.uvc.UVCType;
import com.infisense.usbdual.Const;
import com.infisense.usbir.R;

import java.util.ArrayList;
import java.util.List;

/*
 * @Description:
 * @Author:         brilliantzhao
 * @CreateDate:     2022.3.28 14:48
 * @UpdateUser:
 * @UpdateDate:     2022.3.28 14:48
 * @UpdateRemark:
 */
public class IRUVCDual {
    public String TAG = "IRUVC";
    private final Context mContext;
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
    // 是否使用IRISPalgorithm集成
    private boolean isUseIRISP;
    // 是否使用GPU方案
    private boolean isUseGPU = false;
    // current的gainstate
    private CommonParams.GainStatus gainStatus = CommonParams.GainStatus.HIGH_GAIN;
    // module支持的高低gainmode
    private CommonParams.GainMode gainMode = CommonParams.GainMode.GAIN_MODE_HIGH_LOW;
    private short[] nuc_table_high = new short[8192];
    private short[] nuc_table_low = new short[8192];
    private boolean isGetNucFromFlash; // 是否从coreFlash中读取的nucdata，会影响到temperature measurement修正的资源release
    //
    private byte[] priv_high = new byte[1201];
    private byte[] priv_low = new byte[1201];
    private short[] kt_high = new short[1201];
    private short[] kt_low = new short[1201];
    private short[] bt_high = new short[1201];
    private short[] bt_low = new short[1201];

    // coretemperature
    private int[] curVtemp = new int[1];

    private ConnectCallback mConnectCallback;

    public void setDualUVCCamera(DualUVCCamera dualUVCCamera) {
        this.dualUVCCamera = dualUVCCamera;
    }

    public DualUVCCamera dualUVCCamera;

    public void setPseudocolorMode(CommonParams.PseudoColorType pseudocolorMode) {
        this.pseudocolorMode = pseudocolorMode;
    }

    private CommonParams.PseudoColorType pseudocolorMode;

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

    private Handler handler;

    public boolean rotate = false;

    public void setRotate(boolean rotate) {
        this.rotate = rotate;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public void setTemperature(byte[] temperature) {
        this.temperature = temperature;
    }

    /**
     * @param cameraWidth
     * @param cameraHeight
     * @param context
     * @param syncimage
     */
    public IRUVCDual(int cameraWidth, int cameraHeight, Context context, SynchronizedBitmap syncimage,
                     ConnectCallback connectCallback) {
        this.cameraWidth = cameraWidth;
        this.cameraHeight = cameraHeight;
        this.mContext = context;
        this.syncimage = syncimage;
        this.mConnectCallback = connectCallback;
        initUVCCamera(cameraWidth, cameraHeight);
        mUSBMonitor = new USBMonitor(context, new USBMonitor.OnDeviceConnectListener() {

            // called by checking usb device
            // do request device permission
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

            // called by connect to usb camera
            // do open camera,start previewing
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

            // called by disconnect to usb camera
            // do nothing
            @Override
            public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
                Log.w(TAG, "onDisconnect");
                Const.isDeviceConnected = false;
            }

            // called by taking out usb device
            // do close camera
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
        int high_gain_over_temp_data = (int) ((100 + 273.15) * 16 * 4); //高gain下触发防灼烧的temperature
        float pixel_above_prop = 0.02f;//device像素总area积的百分比
        int switch_frame_cnt = 7 * 15;//continuous满足触发条件超过该阈值会触发防灼烧(假设出图速度为15帧每秒，则7 * 15大概为7秒)
        int close_frame_cnt = 10 * 15;//触发防灼烧之后，经过该阈值的帧数之后会解除防灼烧(假设出图速度为15帧每秒，则10 * 15大概为10秒)
    }

    /**
     * @param cameraWidth
     * @param cameraHeight
     * @param context
     * @param pid
     * @param fps
     * @param connectCallback
     * @param iFrameCallback
     */
    public IRUVCDual(int cameraWidth, int cameraHeight, Context context, int pid, int fps,
                     ConnectCallback connectCallback, IFrameCallback iFrameCallback) {
        this.mPid = pid;
        this.mFps = fps;
        this.cameraWidth = cameraWidth;
        this.cameraHeight = cameraHeight;
        this.mContext = context;
        this.mConnectCallback = connectCallback;
        this.iFrameCallback = iFrameCallback;
        //
        initUVCCamera(cameraWidth, cameraHeight);
        //
        mUSBMonitor = new USBMonitor(context, new USBMonitor.OnDeviceConnectListener() {
            // called by checking usb device
            // do request device permission
            @Override
            public void onAttach(UsbDevice device) {
                Log.w(TAG, "USBMonitor-onAttach mPid = " + pid + " getProductId = " + device.getProductId());
                /**
                 * USBMonitor会同时响应所有的UVCdevice，
                 * 需要根据自己的initializepid判断自己需要initialize的device
                 */
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

            // called by taking out usb device
            // do close camera
            @Override
            public void onDettach(UsbDevice device) {
                Log.w(TAG, "USBMonitor-onDettach mPid = " + pid);
                Const.isDeviceConnected = false;
                if (uvcCamera != null && uvcCamera.getOpenStatus()) {
//                    stopPreview();
                    if (handler != null && status != 2) {
                        handler.sendEmptyMessage(Const.RESTART_USB);
                    }
                    status = 2;
                }
            }

            // called by connect to usb camera
            // do open camera,start previewing
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

            // called by disconnect to usb camera
            // do nothing
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
        //
        initUVCCamera(cameraWidth, cameraHeight);
        //
        mUSBMonitor = new USBMonitor(context, new USBMonitor.OnDeviceConnectListener() {
            // called by checking usb device
            // do request device permission
            @Override
            public void onAttach(UsbDevice device) {
                Log.w(TAG, "USBMonitor-onAttach mPid = " + pid + " getProductId = " + device.getProductId());
                /**
                 * USBMonitor会同时响应所有的UVCdevice，
                 * 需要根据自己的initializepid判断自己需要initialize的device
                 */
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

            // called by taking out usb device
            // do close camera
            @Override
            public void onDettach(UsbDevice device) {
                Log.w(TAG, "USBMonitor-onDettach mPid = " + pid);
                Const.isDeviceConnected = false;
                if (uvcCamera != null && uvcCamera.getOpenStatus()) {
//                    stopPreview();
                    if (handler != null && status != 2) {
                        handler.sendEmptyMessage(Const.RESTART_USB);
                    }
                    status = 2;
                }
            }

            // called by connect to usb camera
            // do open camera,start previewing
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

            // called by disconnect to usb camera
            // do nothing
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

    /**
     * @param cameraWidth
     * @param cameraHeight
     * @param context
     * @param syncimage
     * @param pid
     */
    public IRUVCDual(int cameraWidth, int cameraHeight, Context context, SynchronizedBitmap syncimage, int pid,
                     ConnectCallback connectCallback, boolean isUseIRISP) {
        this.mPid = pid;
        this.cameraWidth = cameraWidth;
        this.cameraHeight = cameraHeight;
        this.mContext = context;
        this.syncimage = syncimage;
        this.isUseIRISP = isUseIRISP;
        this.mConnectCallback = connectCallback;
        //
        initUVCCamera(cameraWidth, cameraHeight);
        //
        mUSBMonitor = new USBMonitor(context, new USBMonitor.OnDeviceConnectListener() {

            // called by checking usb device
            // do request device permission
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

            // called by taking out usb device
            // do close camera
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

            // called by connect to usb camera
            // do open camera,start previewing
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

            // called by disconnect to usb camera
            // do nothing
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

    /**
     * @param cameraWidth
     * @param cameraHeight
     */
    public void initUVCCamera(int cameraWidth, int cameraHeight) {
        Log.i(TAG, "initUVCCamera->cameraWidth = " + cameraWidth + " cameraHeight = " + cameraHeight);
        // UVCCamera initialize
        ConcreateUVCBuilder concreateUVCBuilder = new ConcreateUVCBuilder();
        uvcCamera = concreateUVCBuilder
                .setUVCType(UVCType.USB_UVC)
                .build();
    }

    /**
     * @return
     */
    public UVCCamera getUvcCamera() {
        return uvcCamera;
    }

    /**
     * @return
     */
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
        // matching all of filter devices
        return mUSBMonitor.getDeviceList(deviceFilters);
    }

    /**
     * @param index
     * @return
     */
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

    /**
     * @param ctrlBlock
     */
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
        // uvc开启
        uvcCamera.openUVCCamera(ctrlBlock);
    }

    
    public void startPreview() {
        Log.w(TAG, "startPreview mPid = " + mPid + " isUseIRISP = " + isUseIRISP);
        uvcCamera.setOpenStatus(true);
        //
        if (iFrameCallback != null) {
            uvcCamera.setFrameCallback(iFrameCallback);
        }
        uvcCamera.onStartPreview();
        if (mPid == 0x5830 || mPid == 0x5840) {
            //settingsinfrared镜像出图，跟原生visible light保持一直
            ircmd.startPreview(CommonParams.PreviewPathChannel.PREVIEW_PATH0,
                    CommonParams.StartPreviewSource.SOURCE_SENSOR,
                    25, CommonParams.StartPreviewMode.VOC_DVP_MODE,
                    CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT);
            ircmd.setPropImageParams(CommonParams.PropImageParams.IMAGE_PROP_SEL_MIRROR_FLIP,
                    Const.IR_MIRROR_FLIP_TYPE);
        }
    }

    /**
     * Get/Retrieve支持的分辨率list
     *
     * @return
     */
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

    /**
     * init IRCMD
     * 可以根据Get/Retrieve到的分辨率list，来区分不同的module，从而改变不同的cmdparameter来调用不同的SDK
     *
     * @param previewList
     */
    public void initIRCMD(List<CameraSize> previewList) {
        for (CameraSize size : previewList) {
//            Log.i(TAG, "SupportedSize : " + size.width + " * " + size.height);
        }
        // IRCMD init
        if (uvcCamera != null) {
            ConcreteIRCMDBuilder concreteIRCMDBuilder = new ConcreteIRCMDBuilder();
            ircmd = concreteIRCMDBuilder
                    .setIrcmdType(IRCMDType.USB_IR_256_384)
                    .setIdCamera(uvcCamera.getNativePtr())
                    .build();
            //
            if (mConnectCallback != null) {
                Log.d(TAG, "onIRCMDCreate");
                mConnectCallback.onIRCMDCreate(ircmd);
            }
        }
    }

    /**
     * 之前的openUVCCameramethod中传入的都是默认值，这里需要根据实际传入对应的值
     *
     * @param cameraWidth
     * @param cameraHeight
     */
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

    /**
     * @param ctrlBlock
     */
    private void handleUSBConnect(USBMonitor.UsbControlBlock ctrlBlock) {
        Log.d(TAG, "handleUSBConnect mPid = " + mPid);
        openUVCCamera(ctrlBlock);
        // Get/Retrievedevice的分辨率list
        List<CameraSize> previewList = getAllSupportedSize();
        // 可以根据Get/Retrieve到的分辨率list，来区分不同的module，从而改变不同的cmdparameter来调用不同的SDK
        if (mPid == 0x5830 || mPid == 0x5840) {
            initIRCMD(previewList);
            /**
             * Adjust带宽
             * 部分分辨率或在部分机型上，会出现无法出图，或出图一段时间后卡顿的问题，需要configuration对应的带宽
             */
            uvcCamera.setDefaultBandwidth(1.0f);       //Adjust带宽
            uvcCamera.setDefaultPreviewMinFps(1);
            uvcCamera.setDefaultPreviewMaxFps(mFps);
        } else {
            Log.d(TAG, "startVLCamera handleUSBConnect mPid = " + mPid + " setDefaultPreviewMode");
            /**
             * visible lightmodule
             * DEFAULT 0 YUV
             *  Mjpeg 1  RGB
             */
            uvcCamera.setDefaultPreviewMode(CommonParams.FRAMEFORMATType.FRAME_FORMAT_MJPEG);
            /**
             * Adjust带宽
             * 部分分辨率或在部分机型上，会出现无法出图，或出图一段时间后卡顿的问题，需要configuration对应的带宽
             */
            uvcCamera.setDefaultBandwidth(0.6f);       //Adjust带宽
            uvcCamera.setDefaultPreviewMinFps(1);
            uvcCamera.setDefaultPreviewMaxFps(mFps);
        }

        // 根据device的分辨率list，这里可以动态的settingsmodule的宽高(这里作为示例，用的是从外部传入的方式)
        int result = setPreviewSize(cameraWidth, cameraHeight);
        if (result == 0) {
            //
            Log.d(TAG, "handleUSBConnect setPreviewSize success = " );
            startPreview();
        } else {
            Log.d(TAG, "handleUSBConnect setPreviewSize fail = " );
            stopPreview();
        }

    }

}
