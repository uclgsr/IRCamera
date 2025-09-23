package com.mpdc4gsr.module.thermalunified.stubs

import android.content.Context
import android.util.AttributeSet
import android.view.View

/**
 * Stub implementations for missing UI widgets
 * These are placeholders to resolve compilation issues
 */

class SeekBarIndicator {
    var indicatorBackgroundColor: Int = 0
}

interface OnRangeChangedListener {
    fun onRangeChanged(
        view: RangeSeekBar?, 
        leftValue: Float, 
        rightValue: Float, 
        isFromUser: Boolean, 
        tempMode: Int
    )
    fun onStartTrackingTouch(view: RangeSeekBar?, isLeft: Boolean)
    fun onStopTrackingTouch(view: RangeSeekBar?, isLeft: Boolean)
}

class RangeSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    var onRangeChangedListener: OnRangeChangedListener? = null
    var currentRange = floatArrayOf(0f, 100f)
    var tempMode: Int = 0
    var leftSeekBar: SeekBarIndicator = SeekBarIndicator()
    var rightSeekBar: SeekBarIndicator = SeekBarIndicator()
    var indicatorBackgroundColor: Int = 0
    
    companion object {
        const val TEMP_MODE_MIN = -20
        const val TEMP_MODE_MAX = 150
        const val TEMP_MODE_INTERVAL = 1
        const val TEMP_MODE_CLOSE = 0
    }
    
    fun setRange(min: Float, max: Float) {
        currentRange[0] = min
        currentRange[1] = max
    }
    
    fun setOnRangeChangedListener(listener: OnRangeChangedListener) {
        onRangeChangedListener = listener
    }
    
    fun setRangeAndPro(minTemp: Float, maxTemp: Float, interval: Float, mode: Float) {}
    fun setRangeAndPro(minTemp: Float, maxTemp: Float, interval: Float, mode: Int) {}
    fun setRangeAndPro(range: String) {}
    fun setColorList(colors: IntArray?) {}
    fun setColorList(colors: Array<Int>?) {}
    fun setPlaces(places: FloatArray?) {}
    fun setPlaces(places: Array<Float>?) {}
    fun setPseudocode(code: Int) {}
    fun setIndicatorTextDecimalFormat(format: String) {}
}

class BitmapConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr)

class CameraPreView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    var cameraPreViewCloseListener: (() -> Unit)? = null
    
    fun getBitmap(): android.graphics.Bitmap? = null
    fun closeCamera() {}
    fun openCamera() {}
    fun setRotation(enabled: Boolean) {}
    
    // Property accessor
    var visibility: Int
        get() = getVisibility()
        set(value) { setVisibility(value) }
}

// Stub for CameraView (from infisense package)
class CameraView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    var bitmap: android.graphics.Bitmap? = null
    var isOpenAmplify: Boolean = false
    
    fun setShowCross(show: Boolean) {}
    fun setSyncimage(bitmap: android.graphics.Bitmap?) {}
    fun setTemperature(temp: Any?) {}
    fun setImageSize(width: Int, height: Int) {}
    fun setImageSize(width: Int, height: Int, context: Any?) {}
    fun updateSelectBitmap() {}
    fun updateTargetBitmap() {}
    fun setCameraAlpha(alpha: Float) {}
    fun getScaledBitmap(): android.graphics.Bitmap? = null
    fun closeCamera() {}
    fun openCamera() {}
    fun clear() {}
    fun start() {}
    
    // Property accessors that are being used
    var visibility: Int
        get() = getVisibility()
        set(value) { setVisibility(value) }
}

// Stub for TemperatureView (from infisense package)
class TemperatureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    var temperatureRegionMode: Int = 0
    var isUserHighTemp: Boolean = false
    var isUserLowTemp: Boolean = false
    var isEnabled: Boolean = true
    var regionAndValueBitmap: android.graphics.Bitmap? = null
    var isShowFull: Boolean = false
    var layoutParams: android.view.ViewGroup.LayoutParams? = null
    
    fun setIndicatorTextDecimalFormat(format: String) {}
    fun setTextSize(size: Int) {}
    fun setLinePaintColor(color: Int) {}
    fun setRangeAndPro(minTemp: Float, maxTemp: Float, interval: Float, mode: Float) {}
    fun setRangeAndPro(minTemp: Float, maxTemp: Float, interval: Float, mode: Int) {}
    fun setRangeAndPro(range: String) {}
    fun setOnTrendChangeListener(listener: Any?) {}
    fun setOnTrendAddListener(listener: Any?) {}
    fun setOnTrendRemoveListener(listener: Any?) {}
    fun setPseudocode(code: Int) {}
    fun setUserHighTemp(temp: Float) {}
    fun setUserHighTemp(enabled: Boolean) {}
    fun setUserLowTemp(temp: Float) {}
    fun setUserLowTemp(enabled: Boolean) {}
    fun setImageSize(width: Int, height: Int) {}
    fun setImageSize(width: Int, height: Int, context: Any?) {}
    fun updateMagnifier() {}
    fun start() {}
    fun stop() {}
    fun post(action: Runnable) {}
    fun clear() {}
    
    var listener: Any? = null
    
    // Property accessors that are being used  
    var visibility: Int
        get() = getVisibility()
        set(value) { setVisibility(value) }
}