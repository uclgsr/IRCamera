package com.mpdc4gsr.module.thermal.activity

import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.BarUtils
import com.mpdc4gsr.lib.core.ktbase.BaseActivity
import com.mpdc4gsr.lib.core.menu.MenuFirstTabView
import com.mpdc4gsr.module.thermal.ir.R
import com.mpdc4gsr.module.thermal.ir.adapter.MenuTabAdapter
import com.mpdc4gsr.module.thermal.ir.event.ThermalActionEvent
import org.greenrobot.eventbus.EventBus
import com.mpdc4gsr.lib.core.R as LibR

class ThermalActivity : BaseActivity() {
    private val menuAdapter by lazy { MenuTabAdapter(this) }

    override fun initContentView() = R.layout.activity_thermal

    override fun initView() {

        val toolbar =
            findViewById<androidx.appcompat.widget.Toolbar>(com.mpdc4gsr.lib.core.R.id.toolbar_lay)
        toolbar?.title = getString(R.string.main_thermal)

        val blackColor = ContextCompat.getColor(this, LibR.color.black)
        toolbar?.setBackgroundColor(blackColor)
        BarUtils.setStatusBarColor(this, blackColor)
        BarUtils.setNavBarColor(window, blackColor)
        initRecycler()

        val thermalTab = findViewById<MenuFirstTabView>(R.id.thermal_tab)
        thermalTab.onTabClickListener = { view ->

            showRecycler(view.selectPosition)
        }
    }

    override fun initData() {
    }

    private fun initRecycler() {
        val thermalRecycler = findViewById<RecyclerView>(R.id.thermal_recycler)
        thermalRecycler.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        thermalRecycler.adapter = menuAdapter
        thermalRecycler.visibility = View.GONE
        menuAdapter.initType(1)
        menuAdapter.listener =
            object : MenuTabAdapter.OnItemClickListener {
                override fun onClick(index: Int) {

                    Log.w("123", "index: $index")
                    EventBus.getDefault().post(ThermalActionEvent(action = index))
                }
            }
    }

    fun showRecycler(select: Int) {
        val thermalRecycler = findViewById<RecyclerView>(R.id.thermal_recycler)
        menuAdapter.initType(select)
        if (select == 5) {
            thermalRecycler.visibility = View.GONE
            EventBus.getDefault().post(ThermalActionEvent(action = 5000))
        } else {
            thermalRecycler.visibility = View.VISIBLE
        }
    }
}
