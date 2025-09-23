package com.mpdc4gsr.module.thermalunified.activity

import android.graphics.Bitmap
import com.mpdc4gsr.libunified.app.ktbase.BasePickImgActivity
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.fragment.IRPlushFragment


class ImagePickIRPlushActivity : BasePickImgActivity() {
    var irFragment: IRPlushFragment? = null

    override fun initView() {
        irFragment =
            if (savedInstanceState == null) {
                IRPlushFragment()
            } else {
                supportFragmentManager.findFragmentById(R.id.fragment_container_view) as IRPlushFragment
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
