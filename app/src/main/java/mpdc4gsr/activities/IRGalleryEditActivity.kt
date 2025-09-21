package mpdc4gsr.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Parcelable
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.drawToBitmap
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.SizeUtils
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivityIrGalleryEditBinding
import com.elvishew.xlog.XLog
import com.energy.iruvc.ircmd.IRCMDType
import com.energy.iruvc.ircmd.IRUtils
import com.energy.iruvc.utils.CommonParams
import com.example.thermal_lite.IrConst
import com.example.thermal_lite.util.CommonUtil
import com.example.thermal_lite.util.IRTool
import com.infisense.usbir.utils.OpencvTools
import com.infisense.usbir.utils.PseudocodeUtils.changePseudocodeModeByOld
import com.infisense.usbir.view.ITsTempListener
import com.mpdc4gsr.lib.core.BaseApplication
import com.mpdc4gsr.lib.core.activity.PseudoSetActivity
import com.mpdc4gsr.lib.core.bean.CustomPseudoBean
import com.mpdc4gsr.lib.core.bean.event.ReportCreateEvent
import com.mpdc4gsr.lib.core.comm.dialog.ColorPickDialog
import com.mpdc4gsr.lib.core.comm.dialog.TempAlarmSetDialog
import com.mpdc4gsr.lib.core.common.ProductType.PRODUCT_NAME_TC001LITE
import com.mpdc4gsr.lib.core.common.ProductType.PRODUCT_NAME_TS
import com.mpdc4gsr.lib.core.common.SharedManager
import com.mpdc4gsr.lib.core.config.ExtraKeyConfig
import com.mpdc4gsr.lib.core.config.FileConfig
import com.mpdc4gsr.lib.core.config.RouterConfig
import com.mpdc4gsr.lib.core.dialog.TipDialog
import com.mpdc4gsr.lib.core.dialog.TipWaterMarkDialog
import com.mpdc4gsr.lib.core.ktbase.BaseBindingActivity
import com.mpdc4gsr.lib.core.lms.LMS.mContext
import com.mpdc4gsr.lib.core.menu.constant.FenceType
import com.mpdc4gsr.lib.core.menu.constant.SettingType
import com.mpdc4gsr.lib.core.navigation.NavigationManager
import com.mpdc4gsr.lib.core.tools.ScreenTool
import com.mpdc4gsr.lib.core.tools.TimeTool
import com.mpdc4gsr.lib.core.tools.ToastTools
import com.mpdc4gsr.lib.core.tools.UnitTools
import com.mpdc4gsr.lib.core.tools.UnitTools.showToCValue
import com.mpdc4gsr.lib.core.tools.UnitTools.showUnitValue
import com.mpdc4gsr.lib.core.utils.BitmapUtils
import com.mpdc4gsr.lib.core.utils.Constants.IS_REPORT_FIRST
import com.mpdc4gsr.lib.core.utils.ImageUtils
import com.mpdc4gsr.lib.core.utils.ScreenUtil
import com.mpdc4gsr.lib.ui.widget.seekbar.OnRangeChangedListener
import com.mpdc4gsr.lib.ui.widget.seekbar.RangeSeekBar
import com.mpdc4gsr.module.thermal.ir.event.GalleryAddEvent
import com.mpdc4gsr.module.thermal.ir.event.ImageGalleryEvent
import com.mpdc4gsr.module.thermal.ir.frame.FrameStruct
import com.mpdc4gsr.module.thermal.ir.frame.FrameTool
import com.mpdc4gsr.module.thermal.ir.frame.ImageParams
import com.mpdc4gsr.module.thermal.ir.report.bean.ImageTempBean
import com.mpdc4gsr.module.thermal.ir.view.TemperatureBaseView.Mode
import com.mpdc4gsr.module.thermal.ir.viewmodel.IRGalleryEditViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.Locale
import com.example.thermal_lite.R as ThermalLiteR
import com.mpdc4gsr.module.thermal.ir.R as ThermalIrR

