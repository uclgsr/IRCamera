package com.topdon.lib.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.widget.LinearLayout
import com.topdon.lib.ui.databinding.UiWifiSteeringWheelViewBinding

/**
 * 校准方向
 */
class WifiSteeringWheelView : LinearLayout, OnClickListener {
    private val binding: UiWifiSteeringWheelViewBinding

    var listener: ((action: Int, moveX: Int, moveY: Int) -> Unit)? = null
    var moveX = 0
    var moveY = 0

    private val steeringWheelStartBtn by lazy { binding.steeringWheelStartBtn }
    private val steeringWheelCenterBtn by lazy { binding.steeringWheelCenterBtn }
    private val steeringWheelEndBtn by lazy { binding.steeringWheelEndBtn }
    private val steeringWheelTopBtn by lazy { binding.steeringWheelTopBtn }
    private val steeringWheelBottomBtn by lazy { binding.steeringWheelBottomBtn }
    private val tvConfirm by lazy { binding.tvConfirm }

    var rotationIR = 270
        set(value) {
            field = value
            if (value == 270 || value == 90) {
                tvConfirm?.rotation = 270f
                rotation = 90f
            } else {
                tvConfirm?.rotation = 0f
                rotation = 0f
            }
            requestLayout()
        }

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        binding = UiWifiSteeringWheelViewBinding.inflate(LayoutInflater.from(context), this, true)
        initView()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    ) {
        binding = UiWifiSteeringWheelViewBinding.inflate(LayoutInflater.from(context), this, true)
        initView()
    }

    private fun initView() {
        // Views are already inflated via binding in constructor
        steeringWheelStartBtn.setOnClickListener(this)
        steeringWheelCenterBtn.setOnClickListener(this)
        steeringWheelEndBtn.setOnClickListener(this)
        steeringWheelTopBtn.setOnClickListener(this)
        steeringWheelBottomBtn.setOnClickListener(this)
        if (rotationIR == 270 || rotationIR == 90) {
            tvConfirm.rotation = 270f
            rotation = 90f
        } else {
            tvConfirm.rotation = 0f
            rotation = 0f
        }
    }

    val moveI = 2

    override fun onClick(v: View?) {
        when (v) {
            steeringWheelStartBtn -> {
//                moveY -= moveI
                listener?.invoke(-1, moveX, moveY)
            }
            steeringWheelCenterBtn -> {
                listener?.invoke(0, moveX, moveY)
            }
            steeringWheelTopBtn -> {
//                moveX += moveI
                listener?.invoke(2, moveX, moveY)
            }
            steeringWheelBottomBtn -> {
//                moveX -= moveI
                listener?.invoke(3, moveX, moveY)
            }
            steeringWheelEndBtn -> {
//                moveY += moveI
                listener?.invoke(1, moveX, moveY)
            }
        }
    }
}
