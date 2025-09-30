package com.mpdc4gsr.module.thermalunified.activity

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import com.mpdc4gsr.libunified.app.ktbase.BaseActivity
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.fragment.IRCorrectionFragmentCompose


class IRCorrectionThreeActivity : BaseActivity() {
    override fun initContentView(): Int = R.layout.activity_ir_correction_three

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fragment: IRCorrectionFragmentCompose =
            if (savedInstanceState == null) {
                IRCorrectionFragmentCompose()
            } else {
                supportFragmentManager.findFragmentById(R.id.fragment_container_view) as IRCorrectionFragmentCompose
            }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.fragment_container_view, fragment)
                .commit()
        }

        findViewById<TextView>(R.id.tv_correction).setOnClickListener {
            if (fragment.frameReady) {
                val intent = Intent(this, IRCorrectionFourComposeActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    override fun initView() {
    }

    override fun initData() {}
}
