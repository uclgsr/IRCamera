package com.mpdc4gsr.module.thermal.ir.fragment

import android.graphics.Bitmap
import android.view.SurfaceView
import com.mpdc4gsr.libunified.ir.usbdual.Const
import com.mpdc4gsr.libunified.ir.usbdual.camera.DualViewWithExternalCameraCommonApi
import com.mpdc4gsr.libunified.ir.view.TemperatureView
import com.mpdc4gsr.module.thermal.ir.R
import com.mpdc4gsr.module.thermal.ir.activity.BaseIRPlushFragment


class IRPlushFragment : BaseIRPlushFragment() {

    private lateinit var dualTextureViewNativeCamera: SurfaceView
    private lateinit var temperatureView: TemperatureView

    override fun onViewCreated(
        view: android.view.View,
        savedInstanceState: android.os.Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

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

    fun getBitmap(): Bitmap? {
        return dualView?.scaledBitmap
    }
}
