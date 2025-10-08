// Merged ALL .kt and .java files from the '_ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\comm' directory and its subdirectories.
// Total files: 5 | Generated on: 2025-10-08 01:42:45


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\bean\libunified_src_main_java_com_mpdc4gsr_libunified_app_comm_bean_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\bean' subtree
// Files: 1; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\bean\SaveSettingBean.kt =====

package com.mpdc4gsr.libunified.app.comm.bean

import android.util.TypedValue
import com.mpdc4gsr.libunified.compat.ContextProvider
import com.mpdc4gsr.libunified.compat.SPUtils
import com.google.gson.Gson
import com.mpdc4gsr.libunified.app.bean.AlarmBean
import com.mpdc4gsr.libunified.app.bean.CameraItemBean
import com.mpdc4gsr.libunified.app.bean.ObserveBean
import com.mpdc4gsr.libunified.app.common.SaveSettingUtils
import com.mpdc4gsr.libunified.app.config.DeviceConfig
import com.mpdc4gsr.libunified.app.utils.CommUtils

class SaveSettingBean(private val isWifi: Boolean = false) {
    private fun getSPUtils(): SPUtils =
        SPUtils.getInstance(if (isWifi) "WifiSaveSettingUtils" else "SaveSettingUtils")

    var isSaveSetting: Boolean = getSPUtils().getBoolean("isSaveSetting", true)
        set(value) {
            field = value
            getSPUtils().put("isSaveSetting", value)
        }
    var isMeasureTempMode: Boolean =
        if (isSaveSetting) getSPUtils().getBoolean("isMeasureTempMode", true) else true
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("isMeasureTempMode", value)
            }
        }
    var isOpenAmplify: Boolean =
        if (isSaveSetting) getSPUtils().getBoolean("isOpenAmplify", false) else false
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("isOpenAmplify", value)
            }
        }
    var isVideoMode: Boolean =
        if (isSaveSetting) getSPUtils().getBoolean("isVideoMode", false) else false
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("isVideoMode", value)
            }
        }
    var isAutoShutter: Boolean =
        if (isSaveSetting) getSPUtils().getBoolean("isAutoShutter", true) else true
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("isAutoShutter", value)
            }
        }
    var isRecordAudio: Boolean =
        if (isSaveSetting) getSPUtils().getBoolean("isRecordAudio", false) else false
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("isRecordAudio", value)
            }
        }
    var delayCaptureSecond: Int =
        if (isSaveSetting) getSPUtils().getInt("delayCaptureSecond", 0) else 0
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("delayCaptureSecond", value)
            }
        }
    var fusionType: Int =
        if (isSaveSetting) {
            getSPUtils().getInt(
                "fusionType",
                SaveSettingUtils.FusionTypeLPYFusion,
            )
        } else {
            SaveSettingUtils.FusionTypeLPYFusion
        }
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("fusionType", value)
            }
        }
    var isOpenTwoLight: Boolean =
        if (isSaveSetting) getSPUtils().getBoolean("isOpenTwoLight", false) else false
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("isOpenTwoLight", value)
            }
        }
    var twoLightAlpha: Int = if (isSaveSetting) getSPUtils().getInt("twoLightAlpha", 50) else 50
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("twoLightAlpha", value)
            }
        }
    var pseudoColorMode: Int = if (isSaveSetting) getSPUtils().getInt("pseudoColorMode", 3) else 3
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("pseudoColorMode", value)
            }
        }
    var isOpenPseudoBar: Boolean =
        if (isSaveSetting) getSPUtils().getBoolean("isOpenPseudoBar", true) else true
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("isOpenPseudoBar", value)
            }
        }
    var contrastValue: Int = if (isSaveSetting) getSPUtils().getInt("contrastValue", 128) else 128
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("contrastValue", value)
            }
        }
    var ddeConfig: Int = if (isSaveSetting) getSPUtils().getInt("ddeConfig", 2) else 2
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("ddeConfig", value)
            }
        }
    var alarmBean: AlarmBean =
        if (isSaveSetting) {
            val json = getSPUtils().getString("alarmBean", "")
            if (json.isNullOrEmpty()) AlarmBean() else Gson().fromJson(json, AlarmBean::class.java)
        } else {
            AlarmBean()
        }
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("alarmBean", Gson().toJson(value))
            }
        }
    var rotateAngle: Int =
        if (isSaveSetting) {
            getSPUtils().getInt(
                "rotateAngle",
                DeviceConfig.S_ROTATE_ANGLE,
            )
        } else {
            DeviceConfig.S_ROTATE_ANGLE
        }
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("rotateAngle", value)
            }
        }

    fun isRotatePortrait(): Boolean = rotateAngle == 90 || rotateAngle == 270
    var isOpenMirror: Boolean =
        if (isSaveSetting) getSPUtils().getBoolean("isOpenMirror", false) else false
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("isOpenMirror", value)
            }
        }
    var isOpenCompass: Boolean =
        if (isSaveSetting) getSPUtils().getBoolean("isOpenCompass", false) else false
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("isOpenCompass", value)
            }
        }
    var tempTextColor: Int = if (isSaveSetting) getSPUtils().getInt(
        "tempTextColor",
        0xffffffff.toInt()
    ) else 0xffffffff.toInt()
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("tempTextColor", value)
            }
        }
    var tempTextSize: Int = run {
        val context = ContextProvider.getContext()
        val defaultSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            14f,
            context.resources.displayMetrics
        ).toInt()
        if (isSaveSetting) getSPUtils().getInt("tempTextSize", defaultSize) else defaultSize
    }
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("tempTextSize", value)
            }
        }

    fun isTempTextDefault(): Boolean {
        val context = ContextProvider.getContext()
        val defaultSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            14f,
            context.resources.displayMetrics
        ).toInt()
        return tempTextColor == 0xffffffff.toInt() && tempTextSize == defaultSize
    }

    var temperatureMode: Int =
        if (isSaveSetting) {
            getSPUtils().getInt(
                "temperatureMode",
                CameraItemBean.TYPE_TMP_C,
            )
        } else {
            CameraItemBean.TYPE_TMP_C
        }
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("temperatureMode", value)
            }
        }
    var isOpenHighPoint: Boolean =
        if (isSaveSetting) getSPUtils().getBoolean("isOpenHighPoint", false) else false
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("isOpenHighPoint", value)
            }
        }
    var isOpenLowPoint: Boolean =
        if (isSaveSetting) getSPUtils().getBoolean("isOpenLowPoint", false) else false
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("isOpenLowPoint", value)
            }
        }
    var aiTraceType: Int = if (isSaveSetting) getSPUtils().getInt(
        "aiTraceType",
        ObserveBean.TYPE_NONE
    ) else ObserveBean.TYPE_NONE
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("aiTraceType", value)
            }
        }
    var isOpenTarget: Boolean =
        if (isSaveSetting) getSPUtils().getBoolean("isOpenTarget", false) else false
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("isOpenTarget", value)
            }
        }
    var targetMeasureMode: Int =
        if (isSaveSetting) {
            getSPUtils().getInt(
                "targetMeasureMode",
                ObserveBean.TYPE_MEASURE_PERSON,
            )
        } else {
            ObserveBean.TYPE_MEASURE_PERSON
        }
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("targetMeasureMode", value)
            }
        }
    var targetType: Int =
        if (isSaveSetting) {
            getSPUtils().getInt(
                "targetType",
                ObserveBean.TYPE_TARGET_HORIZONTAL,
            )
        } else {
            ObserveBean.TYPE_TARGET_HORIZONTAL
        }
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("targetType", value)
            }
        }
    var targetColorType: Int =
        if (isSaveSetting) {
            getSPUtils().getInt(
                "targetColorType",
                ObserveBean.TYPE_TARGET_COLOR_GREEN,
            )
        } else {
            ObserveBean.TYPE_TARGET_COLOR_GREEN
        }
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("targetColorType", value)
            }
        }
    var reportAuthorName: String =
        if (isSaveSetting) {
            getSPUtils().getString(
                "reportAuthorName",
                CommUtils.getAppName(),
            )
        } else {
            CommUtils.getAppName()
        }
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("reportAuthorName", value)
            }
        }
    var reportWatermarkText: String =
        if (isSaveSetting) {
            getSPUtils().getString(
                "reportWatermarkText",
                CommUtils.getAppName(),
            )
        } else {
            CommUtils.getAppName()
        }
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("reportWatermarkText", value)
            }
        }
    var reportHumidity: Int = if (isSaveSetting) getSPUtils().getInt("reportHumidity", 500) else 500
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("reportHumidity", value)
            }
        }
}


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\dialog\libunified_src_main_java_com_mpdc4gsr_libunified_app_comm_dialog_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\dialog' subtree
// Files: 2; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\dialog\ColorPickDialog.kt =====

package com.mpdc4gsr.libunified.app.comm.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.mpdc4gsr.libunified.compat.dpToPx
import com.mpdc4gsr.libunified.compat.spToPx
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.app.utils.ColorUtils
import com.mpdc4gsr.libunified.app.utils.ScreenUtils
import com.mpdc4gsr.libunified.app.view.ColorSelectView
import com.mpdc4gsr.libunified.ui.widget.seekbar.OnRangeChangedListener
import com.mpdc4gsr.libunified.ui.widget.seekbar.RangeSeekBar

