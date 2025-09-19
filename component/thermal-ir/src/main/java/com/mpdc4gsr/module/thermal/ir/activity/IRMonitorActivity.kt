package com.mpdc4gsr.module.thermal.ir.activity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.mpdc4gsr.lib.core.config.RouterConfig
import com.topdon.lib.ui.dialog.MonitorSelectDialog
import com.topdon.libcom.navigation.NavigationManager
import com.topdon.module.thermal.ir.bean.SelectPositionBean
import com.topdon.module.thermal.ir.databinding.ActivityIrMonitorBinding
import com.topdon.module.thermal.ir.event.ThermalActionEvent
import org.greenrobot.eventbus.EventBus

class IRMonitorActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var binding: ActivityIrMonitorBinding
    private var selectIndex: SelectPositionBean? = null 

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIrMonitorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
    }

    private fun initView() {
        binding.motionBtn.setOnClickListener(this)
        binding.motionStartBtn.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.motionBtn -> {
                MonitorSelectDialog.Builder(this)
                    .setPositiveListener {
                        updateUI()
                        when (it) {
                            1 -> EventBus.getDefault().post(ThermalActionEvent(action = 2001))
                            2 -> EventBus.getDefault().post(ThermalActionEvent(action = 2002))
                            else -> EventBus.getDefault().post(ThermalActionEvent(action = 2003))
                        }
                    }
                    .create().show()
            }

            binding.motionStartBtn -> {
                if (selectIndex == null) {
                    MonitorSelectDialog.Builder(this)
                        .setPositiveListener {
                            updateUI()
                            when (it) {
                                1 -> EventBus.getDefault().post(ThermalActionEvent(action = 2001))
                                2 -> EventBus.getDefault().post(ThermalActionEvent(action = 2002))
                                else -> EventBus.getDefault()
                                    .post(ThermalActionEvent(action = 2003))
                            }
                        }
                        .create().show()
                    return
                }

                NavigationManager.getInstance().build(RouterConfig.IR_MONITOR_CHART)
                    .withParcelable("select", selectIndex as android.os.Parcelable)
                    .navigation(this)
                finish()
            }
        }
    }

    fun select(selectIndex: SelectPositionBean?) {
        this.selectIndex = selectIndex
    }

    private fun updateUI() {
        binding.motionStartBtn.visibility = View.VISIBLE
        binding.motionBtn.visibility = View.GONE
    }

    private fun disConnected() {
        finish()
    }
}
