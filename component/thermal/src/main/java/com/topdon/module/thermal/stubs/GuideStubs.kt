package com.topdon.module.thermal.stubs

// TODO: Temporary stubs for hardware-specific classes - replace when guide library is available

class GuideInterface {
    interface IrDataCallback {
        fun processIrData(yuv: ByteArray, temp: FloatArray)
    }
    fun init(context: android.content.Context, callback: IrDataCallback): Int = 0
    fun yuv2Bitmap(bitmap: android.graphics.Bitmap?, yuv: ByteArray) {}
    fun getImageStatus(): Int = 0
    fun exit() {}
    fun setRange(range: Int) {}
    fun nuc() {}
    fun changePalette(index: Int) {}
}

class IrSurfaceView(context: android.content.Context) : android.view.View(context) {
    fun setMatrix(rotationAngle: Float, width: Float, height: Float) {}
    fun doDraw(bitmap: android.graphics.Bitmap?, status: Int) {}
    fun setOpenLut() {}
    fun getSaturationValue(): Int = 0
    fun setSaturationValue(value: Int) {}
}