class ColorPickDialog(
    context: Context,
    @ColorInt private var color: Int,
    var textSize: Int,
    var textSizeIsDP: Boolean = false,
) : Dialog(context, R.style.InfoDialog), View.OnClickListener {
    var onPickListener: ((color: Int, textSize: Int) -> Unit)? = null
    private val rootView: View =
        LayoutInflater.from(context).inflate(R.layout.dialog_color_pick, null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(true)
        setCanceledOnTouchOutside(true)
        setContentView(rootView)
        window?.let {
            val layoutParams = it.attributes
            layoutParams.width = (ScreenUtils.getScreenWidth(context) * 0.9).toInt()
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            it.attributes = layoutParams
        }
        val activeTrackColor =
            ColorUtils.setColorAlpha(
                ContextCompat.getColor(context, R.color.we_read_theme_color),
                0.1f
            )
        val iconTintColor =
            ColorUtils.setColorAlpha(
                ContextCompat.getColor(context, R.color.we_read_theme_color),
                0.7f
            )
        when (color) {
            0xff0000ff.toInt() -> rootView.findViewById<View>(R.id.view_color1).isSelected = true
            0xffff0000.toInt() -> rootView.findViewById<View>(R.id.view_color2).isSelected = true
            0xff00ff00.toInt() -> rootView.findViewById<View>(R.id.view_color3).isSelected = true
            0xffffff00.toInt() -> rootView.findViewById<View>(R.id.view_color4).isSelected = true
            0xff000000.toInt() -> rootView.findViewById<View>(R.id.view_color5).isSelected = true
            0xffffffff.toInt() -> rootView.findViewById<View>(R.id.view_color6).isSelected = true
            else -> rootView.findViewById<ColorSelectView>(R.id.color_select_view)
                .selectColor(color)
        }
        rootView.findViewById<ColorSelectView>(R.id.color_select_view).onSelectListener = {
            unSelect6Color()
            color = it
        }
        if (textSize != -1) {
            findViewById<TextView>(R.id.tv_size_title).visibility = View.VISIBLE
            findViewById<TextView>(R.id.tv_size_value).visibility = View.VISIBLE
            findViewById<TextView>(R.id.tv_nifty_left).visibility = View.VISIBLE
            findViewById<TextView>(R.id.tv_nifty_right).visibility = View.VISIBLE
            findViewById<RangeSeekBar>(R.id.nifty_slider_view).visibility = View.VISIBLE
            findViewById<RangeSeekBar>(R.id.nifty_slider_view).setOnRangeChangedListener(
                object : OnRangeChangedListener {
                    override fun onRangeChanged(
                        view: RangeSeekBar,
                        leftValue: Float,
                        rightValue: Float,
                        isFromUser: Boolean,
                        tempMode: Int
                    ) {
                        var text = ""
                        text =
                            if (leftValue <= 0) {
                                textSize = 14
                                context.getString(R.string.temp_text_standard)
                            } else if (leftValue <= 50) {
                                textSize = 16
                                context.getString(R.string.temp_text_big)
                            } else {
                                textSize = 18
                                context.getString(R.string.temp_text_sup_big)
                            }
                        findViewById<TextView>(R.id.tv_size_value).text = text
                    }

                    override fun onStartTrackingTouch(
                        view: RangeSeekBar,
                        isLeft: Boolean,
                    ) {
                    }

                    override fun onStopTrackingTouch(
                        view: RangeSeekBar,
                        isLeft: Boolean,
                    ) {
                    }
                },
            )
            findViewById<RangeSeekBar>(R.id.nifty_slider_view).setProgress(
                textSizeToNifyValue(
                    textSize
                )
            )
        } else {
            findViewById<RangeSeekBar>(R.id.nifty_slider_view).visibility = View.GONE
        }
        rootView.findViewById<View>(R.id.view_color1).setOnClickListener(this)
        rootView.findViewById<View>(R.id.view_color2).setOnClickListener(this)
        rootView.findViewById<View>(R.id.view_color3).setOnClickListener(this)
        rootView.findViewById<View>(R.id.view_color4).setOnClickListener(this)
        rootView.findViewById<View>(R.id.view_color5).setOnClickListener(this)
        rootView.findViewById<View>(R.id.view_color6).setOnClickListener(this)
        rootView.findViewById<View>(R.id.rl_close).setOnClickListener(this)
        rootView.findViewById<View>(R.id.tv_save).setOnClickListener(this)
    }

    private fun textSizeToNifyValue(
        size: Int,
        // isTC007 parameter removed - TC007 functionality disabled
    ): Float {
        // Always use default behavior, TC007 functionality removed
        return when (size) {
            14f.spToPx(context).toInt() -> 0f
            16f.spToPx(context).toInt() -> 50f
            else -> 100f
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            rootView.findViewById<View>(R.id.rl_close) -> dismiss()
            rootView.findViewById<View>(R.id.tv_save) -> {
                dismiss()
                onPickListener?.invoke(color, textSize)
            }

            rootView.findViewById<View>(R.id.view_color1) -> {
                unSelect6Color()
                rootView.findViewById<ColorSelectView>(R.id.color_select_view).reset()
                rootView.findViewById<View>(R.id.view_color1).isSelected = true
                color = 0xff0000ff.toInt()
            }

            rootView.findViewById<View>(R.id.view_color2) -> {
                unSelect6Color()
                rootView.findViewById<ColorSelectView>(R.id.color_select_view).reset()
                rootView.findViewById<View>(R.id.view_color2).isSelected = true
                color = 0xffff0000.toInt()
            }

            rootView.findViewById<View>(R.id.view_color3) -> {
                unSelect6Color()
                rootView.findViewById<ColorSelectView>(R.id.color_select_view).reset()
                rootView.findViewById<View>(R.id.view_color3).isSelected = true
                color = 0xff00ff00.toInt()
            }

            rootView.findViewById<View>(R.id.view_color4) -> {
                unSelect6Color()
                rootView.findViewById<ColorSelectView>(R.id.color_select_view).reset()
                rootView.findViewById<View>(R.id.view_color4).isSelected = true
                color = 0xffffff00.toInt()
            }

            rootView.findViewById<View>(R.id.view_color5) -> {
                unSelect6Color()
                rootView.findViewById<ColorSelectView>(R.id.color_select_view).reset()
                rootView.findViewById<View>(R.id.view_color5).isSelected = true
                color = 0xff000000.toInt()
            }

            rootView.findViewById<View>(R.id.view_color6) -> {
                unSelect6Color()
                rootView.findViewById<ColorSelectView>(R.id.color_select_view).reset()
                rootView.findViewById<View>(R.id.view_color6).isSelected = true
                color = 0xffffffff.toInt()
            }
        }
    }

    private fun unSelect6Color() {
        rootView.findViewById<View>(R.id.view_color1).isSelected = false
        rootView.findViewById<View>(R.id.view_color2).isSelected = false
        rootView.findViewById<View>(R.id.view_color3).isSelected = false
        rootView.findViewById<View>(R.id.view_color4).isSelected = false
        rootView.findViewById<View>(R.id.view_color5).isSelected = false
        rootView.findViewById<View>(R.id.view_color6).isSelected = false
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\dialog\TempAlarmSetDialog.kt =====

package com.mpdc4gsr.libunified.app.comm.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import coil.load
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.app.bean.AlarmBean
import com.mpdc4gsr.libunified.app.tools.ToastTools
import com.mpdc4gsr.libunified.app.tools.UnitTools

class TempAlarmSetDialog(
    context: Context,
    private val isEdit: Boolean,
) : Dialog(context, R.style.app_compat_dialog), CompoundButton.OnCheckedChangeListener {
    var alarmBean = AlarmBean()
        set(value) {
            field = value.copy()
        }
    var onSaveListener: ((alarmBean: AlarmBean) -> Unit)? = null
    private var mediaPlayer: MediaPlayer? = null
    public var hideAlarmMark = false
    private lateinit var clRoot: ConstraintLayout
    private lateinit var clClose: ConstraintLayout
    private lateinit var tvSave: TextView
    private lateinit var ivRingtone1: ImageView
    private lateinit var ivRingtone2: ImageView
    private lateinit var ivRingtone3: ImageView
    private lateinit var ivRingtone4: ImageView
    private lateinit var ivRingtone5: ImageView
    private lateinit var switchAlarmHigh: SwitchCompat
    private lateinit var switchAlarmLow: SwitchCompat
    private lateinit var switchAlarmMark: SwitchCompat
    private lateinit var switchAlarmRingtone: SwitchCompat
    private lateinit var imgMarkHigh: ImageView
    private lateinit var imgMarkLow: ImageView
    private lateinit var ivCheckStoke: ImageView
    private lateinit var ivCheckMatrix: ImageView
    private lateinit var tvAlarmHighUnit: TextView
    private lateinit var tvAlarmLowUnit: TextView
    private lateinit var etAlarmHigh: EditText
    private lateinit var etAlarmLow: EditText
    private lateinit var imgCAlarmHigh: ImageView
    private lateinit var imgCAlarmLow: ImageView
    private lateinit var clAlarmMark: ConstraintLayout
    private lateinit var clRingtoneSelect: ConstraintLayout
    private lateinit var tvAlarmRingtone: TextView
    private lateinit var tvAlarmMark: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        setContentView(LayoutInflater.from(context).inflate(R.layout.dialog_temp_alarm_set, null))
        initView()
        window?.let {
            val layoutParams = it.attributes
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            it.attributes = layoutParams
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        dismiss()
    }

    private fun initView() {
        clRoot = findViewById(R.id.cl_root)
        clClose = findViewById(R.id.cl_close)
        tvSave = findViewById(R.id.tv_save)
        ivRingtone1 = findViewById(R.id.iv_ringtone1)
        ivRingtone2 = findViewById(R.id.iv_ringtone2)
        ivRingtone3 = findViewById(R.id.iv_ringtone3)
        ivRingtone4 = findViewById(R.id.iv_ringtone4)
        ivRingtone5 = findViewById(R.id.iv_ringtone5)
        switchAlarmHigh = findViewById(R.id.switch_alarm_high)
        switchAlarmLow = findViewById(R.id.switch_alarm_low)
        switchAlarmMark = findViewById(R.id.switch_alarm_mark)
        switchAlarmRingtone = findViewById(R.id.switch_alarm_ringtone)
        imgMarkHigh = findViewById(R.id.img_mark_high)
        imgMarkLow = findViewById(R.id.img_mark_low)
        ivCheckStoke = findViewById(R.id.iv_check_stoke)
        ivCheckMatrix = findViewById(R.id.iv_check_matrix)
        tvAlarmHighUnit = findViewById(R.id.tv_alarm_high_unit)
        tvAlarmLowUnit = findViewById(R.id.tv_alarm_low_unit)
        etAlarmHigh = findViewById(R.id.et_alarm_high)
        etAlarmLow = findViewById(R.id.et_alarm_low)
        imgCAlarmHigh = findViewById(R.id.img_c_alarm_high)
        imgCAlarmLow = findViewById(R.id.img_c_alarm_low)
        clAlarmMark = findViewById(R.id.cl_alarm_mark)
        clRingtoneSelect = findViewById(R.id.cl_ringtone_select)
        tvAlarmRingtone = findViewById(R.id.tv_alarm_ringtone)
        tvAlarmMark = findViewById(R.id.tv_alarm_mark)
        clRoot.setOnClickListener { dismiss() }
        clClose.setOnClickListener { dismiss() }
        tvSave.setOnClickListener { save() }
        ivRingtone1.setOnClickListener { selectRingtone(0) }
        ivRingtone2.setOnClickListener { selectRingtone(1) }
        ivRingtone3.setOnClickListener { selectRingtone(2) }
        ivRingtone4.setOnClickListener { selectRingtone(3) }
        ivRingtone5.setOnClickListener { selectRingtone(4) }
        switchAlarmHigh.setOnCheckedChangeListener(this)
        switchAlarmLow.setOnCheckedChangeListener(this)
        switchAlarmMark.setOnCheckedChangeListener(this)
        switchAlarmRingtone.setOnCheckedChangeListener(this)
        imgMarkHigh.setOnClickListener {
            showColorDialog(true)
        }
        imgMarkLow.setOnClickListener {
            showColorDialog(false)
        }
        ivCheckStoke.setOnClickListener {
            if (!ivCheckStoke.isSelected) {
                ivCheckStoke.isSelected = true
                ivCheckMatrix.isSelected = false
                alarmBean.markType = AlarmBean.TYPE_ALARM_MARK_STROKE
            }
        }
        ivCheckMatrix.setOnClickListener {
            if (!ivCheckMatrix.isSelected) {
                ivCheckStoke.isSelected = false
                ivCheckMatrix.isSelected = true
                alarmBean.markType = AlarmBean.TYPE_ALARM_MARK_MATRIX
            }
        }
        tvAlarmHighUnit.text = UnitTools.showUnit()
        tvAlarmLowUnit.text = UnitTools.showUnit()
    }

    override fun show() {
        super.show()
        refreshAlarmView()
    }

    private fun refreshAlarmView() {
        switchAlarmHigh.isChecked = alarmBean.isHighOpen
        switchAlarmLow.isChecked = alarmBean.isLowOpen
        switchAlarmMark.isChecked = isEdit || alarmBean.isMarkOpen
        if (!isEdit) {
            switchAlarmRingtone.isChecked = alarmBean.isRingtoneOpen
        }
        ivCheckStoke.isSelected = alarmBean.markType == AlarmBean.TYPE_ALARM_MARK_STROKE
        ivCheckMatrix.isSelected = alarmBean.markType == AlarmBean.TYPE_ALARM_MARK_MATRIX
        imgCAlarmHigh.load(ColorDrawable(alarmBean.highColor))
        imgCAlarmLow.load(ColorDrawable(alarmBean.lowColor))
        etAlarmHigh.isEnabled = switchAlarmHigh.isChecked
        etAlarmLow.isEnabled = switchAlarmLow.isChecked
        clAlarmMark.isVisible = isEdit || switchAlarmMark.isChecked
        clRingtoneSelect.isVisible = !isEdit && switchAlarmRingtone.isChecked
        tvAlarmRingtone.isVisible = !isEdit
        switchAlarmRingtone.isVisible = !isEdit
        if (hideAlarmMark) {
            tvAlarmMark.visibility = View.GONE
            switchAlarmMark.visibility = View.GONE
            clAlarmMark.visibility = View.GONE
        }
        switchAlarmMark.isVisible = !isEdit
        if (alarmBean.highTemp == Float.MAX_VALUE) {
            etAlarmHigh.setText("")
        } else {
            etAlarmHigh.setText(UnitTools.showUnitValue(alarmBean.highTemp).toString())
        }
        if (alarmBean.lowTemp == Float.MIN_VALUE) {
            etAlarmLow.setText("")
        } else {
            etAlarmLow.setText(UnitTools.showUnitValue(alarmBean.lowTemp).toString())
        }
        ivRingtone1.isSelected = false
        ivRingtone2.isSelected = false
        ivRingtone3.isSelected = false
        ivRingtone4.isSelected = false
        ivRingtone5.isSelected = false
        when (alarmBean.ringtoneType) {
            0 -> ivRingtone1.isSelected = true
            1 -> ivRingtone2.isSelected = true
            2 -> ivRingtone3.isSelected = true
            3 -> ivRingtone4.isSelected = true
            4 -> ivRingtone5.isSelected = true
        }
    }

    private fun save() {
        try {
            val inputHigh =
                if (switchAlarmHigh.isChecked) {
                    if (etAlarmHigh.text.isNotEmpty()) UnitTools.showToCValue(
                        etAlarmHigh.text.toString().toFloat()
                    ) else null
                } else {
                    null
                }
            val inputLow =
                if (switchAlarmLow.isChecked) {
                    if (etAlarmLow.text.isNotEmpty()) UnitTools.showToCValue(
                        etAlarmLow.text.toString().toFloat()
                    ) else null
                } else {
                    null
                }
            if (inputHigh != null && inputLow != null && inputLow > inputHigh) {
                ToastTools.showShort(R.string.tip_input_format)
                return
            }
        } catch (e: Exception) {
            ToastTools.showShort(R.string.tip_input_format)
            return
        }
        val inputHigh = if (etAlarmHigh.text.isNotEmpty()) etAlarmHigh.text.toString() else ""
        val inputLow = if (etAlarmLow.text.isNotEmpty()) etAlarmLow.text.toString() else ""
        var highValue: Float? = null
        var lowValue: Float? = null
        try {
            highValue =
                if (inputHigh.isNotEmpty()) UnitTools.showToCValue(inputHigh.toFloat()) else null
            lowValue =
                if (inputLow.isNotEmpty()) UnitTools.showToCValue(inputLow.toFloat()) else null
        } catch (_: Exception) {
        }
        alarmBean.highTemp = highValue ?: Float.MAX_VALUE
        alarmBean.lowTemp = lowValue ?: Float.MIN_VALUE
        alarmBean.isHighOpen = switchAlarmHigh.isChecked
        alarmBean.isLowOpen = switchAlarmLow.isChecked
        alarmBean.isRingtoneOpen = switchAlarmRingtone.isChecked
        onSaveListener?.invoke(alarmBean)
        dismiss()
    }

    private fun showColorDialog(isHigh: Boolean) {
        val colorPickDialog =
            ColorPickDialog(context, if (isHigh) alarmBean.highColor else alarmBean.lowColor, -1)
        colorPickDialog.onPickListener = { it: Int, i1: Int ->
            if (isHigh) {
                alarmBean.highColor = it
                imgCAlarmHigh.load(ColorDrawable(it))
            } else {
                alarmBean.lowColor = it
                imgCAlarmLow.load(ColorDrawable(it))
            }
        }
        colorPickDialog.show()
    }

    override fun dismiss() {
        super.dismiss()
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        when (buttonView?.id) {
            R.id.switch_alarm_high -> {
                etAlarmHigh.isEnabled = isChecked
                alarmBean.isHighOpen = isChecked
            }

            R.id.switch_alarm_low -> {
                etAlarmLow.isEnabled = isChecked
                alarmBean.isLowOpen = isChecked
            }

            R.id.switch_alarm_mark -> {
                clAlarmMark.isVisible = isChecked
                alarmBean.isMarkOpen = isChecked
            }

            R.id.switch_alarm_ringtone -> {
                clRingtoneSelect.isVisible = isChecked
                if (isChecked) {
                    selectRingtone(alarmBean.ringtoneType)
                } else {
                    selectRingtone(null)
                }
            }
        }
    }

    private fun selectRingtone(position: Int?) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            }
        } catch (_: Exception) {
        }
        if (position == null) {
            return
        }
        alarmBean.ringtoneType = position
        ivRingtone1.isSelected = false
        ivRingtone2.isSelected = false
        ivRingtone3.isSelected = false
        ivRingtone4.isSelected = false
        ivRingtone5.isSelected = false
        when (position) {
            0 -> ivRingtone1.isSelected = true
            1 -> ivRingtone2.isSelected = true
            2 -> ivRingtone3.isSelected = true
            3 -> ivRingtone4.isSelected = true
            4 -> ivRingtone5.isSelected = true
        }
        when (position) {
            0 -> mediaPlayer = MediaPlayer.create(context, R.raw.ringtone1)
            1 -> mediaPlayer = MediaPlayer.create(context, R.raw.ringtone2)
            2 -> mediaPlayer = MediaPlayer.create(context, R.raw.ringtone3)
            3 -> mediaPlayer = MediaPlayer.create(context, R.raw.ringtone4)
            4 -> mediaPlayer = MediaPlayer.create(context, R.raw.ringtone5)
        }
        mediaPlayer?.start()
    }
}


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\libunified_src_main_java_com_mpdc4gsr_libunified_app_comm_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\comm' subtree
// Files: 14; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\AlarmHelp.kt =====

