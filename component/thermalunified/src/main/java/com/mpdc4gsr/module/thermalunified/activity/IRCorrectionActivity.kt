package com.mpdc4gsr.module.thermalunified.activity

import android.content.Intent
import android.widget.TextView
import com.mpdc4gsr.libunified.app.config.ExtraKeyConfig
import com.mpdc4gsr.libunified.app.ktbase.BaseActivity
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.event.CorrectionFinishEvent
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class IRCorrectionActivity : BaseActivity() {
    override fun initContentView(): Int = R.layout.activity_ir_correction

    override fun initView() {
        findViewById<TextView>(R.id.tv_correction).setOnClickListener {
            val jumpIntent = Intent(this, IRCorrectionTwoActivity::class.java)
            jumpIntent.putExtra(
                ExtraKeyConfig.IS_TC007,
                intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false)
            )
            startActivity(jumpIntent)
        }
    }

    override fun initData() {}

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun finishCorrection(event: CorrectionFinishEvent) {
        finish()
    }
}
