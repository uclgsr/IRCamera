package com.topdon.module.thermal.ir.activity

import android.content.Intent
import android.widget.ImageView
import android.widget.TextView
import com.topdon.lib.core.navigation.NavigationManager
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.socket.WebSocketProxy
import com.topdon.lib.core.tools.DeviceTools
import com.topdon.module.thermal.ir.R
import com.topdon.module.thermal.ir.event.CorrectionFinishEvent
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 *
 * 锅盖矫正
 * @author: CaiSongL
 * @date: 2023/8/4 9:06
 *
 * 需要传递参数：
 * - [ExtraKeyConfig.IS_TC007] - 当前设备是否为 TC007
 */
// Legacy ARouter route annotation - now using NavigationManager
class IRCorrectionTwoActivity : BaseActivity() {

    /**
     * From上一界面传递过来的，当前是否为 TC007 设备类型.
     * true-TC007 false-其他插件式设备
     */
    private var isTC007 = false
    
    // Modern findViewById references
    private lateinit var tvCorrection: TextView

    override fun initContentView(): Int = R.layout.activity_ir_correction_two

    override fun initView() {
        isTC007 = intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false)

        val ivSketchMap = findViewById<ImageView>(R.id.iv_sketch_map)
        tvCorrection = findViewById(R.id.tv_correction)
        
        ivSketchMap.setImageResource(if (isTC007) R.drawable.ic_corrected_tc007 else R.drawable.ic_corrected_line)

        if (if (isTC007) WebSocketProxy.getInstance().isTC007Connect() else DeviceTools.isConnect()) {
            tvCorrection.setBackgroundResource(com.topdon.lib.core.R.drawable.bg_corners05_solid_theme)
        } else {
            tvCorrection.setBackgroundResource(com.topdon.lib.core.R.drawable.bg_corners05_solid_50_theme)
        }

        tvCorrection.setOnClickListener {
            if (if (isTC007) WebSocketProxy.getInstance().isTC007Connect() else DeviceTools.isConnect()) {
                if (isTC007) {
                    NavigationManager.getInstance().build(RouterConfig.IR_CORRECTION_07).navigation(this)
                } else {
                    if (DeviceTools.isTC001LiteConnect()){
                        NavigationManager.getInstance().build(RouterConfig.IR_CORRECTION_THREE_LITE).navigation(this)
                    } else if (DeviceTools.isHikConnect()) {
                        NavigationManager.getInstance().build(RouterConfig.IR_HIK_CORRECT_THREE).navigation(this)
                    } else{
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