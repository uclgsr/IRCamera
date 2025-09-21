package com.example.thermal_lite.activity

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ToastUtils
import com.example.thermal_lite.R
import com.example.thermal_lite.databinding.ActivityIrCorrectionLiteFourBinding
import com.example.thermal_lite.fragment.IRMonitorLiteFragment
import com.mpdc4gsr.libunified.app.dialog.TipDialog
import com.mpdc4gsr.libunified.app.ktbase.BaseActivity
import com.mpdc4gsr.module.thermal.ir.event.CorrectionFinishEvent
import com.mpdc4gsr.module.thermal.ir.view.TimeDownView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus


class IRCorrectionLiteFourActivity : BaseActivity() {
    private lateinit var binding: ActivityIrCorrectionLiteFourBinding
    val time = 60
    var result = false

    override fun initContentView(): Int = R.layout.activity_ir_correction_lite_four

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                TipDialog.Builder(this@IRCorrectionLiteFourActivity)
                    .setTitleMessage(getString(R.string.app_tip))
                    .setMessage(R.string.tips_cancel_correction)
                    .setPositiveListener(R.string.app_yes) {
                        EventBus.getDefault().post(CorrectionFinishEvent())
                        finish()
                    }.setCancelListener(R.string.app_no) {
                    }
                    .create().show()
            }
        })

        binding.titleView.setLeftClickListener {
            TipDialog.Builder(this)
                .setTitleMessage(getString(R.string.app_tip))
                .setMessage(R.string.tips_cancel_correction)
                .setPositiveListener(R.string.app_yes) {
                    EventBus.getDefault().post(CorrectionFinishEvent())
                    finish()
                }.setCancelListener(R.string.app_no) {
                }
                .create().show()
        }

        val irFragment =
            if (savedInstanceState == null) {
                IRMonitorLiteFragment()
            } else {
                supportFragmentManager.findFragmentById(R.id.fragment_container_view) as IRMonitorLiteFragment
            }
        lifecycleScope.launch {
            delay(1000)
            if (savedInstanceState == null) {
                supportFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.fragment_container_view, irFragment)
                    .commit()
            }
        }

        binding.timeDownView.postDelayed({

            if (binding.timeDownView.downTimeWatcher == null) {
                binding.timeDownView.setOnTimeDownListener(
                    object : TimeDownView.DownTimeWatcher {
                        override fun onTime(num: Int) {
                            if (num == 35) {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    result = irFragment.autoStart()
                                }
                            }
                        }

                        override fun onLastTime(num: Int) {
                        }

                        override fun onLastTimeFinish(num: Int) {
                            try {
                                if (!result) {
                                    ToastUtils.showShort("标定保存失败，请重新标定")
                                    return
                                }
                                if (!this@IRCorrectionLiteFourActivity.isFinishing) {
                                    TipDialog.Builder(this@IRCorrectionLiteFourActivity)
                                        .setMessage(R.string.correction_complete)
                                        .setPositiveListener(R.string.app_confirm) {
                                            EventBus.getDefault().post(CorrectionFinishEvent())
                                            finish()
                                        }
                                        .create().show()
                                }
                            } catch (e: Exception) {
                            }
                        }
                    },
                )
            }
            binding.timeDownView.downSecond(time, false)
        }, 2000)
    }

    override fun initView() {
        binding = ActivityIrCorrectionLiteFourBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }


    override fun disConnected() {
        super.disConnected()
        binding.timeDownView.cancel()
        EventBus.getDefault().post(CorrectionFinishEvent())
        finish()
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().post(CorrectionFinishEvent())
        finish()
    }

    override fun initData() {
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.timeDownView.cancel()
    }
}
