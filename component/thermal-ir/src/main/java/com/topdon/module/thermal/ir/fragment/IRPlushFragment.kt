package com.topdon.module.thermal.ir.fragment

import android.graphics.Bitmap
import android.view.SurfaceView
import android.view.View
import com.infisense.usbdual.Const
import com.infisense.usbdual.camera.DualViewWithExternalCameraCommonApi
import com.infisense.usbir.view.TemperatureView
import com.topdon.module.thermal.ir.R
import com.topdon.module.thermal.ir.activity.BaseIRPlushFragment

/**
 * des:
 * author: CaiSongL
 * date: 2024/9/3 11:43
 **/
class IRPlushFragment : BaseIRPlushFragment() {

    // findViewById declarations using proper view reference in onViewCreated
    private lateinit var dualTextureViewNativeCamera: SurfaceView
    private lateinit var temperatureView: TemperatureView
    
    override fun onViewCreated(view: android.view.View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Initialize findViewById in onViewCreated
        dualTextureViewNativeCamera = view.findViewById(R.id.dualTextureViewNativeCamera)
        temperatureView = view.findViewById(R.id.temperature_view)
    }

    override fun getSurfaceView(): SurfaceView {
        return dualTextureViewNativeCamera
    }

    override fun getTemperatureDualView(): TemperatureView {
        return temperatureView
    }

    override suspend fun onDualViewCreate(dualView: DualViewWithExternalCameraCommonApi?) {

    }

    override fun isDualIR(): Boolean {
        return true
    }

    override fun setTemperatureViewType() {
        getTemperatureDualView().productType = Const.TYPE_IR_DUAL
    }

    override fun initContentView(): Int {
        return R.layout.fragment_ir_plush
    }

    override fun initData() {
    }

    override fun initView() {
        super.initView()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    fun getBitmap() : Bitmap?{
        return dualView?.scaledBitmap
    }
}