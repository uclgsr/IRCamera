package com.topdon.house.popup

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupWindow
import androidx.core.view.isVisible
import com.blankj.utilcode.util.SizeUtils
import com.topdon.house.R

/**
 * 房屋检测的选项 Popup 最多 3 个选项，就不跟 TC003 一样搞列表了。
 *
 * Created by LCG on 2024/8/23.
 */
internal class ThreePickPopup(val context: Context, strIdArray: List<Int>, private var onPickListener: (position: Int) -> Unit) : PopupWindow(), View.OnClickListener {

    init {
        contentView = LayoutInflater.from(context).inflate(R.layout.popup_three_pick, null)
        val tvOption1 = contentView.findViewById<android.widget.TextView>(R.id.tv_option1)
        val tvOption2 = contentView.findViewById<android.widget.TextView>(R.id.tv_option2)
        val tvOption3 = contentView.findViewById<android.widget.TextView>(R.id.tv_option3)
        val viewLine2 = contentView.findViewById<android.view.View>(R.id.view_line2)
        
        tvOption1.text = context.getString(strIdArray[0])
        tvOption2.text = context.getString(strIdArray[1])
        if (strIdArray.size >= 3) {
            tvOption3.text = context.getString(strIdArray[2])
        } else {
            tvOption3.isVisible = false
            viewLine2.isVisible = false
        }


        val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec((context.resources.displayMetrics.widthPixels * 0.42).toInt(), View.MeasureSpec.EXACTLY)
        val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(context.resources.displayMetrics.heightPixels, View.MeasureSpec.AT_MOST)
        contentView.measure(widthMeasureSpec, heightMeasureSpec)

        width = contentView.measuredWidth
        height = contentView.measuredHeight

        isOutsideTouchable = true

        tvOption1.setOnClickListener(this)
        tvOption2.setOnClickListener(this)
        tvOption3.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        val tvOption1 = contentView.findViewById<android.widget.TextView>(R.id.tv_option1)
        val tvOption2 = contentView.findViewById<android.widget.TextView>(R.id.tv_option2)
        val tvOption3 = contentView.findViewById<android.widget.TextView>(R.id.tv_option3)
        
        when (v) {
            tvOption1 -> onPickListener.invoke(0)
            tvOption2 -> onPickListener.invoke(1)
            tvOption3 -> onPickListener.invoke(2)
        }
        dismiss()
    }

    /**
     * 显示
     * @param isLeft true-左对齐 false-右对齐
     */
    fun show(anchor: View, isLeft: Boolean) {
        val heightPixels = context.resources.displayMetrics.heightPixels
        val locationArray = IntArray(2)
        anchor.getLocationInWindow(locationArray)

        val x = if (isLeft) locationArray[0] else locationArray[0] + anchor.width + SizeUtils.dp2px(17f) - width

        if (isLeft) {
            if (locationArray[1] >= height) {//在 anchor 上面放得下
                showAtLocation(anchor, Gravity.NO_GRAVITY, x, locationArray[1] - height)
            } else {//上面放不下就放下面吧
                showAsDropDown(anchor, Gravity.NO_GRAVITY, x, locationArray[1] + anchor.height)
            }
        } else {
            if (heightPixels - locationArray[1] - anchor.height - SizeUtils.dp2px(10f) > height) {//在 anchor 底部放得下
                showAtLocation(anchor, Gravity.NO_GRAVITY, x, locationArray[1] + anchor.height + SizeUtils.dp2px(10f))
            } else {//下面放不下就放上面吧
                showAtLocation(anchor, Gravity.NO_GRAVITY, x, (locationArray[1] - SizeUtils.dp2px(10f) - height).coerceAtLeast(0))
            }
        }
    }
}