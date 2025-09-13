package com.topdon.module.thermal.ir.fragment

import android.graphics.Bitmap
import android.util.Log
import android.view.ViewGroup
import android.view.WindowManager
import android.yt.jni.Usbcontorl
import androidx.lifecycle.lifecycleScope
import com.elvishew.xlog.XLog
import com.energy.iruvc.ircmd.IRCMD
import com.energy.iruvc.utils.CommonParams
import com.energy.iruvc.utils.SynchronizedBitmap
import com.energy.iruvc.uvc.ConnectCallback
import com.energy.iruvc.uvc.UVCCamera
import com.infisense.usbir.camera.IRUVCTC
import com.infisense.usbir.config.MsgCode
import com.infisense.usbir.event.IRMsgEvent
import com.infisense.usbir.event.PreviewComplete
import com.infisense.usbir.thread.ImageThreadTC
import com.infisense.usbir.utils.USBMonitorCallback
import com.infisense.usbir.view.CameraView
import com.infisense.usbir.view.ITsTempListener
import com.infisense.usbir.view.TemperatureView
import com.infisense.usbir.view.TemperatureView.*
import com.topdon.lib.core.bean.event.device.DeviceCameraEvent
import com.topdon.lib.core.common.SaveSettingUtil
import com.topdon.lib.core.config.DeviceConfig
import com.topdon.lib.core.ktbase.BaseFragment
import com.topdon.lib.core.utils.ScreenUtil
import com.topdon.module.thermal.ir.R
import com.topdon.module.thermal.ir.repository.ConfigRepository
import com.topdon.module.thermal.ir.utils.CalibrationTools
import kotlinx.coroutines.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
thermal imaging选取point
 */
/**
 * I r correction fragment for thermal imaging components.
 * Handles specific UI sections and user interactions.
 */
class IRCorrectionFragment : BaseFragment(), ITsTempListener {

默认data流mode：image+temperature复合data */
    protected var defaultDataFlowMode = CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT

    private var ircmd: IRCMD? = null

    // View references
    private lateinit var temperatureView: TemperatureView
    private var cameraView: CameraView? = null
    private lateinit var thermalLay: ViewGroup

    override fun initContentView() = R.layout.fragment_ir_monitor_thermal

    private var rotateAngle = 270 // 校对默认角度270

    override fun initView() {
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Initialize views with findViewById
        temperatureView = requireView().findViewById<TemperatureView>(R.id.temperatureView)
        cameraView = requireView().findViewById<CameraView>(R.id.cameraView)
        thermalLay = requireView().findViewById<ViewGroup>(R.id.thermal_lay)

        initDataIR()
    }

    override fun initData() {
    }

    private var imageThread: ImageThreadTC? = null
    private var bitmap: Bitmap? = null
    private var iruvc: IRUVCTC? = null
    private val cameraWidth = 256
    private val cameraHeight = 384
    private val tempHeight = 192
    private var imageWidth = cameraWidth
    private var imageHeight = cameraHeight - tempHeight
    private val image = ByteArray(imageWidth * imageHeight * 2)
    private val temperature = ByteArray(imageWidth * imageHeight * 2)
    private val syncimage = SynchronizedBitmap()
    private var isrun = false
    private var pseudocolorMode = 0

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun irEvent(event: IRMsgEvent) {
        if (event.code == MsgCode.RESTART_USB) {
            restartUsbCamera()
        }
    }

