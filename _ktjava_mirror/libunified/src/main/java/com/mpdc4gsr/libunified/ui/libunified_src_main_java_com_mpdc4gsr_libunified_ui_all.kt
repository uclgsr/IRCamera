// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\ui' subtree
// Files: 25; Generated 2025-10-07 23:07:48


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ui\bean\ColorBean.kt =====

package com.mpdc4gsr.libunified.ui.bean

data class ColorBean(
    val res: Int,
    val name: String,
    val code: Int
)


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ui\camera\CameraPreView.kt =====

package com.mpdc4gsr.libunified.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.elvishew.xlog.XLog

class CameraPreView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val TAG = "CameraPreView"
    private var cameraAlpha: Float = 1.0f
    private var isRotationSet: Boolean = false

    interface CameraPreViewCloseListener {
        fun onClose()
    }

    private var closeListener: CameraPreViewCloseListener? = null
    var cameraPreViewCloseListener: CameraPreViewCloseListener? = null
        set(value) {
            field = value
            closeListener = value
        }

    override fun setRotation(rotation: Float) {
        super.setRotation(rotation)
        isRotationSet = true
        XLog.d(TAG, "Camera rotation set to: $rotation")
    }

    fun setRotation(enabled: Boolean) {
        isRotationSet = enabled
        if (!enabled) {
            super.setRotation(0f)
        }
        XLog.d(TAG, "Camera rotation enabled: $enabled")
    }

    fun setCameraAlpha(alpha: Float) {
        this.cameraAlpha = alpha
        this.alpha = alpha
        XLog.d(TAG, "Camera alpha set to: $alpha")
    }

    fun getCameraAlpha(): Float = cameraAlpha
    fun openCamera() {
        visibility = VISIBLE
        XLog.d(TAG, "Camera opened")
    }

    fun closeCamera() {
        visibility = GONE
        closeListener?.onClose()
        XLog.d(TAG, "Camera closed")
    }

    fun getBitmap(): Bitmap? {
        // This would typically capture the current frame
        // For now, return null as placeholder
        XLog.d(TAG, "Bitmap requested")
        return null
    }

    fun getBitmap(width: Int, height: Int): Bitmap? {
        // This would typically capture and scale the current frame
        XLog.d(TAG, "Scaled bitmap requested: ${width}x${height}")
        return null
    }

    fun setOnCloseListener(listener: CameraPreViewCloseListener) {
        this.closeListener = listener
    }

    override fun post(action: Runnable): Boolean {
        return super.post(action)
    }

    override fun setLayoutParams(params: ViewGroup.LayoutParams?) {
        super.setLayoutParams(params)
        XLog.d(TAG, "Layout params updated")
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        when (visibility) {
            VISIBLE -> XLog.d(TAG, "Camera preview visible")
            GONE -> XLog.d(TAG, "Camera preview gone")
            INVISIBLE -> XLog.d(TAG, "Camera preview invisible")
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ui\camera\CameraPreviewManager.kt =====

package com.mpdc4gsr.libunified.ui.camera

import android.graphics.Bitmap
import android.os.Handler
import com.elvishew.xlog.XLog
import com.infisense.usbir.view.CameraView
import com.mpdc4gsr.libunified.app.bean.AlarmBean
import com.mpdc4gsr.libunified.common.RotateDegree

class CameraPreviewManager private constructor() {
    private val TAG = "CameraPreviewManager"

    companion object {
        @Volatile
        private var INSTANCE: CameraPreviewManager? = null
        fun getInstance(): CameraPreviewManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CameraPreviewManager().also { INSTANCE = it }
            }
        }
    }

    // Properties
    var imageRotate: Int = 0
        set(value) {
            field = value
            XLog.d(TAG, "Image rotate set to: $value")
        }

    // Property that accepts RotateDegree enum
    var imageRotateDegree: RotateDegree = RotateDegree.DEGREE_0
        set(value) {
            field = value
            imageRotate = value.getValue()
            XLog.d(TAG, "Image rotate degree set to: $value")
        }
    var alarmBean: AlarmBean? = null
        set(value) {
            field = value
            XLog.d(TAG, "Alarm bean updated")
        }
    private var cameraView: CameraView? = null
    private var handler: Handler? = null
    private var tempDataChangeCallback: ((Any) -> Unit)? = null
    private var scaledBitmapCache: Bitmap? = null

    // Frame data for thermal imaging
    var frameIrAndTempData: ByteArray? = null
        set(value) {
            field = value
            XLog.d(TAG, "Frame IR and temp data updated")
        }

    // Configuration
    private var minLimit: Float = 0f
    private var maxLimit: Float = 100f
    private var pseudocolorMode: Int = 0
    fun init(cameraView: CameraView, handler: Handler) {
        this.cameraView = cameraView
        this.handler = handler
        XLog.d(TAG, "CameraPreviewManager initialized")
    }

    fun setLimit(min: Float, max: Float) {
        this.minLimit = min
        this.maxLimit = max
        XLog.d(TAG, "Temperature limits set: min=$min, max=$max")
    }

    fun setPseudocolorMode(mode: Int) {
        this.pseudocolorMode = mode
        XLog.d(TAG, "Pseudocolor mode set to: $mode")
    }

    fun setOnTempDataChangeCallback(callback: (Any) -> Unit) {
        this.tempDataChangeCallback = callback
        XLog.d(TAG, "Temperature data change callback set")
    }

    fun scaledBitmap(cached: Boolean = false): Bitmap? {
        return if (cached && scaledBitmapCache != null) {
            scaledBitmapCache
        } else {
            cameraView?.getScaledBitmap()?.also { bitmap ->
                if (cached) {
                    scaledBitmapCache = bitmap
                }
            }
        }
    }

    fun getCameraBitmap(): Bitmap? {
        return cameraView?.bitmap
    }

    fun updateCameraBitmap(bitmap: Bitmap?) {
        bitmap?.let {
            cameraView?.bitmap = it
            scaledBitmapCache = null // Invalidate cache
        }
    }

    fun startPreview() {
        cameraView?.start()
        XLog.d(TAG, "Preview started")
    }

    fun stopPreview() {
        cameraView?.stop()
        scaledBitmapCache = null
        XLog.d(TAG, "Preview stopped")
    }

    fun pausePreview() {
        XLog.d(TAG, "Preview paused")
    }

    fun resumePreview() {
        XLog.d(TAG, "Preview resumed")
    }

    fun release() {
        cameraView = null
        handler = null
        tempDataChangeCallback = null
        scaledBitmapCache = null
        XLog.d(TAG, "CameraPreviewManager released")
    }

    fun releaseSource() {
        release()
        XLog.d(TAG, "CameraPreviewManager source released")
    }

    // Method overloads for different parameter combinations
    fun setLimit(min: Float, max: Float, param3: Any) {
        setLimit(min, max)
        XLog.d(TAG, "Temperature limits set with additional parameter: min=$min, max=$max")
    }

    fun setColorList(colors: Nothing?, nothing: Nothing?, bool: Boolean, f: Float, f1: Float) {
        XLog.d(TAG, "Color list set")
    }

    fun setAutoSwitchGainEnable(enabled: Boolean) {
        XLog.d(TAG, "Auto switch gain enabled: $enabled")
    }

    // Camera view initialization with different types
    fun init(surfaceView: Any, handler: Handler) {
        this.handler = handler
        XLog.d(TAG, "CameraPreviewManager initialized with surface view")
    }

    // Thermal data processing
    fun processThermalData(data: Any) {
        tempDataChangeCallback?.invoke(data)
    }

    // Camera controls
    fun setImageRotation(rotation: Int) {
        imageRotate = rotation
    }

    fun getImageRotation(): Int = imageRotate
    fun setTemperatureLimits(min: Float, max: Float) {
        setLimit(min, max)
    }

    fun getMinTemperature(): Float = minLimit
    fun getMaxTemperature(): Float = maxLimit

    // Pseudocolor controls
    fun getPseudocolorMode(): Int = pseudocolorMode
    fun applyPseudocolor(bitmap: Bitmap): Bitmap {
        // Placeholder for pseudocolor processing
        // In real implementation, this would apply thermal color mapping
        return bitmap
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ui\fence\FenceLineView.kt =====

package com.mpdc4gsr.libunified.ui.fence

import android.content.Context
import android.util.AttributeSet
import android.view.View

class FenceLineView : View {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    var listener: CallBack? = null
    fun clear() {
        // Clear fence line view state
    }

    interface CallBack {
        fun callback(startPoint: IntArray, endPoint: IntArray, srcRect: IntArray)
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ui\fence\FencePointView.kt =====

package com.mpdc4gsr.libunified.ui.fence

import android.content.Context
import android.util.AttributeSet
import android.view.View

class FencePointView : View {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    var listener: CallBack? = null
    fun clear() {
        // Clear fence point view state
    }

    interface CallBack {
        fun callback(startPoint: IntArray, srcRect: IntArray)
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ui\fence\FenceView.kt =====

package com.mpdc4gsr.libunified.ui.fence

import android.content.Context
import android.util.AttributeSet
import android.view.View

class FenceView : View {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    var listener: CallBack? = null
    fun clear() {
        // Clear fence view state
    }

    interface CallBack {
        fun callback(startPoint: IntArray, endPoint: IntArray, srcRect: IntArray)
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ui\listener\SingleClickListener.kt =====

package com.mpdc4gsr.libunified.ui.listener

import android.view.View

abstract class SingleClickListener : View.OnClickListener {
    private var lastClickTime: Long = 0
    private val minInterval: Long = 500 // Minimum interval between clicks in milliseconds
    override fun onClick(v: View?) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime >= minInterval) {
            lastClickTime = currentTime
            onSingleClick()
        }
    }

    abstract fun onSingleClick()
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ui\R.kt =====

package com.mpdc4gsr.libunified.ui

object R {
    object drawable {
        // Target thermal menu items - reference existing drawables
        const val ic_menu_thermal6001 = com.mpdc4gsr.libunified.R.drawable.ic_menu_thermal6001
        const val ic_menu_thermal6002 = com.mpdc4gsr.libunified.R.drawable.ic_menu_thermal6002
        const val ic_menu_thermal6003 = com.mpdc4gsr.libunified.R.drawable.ic_menu_thermal6003
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ui\SettingNightView.kt =====

package com.mpdc4gsr.libunified.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.mpdc4gsr.libunified.R

class SettingNightView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private val itemSettingImage: ImageView
    private val itemSettingEndImage: ImageView
    private val tvEnd: TextView
    private val itemSettingText: TextView
    private val itemSettingLine: View
    var isRightArrowVisible: Boolean = true
        set(value) {
            field = value
            itemSettingEndImage.isVisible = value
        }

    init {
        LayoutInflater.from(context).inflate(R.layout.ui_setting_view_night, this, true)
        itemSettingImage = findViewById(R.id.item_setting_image)
        itemSettingEndImage = findViewById(R.id.item_setting_end_image)
        tvEnd = findViewById(R.id.tv_end)
        itemSettingText = findViewById(R.id.item_setting_text)
        itemSettingLine = findViewById(R.id.item_setting_line)
        // Handle custom attributes
        context.obtainStyledAttributes(attrs, R.styleable.SettingNightView, defStyleAttr, 0).apply {
            try {
                // Set text
                getString(R.styleable.SettingNightView_setting_text_night)?.let {
                    itemSettingText.text = it
                }
                // Set icon
                val iconRes = getResourceId(R.styleable.SettingNightView_setting_icon_night, 0)
                if (iconRes != 0) {
                    itemSettingImage.setImageResource(iconRes)
                }
                // Set show icon visibility
                val showIcon =
                    getBoolean(R.styleable.SettingNightView_setting_icon_show_night, true)
                itemSettingImage.isVisible = showIcon
                // Set more arrow visibility
                val showMore = getBoolean(R.styleable.SettingNightView_setting_more_night, true)
                itemSettingEndImage.isVisible = showMore
                isRightArrowVisible = showMore
                // Set line visibility
                val showLine = getBoolean(R.styleable.SettingNightView_setting_line_night, false)
                itemSettingLine.isVisible = showLine
            } finally {
                recycle()
            }
        }
    }

    fun setRightTextId(textResId: Int) {
        if (textResId == 0) {
            tvEnd.visibility = GONE
            itemSettingEndImage.visibility = if (isRightArrowVisible) VISIBLE else GONE
        } else {
            tvEnd.setText(textResId)
            tvEnd.visibility = VISIBLE
            itemSettingEndImage.visibility = GONE
        }
    }

    override fun setOnClickListener(listener: OnClickListener?) {
        super.setOnClickListener(listener)
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\BarPickView.kt =====

package com.mpdc4gsr.libunified.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import com.mpdc4gsr.libunified.compat.dpToPx
import com.mpdc4gsr.libunified.compat.spToPx
import com.mpdc4gsr.libunified.R

class BarPickView : View {
    companion object {
        @ColorInt
        private const val DEFAULT_BG_COLOR = 0xff787878.toInt()

        @ColorInt
        private const val DEFAULT_PROGRESS_COLOR = 0xffffffff.toInt()
        private const val THUMB_CORNERS = 11f
        private const val THUMB_STROKE_WIDTH = 1.5f
    }

    var onStartTrackingTouch: ((progress: Int, max: Int) -> Unit)? = null
    var onProgressChanged: ((progress: Int, max: Int) -> Unit)? = null
    var onStopTrackingTouch: ((progress: Int, max: Int) -> Unit)? = null
    var valueFormatListener: ((progress: Int) -> String) = {
        it.toString()
    }
    var max: Int = 100
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }
    var min: Int = 0
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }
    private var progress: Int = 0
        set(value) {
            if (field != value) {
                field = value.coerceAtLeast(min).coerceAtMost(max)
                invalidate()
            }
        }

    fun setProgressAndRefresh(progress: Int) {
        this.progress = progress
        onProgressChanged?.invoke(this.progress, max)
    }

    private val barSize: Int
    private val rotate: Int
    private val labelText: String
    private val path = Path()
    private val paint = TextPaint()
    private val thumbRect = RectF()
    private val barRect = RectF()

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        0
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    ) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.BarPickView, 0, 0)
        max = typedArray.getInt(R.styleable.BarPickView_android_max, 100)
        min = typedArray.getInt(R.styleable.BarPickView_barMin, 0)
        progress =
            typedArray.getInt(R.styleable.BarPickView_android_progress, min).coerceAtMost(max)
                .coerceAtLeast(min)
        barSize = typedArray.getInt(R.styleable.BarPickView_barSize, 4f.dpToPx(context).toInt())
        rotate = typedArray.getInt(R.styleable.BarPickView_barOrientation, 0)
        labelText = typedArray.getString(R.styleable.BarPickView_barLabel) ?: ""
        val textSize = typedArray.getDimensionPixelSize(
            R.styleable.BarPickView_android_textSize,
            13f.spToPx(context).toInt()
        )
        typedArray.recycle()
        paint.isAntiAlias = true
        paint.textSize = textSize.toFloat()
        paint.strokeWidth = THUMB_STROKE_WIDTH.dpToPx(context)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return false
        }
        parent.requestDisallowInterceptTouchEvent(true)
        val x: Float = event.x - barRect.left
        val y: Float = event.y - barRect.top
        val barWidth: Float = barRect.width()
        val barHeight: Float = barRect.height()
        progress = when (rotate) {
            0 -> (x / barWidth * (max - min) + min).toInt()
            180 -> ((barWidth - x) / barWidth * (max - min) + min).toInt()
            90 -> (y / barHeight * (max - min) + min).toInt()
            else -> ((barHeight - y) / barHeight * (max - min) + min).toInt()
        }.coerceAtLeast(min).coerceAtMost(max)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> onStartTrackingTouch?.invoke(progress, max)
            MotionEvent.ACTION_MOVE -> onProgressChanged?.invoke(progress, max)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                onStopTrackingTouch?.invoke(progress, max)
            }
        }
        return true
    }

    private fun computeThumbWidth(): Int {
        val minTextWidth = paint.measureText(valueFormatListener.invoke(min)).toInt()
        val maxTextWidth = paint.measureText(valueFormatListener.invoke(max)).toInt()
        return minTextWidth.coerceAtLeast(maxTextWidth) + 12f.dpToPx(context).toInt()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val thumbWidth = computeThumbWidth()
        val thumbHeight: Int =
            paint.fontMetricsInt.bottom - paint.fontMetricsInt.top + 4f.dpToPx(context).toInt()
        val width: Int = if (rotate == 0 || rotate == 180) {
            if (widthMode == MeasureSpec.UNSPECIFIED) context.resources.displayMetrics.widthPixels else widthSize
        } else {
            val wantWidth: Int = thumbWidth + paddingStart + paddingEnd
            when (widthMode) {
                MeasureSpec.EXACTLY -> widthSize
                MeasureSpec.AT_MOST -> wantWidth.coerceAtMost(widthSize)
                MeasureSpec.UNSPECIFIED -> wantWidth
                else -> wantWidth
            }
        }
        val height: Int = if (rotate == 0 || rotate == 180) {
            val wantHeight: Int = thumbHeight + paddingTop + paddingBottom
            when (heightMode) {
                MeasureSpec.EXACTLY -> heightSize
                MeasureSpec.AT_MOST -> wantHeight.coerceAtMost(heightSize)
                MeasureSpec.UNSPECIFIED -> wantHeight
                else -> wantHeight
            }
        } else {
            if (heightMode == MeasureSpec.UNSPECIFIED) context.resources.displayMetrics.heightPixels else heightSize
        }
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        computeBarRect()
        computeThumbRect()
        clipToBarRect(canvas)
        drawBgBar(canvas)
        drawProgress(canvas)
        canvas.restore()
        drawThumb(canvas)
        drawText(canvas)
    }

    private fun computeBarRect() {
        val textHeight = paint.fontMetricsInt.bottom - paint.fontMetricsInt.top
        val textMargin = 4f.dpToPx(context)
        val thumbWidth = computeThumbWidth()
        val thumbHeight = textHeight + 4f.dpToPx(context).toInt()
        if (rotate == 0 || rotate == 180) {
            val labelTextSpace =
                if (labelText.isEmpty()) 0 else (paint.measureText(labelText)
                    .toInt() + 6f.dpToPx(context).toInt())
            val leftText = valueFormatListener.invoke(if (rotate == 0) min else max)
            val rightText = valueFormatListener.invoke(if (rotate == 0) max else min)
            val leftTextWidth = paint.measureText(leftText).toInt()
            val rightTextWidth = paint.measureText(rightText).toInt()
            val left =
                paddingStart.toFloat() + leftTextWidth + textMargin + if (rotate == 0) labelTextSpace else 0
            val top = (paddingTop + thumbHeight / 2 - barSize / 2).toFloat()
            val right =
                (measuredWidth - paddingEnd - rightTextWidth - textMargin - if (rotate == 0) 0 else labelTextSpace).toFloat()
            val bottom = top + barSize
            barRect.set(left, top, right, bottom)
        } else {
            val labelTextSpace = if (labelText.isEmpty()) 0 else (textHeight + 6f.dpToPx(context).toInt())
            val left = (paddingStart + thumbWidth / 2 - barSize / 2).toFloat()
            val top =
                paddingTop.toFloat() + textHeight + textMargin + if (rotate == 90) labelTextSpace else 0
            val right = left + barSize
            val bottom =
                (measuredHeight - paddingBottom - textHeight - textMargin - if (rotate == 90) 0 else labelTextSpace).toFloat()
            barRect.set(left, top, right, bottom)
        }
    }

    private fun computeThumbRect() {
        val thumbWidth = computeThumbWidth()
        val thumbHeight: Int =
            paint.fontMetricsInt.bottom - paint.fontMetricsInt.top + 4f.dpToPx(context).toInt()
        if (rotate == 0 || rotate == 180) {
            val progressWidth = (barRect.width() * (progress - min) / (max - min)).toInt()
            val left =
                (if (rotate == 0) (barRect.left + progressWidth - thumbWidth / 2) else (barRect.right - progressWidth - thumbWidth / 2))
                    .toInt()
                    .coerceAtLeast(barRect.left.toInt())
                    .coerceAtMost(barRect.right.toInt() - thumbWidth)
            val right = left + thumbWidth
            val top = paddingTop
            val bottom = measuredHeight - paddingBottom
            thumbRect.set(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
        } else {
            val progressHeight =
                (barRect.height() * (progress - min) / (max - min).toFloat()).toInt()
            val left = paddingStart
            val right = measuredWidth - paddingEnd
            val top =
                (if (rotate == 90) (barRect.top + progressHeight - thumbHeight / 2f) else (barRect.bottom - progressHeight - thumbHeight / 2f))
                    .toInt()
                    .coerceAtLeast(barRect.top.toInt())
                    .coerceAtMost(barRect.bottom.toInt() - thumbHeight)
            val bottom = top + thumbHeight
            thumbRect.set(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
        }
    }

    private fun clipToBarRect(canvas: Canvas) {
        canvas.save()
        val radius = (barSize / 2).toFloat()
        if (rotate == 0 || rotate == 180) {
            path.rewind()
            path.moveTo(barRect.left + radius, barRect.top)
            path.lineTo(barRect.right - radius, barRect.top)
            path.quadTo(barRect.right, barRect.top, barRect.right, barRect.top + barSize / 2)
            path.quadTo(barRect.right, barRect.bottom, barRect.right - radius, barRect.bottom)
            path.lineTo(barRect.left + radius, barRect.bottom)
            path.quadTo(barRect.left, barRect.bottom, barRect.left, barRect.bottom - barSize / 2)
            path.quadTo(barRect.left, barRect.top, barRect.left + radius, barRect.top)
            canvas.clipPath(path)
        } else {
            path.rewind()
            path.moveTo(barRect.left, barRect.bottom - radius)
            path.lineTo(barRect.left, barRect.top + radius)
            path.quadTo(barRect.left, barRect.top, barRect.left + barSize / 2, barRect.top)
            path.quadTo(barRect.right, barRect.top, barRect.right, barRect.top + radius)
            path.lineTo(barRect.right, barRect.bottom - radius)
            path.quadTo(barRect.right, barRect.bottom, barRect.right - barSize / 2, barRect.bottom)
            path.quadTo(barRect.left, barRect.bottom, barRect.left, barRect.bottom - radius)
            canvas.clipPath(path)
        }
    }

    private fun drawBgBar(canvas: Canvas) {
        paint.color = DEFAULT_BG_COLOR
        val left = barRect.left
        val top = barRect.top
        val right = barRect.right
        val bottom = barRect.bottom
        if (rotate == 0 || rotate == 180) {
            val thumbWidth = computeThumbWidth()
            val bgWidth = (barRect.width() * (max - progress) / (max - min).toFloat()).toInt()
            if (bgWidth == 0) {
                return
            }
            if (rotate == 0) {
                canvas.drawRect(
                    (right - bgWidth + thumbWidth / 2).coerceAtLeast(left + thumbWidth),
                    top,
                    right,
                    bottom,
                    paint
                )
            } else {
                canvas.drawRect(
                    left,
                    top,
                    (left + bgWidth - thumbWidth / 2).coerceAtMost(right - thumbWidth),
                    bottom,
                    paint
                )
            }
        } else {
            val thumbHeight =
                paint.fontMetricsInt.bottom - paint.fontMetricsInt.top + 4f.dpToPx(context).toInt()
            val bgHeight = (barRect.height() * (max - progress) / (max - min).toFloat()).toInt()
            if (bgHeight == 0) {
                return
            }
            if (rotate == 90) {
                canvas.drawRect(
                    left,
                    (bottom - bgHeight + thumbHeight / 2).coerceAtLeast(top + thumbHeight),
                    right,
                    bottom,
                    paint
                )
            } else {
                canvas.drawRect(
                    left,
                    top,
                    right,
                    (top + bgHeight - thumbHeight / 2).coerceAtMost(bottom - thumbHeight),
                    paint
                )
            }
        }
    }

    private fun drawProgress(canvas: Canvas) {
        paint.color = DEFAULT_PROGRESS_COLOR
        val left = barRect.left
        val top = barRect.top
        val right = barRect.right
        val bottom = barRect.bottom
        if (rotate == 0 || rotate == 180) {
            val thumbWidth = computeThumbWidth()
            val progressWidth = (barRect.width() * (progress - min) / (max - min).toFloat()).toInt()
            if (progressWidth == 0) {
                return
            }
            if (rotate == 0) {
                canvas.drawRect(
                    left,
                    top,
                    (left + progressWidth - thumbWidth / 2).coerceAtMost(right - thumbWidth),
                    bottom,
                    paint
                )
            } else {
                canvas.drawRect(
                    (right - progressWidth + thumbWidth / 2).coerceAtLeast(left + thumbWidth),
                    top,
                    right,
                    bottom,
                    paint
                )
            }
        } else {
            val thumbHeight =
                paint.fontMetricsInt.bottom - paint.fontMetricsInt.top + 4f.dpToPx(context).toInt()
            val progressHeight =
                (barRect.height() * (progress - min) / (max - min).toFloat()).toInt()
            if (progressHeight == 0) {
                return
            }
            if (rotate == 90) {
                canvas.drawRect(
                    left,
                    top,
                    right,
                    (top + progressHeight - thumbHeight / 2).coerceAtMost(bottom - thumbHeight),
                    paint
                )
            } else {
                canvas.drawRect(
                    left,
                    (bottom - progressHeight + thumbHeight / 2).coerceAtLeast(top + thumbHeight),
                    right,
                    bottom,
                    paint
                )
            }
        }
    }

    private fun drawThumb(canvas: Canvas) {
        paint.style = Paint.Style.STROKE
        val radius = THUMB_CORNERS.dpToPx(context)
        canvas.drawRoundRect(thumbRect, radius, radius, paint)
        paint.style = Paint.Style.FILL
        val progressText = valueFormatListener.invoke(progress)
        val textWidth = paint.measureText(progressText)
        val x = thumbRect.left + (thumbRect.width() - textWidth) / 2
        val y = thumbRect.top + 2f.dpToPx(context) - paint.fontMetricsInt.top
        canvas.drawText(progressText, x, y, paint)
    }

    private fun drawText(canvas: Canvas) {
        if (rotate == 0 || rotate == 180) {
            val y = thumbRect.top + 2f.dpToPx(context) - paint.fontMetricsInt.top
            val labelTextWidth = paint.measureText(labelText)
            val labelX =
                if (rotate == 0) paddingStart.toFloat() else (width - paddingEnd - labelTextWidth)
            canvas.drawText(labelText, labelX, y, paint)
            val leftText = valueFormatListener.invoke(if (rotate == 0) min else max)
            val leftX = barRect.left - 4f.dpToPx(context) - paint.measureText(leftText)
            canvas.drawText(leftText, leftX, y, paint)
            val rightText = valueFormatListener.invoke(if (rotate == 0) max else min)
            canvas.drawText(rightText, barRect.right + 4f.dpToPx(context), y, paint)
        } else {
            val topText = valueFormatListener.invoke(if (rotate == 90) min else max)
            val topTextWidth = paint.measureText(topText)
            val topX = thumbRect.left + (thumbRect.width() - topTextWidth) / 2
            val topY = barRect.top - 4f.dpToPx(context) - paint.fontMetricsInt.bottom
            canvas.drawText(topText, topX, topY, paint)
            val bottomText = valueFormatListener.invoke(if (rotate == 90) max else min)
            val bottomTextWidth = paint.measureText(bottomText)
            val bottomX = thumbRect.left + (thumbRect.width() - bottomTextWidth) / 2
            val bottomY = barRect.bottom + 4f.dpToPx(context) - paint.fontMetricsInt.top
            canvas.drawText(bottomText, bottomX, bottomY, paint)
            val labelTextWidth = paint.measureText(labelText)
            val labelX = thumbRect.left + (thumbRect.width() - labelTextWidth) / 2
            val labelY = if (rotate == 90) {
                (paddingTop + 6f.dpToPx(context) - paint.fontMetricsInt.top).toFloat()
            } else {
                (height - 6f.dpToPx(context) - paint.fontMetricsInt.bottom).toFloat()
            }
            canvas.drawText(labelText, labelX, labelY, paint)
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\BatteryView.kt =====

package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class BatteryView : AppCompatImageView {
    var battery = -1
        set(value) {
            field = value
            invalidate()
        }
    var isCharging = false
        set(value) {
            field = value
            invalidate()
        }
    private val paint = Paint()
    private val path = Path()

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        paint.isAntiAlias = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        when (heightMode) {
            MeasureSpec.EXACTLY -> {
                val wantWidth = (heightSize * 58 / 30f).toInt()
                when (widthMode) {
                    MeasureSpec.EXACTLY -> setMeasuredDimension(widthSize, heightSize)
                    MeasureSpec.AT_MOST -> setMeasuredDimension(
                        wantWidth.coerceAtMost(widthSize),
                        heightSize
                    )

                    else -> setMeasuredDimension(wantWidth, heightSize)
                }
            }

            MeasureSpec.AT_MOST -> {
                when (widthMode) {
                    MeasureSpec.EXACTLY -> setMeasuredDimension(
                        widthSize,
                        (widthSize * 30 / 58f).toInt().coerceAtMost(heightSize)
                    )

                    MeasureSpec.AT_MOST -> {
                        if (widthSize < 58) {
                            if (heightSize < 30) {//
                                if ((widthSize * 30 / 58f).toInt() <= heightSize) {
                                    setMeasuredDimension(widthSize, (widthSize * 30 / 58f).toInt())
                                } else {
                                    setMeasuredDimension(
                                        (heightSize * 58 / 30f).toInt(),
                                        heightSize
                                    )
                                }
                            } else {//
                                setMeasuredDimension(widthSize, (widthSize * 30 / 58f).toInt())
                            }
                        } else {
                            if (heightSize < 30) {//
                                setMeasuredDimension((heightSize * 58 / 30f).toInt(), heightSize)
                            } else {//
                                setMeasuredDimension(58, 30)
                            }
                        }
                    }

                    else -> setMeasuredDimension(
                        (widthSize * 30.coerceAtMost(heightSize) / 58f).toInt(),
                        30.coerceAtMost(heightSize)
                    )
                }
            }

            else -> {
                when (widthMode) {
                    MeasureSpec.EXACTLY -> setMeasuredDimension(
                        widthSize,
                        (widthSize * 30 / 58f).toInt()
                    )

                    MeasureSpec.AT_MOST -> setMeasuredDimension(
                        58.coerceAtMost(widthSize),
                        (58.coerceAtMost(widthSize) * 30 / 58f).toInt()
                    )

                    else -> setMeasuredDimension(58, 30)
                }
            }
        }
        drawWidth =
            if ((measuredWidth * 30 / 58f).toInt() <= measuredHeight) measuredWidth else (measuredHeight * 58 / 30f).toInt()
        drawHeight =
            if ((measuredWidth * 30 / 58f).toInt() <= measuredHeight) (measuredWidth * 30 / 58f).toInt() else measuredHeight
        paint.strokeWidth = drawWidth * 2 / 58f
        val levelWidth = drawWidth * 42 / 58f
        val levelHeight = drawHeight * 20 / 30f
        val radius = drawWidth * 4 / 58f
        val left = drawWidth * 5 / 58f
        val top = drawWidth * 5 / 58f
        val right = left + levelWidth
        val bottom = top + levelHeight
        path.rewind()
        path.moveTo(left + radius, top)
        path.lineTo(right - radius, top)
        path.quadTo(right, top, right, top + radius)
        path.lineTo(right, bottom - radius)
        path.quadTo(right, bottom, right - radius, bottom)
        path.lineTo(left + radius, bottom)
        path.quadTo(left, bottom, left, bottom - radius)
        path.lineTo(left, top + radius)
        path.quadTo(left, top, left + radius, top)
    }

    private var drawWidth: Int = 0
    private var drawHeight: Int = 0
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //
        val lineSize = drawWidth * 2 / 58f
        val roundSize = drawWidth * 6 / 58f
        val batteryWidth = drawWidth * 50 / 58f
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.BUTT
        paint.color = 0xff83808c.toInt()
        canvas.drawRoundRect(
            lineSize / 2,
            lineSize / 2,
            lineSize / 2 + batteryWidth,
            drawHeight.toFloat() - lineSize / 2,
            roundSize,
            roundSize,
            paint
        )
        //
        val anodeWidth = drawWidth * 3 / 58f
        val anodeHeight = drawHeight * 8 / 30f - lineSize
        val anodeX = drawWidth - anodeWidth / 2
        val anodeStartY = (drawHeight - anodeHeight) / 2
        paint.style = Paint.Style.FILL
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = anodeWidth
        canvas.drawLine(anodeX, anodeStartY, anodeX, anodeStartY + anodeHeight, paint)
        //
        if (battery <= 0) {
            return
        }
        val progressWidth = drawWidth * 42 / 58f * battery / 100
        paint.strokeCap = Paint.Cap.BUTT
        paint.color =
            (if (isCharging) 0xff6dc80e else if (battery <= 10) 0xffeb433e else 0xffffffff).toInt()
        canvas.clipPath(path)
        canvas.drawRect(
            lineSize + anodeWidth,
            lineSize + anodeWidth,
            lineSize + anodeWidth + progressWidth,
            drawHeight - lineSize - anodeWidth,
            paint
        )
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\BitmapConstraintLayout.kt =====

package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.drawToBitmap

open class BitmapConstraintLayout : ConstraintLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    @Volatile
    var viewBitmap: Bitmap? = null
    fun updateBitmap() {
        if (!isShown) {
            return
        }
        try {
            viewBitmap = this.drawToBitmap()
        } catch (_: Exception) {
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\Comm3DSeekBar.kt =====

package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatSeekBar
import com.mpdc4gsr.libunified.compat.dpToPx
import com.mpdc4gsr.libunified.compat.spToPx
import com.mpdc4gsr.libunified.R
import kotlin.math.roundToInt

class Comm3DSeekBar : AppCompatSeekBar {
    private lateinit var mPaint: TextPaint
    private val orientation: Int
    private var mMaxWidth = 48
    private var mMaxHeight = 48
    private var mMinWidth = 24
    private var mMinHeight = 24
    var level = 0;

    //
    private val mProgressTextRect: Rect = Rect()

    //
    private val mThumbWidth: Int = 50f.dpToPx(context).toInt()

    //
    private val mIndicatorWidth: Int = 50f.dpToPx(context).toInt()
    private var onSeekBarChangeListener: OnSeekBarChangeListener? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        val typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.CommSeekBar, defStyleAttr, 0)
        orientation = typedArray.getInt(R.styleable.CommSeekBar_android_orientation, 0)
        mMaxWidth =
            typedArray.getDimensionPixelSize(R.styleable.CommSeekBar_android_maxWidth, mMaxWidth)
        mMaxHeight =
            typedArray.getDimensionPixelSize(R.styleable.CommSeekBar_android_maxHeight, mMaxHeight)
        mMinWidth =
            typedArray.getDimensionPixelSize(R.styleable.CommSeekBar_android_minWidth, mMinWidth)
        mMinHeight =
            typedArray.getDimensionPixelSize(R.styleable.CommSeekBar_android_minHeight, mMinHeight)
        mPaint = TextPaint()
        mPaint.setAntiAlias(true)
        mPaint.setColor(Color.parseColor("#00574B"))
        mPaint.setTextSize(16f.spToPx(context).toFloat())
        typedArray.recycle()
    }

    override fun setOnSeekBarChangeListener(l: OnSeekBarChangeListener?) {
        if (orientation == 0) {
            super.setOnSeekBarChangeListener(l)
        } else {
            onSeekBarChangeListener = l
        }
    }

    override fun setProgress(progress: Int) {
        super.setProgress(progress)
        if (orientation != 0) {
            onSeekBarChangeListener?.onProgressChanged(this, progress, false)
        }
    }

    override fun setProgress(progress: Int, animate: Boolean) {
        super.setProgress(progress, animate)
        if (orientation != 0) {
            onSeekBarChangeListener?.onProgressChanged(this, progress, false)
        }
    }

    override fun setMax(max: Int) {
        super.setMax(max)
        if (orientation != 0) {
            onSeekBarChangeListener?.onProgressChanged(this, progress, false)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (orientation == 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        } else {
            val d = progressDrawable
            val thumbWidth = thumb?.intrinsicWidth ?: 0
            var dw = 0
            var dh = 0
            if (d != null) {
                dw = mMinWidth.coerceAtLeast(mMaxWidth.coerceAtMost(d.intrinsicWidth))
                dw = thumbWidth.coerceAtLeast(dw)
                dh = mMinHeight.coerceAtLeast(mMaxHeight.coerceAtMost(d.intrinsicHeight))
            }
            dw += paddingLeft + paddingRight
            dh += paddingTop + paddingBottom
            setMeasuredDimension(
                resolveSizeAndState(dw, widthMeasureSpec, 0),
                resolveSizeAndState(dh, heightMeasureSpec, 0)
            )
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (orientation != 0) {
            calculateDrawable(w, h)
        }
    }

    private fun calculateDrawable(w: Int, h: Int) {
        val paddingWidth: Int = w - paddingLeft - paddingRight
        val paddingHeight: Int = h - paddingTop - paddingBottom
        val trackWidth = mMaxWidth.coerceAtMost(paddingWidth)
        val thumbWidth = thumb?.intrinsicWidth ?: 0
        val thumbHeight = thumb?.intrinsicHeight ?: 0
        val trackOffset: Int
        val thumbTopOffset: Int
        if (thumbWidth > trackWidth) {
            val offsetHeight = (paddingWidth - thumbWidth) / 2
            trackOffset = offsetHeight + (thumbWidth - trackWidth) / 2
            thumbTopOffset = offsetHeight
        } else {
            val offsetHeight = (paddingWidth - trackWidth) / 2
            trackOffset = offsetHeight
            thumbTopOffset = offsetHeight + (trackWidth - thumbWidth) / 2
        }
        if (progressDrawable != null) {
            progressDrawable.setBounds(0, trackOffset, paddingHeight, trackOffset + trackWidth)
        }
        if (thumb != null) {
            val available: Int = paddingHeight - thumbHeight + thumbOffset * 2
            val left = progress / max.toFloat() * available + 0.5f
            val reviseLeft = left.coerceAtLeast(thumbHeight / 2 + 0.5f)
                .coerceAtMost(paddingHeight - thumbHeight / 2 - 0.5f).toInt()
            thumb.setBounds(
                reviseLeft,
                thumbTopOffset,
                reviseLeft + thumbHeight,
                thumbTopOffset + thumbWidth
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (orientation == 0) {
            super.onDraw(canvas)
//            val progressText = "$progress%"
//            mPaint.getTextBounds(progressText, 0, progressText.length, mProgressTextRect)
//            // 
//            val progressRatio = progress.toFloat() / max
//            // thumb
//            val thumbOffset: Float =
//                (mThumbWidth - mProgressTextRect.width()) / 2 - mThumbWidth * progressRatio
//            val thumbX = width * progressRatio + thumbOffset
//            val thumbY: Float = height / 2f + mProgressTextRect.height() / 2f
//            canvas!!.drawText(progressText, thumbX, thumbY, mPaint)
        } else {
            canvas?.let {
                it.rotate(90f)
                it.translate(-paddingStart.toFloat(), -width.toFloat() + paddingEnd)
                super.onDraw(canvas)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (orientation == 0) {
            return super.onTouchEvent(event)
        }
        if (!isEnabled) {
            return false
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isPressed = true
                trackTouchEvent(event)
                onSeekBarChangeListener?.onStartTrackingTouch(this)
            }

            MotionEvent.ACTION_MOVE -> {
                trackTouchEvent(event)
            }

            MotionEvent.ACTION_UP -> {
                isPressed = false
                trackTouchEvent(event)
                invalidate()
                onSeekBarChangeListener?.onStopTrackingTouch(this)
            }

            MotionEvent.ACTION_CANCEL -> {
                isPressed = false
                invalidate()
                stopTrackTouchLevel()
                onSeekBarChangeListener?.onStopTrackingTouch(this)
            }
        }
        return true
    }

    fun stopTrackTouchLevel() {
        if (level > 0) {
            val newLevel = (progress.toFloat() / 100 * 4).roundToInt()
            setProgress((newLevel.toFloat() / level * 100).toInt())
        }
    }

    private fun trackTouchEvent(event: MotionEvent) {
        val y = event.y.roundToInt()
        progress = if (y < paddingTop) {
            0
        } else if (y > height - paddingBottom) {
            max
        } else {
            val availableHeight: Int = height - paddingTop - paddingBottom
            val scale: Float = (y - paddingTop) / availableHeight.toFloat()
            (scale * max).roundToInt()
        }
        stopTrackTouchLevel()
        if (thumb != null) {
            calculateDrawable(width, height)
            invalidate()
        }
        onSeekBarChangeListener?.onProgressChanged(this, progress, true)
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\CommSeekBar.kt =====

package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatSeekBar
import com.mpdc4gsr.libunified.R
import kotlin.math.roundToInt

class CommSeekBar : AppCompatSeekBar {
    private val orientation: Int
    private var mMaxWidth = 48
    private var mMaxHeight = 48
    private var mMinWidth = 24
    private var mMinHeight = 24
    var level = 0;
    private var onSeekBarChangeListener: OnSeekBarChangeListener? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        val typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.CommSeekBar, defStyleAttr, 0)
        orientation = typedArray.getInt(R.styleable.CommSeekBar_android_orientation, 0)
        mMaxWidth =
            typedArray.getDimensionPixelSize(R.styleable.CommSeekBar_android_maxWidth, mMaxWidth)
        mMaxHeight =
            typedArray.getDimensionPixelSize(R.styleable.CommSeekBar_android_maxHeight, mMaxHeight)
        mMinWidth =
            typedArray.getDimensionPixelSize(R.styleable.CommSeekBar_android_minWidth, mMinWidth)
        mMinHeight =
            typedArray.getDimensionPixelSize(R.styleable.CommSeekBar_android_minHeight, mMinHeight)
        typedArray.recycle()
    }

    override fun setOnSeekBarChangeListener(l: OnSeekBarChangeListener?) {
        if (orientation == 0) {
            super.setOnSeekBarChangeListener(l)
        } else {
            onSeekBarChangeListener = l
        }
    }

    override fun setProgress(progress: Int) {
        super.setProgress(progress)
        if (orientation != 0) {
            onSeekBarChangeListener?.onProgressChanged(this, progress, false)
        }
    }

    override fun setProgress(progress: Int, animate: Boolean) {
        super.setProgress(progress, animate)
        if (orientation != 0) {
            onSeekBarChangeListener?.onProgressChanged(this, progress, false)
        }
    }

    override fun setMax(max: Int) {
        super.setMax(max)
        if (orientation != 0) {
            onSeekBarChangeListener?.onProgressChanged(this, progress, false)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (orientation == 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        } else {
            val d = progressDrawable
            val thumbWidth = thumb?.intrinsicWidth ?: 0
            var dw = 0
            var dh = 0
            if (d != null) {
                dw = mMinWidth.coerceAtLeast(mMaxWidth.coerceAtMost(d.intrinsicWidth))
                dw = thumbWidth.coerceAtLeast(dw)
                dh = mMinHeight.coerceAtLeast(mMaxHeight.coerceAtMost(d.intrinsicHeight))
            }
            dw += paddingLeft + paddingRight
            dh += paddingTop + paddingBottom
            setMeasuredDimension(
                resolveSizeAndState(dw, widthMeasureSpec, 0),
                resolveSizeAndState(dh, heightMeasureSpec, 0)
            )
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (orientation != 0) {
            calculateDrawable(w, h)
        }
    }

    private fun calculateDrawable(w: Int, h: Int) {
        val paddingWidth: Int = w - paddingLeft - paddingRight
        val paddingHeight: Int = h - paddingTop - paddingBottom
        val trackWidth = mMaxWidth.coerceAtMost(paddingWidth)
        val thumbWidth = thumb?.intrinsicWidth ?: 0
        val thumbHeight = thumb?.intrinsicHeight ?: 0
        val trackOffset: Int
        val thumbTopOffset: Int
        if (thumbWidth > trackWidth) {
            val offsetHeight = (paddingWidth - thumbWidth) / 2
            trackOffset = offsetHeight + (thumbWidth - trackWidth) / 2
            thumbTopOffset = offsetHeight
        } else {
            val offsetHeight = (paddingWidth - trackWidth) / 2
            trackOffset = offsetHeight
            thumbTopOffset = offsetHeight + (trackWidth - thumbWidth) / 2
        }
        if (progressDrawable != null) {
            progressDrawable.setBounds(0, trackOffset, paddingHeight, trackOffset + trackWidth)
        }
        if (thumb != null) {
            val available: Int = paddingHeight - thumbHeight + thumbOffset * 2
            val left = progress / max.toFloat() * available + 0.5f
            val reviseLeft = left.coerceAtLeast(thumbHeight / 2 + 0.5f)
                .coerceAtMost(paddingHeight - thumbHeight / 2 - 0.5f).toInt()
            thumb.setBounds(
                reviseLeft,
                thumbTopOffset,
                reviseLeft + thumbHeight,
                thumbTopOffset + thumbWidth
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (orientation == 0) {
            super.onDraw(canvas)
        } else {
            canvas?.let {
                it.rotate(90f)
                it.translate(-paddingStart.toFloat(), -width.toFloat() + paddingEnd)
                super.onDraw(canvas)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (orientation == 0) {
            return super.onTouchEvent(event)
        }
        if (!isEnabled) {
            return false
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isPressed = true
                trackTouchEvent(event)
                onSeekBarChangeListener?.onStartTrackingTouch(this)
            }

            MotionEvent.ACTION_MOVE -> {
                trackTouchEvent(event)
            }

            MotionEvent.ACTION_UP -> {
                isPressed = false
                trackTouchEvent(event)
                invalidate()
                onSeekBarChangeListener?.onStopTrackingTouch(this)
            }

            MotionEvent.ACTION_CANCEL -> {
                isPressed = false
                invalidate()
                stopTrackTouchLevel()
                onSeekBarChangeListener?.onStopTrackingTouch(this)
            }
        }
        return true
    }

    fun stopTrackTouchLevel() {
        if (level > 0) {
            val newLevel = (progress.toFloat() / 100 * 4).roundToInt()
            setProgress((newLevel.toFloat() / level * 100).toInt())
        }
    }

    private fun trackTouchEvent(event: MotionEvent) {
        val y = event.y.roundToInt()
        progress = if (y < paddingTop) {
            0
        } else if (y > height - paddingBottom) {
            max
        } else {
            val availableHeight: Int = height - paddingTop - paddingBottom
            val scale: Float = (y - paddingTop) / availableHeight.toFloat()
            (scale * max).roundToInt()
        }
        stopTrackTouchLevel()
        if (thumb != null) {
            calculateDrawable(width, height)
            invalidate()
        }
        onSeekBarChangeListener?.onProgressChanged(this, progress, true)
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\CountDownView.kt =====

package com.mpdc4gsr.libunified.ui.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.mpdc4gsr.libunified.R

class CountDownView : View {
    //
    private var mRingColor = 0

    //
    private var mRingWidth = 0

    //
    private var mRingProgressTextSize = 0

    //
    private var mWidth = 0

    //
    private var mHeight = 0

    //
    private var mRingText: String? = null
    private lateinit var mPaint: Paint
    private lateinit var mTextPaint: Paint

    //
    private var mRectF: RectF? = null

    //
    private var mProgressTextColor = 0
    private var mCountdownTime = 0
    private var mCurrentProgress = 0f
    private var valueAnimator: ValueAnimator? = null
    private var mListener: OnCountDownListener? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.CountDownView)
        for (i in 0 until ta.indexCount) {
            when (ta.getIndex(i)) {
                R.styleable.CountDownView_ringColor -> mRingColor =
                    ta.getColor(
                        R.styleable.CountDownView_ringColor,
                        ContextCompat.getColor(context, R.color.colorAccent)
                    )

                R.styleable.CountDownView_ringWidth -> mRingWidth =
                    ta.getDimensionPixelSize(
                        R.styleable.CountDownView_ringWidth,
                        40
                    )

                R.styleable.CountDownView_progressTextSize -> mRingProgressTextSize =
                    ta.getDimensionPixelSize(
                        R.styleable.CountDownView_progressTextSize,
                        20
                    )

                R.styleable.CountDownView_progressTextColor -> mProgressTextColor =
                    ta.getColor(
                        R.styleable.CountDownView_progressTextColor,
                        ContextCompat.getColor(context, R.color.colorAccent)
                    )

                R.styleable.CountDownView_countdownTime -> mCountdownTime =
                    ta.getInteger(
                        R.styleable.CountDownView_countdownTime,
                        60
                    )

                R.styleable.CountDownView_progressText -> mRingText =
                    ta.getString(R.styleable.CountDownView_progressText)
            }
        }
        ta.recycle()
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mPaint.isAntiAlias = true
        mTextPaint = Paint()
        this.setWillNotDraw(false)
    }

    @SuppressLint("DrawAllocation")
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        mWidth = measuredWidth
        mHeight = measuredHeight
        mRectF = RectF(
            0 + mRingWidth / 2f,
            0 + mRingWidth / 2f,
            mWidth - mRingWidth / 2f,
            mHeight - mRingWidth / 2f
        )
    }

    fun setCountdownTime(mCountdownTime: Int) {
        this.mCountdownTime = mCountdownTime
        mRingText = mCountdownTime.toString()
        invalidate()
    }

    private fun getValueAnimator(countdownTime: Long): ValueAnimator? {
        val valueAnimator = ValueAnimator.ofFloat(0f, 100f)
        valueAnimator.duration = countdownTime
        valueAnimator.interpolator = LinearInterpolator()
        valueAnimator.repeatCount = 0
        return valueAnimator
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //
        mPaint.color = mRingColor
        mPaint.style = Paint.Style.FILL
        mPaint.strokeWidth = mRingWidth.toFloat()
        canvas.drawArc(mRectF!!, -90f, mCurrentProgress - 360, false, mPaint)
        val font = ResourcesCompat.getFont(context, R.font.roboto_regular)
        //
        mTextPaint.isAntiAlias = true
        mTextPaint.textAlign = Paint.Align.CENTER
        mTextPaint.typeface = font
        // (5 4 3 2 1)
        // val text: String = (mCountdownTime - (mCurrentProgress / 360f * mCountdownTime)).toInt().toString()
        mTextPaint.textSize = mRingProgressTextSize.toFloat()
        mTextPaint.color = mProgressTextColor
        //
        val fontMetrics = mTextPaint.fontMetricsInt
        val baseline =
            ((mRectF!!.bottom + mRectF!!.top - fontMetrics.bottom - fontMetrics.top) / 2).toInt()
        canvas.drawText(mRingText!!, mRectF!!.centerX(), baseline.toFloat(), mTextPaint)
    }

    fun startCountDown() {
        if (!isAttachedToWindow) {
            return
        }
        valueAnimator = getValueAnimator((mCountdownTime * 1000).toLong())
        valueAnimator!!.addUpdateListener { animation ->
            val i = animation.animatedValue.toString().toFloat()
            mCurrentProgress = (360 * (i / 100f))
            invalidate()
        }
        if (isAttachedToWindow) {
            valueAnimator!!.start()
        }
        valueAnimator!!.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                //
                if (mListener != null) {
                    mListener!!.countDownFinished()
                }
            }
        })
    }

    fun stopCountDown() {
        if (valueAnimator!!.isRunning) {
            valueAnimator!!.cancel()
        }
    }

    fun setOnCountDownListener(mListener: OnCountDownListener) {
        this.mListener = mListener
    }

    interface OnCountDownListener {
        fun countDownFinished()
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\LiteSurfaceView.kt =====

package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.SurfaceView
import java.nio.ByteBuffer

class LiteSurfaceView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : SurfaceView(context, attrs) {
    var mFinalImageWidth = 0
    var mFinalImageHeight = 0
    var tmpData: ByteArray? = null
    var mIrRotateData: ByteArray? = null
    var imageBitmap: Bitmap? = null
    fun scaleBitmap(): Bitmap {
        try {
            val irData =
                mIrRotateData ?: return Bitmap.createBitmap(
                    measuredWidth,
                    measuredHeight,
                    Bitmap.Config.ARGB_8888
                )
            if (tmpData == null) {
                tmpData = ByteArray(irData.size)
            }
            val tempData = tmpData ?: return Bitmap.createBitmap(
                measuredWidth,
                measuredHeight,
                Bitmap.Config.ARGB_8888
            )
            System.arraycopy(irData, 0, tempData, 0, irData.size)
            if (imageBitmap == null || imageBitmap!!.getWidth() != mFinalImageWidth) {
                imageBitmap =
                    Bitmap.createBitmap(
                        mFinalImageWidth,
                        mFinalImageHeight,
                        Bitmap.Config.ARGB_8888
                    )
            }
            imageBitmap?.copyPixelsFromBuffer(ByteBuffer.wrap(tempData))
            return Bitmap.createScaledBitmap(
                imageBitmap!!,
                measuredWidth, measuredHeight, true
            )
        } catch (e: Exception) {
            return Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\MarqueeButton.kt =====

package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton

class MarqueeButton : AppCompatButton {
    constructor (context: Context) : super(context)
    constructor (context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor (context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    override fun isFocused(): Boolean {
        return true
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\MarqueeText.kt =====

package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class MarqueeText : AppCompatTextView {
    constructor (context: Context) : super(context)
    constructor (context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor (context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    override fun isFocused(): Boolean {
        return true
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\MyItemDecoration.kt =====

package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

class MyItemDecoration(context: Context) : RecyclerView.ItemDecoration() {
    var wholeLeft: Float? = null
    var wholeRight: Float? = null
    var wholeTop: Float? = null
    var wholeBottom: Float? = null
    var itemLeft: Float? = null
    var itemRight: Float? = null
    var itemTop: Float? = null
    var itemBottom: Float? = null
    private val density: Float = context.resources.displayMetrics.density
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        val itemCount = parent.adapter?.itemCount ?: return
        val position = parent.getChildAdapterPosition(view)
        val layoutManager = parent.layoutManager
        when (layoutManager) {
            is GridLayoutManager -> {
                if (layoutManager.orientation == LinearLayoutManager.VERTICAL) {
                    setVerticalMulti(outRect, position, itemCount, layoutManager.spanCount)
                } else {
                    setHorizontalMulti(outRect, position, itemCount, layoutManager.spanCount)
                }
            }

            is LinearLayoutManager -> {
                if (layoutManager.orientation == LinearLayoutManager.VERTICAL) {
                    setVerticalOne(outRect, position, itemCount)
                } else {
                    setHorizontalOne(outRect, position, itemCount)
                }
            }

            is StaggeredGridLayoutManager -> {
                val layoutParams = view.layoutParams
                val spanIndex =
                    if (layoutParams is StaggeredGridLayoutManager.LayoutParams) layoutParams.spanIndex else 0
                if (layoutManager.orientation == LinearLayoutManager.VERTICAL) {
                    setVerticalMultiStaggered(
                        outRect,
                        position,
                        itemCount,
                        layoutManager.spanCount,
                        spanIndex
                    )
                } else {
                    setHorizontalMulti(outRect, position, itemCount, layoutManager.spanCount)
                }
            }
        }
    }

    private fun setVerticalOne(outRect: Rect, position: Int, itemCount: Int) {
        val left: Int = dp2px(wholeLeft ?: ((itemLeft ?: 0f) * 2))
        val right: Int = dp2px(wholeRight ?: ((itemRight ?: 0f) * 2))
        val top: Int =
            dp2px(if (position == 0) wholeTop ?: ((itemTop ?: 0f) * 2) else (itemTop ?: 0f))
        val bottom: Int =
            dp2px(
                if (position == itemCount - 1) wholeBottom ?: ((itemBottom
                    ?: 0f) * 2) else (itemBottom ?: 0f)
            )
        outRect.set(left, top, right, bottom)
    }

    private fun setHorizontalOne(outRect: Rect, position: Int, itemCount: Int) {
        val left: Int =
            dp2px(if (position == 0) wholeLeft ?: ((itemLeft ?: 0f) * 2) else (itemLeft ?: 0f))
        val right: Int =
            dp2px(
                if (position == itemCount - 1) wholeRight ?: ((itemRight ?: 0f) * 2) else (itemRight
                    ?: 0f)
            )
        val top: Int = dp2px(wholeTop ?: ((itemTop ?: 0f) * 2))
        val bottom: Int = dp2px(wholeBottom ?: ((itemBottom ?: 0f) * 2))
        outRect.set(left, top, right, bottom)
    }

    private fun setVerticalMulti(outRect: Rect, position: Int, itemCount: Int, spanCount: Int) {
        val totalRow = itemCount / spanCount + if (itemCount % spanCount == 0) 0 else 1 //
        val rowPosition = position / spanCount    // position [0, totalRow)
        val columnPosition = position % spanCount // position [0, spanCount)
        val left: Int = dp2px(
            if (columnPosition == 0) wholeLeft ?: ((itemLeft ?: 0f) * 2) else (itemLeft ?: 0f)
        )
        val right: Int =
            dp2px(
                if (columnPosition == spanCount - 1) wholeRight ?: ((itemRight
                    ?: 0f) * 2) else (itemRight ?: 0f)
            )
        val top: Int =
            dp2px(if (rowPosition == 0) wholeTop ?: ((itemTop ?: 0f) * 2) else (itemTop ?: 0f))
        val bottom: Int =
            dp2px(
                if (rowPosition == totalRow - 1) wholeBottom ?: ((itemBottom
                    ?: 0f) * 2) else (itemBottom ?: 0f)
            )
        outRect.set(left, top, right, bottom)
    }

    private fun setVerticalMultiStaggered(
        outRect: Rect,
        position: Int,
        itemCount: Int,
        spanCount: Int,
        spanIndex: Int
    ) {
        val totalRow = itemCount / spanCount + if (itemCount % spanCount == 0) 0 else 1 //
        val rowPosition = position / spanCount //position[0, totalRow)
        val left: Int =
            dp2px(if (spanIndex == 0) wholeLeft ?: ((itemLeft ?: 0f) * 2) else (itemLeft ?: 0f))
        val right: Int =
            dp2px(
                if (spanIndex == spanCount - 1) wholeRight ?: ((itemRight
                    ?: 0f) * 2) else (itemRight ?: 0f)
            )
        val top: Int =
            dp2px(if (rowPosition == 0) wholeTop ?: ((itemTop ?: 0f) * 2) else (itemTop ?: 0f))
        val bottom: Int =
            dp2px(
                if (rowPosition == totalRow - 1) wholeBottom ?: ((itemBottom
                    ?: 0f) * 2) else (itemBottom ?: 0f)
            )
        outRect.set(left, top, right, bottom)
    }

    private fun setHorizontalMulti(outRect: Rect, position: Int, itemCount: Int, spanCount: Int) {
        // MVP implementation: Basic horizontal multi-row spacing
        // Can be enhanced when horizontal multi-row requirements are clarified
        val column = position % spanCount
        val row = position / spanCount
        val left: Int =
            dp2px(if (column == 0) wholeLeft ?: ((itemLeft ?: 0f) * 2) else (itemLeft ?: 0f))
        val right: Int =
            dp2px(
                if (column == spanCount - 1) wholeRight ?: ((itemRight ?: 0f) * 2) else (itemRight
                    ?: 0f)
            )
        val top: Int = dp2px(if (row == 0) wholeTop ?: ((itemTop ?: 0f) * 2) else (itemTop ?: 0f))
        val bottom: Int = dp2px(wholeBottom ?: ((itemBottom ?: 0f) * 2))
        outRect.set(left, top, right, bottom)
    }

    private fun dp2px(dpValue: Float): Int = (dpValue * density + 0.5f).toInt()
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\NoScrollViewPager.kt =====

package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

class NoScrollViewPager : ViewPager {
    private var isCanScroll = false

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return isCanScroll && super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        return isCanScroll && super.onTouchEvent(ev)
    }

    override fun setCurrentItem(item: Int) {
        //
        super.setCurrentItem(item, false)
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\RadioGroupPlus.kt =====

package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.annotation.IdRes

class RadioGroupPlus : LinearLayout {
    // holds the checked id; the selection is empty by default
    @get:IdRes
    var checkedRadioButtonId = -1
        private set

    // tracks children radio buttons checked state
    private var mChildOnCheckedChangeListener: CompoundButton.OnCheckedChangeListener? = null

    // when true, mOnCheckedChangeListener discards events
    private var mProtectFromCheckedChange = false
    private var mOnCheckedChangeListener: OnCheckedChangeListener? = null
    private var mPassThroughListener: PassThroughHierarchyChangeListener? = null

    constructor(context: Context?) : super(context) {
        orientation = VERTICAL
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        // MVP implementation: Basic attribute handling for RadioGroupPlus
        // Enhanced attribute processing can be added as needed
        init()
    }

    private fun init() {
        mChildOnCheckedChangeListener = CheckedStateTracker()
        mPassThroughListener = PassThroughHierarchyChangeListener()
        super.setOnHierarchyChangeListener(mPassThroughListener)
    }

    override fun setOnHierarchyChangeListener(listener: OnHierarchyChangeListener) {
        // the user listener is delegated to our pass-through listener
        mPassThroughListener!!.mOnHierarchyChangeListener = listener
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        // checks the appropriate radio button as requested in the XML file
        if (checkedRadioButtonId != -1) {
            mProtectFromCheckedChange = true
            setCheckedStateForView(checkedRadioButtonId, true)
            mProtectFromCheckedChange = false
            setCheckedId(checkedRadioButtonId)
        }
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        if (child is RadioButton) {
            val button = child
            if (button.isChecked) {
                mProtectFromCheckedChange = true
                if (checkedRadioButtonId != -1) {
                    setCheckedStateForView(checkedRadioButtonId, false)
                }
                mProtectFromCheckedChange = false
                setCheckedId(button.id)
            }
        }
        super.addView(child, index, params)
    }

    fun check(@IdRes id: Int) {
        // don't even bother
        if (id != -1 && id == checkedRadioButtonId) {
            return
        }
        if (checkedRadioButtonId != -1) {
            setCheckedStateForView(checkedRadioButtonId, false)
        }
        if (id != -1) {
            setCheckedStateForView(id, true)
        }
        setCheckedId(id)
    }

    private fun setCheckedId(@IdRes id: Int) {
        checkedRadioButtonId = id
        if (mOnCheckedChangeListener != null) {
            mOnCheckedChangeListener!!.onCheckedChanged(this, checkedRadioButtonId)
        }
    }

    private fun setCheckedStateForView(viewId: Int, checked: Boolean) {
        val checkedView = findViewById<View>(viewId)
        if (checkedView != null && checkedView is RadioButton) {
            checkedView.isChecked = checked
        }
    }

    fun clearCheck() {
        check(-1)
    }

    fun setOnCheckedChangeListener(listener: OnCheckedChangeListener) {
        mOnCheckedChangeListener = listener
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams): Boolean {
        return p is RadioGroup.LayoutParams
    }

    override fun generateDefaultLayoutParams(): LinearLayout.LayoutParams {
        return LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun getAccessibilityClassName(): CharSequence {
        return RadioGroup::class.java.name
    }

    class LayoutParams : LinearLayout.LayoutParams {
        constructor(c: Context?, attrs: AttributeSet?) : super(c, attrs) {}
        constructor(w: Int, h: Int) : super(w, h) {}
        constructor(w: Int, h: Int, initWeight: Float) : super(w, h, initWeight) {}
        constructor(p: ViewGroup.LayoutParams?) : super(p) {}
        constructor(source: MarginLayoutParams?) : super(source) {}

        override fun setBaseAttributes(
            a: TypedArray,
            widthAttr: Int, heightAttr: Int
        ) {
            width = if (a.hasValue(widthAttr)) {
                a.getLayoutDimension(widthAttr, "layout_width")
            } else {
                WRAP_CONTENT
            }
            height = if (a.hasValue(heightAttr)) {
                a.getLayoutDimension(heightAttr, "layout_height")
            } else {
                WRAP_CONTENT
            }
        }
    }

    interface OnCheckedChangeListener {
        fun onCheckedChanged(group: RadioGroupPlus, @IdRes checkedId: Int)
    }

    private inner class CheckedStateTracker : CompoundButton.OnCheckedChangeListener {
        override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
            // prevents from infinite recursion
            if (mProtectFromCheckedChange) {
                return
            }
            mProtectFromCheckedChange = true
            if (checkedRadioButtonId != -1) {
                setCheckedStateForView(checkedRadioButtonId, false)
            }
            mProtectFromCheckedChange = false
            val id = buttonView.id
            setCheckedId(id)
        }
    }

    private inner class PassThroughHierarchyChangeListener :
        OnHierarchyChangeListener {
        var mOnHierarchyChangeListener: OnHierarchyChangeListener? = null
        fun traverseTree(view: View) {
            if (view is RadioButton) {
                var id = view.getId()
                // generates an id if it's missing
                if (id == NO_ID) {
                    id = generateViewId()
                    view.setId(id)
                }
                view.setOnCheckedChangeListener(
                    mChildOnCheckedChangeListener
                )
            }
            if (view !is ViewGroup) {
                return
            }
            val viewGroup = view
            if (viewGroup.childCount == 0) {
                return
            }
            for (i in 0 until viewGroup.childCount) {
                traverseTree(viewGroup.getChildAt(i))
            }
        }

        override fun onChildViewAdded(parent: View, child: View) {
            traverseTree(child)
            if (parent === this@RadioGroupPlus && child is RadioButton) {
                var id = child.getId()
                // generates an id if it's missing
                if (id == NO_ID) {
                    id = generateViewId()
                    child.setId(id)
                }
                child.setOnCheckedChangeListener(
                    mChildOnCheckedChangeListener
                )
            }
            mOnHierarchyChangeListener?.onChildViewAdded(parent, child)
        }

        override fun onChildViewRemoved(parent: View, child: View) {
            if (parent === this@RadioGroupPlus && child is RadioButton) {
                child.setOnCheckedChangeListener(null)
            }
            mOnHierarchyChangeListener?.onChildViewRemoved(parent, child)
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\RoundImageView.kt =====

package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.mpdc4gsr.libunified.R

class RoundImageView : AppCompatImageView {
    companion object {
        const val LEFT_TOP = 1
        const val RIGHT_TOP = 2
        const val LEFT_BOTTOM = 4
        const val RIGHT_BOTTOM = 8
        private const val DEFAULT_RADIUS = 10f
        private const val DEFAULT_POSITION = 15
    }

    var position = 0 //
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }
    private var radius = 0 //ï¼Œ px
    private val path = Path()//
    private var density = 0f //ï¼Œdppx

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        density = context.resources.displayMetrics.density
        val typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.RoundImageView, defStyleAttr, 0)
        radius = typedArray.getDimensionPixelSize(
            R.styleable.RoundImageView_round_radius,
            dp2px(DEFAULT_RADIUS)
        )
        position = typedArray.getInt(R.styleable.RoundImageView_round_position, DEFAULT_POSITION)
        typedArray.recycle()
    }

    override fun onDraw(canvas: Canvas) {
        path.rewind()
        if (position and LEFT_TOP == LEFT_TOP) {
            path.moveTo(radius.toFloat(), 0f)
        }
        if (position and RIGHT_TOP == RIGHT_TOP) {
            path.lineTo((width - radius).toFloat(), 0f)
            path.quadTo(width.toFloat(), 0f, width.toFloat(), radius.toFloat())
        } else {
            path.lineTo(width.toFloat(), 0f)
        }
        if (position and RIGHT_BOTTOM == RIGHT_BOTTOM) {
            path.lineTo(width.toFloat(), (height - radius).toFloat())
            path.quadTo(
                width.toFloat(),
                height.toFloat(),
                (width - radius).toFloat(),
                height.toFloat()
            )
        } else {
            path.lineTo(width.toFloat(), height.toFloat())
        }
        if (position and LEFT_BOTTOM == LEFT_BOTTOM) {
            path.lineTo(radius.toFloat(), height.toFloat())
            path.quadTo(0f, height.toFloat(), 0f, (height - radius).toFloat())
        } else {
            path.lineTo(0f, height.toFloat())
        }
        if (position and LEFT_TOP == LEFT_TOP) {
            path.lineTo(0f, radius.toFloat())
            path.quadTo(0f, 0f, radius.toFloat(), 0f)
        } else {
            path.lineTo(0f, 0f)
        }
        canvas.clipPath(path)
        super.onDraw(canvas)
    }

    fun setRadius(radius: Float) {
        if (this.radius != dp2px(radius)) {
            this.radius = dp2px(radius)
            invalidate()
        }
    }

    private fun dp2px(dpValue: Float): Int {
        return (dpValue * density + 0.5f).toInt()
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\SteeringWheelView.kt =====

package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.OnClickListener
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.mpdc4gsr.libunified.R

class SteeringWheelView : LinearLayout, OnClickListener {
    private lateinit var tvConfirm: TextView
    private lateinit var steeringWheelStartBtn: ImageView
    private lateinit var steeringWheelCenterBtn: ImageView
    private lateinit var steeringWheelEndBtn: ImageView
    var listener: ((action: Int, moveX: Int) -> Unit)? = null
    var moveX = 30
    var rotationIR = 270
        set(value) {
            field = value
            if (value == 270 || value == 90) {
                if (::tvConfirm.isInitialized) tvConfirm.rotation = 270f
                rotation = 90f
            } else {
                if (::tvConfirm.isInitialized) tvConfirm.rotation = 0f
                rotation = 0f
            }
            requestLayout()
        }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private fun initView() {
        inflate(context, R.layout.ui_steering_wheel_view, this)
        tvConfirm = findViewById(R.id.tv_confirm)
        steeringWheelStartBtn = findViewById(R.id.steering_wheel_start_btn)
        steeringWheelCenterBtn = findViewById(R.id.steering_wheel_center_btn)
        steeringWheelEndBtn = findViewById(R.id.steering_wheel_end_btn)
        steeringWheelStartBtn.setOnClickListener(this)
        steeringWheelCenterBtn.setOnClickListener(this)
        steeringWheelEndBtn.setOnClickListener(this)
        if (rotationIR == 270 || rotationIR == 90) {
            tvConfirm.rotation = 270f
            rotation = 90f
        } else {
            tvConfirm.rotation = 0f
            rotation = 0f
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            steeringWheelStartBtn -> {
                moveX += 10
                if (moveX > 60) {
                    moveX = 60
                }
                listener?.invoke(-1, moveX)
            }

            steeringWheelCenterBtn -> {
                listener?.invoke(0, moveX)
            }

            steeringWheelEndBtn -> {
                moveX -= 10
                if (moveX < -20) {
                    moveX = -20
                }
                listener?.invoke(1, moveX)
            }
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\TipsSeekBar.kt =====

package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.mpdc4gsr.libunified.compat.dpToPx
import com.mpdc4gsr.libunified.compat.spToPx
import com.mpdc4gsr.libunified.R

class TipsSeekBar : ViewGroup, SeekBar.OnSeekBarChangeListener {
    private val tipsPercent: Float
    private val seekPercent: Float
    private val seekBar: SeekBar
    private val tvTips: TextView
    private val tvMin: TextView
    private val tvMax: TextView
    var progress: Int
        get() {
            return seekBar.progress
        }
        set(value) {
            seekBar.progress = value
            if (valueFormatListener != null) {
                tvTips.text = valueFormatListener?.invoke(value)
            }
        }
    var valueText: String
        get() {
            return tvTips.text.toString()
        }
        set(value) {
            tvTips.text = value
        }
    var onProgressChangeListener: ((progress: Int, fromUser: Boolean) -> Unit)? = null
    var onStopTrackingTouch: ((progress: Int) -> Unit)? = null
    var valueFormatListener: ((progress: Int) -> CharSequence?)? = null
        set(value) {
            tvTips.text = value?.invoke(seekBar.progress)
            field = value
        }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        0
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    ) {
        // seekBar  maxHeight  29  xml ï¼Œ View  maxHeight, attr  seekBar
        val thumb = ContextCompat.getDrawable(context, R.drawable.ic_tips_seek_bar_thumb)
        val thumbWidth = thumb?.intrinsicWidth ?: 0
        seekBar = SeekBar(context, attrs)
        seekBar.splitTrack = false
        seekBar.thumb = thumb
        seekBar.progressDrawable =
            ContextCompat.getDrawable(context, R.drawable.ui_progress_ir_camera_setting)
        seekBar.setPadding(thumbWidth / 2, 0, thumbWidth / 2, 0)
        seekBar.setOnSeekBarChangeListener(this)
        addView(seekBar, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        tvTips = TextView(context)
        tvTips.text = seekBar.progress.toString()
        tvTips.textSize = 12f
        tvTips.gravity = Gravity.CENTER
        tvTips.paint.isFakeBoldText = true
        tvTips.setTextColor(0xff16131e.toInt())
        tvTips.setBackgroundResource(R.drawable.ic_tips_seek_bar_tips_bg)
        addView(tvTips)
        val typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.TipsSeekBar, defStyleAttr, 0)
        val minText = typedArray.getText(R.styleable.TipsSeekBar_minText)
        val maxText = typedArray.getText(R.styleable.TipsSeekBar_maxText)
        tipsPercent = typedArray.getFraction(R.styleable.TipsSeekBar_tipsPercent, 1, 1, 0f)
        seekPercent = typedArray.getFraction(R.styleable.TipsSeekBar_seekPercent, 1, 1, 0f)
        typedArray.recycle()
        tvMin = TextView(context)
        tvMin.text = minText
        tvMin.textSize = 14f
        tvMin.setTextColor(0xffffffff.toInt())
        addView(tvMin)
        tvMax = TextView(context)
        tvMax.text = maxText
        tvMax.textSize = 14f
        tvMax.setTextColor(0xffffffff.toInt())
        addView(tvMax)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val width =
            if (widthMode == MeasureSpec.UNSPECIFIED) context.resources.displayMetrics.widthPixels else widthSize
        for (i in 0 until childCount) {
            when (val child = getChildAt(i)) {
                seekBar -> {
                    val childWidthSpec = MeasureSpec.makeMeasureSpec(
                        (width * seekPercent).toInt(),
                        MeasureSpec.EXACTLY
                    )
                    val childHeightSpc =
                        MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.AT_MOST)
                    child.measure(
                        childWidthSpec,
                        if (heightMode == MeasureSpec.EXACTLY) childHeightSpc else heightMeasureSpec
                    )
                }

                tvTips -> {
                    val tipsWidth = (width * tipsPercent).toInt()
                    val tipsHeight = (tipsWidth * 44 / 56f).toInt()
                    val childWidthSpec = MeasureSpec.makeMeasureSpec(tipsWidth, MeasureSpec.EXACTLY)
                    val childHeightSpc =
                        MeasureSpec.makeMeasureSpec(tipsHeight, MeasureSpec.EXACTLY)
                    child.measure(childWidthSpec, childHeightSpc)
                }

                else -> {
                    measureChild(child, widthMeasureSpec, heightMeasureSpec)
                }
            }
        }
        val height =
            tvTips.measuredHeight + 5f.dpToPx(context).toInt() + (seekBar.thumb?.intrinsicHeight
                ?: seekBar.measuredHeight)
        setMeasuredDimension(width, if (heightMode == MeasureSpec.EXACTLY) heightSize else height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val childWidth = child.measuredWidth
            val childHeight = child.measuredHeight
            when (child) {
                seekBar -> {
                    val top = paddingTop + tvTips.measuredHeight + 5f.dpToPx(context).toInt()
                    val left = (measuredWidth - childWidth) / 2
                    child.layout(left, top, left + childWidth, top + childHeight)
                }

                tvTips -> {
                    val seekBarSeeWidth =
                        seekBar.measuredWidth - seekBar.paddingLeft - seekBar.paddingRight
                    val baseLeft = (measuredWidth - seekBarSeeWidth) / 2
                    val progressLeft =
                        (seekBarSeeWidth * seekBar.progress / seekBar.max.toFloat()).toInt()
                    val left = baseLeft + progressLeft - childWidth / 2
                    child.layout(left, paddingTop, left + childWidth, paddingTop + childHeight)
                }

                tvMin -> {
                    val baseTop = paddingTop + tvTips.measuredHeight + 5f.dpToPx(context).toInt()
                    val top = baseTop + (seekBar.measuredHeight - childHeight) / 2
                    child.layout(paddingStart, top, paddingStart + childWidth, top + childHeight)
                }

                tvMax -> {
                    val baseTop = paddingTop + tvTips.measuredHeight + 5f.dpToPx(context).toInt()
                    val top = baseTop + (seekBar.measuredHeight - childHeight) / 2
                    val left = measuredWidth - paddingEnd - childWidth
                    child.layout(left, top, left + childWidth, top + childHeight)
                }
            }
        }
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        tvTips.text =
            if (valueFormatListener == null) progress.toString() else valueFormatListener?.invoke(
                progress
            )
        requestLayout()
        onProgressChangeListener?.invoke(progress, fromUser)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        onStopTrackingTouch?.invoke(this.seekBar.progress)
    }

    fun getFormattedValue(): String {
        return valueFormatListener?.invoke(progress)?.toString() ?: progress.toString()
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\WifiSteeringWheelView.kt =====

package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.OnClickListener
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.mpdc4gsr.libunified.R

class WifiSteeringWheelView : LinearLayout, OnClickListener {
    private lateinit var tvConfirm: TextView
    private lateinit var steeringWheelStartBtn: ImageView
    private lateinit var steeringWheelCenterBtn: ImageView
    private lateinit var steeringWheelEndBtn: ImageView
    private lateinit var steeringWheelTopBtn: ImageView
    private lateinit var steeringWheelBottomBtn: ImageView
    var listener: ((action: Int, moveX: Int, moveY: Int) -> Unit)? = null
    var moveX = 0
    var moveY = 0
    var rotationIR = 270
        set(value) {
            field = value
            if (value == 270 || value == 90) {
                if (::tvConfirm.isInitialized) tvConfirm.rotation = 270f
                rotation = 90f
            } else {
                if (::tvConfirm.isInitialized) tvConfirm.rotation = 0f
                rotation = 0f
            }
            requestLayout()
        }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private fun initView() {
        inflate(context, R.layout.ui_wifi_steering_wheel_view, this)
        tvConfirm = findViewById(R.id.tv_confirm)
        steeringWheelStartBtn = findViewById(R.id.steering_wheel_start_btn)
        steeringWheelCenterBtn = findViewById(R.id.steering_wheel_center_btn)
        steeringWheelEndBtn = findViewById(R.id.steering_wheel_end_btn)
        steeringWheelTopBtn = findViewById(R.id.steering_wheel_top_btn)
        steeringWheelBottomBtn = findViewById(R.id.steering_wheel_bottom_btn)
        steeringWheelStartBtn.setOnClickListener(this)
        steeringWheelCenterBtn.setOnClickListener(this)
        steeringWheelEndBtn.setOnClickListener(this)
        steeringWheelTopBtn.setOnClickListener(this)
        steeringWheelBottomBtn.setOnClickListener(this)
        if (rotationIR == 270 || rotationIR == 90) {
            tvConfirm.rotation = 270f
            rotation = 90f
        } else {
            tvConfirm.rotation = 0f
            rotation = 0f
        }
    }

    val moveI = 2
    override fun onClick(v: View?) {
        when (v) {
            steeringWheelStartBtn -> {
//                moveY -= moveI
                listener?.invoke(-1, moveX, moveY)
            }

            steeringWheelCenterBtn -> {
                listener?.invoke(0, moveX, moveY)
            }

            steeringWheelTopBtn -> {
//                moveX += moveI
                listener?.invoke(2, moveX, moveY)
            }

            steeringWheelBottomBtn -> {
//                moveX -= moveI
                listener?.invoke(3, moveX, moveY)
            }

            steeringWheelEndBtn -> {
//                moveY += moveI
                listener?.invoke(1, moveX, moveY)
            }
        }
    }
}


