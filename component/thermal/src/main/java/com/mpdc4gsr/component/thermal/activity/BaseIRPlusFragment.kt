package com.mpdc4gsr.component.thermal.activity

import android.graphics.ImageFormat
import android.hardware.usb.UsbDevice
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.energy.iruvc.ircmd.IRCMD
import com.energy.iruvc.sdkisp.LibIRProcess
import com.energy.iruvc.usb.USBMonitor
import com.energy.iruvc.utils.*
import com.energy.iruvc.uvc.ConnectCallback
import com.energy.iruvc.uvc.UVCCamera
import com.mpdc4gsr.component.shared.app.common.SaveSettingUtils
import com.mpdc4gsr.component.shared.app.ktbase.BaseFragment
import com.mpdc4gsr.component.shared.app.utils.SharedScreenUtils
import com.mpdc4gsr.component.shared.ir.usbdual.Const
import com.mpdc4gsr.component.shared.ir.usbdual.camera.DualViewWithExternalCameraCommonApi
import com.mpdc4gsr.component.shared.ir.usbdual.camera.IRUVCDual
import com.mpdc4gsr.component.shared.ir.usbdual.camera.USBMonitorManager
import com.mpdc4gsr.component.shared.ir.usbdual.inf.OnUSBConnectListener
import com.mpdc4gsr.component.shared.ir.utils.PseudocodeUtils
import com.mpdc4gsr.component.shared.ir.view.ITsTempListener
import com.mpdc4gsr.component.shared.ir.view.TemperatureView
import com.mpdc4gsr.component.thermal.compat.dpToPx
import com.mpdc4gsr.component.thermal.extension.setAutoShutter
import com.mpdc4gsr.component.thermal.extension.setContrast
import com.mpdc4gsr.component.thermal.extension.setMirror
import com.mpdc4gsr.component.thermal.extension.setPropDdeLevel
import com.mpdc4gsr.component.thermal.repository.ConfigRepository
import com.mpdc4gsr.component.thermal.utils.DualParamsUtils
import com.mpdc4gsr.component.thermal.utils.IRCmdTools
import com.mpdc4gsr.component.thermal.utils.IRCmdTools.getSNStr
import kotlinx.coroutines.*
import java.io.IOException
import java.io.InputStream

