package com.topdon.module.thermal.ir.popup

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec
import android.widget.PopupWindow
import android.widget.TextView
import com.blankj.utilcode.util.SizeUtils
import com.topdon.module.thermal.ir.R

/**
 * 图库目录切换 PopupWindow.
 *
 * Created by LCG on 2024/1/5.
 */
class GalleryChangePopup(private val context: Context) : PopupWindow() {

    // View references using findViewById
    private val tvLine: TextView by lazy { contentView.findViewById(R.id.tv_line) }
    private val tvTs004: TextView by lazy { contentView.findViewById(R.id.tv_ts004) }
    private val tvTc007: TextView by lazy { contentView.findViewById(R.id.tv_tc007) }


    /**
     * 一个选项被选中事件监听.
     */
    var onPickListener: ((position: Int, str: String) -> Unit)? = null


    init {
        val widthMeasureSpec = MeasureSpec.makeMeasureSpec((context.resources.displayMetrics.widthPixels * 0.6).toInt(), MeasureSpec.EXACTLY)
        val heightMeasureSpec = MeasureSpec.makeMeasureSpec(context.resources.displayMetrics.heightPixels, MeasureSpec.AT_MOST)
        contentView = LayoutInflater.from(context).inflate(R.layout.popup_gallery_change, null)
        contentView.measure(widthMeasureSpec, heightMeasureSpec)

        width = contentView.measuredWidth
        height = contentView.measuredHeight

        isOutsideTouchable = true

        tvLine.setOnClickListener {
            dismiss()
            onPickListener?.invoke(0, context.getString(R.string.tc_has_line_device))
        }
        tvTs004.setOnClickListener {
            dismiss()
            onPickListener?.invoke(1, "TS004")
        }
        tvTc007.setOnClickListener {
            dismiss()
            onPickListener?.invoke(2, "TC007")
        }
    }

    fun show(anchor: View) {
        val locationArray = IntArray(2)
        anchor.getLocationInWindow(locationArray)

        val x = locationArray[0] + anchor.width / 2 - width / 2
        val y = locationArray[1] + anchor.height - SizeUtils.dp2px(5f)
        showAtLocation(anchor, Gravity.NO_GRAVITY, x, y)
    }
}