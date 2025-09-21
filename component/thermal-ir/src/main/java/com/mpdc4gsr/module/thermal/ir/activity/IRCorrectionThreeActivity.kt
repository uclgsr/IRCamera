package com.mpdc4gsr.module.thermal.ir.activity

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import com.mpdc4gsr.libunified.app.ktbase.BaseActivity
import com.mpdc4gsr.module.thermal.ir.R
import com.mpdc4gsr.module.thermal.ir.fragment.IRCorrectionFragment


class IRCorrectionThreeActivity : BaseActivity() {
    override fun initContentView(): Int = R.layout.activity_ir_correction_three

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fragment: IRCorrectionFragment =
            if (savedInstanceState == null) {
                IRCorrectionFragment()
            } else {
                supportFragmentManager.findFragmentById(R.id.fragment_container_view) as IRCorrectionFragment
            }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.fragment_container_view, fragment)
                .commit()
        }

        findViewById<TextView>(R.id.tv_correction).setOnClickListener {
            if (fragment.frameReady) {
                val intent = Intent(this, IRCorrectionFourActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    override fun initView() {
    }

    override fun initData() {}
}