package com.mpdc4gsr.libunified.app.comm

import android.content.Context
import android.media.MediaPlayer
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.app.bean.AlarmBean
import com.mpdc4gsr.libunified.app.comm.util.SingletonHolder
import com.mpdc4gsr.libunified.app.comm.view.TempLayout

class AlarmHelp private constructor(val context: Context) {
    companion object : SingletonHolder<AlarmHelp, Context>(::AlarmHelp)

    private var mediaPlayer: MediaPlayer? = null
    private var ringtoneResPosition = -1
    private var isOpenLowTemp = false
    private var isOpenHighTemp = false
    private var isTempAlarmRingtoneOpen = false
    private var maxTemp: Float = 0f
    private var minTemp: Float = 0f
    private var isPause = false
    private var alarmBean: AlarmBean? = null
    fun updateData(alarmBean: AlarmBean) {
        this.alarmBean = alarmBean
        isTempAlarmRingtoneOpen = alarmBean?.isRingtoneOpen ?: false
        isOpenLowTemp = alarmBean?.isLowOpen ?: false
        isOpenHighTemp = alarmBean?.isHighOpen ?: false
        ringtoneResPosition = alarmBean?.ringtoneType ?: -1
        maxTemp = alarmBean?.highTemp ?: Float.MAX_VALUE
        minTemp = alarmBean?.lowTemp ?: Float.MIN_VALUE
        if (isTempAlarmRingtoneOpen) {
            when (ringtoneResPosition) {
                0 -> mediaPlayer = MediaPlayer.create(context, R.raw.ringtone1)
                1 -> mediaPlayer = MediaPlayer.create(context, R.raw.ringtone2)
                2 -> mediaPlayer = MediaPlayer.create(context, R.raw.ringtone3)
                3 -> mediaPlayer = MediaPlayer.create(context, R.raw.ringtone4)
                4 -> mediaPlayer = MediaPlayer.create(context, R.raw.ringtone5)
            }
            mediaPlayer?.isLooping = true
        } else {
            mediaPlayer = null
        }
    }

    fun updateData(
        low: Float?,
        high: Float?,
        ringtone: Int?,
    ) {
        if (low == null) {
            isOpenLowTemp = false
        } else {
            isOpenLowTemp = true
            minTemp = low
        }
        if (high == null) {
            isOpenHighTemp = false
        } else {
            isOpenHighTemp = true
            maxTemp = high
        }
        if (ringtone == null) {
            isTempAlarmRingtoneOpen = false
            ringtoneResPosition = -1
            try {
                stopPlayer()
                mediaPlayer?.release()
                mediaPlayer = null
            } catch (_: Exception) {
            }
        } else {
            isTempAlarmRingtoneOpen = true
            try {
                stopPlayer()
                mediaPlayer?.release()
                mediaPlayer = null
            } catch (_: Exception) {
            }
            when (ringtone) {
                0 -> mediaPlayer = MediaPlayer.create(context, R.raw.ringtone1)
                1 -> mediaPlayer = MediaPlayer.create(context, R.raw.ringtone2)
                2 -> mediaPlayer = MediaPlayer.create(context, R.raw.ringtone3)
                3 -> mediaPlayer = MediaPlayer.create(context, R.raw.ringtone4)
                4 -> mediaPlayer = MediaPlayer.create(context, R.raw.ringtone5)
            }
            mediaPlayer?.isLooping = true
            ringtoneResPosition = ringtone
        }
    }

    fun alarmData(
        realMax: Float,
        realMin: Float,
        tempLayout: TempLayout?,
    ) {
        if (isOpenHighTemp && isOpenLowTemp) {
            if (realMax > maxTemp && realMin < minTemp) {
                tempLayout?.startAnimation(TempLayout.TYPE_A)
                startMediaPlayer()
            } else if (realMax > maxTemp) {
                tempLayout?.startAnimation(TempLayout.TYPE_HOT)
                startMediaPlayer()
            } else if (realMin < minTemp) {
                tempLayout?.startAnimation(TempLayout.TYPE_LT)
                startMediaPlayer()
            } else {
                tempLayout?.stopAnimation()
                stopPlayer()
            }
        } else if (isOpenHighTemp) {
            if (realMax > maxTemp) {
                tempLayout?.startAnimation(TempLayout.TYPE_HOT)
                startMediaPlayer()
            } else {
                tempLayout?.stopAnimation()
                stopPlayer()
            }
        } else if (isOpenLowTemp) {
            if (realMin < minTemp) {
                tempLayout?.startAnimation(TempLayout.TYPE_LT)
                startMediaPlayer()
            } else {
                tempLayout?.stopAnimation()
                stopPlayer()
            }
        } else {
            tempLayout?.stopAnimation()
            stopPlayer()
        }
    }

    private fun stopPlayer() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
        }
    }

    private fun startMediaPlayer() {
        if (mediaPlayer?.isPlaying != true && !isPause) {
            mediaPlayer?.seekTo(0)
            mediaPlayer?.start()
        }
    }

    fun onDestroy(isSaveSetting: Boolean) {
        if (!isSaveSetting) {
            isTempAlarmRingtoneOpen = false
            isOpenHighTemp = false
            isOpenLowTemp = false
        }
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
            mediaPlayer = null
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                isPause = true
            }
        }
    }

    fun onResume() {
        isPause = false
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\bean\SaveSettingBean.kt =====

package com.mpdc4gsr.libunified.app.comm.bean

import android.util.TypedValue
import com.mpdc4gsr.libunified.compat.ContextProvider
import com.mpdc4gsr.libunified.compat.SPUtils
import com.google.gson.Gson
import com.mpdc4gsr.libunified.app.bean.AlarmBean
import com.mpdc4gsr.libunified.app.bean.CameraItemBean
import com.mpdc4gsr.libunified.app.bean.ObserveBean
import com.mpdc4gsr.libunified.app.common.SaveSettingUtils
import com.mpdc4gsr.libunified.app.config.DeviceConfig
import com.mpdc4gsr.libunified.app.utils.CommUtils

class SaveSettingBean(private val isWifi: Boolean = false) {
    private fun getSPUtils(): SPUtils =
        SPUtils.getInstance(if (isWifi) "WifiSaveSettingUtils" else "SaveSettingUtils")

    var isSaveSetting: Boolean = getSPUtils().getBoolean("isSaveSetting", true)
        set(value) {
            field = value
            getSPUtils().put("isSaveSetting", value)
        }
    var isMeasureTempMode: Boolean =
        if (isSaveSetting) getSPUtils().getBoolean("isMeasureTempMode", true) else true
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("isMeasureTempMode", value)
            }
        }
    var isOpenAmplify: Boolean =
        if (isSaveSetting) getSPUtils().getBoolean("isOpenAmplify", false) else false
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("isOpenAmplify", value)
            }
        }
    var isVideoMode: Boolean =
        if (isSaveSetting) getSPUtils().getBoolean("isVideoMode", false) else false
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("isVideoMode", value)
            }
        }
    var isAutoShutter: Boolean =
        if (isSaveSetting) getSPUtils().getBoolean("isAutoShutter", true) else true
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("isAutoShutter", value)
            }
        }
    var isRecordAudio: Boolean =
        if (isSaveSetting) getSPUtils().getBoolean("isRecordAudio", false) else false
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("isRecordAudio", value)
            }
        }
    var delayCaptureSecond: Int =
        if (isSaveSetting) getSPUtils().getInt("delayCaptureSecond", 0) else 0
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("delayCaptureSecond", value)
            }
        }
    var fusionType: Int =
        if (isSaveSetting) {
            getSPUtils().getInt(
                "fusionType",
                SaveSettingUtils.FusionTypeLPYFusion,
            )
        } else {
            SaveSettingUtils.FusionTypeLPYFusion
        }
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("fusionType", value)
            }
        }
    var isOpenTwoLight: Boolean =
        if (isSaveSetting) getSPUtils().getBoolean("isOpenTwoLight", false) else false
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("isOpenTwoLight", value)
            }
        }
    var twoLightAlpha: Int = if (isSaveSetting) getSPUtils().getInt("twoLightAlpha", 50) else 50
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("twoLightAlpha", value)
            }
        }
    var pseudoColorMode: Int = if (isSaveSetting) getSPUtils().getInt("pseudoColorMode", 3) else 3
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("pseudoColorMode", value)
            }
        }
    var isOpenPseudoBar: Boolean =
        if (isSaveSetting) getSPUtils().getBoolean("isOpenPseudoBar", true) else true
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("isOpenPseudoBar", value)
            }
        }
    var contrastValue: Int = if (isSaveSetting) getSPUtils().getInt("contrastValue", 128) else 128
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("contrastValue", value)
            }
        }
    var ddeConfig: Int = if (isSaveSetting) getSPUtils().getInt("ddeConfig", 2) else 2
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("ddeConfig", value)
            }
        }
    var alarmBean: AlarmBean =
        if (isSaveSetting) {
            val json = getSPUtils().getString("alarmBean", "")
            if (json.isNullOrEmpty()) AlarmBean() else Gson().fromJson(json, AlarmBean::class.java)
        } else {
            AlarmBean()
        }
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("alarmBean", Gson().toJson(value))
            }
        }
    var rotateAngle: Int =
        if (isSaveSetting) {
            getSPUtils().getInt(
                "rotateAngle",
                DeviceConfig.S_ROTATE_ANGLE,
            )
        } else {
            DeviceConfig.S_ROTATE_ANGLE
        }
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("rotateAngle", value)
            }
        }

    fun isRotatePortrait(): Boolean = rotateAngle == 90 || rotateAngle == 270
    var isOpenMirror: Boolean =
        if (isSaveSetting) getSPUtils().getBoolean("isOpenMirror", false) else false
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("isOpenMirror", value)
            }
        }
    var isOpenCompass: Boolean =
        if (isSaveSetting) getSPUtils().getBoolean("isOpenCompass", false) else false
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("isOpenCompass", value)
            }
        }
    var tempTextColor: Int = if (isSaveSetting) getSPUtils().getInt(
        "tempTextColor",
        0xffffffff.toInt()
    ) else 0xffffffff.toInt()
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("tempTextColor", value)
            }
        }
    var tempTextSize: Int = run {
        val context = ContextProvider.getContext()
        val defaultSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            14f,
            context.resources.displayMetrics
        ).toInt()
        if (isSaveSetting) getSPUtils().getInt("tempTextSize", defaultSize) else defaultSize
    }
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("tempTextSize", value)
            }
        }

    fun isTempTextDefault(): Boolean {
        val context = ContextProvider.getContext()
        val defaultSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            14f,
            context.resources.displayMetrics
        ).toInt()
        return tempTextColor == 0xffffffff.toInt() && tempTextSize == defaultSize
    }

    var temperatureMode: Int =
        if (isSaveSetting) {
            getSPUtils().getInt(
                "temperatureMode",
                CameraItemBean.TYPE_TMP_C,
            )
        } else {
            CameraItemBean.TYPE_TMP_C
        }
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("temperatureMode", value)
            }
        }
    var isOpenHighPoint: Boolean =
        if (isSaveSetting) getSPUtils().getBoolean("isOpenHighPoint", false) else false
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("isOpenHighPoint", value)
            }
        }
    var isOpenLowPoint: Boolean =
        if (isSaveSetting) getSPUtils().getBoolean("isOpenLowPoint", false) else false
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("isOpenLowPoint", value)
            }
        }
    var aiTraceType: Int = if (isSaveSetting) getSPUtils().getInt(
        "aiTraceType",
        ObserveBean.TYPE_NONE
    ) else ObserveBean.TYPE_NONE
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("aiTraceType", value)
            }
        }
    var isOpenTarget: Boolean =
        if (isSaveSetting) getSPUtils().getBoolean("isOpenTarget", false) else false
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("isOpenTarget", value)
            }
        }
    var targetMeasureMode: Int =
        if (isSaveSetting) {
            getSPUtils().getInt(
                "targetMeasureMode",
                ObserveBean.TYPE_MEASURE_PERSON,
            )
        } else {
            ObserveBean.TYPE_MEASURE_PERSON
        }
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("targetMeasureMode", value)
            }
        }
    var targetType: Int =
        if (isSaveSetting) {
            getSPUtils().getInt(
                "targetType",
                ObserveBean.TYPE_TARGET_HORIZONTAL,
            )
        } else {
            ObserveBean.TYPE_TARGET_HORIZONTAL
        }
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("targetType", value)
            }
        }
    var targetColorType: Int =
        if (isSaveSetting) {
            getSPUtils().getInt(
                "targetColorType",
                ObserveBean.TYPE_TARGET_COLOR_GREEN,
            )
        } else {
            ObserveBean.TYPE_TARGET_COLOR_GREEN
        }
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("targetColorType", value)
            }
        }
    var reportAuthorName: String =
        if (isSaveSetting) {
            getSPUtils().getString(
                "reportAuthorName",
                CommUtils.getAppName(),
            )
        } else {
            CommUtils.getAppName()
        }
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("reportAuthorName", value)
            }
        }
    var reportWatermarkText: String =
        if (isSaveSetting) {
            getSPUtils().getString(
                "reportWatermarkText",
                CommUtils.getAppName(),
            )
        } else {
            CommUtils.getAppName()
        }
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("reportWatermarkText", value)
            }
        }
    var reportHumidity: Int = if (isSaveSetting) getSPUtils().getInt("reportHumidity", 500) else 500
        set(value) {
            field = value
            if (isSaveSetting) {
                getSPUtils().put("reportHumidity", value)
            }
        }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\dialog\ColorPickDialog.kt =====

