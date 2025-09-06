package com.topdon.module.thermal.ir.activity

import android.content.Intent
import android.widget.TextView
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
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
class IRCorrectionActivity : BaseActivity() {

    override fun initContentView(): Int = R.layout.activity_ir_correction

    override fun initView() {
        findViewById<TextView>(R.id.tv_correction).setOnClickListener {
            val jumpIntent = Intent(this,IRCorrectionTwoActivity::class.java)
            jumpIntent.putExtra(ExtraKeyConfig.IS_TC007, intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false))
            startActivity(jumpIntent)
        }
    }

    override fun initData() {}

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun finishCorrection(event: CorrectionFinishEvent) {
        finish()
    }
}