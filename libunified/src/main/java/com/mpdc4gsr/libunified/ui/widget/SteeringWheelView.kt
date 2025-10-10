package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.OnClickListener
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.mpdc4gsr.libunified.R

class SteeringWheelView :
    LinearLayout,
    OnClickListener {
    private lateinit var tvConfirm: TextView
    private lateinit var steeringWheelStartBtn: ImageView
    private lateinit var steeringWheelCenterBtn: ImageView
    private lateinit var steeringWheelEndBtn: ImageView
    var listener: ((action: Int, moveX: Int) -> Unit)? = null
    var moveX = 30
    var rotationIR = 270
        set(value) {
            field = value
            if (value == 270 || value == 90) {
                if (::tvConfirm.isInitialized) tvConfirm.rotation = 270f
                rotation = 90f
            } else {
                if (::tvConfirm.isInitialized) tvConfirm.rotation = 0f
                rotation = 0f
            }
            requestLayout()
        }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    )

    private fun initView() {
        inflate(context, R.layout.ui_steering_wheel_view, this)
        tvConfirm = findViewById(R.id.tv_confirm)
        steeringWheelStartBtn = findViewById(R.id.steering_wheel_start_btn)
        steeringWheelCenterBtn = findViewById(R.id.steering_wheel_center_btn)
        steeringWheelEndBtn = findViewById(R.id.steering_wheel_end_btn)
        steeringWheelStartBtn.setOnClickListener(this)
        steeringWheelCenterBtn.setOnClickListener(this)
        steeringWheelEndBtn.setOnClickListener(this)
        if (rotationIR == 270 || rotationIR == 90) {
            tvConfirm.rotation = 270f
            rotation = 90f
        } else {
            tvConfirm.rotation = 0f
            rotation = 0f
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            steeringWheelStartBtn -> {
                moveX += 10
                if (moveX > 60) {
                    moveX = 60
                }
                listener?.invoke(-1, moveX)
            }

            steeringWheelCenterBtn -> {
                listener?.invoke(0, moveX)
            }

            steeringWheelEndBtn -> {
                moveX -= 10
                if (moveX < -20) {
                    moveX = -20
                }
                listener?.invoke(1, moveX)
            }
        }
    }
}