package com.mpdc4gsr.libunified.app.comm.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.mpdc4gsr.libunified.compat.dpToPx
import com.mpdc4gsr.libunified.compat.spToPx
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.app.utils.ColorUtils
import com.mpdc4gsr.libunified.app.utils.ScreenUtils
import com.mpdc4gsr.libunified.app.view.ColorSelectView
import com.mpdc4gsr.libunified.ui.widget.seekbar.OnRangeChangedListener
import com.mpdc4gsr.libunified.ui.widget.seekbar.RangeSeekBar

class ColorPickDialog(
    context: Context,
    @ColorInt private var color: Int,
    var textSize: Int,
    var textSizeIsDP: Boolean = false,
) : Dialog(context, R.style.InfoDialog), View.OnClickListener {
    var onPickListener: ((color: Int, textSize: Int) -> Unit)? = null
    private val rootView: View =
        LayoutInflater.from(context).inflate(R.layout.dialog_color_pick, null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(true)
        setCanceledOnTouchOutside(true)
        setContentView(rootView)
        window?.let {
            val layoutParams = it.attributes
            layoutParams.width = (ScreenUtils.getScreenWidth(context) * 0.9).toInt()
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            it.attributes = layoutParams
        }
        val activeTrackColor =
            ColorUtils.setColorAlpha(
                ContextCompat.getColor(context, R.color.we_read_theme_color),
                0.1f
            )
        val iconTintColor =
            ColorUtils.setColorAlpha(
                ContextCompat.getColor(context, R.color.we_read_theme_color),
                0.7f
            )
        when (color) {
            0xff0000ff.toInt() -> rootView.findViewById<View>(R.id.view_color1).isSelected = true
            0xffff0000.toInt() -> rootView.findViewById<View>(R.id.view_color2).isSelected = true
            0xff00ff00.toInt() -> rootView.findViewById<View>(R.id.view_color3).isSelected = true
            0xffffff00.toInt() -> rootView.findViewById<View>(R.id.view_color4).isSelected = true
            0xff000000.toInt() -> rootView.findViewById<View>(R.id.view_color5).isSelected = true
            0xffffffff.toInt() -> rootView.findViewById<View>(R.id.view_color6).isSelected = true
            else -> rootView.findViewById<ColorSelectView>(R.id.color_select_view)
                .selectColor(color)
        }
        rootView.findViewById<ColorSelectView>(R.id.color_select_view).onSelectListener = {
            unSelect6Color()
            color = it
        }
        if (textSize != -1) {
            findViewById<TextView>(R.id.tv_size_title).visibility = View.VISIBLE
            findViewById<TextView>(R.id.tv_size_value).visibility = View.VISIBLE
            findViewById<TextView>(R.id.tv_nifty_left).visibility = View.VISIBLE
            findViewById<TextView>(R.id.tv_nifty_right).visibility = View.VISIBLE
            findViewById<RangeSeekBar>(R.id.nifty_slider_view).visibility = View.VISIBLE
            findViewById<RangeSeekBar>(R.id.nifty_slider_view).setOnRangeChangedListener(
                object : OnRangeChangedListener {
                    override fun onRangeChanged(
                        view: RangeSeekBar,
                        leftValue: Float,
                        rightValue: Float,
                        isFromUser: Boolean,
                        tempMode: Int
                    ) {
                        var text = ""
                        text =
                            if (leftValue <= 0) {
                                textSize = 14
                                context.getString(R.string.temp_text_standard)
                            } else if (leftValue <= 50) {
                                textSize = 16
                                context.getString(R.string.temp_text_big)
                            } else {
                                textSize = 18
                                context.getString(R.string.temp_text_sup_big)
                            }
                        findViewById<TextView>(R.id.tv_size_value).text = text
                    }

                    override fun onStartTrackingTouch(
                        view: RangeSeekBar,
                        isLeft: Boolean,
                    ) {
                    }

                    override fun onStopTrackingTouch(
                        view: RangeSeekBar,
                        isLeft: Boolean,
                    ) {
                    }
                },
            )
            findViewById<RangeSeekBar>(R.id.nifty_slider_view).setProgress(
                textSizeToNifyValue(
                    textSize
                )
            )
        } else {
            findViewById<RangeSeekBar>(R.id.nifty_slider_view).visibility = View.GONE
        }
        rootView.findViewById<View>(R.id.view_color1).setOnClickListener(this)
        rootView.findViewById<View>(R.id.view_color2).setOnClickListener(this)
        rootView.findViewById<View>(R.id.view_color3).setOnClickListener(this)
        rootView.findViewById<View>(R.id.view_color4).setOnClickListener(this)
        rootView.findViewById<View>(R.id.view_color5).setOnClickListener(this)
        rootView.findViewById<View>(R.id.view_color6).setOnClickListener(this)
        rootView.findViewById<View>(R.id.rl_close).setOnClickListener(this)
        rootView.findViewById<View>(R.id.tv_save).setOnClickListener(this)
    }

    private fun textSizeToNifyValue(
        size: Int,
        // isTC007 parameter removed - TC007 functionality disabled
    ): Float {
        // Always use default behavior, TC007 functionality removed
        return when (size) {
            14f.spToPx(context).toInt() -> 0f
            16f.spToPx(context).toInt() -> 50f
            else -> 100f
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            rootView.findViewById<View>(R.id.rl_close) -> dismiss()
            rootView.findViewById<View>(R.id.tv_save) -> {
                dismiss()
                onPickListener?.invoke(color, textSize)
            }

            rootView.findViewById<View>(R.id.view_color1) -> {
                unSelect6Color()
                rootView.findViewById<ColorSelectView>(R.id.color_select_view).reset()
                rootView.findViewById<View>(R.id.view_color1).isSelected = true
                color = 0xff0000ff.toInt()
            }

            rootView.findViewById<View>(R.id.view_color2) -> {
                unSelect6Color()
                rootView.findViewById<ColorSelectView>(R.id.color_select_view).reset()
                rootView.findViewById<View>(R.id.view_color2).isSelected = true
                color = 0xffff0000.toInt()
            }

            rootView.findViewById<View>(R.id.view_color3) -> {
                unSelect6Color()
                rootView.findViewById<ColorSelectView>(R.id.color_select_view).reset()
                rootView.findViewById<View>(R.id.view_color3).isSelected = true
                color = 0xff00ff00.toInt()
            }

            rootView.findViewById<View>(R.id.view_color4) -> {
                unSelect6Color()
                rootView.findViewById<ColorSelectView>(R.id.color_select_view).reset()
                rootView.findViewById<View>(R.id.view_color4).isSelected = true
                color = 0xffffff00.toInt()
            }

            rootView.findViewById<View>(R.id.view_color5) -> {
                unSelect6Color()
                rootView.findViewById<ColorSelectView>(R.id.color_select_view).reset()
                rootView.findViewById<View>(R.id.view_color5).isSelected = true
                color = 0xff000000.toInt()
            }

            rootView.findViewById<View>(R.id.view_color6) -> {
                unSelect6Color()
                rootView.findViewById<ColorSelectView>(R.id.color_select_view).reset()
                rootView.findViewById<View>(R.id.view_color6).isSelected = true
                color = 0xffffffff.toInt()
            }
        }
    }

    private fun unSelect6Color() {
        rootView.findViewById<View>(R.id.view_color1).isSelected = false
        rootView.findViewById<View>(R.id.view_color2).isSelected = false
        rootView.findViewById<View>(R.id.view_color3).isSelected = false
        rootView.findViewById<View>(R.id.view_color4).isSelected = false
        rootView.findViewById<View>(R.id.view_color5).isSelected = false
        rootView.findViewById<View>(R.id.view_color6).isSelected = false
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\dialog\TempAlarmSetDialog.kt =====

package com.mpdc4gsr.libunified.app.comm.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import coil.load
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.app.bean.AlarmBean
import com.mpdc4gsr.libunified.app.tools.ToastTools
import com.mpdc4gsr.libunified.app.tools.UnitTools

class TempAlarmSetDialog(
    context: Context,
    private val isEdit: Boolean,
) : Dialog(context, R.style.app_compat_dialog), CompoundButton.OnCheckedChangeListener {
    var alarmBean = AlarmBean()
        set(value) {
            field = value.copy()
        }
    var onSaveListener: ((alarmBean: AlarmBean) -> Unit)? = null
    private var mediaPlayer: MediaPlayer? = null
    public var hideAlarmMark = false
    private lateinit var clRoot: ConstraintLayout
    private lateinit var clClose: ConstraintLayout
    private lateinit var tvSave: TextView
    private lateinit var ivRingtone1: ImageView
    private lateinit var ivRingtone2: ImageView
    private lateinit var ivRingtone3: ImageView
    private lateinit var ivRingtone4: ImageView
    private lateinit var ivRingtone5: ImageView
    private lateinit var switchAlarmHigh: SwitchCompat
    private lateinit var switchAlarmLow: SwitchCompat
    private lateinit var switchAlarmMark: SwitchCompat
    private lateinit var switchAlarmRingtone: SwitchCompat
    private lateinit var imgMarkHigh: ImageView
    private lateinit var imgMarkLow: ImageView
    private lateinit var ivCheckStoke: ImageView
    private lateinit var ivCheckMatrix: ImageView
    private lateinit var tvAlarmHighUnit: TextView
    private lateinit var tvAlarmLowUnit: TextView
    private lateinit var etAlarmHigh: EditText
    private lateinit var etAlarmLow: EditText
    private lateinit var imgCAlarmHigh: ImageView
    private lateinit var imgCAlarmLow: ImageView
    private lateinit var clAlarmMark: ConstraintLayout
    private lateinit var clRingtoneSelect: ConstraintLayout
    private lateinit var tvAlarmRingtone: TextView
    private lateinit var tvAlarmMark: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        setContentView(LayoutInflater.from(context).inflate(R.layout.dialog_temp_alarm_set, null))
        initView()
        window?.let {
            val layoutParams = it.attributes
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            it.attributes = layoutParams
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        dismiss()
    }

    private fun initView() {
        clRoot = findViewById(R.id.cl_root)
        clClose = findViewById(R.id.cl_close)
        tvSave = findViewById(R.id.tv_save)
        ivRingtone1 = findViewById(R.id.iv_ringtone1)
        ivRingtone2 = findViewById(R.id.iv_ringtone2)
        ivRingtone3 = findViewById(R.id.iv_ringtone3)
        ivRingtone4 = findViewById(R.id.iv_ringtone4)
        ivRingtone5 = findViewById(R.id.iv_ringtone5)
        switchAlarmHigh = findViewById(R.id.switch_alarm_high)
        switchAlarmLow = findViewById(R.id.switch_alarm_low)
        switchAlarmMark = findViewById(R.id.switch_alarm_mark)
        switchAlarmRingtone = findViewById(R.id.switch_alarm_ringtone)
        imgMarkHigh = findViewById(R.id.img_mark_high)
        imgMarkLow = findViewById(R.id.img_mark_low)
        ivCheckStoke = findViewById(R.id.iv_check_stoke)
        ivCheckMatrix = findViewById(R.id.iv_check_matrix)
        tvAlarmHighUnit = findViewById(R.id.tv_alarm_high_unit)
        tvAlarmLowUnit = findViewById(R.id.tv_alarm_low_unit)
        etAlarmHigh = findViewById(R.id.et_alarm_high)
        etAlarmLow = findViewById(R.id.et_alarm_low)
        imgCAlarmHigh = findViewById(R.id.img_c_alarm_high)
        imgCAlarmLow = findViewById(R.id.img_c_alarm_low)
        clAlarmMark = findViewById(R.id.cl_alarm_mark)
        clRingtoneSelect = findViewById(R.id.cl_ringtone_select)
        tvAlarmRingtone = findViewById(R.id.tv_alarm_ringtone)
        tvAlarmMark = findViewById(R.id.tv_alarm_mark)
        clRoot.setOnClickListener { dismiss() }
        clClose.setOnClickListener { dismiss() }
        tvSave.setOnClickListener { save() }
        ivRingtone1.setOnClickListener { selectRingtone(0) }
        ivRingtone2.setOnClickListener { selectRingtone(1) }
        ivRingtone3.setOnClickListener { selectRingtone(2) }
        ivRingtone4.setOnClickListener { selectRingtone(3) }
        ivRingtone5.setOnClickListener { selectRingtone(4) }
        switchAlarmHigh.setOnCheckedChangeListener(this)
        switchAlarmLow.setOnCheckedChangeListener(this)
        switchAlarmMark.setOnCheckedChangeListener(this)
        switchAlarmRingtone.setOnCheckedChangeListener(this)
        imgMarkHigh.setOnClickListener {
            showColorDialog(true)
        }
        imgMarkLow.setOnClickListener {
            showColorDialog(false)
        }
        ivCheckStoke.setOnClickListener {
            if (!ivCheckStoke.isSelected) {
                ivCheckStoke.isSelected = true
                ivCheckMatrix.isSelected = false
                alarmBean.markType = AlarmBean.TYPE_ALARM_MARK_STROKE
            }
        }
        ivCheckMatrix.setOnClickListener {
            if (!ivCheckMatrix.isSelected) {
                ivCheckStoke.isSelected = false
                ivCheckMatrix.isSelected = true
                alarmBean.markType = AlarmBean.TYPE_ALARM_MARK_MATRIX
            }
        }
        tvAlarmHighUnit.text = UnitTools.showUnit()
        tvAlarmLowUnit.text = UnitTools.showUnit()
    }

    override fun show() {
        super.show()
        refreshAlarmView()
    }

    private fun refreshAlarmView() {
        switchAlarmHigh.isChecked = alarmBean.isHighOpen
        switchAlarmLow.isChecked = alarmBean.isLowOpen
        switchAlarmMark.isChecked = isEdit || alarmBean.isMarkOpen
        if (!isEdit) {
            switchAlarmRingtone.isChecked = alarmBean.isRingtoneOpen
        }
        ivCheckStoke.isSelected = alarmBean.markType == AlarmBean.TYPE_ALARM_MARK_STROKE
        ivCheckMatrix.isSelected = alarmBean.markType == AlarmBean.TYPE_ALARM_MARK_MATRIX
        imgCAlarmHigh.load(ColorDrawable(alarmBean.highColor))
        imgCAlarmLow.load(ColorDrawable(alarmBean.lowColor))
        etAlarmHigh.isEnabled = switchAlarmHigh.isChecked
        etAlarmLow.isEnabled = switchAlarmLow.isChecked
        clAlarmMark.isVisible = isEdit || switchAlarmMark.isChecked
        clRingtoneSelect.isVisible = !isEdit && switchAlarmRingtone.isChecked
        tvAlarmRingtone.isVisible = !isEdit
        switchAlarmRingtone.isVisible = !isEdit
        if (hideAlarmMark) {
            tvAlarmMark.visibility = View.GONE
            switchAlarmMark.visibility = View.GONE
            clAlarmMark.visibility = View.GONE
        }
        switchAlarmMark.isVisible = !isEdit
        if (alarmBean.highTemp == Float.MAX_VALUE) {
            etAlarmHigh.setText("")
        } else {
            etAlarmHigh.setText(UnitTools.showUnitValue(alarmBean.highTemp).toString())
        }
        if (alarmBean.lowTemp == Float.MIN_VALUE) {
            etAlarmLow.setText("")
        } else {
            etAlarmLow.setText(UnitTools.showUnitValue(alarmBean.lowTemp).toString())
        }
        ivRingtone1.isSelected = false
        ivRingtone2.isSelected = false
        ivRingtone3.isSelected = false
        ivRingtone4.isSelected = false
        ivRingtone5.isSelected = false
        when (alarmBean.ringtoneType) {
            0 -> ivRingtone1.isSelected = true
            1 -> ivRingtone2.isSelected = true
            2 -> ivRingtone3.isSelected = true
            3 -> ivRingtone4.isSelected = true
            4 -> ivRingtone5.isSelected = true
        }
    }

    private fun save() {
        try {
            val inputHigh =
                if (switchAlarmHigh.isChecked) {
                    if (etAlarmHigh.text.isNotEmpty()) UnitTools.showToCValue(
                        etAlarmHigh.text.toString().toFloat()
                    ) else null
                } else {
                    null
                }
            val inputLow =
                if (switchAlarmLow.isChecked) {
                    if (etAlarmLow.text.isNotEmpty()) UnitTools.showToCValue(
                        etAlarmLow.text.toString().toFloat()
                    ) else null
                } else {
                    null
                }
            if (inputHigh != null && inputLow != null && inputLow > inputHigh) {
                ToastTools.showShort(R.string.tip_input_format)
                return
            }
        } catch (e: Exception) {
            ToastTools.showShort(R.string.tip_input_format)
            return
        }
        val inputHigh = if (etAlarmHigh.text.isNotEmpty()) etAlarmHigh.text.toString() else ""
        val inputLow = if (etAlarmLow.text.isNotEmpty()) etAlarmLow.text.toString() else ""
        var highValue: Float? = null
        var lowValue: Float? = null
        try {
            highValue =
                if (inputHigh.isNotEmpty()) UnitTools.showToCValue(inputHigh.toFloat()) else null
            lowValue =
                if (inputLow.isNotEmpty()) UnitTools.showToCValue(inputLow.toFloat()) else null
        } catch (_: Exception) {
        }
        alarmBean.highTemp = highValue ?: Float.MAX_VALUE
        alarmBean.lowTemp = lowValue ?: Float.MIN_VALUE
        alarmBean.isHighOpen = switchAlarmHigh.isChecked
        alarmBean.isLowOpen = switchAlarmLow.isChecked
        alarmBean.isRingtoneOpen = switchAlarmRingtone.isChecked
        onSaveListener?.invoke(alarmBean)
        dismiss()
    }

    private fun showColorDialog(isHigh: Boolean) {
        val colorPickDialog =
            ColorPickDialog(context, if (isHigh) alarmBean.highColor else alarmBean.lowColor, -1)
        colorPickDialog.onPickListener = { it: Int, i1: Int ->
            if (isHigh) {
                alarmBean.highColor = it
                imgCAlarmHigh.load(ColorDrawable(it))
            } else {
                alarmBean.lowColor = it
                imgCAlarmLow.load(ColorDrawable(it))
            }
        }
        colorPickDialog.show()
    }

    override fun dismiss() {
        super.dismiss()
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        when (buttonView?.id) {
            R.id.switch_alarm_high -> {
                etAlarmHigh.isEnabled = isChecked
                alarmBean.isHighOpen = isChecked
            }

            R.id.switch_alarm_low -> {
                etAlarmLow.isEnabled = isChecked
                alarmBean.isLowOpen = isChecked
            }

            R.id.switch_alarm_mark -> {
                clAlarmMark.isVisible = isChecked
                alarmBean.isMarkOpen = isChecked
            }

            R.id.switch_alarm_ringtone -> {
                clRingtoneSelect.isVisible = isChecked
                if (isChecked) {
                    selectRingtone(alarmBean.ringtoneType)
                } else {
                    selectRingtone(null)
                }
            }
        }
    }

    private fun selectRingtone(position: Int?) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            }
        } catch (_: Exception) {
        }
        if (position == null) {
            return
        }
        alarmBean.ringtoneType = position
        ivRingtone1.isSelected = false
        ivRingtone2.isSelected = false
        ivRingtone3.isSelected = false
        ivRingtone4.isSelected = false
        ivRingtone5.isSelected = false
        when (position) {
            0 -> ivRingtone1.isSelected = true
            1 -> ivRingtone2.isSelected = true
            2 -> ivRingtone3.isSelected = true
            3 -> ivRingtone4.isSelected = true
            4 -> ivRingtone5.isSelected = true
        }
        when (position) {
            0 -> mediaPlayer = MediaPlayer.create(context, R.raw.ringtone1)
            1 -> mediaPlayer = MediaPlayer.create(context, R.raw.ringtone2)
            2 -> mediaPlayer = MediaPlayer.create(context, R.raw.ringtone3)
            3 -> mediaPlayer = MediaPlayer.create(context, R.raw.ringtone4)
            4 -> mediaPlayer = MediaPlayer.create(context, R.raw.ringtone5)
        }
        mediaPlayer?.start()
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\IrParam.kt =====

package com.mpdc4gsr.libunified.app.comm

enum class IrParam {
    ParamLevel,
    ParamAlarm,
    ParamSharpness,
    ParamTempFont,
    ParamRotate,
    ParamColor,
    ParamMirror,
    ParamCompass,
    ParamPColor,
    ParamTemperature,
}

data class TempFont(val textSize: Int, val textColor: Int)


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\PDFHelp.kt =====

package com.mpdc4gsr.libunified.app.comm

import android.content.ContentValues
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfDocument.PageInfo
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ScrollView
import androidx.core.content.ContextCompat
import com.mpdc4gsr.libunified.compat.ContextProvider
import androidx.documentfile.provider.DocumentFile
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.app.config.FileConfig
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

object PDFHelp {
    fun savePdfFileByListView(
        name: String,
        view: ScrollView,
        viewList: MutableList<View>,
        watermarkView: View,
    ): String {
        val onePageHeight: Int = (view.width * 297f / 210f).toInt()
        var onePageContentHeight = 0f
        val pdfDocument = PdfDocument()
        var page: PdfDocument.Page? = null
        var canvas: Canvas? = null
        val paint = Paint()
        paint.color = 0xff16131e.toInt()
        for (index in 0 until viewList.size) {
            val contentHeight = viewList[index].measuredHeight
            if (onePageContentHeight + contentHeight > onePageHeight) {
                onePageContentHeight = 0f
                pdfDocument.finishPage(page)
                page = null
            }
            if (page == null) {
                val pageInfo =
                    PageInfo.Builder(view.width, onePageHeight, 1)
                        .setContentRect(Rect(0, 0, view.width, onePageHeight))
                        .create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                canvas.drawRect(0f, 0f, view.width.toFloat(), onePageHeight.toFloat(), paint)
                if (index == 0) {
                    val bgTopDrawable: Drawable? =
                        ContextCompat.getDrawable(view.context, R.drawable.ic_report_create_bg_top)
                    bgTopDrawable?.setBounds(0, 0, view.width, (view.width * 1026 / 1125f).toInt())
                    bgTopDrawable?.draw(canvas)
                }
                canvas.save()
                watermarkView.draw(canvas)
                canvas.restore()
            }
            canvas?.save()
            canvas?.translate((view.width - viewList[index].measuredWidth) / 2f, 0f)
            viewList[index].draw(canvas!!)
            canvas?.restore()
            canvas?.translate(0f, contentHeight.toFloat())
            onePageContentHeight += contentHeight
            if (page != null && index == viewList.size - 1) {
                pdfDocument.finishPage(page)
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val pdfFile = File(FileConfig.getPdfDir(), "$name.pdf")
            val fos = FileOutputStream(pdfFile)
            pdfDocument.writeTo(fos)
            fos.flush()
            fos.close()
            return pdfFile.absolutePath
        } else {
            val fileName = "$name.pdf"
            val values = ContentValues()
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            values.put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                FileConfig.getPdfDir(),
            )
            val contentUri = MediaStore.Files.getContentUri("external")
            val uri = ContextProvider.getContext().contentResolver.insert(contentUri, values)
            return if (uri != null) {
                val outputStream = ContextProvider.getContext().contentResolver.openOutputStream(uri)
                if (outputStream != null) {
                    pdfDocument.writeTo(outputStream)
                    outputStream.flush()
                    outputStream.close()
                }
                val documentFile = DocumentFile.fromSingleUri(ContextProvider.getContext(), uri)
                val filePath = uri.toString()
                Log.w("[ph][ph]", filePath)
                filePath
            } else {
                ""
            }
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\util\SingletonHolder.kt =====

package com.mpdc4gsr.libunified.app.comm.util

open class SingletonHolder<out T, in A>(private val creator: (A) -> T) {
    @Volatile
    private var instance: T? = null
    fun getInstance(arg: A): T {
        // First check without synchronization for performance
        return instance ?: synchronized(this) {
            // Second check with synchronization to ensure thread safety
            instance ?: creator(arg).also { instance = it }
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\view\CommLoadMoreView.kt =====

package com.mpdc4gsr.libunified.app.comm.view

import android.view.View
import android.view.ViewGroup
import com.chad.library.adapter.base.loadmore.BaseLoadMoreView
import com.chad.library.adapter.base.util.getItemView
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.mpdc4gsr.libunified.R

class CommLoadMoreView : BaseLoadMoreView() {
    override fun getRootView(parent: ViewGroup): View =
        parent.getItemView(R.layout.layout_load_more_view)

    override fun getLoadingView(holder: BaseViewHolder): View =
        holder.getView(R.id.load_more_loading_view)

    override fun getLoadComplete(holder: BaseViewHolder): View =
        holder.getView(R.id.load_more_load_complete_view)

    override fun getLoadEndView(holder: BaseViewHolder): View =
        holder.getView(R.id.load_more_load_end_view)

    override fun getLoadFailView(holder: BaseViewHolder): View =
        holder.getView(R.id.load_more_load_fail_view)
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\view\TempLayout.kt =====

package com.mpdc4gsr.libunified.app.comm.view

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.mpdc4gsr.libunified.R

class TempLayout : LinearLayout {
    companion object {
        val TYPE_HOT = 1
        val TYPE_LT = 2
        val TYPE_A = 3
    }

    private var alphaAnimator: ObjectAnimator? = null
    var rootV: View? = null
    var bg: View? = null
    var isHot: Boolean = true
    var type = -1

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView()
    }

    var animatorAlpha = 1f
    private fun initView() {
        rootV = LayoutInflater.from(context).inflate(R.layout.layout_temp_bg, this)
        bg = rootV?.findViewById(R.id.bg)
        alphaAnimator = ObjectAnimator.ofFloat(this, "alpha", 0f, 1f)
        alphaAnimator?.duration = 500
        alphaAnimator?.interpolator =
            BreatheInterpolator()
        alphaAnimator?.addUpdateListener {
            animatorAlpha = it.getAnimatedValue("alpha") as Float
        }
        alphaAnimator?.repeatCount = ValueAnimator.INFINITE
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    )

    fun startAnimation(type: Int) {
        this.visibility = View.VISIBLE
        if (this.type != type) {
            alphaAnimator?.cancel()
            alphaAnimator?.removeAllListeners()
            when (type) {
                TYPE_HOT -> {
                    isHot = true
                    alphaAnimator?.repeatCount = ValueAnimator.INFINITE
                    bg?.setBackgroundResource(R.drawable.ic_ir_read_bg)
                }

                TYPE_A -> {
                    alphaAnimator?.repeatCount = 0
                    alphaAnimator?.addListener(animatorListener)
                }

                else -> {
                    alphaAnimator?.repeatCount = ValueAnimator.INFINITE
                    isHot = false
                    bg?.setBackgroundResource(R.drawable.ic_ir_blue_bg)
                }
            }
            if (isAttachedToWindow) {
                try {
                    alphaAnimator?.start()
                } catch (e: IllegalStateException) {
                    Log.w("TempLayout", "Failed to start animator: ${e.message}")
                }
            }
            this.type = type
        }
    }

    var animatorListener: Animator.AnimatorListener =
        object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
            }

            override fun onAnimationEnd(animation: Animator) {
                if (this@TempLayout.visibility == View.VISIBLE && isAttachedToWindow) {
                    isHot = !isHot
                    if (isHot) {
                        bg?.setBackgroundResource(R.drawable.ic_ir_read_bg)
                    } else {
                        bg?.setBackgroundResource(R.drawable.ic_ir_blue_bg)
                    }
                    try {
                        alphaAnimator?.start()
                    } catch (e: IllegalStateException) {
                        Log.w("TempLayout", "Failed to restart animator in onAnimationEnd: ${e.message}")
                    }
                }
            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        }

    fun stopAnimation() {
        this.type = -1
        alphaAnimator?.removeAllListeners()
        this.visibility = View.GONE
        alphaAnimator?.cancel()
    }

    fun startAlphaBreathAnimation() {
        if (isAttachedToWindow) {
            try {
                alphaAnimator?.start()
            } catch (e: IllegalStateException) {
                Log.w("TempLayout", "Failed to start breath animation: ${e.message}")
            }
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\common\ProductType.kt =====

package com.mpdc4gsr.libunified.app.common

object ProductType {
    const val PRODUCT_NAME_TC = "TC001"
    const val PRODUCT_NAME_TS = "TS001"
    const val PRODUCT_NAME_TCP = "TC_PLUS"

    // PRODUCT_NAME_TC007 removed
    // PRODUCT_NAME_TS004 removed
    const val PRODUCT_NAME_TC007 = "TC007"  // Re-added for compatibility
    const val PRODUCT_NAME_TC001LITE = "TCLite"
    const val PRODUCT_NAME_TC002C_DUO = "TC002C_DUO"
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\common\SaveSettingUtils.kt =====

package com.mpdc4gsr.libunified.app.common

import android.util.TypedValue
import com.mpdc4gsr.libunified.compat.SPUtils
import com.google.gson.Gson
import com.mpdc4gsr.libunified.compat.ContextProvider
import com.mpdc4gsr.libunified.app.bean.AlarmBean
import com.mpdc4gsr.libunified.app.bean.CameraItemBean
import com.mpdc4gsr.libunified.app.bean.ObserveBean
import com.mpdc4gsr.libunified.app.config.DeviceConfig
import com.mpdc4gsr.libunified.app.utils.CommUtils

object SaveSettingUtils {
    private const val SP_NAME = "SaveSettingUtils"
    const val FusionTypeLPYFusion = 4
    const val FusionTypeMeanFusion = 2
    const val FusionTypeIROnly = 1
    const val FusionTypeVLOnly = 0

    // FusionTypeTC007Fusion constant removed - TC007 device support discontinued
    const val FusionTypeHSLFusion = 3
    const val FusionTypeScreenFusion = 5
    const val FusionTypeIROnlyNoFusion = 6
    fun reset() {
        isMeasureTempMode = true
        isVideoMode = false
        isAutoShutter = true
        isRecordAudio = false
        isOpenMirror = false
        delayCaptureSecond = 0
        contrastValue = 128
        pseudoColorMode = 3
        rotateAngle = DeviceConfig.S_ROTATE_ANGLE
        isOpenPseudoBar = true
        isOpenTwoLight = false
        twoLightAlpha = 50
        ddeConfig = 2
        tempTextColor = 0xffffffff.toInt()
        temperatureMode = CameraItemBean.TYPE_TMP_C
        alarmBean = AlarmBean()
        isOpenCompass = false
        isOpenHighPoint = false
        isOpenLowPoint = false
        aiTraceType = ObserveBean.TYPE_NONE
        isOpenTarget = false
        targetMeasureMode = ObserveBean.TYPE_MEASURE_PERSON
        targetType = ObserveBean.TYPE_TARGET_HORIZONTAL
        targetColorType = ObserveBean.TYPE_TARGET_COLOR_GREEN
        reportAuthorName = CommUtils.getAppName()
        reportWatermarkText = CommUtils.getAppName()
        reportHumidity = 500
        fusionType = FusionTypeLPYFusion
        isOpenAmplify = false
    }

    var isSaveSetting: Boolean
        get() = SPUtils.getInstance(SP_NAME).getBoolean("isSaveSetting", true)
        set(value) {
            SPUtils.getInstance(SP_NAME).put("isSaveSetting", value)
        }
    var isMeasureTempMode: Boolean
        get() = if (isSaveSetting) SPUtils.getInstance(SP_NAME)
            .getBoolean("isMeasureTempMode", true) else true
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isMeasureTempMode", value)
            }
        }
    var isOpenAmplify: Boolean
        get() = if (isSaveSetting) SPUtils.getInstance(SP_NAME)
            .getBoolean("isOpenAmplify", false) else false
        set(value) {
            SPUtils.getInstance(SP_NAME).put("isOpenAmplify", value)
        }
    var isVideoMode: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isVideoMode", false)
            } else {
                false
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isVideoMode", value)
            }
        }
    var isAutoShutter: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isAutoShutter", true)
            } else {
                true
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isAutoShutter", value)
            }
        }
    var isRecordAudio: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isRecordAudio", false)
            } else {
                false
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isRecordAudio", value)
            }
        }
    var delayCaptureSecond: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getInt("delayCaptureSecond", 0)
            } else {
                0
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("delayCaptureSecond", value)
            }
        }
    var fusionType: Int
        get() = if (isSaveSetting) SPUtils.getInstance(SP_NAME)
            .getInt("fusionType", FusionTypeLPYFusion) else FusionTypeLPYFusion
        set(value) {
            SPUtils.getInstance(SP_NAME).put("fusionType", value)
        }
    var isOpenTwoLight: Boolean
        get() = if (isSaveSetting) SPUtils.getInstance(SP_NAME)
            .getBoolean("isOpenTwoLight", false) else false
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isOpenTwoLight", value)
            }
        }
    var twoLightAlpha: Int
        get() = if (isSaveSetting) SPUtils.getInstance(SP_NAME).getInt("twoLightAlpha", 50) else 50
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("twoLightAlpha", value)
            }
        }
    var pseudoColorMode: Int
        get() = if (isSaveSetting) SPUtils.getInstance(SP_NAME).getInt("pseudoColorMode", 3) else 3
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("pseudoColorMode", value)
            }
        }
    var isOpenPseudoBar: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isOpenPseudoBar", true)
            } else {
                true
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isOpenPseudoBar", value)
            }
        }
    var contrastValue: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getInt("contrastValue", 128)
            } else {
                128
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("contrastValue", value)
            }
        }
    var ddeConfig: Int
        get() = if (isSaveSetting) SPUtils.getInstance(SP_NAME).getInt("ddeConfig", 2) else 2
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("ddeConfig", value)
            }
        }
    var alarmBean: AlarmBean
        get() =
            if (isSaveSetting) {
                val json = SPUtils.getInstance(SP_NAME).getString("alarmBean", "")
                if (json.isNullOrEmpty()) AlarmBean() else Gson().fromJson(
                    json,
                    AlarmBean::class.java
                )
            } else {
                AlarmBean()
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("alarmBean", Gson().toJson(value))
            }
        }
    var rotateAngle: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getInt("rotateAngle", DeviceConfig.S_ROTATE_ANGLE)
            } else {
                DeviceConfig.S_ROTATE_ANGLE
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("rotateAngle", value)
            }
        }
    var isOpenMirror: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isOpenMirror", false)
            } else {
                false
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isOpenMirror", value)
            }
        }
    var isOpenCompass: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isOpenCompass", false)
            } else {
                false
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isOpenCompass", value)
            }
        }
    var tempTextColor: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getInt("tempTextColor", 0xffffffff.toInt())
            } else {
                0xffffffff.toInt()
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("tempTextColor", value)
            }
        }
    var tempTextSize: Int
        get() {
            val context = ContextProvider.getContext()
            val defaultSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                14f,
                context.resources.displayMetrics
            ).toInt()
            return if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getInt("tempTextSize", defaultSize)
            } else {
                defaultSize
            }
        }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("tempTextSize", value)
            }
        }
    var temperatureMode: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getInt("temperatureMode", CameraItemBean.TYPE_TMP_C)
            } else {
                CameraItemBean.TYPE_TMP_C
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("temperatureMode", value)
            }
        }
    var isOpenHighPoint: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isOpenHighPoint", false)
            } else {
                false
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isOpenHighPoint", value)
            }
        }
    var isOpenLowPoint: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isOpenLowPoint", false)
            } else {
                false
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isOpenLowPoint", value)
            }
        }
    var aiTraceType: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getInt("aiTraceType", ObserveBean.TYPE_NONE)
            } else {
                ObserveBean.TYPE_NONE
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("aiTraceType", value)
            }
        }
    var isOpenTarget: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isOpenTarget", false)
            } else {
                false
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isOpenTarget", value)
            }
        }
    var targetMeasureMode: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).getInt(
                    "targetMeasureMode",
                    ObserveBean.TYPE_MEASURE_PERSON,
                )
            } else {
                ObserveBean.TYPE_MEASURE_PERSON
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("targetMeasureMode", value)
            }
        }
    var targetType: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).getInt(
                    "targetType",
                    ObserveBean.TYPE_TARGET_HORIZONTAL,
                )
            } else {
                ObserveBean.TYPE_TARGET_HORIZONTAL
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("targetType", value)
            }
        }
    var targetColorType: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).getInt(
                    "targetColorType",
                    ObserveBean.TYPE_TARGET_COLOR_GREEN,
                )
            } else {
                ObserveBean.TYPE_TARGET_COLOR_GREEN
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("targetColorType", value)
            }
        }
    var reportAuthorName: String
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getString("reportAuthorName", CommUtils.getAppName())
            } else {
                CommUtils.getAppName()
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("reportAuthorName", value)
            }
        }
    var reportWatermarkText: String
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getString("reportWatermarkText", CommUtils.getAppName())
            } else {
                CommUtils.getAppName()
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("reportWatermarkText", value)
            }
        }
    var reportHumidity: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getInt("reportHumidity", 500)
            } else {
                500
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("reportHumidity", value)
            }
        }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\common\SharedManager.kt =====

