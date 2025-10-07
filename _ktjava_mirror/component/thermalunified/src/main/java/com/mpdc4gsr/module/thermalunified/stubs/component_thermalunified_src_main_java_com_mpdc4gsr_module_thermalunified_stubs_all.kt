// Merged .kt under 'component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\stubs' subtree
// Files: 6; Generated 2025-10-07 23:07:44


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\stubs\FenceViews.kt =====

package com.mpdc4gsr.module.thermalunified.stubs

import android.content.Context
import android.util.AttributeSet
import android.view.View

class FenceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    var listener: CallBack? = null
    fun clear() {
        // Stub implementation for clear method
    }

    interface CallBack {
        fun callback(
            startPoint: IntArray,
            endPoint: IntArray,
            srcRect: IntArray,
        )
    }
}

class FencePointView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    var listener: CallBack? = null
    fun clear() {
        // Stub implementation for clear method
    }

    interface CallBack {
        fun callback(
            startPoint: IntArray,
            srcRect: IntArray,
        )
    }
}

class FenceLineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    var listener: CallBack? = null
    fun clear() {
        // Stub implementation for clear method
    }

    interface CallBack {
        fun callback(
            startPoint: IntArray,
            endPoint: IntArray,
            srcRect: IntArray,
        )
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\stubs\GuideStubs.kt =====

package com.mpdc4gsr.module.thermalunified.stubs

typealias GuideInterface = com.mpdc4gsr.libunified.app.matrix.GuideInterface
typealias IrSurfaceView = com.mpdc4gsr.libunified.app.matrix.IrSurfaceView


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\stubs\MonitorSelectDialogStub.kt =====

package com.mpdc4gsr.module.thermalunified.stubs

import android.content.Context

class MonitorSelectDialog {
    class Builder(private val context: Context) {
        private var positiveListener: ((Int) -> Unit)? = null
        fun setPositiveListener(listener: (Int) -> Unit): Builder {
            this.positiveListener = listener
            return this
        }

        fun create(): MonitorSelectDialog {
            return MonitorSelectDialog()
        }
    }

    fun show() {
        // Stub implementation - would show a dialog with monitor selection options
        // For now, just trigger the positive listener with default option
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\stubs\ThermalInputDialog.kt =====

package com.mpdc4gsr.module.thermalunified.stubs

import android.app.AlertDialog
import android.content.Context
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class ThermalInputDialog {
    class Builder(private val context: Context) {
        private var message: String = ""
        private var positiveListener: ((Float, Float, Int, Int) -> Unit)? = null
        private var cancelListener: (() -> Unit)? = null
        private var maxTemp: Float = 100f
        private var minTemp: Float = 0f
        private var maxColor: Int = android.graphics.Color.RED
        private var minColor: Int = android.graphics.Color.BLUE
        fun setMessage(message: String): Builder {
            this.message = message
            return this
        }

        fun setNum(max: Float = 100f, min: Float = 0f): Builder {
            this.maxTemp = max
            this.minTemp = min
            return this
        }

        fun setColor(
            maxColor: Int = android.graphics.Color.RED,
            minColor: Int = android.graphics.Color.BLUE
        ): Builder {
            this.maxColor = maxColor
            this.minColor = minColor
            return this
        }

        fun setPositiveListener(
            textResId: Int,
            listener: (Float, Float, Int, Int) -> Unit
        ): Builder {
            this.positiveListener = listener
            return this
        }

        fun setCancelListener(textResId: Int): Builder {
            return this
        }

        fun setCancelListener(text: String, listener: () -> Unit): Builder {
            this.cancelListener = listener
            return this
        }

        fun create(): ThermalInputDialog {
            return ThermalInputDialog().apply {
                this.context = this@Builder.context
                this.message = this@Builder.message
                this.positiveListener = this@Builder.positiveListener
                this.cancelListener = this@Builder.cancelListener
                this.maxTemp = this@Builder.maxTemp
                this.minTemp = this@Builder.minTemp
                this.maxColor = this@Builder.maxColor
                this.minColor = this@Builder.minColor
            }
        }
    }

    private lateinit var context: Context
    private var message: String = ""
    private var positiveListener: ((Float, Float, Int, Int) -> Unit)? = null
    private var cancelListener: (() -> Unit)? = null
    private var maxTemp: Float = 100f
    private var minTemp: Float = 0f
    private var maxColor: Int = android.graphics.Color.RED
    private var minColor: Int = android.graphics.Color.BLUE
    fun show() {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }
        // Add message
        layout.addView(TextView(context).apply {
            text = message
            setPadding(0, 0, 0, 20)
        })
        // Add input fields
        val maxTempEdit = EditText(context).apply {
            hint = "Max Temperature"
            setText(maxTemp.toString())
        }
        layout.addView(TextView(context).apply { text = "Max Temperature:" })
        layout.addView(maxTempEdit)
        val minTempEdit = EditText(context).apply {
            hint = "Min Temperature"
            setText(minTemp.toString())
        }
        layout.addView(TextView(context).apply { text = "Min Temperature:" })
        layout.addView(minTempEdit)
        AlertDialog.Builder(context)
            .setTitle("Thermal Input")
            .setView(layout)
            .setPositiveButton("OK") { _, _ ->
                try {
                    val maxVal = maxTempEdit.text.toString().toFloat()
                    val minVal = minTempEdit.text.toString().toFloat()
                    positiveListener?.invoke(maxVal, minVal, maxColor, minColor)
                } catch (e: NumberFormatException) {
                    // Use default values if parsing fails
                    positiveListener?.invoke(maxTemp, minTemp, maxColor, minColor)
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                cancelListener?.invoke()
            }
            .create()
            .show()
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\stubs\TipDialogs.kt =====

package com.mpdc4gsr.module.thermalunified.stubs

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class TipGuideDialog : DialogFragment() {
    var closeEvent: ((Boolean) -> Unit)? = null

    companion object {
        fun newInstance(): TipGuideDialog {
            return TipGuideDialog()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle("Guide")
            .setMessage("This is a guide dialog for thermal camera functionality.")
            .setPositiveButton("Got it") { _, _ ->
                closeEvent?.invoke(false)
            }
            .setNegativeButton("Don't show again") { _, _ ->
                closeEvent?.invoke(true)
            }
            .create()
    }
}

class TipPreviewDialog : DialogFragment() {
    var closeEvent: ((Boolean) -> Unit)? = null

    companion object {
        fun newInstance(): TipPreviewDialog {
            return TipPreviewDialog()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle("Preview Tip")
            .setMessage("This is a preview tip dialog for thermal camera preview.")
            .setPositiveButton("Continue") { _, _ ->
                closeEvent?.invoke(false)
            }
            .setNegativeButton("Don't show again") { _, _ ->
                closeEvent?.invoke(true)
            }
            .create()
    }
}

class TipObserveDialog {
    class Builder(private val context: Context) {
        private var title: String = "Tip"
        private var message: String = ""
        private var cancelListener: ((Boolean) -> Unit)? = null
        fun setTitle(resId: Int): Builder {
            this.title = context.getString(resId)
            return this
        }

        fun setMessage(resId: Int): Builder {
            this.message = context.getString(resId)
            return this
        }

        fun setCancelListener(listener: (Boolean) -> Unit): Builder {
            this.cancelListener = listener
            return this
        }

        fun create(): TipObserveDialog = TipObserveDialog().apply {
            this.context = this@Builder.context
            this.title = this@Builder.title
            this.message = this@Builder.message
            this.cancelListener = this@Builder.cancelListener
        }
    }

    private lateinit var context: Context
    private var title: String = ""
    private var message: String = ""
    private var cancelListener: ((Boolean) -> Unit)? = null
    fun show() {
        val dialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                cancelListener?.invoke(false)
            }
            .setNegativeButton("Don't show again") { _, _ ->
                cancelListener?.invoke(true)
            }
            .create()
        dialog.show()
    }
}

class TipDialog {
    class Builder(private val context: Context) {
        private var title: String = "Tip"
        private var message: String = ""
        private var positiveListener: (() -> Unit)? = null
        private var negativeListener: (() -> Unit)? = null
        fun setTitle(resId: Int): Builder {
            this.title = context.getString(resId)
            return this
        }

        fun setMessage(resId: Int): Builder {
            this.message = context.getString(resId)
            return this
        }

        fun setPositiveListener(resId: Int, listener: () -> Unit): Builder {
            this.positiveListener = listener
            return this
        }

        fun setNegativeListener(resId: Int, listener: () -> Unit): Builder {
            this.negativeListener = listener
            return this
        }

        fun create(): TipDialog = TipDialog().apply {
            this.context = this@Builder.context
            this.title = this@Builder.title
            this.message = this@Builder.message
            this.positiveListener = this@Builder.positiveListener
            this.negativeListener = this@Builder.negativeListener
        }
    }

    private lateinit var context: Context
    private var title: String = ""
    private var message: String = ""
    private var positiveListener: (() -> Unit)? = null
    private var negativeListener: (() -> Unit)? = null
    fun show() {
        val builder = AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ ->
                positiveListener?.invoke()
            }
        negativeListener?.let {
            builder.setNegativeButton("No") { _, _ ->
                it.invoke()
            }
        }
        builder.create().show()
    }
}

class TempAlarmSetDialog {
    class Builder(private val context: Context) {
        private var numText: String = ""
        fun setNum(num: String): Builder {
            numText = num
            return this
        }

        fun create(): TempAlarmSetDialog = TempAlarmSetDialog().apply {
            this.context = this@Builder.context
            this.numText = this@Builder.numText
        }
    }

    private lateinit var context: Context
    private var numText: String = ""
    fun show() {
        AlertDialog.Builder(context)
            .setTitle("Temperature Alarm")
            .setMessage("Set temperature alarm: $numText")
            .setPositiveButton("OK") { _, _ -> }
            .setNegativeButton("Cancel") { _, _ -> }
            .create()
            .show()
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\stubs\UIWidgets.kt =====

package com.mpdc4gsr.module.thermalunified.stubs

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class SeekBarIndicator {
    var indicatorBackgroundColor: Int = Color.LTGRAY
}

interface OnRangeChangedListener {
    fun onRangeChanged(
        view: RangeSeekBar?,
        leftValue: Float,
        rightValue: Float,
        isFromUser: Boolean,
        tempMode: Int
    )

    fun onStartTrackingTouch(view: RangeSeekBar?, isLeft: Boolean) {
        // Default implementation
    }

    fun onStopTrackingTouch(view: RangeSeekBar?, isLeft: Boolean) {
        // Default implementation
    }
}

class RangeSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    // MERGED: Combined constants from both companion objects
    companion object {
        const val TEMP_MODE_MIN = -20
        const val TEMP_MODE_MAX = 150
        const val TEMP_MODE_INTERVAL = 1
        const val TEMP_MODE_CLOSE = 0
    }

    // FIXED: Removed duplicate property declarations
    private var onRangeChangedListener: OnRangeChangedListener? = null
    var currentRange = floatArrayOf(0f, 100f)
    var tempMode: Int = TEMP_MODE_CLOSE
    var leftSeekBar: SeekBarIndicator = SeekBarIndicator()
    var rightSeekBar: SeekBarIndicator = SeekBarIndicator()
    var indicatorBackgroundColor: Int = Color.LTGRAY
    private var minValue = 0f
    private var maxValue = 100f
    private var leftValue = 20f
    private var rightValue = 80f
    private var isDragging = false
    private var isDraggingLeft = false
    private val trackPaint = Paint().apply {
        color = Color.GRAY
        isAntiAlias = true
    }
    private val thumbPaint = Paint().apply {
        color = Color.BLUE
        isAntiAlias = true
    }
    private val rightThumbPaint = Paint().apply {
        color = Color.RED
        isAntiAlias = true
    }

    fun setOnRangeChangedListener(listener: OnRangeChangedListener?) {
        onRangeChangedListener = listener
    }

    fun setRange(min: Float, max: Float) {
        minValue = min
        maxValue = max
        currentRange[0] = min
        currentRange[1] = max
        leftValue = min + (max - min) * 0.2f
        rightValue = min + (max - min) * 0.8f
        invalidate()
    }

    fun setRangeAndPro(minTemp: Float, maxTemp: Float, interval: Float, mode: Int) {
        setRange(minTemp, maxTemp)
        tempMode = mode
    }

    fun setRangeAndPro(range: String) {
        // Parse range string if needed - basic implementation
    }

    fun setColorList(colors: IntArray?) {
        colors?.let {
            if (it.isNotEmpty()) {
                trackPaint.color = it[0]
            }
        }
        invalidate()
    }

    fun setPlaces(places: FloatArray?) {
        // Store gradient positions if needed
    }

    fun setPseudocode(code: Int) {
        tempMode = code
    }

    fun setIndicatorTextDecimalFormat(format: String) {
        // Store format for text display
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val trackHeight = 8f
        val thumbRadius = 20f
        val y = height / 2f
        // Draw track
        canvas.drawRoundRect(
            paddingStart.toFloat(),
            y - trackHeight / 2,
            (width - paddingEnd).toFloat(),
            y + trackHeight / 2,
            trackHeight / 2,
            trackHeight / 2,
            trackPaint
        )
        // Calculate thumb positions
        val trackWidth = width - paddingStart - paddingEnd
        val leftThumbX = paddingStart + (leftValue - minValue) / (maxValue - minValue) * trackWidth
        val rightThumbX =
            paddingStart + (rightValue - minValue) / (maxValue - minValue) * trackWidth
        // Draw thumbs
        canvas.drawCircle(leftThumbX, y, thumbRadius, thumbPaint)
        canvas.drawCircle(rightThumbX, y, thumbRadius, rightThumbPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        val trackWidth = width - paddingStart - paddingEnd
        val leftThumbX = paddingStart + (leftValue - minValue) / (maxValue - minValue) * trackWidth
        val rightThumbX =
            paddingStart + (rightValue - minValue) / (maxValue - minValue) * trackWidth
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val leftDistance = abs(event.x - leftThumbX)
                val rightDistance = abs(event.x - rightThumbX)
                isDraggingLeft = leftDistance < rightDistance
                isDragging = true
                onRangeChangedListener?.onStartTrackingTouch(this, isDraggingLeft)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val ratio = ((event.x - paddingStart) / trackWidth).coerceIn(0f, 1f)
                    val newValue = minValue + ratio * (maxValue - minValue)
                    if (isDraggingLeft) {
                        leftValue = newValue.coerceIn(minValue, rightValue)
                    } else {
                        rightValue = newValue.coerceIn(leftValue, maxValue)
                    }
                    onRangeChangedListener?.onRangeChanged(
                        this,
                        leftValue,
                        rightValue,
                        true,
                        tempMode
                    )
                    invalidate()
                    return true
                }
            }

            MotionEvent.ACTION_UP -> {
                if (isDragging) {
                    onRangeChangedListener?.onStopTrackingTouch(this, isDraggingLeft)
                    isDragging = false
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }
}

// MERGED: Kept both classes from the first conflict
class SeekBarStub {
    var indicatorBackgroundColor: Int = 0
}

class BitmapConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.constraintlayout.widget.ConstraintLayout(context, attrs, defStyleAttr)

// MERGED: Chose the more complete implementation for CameraPreView and included its dependencies
class CameraPreView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    var cameraPreViewCloseListener: (() -> Unit)? = null
    private var isOpen = false
    private var alphaValue = 1.0f
    fun getBitmap(): android.graphics.Bitmap? {
        return try {
            android.graphics.Bitmap.createBitmap(
                width.coerceAtLeast(1),
                height.coerceAtLeast(1),
                android.graphics.Bitmap.Config.ARGB_8888
            )
        } catch (e: Exception) {
            null
        }
    }

    fun closeCamera() {
        isOpen = false
        cameraPreViewCloseListener?.invoke()
        invalidate()
    }

    fun openCamera() {
        isOpen = true
        invalidate()
    }

    fun setRotation(enabled: Boolean) {
        rotation = if (enabled) 90f else 0f
    }

    fun setCameraAlpha(alpha: Float) {
        this.alphaValue = alpha.coerceIn(0f, 1f)
        setAlpha(this.alphaValue)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isOpen) {
            val paint = Paint().apply {
                color = Color.DKGRAY
                alpha = (255 * this@CameraPreView.alphaValue).toInt()
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }
    }
}

class CameraView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    var bitmap: android.graphics.Bitmap? = null
    var isOpenAmplify: Boolean = false
    private var showCross = false
    private var isStarted = false
    fun setShowCross(show: Boolean) {
        showCross = show
        invalidate()
    }

    fun setSyncimage(bitmap: android.graphics.Bitmap?) {
        this.bitmap = bitmap
        invalidate()
    }

    fun setSyncimage(syncBitmap: Any?) { // Using Any? for the unknown SynchronizedBitmap type
        // Handle synchronized bitmap
        invalidate()
    }

    fun setTemperature(temp: Any?) {
        // Store temperature data
    }

    fun setImageSize(width: Int, height: Int) {
        layoutParams = layoutParams?.apply {
            this.width = width
            this.height = height
        }
    }

    fun setCameraAlpha(alpha: Float) {
        setAlpha(alpha.coerceIn(0f, 1f))
    }

    fun getScaledBitmap(): android.graphics.Bitmap {
        return bitmap ?: android.graphics.Bitmap.createBitmap(
            width.coerceAtLeast(1),
            height.coerceAtLeast(1),
            android.graphics.Bitmap.Config.ARGB_8888
        )
    }

    fun closeCamera() {
        isStarted = false
    }

    fun openCamera() {
        isStarted = true
    }

    fun clear() {
        bitmap = null
        invalidate()
    }

    fun start() {
        isStarted = true
    }

    fun stop() {
        isStarted = false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bitmap?.let { bmp ->
            if (!bmp.isRecycled) {
                canvas.drawBitmap(bmp, 0f, 0f, null)
            }
        }
        if (showCross) {
            val paint = Paint().apply {
                color = Color.RED
                strokeWidth = 4f
            }
            val centerX = width / 2f
            val centerY = height / 2f
            canvas.drawLine(centerX - 20, centerY, centerX + 20, centerY, paint)
            canvas.drawLine(centerX, centerY - 20, centerX, centerY + 20, paint)
        }
    }
}

class TemperatureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    var temperatureRegionMode: Int = 0
    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
    }

    var regionAndValueBitmap: android.graphics.Bitmap? = null
    var isShowFull: Boolean = false
    private var userHighTemp: Boolean = false
    private var userLowTemp: Boolean = false
    private var textSize = 12
    private var lineColor = Color.WHITE
    fun setIndicatorTextDecimalFormat(format: String) {
        // Store format
    }

    fun setTextSize(size: Int) {
        textSize = size
    }

    fun setLinePaintColor(color: Int) {
        lineColor = color
    }

    fun setRangeAndPro(minTemp: Float, maxTemp: Float, interval: Float, mode: Int) {
        // Set temperature range
    }

    fun setOnTrendChangeListener(listener: Any?) {
        // Store listener
    }

    fun setUserHighTemp(enabled: Boolean) {
        userHighTemp = enabled
    }

    fun setUserLowTemp(enabled: Boolean) {
        userLowTemp = enabled
    }

    fun setImageSize(width: Int, height: Int) {
        layoutParams = layoutParams?.apply {
            this.width = width
            this.height = height
        }
    }

    fun setImageSize(width: Int, height: Int, context: Any?) {
        setImageSize(width, height)
    }

    fun updateMagnifier() {
        invalidate()
    }

    fun start() {
        // Start temperature monitoring
    }

    fun stop() {
        // Stop temperature monitoring
    }

    override fun post(action: Runnable): Boolean {
        return super.post(action)
    }

    fun clear() {
        regionAndValueBitmap = null
        invalidate()
    }

    fun setSyncimage(bitmap: android.graphics.Bitmap?) {
        regionAndValueBitmap = bitmap
        invalidate()
    }

    fun setTemperature(temp: Any?) {
        // Set temperature data
    }

    var listener: Any? = null
}


