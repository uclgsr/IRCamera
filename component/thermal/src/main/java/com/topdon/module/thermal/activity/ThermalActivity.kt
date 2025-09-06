package com.topdon.module.thermal.activity

import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.BarUtils
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
import androidx.core.content.ContextCompat
import com.topdon.lib.core.R as LibR
import com.topdon.menu.MenuFirstTabView
import com.topdon.module.thermal.R
import com.topdon.module.thermal.adapter.MenuTabAdapter
import com.topdon.module.thermal.fragment.event.ThermalActionEvent
import org.greenrobot.eventbus.EventBus

// Legacy ARouter route annotation - now using NavigationManager
class ThermalActivity : BaseActivity() {

    private val menuAdapter by lazy { MenuTabAdapter(this) }

    override fun initContentView() = R.layout.activity_thermal

    override fun initView() {
        // Set toolbar title
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(com.topdon.lib.core.R.id.toolbar_lay)
        toolbar?.title = getString(R.string.main_thermal)
        
        val blackColor = ContextCompat.getColor(this, LibR.color.black)
        toolbar?.setBackgroundColor(blackColor)
        BarUtils.setStatusBarColor(this, blackColor)
        BarUtils.setNavBarColor(window, blackColor)
        initRecycler()
        
        val thermalTab = findViewById<MenuFirstTabView>(R.id.thermal_tab)
        thermalTab.onTabClickListener = { view ->
            //一级菜单选择
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
        menuAdapter.listener = object : MenuTabAdapter.OnItemClickListener {
            override fun onClick(index: Int) {
                //二级菜单选择
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