package com.mpdc4gsr.libunified.app.common

import android.content.Context
import android.util.Base64
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.mpdc4gsr.libunified.compat.SPUtils
import com.mpdc4gsr.libunified.app.bean.CarDetectChildBean
import com.mpdc4gsr.libunified.app.bean.ContinuousBean
import com.mpdc4gsr.libunified.app.bean.WatermarkBean
import com.mpdc4gsr.libunified.app.utils.CarDetectData

object SharedManager {
    var hasClickWinter: Boolean
        get() = SPUtils.getInstance().getBoolean("hasClickWinter", false)
        set(value) = SPUtils.getInstance().put("hasClickWinter", value)
    var isNeedShowTrendTips: Boolean
        get() = SPUtils.getInstance().getBoolean("isNeedShowTrendTips", true)
        set(value) = SPUtils.getInstance().put("isNeedShowTrendTips", value)
    var hasShownStoragePermissionTip: Boolean
        get() = SPUtils.getInstance().getBoolean("hasShownStoragePermissionTip", false)
        set(value) = SPUtils.getInstance().put("hasShownStoragePermissionTip", value)
    var houseSpaceUnit: Int
        get() = SPUtils.getInstance().getInt("houseSpaceUnit", 0)
        set(value) {
            SPUtils.getInstance().put("houseSpaceUnit", value)
        }
    var costUnit: Int
        get() = SPUtils.getInstance().getInt("costUnit", 0)
        set(value) {
            SPUtils.getInstance().put("costUnit", value)
        }
    var hasTcLine: Boolean
        get() = SPUtils.getInstance().getBoolean("hasConnectTcLine", false)
        set(value) {
            SPUtils.getInstance().put("hasConnectTcLine", value)
        }