abstract class BaseIRPlusFragment :
    BaseFragment(),
    OnUSBConnectListener,
    ITsTempListener,
    IIRFrameCallback {
    val INIT_ALIGN_DATA = floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f)
    protected var dualView: DualViewWithExternalCameraCommonApi? = null
    protected var pseudoColorModeDual = CommonParams.PseudoColorUsbDualType.IRONBOW_MODE
    private var hasStartPreview = false
    protected var ircmd: IRCMD? = null
    protected var snStr = ""
    protected var defaultDataFlowMode = CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT
    private var irPid = 0x5830
    private var irFps = 25
    private var irCameraWidth =
        0
    private var irCameraHeight =
        0
    private var irTempHeight =
        0
    private var imageWidth =
        0
    private var imageHeight =
        0
    protected var temperatureSrc: ByteArray? = null
    protected var mCurrentFusionType = DualParamsUtils.fusionTypeToParams(SaveSettingUtils.fusionType)
    private var syncimage = SynchronizedBitmap()
    protected var isConfigWait = true
    protected var pseudoColorMode = SaveSettingUtils.pseudoColorMode
    private var vlPid = 12337
    private var vlFps = 30
    protected var vlCameraWidth = 1280
    protected var vlCameraHeight = 720
    private var vlData = ByteArray(vlCameraWidth * vlCameraHeight * 3)
    private var dualCameraWidth = 480
    private var dualCameraHeight = 640
    protected var isrun = false
    protected val isUseIRISP = false
    protected var fullScreenlayoutParams: FrameLayout.LayoutParams? = null
    protected var psedocolor: Array<ByteArray>? = null
    protected var dualRotate = 0
    protected var dualDisp = 30
    private var vlUVCCamera: IRUVCDual? = null

    abstract fun getSurfaceView(): SurfaceView

    abstract fun getTemperatureDualView(): TemperatureView

    abstract suspend fun onDualViewCreate(dualView: DualViewWithExternalCameraCommonApi?)

    open fun initdata() {
    }

    open fun initDataFlowMode(dataFlowMode: CommonParams.DataFlowMode) {
        when (dataFlowMode) {
            CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT -> {
                irCameraWidth = 256
                irCameraHeight = 384
                irTempHeight = 192
                imageWidth = irCameraHeight - irTempHeight
                imageHeight = irCameraWidth
                temperatureSrc = ByteArray(imageWidth * imageHeight * 2)
            }

            CommonParams.DataFlowMode.IMAGE_OUTPUT -> {
                irCameraWidth = 256
                irCameraHeight = 192
                irTempHeight = 0
                imageWidth = irCameraHeight - irTempHeight
                imageHeight = irCameraWidth
                temperatureSrc = ByteArray(imageWidth * imageHeight * 2)
            }

            CommonParams.DataFlowMode.TEMP_OUTPUT -> {
                irCameraWidth = 256
                irCameraHeight = 192
                irTempHeight = 0
                imageWidth = irCameraHeight - irTempHeight
                imageHeight = irCameraWidth
                temperatureSrc = ByteArray(imageWidth * imageHeight * 2)
            }

            else -> {
                irCameraWidth = 256
                irCameraHeight = 192
                irTempHeight = 0
                imageWidth = irCameraHeight - irTempHeight
                imageHeight = irCameraWidth
                temperatureSrc = ByteArray(imageWidth * imageHeight * 2)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        dualStart()
    }

    override fun onPause() {
        super.onPause()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    abstract fun isDualIR(): Boolean

    abstract fun setTemperatureViewType()

    override fun initView() {
        if (isDualIR()) {
            getTemperatureDualView().setTextSize(SaveSettingUtils.tempTextSize)
            initDataFlowMode(defaultDataFlowMode)
            initIrDualdata()
        }
    }

    private fun initIrDualdata() {
        var width = 0
        var height = 0
        val screenWidth: Int = SharedScreenUtils.getScreenWidth(requireContext())
        val screenHeight: Int =
            SharedScreenUtils.getScreenHeight(requireContext()) - 52f.dpToPx(requireContext()).toInt()
        if (screenWidth > screenHeight) {
            width = screenHeight * imageWidth / imageHeight
            height = screenHeight
        } else {
            width = screenWidth
            height = screenWidth * imageHeight / imageWidth
        }
        fullScreenlayoutParams =
            FrameLayout.LayoutParams(
                width,
                height,
            )
        getSurfaceView().layoutParams = fullScreenlayoutParams
        getTemperatureDualView().layoutParams = fullScreenlayoutParams
        USBMonitorManager.getInstance().init(irPid, isUseIRISP, defaultDataFlowMode)
        USBMonitorManager.getInstance().addOnUSBConnectListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        mIrHandler.removeCallbacksAndMessages(null)
        USBMonitorManager.getInstance().removeOnUSBConnectListener(this)
    }

    open fun initPseudocolor() {
        val am = requireContext().assets
        var `is`: InputStream? = null
        try {
            psedocolor = Array(11) { ByteArray(0) }
            `is` = am.open("pseudocolor/White_Hot.bin")
            var lenth = `is`.available()
            psedocolor!![0] = ByteArray(lenth + 1)
            if (`is`.read(psedocolor!![0]) != lenth) {
            }
            psedocolor!![0][lenth] = 0
            dualView!!.getDualUVCCamera().loadPseudocolor(
                CommonParams.PseudoColorUsbDualType.WHITE_HOT_MODE,
                psedocolor!![0],
            )
            setFusion(mCurrentFusionType)
            `is`.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                `is`?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    open fun setFusion(fusion: DualCameraParams.FusionType) {
        dualView?.setCurrentFusionType(fusion)
        getTemperatureDualView().setCurrentFusionType(fusion)
        if (fusion == DualCameraParams.FusionType.IROnlyNoFusion) {
            getTemperatureDualView().setImageSize(Const.IR_HEIGHT, Const.IR_WIDTH, null)
        } else {
            getTemperatureDualView().setImageSize(dualCameraWidth, dualCameraHeight, null)
        }
    }

    val calibrationDataSize = 192
    val SAVE_DUAL_BIN = "dual_calibration_parameters2.bin"

    open fun initDefIntegralArgsDISP_VALUE(typeLoadParameters: DualCameraParams.TypeLoadParameters) {
        if (!isDualIR()) {
            return
        }
        lifecycleScope.launch {
            val parameters = IRCmdTools.getDualBytes(USBMonitorManager.getInstance().ircmd)
            val data = dualView?.dualUVCCamera?.loadParameters(parameters, typeLoadParameters)
            dualDisp = IRCmdTools.dispNumber
            setDispViewData(dualDisp)
            dualView?.dualUVCCamera?.setDisp(dualDisp)
            dualView?.startPreview()
        }
    }

    open fun setDispViewData(dualDisp: Int) {
    }

    open fun restartDualCamera() {
        if (isrun) {
            USBMonitorManager.getInstance().isReStart = true
            dualStop()
            SystemClock.sleep(200)
            dualStart()
        }
    }

    override fun onStop() {
        super.onStop()
        dualStop()
    }

    open fun dualStart() {
        if (!isDualIR()) {
            return
        }
        USBMonitorManager.getInstance().registerUSB()
        getTemperatureDualView().setUseIRISP(isUseIRISP)
        if (mCurrentFusionType == DualCameraParams.FusionType.IROnlyNoFusion) {
            getTemperatureDualView().setImageSize(Const.IR_HEIGHT, Const.IR_WIDTH, null)
        } else {
            getTemperatureDualView().setImageSize(dualCameraWidth, dualCameraHeight, null)
        }
        setTemperatureViewType()
        getTemperatureDualView().start()
    }

    var mIrHandler: Handler =
        object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                if (!isDualIR()) {
                    return
                }
                if (msg.what == Const.RESTART_USB) {
                    restartDualCamera()
                } else if (msg.what == Const.HANDLE_CONNECT) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        startVLCamera(vlPid, vlFps, vlCameraWidth, vlCameraHeight)
                        initDualCamera()
                        initDefIntegralArgsDISP_VALUE(DualCameraParams.TypeLoadParameters.ROTATE_270)
                    }
                } else if (msg.what == Const.HANDLE_REGISTER) {
                    USBMonitorManager.getInstance().registerUSB()
                } else if (msg.what == Const.SHOW_LOADING) {
                    showLoadingDialog()
                } else if (msg.what == Const.HIDE_LOADING) {
                    dismissLoadingDialog()
                } else if (msg.what == Const.SHOW_RESTART_MESSAGE) {
                    Toast
                        .makeText(
                            context,
                            "please restart app or reinsert device",
                            Toast.LENGTH_SHORT,
                        ).show()
                    activity?.finish()
                }
            }
        }

    open fun initDualCamera() {
        if (!isDualIR()) {
            return
        }
        if (dualView != null) {
            return
        }
        dualView =
            DualViewWithExternalCameraCommonApi(
                getSurfaceView(),
                USBMonitorManager.getInstance().uvcCamera,
                defaultDataFlowMode,
                irCameraWidth,
                irCameraHeight - irTempHeight,
                vlCameraWidth,
                vlCameraHeight,
                dualCameraWidth,
                dualCameraHeight,
                isUseIRISP,
                dualRotate,
                this,
            )
        dualView?.addFrameCallback(getTemperatureDualView())
        getTemperatureDualView().setDualUVCCamera(dualView!!.getDualUVCCamera())
        initPseudocolor()
        dualView?.setHandler(mIrHandler)
        isrun = true
    }

    open fun startVLCamera(
        pid: Int,
        fps: Int,
        cameraWidth: Int,
        cameraHeight: Int,
    ) {
        if (!isDualIR()) {
            return
        }
        vlUVCCamera =
            IRUVCDual(
                cameraWidth,
                cameraHeight,
                requireContext(),
                pid,
                fps,
                object : ConnectCallback {
                    override fun onCameraOpened(uvcCamera: UVCCamera) {
                    }

                    override fun onIRCMDCreate(ircmd: IRCMD) {
                        setUVCCameraICMD(ircmd)
                    }
                },
                IFrameCallback { frame ->
                    if (dualView != null &&
                        dualView?.getDualUVCCamera() != null &&
                        Const.isDeviceConnected
                    ) {
                        System.arraycopy(frame, 0, vlData, 0, vlData.size)
                        dualView?.getDualUVCCamera()?.updateFrame(
                            ImageFormat.FLEX_RGB_888,
                            vlData,
                            vlCameraWidth,
                            vlCameraHeight,
                        )
                    }
                },
            )
        vlUVCCamera?.setHandler(mIrHandler)
        vlUVCCamera?.registerUSB()
        vlUVCCamera?.TAG = "mjpeg"
    }

    open fun setUVCCameraICMD(ircmd: IRCMD) {
        this@BaseIRPlusFragment.ircmd = ircmd
        snStr = getSNStr(ircmd)
        isConfigWait = false
    }

    override fun onStart() {
        super.onStart()
        if (!isrun) {
            isrun = true
            configParam()
        }
    }

    private var isFirst = true
    private var configJob: Job? = null
    private val timeMillis = 150L

    private fun configParam() {
        configJob =
            lifecycleScope.launch {
                while (isConfigWait && isActive) {
                    delay(200)
                }
                delay(500)
                val config = ConfigRepository.readConfig(false)
                val disChar = (config.distance * 128).toInt()
                val emsChar = (config.radiation * 128).toInt()
                delay(timeMillis)
                ircmd?.setPropTPDParams(
                    CommonParams.PropTPDParams.TPD_PROP_EMS,
                    CommonParams.PropTPDParamsValue.NumberType(emsChar.toString()),
                )
                delay(timeMillis)
                ircmd?.setPropTPDParams(
                    CommonParams.PropTPDParams.TPD_PROP_DISTANCE,
                    CommonParams.PropTPDParamsValue.NumberType(disChar.toString()),
                )
                delay(timeMillis)
                if (isFirst && isrun) {
                    ircmd?.setMirror(false)
                    delay(timeMillis)
                    withContext(Dispatchers.IO) {
                        ircmd?.setAutoShutter(true)
                        isFirst = false
                    }
                    ircmd?.setPropDdeLevel(2)
                    ircmd?.setContrast(128)
                }
                ircmd?.setPropImageParams(
                    CommonParams.PropImageParams.IMAGE_PROP_ONOFF_AGC,
                    CommonParams.PropImageParamsValue.StatusSwith.ON,
                )
                if (syncimage.type == 1) {
                    ircmd?.tc1bShutterManual()
                } else {
                    ircmd?.updateOOCOrB(CommonParams.UpdateOOCOrBType.B_UPDATE)
                }
            }
    }

    open fun dualStop() {
        if (!isDualIR()) {
            return
        }
        isrun = false
        syncimage.valid = false
        isConfigWait = true
        getTemperatureDualView().stop()
        USBMonitorManager.getInstance().unregisterUSB()
        ircmd?.onDestroy()
        ircmd = null
        SystemClock.sleep(100)
        if (dualView != null) {
            dualView?.removeFrameCallback(getTemperatureDualView())
            dualView?.dualUVCCamera?.onPausePreview()
            USBMonitorManager.getInstance().stopPreview()
            if (vlUVCCamera != null) {
                vlUVCCamera?.unregisterUSB()
                vlUVCCamera?.stopPreview()
                vlUVCCamera = null
            }
            SystemClock.sleep(100)
            dualView?.stopPreview()
            dualView = null
        }
    }

    override fun onAttach(device: UsbDevice?) {
    }

    override fun onGranted(
        usbDevice: UsbDevice?,
        granted: Boolean,
    ) {
    }

    override fun onDettach(device: UsbDevice?) {
    }

    override fun onConnect(
        device: UsbDevice?,
        ctrlBlock: USBMonitor.UsbControlBlock?,
        createNew: Boolean,
    ) {
        mIrHandler.sendEmptyMessage(Const.HANDLE_CONNECT)
    }

    override fun onDisconnect(
        device: UsbDevice?,
        ctrlBlock: USBMonitor.UsbControlBlock?,
    ) {
    }

    override fun onCancel(device: UsbDevice?) {
    }

    override fun onIRCMDInit(ircmd: IRCMD?) {
        setUVCCameraICMD(ircmd!!)
    }

    override fun onCompleteInit() {
        mIrHandler.sendEmptyMessage(Const.HIDE_LOADING)
    }

    override fun onSetPreviewSizeFail() {
        mIrHandler.sendEmptyMessage(Const.SHOW_RESTART_MESSAGE)
    }

    protected val preIrARGBData = ByteArray(256 * 192 * 4)
    protected val preIrData = ByteArray(256 * 192 * 2)
    protected val preTempData = ByteArray(256 * 192 * 2)

    override fun onIrFrame(irFrame: ByteArray?): ByteArray {
        irFrame?.let {
            System.arraycopy(it, 0, preIrData, 0, preIrData.size)
        } ?: return preIrARGBData
        LibIRProcess.convertYuyvMapToARGBPseudocolor(
            preIrData,
            (Const.IR_WIDTH * Const.IR_HEIGHT).toLong(),
            PseudocodeUtils.changePseudocodeModeByOld(pseudoColorMode),
            preIrARGBData,
        )
        return preIrARGBData
    }
}




