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
import com.mpdc4gsr.libunified.app.utils.LibraryLogger
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
        } catch (exception: Exception) {
            LibraryLogger.e("TempAlarmSetDialog", "Unexpected Exception in TempAlarmSetDialog catch block", exception)
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
        } catch (exception: Exception) {
            LibraryLogger.e("TempAlarmSetDialog", "Unexpected Exception in TempAlarmSetDialog catch block", exception)
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
        } catch (exception: Exception) {
            LibraryLogger.e("TempAlarmSetDialog", "Unexpected Exception in TempAlarmSetDialog catch block", exception)
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
