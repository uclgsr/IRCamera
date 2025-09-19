package com.mpdc4gsr.module.thermal.ir.activity

import android.content.Intent
import android.widget.ImageView
import android.widget.TextView
import com.mpdc4gsr.lib.core.config.ExtraKeyConfig
import com.mpdc4gsr.lib.core.config.RouterConfig
import com.mpdc4gsr.lib.core.ktbase.BaseActivity
import com.mpdc4gsr.lib.core.navigation.NavigationManager
import com.mpdc4gsr.lib.core.socket.WebSocketProxy
import com.mpdc4gsr.lib.core.tools.DeviceTools
import com.mpdc4gsr.module.thermal.ir.R
import com.mpdc4gsr.module.thermal.ir.event.CorrectionFinishEvent
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode



class IRCorrectionTwoActivity : BaseActivity() {
    
    private var isTC007 = false

    private lateinit var tvCorrection: TextView

    override fun initContentView(): Int = R.layout.activity_ir_correction_two

    override fun initView() {
        isTC007 = intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false)

        val ivSketchMap = findViewById<ImageView>(R.id.iv_sketch_map)
        tvCorrection = findViewById(R.id.tv_correction)

        ivSketchMap.setImageResource(if (isTC007) R.drawable.ic_corrected_tc007 else R.drawable.ic_corrected_line)

        if (if (isTC007) WebSocketProxy.getInstance()
                .isTC007Connect() else DeviceTools.isConnect()
        ) {
            tvCorrection.setBackgroundResource(com.topdon.lib.core.R.drawable.bg_corners05_solid_theme)
        } else {
            tvCorrection.setBackgroundResource(com.topdon.lib.core.R.drawable.bg_corners05_solid_50_theme)
        }

        tvCorrection.setOnClickListener {
            if (if (isTC007) WebSocketProxy.getInstance()
                    .isTC007Connect() else DeviceTools.isConnect()
            ) {
                if (isTC007) {
                    NavigationManager.getInstance().build(RouterConfig.IR_CORRECTION_07)
                        .navigation(this)
                } else {
                    if (DeviceTools.isTC001LiteConnect()) {
                        NavigationManager.getInstance().build(RouterConfig.IR_CORRECTION_THREE_LITE)
                            .navigation(this)
                    } else if (DeviceTools.isHikConnect()) {
                        NavigationManager.getInstance().build(RouterConfig.IR_HIK_CORRECT_THREE)
                            .navigation(this)
                    } else {
                        startActivity(Intent(this, IRCorrectionThreeActivity::class.java))
                    }
                }
            }
        }
    }

    override fun connected() {
        if (!isTC007) {
            tvCorrection.setBackgroundResource(com.topdon.lib.core.R.drawable.bg_corners05_solid_theme)
        }
    }

    override fun disConnected() {
        if (!isTC007) {
            tvCorrection.setBackgroundResource(com.topdon.lib.core.R.drawable.bg_corners05_solid_50_theme)
        }
    }

    override fun onSocketConnected(isTS004: Boolean) {
        if (isTC007 && !isTS004) {
            tvCorrection.setBackgroundResource(com.topdon.lib.core.R.drawable.bg_corners05_solid_theme)
        }
    }

    override fun onSocketDisConnected(isTS004: Boolean) {
        if (isTC007 && !isTS004) {
            tvCorrection.setBackgroundResource(com.topdon.lib.core.R.drawable.bg_corners05_solid_50_theme)
        }
    }

    override fun initData() {}

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun finishCorrection(event: CorrectionFinishEvent) {
        finish()
    }
}
