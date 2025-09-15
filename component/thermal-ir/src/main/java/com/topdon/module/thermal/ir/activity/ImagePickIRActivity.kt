package com.topdon.module.thermal.ir.activity

import android.graphics.Bitmap
import android.view.View
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BasePickImgActivity
import com.topdon.module.thermal.ir.R
import com.topdon.module.thermal.ir.fragment.IRMonitorThermalFragment

/**
    * des:单光红外拍照
    * author: CaiSongL
    * date: 2024/8/24 18:10
    **/
// Legacy ARouter route annotation - now using NavigationManager
class ImagePickIRActivity : BasePickImgActivity() {

    var irFragment : IRMonitorThermalFragment ?= null

    override fun initView() {
    irFragment = if (savedInstanceState == null) {
    IRMonitorThermalFragment.newInstance(true)
    } else {
    supportFragmentManager.findFragmentById(R.id.fragment_container_view) as IRMonitorThermalFragment
    }
    if (savedInstanceState == null) {
    supportFragmentManager.beginTransaction()
    .setReorderingAllowed(true)
    .add(R.id.fragment_container_view, irFragment!!)
    .commit()
    }

    }

    override suspend fun getPickBitmap(): Bitmap? {
    return irFragment?.getBitmap() ?: null
    }

    override fun initData() {

    }


}