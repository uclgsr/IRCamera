package com.mpdc4gsr.module.thermalunified.popup

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupWindow
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.view.isVisible
import com.mpdc4gsr.module.thermalunified.R


@SuppressLint("SetTextI18n")
class SeekBarPopup(context: Context, hasTitle: Boolean = false) : PopupWindow() {
    private val seekBar: SeekBar
    private val tvTitle: TextView
    private val tvValue: TextView
    
    var progress: Int
        get() = seekBar.progress
        set(value) {
            seekBar.progress = value
        }

    var max: Int
        get() = seekBar.max
        set(value) {
            seekBar.max = value
        }


    var isRealTimeTrigger = false

    var onValuePickListener: ((progress: Int) -> Unit)? = null

    init {
        contentView = LayoutInflater.from(context).inflate(R.layout.pop_seek_bar, null)
        
        seekBar = contentView.findViewById(R.id.seek_bar)
        tvTitle = contentView.findViewById(R.id.tv_title)
        tvValue = contentView.findViewById(R.id.tv_value)
        
        val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(
            context.resources.displayMetrics.widthPixels,
            View.MeasureSpec.EXACTLY
        )
        val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(
            context.resources.displayMetrics.heightPixels,
            View.MeasureSpec.AT_MOST
        )
        tvTitle.isVisible = hasTitle
        contentView.measure(widthMeasureSpec, heightMeasureSpec)
        tvValue.text = "$progress%"
        seekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean,
                ) {
                    tvValue.text = "$progress%"
                    if (isRealTimeTrigger) {
                        onValuePickListener?.invoke(progress)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    onValuePickListener?.invoke(seekBar.progress)
                }
            },
        )

        width = contentView.measuredWidth
        height = contentView.measuredHeight
        isOutsideTouchable = false
    }

    fun show(
        anchor: View,
        isDropDown: Boolean,
    ) {
        if (isDropDown) {
            showAsDropDown(anchor)
        } else {
            showAsDropDown(anchor, 0, -height, Gravity.NO_GRAVITY)
        }
    }
}
