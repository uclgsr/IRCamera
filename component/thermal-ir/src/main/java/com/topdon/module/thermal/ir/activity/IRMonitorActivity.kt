package com.topdon.module.thermal.ir.activity

import android.view.View
import android.widget.Button
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.ui.dialog.MonitorSelectDialog
import com.topdon.libcom.navigation.NavigationManager
import com.topdon.module.thermal.ir.R
import com.topdon.module.thermal.ir.bean.SelectPositionBean
import com.topdon.module.thermal.ir.event.MonitorSaveEvent
import com.topdon.module.thermal.ir.event.ThermalActionEvent
import org.greenrobot.eventbus.EventBus

/**
 * 选取区域监听
 */
class IRMonitorActivity : BaseActivity(), View.OnClickListener {

    private var selectIndex: SelectPositionBean? = null//选取点

    override fun initContentView() = R.layout.activity_ir_monitor

    override fun initView() {
        findViewById<Button>(R.id.motion_btn).setOnClickListener(this)
        findViewById<Button>(R.id.motion_start_btn).setOnClickListener(this)
    }

    override fun initData() {

    }

    override fun onClick(v: View?) {
        when (v) {
            findViewById<Button>(R.id.motion_btn) -> {
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
            findViewById<Button>(R.id.motion_start_btn) -> {
                if (selectIndex == null) {
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
                    return
                }
                //开始温度监听
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
        findViewById<Button>(R.id.motion_start_btn).visibility = View.VISIBLE
        findViewById<Button>(R.id.motion_btn).visibility = View.GONE
    }

    override fun disConnected() {
        super.disConnected()
        finish()
    }


}