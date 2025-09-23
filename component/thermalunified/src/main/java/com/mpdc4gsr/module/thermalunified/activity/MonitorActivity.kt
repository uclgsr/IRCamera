package com.mpdc4gsr.module.thermalunified.activity

import android.view.View
import android.widget.Button
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.BarUtils
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.ktbase.BaseActivity
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import com.mpdc4gsr.module.thermalunified.stubs.MonitorSelectDialog
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.event.ThermalActionEvent
import org.greenrobot.eventbus.EventBus

class MonitorActivity : BaseActivity(), View.OnClickListener {
    companion object {
        const val STATS_START = 101
        const val STATS_MONITOR = 102
        const val STATS_FINISH = 103
    }

    var MONITOR_ACTION = STATS_START

    private var selectType = 1
    private var selectIndex: ArrayList<Int> = arrayListOf()

    override fun initContentView() = R.layout.activity_monitor

    override fun initView() {

        val toolbar =
            findViewById<androidx.appcompat.widget.Toolbar>(com.mpdc4gsr.libunified.R.id.toolbar_lay)
        toolbar?.title = getString(R.string.main_thermal_motion)

        val blackColor = ContextCompat.getColor(this, com.mpdc4gsr.libunified.R.color.black)
        toolbar?.setBackgroundColor(blackColor)
        BarUtils.setStatusBarColor(this, blackColor)
        BarUtils.setNavBarColor(window, blackColor)
        findViewById<Button>(R.id.motion_log_btn).setOnClickListener(this)
        findViewById<Button>(R.id.motion_btn).setOnClickListener(this)
        findViewById<Button>(R.id.motion_start_btn).setOnClickListener(this)


    }

    override fun initData() {
    }

    override fun onClick(v: View?) {
        when (v) {
            findViewById<Button>(R.id.motion_log_btn) -> {
                NavigationManager.getInstance().build(RouterConfig.LOG_MP_CHART).navigation(this)
            }

            findViewById<Button>(R.id.motion_btn) -> {
                MonitorSelectDialog.Builder(this)
                    .setPositiveListener { select ->
                        updateUI()
                        when (select) {
                            1 -> EventBus.getDefault().post(ThermalActionEvent(action = 2001))
                            2 -> EventBus.getDefault().post(ThermalActionEvent(action = 2002))
                            else ->
                                EventBus.getDefault()
                                    .post(ThermalActionEvent(action = 2003))
                        }
                    }
                    .create().show()
            }

            findViewById<Button>(R.id.motion_start_btn) -> {
                NavigationManager.getInstance().build(RouterConfig.MONITOR_CHART)
                    .withInt("type", selectType)
                    .navigation(this)
                finish()
            }
        }
    }

    fun select(
        selectType: Int,
        selectIndex: ArrayList<Int>,
    ) {
        findViewById<Button>(R.id.motion_start_btn).isEnabled = true
        this.selectType = selectType
        this.selectIndex = selectIndex
    }

    private fun updateUI() {
        val motionStartBtn = findViewById<Button>(R.id.motion_start_btn)
        val motionLogBtn = findViewById<Button>(R.id.motion_log_btn)
        val motionBtn = findViewById<Button>(R.id.motion_btn)
        motionStartBtn.isEnabled = false
        motionStartBtn.visibility = View.VISIBLE
        motionLogBtn.visibility = View.GONE
        motionBtn.visibility = View.GONE
    }

    fun updateTime(time: Long) {
        val ss = time % 60
        val mm = time / 60 % 60
        val ssStr = String.format("%02d", ss)
        val mmStr = String.format("%02d", mm)
        findViewById<Button>(R.id.motion_start_btn).text = "$mmStr:$ssStr"
    }
}
