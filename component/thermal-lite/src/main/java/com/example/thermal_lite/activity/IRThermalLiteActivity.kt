package com.example.thermal_lite.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.hardware.SensorManager
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.OrientationEventListener
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.PopupWindow
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.view.drawToBitmap
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.SizeUtils
import com.blankj.utilcode.util.ToastUtils
import com.elvishew.xlog.XLog
import com.energy.ac020library.bean.IrcmdError
import com.energy.commoncomponent.bean.RotateDegree
import com.energy.irutilslibrary.LibIRTempAC020
import com.energy.irutilslibrary.bean.GainStatus
import com.energy.iruvc.sdkisp.LibIRProcess
import com.energy.iruvc.utils.CommonParams
import com.energy.iruvc.utils.SynchronizedBitmap
import com.energy.iruvccamera.usb.USBMonitor
import com.example.thermal_lite.IrConst
import com.example.thermal_lite.R
import com.example.thermal_lite.camera.CameraPreviewManager
import com.example.thermal_lite.camera.DeviceControlManager
import com.example.thermal_lite.camera.DeviceIrcmdControlManager
import com.example.thermal_lite.camera.OnUSBConnectListener
import com.example.thermal_lite.camera.TempCompensation
import com.example.thermal_lite.camera.USBMonitorManager
import com.example.thermal_lite.databinding.ActivityIrThermalLiteBinding
import com.example.thermal_lite.util.CommonUtil
import com.example.thermal_lite.util.IRTool
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.infisense.usbir.inf.ILiteListener
import com.infisense.usbir.utils.ViewStubUtils
import com.infisense.usbir.view.ITsTempListener
import com.infisense.usbir.view.TemperatureView
import com.infisense.usbir.view.TemperatureView.GONE
import com.infisense.usbir.view.TemperatureView.REGION_MODE_CENTER
import com.infisense.usbir.view.TemperatureView.REGION_MODE_CLEAN
import com.infisense.usbir.view.TemperatureView.REGION_MODE_LINE
import com.infisense.usbir.view.TemperatureView.REGION_MODE_POINT
import com.infisense.usbir.view.TemperatureView.REGION_MODE_RECTANGLE
import com.infisense.usbir.view.TemperatureView.REGION_NODE_TREND
import com.infisense.usbir.view.TemperatureView.VISIBLE
import com.topdon.lib.core.BaseApplication
import com.topdon.lib.core.bean.CameraItemBean
import com.topdon.lib.core.comm.IrParam
import com.topdon.lib.core.common.ProductType
import com.topdon.lib.core.common.SaveSettingUtil
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.common.SharedManager.getTemperature
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.dialog.EmissivityTipPopup
import com.topdon.lib.core.dialog.NotTipsSelectDialog
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.dialog.TipEmissivityDialog
import com.topdon.lib.core.dialog.TipShutterDialog
import com.topdon.lib.core.navigation.NavigationManager
import com.topdon.lib.core.repository.GalleryRepository
import com.topdon.lib.core.tools.CheckDoubleClick
import com.topdon.lib.core.tools.NumberTools
import com.topdon.lib.core.tools.SpanBuilder
import com.topdon.lib.core.tools.TimeTool
import com.topdon.lib.core.tools.ToastTools
import com.topdon.lib.core.tools.UnitTools
import com.topdon.lib.core.utils.BitmapUtils
import com.topdon.lib.core.utils.CommUtils
import com.topdon.lib.core.utils.ImageUtils
import com.topdon.lib.core.utils.ScreenUtil
import com.topdon.lib.ui.dialog.TipPreviewDialog
import com.topdon.lib.ui.widget.seekbar.OnRangeChangedListener
import com.topdon.lib.ui.widget.seekbar.RangeSeekBar
import com.topdon.libcom.AlarmHelp
import com.topdon.libcom.dialog.ColorPickDialog
import com.topdon.libcom.dialog.TempAlarmSetDialog
import com.topdon.lms.sdk.LMS.mContext
import com.topdon.menu.constant.FenceType
import com.topdon.menu.constant.SettingType
import com.topdon.menu.constant.TwoLightType
import com.topdon.module.thermal.ir.activity.BaseIRActivity
import com.topdon.module.thermal.ir.adapter.CameraItemAdapter
import com.topdon.module.thermal.ir.bean.DataBean
import com.topdon.module.thermal.ir.event.GalleryAddEvent
import com.topdon.module.thermal.ir.frame.FrameStruct
import com.topdon.module.thermal.ir.popup.SeekBarPopup
import com.topdon.module.thermal.ir.repository.ConfigRepository
import com.topdon.module.thermal.ir.utils.IRConfigData
import com.topdon.module.thermal.ir.video.VideoRecordFFmpeg
import com.topdon.module.thermal.ir.view.TimeDownView
import com.topdon.pseudo.activity.PseudoSetActivity
import com.topdon.pseudo.bean.CustomPseudoBean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import kotlin.math.abs



class IRThermalLiteActivity : BaseIRActivity(), ITsTempListener, ILiteListener {
    companion object {

        const val HANDLE_INIT_FAIL = 1001
        const val HANDLE_SHOW_FPS = 1002
        const val HANDLE_SHOW_SUN_PROTECT_FLAG = 1003
        const val HANDLE_SHOW_TOAST = 1004
        const val HIDE_LOADING = 1005
        const val PREVIEW_FAIL = 1006
        const val SHOW_LOADING = 1007
    }

    private lateinit var binding: ActivityIrThermalLiteBinding

    private val spaceChart by lazy { binding.spaceChart }

    private val tvTypeInd by lazy { binding.tvTypeInd }
    private val popTimeText by lazy { binding.popTimeText }
    private val popTimeLay by lazy { binding.popTimeLay }
    private var mPreviewWidth = 256
    private var mPreviewHeight = 192
    private var mPreviewLayoutParams: RelativeLayout.LayoutParams? = null
    private var mOnUSBConnectListener: OnUSBConnectListener? = null

    @Suppress("DEPRECATION")
    private var mProgressDialog: ProgressDialog? = null

    private var pseudoColorMode = SaveSettingUtil.pseudoColorMode

    private var popupWindow: PopupWindow? = null

    private var tempAlarmSetDialog: TempAlarmSetDialog? = null
    private var alarmBean = SaveSettingUtil.alarmBean
    private var temperatureMode: Int = SaveSettingUtil.temperatureMode 

    private var upColor = 0
    private var downColor = 0
    private var editMaxValue = Float.MAX_VALUE
    private var editMinValue = Float.MIN_VALUE
    private var realLeftValue = -1f
    private var realRightValue = -1f
    private val syncimage = SynchronizedBitmap()
    private val cameraWidth = 256
    private val cameraHeight = 384
    private val tempHeight = 192
    private var imageWidth = cameraWidth
    private var imageHeight = cameraHeight - tempHeight

    private var initRotate = 0 
    private var correctRotate = 0 

    @Volatile
    private var temperatureBytes = ByteArray(imageWidth * imageHeight * 2) 
    private var temperaturerun = false
    private var isShowC: Boolean = false
    private var customPseudoBean = CustomPseudoBean.loadFromShared()
    private var isVideo = false
    private var videoRecord: VideoRecordFFmpeg? = null
    var isTouchSeekBar = false
    private val imageRes = LibIRProcess.ImageRes_t() 
    private val dstTempBytes = ByteArray(192 * 256 * 2)

    @Volatile
    private var isTempShowDialog = false

    private var gainSelChar = -2

    private var storageRequestType = 0
    private var autoJob: Job? = null
    var isAutoCamera = false
    var isOpenPreview = false 
    private var flow: Job? = null
    private var ctrlBlock: USBMonitor.UsbControlBlock? = null
    private var cameraAlpha = SaveSettingUtil.twoLightAlpha
    private var isRecordAudio = SaveSettingUtil.isRecordAudio
    private var showCameraSetting = false
    private val cameraItemBeanList by lazy {
        mutableListOf(
            CameraItemBean(
                "延迟",
                CameraItemBean.TYPE_DELAY,
                time = SaveSettingUtil.delayCaptureSecond,
            ),
            CameraItemBean(
                "自动快门",
                CameraItemBean.TYPE_ZDKM,
                isSel = SaveSettingUtil.isAutoShutter,
            ),
            CameraItemBean("手动快门", CameraItemBean.TYPE_SDKM),
            CameraItemBean(
                "声音",
                CameraItemBean.TYPE_AUDIO,
                isSel =
                    SaveSettingUtil.isRecordAudio &&
                            ActivityCompat.checkSelfPermission(
                                this,
                                Manifest.permission.RECORD_AUDIO,
                            )
                            == PackageManager.PERMISSION_GRANTED,
            ),
            CameraItemBean("设置", CameraItemBean.TYPE_SETTING),
        )
    }
    private var cameraItemAdapter: CameraItemAdapter? = null
    private var audioPosition: Int = 0
    private var isAutoShutter: Boolean = SaveSettingUtil.isAutoShutter
    private var isConfigWait = true
    private var configJob: Job? = null
    private var isRotation = false
    private lateinit var orientationEventListener: OrientationEventListener
    private var mOrientation = 0
    private var isReverseRotation = true
    private var isPause = false