    // hasTS004 and hasTC007 properties removed - TS004/TC007 device support discontinued
    // hasTC007 property removed - TC007 device support discontinued
    // irConfigJsonTC007 property removed - TC007 device support discontinued
    var homeGuideStep: Int
        get() {
            val value = SPUtils.getInstance().getInt("homeGuideStep", 2)
            return if (value == 1) 2 else value
        }
        set(value) {
            SPUtils.getInstance().put("homeGuideStep", value)
        }
    var configGuideStep: Int
        get() = SPUtils.getInstance().getInt("configGuideStep", 1)
        set(value) = SPUtils.getInstance().put("configGuideStep", value)
    var isHideEmissivityTips: Boolean
        get() = SPUtils.getInstance().getBoolean("isHideEmissivityTips", false)
        set(value) {
            SPUtils.getInstance().put("isHideEmissivityTips", value)
        }
    var is07HideEmissivityTips: Boolean
        get() = SPUtils.getInstance().getBoolean("is07HideEmissivityTips", false)
        set(value) {
            SPUtils.getInstance().put("is07HideEmissivityTips", value)
        }
    var is04TISR: Boolean
        get() = SPUtils.getInstance().getBoolean("is04TISR", false)
        set(value) {
            SPUtils.getInstance().put("is04TISR", value)
        }
    var is04AutoSync: Boolean
        get() = SPUtils.getInstance().getBoolean("is04AutoSync", false)
        set(value) {
            SPUtils.getInstance().put("is04AutoSync", value)
        }