class IRGalleryEditActivity : BaseBindingActivity<ActivityIrGalleryEditBinding>(),
    View.OnClickListener, ITsTempListener {
    private val TAG = "IRGalleryEditActivity"

    private var isShowC: Boolean = false

    private var isTC007 = false

    private val imageWidth = 256
    private val imageHeight = 192
    private val viewModel: IRGalleryEditViewModel by viewModels()
    private var filePath = ""

    private var mFrame = ByteArray(192 * 256 * 4)
    private val frameTool by lazy { FrameTool() }

    private var pseudocodeMode = 3
    private var leftValue = 0f
    private var rightValue = 10000f
    private var max = 10000f
    private var min = 0f
    private var rotate = ImageParams.ROTATE_270
    private var struct: FrameStruct = FrameStruct()
    private var ts_data_H: ByteArray? = null
    private var ts_data_L: ByteArray? = null

    private val titleView get() = binding.titleView
    private val editRecyclerSecond get() = binding.editRecyclerSecond
    private val editRecyclerFirst get() = binding.editRecyclerFirst
    private val irImageView get() = binding.irImageView
    private val temperatureView get() = binding.temperatureView
    private val temperatureSeekbar get() = binding.temperatureSeekbar
    private val temperatureIvLock get() = binding.temperatureIvLock
    private val temperatureIvInput get() = binding.temperatureIvInput
    private val tvTempContent get() = binding.tvTempContent
    private val colorBarView get() = binding.colorBarView
    private val llBottom get() = binding.llBottom

    override fun initContentLayoutId() = R.layout.activity_ir_gallery_edit

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initData()
    }

    private fun initView() {
        initIntent()
        initUI()
        initListener()
        initRecycler()
        initObserve()
    }

    private fun initIntent() {
        lifecycleScope.launch {
            ts_data_H = CommonUtil.getAssetData(this@IRGalleryEditActivity, "ts/TS001_H.bin")
            ts_data_L = CommonUtil.getAssetData(this@IRGalleryEditActivity, "ts/TS001_L.bin")

            if (BaseApplication.instance.tau_data_H == null) {
                BaseApplication.instance.tau_data_H =
                    CommonUtil.getAssetData(mContext, IrConst.TAU_HIGH_GAIN_ASSET_PATH)
            }
            if (BaseApplication.instance.tau_data_L == null) {
                BaseApplication.instance.tau_data_L =
                    CommonUtil.getAssetData(mContext, IrConst.TAU_LOW_GAIN_ASSET_PATH)
            }
        }
        if (intent.hasExtra(ExtraKeyConfig.FILE_ABSOLUTE_PATH)) {
            filePath = intent.getStringExtra(ExtraKeyConfig.FILE_ABSOLUTE_PATH)!!
        }
        isReportPick = intent.getBooleanExtra(ExtraKeyConfig.IS_PICK_REPORT_IMG, false)
        isTC007 = intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false)

        editRecyclerSecond.fenceSelectType = FenceType.DEL
        temperatureView.isShowName = isReportPick
        temperatureView.mode = Mode.CLEAR
        temperatureView.setITsTempListener(this)
        if (isTC007) {
            temperatureSeekbar.progressHeight = SizeUtils.dp2px(10f)
        }
    }

    private fun initObserve() {
        viewModel.resultLiveData.observe(this) {

            System.arraycopy(it.frame, 0, mFrame, 0, it.frame.size)
            showImage(it.capital, it.frame)
        }
    }

    private fun initData() {
        viewModel.initData(filePath)

        editRecyclerFirst.isBarSelect = true
        colorBarView.isVisible = true
    }

    private fun initListener() {
        temperatureIvLock.setOnClickListener(this)
        temperatureIvInput.setOnClickListener(this)
    }

    private fun setRotate(rotate: ImageParams) {
        if (rotate == ImageParams.ROTATE_270 || rotate == ImageParams.ROTATE_90) {
            temperatureView.setImageSize(imageHeight, imageWidth)
        } else {
            temperatureView.setImageSize(imageWidth, imageHeight)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showImage(
        capital: ByteArray,
        frame: ByteArray,
    ) {
        lifecycleScope.launch {
            frameTool.read(frame)
            struct = FrameStruct(capital)
            frameTool.initStruct(struct)
            isShowC = SharedManager.getTemperature() == 1
            rotate = frameTool.initRotate()
            pseudocodeMode = struct.pseudo
            setRotate(rotate)
            delay(200)
            updateImage(
                frameTool.getScrPseudoColorScaledBitmap(
                    changePseudocodeModeByOld(pseudocodeMode),
                    rotate = rotate,
                    customPseudoBean = struct.customPseudoBean,
                    maxTemperature = tempCorrect(frameTool.getSrcTemp().maxTemperature),
                    minTemperature = tempCorrect(frameTool.getSrcTemp().minTemperature),
                    isAmplify = struct.isAmplify,
                ),
            )

            val tempResult = frameTool.getSrcTemp()
            if (!struct.customPseudoBean.isUseCustomPseudo) {
                struct.customPseudoBean.maxTemp = tempCorrect(tempResult.maxTemperature)
                struct.customPseudoBean.minTemp = tempCorrect(tempResult.minTemperature)
                editRecyclerSecond.setPseudoColor(pseudocodeMode)
            }




            editRecyclerSecond.setSettingSelected(
                SettingType.ALARM,
                struct.alarmBean.isHighOpen || struct.alarmBean.isLowOpen
            )
            editRecyclerSecond.setSettingSelected(
                SettingType.WATERMARK,
                struct.watermarkBean.isOpen
            )
            editRecyclerSecond.setSettingSelected(
                SettingType.FONT,
                struct.textColor != 0xffffffff.toInt() || struct.textSize != SizeUtils.sp2px(14f),
            )
            temperatureView.textColor = struct.textColor
            temperatureView.tempTextSize = struct.textSize
            temperatureView.setData(frameTool.getTempBytes(rotate = rotate))
            updateTemperatureSeekBar(false, ThermalLiteR.drawable.svg_pseudo_bar_lock, "lock")
            temperatureSeekbar.setPseudocode(pseudocodeMode)
            temperatureSeekbar.setOnRangeChangedListener(
                object : OnRangeChangedListener {
                    override fun onRangeChanged(
                        view: RangeSeekBar?,
                        leftValue: Float,
                        rightValue: Float,
                        isFromUser: Boolean,
                        tempMode: Int,
                    ) {
                        if (leftValue < rightValue) {
                            max = rightValue
                            min = leftValue
                        } else {
                            max = leftValue
                            min = rightValue
                        }
                        if (!struct.customPseudoBean.isUseCustomPseudo) {
                            updateImage(
                                frameTool.getScrPseudoColorScaledBitmap(
                                    changePseudocodeModeByOld(pseudocodeMode),
                                    showToCValue(max),
                                    showToCValue(min),
                                    rotate,
                                    struct.customPseudoBean,
                                    maxTemperature = tempCorrect(frameTool.getSrcTemp().maxTemperature),
                                    minTemperature = tempCorrect(frameTool.getSrcTemp().minTemperature),
                                    struct.isAmplify,
                                ),
                            )
                        }
                    }

                    override fun onStartTrackingTouch(
                        view: RangeSeekBar?,
                        isLeft: Boolean,
                    ) {

                    }

                    override fun onStopTrackingTouch(
                        view: RangeSeekBar?,
                        isLeft: Boolean,
                    ) {

                    }
                },
            )
            temperatureSeekbar.setIndicatorTextStringFormat("%.1f")
            if (struct.customPseudoBean.isUseCustomPseudo) {
                tvTempContent.visibility = View.VISIBLE
                tvTempContent.text = "Max:${
                    UnitTools.showC(
                        tempCorrect(tempResult.maxTemperature),
                        isShowC
                    )
                }\nMin:${UnitTools.showC(tempCorrect(tempResult.minTemperature), isShowC)}"
                rightValue = showUnitValue(struct.customPseudoBean.maxTemp, isShowC)
                leftValue = showUnitValue(struct.customPseudoBean.minTemp, isShowC)
                temperatureIvInput.setImageResource(ThermalIrR.drawable.ir_model)
                temperatureIvLock.visibility = View.INVISIBLE
                temperatureSeekbar.setColorList(
                    struct.customPseudoBean.getColorList(struct.isTC007())?.reversedArray()
                )
                temperatureSeekbar.setPlaces(struct.customPseudoBean.getPlaceList())
            } else {
                tvTempContent.visibility = View.GONE
                tvTempContent.text = "Max:${
                    UnitTools.showC(
                        tempCorrect(tempResult.maxTemperature),
                        isShowC
                    )
                }\nMin:${UnitTools.showC(tempCorrect(tempResult.minTemperature), isShowC)}"
                rightValue = showUnitValue(tempCorrect(tempResult.maxTemperature), isShowC)
                leftValue = showUnitValue(tempCorrect(tempResult.minTemperature), isShowC)
                temperatureIvInput.setImageResource(ThermalIrR.drawable.ic_color_edit)
                temperatureIvLock.visibility = View.VISIBLE
            }
            temperatureSeekbar.setRange(leftValue, rightValue, 0.1f)
            temperatureSeekbar.setProgress(leftValue, rightValue)
            if (ScreenTool.isIPad(this@IRGalleryEditActivity)) {
                colorBarView.setPadding(0, SizeUtils.dp2px(40f), 0, SizeUtils.dp2px(40f))
            }
        }
    }

    private fun updateImage(bitmap: Bitmap?) {
        bitmap?.let {
            val params = irImageView.layoutParams as ConstraintLayout.LayoutParams
            params.dimensionRatio = "${bitmap.width}:${bitmap.height}"
            runOnUiThread {
                irImageView.layoutParams = params
            }
            if (struct.watermarkBean.isOpen) {
                val width = ScreenUtil.getScreenWidth(this)
                val height = (width * bitmap.height / bitmap.width.toFloat()).toInt()
                irImageView.setImageBitmap(
                    BitmapUtils.drawCenterLable(
                        Bitmap.createScaledBitmap(it, width, height, true),
                        struct.watermarkBean.title,
                        struct.watermarkBean.address,
                        if (struct.watermarkBean.isAddTime) TimeTool.getNowTime() else "",
                        if (temperatureSeekbar.isVisible) {
                            temperatureSeekbar.measuredWidth
                        } else {
                            0
                        },
                    ),
                )
            } else {
                irImageView.setImageBitmap(it)
            }
        }
    }

    private fun initRecycler() {
        editRecyclerFirst.onTabClickListener = {
            when (it) {
                0 -> editRecyclerSecond.selectPosition(1)
                1 -> editRecyclerSecond.selectPosition(3)
                2 -> editRecyclerSecond.selectPosition(4)
            }
        }
        editRecyclerFirst.onBarClickListener = {
            colorBarView.isVisible = it
        }

        editRecyclerSecond.onFenceListener = { fenceType, isSelected ->
            when (fenceType) {
                FenceType.POINT -> temperatureView.mode = Mode.POINT
                FenceType.LINE -> temperatureView.mode = Mode.LINE
                FenceType.RECT -> temperatureView.mode = Mode.RECT
                FenceType.DEL -> temperatureView.mode = Mode.CLEAR
                FenceType.FULL -> temperatureView.isShowFull = isSelected
                FenceType.TREND -> {

                }
            }
        }
        editRecyclerSecond.onColorListener = { _, it, _ ->
            if (struct.customPseudoBean.isUseCustomPseudo) {
                TipDialog.Builder(this)
                    .setTitleMessage(getString(R.string.app_tip))
                    .setMessage(R.string.tip_change_pseudo_mode)
                    .setPositiveListener(R.string.app_yes) {
                        struct.customPseudoBean.isUseCustomPseudo = false
                        setDefLimit()
                        setPColor(it)
                        updateImageAndSeekbarColorList(struct.customPseudoBean)
                    }.setCancelListener(R.string.app_no) {
                    }
                    .create().show()
            } else {
                setPColor(it)
            }
        }
        editRecyclerSecond.onSettingListener = { type, _ ->
            setSettingValue(type)
        }
    }

    private fun setDefLimit() {
        val tempResult = frameTool.getSrcTemp()
        rightValue = showUnitValue(tempCorrect(tempResult.maxTemperature), isShowC)
        leftValue = showUnitValue(tempCorrect(tempResult.minTemperature), isShowC)
        temperatureSeekbar.setRange(leftValue, rightValue, 0.1f)
        temperatureSeekbar.setProgress(leftValue, rightValue)
    }

    private fun setPColor(code: Int) {
        pseudocodeMode = code
        temperatureSeekbar.setPseudocode(pseudocodeMode)
        updateImage(
            frameTool.getScrPseudoColorScaledBitmap(
                changePseudocodeModeByOld(pseudocodeMode),
                showToCValue(max),
                showToCValue(min),
                rotate,
                struct.customPseudoBean,
                maxTemperature = tempCorrect(frameTool.getSrcTemp().maxTemperature),
                minTemperature = tempCorrect(frameTool.getSrcTemp().minTemperature),
                struct.isAmplify,
            ),
        )
        temperatureSeekbar.setColorList(
            struct.customPseudoBean.getColorList(struct.isTC007())?.reversedArray()
        )
        temperatureSeekbar.setPlaces(struct.customPseudoBean.getPlaceList())
        editRecyclerSecond.setPseudoColor(code)
    }

    private var tempAlarmSetDialog: TempAlarmSetDialog? = null

    private fun setSettingValue(type: SettingType) {
        when (type) {
            SettingType.ALARM -> {

                if (tempAlarmSetDialog == null) {
                    tempAlarmSetDialog = TempAlarmSetDialog(this, true)
                    tempAlarmSetDialog?.onSaveListener = {
                        editRecyclerSecond.setSettingSelected(
                            SettingType.ALARM,
                            it.isHighOpen || it.isLowOpen
                        )
                        struct.alarmBean = it
                        frameTool.initStruct(struct)
                        updateImage(
                            frameTool.getScrPseudoColorScaledBitmap(
                                changePseudocodeModeByOld(pseudocodeMode),
                                showToCValue(max),
                                showToCValue(min),
                                rotate,
                                struct.customPseudoBean,
                                maxTemperature = tempCorrect(frameTool.getSrcTemp().maxTemperature),
                                minTemperature = tempCorrect(frameTool.getSrcTemp().minTemperature),
                                struct.isAmplify,
                            ),
                        )
                    }
                }
                tempAlarmSetDialog?.alarmBean = struct.alarmBean
                tempAlarmSetDialog?.show()
            }

            SettingType.FONT -> {
                val colorPickDialog =
                    ColorPickDialog(this, temperatureView.textColor, temperatureView.tempTextSize)
                colorPickDialog.onPickListener = { it: Int, textSize: Int ->
                    temperatureView?.textColor = it
                    struct.textSize = SizeUtils.sp2px(textSize.toFloat())
                    temperatureView?.tempTextSize = SizeUtils.sp2px(textSize.toFloat())
                    editRecyclerSecond.setSettingSelected(
                        SettingType.FONT,
                        it != 0xffffffff.toInt() || textSize != SizeUtils.sp2px(14f),
                    )
                }
                colorPickDialog.show()
            }

            SettingType.WATERMARK -> {
                TipWaterMarkDialog.Builder(this, struct.watermarkBean)
                    .setCancelListener {
                        struct.watermarkBean = it
                        frameTool.initStruct(struct)
                        editRecyclerSecond.setSettingSelected(SettingType.WATERMARK, it.isOpen)
                        updateImage(
                            frameTool.getScrPseudoColorScaledBitmap(
                                changePseudocodeModeByOld(pseudocodeMode),
                                showToCValue(max),
                                showToCValue(min),
                                rotate,
                                struct.customPseudoBean,
                                maxTemperature = tempCorrect(frameTool.getSrcTemp().maxTemperature),
                                minTemperature = tempCorrect(frameTool.getSrcTemp().minTemperature),
                                struct.isAmplify,
                            ),
                        )
                    }
                    .create().show()
            }

            else -> {

            }
        }
    }

    private fun updateTemperatureSeekBar(
        isEnabled: Boolean,
        resource: Int,
        content: String,
    ) {
        temperatureSeekbar.isEnabled = isEnabled
        temperatureIvLock.setImageResource(resource)
        temperatureIvLock.contentDescription = content
        if (isEnabled) {
            temperatureSeekbar.leftSeekBar.indicatorBackgroundColor = 0xffe17606.toInt()
            temperatureSeekbar.rightSeekBar.indicatorBackgroundColor = 0xffe17606.toInt()
        } else {
            temperatureSeekbar.leftSeekBar.indicatorBackgroundColor = 0
            temperatureSeekbar.rightSeekBar.indicatorBackgroundColor = 0
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            temperatureIvLock -> {
                if (temperatureIvLock.contentDescription == "lock") {
                    updateTemperatureSeekBar(
                        true,
                        ThermalLiteR.drawable.svg_pseudo_bar_unlock,
                        "unlock",
                    )
                } else {
                    setDefLimit()
                    updateTemperatureSeekBar(
                        false,
                        ThermalLiteR.drawable.svg_pseudo_bar_lock,
                        "lock"
                    )
                }
            }

            temperatureIvInput -> {
                val intent = Intent(this, PseudoSetActivity::class.java)
                intent.putExtra(ExtraKeyConfig.IS_TC007, isTC007)
                intent.putExtra(ExtraKeyConfig.CUSTOM_PSEUDO_BEAN, struct.customPseudoBean)
                pseudoSetResult.launch(intent)
            }
        }
    }

    private val pseudoSetResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val tmp =
                    it.data?.getParcelableExtra(ExtraKeyConfig.CUSTOM_PSEUDO_BEAN)
                        ?: CustomPseudoBean()
                updateImageAndSeekbarColorList(tmp)
                temperatureSeekbar.setColorList(tmp.getColorList(struct.isTC007())?.reversedArray())
                temperatureSeekbar.setPlaces(tmp.getPlaceList())

            }
        }

    private fun updateImageAndSeekbarColorList(customPseudoBean: CustomPseudoBean?) {
        customPseudoBean?.let {
            updateImage(
                frameTool.getScrPseudoColorScaledBitmap(
                    changePseudocodeModeByOld(
                        pseudocodeMode,
                    ),
                    rotate = rotate,
                    customPseudoBean = customPseudoBean,
                    maxTemperature = tempCorrect(frameTool.getSrcTemp().maxTemperature),
                    minTemperature = tempCorrect(frameTool.getSrcTemp().minTemperature),
                    isAmplify = struct.isAmplify,
                ),
            )
            if (it.isUseCustomPseudo) {
                temperatureIvLock.visibility = View.INVISIBLE
                tvTempContent.visibility = View.VISIBLE
                updateTemperatureSeekBar(
                    false,
                    ThermalLiteR.drawable.svg_pseudo_bar_lock,
                    "lock"
                )
                temperatureSeekbar.setRangeAndPro(
                    UnitTools.showUnitValue(it.minTemp, isShowC),
                    UnitTools.showUnitValue(it.maxTemp, isShowC),
                    UnitTools.showUnitValue(it.minTemp, isShowC),
                    UnitTools.showUnitValue(it.maxTemp, isShowC),
                )
                editRecyclerSecond.setPseudoColor(-1)
                temperatureIvInput.setImageResource(ThermalIrR.drawable.ir_model)
            } else {
                temperatureIvLock.visibility = View.VISIBLE
                if (struct.customPseudoBean.isUseCustomPseudo) {
                    setDefLimit()
                }
                tvTempContent.visibility = View.GONE
                editRecyclerSecond.setPseudoColor(pseudocodeMode)
                temperatureIvInput.setImageResource(ThermalIrR.drawable.ic_color_edit)
            }
            struct.customPseudoBean = customPseudoBean
            temperatureSeekbar.setColorList(
                customPseudoBean.getColorList(struct.isTC007())?.reversedArray()
            )
            temperatureSeekbar.setPlaces(customPseudoBean.getPlaceList())
        }

    }

    private var isReportPick = false

    private fun initUI() {
        isReportPick = intent.getBooleanExtra(ExtraKeyConfig.IS_PICK_REPORT_IMG, false)
        titleView.setLeftClickListener {
            if (isReportPick) {
                finish()
            } else {
                saveImage()
            }
        }
        titleView.setRightText(if (isReportPick) R.string.app_next else R.string.person_save)
        titleView.setRightClickListener {
            if (!isReportPick) {
                updateIconSave()
            } else {
                showLoadingDialog()
                lifecycleScope.launch(Dispatchers.IO) {

                    var irBitmap =
                        if (struct.isAmplify) {

                            OpencvTools.supImageFourExToBitmap(frameTool.getBaseBitmap(rotate))
                        } else {
                            irImageView.drawToBitmap()
                        }
                    if (temperatureView.mode != Mode.CLEAR) {

                        irBitmap =
                            BitmapUtils.mergeBitmap(irBitmap, temperatureView.drawToBitmap(), 0, 0)
                    }

                    if (colorBarView.visibility == View.VISIBLE) {
                        irBitmap =
                            BitmapUtils.mergeBitmap(irBitmap, colorBarView.drawToBitmap(), 0, 0)
                    }

                    val fileAbsolutePath =
                        ImageUtils.saveToCache(this@IRGalleryEditActivity, irBitmap)
                    launch(Dispatchers.Main) {
                        dismissLoadingDialog()
                        if (intent.getBooleanExtra(IS_REPORT_FIRST, true)) {
                            NavigationManager.build(RouterConfig.REPORT_CREATE_FIRST)
                                .withBoolean(ExtraKeyConfig.IS_TC007, isTC007)
                                .withString(ExtraKeyConfig.FILE_ABSOLUTE_PATH, fileAbsolutePath)
                                .withParcelable(
                                    ExtraKeyConfig.IMAGE_TEMP_BEAN,
                                    buildImageTempBean()
                                )
                                .navigation(this@IRGalleryEditActivity)
                        } else {
                            val navigationBuilder =
                                NavigationManager.build(RouterConfig.REPORT_CREATE_SECOND)
                                    .withBoolean(ExtraKeyConfig.IS_TC007, isTC007)
                                    .withString(ExtraKeyConfig.FILE_ABSOLUTE_PATH, fileAbsolutePath)
                                    .withParcelable(
                                        ExtraKeyConfig.IMAGE_TEMP_BEAN,
                                        buildImageTempBean()
                                    )

                            intent.getParcelableExtra<Parcelable>(ExtraKeyConfig.REPORT_INFO)?.let {
                                navigationBuilder.withParcelable(ExtraKeyConfig.REPORT_INFO, it)
                            }
                            intent.getParcelableExtra<Parcelable>(ExtraKeyConfig.REPORT_CONDITION)
                                ?.let {
                                    navigationBuilder.withParcelable(
                                        ExtraKeyConfig.REPORT_CONDITION,
                                        it
                                    )
                                }
                            intent.getParcelableArrayListExtra<Parcelable>(ExtraKeyConfig.REPORT_IR_LIST)
                                ?.let {
                                    navigationBuilder.withParcelableArrayList(
                                        ExtraKeyConfig.REPORT_IR_LIST,
                                        it
                                    )
                                }
                            navigationBuilder.navigation(this@IRGalleryEditActivity)
                        }
                    }
                }
            }
        }
        editRecyclerSecond.selectPosition(-1)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReportCreate(event: ReportCreateEvent) {
        if (isReportPick) {
            finish()
        }
    }

    private fun keepOneDigit(float: Float) = String.format(Locale.ENGLISH, "%.1f", float)

    private fun buildImageTempBean(): ImageTempBean {
        var full: ImageTempBean.TempBean? = null
        if (temperatureView.isShowFull) {
            temperatureView.fullInfo?.let {
                val max = keepOneDigit(tempCorrect(it.maxTemperature))
                val min = keepOneDigit(tempCorrect(it.minTemperature))
                full = ImageTempBean.TempBean(max, min)
            }
        }

        val pointList = arrayListOf<ImageTempBean.TempBean>()
        for (temp in temperatureView.tempListData.pointTemps) {
            if (temp.type != -99) {
                pointList.add(ImageTempBean.TempBean(keepOneDigit(tempCorrect(temp.maxTemperature))))
            }
        }

        val lineList = arrayListOf<ImageTempBean.TempBean>()
        for (temp in temperatureView.tempListData.lineTemps) {
            if (temp.type != -99) {
                val max = keepOneDigit(tempCorrect(temp.maxTemperature))
                val min = keepOneDigit(tempCorrect(temp.minTemperature))
                val average = keepOneDigit(temp.averageTemperature)
                lineList.add(ImageTempBean.TempBean(max, min, average))
            }
        }

        val rectList = arrayListOf<ImageTempBean.TempBean>()
        for (temp in temperatureView.tempListData.rectangleTemps) {
            if (temp.type != -99) {
                val max = keepOneDigit(tempCorrect(temp.maxTemperature))
                val min = keepOneDigit(tempCorrect(temp.minTemperature))
                val average = keepOneDigit(temp.averageTemperature)
                rectList.add(ImageTempBean.TempBean(max, min, average))
            }
        }

        return ImageTempBean(full, pointList, lineList, rectList)
    }

    private fun saveImage() {
        TipDialog.Builder(this)
            .setTitleMessage(getString(R.string.app_tip))
            .setMessage(R.string.app_save_image)
            .setPositiveListener(R.string.app_yes) {
                updateIconSave()
            }.setCancelListener(R.string.app_no) {
                finish()
            }
            .create().show()
    }

    private fun updateIconSave() {
        lifecycleScope.launch(Dispatchers.IO) {

            var irBitmap =
                if (struct.isAmplify) {

                    OpencvTools.supImageFourExToBitmap(frameTool.getBaseBitmap(rotate))
                } else {
                    irImageView.drawToBitmap()
                }
            if (temperatureView.mode != Mode.CLEAR) {

                irBitmap = BitmapUtils.mergeBitmap(irBitmap, temperatureView.drawToBitmap(), 0, 0)
            }

            if (colorBarView.visibility == View.VISIBLE) {
                irBitmap = BitmapUtils.mergeBitmap(irBitmap, colorBarView.drawToBitmap(), 0, 0)
            }

            var name: String
            irBitmap.let {
                name = ImageUtils.save(bitmap = it, isTC007)
            }
            ImageUtils.saveFrame(bs = mFrame, capital = getCapital(), name = name)
            ToastTools.showShort(R.string.tip_photo_saved)
            EventBus.getDefault().post(GalleryAddEvent())
            MediaScannerConnection.scanFile(
                this@IRGalleryEditActivity,
                arrayOf(FileConfig.lineGalleryDir),
                null,
                null,
            )
            EventBus.getDefault().post(ImageGalleryEvent())
            finish()
        }
    }

    private fun getCapital(): ByteArray {
        val capital: ByteArray?
        capital =
            FrameStruct.toCode(
                name = struct.name,
                width = struct.width,
                height = struct.height,
                rotate = struct.rotate,
                pseudo = pseudocodeMode,
                initRotate = struct.initRotate,
                correctRotate = struct.correctRotate,
                customPseudoBean = struct.customPseudoBean,
                isShowPseudoBar = colorBarView.isVisible,
                textColor = temperatureView.textColor,
                watermarkBean = struct.watermarkBean,
                alarmBean = struct.alarmBean,
                gainStatus = struct.gainStatus,
                textSize = struct.textSize,
                struct.environment,
                struct.distance,
                struct.radiation,
                false,
            )
        return capital
    }

    override fun onBackPressed() {
        lifecycleScope.launch {
            if (isReportPick) {
                finish()
            } else {
                saveImage()
            }
        }
    }

    override fun tempCorrectByTs(temp: Float?): Float {
        var tmp = temp
        try {
            tmp = tempCorrect(temp!!)
        } catch (e: Exception) {
            XLog.i("[ph][ph][ph][ph][ph][ph]: ${e.message}")
        }
        return tmp!!
    }

    private fun tempCorrect(temp: Float): Float {
        var newTemp = temp
        try {
            if (struct == null || struct.distance <= 0 || struct.radiation <= 0) {
                return temp
            }
            val paramsArray =
                floatArrayOf(
                    temp,
                    struct.radiation,
                    struct.environment,
                    struct.environment,
                    struct.distance,
                    0.8f,
                )
            if (struct.name.startsWith(PRODUCT_NAME_TS)) {
                if (ts_data_H == null || ts_data_L == null) return temp
                newTemp =
                    IRUtils.temperatureCorrection(
                        IRCMDType.USB_IR_256_384,
                        CommonParams.ProductType.WN256_ADVANCED,
                        paramsArray[0],
                        ts_data_H,
                        ts_data_L,
                        paramsArray[1],
                        paramsArray[2],
                        paramsArray[3],
                        paramsArray[4],
                        paramsArray[5],
                        0,
                        if (struct.gainStatus == 1) {
                            CommonParams.GainStatus.HIGH_GAIN
                        } else {
                            CommonParams.GainStatus.LOW_GAIN
                        },
                    )
            } else if (struct.name.startsWith(PRODUCT_NAME_TC001LITE)) {

                if (BaseApplication.instance.tau_data_H == null || BaseApplication.instance.tau_data_L == null) return temp
                newTemp =
                    IRTool.temperatureCorrection(
                        temp, paramsArray, BaseApplication.instance.tau_data_H!!,
                        BaseApplication.instance.tau_data_L!!, struct.gainStatus,
                    )
            }
        } catch (e: Exception) {
            XLog.e("$TAG:tempCorrect-${e.message}")
        } finally {
            return newTemp
        }
    }
}
