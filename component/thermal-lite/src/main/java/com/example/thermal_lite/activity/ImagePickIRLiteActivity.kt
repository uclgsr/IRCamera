package com.example.thermal_lite.activity

import android.graphics.Bitmap
import com.example.thermal_lite.fragment.IRMonitorLiteFragment
import com.mpdc4gsr.lib.core.ktbase.BasePickImgActivity
import com.topdon.module.thermal.ir.R



class ImagePickIRLiteActivity : BasePickImgActivity() {
    var irFragment: IRMonitorLiteFragment? = null

    override fun initView() {
        irFragment =
            if (savedInstanceState == null) {
                IRMonitorLiteFragment.newInstance(true)
            } else {
                supportFragmentManager.findFragmentById(R.id.fragment_container_view) as IRMonitorLiteFragment
            }
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.fragment_container_view, irFragment!!)
                .commit()
        }
    }

    override suspend fun getPickBitmap(): Bitmap? {
        return irFragment?.getBitmap()
    }

    override fun initData() {
    }
}