    fun getManualAngle(sId: String): Int {
        return SPUtils.getInstance().getInt("manualAngle_$sId", 1000)
    }

    fun setManualAngle(
        sId: String,
        value: Int,
    ) {
        SPUtils.getInstance().put("manualAngle_$sId", value)
    }

    fun getManualData(sId: String): ByteArray {
        val strValue = SPUtils.getInstance().getString("manualData_$sId")
        return if (strValue.isNullOrEmpty()) {
            byteArrayOf(
                0,
                0,
                -128,
                63,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                -128,
                63,
                0,
                0,
                0,
                0,
            )
        } else {
            Base64.decode(strValue.toByteArray(), Base64.DEFAULT)
        }
    }

    fun setManualData(
        sId: String,
        value: ByteArray,
    ) {
        if (value.size == 24) {
            SPUtils.getInstance()
                .put("manualData_$sId", String(Base64.encode(value, Base64.DEFAULT)))
        }
    }

    var isConnectAutoOpen: Boolean
        get() = SPUtils.getInstance().getBoolean("isConnectAutoOpen", false)
        set(value) {
            SPUtils.getInstance().put("isConnectAutoOpen", value)
        }
    var isConnect07AutoOpen: Boolean
        get() = SPUtils.getInstance().getBoolean("isConnect07AutoOpen", false)
        set(value) {
            SPUtils.getInstance().put("isConnect07AutoOpen", value)
        }
    var isTipOTG: Boolean
        get() = SPUtils.getInstance().getBoolean("isTipOTG", true)
        set(value) {
            SPUtils.getInstance().put("isTipOTG", value)
        }
    var isTipShutter: Boolean
        get() = SPUtils.getInstance().getBoolean("isTipShutter", true)
        set(value) {
            SPUtils.getInstance().put("isTipShutter", value)
        }
    var isTipHighTemp: Boolean
        get() = SPUtils.getInstance().getBoolean("isTipHighTemp", true)
        set(value) {
            SPUtils.getInstance().put("isTipHighTemp", value)
        }
    var isTipPinP: Boolean
        get() = SPUtils.getInstance().getBoolean("isTipPinP", true)
        set(value) {
            SPUtils.getInstance().put("isTipPinP", value)
        }
    var isTipCoordinate: Boolean
        get() = SPUtils.getInstance().getBoolean("isTipCoordinate", true)
        set(value) {
            SPUtils.getInstance().put("isTipCoordinate", value)
        }
    var isTipAIRecognition: Boolean
        get() = SPUtils.getInstance().getBoolean("isTipAIRecognition", true)
        set(value) {
            SPUtils.getInstance().put("isTipAIRecognition", value)
        }
    var isTipObservePhoto: Boolean
        get() = SPUtils.getInstance().getBoolean("isTipObservePhoto", true)
        set(value) {
            SPUtils.getInstance().put("isTipObservePhoto", value)
        }
    var continuousBean: ContinuousBean
        get() {
            val json = SPUtils.getInstance().getString("continuousBean", "")
            return if (json.isNullOrEmpty()) {
                ContinuousBean()
            } else {
                Gson().fromJson(
                    json,
                    ContinuousBean::class.java,
                )
            }
        }
        set(value) {
            SPUtils.getInstance().put("continuousBean", Gson().toJson(value))
        }
    var wifiWatermarkBean: WatermarkBean
        get() {
            val json = SPUtils.getInstance().getString("wifiWatermarkBean", "")
            return if (json.isNullOrEmpty()) {
                WatermarkBean()
            } else {
                Gson().fromJson(
                    json,
                    WatermarkBean::class.java,
                )
            }
        }
        set(value) {
            SPUtils.getInstance().put("watermarkBean", Gson().toJson(value))
        }
    var watermarkBean: WatermarkBean
        get() {
            val json = SPUtils.getInstance().getString("watermarkBean", "")
            return if (json.isNullOrEmpty()) {
                WatermarkBean()
            } else {
                Gson().fromJson(
                    json,
                    WatermarkBean::class.java,
                )
            }
        }
        set(value) {
            SPUtils.getInstance().put("watermarkBean", Gson().toJson(value))
        }
    var isTipChangeDevice: Boolean
        get() = SPUtils.getInstance().getBoolean("isTipChangeDevice", true)
        set(value) {
            SPUtils.getInstance().put("isTipChangeDevice", value)
        }
    var isChangeDevice: Boolean
        get() = SPUtils.getInstance().getBoolean("isChangeDevice", false)
        set(value) {
            SPUtils.getInstance().put("isChangeDevice", value)
        }
    private const val TOKEN: String = "token"
    private const val USER_ID: String = "user_id"
    private const val USERNAME: String = "username"
    private const val NICKNAME: String = "nickname"
    private const val HEAD_ICON: String = "head_icon"
    private const val BASE_HOST: String = "base_host"
    private const val LANGUAGE = "language"
    private const val HAS_SHOW_CLAUSE = "hasShowClause"
    private const val TEMPERATURE_UNIT = "temperature"
    private const val VERSION_CHECK_DATE = "version_check_date"
    private const val DEVICE_SN = "deviceSn"
    private const val DEVICE_VERSION = "deviceVersion"
    private const val IR_CONFIG = "ir_config"
    private const val SP_CUSTOM_PSEUDO = "sp_custom_pseudo"
    private const val SP_TARGET_POP = "sp_target_pop"
    private const val SP_SETTING_IS_PUSH = "sp_setting_is_push"
    private const val SP_SETTING_IS_RECOMMEND = "sp_setting_is_recommend"
    private const val SP_HOT_MODE = "sp_hot_mode"
    private const val SP_CHANGE_DEVICE = "sp_change_device"
    private const val SP_TC007_CUSTOM_PSEUDO = "sp_tc007_custom_pseudo"
    private const val SP_CAR_DETECT = "sp_car_detect"
    fun setToken(token: String) {
        SPUtils.getInstance().put(TOKEN, token)
    }

    fun getToken(): String {
        return SPUtils.getInstance().getString(TOKEN, "")
    }

    fun setUserId(token: String) {
        SPUtils.getInstance().put(USER_ID, token)
    }

    fun getUserId(): String {
        return SPUtils.getInstance().getString(USER_ID, "0")
    }

    fun setUsername(username: String) {
        SPUtils.getInstance().put(USERNAME, username)
    }

    fun getUsername(): String {
        return SPUtils.getInstance().getString(USERNAME, "")
    }

    fun setNickname(nickname: String) {
        SPUtils.getInstance().put(NICKNAME, nickname)
    }

    fun getNickname(): String {
        return SPUtils.getInstance().getString(NICKNAME, "")
    }

    fun setHeadIcon(headIcon: String) {
        SPUtils.getInstance().put(HEAD_ICON, headIcon)
    }

    fun getHeadIcon(): String {
        return SPUtils.getInstance().getString(HEAD_ICON, "")
    }

    fun setBaseHost(value: String) {
        return SPUtils.getInstance().put(BASE_HOST, value)
    }

    fun getBaseHost(): String {
        return SPUtils.getInstance().getString(BASE_HOST, "")
    }

