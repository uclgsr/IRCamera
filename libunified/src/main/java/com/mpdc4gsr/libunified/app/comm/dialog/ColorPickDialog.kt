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
