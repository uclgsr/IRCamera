package com.mpdc4gsr.module.thermal.ir.activity

import android.graphics.Bitmap
import android.view.SurfaceView
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ToastUtils
import com.energy.iruvc.sdkisp.LibIRProcess
import com.energy.iruvc.utils.CommonParams
import com.energy.iruvc.utils.DualCameraParams
import com.infisense.usbdual.Const
import com.infisense.usbir.utils.IRImageHelp
import com.infisense.usbir.utils.PseudocodeUtils
import com.infisense.usbir.view.TemperatureView
import com.mpdc4gsr.lib.core.bean.CameraItemBean
import com.mpdc4gsr.lib.core.common.ProductType.PRODUCT_NAME_TCP
import com.mpdc4gsr.lib.core.common.SaveSettingUtil
import com.mpdc4gsr.lib.core.tools.ToastTools
import com.mpdc4gsr.menu.constant.TwoLightType
import com.mpdc4gsr.module.thermal.ir.R
import com.mpdc4gsr.module.thermal.ir.event.GalleryAddEvent
import com.mpdc4gsr.module.thermal.ir.video.VideoRecordFFmpeg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus


class IRThermalPlusActivity : BaseIRPlushActivity() {
    private val irImageHelp by lazy {
        IRImageHelp()
    }

    private val dualTextureViewNativeCamera by lazy { findViewById<SurfaceView>(R.id.dualTextureViewNativeCamera) }


    override fun initContentView() = R.layout.activity_ir_thermal_double

    override fun isDualIR(): Boolean {
        return true
    }

    override fun getSurfaceView(): SurfaceView {
        return dualTextureViewNativeCamera
    }

    override fun getTemperatureDualView(): TemperatureView {
        return temperatureView
    }

    override fun getProductName(): String {
        return PRODUCT_NAME_TCP
    }

    override fun initView() {
        super.initView()

        cameraView.visibility = View.GONE
        dualTextureViewNativeCamera?.visibility = View.VISIBLE




        when (SaveSettingUtil.fusionType) {
            SaveSettingUtil.FusionTypeLPYFusion -> {
                thermalRecyclerNight?.twoLightType = TwoLightType.TWO_LIGHT_1
            }

            SaveSettingUtil.FusionTypeMeanFusion -> {
                thermalRecyclerNight?.twoLightType = TwoLightType.TWO_LIGHT_2
            }

            SaveSettingUtil.FusionTypeIROnly -> {
                thermalRecyclerNight?.twoLightType = TwoLightType.IR
            }

            SaveSettingUtil.FusionTypeVLOnly -> {
                thermalRecyclerNight?.twoLightType = TwoLightType.LIGHT
            }
        }
    }


    private fun setDisp(
        action: Int,
        data: Int,
    ) {
        if (action == -1 || action == 1) {

            lifecycleScope.launch(Dispatchers.IO) {
                dualDisp = data
                dualView?.dualUVCCamera!!.setDisp(data)
            }
        } else {

            val oemInfo = ByteArray(1024)
            ircmd?.oemRead(CommonParams.ProductType.P2, oemInfo)
            val dataStr = data.toString()
            System.arraycopy(dataStr.toByteArray(), 0, oemInfo, 194, dataStr.toByteArray().size)
            val result = ircmd?.oemWrite(CommonParams.ProductType.P2, oemInfo)

            if (result == 0) {


                thermalRecyclerNight.setTwoLightSelected(TwoLightType.CORRECT, false)

            } else {
                ToastUtils.showShort(R.string.correction_fail)
            }
        }
    }

    override fun setTwoLight(
        twoLightType: TwoLightType,
        isSelected: Boolean,
    ) {
        when (twoLightType) {
            TwoLightType.TWO_LIGHT_1 -> {
                mCurrentFusionType = DualCameraParams.FusionType.LPYFusion
                SaveSettingUtil.fusionType = SaveSettingUtil.FusionTypeLPYFusion
                setFusion(mCurrentFusionType)
            }

            TwoLightType.TWO_LIGHT_2 -> {
                mCurrentFusionType = DualCameraParams.FusionType.MeanFusion
                SaveSettingUtil.fusionType = SaveSettingUtil.FusionTypeMeanFusion
                setFusion(mCurrentFusionType)
            }

            TwoLightType.IR -> {
                mCurrentFusionType = DualCameraParams.FusionType.IROnly
                SaveSettingUtil.fusionType = SaveSettingUtil.FusionTypeIROnly
                setFusion(mCurrentFusionType)
                thermalRecyclerNight.setTwoLightSelected(TwoLightType.CORRECT, false)

            }

            TwoLightType.LIGHT -> {
                mCurrentFusionType = DualCameraParams.FusionType.VLOnly
                SaveSettingUtil.fusionType = SaveSettingUtil.FusionTypeVLOnly
                setFusion(mCurrentFusionType)

                thermalRecyclerNight.setTwoLightSelected(TwoLightType.CORRECT, false)
            }

            TwoLightType.CORRECT -> {
                if (isSelected) {

                    if (mCurrentFusionType != DualCameraParams.FusionType.LPYFusion && mCurrentFusionType != DualCameraParams.FusionType.MeanFusion) {
                        mCurrentFusionType = DualCameraParams.FusionType.LPYFusion
                        thermalRecyclerNight.twoLightType = TwoLightType.TWO_LIGHT_1
                        SaveSettingUtil.fusionType = SaveSettingUtil.FusionTypeLPYFusion
                        setFusion(DualCameraParams.FusionType.LPYFusion)
                    }
                } else {

                }
            }

            else -> {
                super.setTwoLight(twoLightType, isSelected)
            }
        }
    }

