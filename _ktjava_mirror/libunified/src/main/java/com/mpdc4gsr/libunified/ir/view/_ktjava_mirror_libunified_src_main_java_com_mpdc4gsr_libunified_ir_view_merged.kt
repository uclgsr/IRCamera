// Merged ALL .kt and .java files from the '_ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\ir\view' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:45


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\ir\view\libunified_src_main_java_com_mpdc4gsr_libunified_ir_view_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\ir\view' subtree
// Files: 2; Generated 2025-10-07 23:07:50


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ir\view\CaliperImageView.kt =====

package com.mpdc4gsr.libunified.ir.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import com.mpdc4gsr.libunified.R

class CaliperImageView : AppCompatImageView {
    private var showBitmapWidth: Float = 0f
    private var showBitmapHeight: Float = 0F
    private var yscale: Float = 1f
    private var xscale: Float = 1f
    private var parentViewHeight: Float = 0f
    private var parentViewWidth: Float = 0f
    private var imageHeight: Int = 0
    private var imageWidth: Int = 0
    private var originalBitmapHeight: Float = 0f
    private var originalBitmapWidth: Float = 0f
    private var originalBitmap: Bitmap? = null
    private val pxBitmapHeight = 150f
    private var l: Int = 0
    private var r: Int = 0
    private var t: Int = 0
    private var b: Int = 0

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    )

    private fun initView() {
        originalBitmap = (androidx.core.content.ContextCompat.getDrawable(
            context,
            R.drawable.svg_ic_target_horizontal_person_green
        ) as? BitmapDrawable)?.bitmap
        originalBitmapWidth = originalBitmap?.width?.toFloat() ?: 0f
        originalBitmapHeight = originalBitmap?.height?.toFloat() ?: 0f
        visibility = View.GONE
    }

    fun setImageSize(
        imageWidth: Int,
        imageHeight: Int,
        parentViewWidth: Int,
        parentViewHeight: Int,
    ) {
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        if (parentViewWidth > 0) {
            this.parentViewWidth = parentViewWidth.toFloat()
        } else {
            this.parentViewWidth = (parent as ViewGroup).measuredWidth.toFloat()
        }
        if (parentViewHeight > 0) {
            this.parentViewHeight = parentViewHeight.toFloat()
        } else {
            this.parentViewHeight = (parent as ViewGroup).measuredHeight.toFloat()
        }
        if (parentViewWidth > 0) {
            xscale = parentViewWidth.toFloat() / imageWidth.toFloat()
        }
        if (parentViewHeight > 0) {
            yscale = parentViewHeight.toFloat() / imageHeight.toFloat()
        }
        showBitmapHeight = pxBitmapHeight * yscale
        showBitmapWidth = pxBitmapHeight * originalBitmapWidth / originalBitmapHeight * xscale
        visibility = View.VISIBLE
        val layoutParams = this.layoutParams
        layoutParams.width = showBitmapWidth.toInt()
        layoutParams.height = showBitmapHeight.toInt()
        this.layoutParams = layoutParams
        if (l == 0 && t == 0 && r == 0 && b == 0) {
            l = (parentViewWidth / 2 - showBitmapWidth / 2).toInt()
            r = (parentViewWidth / 2 + showBitmapWidth / 2).toInt()
            t = (parentViewHeight / 2 - showBitmapHeight / 2).toInt()
            b = (parentViewHeight / 2 + showBitmapHeight / 2).toInt()
        }
        layout(l, t, r, b)
        requestLayout()
    }

    override fun layout(
        l: Int,
        t: Int,
        r: Int,
        b: Int,
    ) {
        super.layout(l, t, r, b)
    }

    private var downX = 0f
    private var downY = 0f
    private val downTime: Long = 0
    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        if (this.isEnabled) {
            when (event.getAction()) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.getX()
                    downY = event.getY()
                }

                MotionEvent.ACTION_MOVE -> {
                    val xDistance: Float = event.getX() - downX
                    val yDistance: Float = event.getY() - downY
                    if (xDistance != 0f && yDistance != 0f) {
                        l = (left + xDistance).toInt()
                        r = (right + xDistance).toInt()
                        t = (top + yDistance).toInt()
                        b = (bottom + yDistance).toInt()
                        layout(l, t, r, b)
                    }
                }

                MotionEvent.ACTION_UP -> isPressed = false
                MotionEvent.ACTION_CANCEL -> isPressed = false
                else -> {}
            }
            return true
        }
        return false
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ir\view\ZoomCaliperView.kt =====

