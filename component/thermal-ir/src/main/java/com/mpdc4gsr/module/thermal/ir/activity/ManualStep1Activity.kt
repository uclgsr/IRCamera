package com.mpdc4gsr.module.thermal.ir.activity

import android.content.Intent
import android.widget.TextView
import com.mpdc4gsr.libunified.app.ktbase.BaseActivity
import com.mpdc4gsr.module.thermal.ir.R
import com.mpdc4gsr.module.thermal.ir.event.ManualFinishBean
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class ManualStep1Activity : BaseActivity() {

    private lateinit var tvManual: TextView

    override fun initContentView(): Int = R.layout.activity_manual_step1

    override fun initView() {

        tvManual = findViewById(R.id.tv_manual)

        tvManual.setOnClickListener {
            startActivity(Intent(this, ManualStep2Activity::class.java))
        }
    }

    override fun initData() {
    }

    override fun disConnected() {
        super.disConnected()
        finish()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onManualFinishBean(manualFinishBean: ManualFinishBean) {
        finish()
    }
}
