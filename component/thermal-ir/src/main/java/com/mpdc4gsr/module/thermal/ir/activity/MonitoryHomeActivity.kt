package com.mpdc4gsr.module.thermal.ir.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.mpdc4gsr.lib.core.config.ExtraKeyConfig
import com.mpdc4gsr.lib.core.ktbase.BaseActivity
import com.mpdc4gsr.module.thermal.ir.R
import com.mpdc4gsr.module.thermal.ir.event.MonitorSaveEvent
import com.mpdc4gsr.module.thermal.ir.fragment.IRMonitorCaptureFragment
import com.mpdc4gsr.module.thermal.ir.fragment.IRMonitorHistoryFragment
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class MonitoryHomeActivity : BaseActivity() {
    override fun initContentView(): Int = R.layout.activity_monitor_home

    override fun initView() {
        val isTC007 = intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false)
        val viewPager2 = findViewById<ViewPager2>(R.id.view_pager2)
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)

        viewPager2.adapter = ViewPagerAdapter(this, isTC007)
        TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
            tab.setText(if (position == 0) R.string.chart_history else R.string.chart_real_time)
        }.attach()
    }

    override fun initData() {
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMonitorCreate(event: MonitorSaveEvent) {
        findViewById<ViewPager2>(R.id.view_pager2).currentItem = 0
    }

    private class ViewPagerAdapter(activity: MonitoryHomeActivity, val isTC007: Boolean) :
        FragmentStateAdapter(activity) {
        override fun getItemCount() = 2

        override fun createFragment(position: Int): Fragment {
            return if (position == 0) {
                IRMonitorHistoryFragment()
            } else {
                val fragment = IRMonitorCaptureFragment()
                fragment.arguments =
                    Bundle().also { it.putBoolean(ExtraKeyConfig.IS_TC007, isTC007) }
                fragment
            }
        }
    }
}