    private fun getPermissionList(): MutableList<String> {
        return if (this.applicationInfo.targetSdkVersion >= 34) {
            mutableListOf(
                Permission.WRITE_EXTERNAL_STORAGE,
            )
        } else if (this.applicationInfo.targetSdkVersion == 33) {
            mutableListOf(
                Permission.READ_MEDIA_VIDEO,
                Permission.READ_MEDIA_IMAGES,
                Permission.WRITE_EXTERNAL_STORAGE,
            )
        } else {
            mutableListOf(Permission.READ_EXTERNAL_STORAGE, Permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    override fun initContentView(): Int {
        binding = ActivityIrThermalLiteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        return 0 // Return dummy value since setContentView is already called
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        USBMonitorManager.getInstance().registerMonitor()
        lifecycleScope.launch(Dispatchers.IO) {
            delay(1000)
            if (BaseApplication.instance.tau_data_H == null) {
                BaseApplication.instance.tau_data_H =
                    CommonUtil.getAssetData(mContext, IrConst.TAU_HIGH_GAIN_ASSET_PATH)
            }
            if (BaseApplication.instance.tau_data_L == null) {
                BaseApplication.instance.tau_data_L =
                    CommonUtil.getAssetData(mContext, IrConst.TAU_LOW_GAIN_ASSET_PATH)
            }
        }
    }

    private var hasClickTrendDel = true

    @SuppressLint("SetTextI18n")
    override fun initView() {
        imageRes.width = 256.toChar()
        imageRes.height = 192.toChar()
        isShowC = getTemperature() == 1
        binding.temperatureSeekbar.setIndicatorTextDecimalFormat("0.0")
        initPreviewManager()
        val imageRotate =
            when (saveSetBean.rotateAngle) {
                270 -> RotateDegree.DEGREE_270
                90 -> RotateDegree.DEGREE_90
                180 -> RotateDegree.DEGREE_0
                else -> RotateDegree.DEGREE_180
            }
        CameraPreviewManager.getInstance().imageRotate = imageRotate
        initCameraSize()
        initUSBMonitorManager()
        DeviceControlManager.getInstance().init()
        binding.titleView.setLeftClickListener {
            if (binding.timeDownView.isRunning) {
                return@setLeftClickListener
            }
            setResult(200)
            finish()
        }
        binding.titleView.setRightClickListener {
            val config = ConfigRepository.readConfig(false)
            var text = ""
            for (tmp in IRConfigData.irConfigData(this)) {
                if (config.radiation.toString() == tmp.value) {
                    if (text.isEmpty()) {
                        text =
                            "${resources.getString(com.topdon.module.thermal.ir.R.string.tc_temp_test_materials)} : "
                    }
                    text += "${tmp.name}/"
                }
            }
            if (text.isNotEmpty()) {
                text = text.substring(0, text.length - 1)
            }
            EmissivityTipPopup(this@IRThermalLiteActivity, false)
                .setDataBean(config.environment, config.distance, config.radiation, text)
                .build()
                .showAsDropDown(binding.titleView, 0, 0, Gravity.END)
        }




        binding.cameraPreview.cameraPreViewCloseListener = {
            if (isOpenPreview) {
                popupWindow?.dismiss()
                cameraPreviewConfig(false)
            }
        }
        binding.viewMenuFirst.onTabClickListener = {
            ViewStubUtils.showViewStub(binding.viewStubCamera, false, null)
            popupWindow?.dismiss()
            binding.temperatureView.isEnabled = it.selectPosition == 1
            binding.thermalRecyclerNight.selectPosition(it.selectPosition + (if (it.isObserveMode) 10 else 0))
        }
        binding.temperatureIvLock.setOnClickListener {
            if (binding.temperatureIvLock.visibility != View.VISIBLE) {
                return@setOnClickListener
            }
            if (binding.temperatureIvLock.contentDescription == "lock") {
                updateTemperatureSeekBar(true) 
            } else {
                setDefLimit()
                updateTemperatureSeekBar(false) 
            }
        }

        binding.thermalRecyclerNight.isVideoMode = SaveSettingUtil.isVideoMode 
        binding.thermalRecyclerNight.fenceSelectType = FenceType.FULL 
        binding.thermalRecyclerNight.isUnitF = SharedManager.getTemperature() == 0 
        binding.thermalRecyclerNight.setTempLevel(temperatureMode) 
        binding.thermalRecyclerNight.onCameraClickListener = {
            setCamera(it)
        }
        binding.thermalRecyclerNight.onFenceListener = { fenceType, isSelected ->
            setTemp(fenceType, isSelected)
        }
        binding.thermalRecyclerNight.onColorListener = { _, it, _ ->
            if (customPseudoBean.isUseCustomPseudo) {
                TipDialog.Builder(this)
                    .setTitleMessage(getString(com.topdon.module.thermal.ir.R.string.app_tip))
                    .setMessage(com.topdon.module.thermal.ir.R.string.tip_change_pseudo_mode)
                    .setPositiveListener(com.topdon.module.thermal.ir.R.string.app_yes) {
                        customPseudoBean.isUseCustomPseudo = false
                        customPseudoBean.saveToShared()
                        setPColor(it)
                        setDefLimit()
                        updateImageAndSeekbarColorList(customPseudoBean)
                        binding.thermalRecyclerNight.setPseudoColor(pseudoColorMode)
                    }.setCancelListener(com.topdon.module.thermal.ir.R.string.app_no) {
                    }
                    .create().show()
            } else {
                setPColor(it)
            }
        }
        binding.thermalRecyclerNight.onSettingListener = { type, isSelected ->
            setSetting(type, isSelected)
        }
        binding.thermalRecyclerNight.onTempLevelListener = {
            temperatureMode = it
            setConfigForIr(IrParam.ParamTemperature, temperatureMode)
            if (it == CameraItemBean.TYPE_TMP_H && SharedManager.isTipHighTemp) {

                val message =
                    SpanBuilder(getString(com.topdon.module.thermal.ir.R.string.tc_high_temp_test_tips1))
                        .appendDrawable(
                            this@IRThermalLiteActivity,
                            com.topdon.module.thermal.ir.R.drawable.svg_title_temp,
                            SizeUtils.sp2px(24f)
                        )
                        .append(getString(com.topdon.module.thermal.ir.R.string.tc_high_temp_test_tips2))
                TipShutterDialog.Builder(this)
                    .setTitle(com.topdon.module.thermal.ir.R.string.tc_high_temp_test)
                    .setMessage(message)
                    .setCancelListener { isCheck ->
                        SharedManager.isTipHighTemp = !isCheck
                    }
                    .create().show()
            }
        }
        binding.thermalRecyclerNight.onTwoLightListener = { twoLightType, isSelected ->
            setTwoLight(twoLightType, isSelected)
        }
        updateTemperatureSeekBar(false) 
        binding.temperatureView.setTextSize(saveSetBean.tempTextSize)
        binding.temperatureView.setLinePaintColor(saveSetBean.tempTextColor)
        binding.temperatureView.setiLiteListener(this)
        binding.temperatureView.setOnTrendChangeListener {
            lifecycleScope.launch(Dispatchers.Main) {
                if (binding.clTrendOpen.isVisible) {
                    binding.viewChartTrend.refresh(it)
                }
            }
        }
        binding.temperatureView.setOnTrendAddListener {
            if (hasClickTrendDel) {
                hasClickTrendDel = false
                binding.clTrendOpen.isVisible = true
                binding.llTrendClose.isVisible = false
            }
        }
        binding.temperatureView.setOnTrendRemoveListener {
            binding.viewChartTrend.setToEmpty()
        }
        binding.temperatureView.listener =
            TemperatureView.TempListener { max, min, _ ->
                realLeftValue = UnitTools.showUnitValue(min, isShowC)
                realRightValue = UnitTools.showUnitValue(max, isShowC)
                this@IRThermalLiteActivity.runOnUiThread {
                    if (!customPseudoBean.isUseCustomPseudo) {

                        try {
                            binding.temperatureSeekbar.setRangeAndPro(
                                UnitTools.showUnitValue(editMinValue, isShowC),
                                UnitTools.showUnitValue(editMaxValue, isShowC),
                                realLeftValue,
                                realRightValue,
                            )
                            if (editMinValue != Float.MIN_VALUE && editMaxValue != Float.MAX_VALUE) {
                                CameraPreviewManager.getInstance()?.setLimit(
                                    editMaxValue, editMinValue,
                                    upColor, downColor,
                                ) 
                            }
                        } catch (e: Exception) {
                            Log.e("温度图层更新失败", e.message.toString())
                        }
                        try {
                            binding.tvTempContent.text =
                                "Max:${UnitTools.showC(max, isShowC)}\nMin:${
                                    UnitTools.showC(
                                        min,
                                        isShowC
                                    )
                                }"
                        } catch (e: Exception) {
                            Log.e("温度图层更新失败", e.message.toString())
                        }
                    } else {

                        try {
                            binding.tvTempContent.text =
                                " Max:${UnitTools.showC(max, isShowC)}\nMin:${
                                    UnitTools.showC(
                                        min,
                                        isShowC
                                    )
                                }"
                        } catch (e: Exception) {
                            Log.e("温度图层更新失败", e.message.toString())
                        }
                    }
                    try {
                        if (isVideo) {
                            binding.clSeekBar.requestLayout()
                            binding.clSeekBar.updateBitmap()
                        }
                    } catch (e: Exception) {
                        Log.w("伪彩条更新异常:", "${e.message}")
                    }
                    try {
                        AlarmHelp.getInstance(application).alarmData(max, min, binding.tempBg)
                    } catch (e: Exception) {
                        Log.e("温度图层更新失败", e.message.toString())
                    }
                }
            }
        addTemperatureListener()
        if (SaveSettingUtil.isOpenTwoLight) {
            cameraPreviewConfig(false)
        }
        lifecycleScope.launch {
            delay(1000)
            if (!SharedManager.isHideEmissivityTips) {
                showEmissivityTips()
            }
        }





















        initOrientationEventListener()

        binding.ivTrendClose.setOnClickListener {
            binding.clTrendOpen.isVisible = false
            binding.llTrendClose.isVisible = true
        }
        binding.ivTrendOpen.setOnClickListener {
            binding.clTrendOpen.isVisible = true
            binding.llTrendClose.isVisible = false
        }
    }

    private fun initOrientationEventListener() {
        orientationEventListener =
            object : OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
                override fun onOrientationChanged(orientation: Int) {
                    if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                        return
                    }
                    startOrientation()
                    if (mOrientation == 1) {
                        return
                    }
                    requestedOrientation =
                        if ((orientation in 315..360) || (orientation in 0..45)) {
                            if (isRotation && saveSetBean.rotateAngle != 270) {
                                saveSetBean.rotateAngle = 270
                                updateRotateAngle(saveSetBean.rotateAngle)
                                isRotation = !isRotation
                                isReverseRotation = true
                                binding.cameraPreview?.setRotation(false)
                            }
                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        } else {
                            (
                                    if (orientation in 135..225) {
                                        if (isReverseRotation && saveSetBean.rotateAngle != 90) {
                                            saveSetBean.rotateAngle = 90
                                            updateRotateAngle(saveSetBean.rotateAngle)
                                            isReverseRotation = !isReverseRotation
                                            isRotation = true
                                            binding.cameraPreview?.setRotation(true)
                                        }
                                        ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                                    } else {
                                        isRotation = true
                                        isReverseRotation = true
                                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    }
                                    )
                        }
                }
            }
        orientationEventListener.enable()
    }

    private fun startOrientation() {
        orientationEventListener.enable()
        mOrientation =
            if (Settings.System.getInt(
                    contentResolver,
                    Settings.System.ACCELEROMETER_ROTATION
                ) == 0
            ) {
                1
            } else {
                2
            }
    }

    private fun showEmissivityTips() {
        val config = ConfigRepository.readConfig(false)
        var text = ""
        for (tmp in IRConfigData.irConfigData(this)) {
            if (config.radiation.toString() == tmp.value) {
                if (text.isEmpty()) {
                    text =
                        "${resources.getString(com.topdon.module.thermal.ir.R.string.tc_temp_test_materials)} : "
                }
                text += "${tmp.name}/"
            }
        }
        if (text.isNotEmpty()) {
            text = text.substring(0, text.length - 1)
        }
        val dialog =
            TipEmissivityDialog.Builder(this@IRThermalLiteActivity)
                .setDataBean(config.environment, config.distance, config.radiation, text)
                .create()
        dialog.onDismissListener = {
            SharedManager.isHideEmissivityTips = it
        }
        dialog.show()
    }

    private fun addTemperatureListener() {
        binding.temperatureIvLock.setOnClickListener {
            if (binding.temperatureIvLock.visibility != View.VISIBLE) {
                return@setOnClickListener
            }
            if (binding.temperatureIvLock.contentDescription == "lock") {
                updateTemperatureSeekBar(true) 
            } else {
                setDefLimit()
                updateTemperatureSeekBar(false) 
            }
        }
        binding.temperatureIvInput.setOnClickListener {
            val intent = Intent(this, PseudoSetActivity::class.java)
            intent.putExtra(ExtraKeyConfig.IS_TC007, false)
            pseudoSetResult.launch(intent)
        }
        binding.temperatureSeekbar.setOnRangeChangedListener(
            object : OnRangeChangedListener {
                override fun onRangeChanged(
                    view: RangeSeekBar?,
                    leftValue: Float,
                    rightValue: Float,
                    isFromUser: Boolean,
                    tempMode: Int,
                ) {
                    if (isTouchSeekBar) {
                        editMinValue =
                            if (tempMode == RangeSeekBar.TEMP_MODE_MIN || tempMode == RangeSeekBar.TEMP_MODE_INTERVAL) {
                                UnitTools.showToCValue(leftValue, isShowC)
                            } else {
                                Float.MIN_VALUE
                            }
                        editMaxValue =
                            if (tempMode == RangeSeekBar.TEMP_MODE_MAX || tempMode == RangeSeekBar.TEMP_MODE_INTERVAL) {
                                UnitTools.showToCValue(rightValue, isShowC)
                            } else {
                                Float.MAX_VALUE
                            }
                        CameraPreviewManager.getInstance()?.setLimit(
                            editMaxValue,
                            editMinValue,
                            upColor,
                            downColor,
                        ) 
                    }
                }

                override fun onStartTrackingTouch(
                    view: RangeSeekBar?,
                    isLeft: Boolean,
                ) {
                    isTouchSeekBar = true
                }

                override fun onStopTrackingTouch(
                    view: RangeSeekBar?,
                    isLeft: Boolean,
                ) {
                    isTouchSeekBar = false
                }
            },
        )
        binding.thermalRecyclerNight.onFenceListener = { fenceType, isSelected ->
            setTemp(fenceType, isSelected)
        }
    }

    private val pseudoSetResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val customBean =
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        it.data?.getParcelableExtra(
                            ExtraKeyConfig.CUSTOM_PSEUDO_BEAN,
                            CustomPseudoBean::class.java
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        it.data?.getParcelableExtra(ExtraKeyConfig.CUSTOM_PSEUDO_BEAN)
                    }
                updateImageAndSeekbarColorList(customBean ?: CustomPseudoBean())
                customPseudoBean.saveToShared()
            }
        }

    
    private fun setCustomPseudoColorList(
        colorList: IntArray?,
        places: FloatArray?,
        isUseGray: Boolean,
        customMaxTemp: Float,
        customMinTemp: Float,
    ) {
        CameraPreviewManager.getInstance()
            ?.setColorList(colorList, places, isUseGray, customMaxTemp, customMinTemp)
    }

    
    private fun updateImageAndSeekbarColorList(customPseudoBean: CustomPseudoBean?) {
        customPseudoBean?.let {
            binding.temperatureSeekbar.setColorList(
                customPseudoBean.getColorList()?.reversedArray()
            )
            binding.temperatureSeekbar.setPlaces(customPseudoBean.getPlaceList())
            setCustomPseudoColorList(
                customPseudoBean.getColorList(),
                customPseudoBean.getPlaceList(),
                customPseudoBean.isUseGray,
                it.maxTemp,
                it.minTemp,
            )
            if (it.isUseCustomPseudo) {
                binding.temperatureIvLock.visibility = View.INVISIBLE
                binding.tvTempContent.visibility = View.VISIBLE
                setDefLimit()
                updateTemperatureSeekBar(false) 
                binding.temperatureSeekbar.setRangeAndPro(
                    UnitTools.showUnitValue(it.minTemp),
                    UnitTools.showUnitValue(it.maxTemp),
                    UnitTools.showUnitValue(it.minTemp),
                    UnitTools.showUnitValue(it.maxTemp),
                )
                binding.thermalRecyclerNight.setPseudoColor(-1)
                binding.temperatureIvInput.setImageResource(com.topdon.module.thermal.ir.R.drawable.ir_model)
            } else {
                binding.temperatureIvLock.visibility = View.VISIBLE
                binding.thermalRecyclerNight.setPseudoColor(pseudoColorMode)
                if (this.customPseudoBean.isUseCustomPseudo) {
                    setDefLimit()
                }
                binding.tvTempContent.visibility = View.GONE
                binding.temperatureIvInput.setImageResource(com.topdon.module.thermal.ir.R.drawable.ic_color_edit)
            }
            this.customPseudoBean = it
        }
    }

    private fun setDefLimit() {
        editMaxValue = Float.MAX_VALUE
        editMinValue = Float.MIN_VALUE
        CameraPreviewManager.getInstance()
            ?.setLimit(editMaxValue, editMinValue, upColor, downColor) 
        binding.temperatureSeekbar.setRangeAndPro(
            editMinValue,
            editMaxValue,
            realLeftValue,
            realRightValue
        ) 
    }

    private fun updateTemperatureSeekBar(isEnabled: Boolean) {
        binding.temperatureSeekbar.isEnabled = isEnabled
        binding.temperatureSeekbar.drawIndPath(isEnabled)
        binding.temperatureIvLock.setImageResource(if (isEnabled) R.drawable.svg_pseudo_bar_unlock else R.drawable.svg_pseudo_bar_lock)
        binding.temperatureIvLock.contentDescription = if (isEnabled) "unlock" else "lock"
        if (isEnabled) {
            binding.temperatureSeekbar.tempMode = RangeSeekBar.TEMP_MODE_CLOSE
            binding.temperatureSeekbar.leftSeekBar.indicatorBackgroundColor = 0xffe17606.toInt()
            binding.temperatureSeekbar.rightSeekBar.indicatorBackgroundColor = 0xffe17606.toInt()
            binding.temperatureSeekbar.invalidate()
        } else {
            binding.temperatureSeekbar.leftSeekBar.indicatorBackgroundColor = 0
            binding.temperatureSeekbar.rightSeekBar.indicatorBackgroundColor = 0
            binding.temperatureSeekbar.invalidate()
        }
    }

    private fun setTwoLight(
        twoLightType: TwoLightType,
        isSelected: Boolean,
    ) {
        popupWindow?.dismiss()
        when (twoLightType) {
            TwoLightType.P_IN_P -> { 
                cameraPreviewConfig(true)
            }

            TwoLightType.BLEND_EXTENT -> { 
                if (!isOpenPreview && isSelected) { 
                    cameraPreviewConfig(false)
                }
                if (isSelected) {
                    showBlendExtentPopup()
                }
            }

            else -> { 
            }
        }
    }

    private fun setSetting(
        type: SettingType,
        isSelected: Boolean,
    ) {
        popupWindow?.dismiss()
        when (type) {
            SettingType.PSEUDO_BAR -> { 
                saveSetBean.isOpenPseudoBar = !saveSetBean.isOpenPseudoBar
                binding.clSeekBar.isVisible = saveSetBean.isOpenPseudoBar
                binding.thermalRecyclerNight.setSettingSelected(
                    SettingType.PSEUDO_BAR,
                    saveSetBean.isOpenPseudoBar
                )
            }

            SettingType.CONTRAST -> { 
                if (!isSelected) {
                    showContrastPopup()
                }
            }

            SettingType.DETAIL -> {

            }

            SettingType.ALARM -> { 
                showTempAlarmSetDialog()
            }

            SettingType.ROTATE -> { 
                saveSetBean.rotateAngle =
                    if (saveSetBean.rotateAngle == 0) 270 else (saveSetBean.rotateAngle - 90)
                updateRotateAngle(saveSetBean.rotateAngle)
            }

            SettingType.FONT -> { 
                val colorPickDialog =
                    ColorPickDialog(this, saveSetBean.tempTextColor, saveSetBean.tempTextSize)
                colorPickDialog.onPickListener = { it: Int, textSize: Int ->
                    saveSetBean.tempTextColor = it
                    saveSetBean.tempTextSize = SizeUtils.sp2px(textSize.toFloat())
                    binding.temperatureView.setTextSize(saveSetBean.tempTextSize)
                    binding.temperatureView.setLinePaintColor(saveSetBean.tempTextColor)
                    binding.thermalRecyclerNight.setSettingSelected(
                        SettingType.FONT,
                        !saveSetBean.isTempTextDefault()
                    )
                }
                colorPickDialog.show()
            }

            SettingType.MIRROR -> { 
                saveSetBean.isOpenMirror = !saveSetBean.isOpenMirror
                IRTool.basicMirrorAndFlipStatusSet(saveSetBean.isOpenMirror)
                binding.thermalRecyclerNight.setSettingSelected(
                    SettingType.MIRROR,
                    saveSetBean.isOpenMirror
                )
            }

            SettingType.COMPASS -> { 

            }

            SettingType.WATERMARK -> {

            }
        }
    }

    private fun showBlendExtentPopup() {
        val seekBarPopup = SeekBarPopup(this, true)
        seekBarPopup.isRealTimeTrigger = true
        seekBarPopup.progress = cameraAlpha
        seekBarPopup.onValuePickListener = {
            cameraAlpha = it
            SaveSettingUtil.twoLightAlpha = cameraAlpha
            binding.cameraPreview?.setCameraAlpha(cameraAlpha / 100.0f)
        }
        seekBarPopup.setOnDismissListener {
            binding.thermalRecyclerNight.setTwoLightSelected(TwoLightType.BLEND_EXTENT, false)
        }
        seekBarPopup.show(binding.thermalLay, !saveSetBean.isRotatePortrait())
        popupWindow = seekBarPopup
    }

    private fun getProductName(): String {
        return ProductType.PRODUCT_NAME_TC001LITE
    }

    private fun setConfigForIr(
        type: IrParam,
        data: Any?,
    ) {
        when (type) {
            IrParam.ParamTemperature -> {

                lifecycleScope.launch {
                    if (temperatureMode == CameraItemBean.TYPE_TMP_C) {
                        basicGainGetValue[0] = 1
                    }
                    withContext(Dispatchers.IO) {
                        IRTool.basicGainSet(temperatureMode)
                    }
                    if (temperatureMode == CameraItemBean.TYPE_TMP_ZD &&
                        SaveSettingUtil.temperatureMode != temperatureMode
                    ) {
                        ToastTools.showShort(com.topdon.module.thermal.ir.R.string.auto_open)
                    }
                    SaveSettingUtil.temperatureMode = temperatureMode
                }
            }

            IrParam.ParamPColor -> {

                CameraPreviewManager.getInstance().setPseudocolorMode(data as Int)
            }

            IrParam.ParamAlarm -> {

                CameraPreviewManager.getInstance().alarmBean = alarmBean
                SaveSettingUtil.alarmBean = alarmBean
                AlarmHelp.getInstance(this).updateData(
                    if (alarmBean.isLowOpen) alarmBean.lowTemp else null,
                    if (alarmBean.isHighOpen) alarmBean.highTemp else null,
                    if (alarmBean.isRingtoneOpen) alarmBean.ringtoneType else null,
                )
            }

            else -> {
            }
        }
    }

    private fun cameraPreviewConfig(needShowTip: Boolean) {
        if (!CheckDoubleClick.isFastDoubleClick()) {
            if (isOpenPreview) {

                isOpenPreview = false
                binding.cameraPreview.closeCamera()
                binding.thermalRecyclerNight.setTwoLightSelected(TwoLightType.P_IN_P, false)
                binding.cameraPreview.visibility = View.INVISIBLE
                SaveSettingUtil.isOpenTwoLight = false
            } else {
                checkCameraPermission(needShowTip)
            }
        }
    }

    private fun checkCameraPermission(needShowTip: Boolean) {
        if (!XXPermissions.isGranted(
                this,
                Permission.CAMERA,
            )
        ) {
            if (BaseApplication.instance.isDomestic()) {
                TipDialog.Builder(this)
                    .setMessage(
                        getString(
                            com.topdon.module.thermal.ir.R.string.permission_request_camera_app,
                            CommUtils.getAppName()
                        )
                    )
                    .setCancelListener(com.topdon.module.thermal.ir.R.string.app_cancel)
                    .setPositiveListener(com.topdon.module.thermal.ir.R.string.app_confirm) {
                        initCameraPermission(needShowTip)
                    }
                    .create().show()
            } else {
                initCameraPermission(needShowTip)
            }
        } else {
            initCameraPermission(needShowTip)
        }
    }

    private fun initCameraPermission(needShowTip: Boolean) {
        XXPermissions.with(this@IRThermalLiteActivity)
            .permission(Permission.CAMERA)
            .request(
                object : OnPermissionCallback {
                    override fun onGranted(
                        permissions: MutableList<String>,
                        allGranted: Boolean,
                    ) {
                        try {
                            if (allGranted) {

                                binding.thermalRecyclerNight.setTwoLightSelected(
                                    TwoLightType.P_IN_P,
                                    true
                                )
                                binding.cameraPreview.visibility = View.VISIBLE
                                binding.cameraPreview?.setCameraAlpha(cameraAlpha / 100.0f)
                                binding.cameraPreview.post {
                                    isOpenPreview = true
                                    binding.cameraPreview.openCamera()
                                    SaveSettingUtil.isOpenTwoLight = true
                                }
                                if (needShowTip && SharedManager.isTipPinP) {
                                    val dialog = TipPreviewDialog.newInstance()
                                    dialog.closeEvent = {
                                        SharedManager.isTipPinP = !it
                                    }
                                    dialog.show(supportFragmentManager, "")
                                }
                            } else {
                                binding.thermalRecyclerNight.setTwoLightSelected(
                                    TwoLightType.P_IN_P,
                                    false
                                )
                                ToastUtils.showShort(R.string.scan_ble_tip_authorize)
                            }
                        } catch (e: Exception) {
                            XLog.e("画中画" + e.message)
                        }
                    }

                    override fun onDenied(
                        permissions: MutableList<String>,
                        doNotAskAgain: Boolean,
                    ) {
                        if (doNotAskAgain) {

                            if (BaseApplication.instance.isDomestic()) {
                                ToastUtils.showShort(getString(com.topdon.module.thermal.ir.R.string.app_camera_content))
                                return
                            }
                            TipDialog.Builder(this@IRThermalLiteActivity)
                                .setTitleMessage(getString(com.topdon.module.thermal.ir.R.string.app_tip))
                                .setMessage(getString(com.topdon.module.thermal.ir.R.string.app_camera_content))
                                .setPositiveListener(com.topdon.module.thermal.ir.R.string.app_open) {
                                    AppUtils.launchAppDetailsSettings()
                                }
                                .setCancelListener(com.topdon.module.thermal.ir.R.string.app_cancel) {
                                }
                                .setCanceled(true)
                                .create().show()
                        }
                        binding.thermalRecyclerNight.setTwoLightSelected(TwoLightType.P_IN_P, false)
                    }
                },
            )
    }

    private val mLiteHandler: Handler =
        object : Handler(Looper.myLooper()!!) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                if (msg.what == SHOW_LOADING) {
                    Log.d(TAG, "SHOW_LOADING")
                    showLoadingDialog()
                } else if (msg.what == HIDE_LOADING) {
                    Log.d(TAG, "HIDE_LOADING")
                    hideLoadingDialog()
                    isConfigWait = false
                } else if (msg.what == HANDLE_INIT_FAIL) {
                    Log.d(TAG, "HANDLE_INIT_FAIL")
                    hideLoadingDialog()
                    Toast.makeText(
                        this@IRThermalLiteActivity,
                        "handle init fail !",
                        Toast.LENGTH_LONG
                    ).show()
                } else if (msg.what == HANDLE_SHOW_TOAST) {
                    val message = msg.obj as String
                    Toast.makeText(this@IRThermalLiteActivity, message, Toast.LENGTH_LONG).show()
                } else if (msg.what == PREVIEW_FAIL) {
                    hideLoadingDialog()
                    Toast.makeText(this@IRThermalLiteActivity, "preview fail !", Toast.LENGTH_LONG)
                        .show()
                } else if (msg.what == HANDLE_SHOW_FPS) {
                    val fps = msg.obj as Double
                    binding.fpsText.setText("fps : " + String.format("%.1f", fps))
                } else if (msg.what == HANDLE_SHOW_SUN_PROTECT_FLAG) {
                    Toast.makeText(this@IRThermalLiteActivity, "Sun private", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }

    private fun initPreviewManager() {
        val imageRotate =
            when (saveSetBean.rotateAngle) {
                270 -> RotateDegree.DEGREE_270
                90 -> RotateDegree.DEGREE_90
                180 -> RotateDegree.DEGREE_0
                else -> RotateDegree.DEGREE_180
            }
        CameraPreviewManager.getInstance().imageRotate = imageRotate

        CameraPreviewManager.getInstance().init(binding.cameraView, mLiteHandler)

        CameraPreviewManager.getInstance().setOnTempDataChangeCallback { data ->
            if (data != null) {
                System.arraycopy(data, 0, temperatureBytes, 0, temperatureBytes.size)
            }
            when (saveSetBean.rotateAngle) {
                270 -> {
                    LibIRProcess.rotateLeft90(
                        temperatureBytes,
                        imageRes,
                        CommonParams.IRPROCSRCFMTType.IRPROC_SRC_FMT_Y14,
                        dstTempBytes
                    )
                }

                0 -> {
                    LibIRProcess.rotate180(
                        temperatureBytes,
                        imageRes,
                        CommonParams.IRPROCSRCFMTType.IRPROC_SRC_FMT_Y14,
                        dstTempBytes
                    )
                }

                90 -> {
                    LibIRProcess.rotateRight90(
                        temperatureBytes,
                        imageRes,
                        CommonParams.IRPROCSRCFMTType.IRPROC_SRC_FMT_Y14,
                        dstTempBytes
                    )
                }

                180 -> {
                    System.arraycopy(temperatureBytes, 0, dstTempBytes, 0, dstTempBytes.size)
                }
            }
            binding.temperatureView.setTemperature(dstTempBytes)
        }
        binding.temperatureView.start()
    }

    private fun initCameraSize() {
        if (RotateDegree.DEGREE_0 == CameraPreviewManager.getInstance().imageRotate ||
            RotateDegree.DEGREE_180 == CameraPreviewManager.getInstance().imageRotate
        ) {
            binding.temperatureView.setImageSize(mPreviewWidth, mPreviewHeight, this)
        } else if (RotateDegree.DEGREE_90 == CameraPreviewManager.getInstance().imageRotate ||
            RotateDegree.DEGREE_270 == CameraPreviewManager.getInstance().imageRotate
        ) {
            binding.temperatureView.setImageSize(mPreviewHeight, mPreviewWidth, this)
        }

        val params = binding.thermalLay.layoutParams as ConstraintLayout.LayoutParams
        if (RotateDegree.DEGREE_90 == CameraPreviewManager.getInstance().imageRotate ||
            RotateDegree.DEGREE_270 == CameraPreviewManager.getInstance().imageRotate
        ) {
            params.dimensionRatio = "192:256"
        } else {
            params.dimensionRatio = "256:192"
        }
        runOnUiThread {
            binding.thermalLay.layoutParams = params
        }
        binding.thermalLay.post {
            binding.clSeekBar.requestLayout()
        }
        binding.thermalLay.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (saveSetBean.isRotatePortrait() && binding.thermalLay.measuredHeight > binding.thermalLay.measuredWidth) {
                        val childLayoutParams = binding.temperatureView.layoutParams
                        childLayoutParams.width = binding.thermalLay.measuredWidth
                        childLayoutParams.height = binding.thermalLay.measuredHeight
                        binding.temperatureView.layoutParams = childLayoutParams
                        mPreviewLayoutParams = RelativeLayout.LayoutParams(
                            binding.thermalLay.measuredWidth,
                            binding.thermalLay.measuredHeight
                        )
                        binding.cameraView.setLayoutParams(mPreviewLayoutParams)
                        binding.cameraPreview.layoutParams = mPreviewLayoutParams
                        binding.thermalLay.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    } else if (!saveSetBean.isRotatePortrait() && binding.thermalLay.measuredHeight < binding.thermalLay.measuredWidth) {
                        val childLayoutParams = binding.temperatureView.layoutParams
                        childLayoutParams.width = binding.thermalLay.measuredWidth
                        childLayoutParams.height = binding.thermalLay.measuredHeight
                        binding.temperatureView.layoutParams = childLayoutParams
                        mPreviewLayoutParams = RelativeLayout.LayoutParams(
                            binding.thermalLay.measuredWidth,
                            binding.thermalLay.measuredHeight
                        )
                        binding.cameraView.setLayoutParams(mPreviewLayoutParams)
                        binding.cameraPreview.layoutParams = mPreviewLayoutParams
                        binding.thermalLay.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                }
            },
        )

        binding.temperatureView.setSyncimage(syncimage)
        binding.temperatureView.setTemperature(temperatureBytes)
        binding.temperatureView.setUseIRISP(false)

        binding.clSeekBar.requestLayout()
        binding.clSeekBar.updateBitmap()


        binding.temperatureView.post {
            lifecycleScope.launch {
                if (!temperaturerun) {
                    temperaturerun = true

                    binding.temperatureView?.visibility = View.VISIBLE
                    delay(1000)
                    binding.temperatureView?.temperatureRegionMode = REGION_MODE_CENTER 
                }
            }
        }
    }

    private fun initUSBMonitorManager() {
        USBMonitorManager.getInstance().init()
        mOnUSBConnectListener =
            object : OnUSBConnectListener {
                override fun onAttach(device: UsbDevice?) {
                }

                override fun onGranted(
                    usbDevice: UsbDevice?,
                    granted: Boolean,
                ) {
                }

                override fun onDetach(device: UsbDevice?) {
                    finish()
                }

                override fun onConnect(
                    device: UsbDevice?,
                    ctrlBlock: USBMonitor.UsbControlBlock?,
                    createNew: Boolean,
                ) {
                    this@IRThermalLiteActivity.ctrlBlock = ctrlBlock

                    DeviceControlManager.getInstance().handleStartPreview(ctrlBlock)
                }

                override fun onDisconnect(
                    device: UsbDevice?,
                    ctrlBlock: USBMonitor.UsbControlBlock?,
                ) {


                }

                override fun onCancel(device: UsbDevice?) {
                }

                override fun onCompleteInit() {
                }
            }
        USBMonitorManager.getInstance()
            .addOnUSBConnectListener(IRThermalLiteActivity::class.java.name, mOnUSBConnectListener)
    }

    @Suppress("DEPRECATION")
    private fun showLoadingDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog(this@IRThermalLiteActivity)
            mProgressDialog?.setMessage("Loading...")
            mProgressDialog?.show()
        } else {
            if (!mProgressDialog?.isShowing!!) {
                mProgressDialog?.show()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun hideLoadingDialog() {
        if (mProgressDialog != null) {
            mProgressDialog?.dismiss()
            mProgressDialog = null
        }
    }

    private fun showTempAlarmSetDialog() {
        if (tempAlarmSetDialog == null) {
            tempAlarmSetDialog = TempAlarmSetDialog(this, false)
            tempAlarmSetDialog?.onSaveListener = {
                binding.thermalRecyclerNight.setSettingSelected(
                    SettingType.ALARM,
                    it.isHighOpen || it.isLowOpen
                )
                alarmBean = it
                setConfigForIr(IrParam.ParamAlarm, alarmBean)
            }
        }
        tempAlarmSetDialog?.alarmBean = alarmBean
        tempAlarmSetDialog?.show()
    }

    private fun addLimit() {
    }

    private fun showContrastPopup() {
        binding.thermalRecyclerNight.setSettingSelected(SettingType.CONTRAST, true)

        val seekBarPopup = SeekBarPopup(this)
        seekBarPopup.progress = NumberTools.scale(saveSetBean.contrastValue / 2.56f, 0).toInt()
        seekBarPopup.onValuePickListener = {
            saveSetBean.contrastValue = (it * 2.56f).toInt().coerceAtMost(255)
            IRTool.basicGlobalContrastLevelSet(it)
        }
        seekBarPopup.setOnDismissListener {
            binding.thermalRecyclerNight.setSettingSelected(SettingType.CONTRAST, false)
        }
        seekBarPopup.show(binding.thermalLay, !saveSetBean.isRotatePortrait())
        popupWindow = seekBarPopup
    }

    private fun updateRotateAngle(rotateAngle: Int) {

        CameraPreviewManager.getInstance().setLimit(
            editMaxValue,
            editMinValue,
            upColor,
            downColor,
        ) 
        lifecycleScope.launch {
            binding.temperatureView.clear()
            binding.temperatureView.temperatureRegionMode = REGION_MODE_CENTER
            hasClickTrendDel = true
            spaceChart.isVisible = false
            binding.clTrendOpen.isVisible = false
            binding.llTrendClose.isVisible = false
            val imageRotate =
                when (rotateAngle) {
                    270 -> RotateDegree.DEGREE_270
                    90 -> RotateDegree.DEGREE_90
                    180 -> RotateDegree.DEGREE_0
                    else -> RotateDegree.DEGREE_180
                }
            CameraPreviewManager.getInstance().imageRotate = imageRotate
            initCameraSize()
            delay(100)
            binding.thermalRecyclerNight.fenceSelectType = FenceType.FULL 
            binding.thermalRecyclerNight.setSettingRotate(rotateAngle)
        }
    }

    private fun setPColor(code: Int) {
        pseudoColorMode = code
        setConfigForIr(IrParam.ParamPColor, code)
        binding.temperatureSeekbar.setPseudocode(pseudoColorMode)
        
        SaveSettingUtil.pseudoColorMode = pseudoColorMode
        binding.thermalRecyclerNight.setPseudoColor(code)
    }

    private fun setTemp(
        fenceType: FenceType,
        isSelected: Boolean,
    ) {
        binding.temperatureView.isEnabled = true
        when (fenceType) {
            FenceType.POINT -> { 
                binding.temperatureView.visibility = View.VISIBLE
                binding.temperatureView.temperatureRegionMode = REGION_MODE_POINT
            }

            FenceType.LINE -> { 
                binding.temperatureView.visibility = View.VISIBLE
                binding.temperatureView.temperatureRegionMode = REGION_MODE_LINE
            }

            FenceType.RECT -> { 
                binding.temperatureView.visibility = View.VISIBLE
                binding.temperatureView.temperatureRegionMode = REGION_MODE_RECTANGLE
            }

            FenceType.FULL -> { 
                binding.temperatureView.visibility = View.VISIBLE
                binding.temperatureView.isShowFull = isSelected
            }

            FenceType.TREND -> { 
                if (SharedManager.isNeedShowTrendTips) {
                    NotTipsSelectDialog(this)
                        .setTipsResId(R.string.thermal_trend_tips)
                        .setOnConfirmListener {
                            SharedManager.isNeedShowTrendTips = !it
                        }
                        .show()
                }
                binding.temperatureView.visibility = View.VISIBLE
                binding.temperatureView.temperatureRegionMode = REGION_NODE_TREND
                if (!spaceChart.isVisible) { 
                    spaceChart.isVisible = true
                    binding.clTrendOpen.isVisible = false
                    binding.llTrendClose.isVisible = true
                }
            }

            FenceType.DEL -> { 
                hasClickTrendDel = true
                binding.temperatureView.clear()
                binding.temperatureView.visibility = View.INVISIBLE
                binding.temperatureView.temperatureRegionMode = REGION_MODE_CLEAN
                spaceChart.isVisible = false
                binding.clTrendOpen.isVisible = false
                binding.llTrendClose.isVisible = false
            }
        }
    }

    private var cameraDelaySecond: Int = SaveSettingUtil.delayCaptureSecond

    
    private fun setCamera(actionCode: Int) {
        when (actionCode) {
            0 -> { 
                if (isVideo) {
                    centerCamera()
                    return
                }
                if (cameraDelaySecond > 0) {
                    autoJob?.cancel()
                }
                if (binding.timeDownView.isRunning) {
                    binding.timeDownView.cancel()
                    updateDelayView()
                } else {
                    if (binding.timeDownView.downTimeWatcher == null) {
                        binding.timeDownView.setOnTimeDownListener(
                            object : TimeDownView.DownTimeWatcher {
                                override fun onTime(num: Int) {
                                    updateDelayView()
                                }

                                override fun onLastTime(num: Int) {
                                }

                                override fun onLastTimeFinish(num: Int) {
                                    if (binding.thermalRecyclerNight.isVideoMode) {
                                        updateVideoDelayView()
                                    } else {
                                        updateDelayView()
                                    }
                                    centerCamera()
                                }
                            },
                        )
                    }
                    binding.timeDownView.downSecond(cameraDelaySecond)
                }
            }

            1 -> { 
                lifecycleScope.launch {
                    if (isVideo) {
                        videoRecord?.stopRecord()
                        isVideo = false
                        videoTimeClose()
                        delay(500)
                    }
                    NavigationManager.getInstance()
                        .build(RouterConfig.IR_GALLERY_HOME)
                        .withInt(ExtraKeyConfig.DIR_TYPE, GalleryRepository.DirType.LINE.ordinal)
                        .navigation(this@IRThermalLiteActivity)
                }
            }

            2 -> { 
                settingCamera()
            }

            3 -> { 
                autoJob?.cancel()
                SaveSettingUtil.isVideoMode = false
            }

            4 -> { 
                autoJob?.cancel()
                SaveSettingUtil.isVideoMode = true
            }
        }
    }

    
    private fun settingCamera() {
        showCameraSetting = !showCameraSetting
        if (showCameraSetting) {
            ViewStubUtils.showViewStub(binding.viewStubCamera, true, callback = { view: View? ->
                view?.let {
                    val recyclerView =
                        it.findViewById<RecyclerView>(com.topdon.module.thermal.ir.R.id.recycler_view)
                    if (ScreenUtil.isPortrait(this)) {
                        recyclerView.layoutManager =
                            GridLayoutManager(this, cameraItemBeanList.size)
                    } else {
                        recyclerView.layoutManager =
                            GridLayoutManager(
                                this, cameraItemBeanList.size, GridLayoutManager.VERTICAL, false,
                            )
                    }
                    cameraItemAdapter = CameraItemAdapter(cameraItemBeanList)
                    cameraItemAdapter?.listener = listener@{ position, _ ->
                        when (cameraItemAdapter!!.data[position].type) {
                            CameraItemBean.TYPE_SETTING -> {
                                NavigationManager.getInstance()
                                    .build(RouterConfig.IR_CAMERA_SETTING)
                                    .navigation(this)
                                return@listener
                            }

                            CameraItemBean.TYPE_DELAY -> {
                                if (binding.timeDownView.isRunning) {
                                    return@listener
                                }
                                cameraItemAdapter!!.data[position].changeDelayType()
                                cameraItemAdapter!!.notifyItemChanged(position)
                                when (cameraItemAdapter!!.data[position].time) {
                                    CameraItemBean.DELAY_TIME_0 -> {
                                        ToastUtils.showShort(com.topdon.module.thermal.ir.R.string.off_photography)
                                    }

                                    CameraItemBean.DELAY_TIME_3 -> {
                                        ToastUtils.showShort(com.topdon.module.thermal.ir.R.string.seconds_dalay_3)
                                    }

                                    CameraItemBean.DELAY_TIME_6 -> {
                                        ToastUtils.showShort(com.topdon.module.thermal.ir.R.string.seconds_dalay_6)
                                    }
                                }
                                cameraDelaySecond = cameraItemAdapter!!.data[position].time
                                SaveSettingUtil.delayCaptureSecond = cameraDelaySecond
                            }

                            CameraItemBean.TYPE_AUDIO -> {
                                if (!cameraItemAdapter!!.data[position].isSel) {
                                    storageRequestType = 1
                                    audioPosition = position
                                    checkStoragePermission()
                                } else {
                                    isRecordAudio = false
                                    SaveSettingUtil.isRecordAudio = isRecordAudio
                                    videoRecord?.updateAudioState(false)
                                    cameraItemAdapter!!.data[position].isSel =
                                        !cameraItemAdapter!!.data[position].isSel
                                    cameraItemAdapter!!.notifyItemChanged(position)
                                }
                                return@listener
                            }

                            CameraItemBean.TYPE_SDKM -> {
                                lifecycleScope.launch {
                                    cameraItemAdapter!!.data[position].isSel = true
                                    cameraItemAdapter!!.notifyItemChanged(position)
                                    delay(500)
                                    cameraItemAdapter!!.data[position].isSel = false
                                    cameraItemAdapter!!.notifyItemChanged(position)
                                }

                                IRTool.setOneShutter()
                                ToastUtils.showShort(com.topdon.module.thermal.ir.R.string.app_Manual_Shutter)
                                return@listener
                            }

                            CameraItemBean.TYPE_ZDKM -> {

                                isAutoShutter = !isAutoShutter
                                SaveSettingUtil.isAutoShutter = isAutoShutter
                                cameraItemAdapter!!.data[position].isSel =
                                    !cameraItemAdapter!!.data[position].isSel
                                cameraItemAdapter!!.notifyItemChanged(position)
                                if (SharedManager.isTipShutter && !isAutoShutter) {
                                    val dialog =
                                        TipShutterDialog.Builder(this)
                                            .setMessage(com.topdon.module.thermal.ir.R.string.shutter_tips)
                                            .setCancelListener { isCheck ->
                                                SharedManager.isTipShutter = !isCheck
                                            }
                                            .create()
                                    dialog.show()
                                }
                                IRTool.setAutoShutter(isAutoShutter)
                                return@listener
                            }
                        }
                        cameraItemAdapter!!.data[position].isSel =
                            !cameraItemAdapter!!.data[position].isSel
                        cameraItemAdapter!!.notifyItemChanged(position)
                    }
                    recyclerView.adapter = cameraItemAdapter
                }
            })
        } else {
            ViewStubUtils.showViewStub(binding.viewStubCamera, false, null)
        }
    }

    fun updateVideoDelayView() {
        try {
            if (binding.timeDownView.isRunning) {
                lifecycleScope.launch(Dispatchers.Main) {
                    binding.thermalRecyclerNight.setToRecord(true)
                }
            }
        } catch (e: Exception) {
            Log.e("线程", e.message.toString())
        }
    }

    private fun checkStoragePermission() {
        if (!XXPermissions.isGranted(this, getPermissionList())) {
            if (BaseApplication.instance.isDomestic()) {
                TipDialog.Builder(this)
                    .setMessage(
                        getString(
                            R.string.permission_request_storage_app,
                            CommUtils.getAppName()
                        )
                    )
                    .setCancelListener(R.string.app_cancel)
                    .setPositiveListener(R.string.app_confirm) {
                        if (storageRequestType == 0) {
                            initStoragePermission()
                        } else {
                            initAudioPermission()
                        }
                    }
                    .create().show()
            } else {
                if (storageRequestType == 0) {
                    initStoragePermission()
                } else {
                    initAudioPermission()
                }
            }
        } else {
            if (storageRequestType == 0) {
                initStoragePermission()
            } else {
                initAudioPermission()
            }
        }
    }

    private fun initAudioPermission() {
        XXPermissions.with(this@IRThermalLiteActivity)
            .permission(
                Permission.RECORD_AUDIO,
            )
            .request(
                object : OnPermissionCallback {
                    override fun onGranted(
                        permissions: MutableList<String>,
                        allGranted: Boolean,
                    ) {
                        try {
                            if (allGranted) {

                                isRecordAudio = true
                                SaveSettingUtil.isRecordAudio = isRecordAudio
                                videoRecord?.updateAudioState(true)
                                cameraItemAdapter?.data?.get(audioPosition)?.isSel =
                                    !(cameraItemAdapter?.data?.get(audioPosition)?.isSel ?: false)
                                cameraItemAdapter?.notifyItemChanged(audioPosition)
                            } else {
                                ToastUtils.showShort(com.topdon.module.thermal.ir.R.string.scan_ble_tip_authorize)
                            }
                        } catch (e: Exception) {
                            Log.e("录音启动失败", "" + e.message)
                        }
                    }

                    override fun onDenied(
                        permissions: MutableList<String>,
                        doNotAskAgain: Boolean,
                    ) {
                        if (doNotAskAgain) {

                            if (BaseApplication.instance.isDomestic()) {
                                ToastUtils.showShort(getString(com.topdon.module.thermal.ir.R.string.app_microphone_content))
                                return
                            }
                            TipDialog.Builder(this@IRThermalLiteActivity)
                                .setTitleMessage(getString(com.topdon.module.thermal.ir.R.string.app_tip))
                                .setMessage(getString(com.topdon.module.thermal.ir.R.string.app_microphone_content))
                                .setPositiveListener(com.topdon.module.thermal.ir.R.string.app_open) {
                                    AppUtils.launchAppDetailsSettings()
                                }
                                .setCancelListener(com.topdon.module.thermal.ir.R.string.app_cancel) {
                                }
                                .setCanceled(true)
                                .create().show()
                        }
                    }
                },
            )
    }

    fun updateDelayView() {
        try {
            if (binding.timeDownView.isRunning) {
                lifecycleScope.launch(Dispatchers.Main) {
                    binding.thermalRecyclerNight.setToRecord(true)
                }
            } else {
                lifecycleScope.launch(Dispatchers.Main) {
                    binding.thermalRecyclerNight.refreshImg()
                }
            }
        } catch (e: Exception) {
            Log.e("线程", e.message.toString())
        }
    }

    private fun initStoragePermission() {
        XXPermissions.with(this)
            .permission(
                getPermissionList(),
            )
            .request(
                object : OnPermissionCallback {
                    override fun onGranted(
                        permissions: MutableList<String>,
                        allGranted: Boolean,
                    ) {
                        if (allGranted) {
                            if (!binding.thermalRecyclerNight.isVideoMode) {
                                val setting = SharedManager.continuousBean
                                if (setting.isOpen) {
                                    if (!isAutoCamera) {

                                        autoJob =
                                            countDownCoroutines(
                                                setting.count,
                                                setting.continuaTime,
                                                this@IRThermalLiteActivity.lifecycleScope,
                                                onTick = {
                                                    camera()
                                                },
                                                onStart = {
                                                    tvTypeInd?.visibility = VISIBLE
                                                    isAutoCamera = true
                                                },
                                                onFinish = {
                                                    tvTypeInd?.visibility = GONE
                                                    isAutoCamera = false
                                                },
                                            )
                                        autoJob?.start()
                                    } else {
                                        isAutoCamera = false
                                        autoJob?.cancel()
                                    }
                                } else {
                                    camera()
                                }
                            } else {

                                video()
                            }
                        } else {
                            ToastUtils.showShort(R.string.scan_ble_tip_authorize)
                        }
                    }

                    override fun onDenied(
                        permissions: MutableList<String>,
                        doNotAskAgain: Boolean,
                    ) {
                        if (doNotAskAgain) {

                            if (BaseApplication.instance.isDomestic()) {
                                ToastUtils.showShort(getString(R.string.app_storage_content))
                                return
                            }
                            TipDialog.Builder(this@IRThermalLiteActivity)
                                .setTitleMessage(getString(R.string.app_tip))
                                .setMessage(R.string.app_storage_content)
                                .setPositiveListener(R.string.app_open) {
                                    AppUtils.launchAppDetailsSettings()
                                }
                                .setCancelListener(R.string.app_cancel) {
                                }
                                .setCanceled(true)
                                .create().show()
                        }
                    }
                },
            )
    }

    private fun countDownCoroutines(
        total: Int,
        timeDelay: Long,
        scope: CoroutineScope,
        onTick: (Int) -> Unit,
        onStart: (() -> Unit)? = null,
        onFinish: (() -> Unit)? = null,
    ): Job {
        return flow {
            for (i in total downTo 1) {
                emit(i)
                delay(timeDelay)
            }
        }.flowOn(Dispatchers.Main)
            .onStart { onStart?.invoke() }
            .onCompletion { onFinish?.invoke() }
            .onEach { onTick.invoke(it) }
            .launchIn(scope)
    }

    private fun camera() {
        lifecycleScope.launch(Dispatchers.Default) {
            launch(Dispatchers.Main) {
                binding.thermalRecyclerNight.setToCamera()
            }
            try {
                synchronized(syncimage.dataLock) {

                    var cameraViewBitmap: Bitmap? = getCameraViewBitmap()

                    if (isOpenPreview) {
                        cameraViewBitmap = BitmapUtils.mergeBitmapByView(
                            cameraViewBitmap,
                            binding.cameraPreview.getBitmap(),
                            binding.cameraPreview
                        )

                        binding.cameraPreview.getBitmap()?.let {
                            ImageUtils.saveImageToApp(it)
                        }
                    }


                    if (binding.temperatureView.temperatureRegionMode != REGION_MODE_CLEAN) {
                        cameraViewBitmap = BitmapUtils.mergeBitmap(
                            cameraViewBitmap,
                            binding.temperatureView.regionAndValueBitmap,
                            0,
                            0
                        )
                    }


                    val isShowPseudoBar = binding.clSeekBar.visibility == VISIBLE
                    if (isShowPseudoBar) {
                        val seekBarBitmap = binding.clSeekBar.drawToBitmap()
                        cameraViewBitmap =
                            BitmapUtils.mergeBitmap(
                                cameraViewBitmap,
                                seekBarBitmap,
                                cameraViewBitmap!!.width - seekBarBitmap.width,
                                (cameraViewBitmap.height - seekBarBitmap.height) / 2,
                            )
                        seekBarBitmap.recycle()
                    }

                    val watermarkBean = SharedManager.watermarkBean
                    if (watermarkBean.isOpen) {
                        cameraViewBitmap =
                            BitmapUtils.drawCenterLable(
                                cameraViewBitmap,
                                watermarkBean.title,
                                watermarkBean.address,
                                if (watermarkBean.isAddTime) TimeTool.getNowTime() else "",
                                if (binding.temperatureSeekbar.isVisible) {
                                    binding.temperatureSeekbar.measuredWidth
                                } else {
                                    0
                                },
                            )
                    }

                    
                    
                    
                    
                    

                    var name = ""
                    cameraViewBitmap?.let {
                        name = ImageUtils.save(bitmap = it)
                    }

                    val basicGainGetValue = IntArray(1)
                    val basicGainGet: IrcmdError =
                        DeviceIrcmdControlManager.getInstance().getIrcmdEngine()
                            ?.basicGainGet(basicGainGetValue)!!
                    val p2RotateAngle =
                        if (saveSetBean.rotateAngle == 90) 270 else abs(saveSetBean.rotateAngle - 180)
                    val capital =
                        FrameStruct.toCode(
                            name = getProductName(),
                            width = if (cameraViewBitmap!!.height < cameraViewBitmap.width) 256 else 192,
                            height = if (cameraViewBitmap.height < cameraViewBitmap.width) 192 else 256,
                            rotate = p2RotateAngle,
                            pseudo = pseudoColorMode,
                            initRotate = initRotate,
                            correctRotate = correctRotate,
                            customPseudoBean = customPseudoBean,
                            isShowPseudoBar = isShowPseudoBar,
                            textColor = saveSetBean.tempTextColor,
                            watermarkBean = watermarkBean,
                            alarmBean = alarmBean,
                            basicGainGetValue[0],
                            textSize = saveSetBean.tempTextSize,
                            config?.environment ?: 0f,
                            config?.distance ?: 0f,
                            config?.radiation ?: 0f,
                            false,
                        )
                    ImageUtils.saveFrame(
                        bs = CameraPreviewManager.getInstance().frameIrAndTempData,
                        capital = capital,
                        name = name,
                    )
                    launch(Dispatchers.Main) {
                        binding.thermalRecyclerNight.refreshImg()
                    }
                    EventBus.getDefault().post(GalleryAddEvent())
                }
            } catch (e: Exception) {
                XLog.e(e.message)
            }
        }
    }

    private fun getCameraViewBitmap(): Bitmap {
        return Bitmap.createScaledBitmap(
            CameraPreviewManager.getInstance().scaledBitmap(true),
            binding.cameraView.width,
            binding.cameraView.height,
            true,
        )
    }

    private fun initVideoRecordFFmpeg() {
        videoRecord =
            VideoRecordFFmpeg(
                binding.cameraView,
                binding.cameraPreview,
                binding.temperatureView,
                true,
                binding.clSeekBar,
                binding.tempBg,
                null, null,
                carView = null, 
            )
    }

    private fun video() {
        if (!isVideo) {

            initVideoRecordFFmpeg()
            if (!videoRecord!!.canStartVideoRecord(null)) {
                return
            }
            videoRecord?.stopVideoRecordListener = { isShowVideoRecordTips ->
                this@IRThermalLiteActivity.runOnUiThread {
                    if (isShowVideoRecordTips) {
                        try {
                            val dialog =
                                TipDialog.Builder(this@IRThermalLiteActivity)
                                    .setMessage(com.topdon.module.thermal.ir.R.string.tip_video_record)
                                    .create()
                            dialog.show()
                        } catch (_: Exception) {
                        }
                    }
                    videoRecord?.stopRecord()
                    isVideo = false
                    videoTimeClose()
                    lifecycleScope.launch(Dispatchers.Main) {
                        delay(500)
                        binding.thermalRecyclerNight.refreshImg()
                    }
                }
            }
            binding.clSeekBar.updateBitmap()
            videoRecord?.updateAudioState(isRecordAudio)
            videoRecord?.startRecord()
            isVideo = true
            lifecycleScope.launch(Dispatchers.Main) {
                binding.thermalRecyclerNight.setToRecord(false)
            }
            videoTimeShow()
        } else {
            stopIfVideoing()
        }
    }

    private fun stopIfVideoing() {
        if (isVideo) {
            isVideo = false
            videoRecord?.stopRecord()
            videoTimeClose()
            lifecycleScope.launch(Dispatchers.Main) {
                delay(500)
                binding.thermalRecyclerNight.refreshImg()
                EventBus.getDefault().post(GalleryAddEvent())
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun centerCamera() {
        storageRequestType = 0
        checkStoragePermission()
    }

    override fun initData() {
        initDataIR()
    }

    private fun initDataIR() {
        imageWidth = cameraHeight - tempHeight
        imageHeight = cameraWidth
        CameraPreviewManager.getInstance().alarmBean = alarmBean
        AlarmHelp.getInstance(this).updateData(alarmBean)
        CameraPreviewManager.getInstance().setPseudocolorMode(pseudoColorMode)
        binding.temperatureSeekbar?.setPseudocode(pseudoColorMode)

        binding.clSeekBar.isVisible = saveSetBean.isOpenPseudoBar
        binding.thermalRecyclerNight.setSettingSelected(
            SettingType.PSEUDO_BAR,
            saveSetBean.isOpenPseudoBar
        )

        if (customPseudoBean.isUseCustomPseudo) {
            updateCustomPseudo()
        } else {
            CameraPreviewManager.getInstance().setColorList(null, null, false, 0f, 0f)
            binding.temperatureSeekbar.progressColor
            binding.temperatureIvLock.visibility = View.VISIBLE
            binding.tvTempContent.visibility = View.GONE
            binding.temperatureIvInput.setImageResource(com.topdon.module.thermal.ir.R.drawable.ic_color_edit)
            binding.thermalRecyclerNight.setPseudoColor(pseudoColorMode)
        }
        binding.thermalRecyclerNight.setSettingSelected(
            SettingType.ALARM,
            alarmBean.isHighOpen || alarmBean.isLowOpen
        )
        binding.thermalRecyclerNight.setSettingSelected(
            SettingType.FONT,
            !saveSetBean.isTempTextDefault()
        )
        binding.temperatureView.setTextSize(saveSetBean.tempTextSize)
        binding.temperatureView.setLinePaintColor(saveSetBean.tempTextColor)
        binding.thermalRecyclerNight.setSettingRotate(saveSetBean.rotateAngle)
        binding.thermalRecyclerNight.setTempLevel(temperatureMode)

        configJob =
            lifecycleScope.launch {
                while (isConfigWait && isActive) {
                    delay(200)
                }
                delay(500)
                IRTool.setAutoShutter(false)

                IRTool.basicGlobalContrastLevelSet((saveSetBean.contrastValue / 255f * 100).toInt())

                IRTool.basicMirrorAndFlipStatusSet(saveSetBean.isOpenMirror)
                binding.thermalRecyclerNight.setSettingSelected(
                    SettingType.MIRROR,
                    saveSetBean.isOpenMirror
                )
                CameraPreviewManager.getInstance()?.setLimit(
                    editMaxValue, editMinValue,
                    upColor, downColor,
                )
                delay(2000)

                withContext(Dispatchers.IO) {
                    IRTool.basicGainSet(temperatureMode)
                }

                delay(30 * 1000)
                IRTool.setAutoShutter(isAutoShutter)
                XLog.i("模组配置恢复成功")
            }
    }

    private fun updateCustomPseudo() {
        binding.temperatureSeekbar.setColorList(customPseudoBean.getColorList()?.reversedArray())
        binding.temperatureSeekbar.setPlaces(customPseudoBean.getPlaceList())
        binding.temperatureIvLock.visibility = View.INVISIBLE
        binding.temperatureSeekbar.setRangeAndPro(
            UnitTools.showUnitValue(customPseudoBean.minTemp),
            UnitTools.showUnitValue(customPseudoBean.maxTemp),
            UnitTools.showUnitValue(customPseudoBean.minTemp),
            UnitTools.showUnitValue(customPseudoBean.maxTemp),
        )
        setCustomPseudoColorList(
            customPseudoBean.getColorList(),
            customPseudoBean.getPlaceList(),
            customPseudoBean.isUseGray,
            customPseudoBean.maxTemp,
            customPseudoBean.minTemp,
        )
        binding.tvTempContent.visibility = View.VISIBLE
        binding.thermalRecyclerNight.setPseudoColor(-1)
        binding.temperatureIvInput.setImageResource(com.topdon.module.thermal.ir.R.drawable.ir_model)
    }

    private fun videoTimeShow() {
        flow =
            lifecycleScope.launch {
                val time = 60 * 60 * 4
                flow {
                    repeat(time) {
                        emit(it)
                        delay(1000)
                    }
                }.collect {
                    launch(Dispatchers.Main) {
                        popTimeText.text = TimeTool.showVideoTime(it * 1000L)
                    }
                    if (it == time - 1) {

                        video()
                    }
                }
            }
        popTimeLay.visibility = View.VISIBLE
    }

    private fun videoTimeClose() {
        flow?.cancel()
        flow = null
        popTimeLay.visibility = View.GONE
    }

    override fun onStart() {
        super.onStart()
        initPreviewManager()






        binding.thermalRecyclerNight.updateCameraModel()
    }

    override fun onResume() {
        super.onResume()
        AlarmHelp.getInstance(this).onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (isPause) {
            DeviceControlManager.getInstance().handleResumeDualPreview()
            isPause = false
        }
        binding.thermalRecyclerNight.refreshImg()
        config = ConfigRepository.readConfig(false)
        orientationEventListener.enable()
        setCarDetectPrompt()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        startOrientation()
    }

    override fun onPause() {
        super.onPause()
        orientationEventListener.disable()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        AlarmHelp.getInstance(this).pause()
        isAutoCamera = false
        autoJob?.cancel()
        binding.timeDownView?.cancel()
        isPause = true
        DeviceControlManager.getInstance().handlePauseDualPreview()
    }

    override fun onStop() {
        super.onStop()
        configJob?.cancel()
        binding.temperatureView?.stop()
        binding.timeDownView?.cancel()
        try {
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
                    binding.thermalRecyclerNight.refreshImg()
                }
            }
        } catch (e: Exception) {
            XLog.e("$TAG:onStop-${e.message}")
        }
    }

    override fun onDestroy() {
        TempCompensation.getInstance().stopTempCompensation(false)
        DeviceControlManager.getInstance().handleStopPreview()
        USBMonitorManager.getInstance().unregisterMonitor()
        super.onDestroy()
        AlarmHelp.getInstance(application).onDestroy(SaveSettingUtil.isSaveSetting)
        try {
            if (mOnUSBConnectListener != null) {
                USBMonitorManager.getInstance()
                    .removeOnUSBConnectListener(IRThermalLiteActivity::class.java.name)
                mOnUSBConnectListener = null
            }
            USBMonitorManager.getInstance().destroyMonitor()
            DeviceControlManager.getInstance().release()
            CameraPreviewManager.getInstance().releaseSource()
        } catch (e: Exception) {
            XLog.e("$TAG:lite销毁异常-${e.message}")
        }
        SystemClock.sleep(100)
    }

    private fun setCarDetectPrompt() {
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
    }

    var config: DataBean? = null
    val basicGainGetValue = IntArray(1)
    var basicGainGetTime = 0L

    override fun tempCorrectByTs(temp: Float?): Float {
        if (isPause) {
            return temp!!
        }
        var tempNew = temp
        try {
            if (config == null) {
                config = ConfigRepository.readConfig(false)
            }
            if (isConfigWait) {
                return temp!!
            }
            val defModel = DataBean()
            if (config!!.radiation == defModel.radiation &&
                defModel.environment == config!!.environment &&
                defModel.distance == config!!.distance
            ) {
                return temp!!
            }


            if (System.currentTimeMillis() - basicGainGetTime > 5000L) {
                try {
                    val basicGainGet: IrcmdError? =
                        DeviceIrcmdControlManager.getInstance().getIrcmdEngine()
                            ?.basicGainGet(basicGainGetValue)
                } catch (e: Exception) {
                    XLog.e("增益获取失败")
                }
                basicGainGetTime = System.currentTimeMillis()
            }
            val params_array =
                floatArrayOf(
                    temp!!,
                    config!!.radiation,
                    config!!.environment,
                    config!!.environment,
                    config!!.distance,
                    0.8f,
                )
            if (BaseApplication.instance.tau_data_H == null || BaseApplication.instance.tau_data_L == null) return temp
            tempNew =
                LibIRTempAC020.temperatureCorrection(
                    params_array[0],
                    BaseApplication.instance.tau_data_H,
                    BaseApplication.instance.tau_data_L,
                    params_array[1],
                    params_array[2],
                    params_array[3],
                    params_array[4],
                    params_array[5],
                    if (basicGainGetValue[0] == 0) GainStatus.LOW_GAIN else GainStatus.HIGH_GAIN,
                )
            Log.i(
                TAG,
                "temp correct, oldTemp = " + params_array[0] + " newtemp = " + tempNew +
                        " ems = " + params_array[1] + " ta = " + params_array[2] + " " +
                        "distance = " + params_array[4] + " hum = " + params_array[5] + " basicGain = " + basicGainGetValue[0],
            )
        } catch (e: Exception) {
            XLog.e("$TAG--温度修正异常：${e.message}")
        } finally {
            return tempNew ?: 0f
        }
    }

    override fun getDeltaNucAndVTemp(): Float {
        TempCompensation.getInstance().getDeltaNucAndVTemp()
        return 0f
    }

    override fun compensateTemp(temp: Float): Float {
        return TempCompensation.getInstance().compensateTemp(temp)
    }
}
