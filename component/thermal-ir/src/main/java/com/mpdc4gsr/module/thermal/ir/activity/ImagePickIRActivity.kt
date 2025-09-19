package com.mpdc4gsr.module.thermal.ir.activity

import android.graphics.Bitmap
import com.mpdc4gsr.lib.core.ktbase.BasePickImgActivity
import com.topdon.module.thermal.ir.R
import com.topdon.module.thermal.ir.fragment.IRMonitorThermalFragment




class ImagePickIRActivity : BasePickImgActivity() {
    var irFragment: IRMonitorThermalFragment? = null

    override fun initView() {
        irFragment =
            if (savedInstanceState == null) {
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
