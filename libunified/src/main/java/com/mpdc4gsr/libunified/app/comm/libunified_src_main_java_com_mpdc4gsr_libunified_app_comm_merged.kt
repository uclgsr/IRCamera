// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\app\comm' directory and its subdirectories.
// Total files: 11 | Generated on: 2025-10-08 01:42:38


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\AlarmHelp.kt =====

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


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\bean\SaveSettingBean.kt =====

package com.mpdc4gsr.libunified.app.comm.bean

import android.util.TypedValue
import com.google.gson.Gson
import com.mpdc4gsr.libunified.app.bean.AlarmBean
import com.mpdc4gsr.libunified.app.bean.CameraItemBean
import com.mpdc4gsr.libunified.app.bean.ObserveBean
import com.mpdc4gsr.libunified.app.common.SaveSettingUtils
import com.mpdc4gsr.libunified.app.config.DeviceConfig
import com.mpdc4gsr.libunified.app.utils.CommUtils
import com.mpdc4gsr.libunified.compat.ContextProvider
import com.mpdc4gsr.libunified.compat.SPUtils

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


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\dialog\ColorPickDialog.kt =====

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
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.app.utils.ColorUtils
import com.mpdc4gsr.libunified.app.utils.ScreenUtils
import com.mpdc4gsr.libunified.app.view.ColorSelectView
import com.mpdc4gsr.libunified.compat.spToPx
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


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\dialog\TempAlarmSetDialog.kt =====

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


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\ExcelUtils.java =====

