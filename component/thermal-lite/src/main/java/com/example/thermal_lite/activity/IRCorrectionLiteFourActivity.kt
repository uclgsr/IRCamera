package com.example.thermal_lite.activity

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ToastUtils
import com.example.thermal_lite.R
import com.example.thermal_lite.databinding.ActivityIrCorrectionLiteFourBinding
import com.example.thermal_lite.fragment.IRMonitorLiteFragment
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.module.thermal.ir.event.CorrectionFinishEvent
import com.topdon.module.thermal.ir.view.TimeDownView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

/**
 *
锅盖矫正
 * @author: CaiSongL
 * @date: 2023/8/4 9:06
 */
// Legacy ARouter route annotation - now using NavigationManager
class IRCorrectionLiteFourActivity : BaseActivity() {
    private lateinit var binding: ActivityIrCorrectionLiteFourBinding
    val time = 60
    var result = false

    override fun initContentView(): Int = R.layout.activity_ir_correction_lite_four

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
start矫正
            if (binding.timeDownView.downTimeWatcher == null)
                {
                    binding.timeDownView.setOnTimeDownListener(
                        object : TimeDownView.DownTimeWatcher {
                            override fun onTime(num: Int) {
                                if (num == 35)
                                    {
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            result = irFragment.autoStart()
                                        }
                                    }
                            }

                            override fun onLastTime(num: Int) {
                            }

                            override fun onLastTimeFinish(num: Int) {
                                try {
                                    if (!result)
                                        {
                                            ToastUtils.showShort("calibrationsavefailed，请重新calibration")
                                            return
                                        }
                                    if (!this@IRCorrectionLiteFourActivity.isFinishing)
                                        {
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

    override fun onBackPressed() {
        TipDialog.Builder(this)
            .setTitleMessage(getString(R.string.app_tip))
            .setMessage(R.string.tips_cancel_correction)
            .setPositiveListener(R.string.app_yes) {
                EventBus.getDefault().post(CorrectionFinishEvent())
                super.onBackPressed()
            }.setCancelListener(R.string.app_no) {
            }
            .create().show()
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
