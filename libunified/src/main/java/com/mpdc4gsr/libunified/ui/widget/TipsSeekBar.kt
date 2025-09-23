package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.SeekBar
import com.mpdc4gsr.libunified.R

class TipsSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SeekBar(context, attrs, defStyleAttr) {

    var onStopTrackingTouch: ((Int) -> Unit)? = null
    var valueFormatListener: ((Int) -> String)? = null

    private var minText: String = "0"
    private var maxText: String = "100"
    private var tipsPercent: Float = 0f
    private var seekPercent: Float = 0f

    init {
        // Load custom attributes
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.TipsSeekBar)
            try {
                minText = typedArray.getString(R.styleable.TipsSeekBar_minText) ?: "0"
                maxText = typedArray.getString(R.styleable.TipsSeekBar_maxText) ?: "100"
                tipsPercent = typedArray.getFraction(R.styleable.TipsSeekBar_tipsPercent, 1, 1, 0f)
                seekPercent = typedArray.getFraction(R.styleable.TipsSeekBar_seekPercent, 1, 1, 0f)
                
                // Set initial progress based on seekPercent
                if (seekPercent > 0) {
                    progress = (seekPercent * max).toInt()
                }
            } finally {
                typedArray.recycle()
            }
        }

        // Set up the touch listener
        setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Progress changes are handled real-time
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Called when user starts dragging
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Call our custom callback when user stops dragging
                onStopTrackingTouch?.invoke(progress)
            }
        })
    }

    fun getFormattedValue(): String {
        return valueFormatListener?.invoke(progress) ?: progress.toString()
    }

    fun setMinText(text: String) {
        minText = text
    }

    fun getMinText(): String = minText

    fun setMaxText(text: String) {
        maxText = text
    }

    fun getMaxText(): String = maxText

    fun setTipsPercent(percent: Float) {
        tipsPercent = percent
    }

    fun getTipsPercent(): Float = tipsPercent

    fun setSeekPercent(percent: Float) {
        seekPercent = percent
        progress = (percent * max).toInt()
    }

    fun getSeekPercent(): Float = seekPercent
}