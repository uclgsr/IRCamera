package com.mpdc4gsr.module.thermalunified.activity

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.mpdc4gsr.libunified.app.dialog.TipDialog
import com.mpdc4gsr.libunified.app.ktbase.BaseActivity
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.event.CorrectionFinishEvent
import com.mpdc4gsr.module.thermalunified.fragment.IRCorrectionFragment
import com.mpdc4gsr.module.thermalunified.view.TimeDownView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus


class IRCorrectionFourActivity : BaseActivity() {
    val time = 60

    override fun initContentView(): Int = R.layout.activity_ir_correction_four

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<com.mpdc4gsr.libunified.app.view.TitleView>(R.id.title_view).setLeftClickListener {
            TipDialog.Builder(this)
                .setTitleMessage(getString(com.mpdc4gsr.libunified.R.string.app_tip))
                .setMessage(com.mpdc4gsr.libunified.R.string.tips_cancel_correction)
                .setPositiveListener(com.mpdc4gsr.libunified.R.string.app_yes) {
                    EventBus.getDefault().post(CorrectionFinishEvent())
                    finish()
                }.setCancelListener(com.mpdc4gsr.libunified.R.string.app_no) {
                }
                .create().show()
        }

        val irFragment =
            if (savedInstanceState == null) {
                IRCorrectionFragment()
            } else {
                supportFragmentManager.findFragmentById(R.id.fragment_container_view) as IRCorrectionFragment
            }
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.fragment_container_view, irFragment)
                .commit()
        }

        val timeDownView = findViewById<TimeDownView>(R.id.time_down_view)
        timeDownView.postDelayed({

            if (timeDownView.downTimeWatcher == null) {
                timeDownView.setOnTimeDownListener(
                    object : TimeDownView.DownTimeWatcher {
                        override fun onTime(num: Int) {
                            if (num == 50) {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    irFragment.autoStart()
                                }
                            }
                        }

                        override fun onLastTime(num: Int) {
                        }

                        override fun onLastTimeFinish(num: Int) {
                            try {
                                if (!this@IRCorrectionFourActivity.isFinishing) {
                                    TipDialog.Builder(this@IRCorrectionFourActivity)
                                        .setMessage(com.mpdc4gsr.libunified.R.string.correction_complete)
                                        .setPositiveListener(com.mpdc4gsr.libunified.R.string.app_confirm) {
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
            timeDownView.downSecond(time, false)
        }, 2000)
    }

    override fun initView() {
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        TipDialog.Builder(this)
            .setTitleMessage(getString(com.mpdc4gsr.libunified.R.string.app_tip))
            .setMessage(com.mpdc4gsr.libunified.R.string.tips_cancel_correction)
            .setPositiveListener(com.mpdc4gsr.libunified.R.string.app_yes) {
                EventBus.getDefault().post(CorrectionFinishEvent())
                @Suppress("DEPRECATION")
                super.onBackPressed()
            }.setCancelListener(com.mpdc4gsr.libunified.R.string.app_no) {
            }
            .create().show()
    }

    override fun disConnected() {
        super.disConnected()
        findViewById<TimeDownView>(R.id.time_down_view).cancel()
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
        findViewById<TimeDownView>(R.id.time_down_view).cancel()
    }
}