    /**
初始data
     */
    private fun initDataIR() {
        imageWidth = cameraHeight - tempHeight
        imageHeight = cameraWidth
        temperatureView.setTextSize(SaveSettingUtil.tempTextSize)
        if (ScreenUtil.isPortrait(requireContext())) {
            bitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)
            temperatureView.setImageSize(imageWidth, imageHeight, this@IRCorrectionFragment)
            rotateAngle = DeviceConfig.S_ROTATE_ANGLE
        } else {
            bitmap = Bitmap.createBitmap(imageHeight, imageWidth, Bitmap.Config.ARGB_8888)
            temperatureView.setImageSize(imageHeight, imageWidth, this@IRCorrectionFragment)
            rotateAngle = DeviceConfig.ROTATE_ANGLE
        }
        cameraView?.let { camera ->
            camera.setSyncimage(syncimage)
            camera.bitmap = bitmap
            camera.isDrawLine = false
        }
        temperatureView.setSyncimage(syncimage)
        temperatureView.setTemperature(temperature)
        temperatureView.isEnabled = false
        setViewLay()
某些特定客户的特殊device需要使用该Commanddisabledsensor
        if (Usbcontorl.isload) {
            Usbcontorl.usb3803_mode_setting(1) // Open5V
            Log.w("123", "Open5V")
        }
        temperatureView.clear()
        temperatureView.temperatureRegionMode = REGION_MODE_CLEAN
    }

    /**
image信号processing
     */
    private fun startISP() {
        try {
            imageThread = ImageThreadTC(context, imageWidth, imageHeight)
            imageThread!!.setDataFlowMode(defaultDataFlowMode)
            imageThread!!.setSyncImage(syncimage)
            imageThread!!.setImageSrc(image)
            imageThread!!.setTemperatureSrc(temperature)
            imageThread!!.setBitmap(bitmap)
            imageThread?.setRotate(rotateAngle)
            imageThread!!.setRotate(true)
            imageThread!!.start()
        } catch (e: Exception) {
            Log.e("imageline程重复启动", e.message.toString())
        }
    }

    
    private fun startUSB(isRestart: Boolean) {
        context?.let {
            iruvc =
                IRUVCTC(
                    cameraWidth, cameraHeight, context, syncimage,
                    defaultDataFlowMode,
                    object : ConnectCallback {
                        override fun onCameraOpened(uvcCamera: UVCCamera) {
                        }

                        override fun onIRCMDCreate(ircmd: IRCMD) {
                            Log.i(
                                TAG,
                                "ConnectCallback->onIRCMDCreate",
                            )
                            this@IRCorrectionFragment.ircmd = ircmd
需要等IRCMDinitializecomplete之后才可以调用
//                        ircmd.setPseudoColor(CommonParams.PreviewPathChannel.PREVIEW_PATH0,
//                            PseudocodeUtils.changePseudocodeModeByOld(pseudocolorMode))
                        }
                    },
                    object : USBMonitorCallback {
                        override fun onAttach() {}

                        override fun onGranted() {}

                        override fun onConnect() {}

                        override fun onDisconnect() {}

                        override fun onDettach() {
                            activity?.finish()
                        }

                        override fun onCancel() {
                            activity?.finish()
                        }
                    },
                )
            iruvc!!.isRestart = isRestart
            iruvc!!.setImageSrc(image)
            iruvc!!.setTemperatureSrc(temperature)
            iruvc!!.setRotate(rotateAngle)
            iruvc!!.registerUSB()
        }
    }

    
    private fun restartUsbCamera() {
        if (iruvc != null) {
            iruvc!!.stopPreview()
            iruvc!!.unregisterUSB()
        }
        startUSB(true)
    }

    override fun onStart() {
        super.onStart()
        Log.w(TAG, "onStart")
        if (!isrun) {
初始configuration,pseudo-coloriron red
            temperatureView.postDelayed({
                pseudocolorMode = 3
                startUSB(false)
                startISP()
                temperatureView.start()
                cameraView?.start()
                isrun = true
Restoreconfiguration
                configParam()
            }, 1500)
        }
    }

    override fun onStop() {
        super.onStop()
        Log.w(TAG, "onStop")
        if (iruvc != null) {
            iruvc!!.stopPreview()
            iruvc!!.unregisterUSB()
        }
        imageThread?.interrupt()
        syncimage.valid = false
        temperatureView.stop()
        cameraView?.stop()
        isrun = false
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.w(TAG, "onDestroy")
        try {
            imageThread?.join()
        } catch (e: InterruptedException) {
            Log.e(TAG, "imageThread.join(): catch an interrupted exception")
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun iruvctc(event: PreviewComplete) {
        dealY16ModePreviewComplete()
    }

    var frameReady = false

    private fun dealY16ModePreviewComplete() {
        isConfigWait = false
        iruvc?.setFrameReady(true)
        frameReady = true
    }

    private fun setViewLay() {
        thermalLay.post {
            if (ScreenUtil.isPortrait(requireContext())) {
                val params = thermalLay.layoutParams
                params.width = ScreenUtil.getScreenWidth(requireContext())
                params.height = params.width * imageHeight / imageWidth
                thermalLay.layoutParams = params
            } else {
横屏
                val params = thermalLay.layoutParams
                params.height = thermalLay.height
                params.width = params.height * imageHeight / imageWidth
                thermalLay.layoutParams = params
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun cameraEvent(event: DeviceCameraEvent) {
        when (event.action) {
            100 -> {
准备image
                showLoadingDialog()
            }
            101 -> {
displayimage
                lifecycleScope.launch {
                    delay(500)
                    isConfigWait = false
                    delay(1000)
                    dismissLoadingDialog()
                }
            }
        }
    }

    private var isConfigWait = true

configuration
    private fun configParam() {
        lifecycleScope.launch {
            isConfigWait = true
            while (isConfigWait) {
                delay(100)
            }
            val config = ConfigRepository.readConfig(false)
            val disChar = (config.distance * 128).toInt() // 距离(米)
            val emsChar = (config.radiation * 128).toInt() // 发射率
            XLog.w("settingsTPD_PROP DISTANCE:$disChar, EMS:$emsChar}")
            val timeMillis = 250L
            delay(timeMillis)
emissivity
            ircmd?.setPropTPDParams(
                CommonParams.PropTPDParams.TPD_PROP_EMS,
                CommonParams.PropTPDParamsValue.NumberType(emsChar.toString()),
            )
            delay(timeMillis)
距离
            ircmd?.setPropTPDParams(
                CommonParams.PropTPDParams.TPD_PROP_DISTANCE,
                CommonParams.PropTPDParamsValue.NumberType(disChar.toString()),
            )
自动快门
            delay(timeMillis)
            ircmd?.zoomCenterDown(
                CommonParams.PreviewPathChannel.PREVIEW_PATH0,
                CommonParams.ZoomScaleStep.ZOOM_STEP2,
            )
            delay(timeMillis)
            ircmd?.zoomCenterDown(
                CommonParams.PreviewPathChannel.PREVIEW_PATH0,
                CommonParams.ZoomScaleStep.ZOOM_STEP2,
            )
            delay(timeMillis)
            ircmd?.zoomCenterDown(
                CommonParams.PreviewPathChannel.PREVIEW_PATH0,
                CommonParams.ZoomScaleStep.ZOOM_STEP2,
            )
            delay(timeMillis)
            ircmd?.zoomCenterDown(
                CommonParams.PreviewPathChannel.PREVIEW_PATH0,
                CommonParams.ZoomScaleStep.ZOOM_STEP2,
            )
            iruvc?.let {
部分机型在disabled自动快门，初始会花屏
                withContext(Dispatchers.IO) {
                    if (SaveSettingUtil.isAutoShutter) {
                        ircmd?.setPropAutoShutterParameter(
                            CommonParams.PropAutoShutterParameter.SHUTTER_PROP_SWITCH,
                            CommonParams.PropAutoShutterParameterValue.StatusSwith.ON,
                        )
                    } else
                        {
                            ircmd?.setPropAutoShutterParameter(
                                CommonParams.PropAutoShutterParameter.SHUTTER_PROP_SWITCH,
                                CommonParams.PropAutoShutterParameterValue.StatusSwith.OFF,
                            )
                        }
                }
            }
复位contrast、细节
            delay(timeMillis)
            ircmd?.setPropImageParams(
                CommonParams.PropImageParams.IMAGE_PROP_LEVEL_CONTRAST,
                CommonParams.PropImageParamsValue.NumberType(128.toString()),
            )
            delay(timeMillis)
            ircmd?.setPropImageParams(
                CommonParams.PropImageParams.IMAGE_PROP_LEVEL_DDE,
                CommonParams.PropImageParamsValue.DDEType.DDE_2,
            )
            delay(timeMillis)
            ircmd?.setPropImageParams(
                CommonParams.PropImageParams.IMAGE_PROP_ONOFF_AGC,
                CommonParams.PropImageParamsValue.StatusSwith.ON,
            )
        }
    }

    suspend fun autoStart() {
        withContext(Dispatchers.IO) {
ToastUtils.showShort("taskstart")
锅盖start
1 锅盖calibrationstart
2 disabled自动快门
            CalibrationTools.autoShutter(irCmd = ircmd, false)
            XLog.w("锅盖矫正：" + "锅盖calibrationstart")
常温
3 手动打快门Command
//            CalibrationTools.shutter(irCmd = ircmd, syncImage = syncimage)
XLog.w("锅盖矫正："+"手动打快门Command")
4 disabled锅盖校正
            delay(2000)
            XLog.w("锅盖矫正：" + "Close锅盖校正")
            CalibrationTools.stsSwitch(irCmd = ircmd, false)
5 Send锅盖标
            CalibrationTools.pot(irCmd = ircmd!!, 1)
            XLog.w("锅盖矫正：" + "Send锅盖标")
6 Open锅盖校正
            delay(5000)
            XLog.w("锅盖矫正：" + "Open锅盖校正")
            CalibrationTools.stsSwitch(irCmd = ircmd, true)
            delay(20000)
            XLog.w("锅盖矫正：" + "20000")
high temperature
11 手动打快门Command
//            CalibrationTools.shutter(irCmd = ircmd, syncImage = syncimage)
XLog.w("锅盖矫正："+"手动打快门Command")
12 disabled锅盖校正
            delay(2000)
            CalibrationTools.stsSwitch(irCmd = ircmd, false)
            XLog.w("锅盖矫正：" + "Close锅盖校正")
13 Send锅盖标
            CalibrationTools.pot(irCmd = ircmd!!, 1)
14 Open锅盖校正
            delay(5000)
            XLog.w("锅盖矫正：" + "Open锅盖校正")
            CalibrationTools.stsSwitch(irCmd = ircmd, true)
17 Open自动快门
            CalibrationTools.autoShutter(irCmd = ircmd, true)
锅盖end
            XLog.w("锅盖矫正：" + "锅盖end")
        }
    }
}