package com.mpdc4gsr.libunified.app.comm;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.mpdc4gsr.libunified.R;
import com.mpdc4gsr.libunified.app.common.SharedManager;
import com.mpdc4gsr.libunified.app.config.FileConfig;
import com.mpdc4gsr.libunified.app.db.entity.ThermalEntity;
import com.mpdc4gsr.libunified.app.tools.TimeTools;
import com.mpdc4gsr.libunified.app.tools.UnitTools;
import com.mpdc4gsr.libunified.compat.ContextProvider;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class ExcelUtils {

    @NonNull
    private static String getTemperature(int index, @NonNull byte[] norTempData, boolean isShowC) {
        int tempValue = (norTempData[2 * index + 1] << 8 & 0xff00) | (norTempData[2 * index] & 0xff);
        float value = tempValue / 64f - 273.15f;
        return UnitTools.showC(value, isShowC);
    }

    @Nullable
    public static String exportExcel(@NonNull String name, int width, int height, @NonNull byte[] norTempData, @Nullable Callback callback) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet();
        boolean isShowC = SharedManager.INSTANCE.getTemperature() == 1;
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        long time = System.currentTimeMillis();
        for (int i = 0; i < height; i++) {
            Row row = sheet.createRow(i);
            for (int j = 0; j < width; j++) {
                int index = i * width + j;
                sheet.setColumnWidth(j, 9 * width);
                Cell cell = row.createCell(j);
                cell.setCellStyle(cellStyle);
                cell.setCellValue(getTemperature(index, norTempData, isShowC));
                if (index % 100 == 0 && callback != null) {
                    //11ï¼Œ1001
                    callback.onOneCell(index / 100, width * height / 100);
                }
            }
        }
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                File excel = new File(FileConfig.getExcelDir(), name + ".xlsx");
                FileOutputStream fos = new FileOutputStream(excel);
                workbook.write(fos);
                fos.flush();
                fos.close();
                return excel.getAbsolutePath();
            } else {
                String fileName = name + ".xlsx";
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, FileConfig.getExcelDir());
                Uri contentUri = MediaStore.Files.getContentUri("external");
                Uri uri = ContextProvider.getContext().getContentResolver().insert(contentUri, values);
                if (uri != null) {
                    OutputStream outputStream = ContextProvider.getContext().getContentResolver().openOutputStream(uri);
                    if (outputStream != null) {
                        BufferedOutputStream bos = new BufferedOutputStream(outputStream);
                        workbook.write(bos);
                        bos.flush();
                        bos.close();
                    }
                    DocumentFile documentFile = DocumentFile.fromSingleUri(ContextProvider.getContext(), uri);
                    String filePath = uri.toString();
                    Log.w("", filePath);
                    return filePath;
                } else {
                    return null;
                }
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static String exportExcel(ArrayList<ThermalEntity> listData, boolean isPoint) {
        boolean isShowC = SharedManager.INSTANCE.getTemperature() == 1;
        try {
            // excel xlsx
            Workbook wb = new XSSFWorkbook();
            // 
            Sheet sheet = wb.createSheet();
            String[] title = {ContextProvider.getContext().getString(R.string.detail_date), ContextProvider.getContext().getString(R.string.chart_temperature_low), ContextProvider.getContext().getString(R.string.chart_temperature_high)};
            if (isPoint) {
                title = new String[]{ContextProvider.getContext().getString(R.string.detail_date), ContextProvider.getContext().getString(R.string.chart_temperature)};
            }
            //
            Row row = sheet.createRow(0);
            // 
            int colNum = title.length;
            CellStyle titleStyle = wb.createCellStyle();
            titleStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            titleStyle.setAlignment(HorizontalAlignment.CENTER); // 
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            Font font = wb.createFont();
            font.setBold(true);//
            titleStyle.setFont(font);
            CellStyle contentStyle = wb.createCellStyle();
            contentStyle.setAlignment(HorizontalAlignment.CENTER); // 
            contentStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            for (int i = 0; i < colNum; i++) {
                sheet.setColumnWidth(i, 20 * 256);  // 20
                Cell cell1 = row.createCell(i);
                cell1.setCellStyle(titleStyle);
                //
                cell1.setCellValue(title[i]);
            }
            // 
            for (int rowNum = 0; rowNum < listData.size(); rowNum++) {

                // rowNum + 1 
                row = sheet.createRow(rowNum + 1);
                // 
                row.setHeightInPoints(28f);

                ThermalEntity bean = listData.get(rowNum);

                for (int j = 0; j < title.length; j++) {
                    Cell cell = row.createCell(j);
                    //title[]
                    if (isPoint) {
                        switch (j) {
                            case 0:
                                //
                                cell.setCellValue(bean.getTime());
                                break;
                            case 1:
                                //
                                cell.setCellStyle(contentStyle);
                                cell.setCellValue(UnitTools.showC(bean.getMinTemp()));
                                break;
                        }
                    } else {
                        switch (j) {
                            case 0:
                                //
                                cell.setCellValue(bean.getTime());
                                break;
                            case 1:
                                //
                                cell.setCellStyle(contentStyle);
                                cell.setCellValue(UnitTools.showC(bean.getMinTemp()));
                                break;
                            case 2:
                                //
                                cell.setCellStyle(contentStyle);
                                cell.setCellValue(UnitTools.showC(bean.getMaxTemp(), isShowC));
                                break;
                        }
                    }
                }
            }
            String timeStr;
            if (listData.isEmpty()) {
                timeStr = TimeTools.INSTANCE.showDateSecond();
            } else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
                timeStr = Instant.ofEpochMilli(listData.get(0).getStartTime())
                        .atZone(ZoneId.systemDefault())
                        .format(formatter);
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                File excel = new File(FileConfig.getExcelDir(), "TCView_" + timeStr + ".xlsx");
                FileOutputStream fos = new FileOutputStream(excel);
                wb.write(fos);
                fos.flush();
                fos.close();
                return excel.getAbsolutePath();
            } else {
                String fileName = "TCView_" + timeStr + ".xlsx";
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
//                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/xlsx");
//                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM);
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, FileConfig.getExcelDir());
                Uri contentUri = MediaStore.Files.getContentUri("external");
                Uri uri = ContextProvider.getContext().getContentResolver().insert(contentUri, values);
                if (uri != null) {
                    OutputStream outputStream = ContextProvider.getContext().getContentResolver().openOutputStream(uri);
                    if (outputStream != null) {
                        BufferedOutputStream bos = new BufferedOutputStream(outputStream);
                        wb.write(bos);
                        bos.flush();
                        bos.close();
                    }
                    DocumentFile documentFile = DocumentFile.fromSingleUri(ContextProvider.getContext(), uri);
                    String filePath = documentFile != null ? documentFile.getName() : uri.toString();
                    Log.w("", filePath);
                    return filePath;
                } else {
                    return null;
                }
            }
        } catch (IOException e) {
            Log.e("ExpressExcle", "exportExcel", e);
            return null;
        }

    }

    @FunctionalInterface
    public interface Callback {
        void onOneCell(int current, int total);
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\IrParam.kt =====

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


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\PDFHelp.kt =====

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
import androidx.documentfile.provider.DocumentFile
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.app.config.FileConfig
import com.mpdc4gsr.libunified.compat.ContextProvider
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


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\util\SingletonHolder.kt =====

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


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\view\BreatheInterpolator.java =====

package com.mpdc4gsr.libunified.app.comm.view;

import android.animation.TimeInterpolator;

class BreatheInterpolator implements TimeInterpolator {

    @Override

    public float getInterpolation(float input) {

        float x = 6 * input;

        float k = 1.0f / 3;

        int t = 6;

        int n = 1;//[CHINESE_TEXT]ï¼Œ[CHINESE_TEXT]

        float PI = 3.1416f;

        float output = 0;

        if (x >= ((n - 1) * t) && x < ((n - (1 - k)) * t)) {

            output = (float) (0.5 * Math.sin((PI / (k * t)) * ((x - k * t / 2) - (n - 1) * t)) + 0.5);

        } else if (x >= (n - (1 - k)) * t && x < n * t) {

            output = (float) Math.pow((0.5 * Math.sin((PI / ((1 - k) * t)) * ((x - (3 - k) * t / 2) - (n - 1) * t)) + 0.5), 2);

        }

        return output;

    }

    public void updateTime() {
        String a = "";
        String[] as = a.split("");
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\view\CommLoadMoreView.kt =====

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


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\view\TempLayout.kt =====

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