    fun setLanguage(
        context: Context,
        language: String,
    ) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit().putString(LANGUAGE, language).apply()
    }

    fun getLanguage(context: Context): String {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(LANGUAGE, "")!!
    }

    fun setHasShowClause(hasShowClause: Boolean) {
        return SPUtils.getInstance().put(HAS_SHOW_CLAUSE, hasShowClause)
    }

    fun getHasShowClause(): Boolean {
        return SPUtils.getInstance().getBoolean(HAS_SHOW_CLAUSE, false)
    }

    fun setIRConfig(value: String) {
        return SPUtils.getInstance().put(IR_CONFIG, value)
    }

    fun getIRConfig(): String {
        return SPUtils.getInstance().getString(IR_CONFIG, "")
    }

    fun setTemperature(value: Int) {
        return SPUtils.getInstance().put(TEMPERATURE_UNIT, value)
    }

    fun getTemperature(): Int {
        return SPUtils.getInstance().getInt(TEMPERATURE_UNIT, 1)
    }

    fun setVersionCheckDate(value: Long) {
        return SPUtils.getInstance().put(VERSION_CHECK_DATE, value)
    }

    fun getVersionCheckDate(): Long {
        return SPUtils.getInstance().getLong(VERSION_CHECK_DATE, 0)
    }

    fun setDeviceSn(value: String) {
        return SPUtils.getInstance().put(DEVICE_SN, value)
    }

    fun getDeviceSn(): String {
        return SPUtils.getInstance().getString(DEVICE_SN, "")
    }

    fun setDeviceVersion(value: String) {
        return SPUtils.getInstance().put(DEVICE_VERSION, value)
    }

    fun getDeviceVersion(): String {
        return SPUtils.getInstance().getString(DEVICE_VERSION, "")
    }

    fun saveCustomPseudo(json: String) {
        SPUtils.getInstance().put(SP_CUSTOM_PSEUDO, json)
    }

    fun getCustomPseudo(): String {
        return SPUtils.getInstance().getString(SP_CUSTOM_PSEUDO, "")
    }

    // saveTC007CustomPseudo and getTC0007CustomPseudo methods removed - TC007 device support discontinued
    fun getTargetPop(): Boolean {
        return SPUtils.getInstance().getBoolean(SP_TARGET_POP, false)
    }

    fun saveTargetPop(targetPop: Boolean) {
        SPUtils.getInstance().put(SP_TARGET_POP, targetPop)
    }

    private const val IR_DUAL_DISP = "ir_dual_disp"
    private const val IR_DUAL_DISP_V = "ir_dual_disp_v"
    fun saveSettingIsPush(isPush: Boolean) {
        SPUtils.getInstance().put(SP_SETTING_IS_PUSH, isPush)
    }

    fun getSettingIsPush(): Boolean {
        return SPUtils.getInstance().getBoolean(SP_SETTING_IS_PUSH, true)
    }

    fun saveSettingIsRecommend(isRecommend: Boolean) {
        SPUtils.getInstance().put(SP_SETTING_IS_RECOMMEND, isRecommend)
    }

    fun getSettingIsRecommend(): Boolean {
        return SPUtils.getInstance().getBoolean(SP_SETTING_IS_RECOMMEND, true)
    }

    fun getMainPermissionsState(): Boolean {
        return SPUtils.getInstance().getBoolean("main_permissions_state", false)
    }

    fun setMainPermissionsState(value: Boolean) {
        return SPUtils.getInstance().put("main_permissions_state", value)
    }

    fun getImagePermissionsState(): Boolean {
        return SPUtils.getInstance().getBoolean("storage_permissions_state", false)
    }

    fun setImagePermissionsState(value: Boolean) {
        return SPUtils.getInstance().put("storage_permissions_state", value)
    }

    fun getHotMode(): Int {
        return SPUtils.getInstance().getInt(SP_HOT_MODE, 1)
    }

    fun saveHotMode(hotMode: Int) {
        SPUtils.getInstance().put(SP_HOT_MODE, hotMode)
    }

    fun getChangeDevice(): Int {
        return SPUtils.getInstance().getInt(SP_CHANGE_DEVICE, 0)
    }

    fun saveChangeDevice(device: Int) {
        SPUtils.getInstance().put(SP_CHANGE_DEVICE, device)
    }

    fun getCarDetectInfo(): CarDetectChildBean {
        val detectInfo = SPUtils.getInstance().getString(SP_CAR_DETECT, "")
        if (detectInfo.isEmpty()) {
            return CarDetectData.getDetectList()[0].detectChildBeans[0]
        }
        val detectChildBean = Gson().fromJson(detectInfo, CarDetectChildBean::class.java)
        val type = detectChildBean.type
        val pos = detectChildBean.pos
        return CarDetectData.getDetectList()[type].detectChildBeans[pos]
    }

    fun saveCarDetectInfo(bean: CarDetectChildBean) {
        SPUtils.getInstance().put(SP_CAR_DETECT, Gson().toJson(bean))
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\common\UserInfoManager.kt =====

package com.mpdc4gsr.libunified.app.common

import android.text.TextUtils

class UserInfoManager {
    companion object {
        @Volatile
        var manager: UserInfoManager? = null
        fun getInstance(): UserInfoManager {
            if (manager == null) {
                synchronized(UserInfoManager::class) {
                    if (manager == null) {
                        manager = UserInfoManager()
                    }
                }
            }
            return manager!!
        }
    }

    fun isLogin(): Boolean {
        val token = SharedManager.getToken()
        return if (TextUtils.equals("-1", token)) {
            false
        } else {
            !TextUtils.isEmpty(token)
        }
    }

    fun login(
        token: String,
        userId: String,
        phone: String?,
        email: String,
        nickname: String,
        headUrl: String?,
    ) {
        SharedManager.setUserId(userId)
        SharedManager.setUsername(
            if (getMaskPhone(phone)?.isNotEmpty() == true) getMaskPhone(phone) ?: "" else email
        )
        SharedManager.setNickname(nickname)
        SharedManager.setHeadIcon(headUrl ?: "12345")
        SharedManager.setToken(token)
    }

    fun logout() {
        SharedManager.setToken("")
        SharedManager.setUserId("0")
        SharedManager.setNickname("")
        SharedManager.setHeadIcon("")
    }

    private fun getMaskPhone(phone: String?): String? {
        return phone?.replace("(\\d{3})\\d{4}(\\d{4})".toRegex(), "$1****$2")
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\common\WifiSaveSettingUtils.kt =====

package com.mpdc4gsr.libunified.app.common

import com.mpdc4gsr.libunified.compat.SPUtils
import com.google.gson.Gson
import com.mpdc4gsr.libunified.app.bean.AlarmBean
import com.mpdc4gsr.libunified.app.bean.CameraItemBean
import com.mpdc4gsr.libunified.app.bean.ObserveBean
import com.mpdc4gsr.libunified.app.common.SaveSettingUtils.FusionTypeIROnly
import com.mpdc4gsr.libunified.app.common.SaveSettingUtils.FusionTypeLPYFusion
import com.mpdc4gsr.libunified.app.config.DeviceConfig
import com.mpdc4gsr.libunified.app.utils.CommUtils

object WifiSaveSettingUtils {
    private const val SP_NAME = "WifiSaveSettingUtils"
    const val TYPE_PLUG = 0
    const val TYPE_WIFI = 1
    fun reset() {
        isMeasureTempMode = true
        isVideoMode = false
        isAutoShutter = true
        isRecordAudio = false
        isOpenMirror = false
        delayCaptureSecond = 0
        contrastValue = 128
        pseudoColorMode = 3
        rotateAngle = DeviceConfig.S_ROTATE_ANGLE
        isOpenPseudoBar = true
        isOpenTwoLight = false
        twoLightAlpha = 50
        ddeConfig = 2
        tempTextColor = 0xffffffff.toInt()
        temperatureMode = CameraItemBean.TYPE_TMP_C
        alarmBean = AlarmBean()
        isOpenCompass = false
        isOpenHighPoint = false
        isOpenLowPoint = false
        aiTraceType = ObserveBean.TYPE_NONE
        isOpenTarget = false
        targetMeasureMode = ObserveBean.TYPE_MEASURE_PERSON
        targetType = ObserveBean.TYPE_TARGET_HORIZONTAL
        targetColorType = ObserveBean.TYPE_TARGET_COLOR_GREEN
        reportAuthorName = CommUtils.getAppName()
        reportWatermarkText = CommUtils.getAppName()
        reportHumidity = 500
        fusionType = FusionTypeLPYFusion
        registrationX = 0
        registrationY = 0
    }

    var registrationX: Int
        get() = if (isSaveSetting) SPUtils.getInstance(SP_NAME).getInt("registrationX", 0) else 0
        set(value) {
            SPUtils.getInstance(SP_NAME).put("registrationX", value)
        }
    var registrationY: Int
        get() = if (isSaveSetting) SPUtils.getInstance(SP_NAME).getInt("registrationY", 0) else 0
        set(value) {
            SPUtils.getInstance(SP_NAME).put("registrationY", value)
        }
    var fusionType: Int
        get() = if (isSaveSetting) SPUtils.getInstance(SP_NAME)
            .getInt("fusionType", FusionTypeIROnly) else FusionTypeIROnly
        set(value) {
            SPUtils.getInstance(SP_NAME).put("fusionType", value)
        }
    var isSaveSetting: Boolean
        get() = SPUtils.getInstance(SP_NAME).getBoolean("isSaveSetting", true)
        set(value) {
            SPUtils.getInstance(SP_NAME).put("isSaveSetting", value)
        }
    var isMeasureTempMode: Boolean
        get() = if (isSaveSetting) SPUtils.getInstance(SP_NAME)
            .getBoolean("isMeasureTempMode", true) else true
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isMeasureTempMode", value)
            }
        }
    var isVideoMode: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isVideoMode", false)
            } else {
                false
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isVideoMode", value)
            }
        }
    var isAutoShutter: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isAutoShutter", true)
            } else {
                true
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isAutoShutter", value)
            }
        }
    var isRecordAudio: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isRecordAudio", false)
            } else {
                false
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isRecordAudio", value)
            }
        }
    var isOpenMirror: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isOpenMirror", false)
            } else {
                false
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isOpenMirror", value)
            }
        }
    var delayCaptureSecond: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getInt("delayCaptureSecond", 0)
            } else {
                0
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("delayCaptureSecond", value)
            }
        }
    var contrastValue: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getInt("contrastValue", 128)
            } else {
                128
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("contrastValue", value)
            }
        }
    var pseudoColorMode: Int
        get() = if (isSaveSetting) SPUtils.getInstance(SP_NAME).getInt("pseudoColorMode", 3) else 3
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("pseudoColorMode", value)
            }
        }
    var rotateAngle: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getInt("rotateAngle", DeviceConfig.S_ROTATE_ANGLE)
            } else {
                DeviceConfig.S_ROTATE_ANGLE
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("rotateAngle", value)
            }
        }
    var isOpenPseudoBar: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isOpenPseudoBar", true)
            } else {
                true
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isOpenPseudoBar", value)
            }
        }
    var isOpenTwoLight: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isOpenTwoLight", false)
            } else {
                false
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isOpenTwoLight", value)
            }
        }
    var twoLightAlpha: Int
        get() = if (isSaveSetting) SPUtils.getInstance(SP_NAME).getInt("twoLightAlpha", 50) else 50
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("twoLightAlpha", value)
            }
        }
    var ddeConfig: Int
        get() = if (isSaveSetting) SPUtils.getInstance(SP_NAME).getInt("ddeConfig", 2) else 2
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("ddeConfig", value)
            }
        }
    var tempTextColor: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getInt("tempTextColor", 0xffffffff.toInt())
            } else {
                0xffffffff.toInt()
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("tempTextColor", value)
            }
        }
    var tempTextSize: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getInt("tempTextSize", 14)
            } else {
                14
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("tempTextSize", value)
            }
        }
    var temperatureMode: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getInt("temperatureMode", CameraItemBean.TYPE_TMP_C)
            } else {
                CameraItemBean.TYPE_TMP_C
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("temperatureMode", value)
            }
        }
    var alarmBean: AlarmBean
        get() =
            if (isSaveSetting) {
                val json = SPUtils.getInstance(SP_NAME).getString("alarmBean", "")
                if (json.isNullOrEmpty()) AlarmBean() else Gson().fromJson(
                    json,
                    AlarmBean::class.java
                )
            } else {
                AlarmBean()
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("alarmBean", Gson().toJson(value))
            }
        }
    var isOpenCompass: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isOpenCompass", false)
            } else {
                false
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isOpenCompass", value)
            }
        }
    var isOpenHighPoint: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isOpenHighPoint", false)
            } else {
                false
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isOpenHighPoint", value)
            }
        }
    var isOpenLowPoint: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isOpenLowPoint", false)
            } else {
                false
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isOpenLowPoint", value)
            }
        }
    var aiTraceType: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getInt("aiTraceType", ObserveBean.TYPE_NONE)
            } else {
                ObserveBean.TYPE_NONE
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("aiTraceType", value)
            }
        }
    var isOpenTarget: Boolean
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getBoolean("isOpenTarget", false)
            } else {
                false
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("isOpenTarget", value)
            }
        }
    var targetMeasureMode: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).getInt(
                    "targetMeasureMode",
                    ObserveBean.TYPE_MEASURE_PERSON,
                )
            } else {
                ObserveBean.TYPE_MEASURE_PERSON
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("targetMeasureMode", value)
            }
        }
    var targetType: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).getInt(
                    "targetType",
                    ObserveBean.TYPE_TARGET_HORIZONTAL,
                )
            } else {
                ObserveBean.TYPE_TARGET_HORIZONTAL
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("targetType", value)
            }
        }
    var targetColorType: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).getInt(
                    "targetColorType",
                    ObserveBean.TYPE_TARGET_COLOR_GREEN,
                )
            } else {
                ObserveBean.TYPE_TARGET_COLOR_GREEN
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("targetColorType", value)
            }
        }
    var reportAuthorName: String
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getString("reportAuthorName", CommUtils.getAppName())
            } else {
                CommUtils.getAppName()
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("reportAuthorName", value)
            }
        }
    var reportWatermarkText: String
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getString("reportWatermarkText", CommUtils.getAppName())
            } else {
                CommUtils.getAppName()
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("reportWatermarkText", value)
            }
        }
    var reportHumidity: Int
        get() =
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME)
                    .getInt("reportHumidity", 500)
            } else {
                500
            }
        set(value) {
            if (isSaveSetting) {
                SPUtils.getInstance(SP_NAME).put("reportHumidity", value)
            }
        }
}


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\util\libunified_src_main_java_com_mpdc4gsr_libunified_app_comm_util_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\util' subtree
// Files: 1; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\util\SingletonHolder.kt =====

package com.mpdc4gsr.libunified.app.comm.util

open class SingletonHolder<out T, in A>(private val creator: (A) -> T) {
    @Volatile
    private var instance: T? = null
    fun getInstance(arg: A): T {
        // First check without synchronization for performance
        return instance ?: synchronized(this) {
            // Second check with synchronization to ensure thread safety
            instance ?: creator(arg).also { instance = it }
        }
    }
}


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\view\libunified_src_main_java_com_mpdc4gsr_libunified_app_comm_view_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\view' subtree
// Files: 2; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\view\CommLoadMoreView.kt =====

package com.mpdc4gsr.libunified.app.comm.view

import android.view.View
import android.view.ViewGroup
import com.chad.library.adapter.base.loadmore.BaseLoadMoreView
import com.chad.library.adapter.base.util.getItemView
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.mpdc4gsr.libunified.R

class CommLoadMoreView : BaseLoadMoreView() {
    override fun getRootView(parent: ViewGroup): View =
        parent.getItemView(R.layout.layout_load_more_view)

    override fun getLoadingView(holder: BaseViewHolder): View =
        holder.getView(R.id.load_more_loading_view)

    override fun getLoadComplete(holder: BaseViewHolder): View =
        holder.getView(R.id.load_more_load_complete_view)

    override fun getLoadEndView(holder: BaseViewHolder): View =
        holder.getView(R.id.load_more_load_end_view)

    override fun getLoadFailView(holder: BaseViewHolder): View =
        holder.getView(R.id.load_more_load_fail_view)
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\view\TempLayout.kt =====

package com.mpdc4gsr.libunified.app.comm.view

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.mpdc4gsr.libunified.R

class TempLayout : LinearLayout {
    companion object {
        val TYPE_HOT = 1
        val TYPE_LT = 2
        val TYPE_A = 3
    }

    private var alphaAnimator: ObjectAnimator? = null
    var rootV: View? = null
    var bg: View? = null
    var isHot: Boolean = true
    var type = -1

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView()
    }

    var animatorAlpha = 1f
    private fun initView() {
        rootV = LayoutInflater.from(context).inflate(R.layout.layout_temp_bg, this)
        bg = rootV?.findViewById(R.id.bg)
        alphaAnimator = ObjectAnimator.ofFloat(this, "alpha", 0f, 1f)
        alphaAnimator?.duration = 500
        alphaAnimator?.interpolator =
            BreatheInterpolator()
        alphaAnimator?.addUpdateListener {
            animatorAlpha = it.getAnimatedValue("alpha") as Float
        }
        alphaAnimator?.repeatCount = ValueAnimator.INFINITE
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    )

    fun startAnimation(type: Int) {
        this.visibility = View.VISIBLE
        if (this.type != type) {
            alphaAnimator?.cancel()
            alphaAnimator?.removeAllListeners()
            when (type) {
                TYPE_HOT -> {
                    isHot = true
                    alphaAnimator?.repeatCount = ValueAnimator.INFINITE
                    bg?.setBackgroundResource(R.drawable.ic_ir_read_bg)
                }

                TYPE_A -> {
                    alphaAnimator?.repeatCount = 0
                    alphaAnimator?.addListener(animatorListener)
                }

                else -> {
                    alphaAnimator?.repeatCount = ValueAnimator.INFINITE
                    isHot = false
                    bg?.setBackgroundResource(R.drawable.ic_ir_blue_bg)
                }
            }
            if (isAttachedToWindow) {
                try {
                    alphaAnimator?.start()
                } catch (e: IllegalStateException) {
                    Log.w("TempLayout", "Failed to start animator: ${e.message}")
                }
            }
            this.type = type
        }
    }

    var animatorListener: Animator.AnimatorListener =
        object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
            }

            override fun onAnimationEnd(animation: Animator) {
                if (this@TempLayout.visibility == View.VISIBLE && isAttachedToWindow) {
                    isHot = !isHot
                    if (isHot) {
                        bg?.setBackgroundResource(R.drawable.ic_ir_read_bg)
                    } else {
                        bg?.setBackgroundResource(R.drawable.ic_ir_blue_bg)
                    }
                    try {
                        alphaAnimator?.start()
                    } catch (e: IllegalStateException) {
                        Log.w("TempLayout", "Failed to restart animator in onAnimationEnd: ${e.message}")
                    }
                }
            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        }

    fun stopAnimation() {
        this.type = -1
        alphaAnimator?.removeAllListeners()
        this.visibility = View.GONE
        alphaAnimator?.cancel()
    }

    fun startAlphaBreathAnimation() {
        if (isAttachedToWindow) {
            try {
                alphaAnimator?.start()
            } catch (e: IllegalStateException) {
                Log.w("TempLayout", "Failed to start breath animation: ${e.message}")
            }
        }
    }
}