    override fun getCameraViewBitmap(): Bitmap {
        if (imageEditBytes.size != dualView?.frameIrAndTempData?.size) {
            imageEditBytes = ByteArray(dualView!!.frameIrAndTempData.size)
        }
        System.arraycopy(dualView!!.frameIrAndTempData, 0, imageEditBytes, 0, imageEditBytes.size)
        return dualView?.scaledBitmap!!
    }

    override fun setTemperatureViewType() {
        temperatureView.productType = Const.TYPE_IR_DUAL
        cameraView.productType = Const.TYPE_IR_DUAL
    }

    override fun startUSB(
        isRestart: Boolean,
        isBadFrames: Boolean,
    ) {

    }

    override fun setPColor(code: Int) {
        pseudoColorMode = code
        temperatureSeekbar.setPseudocode(pseudoColorMode)


        SaveSettingUtil.pseudoColorMode = pseudoColorMode
        thermalRecyclerNight.setPseudoColor(code)
    }

    override fun startISP() {
        setCustomPseudoColorList(
            customPseudoBean.getColorList(),
            customPseudoBean.getPlaceList(),
            customPseudoBean.isUseGray,
            customPseudoBean.maxTemp,
            customPseudoBean.minTemp,
        )
    }

    override fun setCustomPseudoColorList(
        colorList: IntArray?,
        places: FloatArray?,
        isUseGray: Boolean,
        customMaxTemp: Float,
        customMinTemp: Float,
    ) {
        irImageHelp.setColorList(colorList, places, isUseGray, customMaxTemp, customMinTemp)
    }

    override fun setRotate(rotateInt: Int) {
        super.setRotate(rotateInt)
        runOnUiThread {

        }

        when (rotateInt) {
            0 -> dualView?.dualUVCCamera?.setImageRotate(DualCameraParams.TypeLoadParameters.ROTATE_90)
            90 -> dualView?.dualUVCCamera?.setImageRotate(DualCameraParams.TypeLoadParameters.ROTATE_180)
            180 -> dualView?.dualUVCCamera?.setImageRotate(DualCameraParams.TypeLoadParameters.ROTATE_270)
            270 -> dualView?.dualUVCCamera?.setImageRotate(DualCameraParams.TypeLoadParameters.ROTATE_0)
        }
    }

    override fun onIrFrame(irFrame: ByteArray?): ByteArray {
        System.arraycopy(irFrame, 0, preIrData, 0, preIrData.size)
        System.arraycopy(irFrame, preIrData.size, preTempData, 0, preTempData.size)
        if (irImageHelp.getColorList() != null) {

            LibIRProcess.convertYuyvMapToARGBPseudocolor(
                preIrData,
                (Const.IR_WIDTH * Const.IR_HEIGHT).toLong(),
                CommonParams.PseudoColorType.PSEUDO_1,
                preIrARGBData,
            )
        } else {
            LibIRProcess.convertYuyvMapToARGBPseudocolor(
                preIrData,
                (Const.IR_WIDTH * Const.IR_HEIGHT).toLong(),
                PseudocodeUtils.changePseudocodeModeByOld(pseudoColorMode),
                preIrARGBData,
            )
        }
        irImageHelp.customPseudoColor(preIrARGBData, preTempData, Const.IR_WIDTH, Const.IR_HEIGHT)

        irImageHelp.setPseudoColorMaxMin(
            preIrARGBData,
            preTempData,
            editMaxValue,
            editMinValue,
            Const.IR_WIDTH,
            Const.IR_HEIGHT,
        )

        val tempData =
            irImageHelp.contourDetection(
                alarmBean,
                preIrARGBData,
                preTempData,
                Const.IR_HEIGHT,
                Const.IR_WIDTH,
            )
        System.arraycopy(tempData, 0, preIrARGBData, 0, preIrARGBData.size)
        return preIrARGBData
    }

    override fun irStop() {
        try {
            configJob?.cancel()

            if (isVideo) {
                isVideo = false
                videoRecord?.stopRecord()
                videoTimeClose()
                CoroutineScope(Dispatchers.Main).launch {
                    delay(500)
                    EventBus.getDefault().post(GalleryAddEvent())
                }
                lifecycleScope.launch {
                    delay(500)
                    thermalRecyclerNight.refreshImg()
                }
            }
        } catch (_: Exception) {
        } finally {
            ircmd?.onDestroy()
            ircmd = null
        }
    }

    override fun initVideoRecordFFmpeg() {
        videoRecord =
            VideoRecordFFmpeg(
                cameraView,
                cameraPreview,
                temperatureView,
                curChooseTabPos == 1,
                cl_seek_bar,
                temp_bg,
                compassView, dualView,
                carView = layCarDetectPrompt,
            )
    }

    override fun irStart() {
        if (!isrun) {
            tvTypeInd.isVisible = false
            startUSB(false, false)
            startISP()
            isrun = true

            configParam()
            thermalRecyclerNight.updateCameraModel()
            initIRConfig()
        }
    }

    override fun setDispViewData(dualDisp: Int) {

    }

    override fun autoConfig() {
        lifecycleScope.launch(Dispatchers.IO) {
            dualView?.let {
                if (!it.auto_gain_switch) {
                    switchAutoGain(true)
                    ToastTools.showShort(R.string.auto_open)
                }
                gainSelChar = CameraItemBean.TYPE_TMP_ZD
            }
        }
        dismissCameraLoading()
        thermalRecyclerNight.setTempLevel(CameraItemBean.TYPE_TMP_ZD)
    }

    override fun switchAutoGain(boolean: Boolean) {
        dualView?.auto_gain_switch = boolean
    }
}