package com.mpdc4gsr.libunified.ir.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import android.util.AttributeSet
import android.util.Size
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Magnifier
import androidx.core.content.ContextCompat
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.compat.dpToPx
import com.mpdc4gsr.libunified.app.bean.ObserveBean
import com.mpdc4gsr.libunified.app.utils.TargetUtils

class ZoomCaliperView : LinearLayout, ScaleGestureDetector.OnScaleGestureListener {
    private var centerX: Float = Float.MAX_VALUE
    private var centerY: Float = Float.MAX_VALUE
    private var cameraCharacteristics: CameraCharacteristics? = null
    private var isReverse: Boolean = false
    private lateinit var mTextureView: View
    private var canScale = false
    private var def_caliper = 180f
    var magnifier: Magnifier? = null
    var textureMagnifier: Magnifier? = null
    var m: Float = 0.0f
    var zoomViewCloseListener: (() -> Unit)? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    )

    private fun initView() {
        inflate(context, R.layout.zoom_bb, this)
        mTextureView = findViewById(R.id.camera_texture)
        lis = ScaleGestureDetector(context, this)
        originalBitmap = (androidx.core.content.ContextCompat.getDrawable(
            context,
            R.drawable.svg_ic_target_horizontal_person_green
        ) as? BitmapDrawable)?.bitmap
            ?: return
        originalBitmapWidth = originalBitmap.width.toFloat()
        originalBitmapHeight = originalBitmap.height.toFloat()
        onResumeView()
    }

    fun setImageSize(
        imageHeight: Int,
        imageWidth: Int,
        parentViewWidth: Int,
        parentViewHeight: Int,
    ) {
        if (this.imageHeight == imageHeight && this.imageWidth == imageWidth) {
            return
        }
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        if (parentViewWidth > 0) {
            this.parentViewWidth = parentViewWidth.toFloat()
        } else {
            this.parentViewWidth = (parent as ViewGroup).measuredWidth.toFloat()
        }
        if (parentViewHeight > 0) {
            this.parentViewHeight = parentViewHeight.toFloat()
        } else {
            this.parentViewHeight = (parent as ViewGroup).measuredHeight.toFloat()
        }
        if (parentViewWidth > 0) {
            xscale = parentViewWidth.toFloat() / imageWidth.toFloat()
        }
        if (parentViewHeight > 0) {
            yscale = parentViewHeight.toFloat() / imageHeight.toFloat()
        }
        showBitmapHeight = pxBitmapHeight * yscale
        showBitmapHeightWidth = pxBitmapHeight * originalBitmapWidth / originalBitmapHeight * xscale
        val layoutParams = mTextureView.layoutParams
        layoutParams.width = showBitmapHeightWidth.toInt()
        layoutParams.height = showBitmapHeight.toInt()
        mTextureView.layoutParams = layoutParams
        (mTextureView as ImageView).setImageBitmap(originalBitmap)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }

    private var startX = 0f
    private var startY = 0f
    private var moveX = 0f
    private var moveY = 0f
    private var parentViewW = 0f
    private var parentViewH = 0f
    private var isScale = false
    private var scale = 1f
    private var scaleW = 0f
    private var scaleH = 0f
    private lateinit var originalBitmap: Bitmap
    private var imageWidth = 0
    private var imageHeight = 0
    private var parentViewWidth = 0f
    private var parentViewHeight = 0f
    private var xscale = 0f
    private var yscale = 0f
    private var originalBitmapWidth = 0f
    private var originalBitmapHeight = 0f
    private var pxBitmapHeight = 200f
    private var showBitmapHeightWidth = 0f
    private var showBitmapHeight = 0f
    private lateinit var lis: ScaleGestureDetector
    var isCheckChildView = false
    var contentWith = 0
    var contentHeight = 0
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (canScale && isScale && event.action != MotionEvent.ACTION_UP) {
            return lis.onTouchEvent(event)
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                scaleW = mTextureView.width * (scale - 1) / 2f
                scaleH = mTextureView.height * (scale - 1) / 2f
                startX = event.x - mTextureView.x
                startY = event.y - mTextureView.y
                val view: View = mTextureView.parent as View
                parentViewW = view.measuredWidth.toFloat()
                parentViewH = view.measuredHeight.toFloat()
                isCheckChildView =
                    isTouchPointInView(mTextureView, event.rawX.toInt(), event.rawY.toInt())
            }

            MotionEvent.ACTION_MOVE -> {
                if (isCheckChildView) {
                    moveX = event.x - startX
                    moveY = event.y - startY
                    if (m < 100f && m >= 50f) {
                        contentWith = (mTextureView.measuredWidth / 2).toInt()
                        contentHeight = (mTextureView.measuredHeight / 2).toInt()
                        if (moveX < (-contentWith / 2)) moveX = (-contentWith / 2).toFloat()
                        if (moveY < (-contentHeight / 2)) moveY = (-contentHeight / 2).toFloat()
                        if (moveX > parentViewW - contentWith * 4 / 3) {
                            moveX = parentViewW - contentWith * 4 / 3
                        }
                        if (parentViewH > parentViewW) {
                            if (moveY > parentViewH - contentHeight * 4 / 3) {
                                moveY = parentViewH - contentHeight * 4 / 3
                            }
                        } else {
                            if (moveY > parentViewH - contentHeight * 4 / 3) {
                                moveY = parentViewH - contentHeight * 4 / 3
                            }
                        }
                    } else if (m <= 20f) {
                        contentWith = (mTextureView.measuredWidth / 2f).toInt()
                        contentHeight = (mTextureView.measuredHeight / 2f).toInt()
                        if (moveX < (-contentWith / 2)) moveX = (-contentWith / 2).toFloat()
                        if (moveY < (-contentHeight / 2)) moveY = (-contentHeight / 2).toFloat()
                        if (moveX > parentViewW - contentWith) {
                            moveX = parentViewW - contentWith
                        }
                        if (parentViewH > parentViewW) {
                            if (moveY > parentViewH - contentHeight) {
                                moveY = parentViewH - contentHeight
                            }
                        } else {
                            if (moveY > parentViewH - contentHeight) {
                                moveY = parentViewH - contentHeight
                            }
                        }
                    } else {
                        contentWith = mTextureView.width
                        contentHeight = mTextureView.height
                        if (moveX < (-contentWith / 2)) moveX = (-contentWith / 2).toFloat()
                        if (moveY < (-contentHeight / 2)) moveY = (-contentHeight / 2).toFloat()
                        if (moveX > parentViewW - mTextureView.width / 2) {
                            moveX = parentViewW - mTextureView.width / 2
                        }
                        if (moveY > parentViewH - mTextureView.height / 2) {
                            moveY = parentViewH - mTextureView.height / 2
                        }
                    }
                    mTextureView.x = moveX
                    mTextureView.y = moveY
                    centerX = mTextureView.x + mTextureView.measuredWidth / 2
                    centerY = mTextureView.y + mTextureView.measuredHeight / 2
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && m < 100f) {
                        magnifier?.show(centerX, centerY)
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                isCheckChildView = false
                isScale = false
                val startX = viewX
                val startY = viewY
                if ((viewX < 0 && startX < -mTextureView.width * scale + 10f.dpToPx(context)) ||
                    (startX > 0 && startX > parentViewW - 10f.dpToPx(context)) ||
                    (startY < 0 && startY < -mTextureView.height * scale + 10f.dpToPx(context)) ||
                    (startY > 0 && startY > parentViewH - 10f.dpToPx(context))
                ) {
                    zoomViewCloseListener?.invoke()
                }
            }
        }
        var canTouch = isCheckChildView
        if (canScale) {
            canTouch = lis.onTouchEvent(event)
        }
        return canTouch
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }

    private fun isTouchPointInView(
        targetView: View?,
        xAxis: Int,
        yAxis: Int,
    ): Boolean {
        if (targetView == null) {
            return false
        }
        val location = IntArray(2)
        targetView.getLocationOnScreen(location)
        val left = location[0]
        val top = location[1]
        val right = left + targetView.measuredWidth
        val bottom = top + targetView.measuredHeight
        return (yAxis >= top) && (yAxis <= bottom) && (xAxis >= left) && (xAxis <= right)
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        isScale = true
        detector?.let {
            val scaleFactor = it.scaleFactor - 1
            scale += scaleFactor
            mTextureView.scaleX = scale
            mTextureView.scaleY = scale
        }
        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        isScale = true
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
    }

    private var mPreviewSize: Size? = null
    fun setRotation(isReverse: Boolean) {
        this.isReverse = isReverse
        updateRotation()
    }

    private fun updateRotation() {
        if (isReverse) {
            mTextureView.rotation = 180f
        } else {
            mTextureView.rotation = 0f
        }
    }

    private fun onResumeView() {
    }

    val viewX: Float
        get() = mTextureView.x - (viewWidth - mTextureView.width) / 2
    val viewY: Float
        get() = mTextureView.y - (viewHeight - mTextureView.height) / 2
    val viewAlpha: Float
        get() = mTextureView.alpha
    val viewWidth: Float
        get() = mTextureView.width * scale
    val viewHeight: Float
        get() = mTextureView.height * scale
    val viewScale: Float
        get() = scale

    fun setCameraAlpha(alpha: Float) {
        mTextureView?.alpha = 1 - alpha
    }

    fun setCaliperM(m: Float) {
        scale = m / def_caliper
        mTextureView.scaleX = scale
        mTextureView.scaleY = scale
        invalidate()
    }

    private var curChooseMeasureMode: Int = ObserveBean.TYPE_MEASURE_PERSON
    private var curChooseTargetMode: Int = ObserveBean.TYPE_TARGET_HORIZONTAL
    fun updateSelectBitmap(
        targetMeasureMode: Int,
        targetType: Int,
        targetColorType: Int,
        parentCameraView: View?,
    ) {
        if (curChooseTargetMode == targetType && curChooseMeasureMode == targetMeasureMode) {
            return
        }
        curChooseMeasureMode = targetMeasureMode
        curChooseTargetMode = targetType
        updateTargetBitmap(targetMeasureMode, targetType, targetColorType, parentCameraView)
    }

    fun updateTargetBitmap(
        targetMeasureMode: Int,
        targetType: Int,
        targetColorType: Int,
        parentCameraView: View?,
    ) {
        this.visibility = View.VISIBLE
        m = TargetUtils.getMeasureSize(targetMeasureMode)
        val targetIcon =
            TargetUtils.getSelectTargetDraw(targetMeasureMode, targetType, targetColorType)
        originalBitmap = (androidx.core.content.ContextCompat.getDrawable(
            context,
            targetIcon
        ) as? BitmapDrawable)?.bitmap ?: return
        (mTextureView as ImageView).setImageBitmap(originalBitmap)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            magnifier?.dismiss()
            if (m >= 100f) {
                setCaliperM(def_caliper)
                mTextureView.visibility = View.VISIBLE
                textureMagnifier?.dismiss()
                magnifier?.dismiss()
                invalidate()
                return
            }
            if (parentCameraView != null) {
                val builder = Magnifier.Builder(parentCameraView)
                if (m < 50f) {
                    setCaliperM(def_caliper / 2)
                    mTextureView.visibility = View.INVISIBLE
                    builder.setInitialZoom(4f)
                    builder.setCornerRadius(282f.dpToPx(context))
                    builder.setClippingEnabled(false)
                    builder.setOverlay(ContextCompat.getDrawable(context, targetIcon))
                    builder.setSize(
                        282f.dpToPx(context).toInt(),
                        282f.dpToPx(context).toInt(),
                    )
                    magnifier = builder.build()
                } else if (m >= 50f && m < 100f) {
                    setCaliperM(def_caliper / 2)
                    mTextureView.visibility = View.VISIBLE
                    builder.setInitialZoom(2f)
                    builder.setCornerRadius(282f.dpToPx(context))
                    builder.setClippingEnabled(false)
                    builder.setSize(
                        282f.dpToPx(context).toInt(),
                        282f.dpToPx(context).toInt(),
                    )
                    magnifier = builder.build()
                }
            }
            requestLayout()
            mTextureView.postDelayed(
                Runnable {
                    centerX = parentCameraView!!.measuredWidth.toFloat() / 2
                    centerY = parentCameraView!!.measuredHeight.toFloat() / 2
                    mTextureView.x = centerX - mTextureView.measuredWidth / 2
                    mTextureView.y = centerY - mTextureView.measuredHeight / 2
                    magnifier?.show(centerX, centerY)
                },
                200,
            )
        }
    }

    fun hideView() {
        this.visibility = GONE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            magnifier?.dismiss()
        }
    }

    fun showView() {
        this.visibility = VISIBLE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            magnifier?.show(centerX, centerY)
        }
    }

    fun updateMagnifier() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            magnifier?.update()
        }
    }

    fun del(reductionXY: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            magnifier?.dismiss()
        }
        curChooseMeasureMode = ObserveBean.TYPE_MEASURE_PERSON
        curChooseTargetMode = ObserveBean.TYPE_TARGET_HORIZONTAL
        if (this.visibility == View.VISIBLE) {
            this.visibility = GONE
            if (reductionXY) {
                centerX = Float.MAX_VALUE
                centerY = Float.MAX_VALUE
            } else {
                val parent = parent as ViewGroup
                centerX = parent.measuredWidth.toFloat() / 2
                centerY = parent.measuredHeight.toFloat() / 2
                mTextureView.x = centerX - mTextureView.width / 2
                mTextureView.y = centerY - mTextureView.height / 2
            }
        }
    }

    fun updateCenter() {
        val parent = parent as ViewGroup
        centerX = parent.measuredWidth.toFloat() / 2
        centerY = parent.measuredHeight.toFloat() / 2
        mTextureView.x = centerX - mTextureView.width / 2
        mTextureView.y = centerY - mTextureView.height / 2
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            magnifier?.show(centerX, centerY)
        }
    }
}