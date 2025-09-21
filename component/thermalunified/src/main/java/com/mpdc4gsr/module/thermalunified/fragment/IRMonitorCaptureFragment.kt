package com.mpdc4gsr.module.thermalunified.fragment

import android.content.Intent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.airbnb.lottie.LottieAnimationView
import com.mpdc4gsr.lib.core.config.ExtraKeyConfig
import com.mpdc4gsr.lib.core.config.RouterConfig
import com.mpdc4gsr.lib.core.ktbase.BaseFragment
import com.mpdc4gsr.lib.core.navigation.NavigationManager
import com.mpdc4gsr.lib.core.socket.WebSocketProxy
import com.mpdc4gsr.lib.core.tools.DeviceTools
import com.mpdc4gsr.lib.core.tools.ToastTools
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.activity.IRMonitorActivity

class IRMonitorCaptureFragment : BaseFragment() {

    private var isTC007 = false

    private lateinit var animationView: LottieAnimationView
    private lateinit var viewStart: View
    private lateinit var ivIcon: ImageView
    private lateinit var tvStart: TextView

    override fun initContentView(): Int = R.layout.fragment_ir_monitor_capture

    override fun initView() {
        isTC007 = arguments?.getBoolean(ExtraKeyConfig.IS_TC007, false) ?: false

        animationView = requireView().findViewById(R.id.animation_view)
        viewStart = requireView().findViewById(R.id.view_start)
        ivIcon = requireView().findViewById(R.id.iv_icon)
        tvStart = requireView().findViewById(R.id.tv_start)

        animationView.setAnimation(if (isTC007) "TC007AnimationJSON.json" else "TDAnimationJSON.json")

        viewStart.setOnClickListener {
            if (isTC007) {
                if (WebSocketProxy.getInstance().isTC007Connect()) {
                    NavigationManager.getInstance().build(RouterConfig.IR_MONITOR_CAPTURE_07)
                        .navigation(requireContext())
                } else {
                    ToastTools.showShort(R.string.device_connect_tip)
                }
            } else {
                if (DeviceTools.isConnect()) {
                    if (DeviceTools.isTC001LiteConnect()) {
                        NavigationManager.getInstance().build(RouterConfig.IR_THERMAL_MONITOR_LITE)
                            .navigation(requireContext())
                    } else if (DeviceTools.isHikConnect()) {
                        NavigationManager.getInstance().build(RouterConfig.IR_HIK_MONITOR_CAPTURE1)
                            .navigation(requireContext())
                    } else {
                        startActivity(Intent(requireContext(), IRMonitorActivity::class.java))
                    }
                } else {
                    ToastTools.showShort(R.string.device_connect_tip)
                }
            }
        }

        refreshUI(
            if (isTC007) WebSocketProxy.getInstance().isTC007Connect() else DeviceTools.isConnect()
        )
    }

    override fun onResume() {
        super.onResume()
        refreshUI(
            if (isTC007) WebSocketProxy.getInstance().isTC007Connect() else DeviceTools.isConnect()
        )
    }

    override fun initData() {
    }

    private fun refreshUI(isConnect: Boolean) {
        animationView.isVisible = !isConnect
        ivIcon.isVisible = isConnect
        viewStart.isVisible = isConnect
        tvStart.isVisible = isConnect
    }

    override fun connected() {
        if (!isTC007) {
            refreshUI(true)
        }
    }

    override fun disConnected() {
        if (!isTC007) {
            refreshUI(false)
        }
    }

    override fun onSocketConnected(isTS004: Boolean) {
        if (isTC007 && !isTS004) {
            refreshUI(true)
        }
    }

    override fun onSocketDisConnected(isTS004: Boolean) {
        if (isTC007 && !isTS004) {
            refreshUI(false)
        }
    }
}
