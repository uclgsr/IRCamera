package com.mpdc4gsr.module.thermalunified.lite.activity

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.mpdc4gsr.module.thermalunified.lite.R
import com.mpdc4gsr.module.thermalunified.lite.databinding.ActivityIrCorrectionLiteThreeBinding
import com.mpdc4gsr.module.thermalunified.lite.fragment.IRMonitorLiteFragment
import com.mpdc4gsr.lib.core.ktbase.BaseActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class IRCorrectionLiteThreeActivity : BaseActivity() {
    private lateinit var binding: ActivityIrCorrectionLiteThreeBinding

    override fun initContentView(): Int = R.layout.activity_ir_correction_lite_three

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val fragment: IRMonitorLiteFragment =
            if (savedInstanceState == null) {
                IRMonitorLiteFragment()
            } else {
                supportFragmentManager.findFragmentById(R.id.fragment_container_view) as IRMonitorLiteFragment
            }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.fragment_container_view, fragment)
                .commit()
        }

        binding.tvCorrection.setOnClickListener {
            lifecycleScope.launch {
                if (fragment.frameReady) {
                    fragment.closeFragment()
                    showCameraLoading()
                    delay(1000)
                    dismissCameraLoading()
                    val intent =
                        Intent(
                            this@IRCorrectionLiteThreeActivity,
                            IRCorrectionLiteFourActivity::class.java,
                        )
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    override fun initView() {
        binding = ActivityIrCorrectionLiteThreeBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun initData() {}
}
