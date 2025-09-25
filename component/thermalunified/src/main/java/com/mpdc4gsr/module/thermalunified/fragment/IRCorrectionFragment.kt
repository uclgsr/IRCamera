package com.mpdc4gsr.module.thermalunified.fragment

import android.graphics.Bitmap
import android.util.Log
import android.view.ViewGroup
import android.view.WindowManager
import androidx.lifecycle.lifecycleScope
import com.elvishew.xlog.XLog
import com.energy.iruvc.ircmd.IRCMD
import com.energy.iruvc.utils.CommonParams
import com.energy.iruvc.utils.SynchronizedBitmap
import com.energy.iruvc.uvc.ConnectCallback
import com.energy.iruvc.uvc.UVCCamera
import com.mpdc4gsr.libunified.app.common.SaveSettingUtil
import com.mpdc4gsr.libunified.app.config.DeviceConfig
import com.mpdc4gsr.libunified.app.ktbase.BaseFragment
import com.mpdc4gsr.libunified.app.utils.ScreenUtil
import com.mpdc4gsr.libunified.ir.android.yt.jni.Usbcontorl
import com.mpdc4gsr.libunified.ir.camera.IRUVCTC
import com.mpdc4gsr.libunified.ir.config.MsgCode
import com.mpdc4gsr.libunified.ir.event.IRMsgEvent
import com.mpdc4gsr.libunified.ir.event.PreviewComplete
import com.mpdc4gsr.libunified.ir.thread.ImageThreadTC
import com.mpdc4gsr.libunified.ir.utils.USBMonitorCallback
import com.mpdc4gsr.libunified.ir.view.CameraView
import com.mpdc4gsr.libunified.ir.view.ITsTempListener
import com.mpdc4gsr.libunified.ir.view.TemperatureView
import com.mpdc4gsr.libunified.ir.view.TemperatureView.REGION_MODE_CLEAN
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.repository.ConfigRepository
import com.mpdc4gsr.module.thermalunified.utils.CalibrationTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class IRCorrectionFragment : BaseFragment(), ITsTempListener {

    protected var defaultDataFlowMode = CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT

    private var ircmd: IRCMD? = null

    private lateinit var temperatureView: TemperatureView
    private var cameraView: CameraView? = null
    private lateinit var thermalLay: ViewGroup

    override fun initContentView() = R.layout.fragment_ir_monitor_thermal

    private var rotateAngle = 270

    override fun initView() {
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

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
        when (event.code) {
            MsgCode.RESTART_USB -> {
                restartUsbCamera()
            }

            100 -> {
                showLoadingDialog()
            }

            101 -> {
                lifecycleScope.launch {
                    delay(500)
                    isConfigWait = false
                    delay(1000)
                    dismissLoadingDialog()
                }
            }
        }
    }

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

        if (Usbcontorl.isload) {
            Usbcontorl.usb3803_mode_setting(1)
            Log.w("123", "打开5V")
        }
        temperatureView.clear()
        temperatureView.temperatureRegionMode = REGION_MODE_CLEAN
    }

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
            Log.e("图像线程重复启动", e.message.toString())
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

            temperatureView.postDelayed({
                pseudocolorMode = 3
                startUSB(false)
                startISP()
                temperatureView.start()
                cameraView?.start()
                isrun = true

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

                val params = thermalLay.layoutParams
                params.height = thermalLay.height
                params.width = params.height * imageHeight / imageWidth
                thermalLay.layoutParams = params
            }
        }
    }


    private var isConfigWait = true

    private fun configParam() {
        lifecycleScope.launch {
            isConfigWait = true
            while (isConfigWait) {
                delay(100)
            }
            val config = ConfigRepository.readConfig(false)
            val disChar = (config.distance * 128).toInt()
            val emsChar = (config.radiation * 128).toInt()
            XLog.w("设置TPD_PROP DISTANCE:$disChar, EMS:$emsChar}")
            val timeMillis = 250L
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

                withContext(Dispatchers.IO) {
                    if (SaveSettingUtil.isAutoShutter) {
                        ircmd?.setPropAutoShutterParameter(
                            CommonParams.PropAutoShutterParameter.SHUTTER_PROP_SWITCH,
                            CommonParams.PropAutoShutterParameterValue.StatusSwith.ON,
                        )
                    } else {
                        ircmd?.setPropAutoShutterParameter(
                            CommonParams.PropAutoShutterParameter.SHUTTER_PROP_SWITCH,
                            CommonParams.PropAutoShutterParameterValue.StatusSwith.OFF,
                        )
                    }
                }
            }

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


            CalibrationTools.autoShutter(irCmd = ircmd, false)
            XLog.w("锅盖矫正：" + "锅盖标定开始")





            delay(2000)
            XLog.w("锅盖矫正：" + "关闭锅盖校正")
            CalibrationTools.stsSwitch(irCmd = ircmd, false)

            CalibrationTools.pot(irCmd = ircmd!!, 1)
            XLog.w("锅盖矫正：" + "发送锅盖标")

            delay(5000)
            XLog.w("锅盖矫正：" + "打开锅盖校正")
            CalibrationTools.stsSwitch(irCmd = ircmd, true)
            delay(20000)
            XLog.w("锅盖矫正：" + "20000")





            delay(2000)
            CalibrationTools.stsSwitch(irCmd = ircmd, false)
            XLog.w("锅盖矫正：" + "关闭锅盖校正")

            CalibrationTools.pot(irCmd = ircmd!!, 1)

            delay(5000)
            XLog.w("锅盖矫正：" + "打开锅盖校正")
            CalibrationTools.stsSwitch(irCmd = ircmd, true)

            CalibrationTools.autoShutter(irCmd = ircmd, true)

            XLog.w("锅盖矫正：" + "锅盖结束")
        }
    }
}
