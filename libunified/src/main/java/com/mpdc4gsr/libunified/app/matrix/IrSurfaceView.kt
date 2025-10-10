package com.mpdc4gsr.libunified.app.matrix

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView

class IrSurfaceView :
    SurfaceView,
    SurfaceHolder.Callback {
    private var mHolder: SurfaceHolder? = null
    private var mCanvas: Canvas? = null
    private val p: Paint by lazy { Paint() }
    private val mMatrix: Matrix by lazy { Matrix() }
    private var openLut = false
    private val mBeforeRotateMatrixValues = FloatArray(9)
    private val mScaleMatrixValues = FloatArray(9)
    private val mRotateMatrixValues = FloatArray(9)

    @Volatile
    private var isPrepare = false

    @Volatile
    private var isLockImage = false
    private var callback: IfrCamOpenOverCallback? = null
    private var mCtx: Context? = null

    constructor(context: Context) : super(context) {
        mCtx = context
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        mCtx = context
        init()
    }

    private fun init() {
        mHolder = holder
        mHolder?.addCallback(this)
        mHolder?.setFormat(PixelFormat.TRANSPARENT)
        p.alpha = 0xff
        mMatrix.setScale(1.0f, 1.0f)
    }

    fun setIsLockImage(isLock: Boolean) {
        isLockImage = isLock
    }

    //    fun setMatrix(scale: Float, x: Float, y: Float) {
//        mMatrix.reset()
//        mMatrix.setScale(scale, scale)
//        mMatrix.postTranslate(x, y)
//        mMatrix.getValues(mBeforeRotateMatrixValues)
//    }
    fun setMatrix(
        rotate: Float,
        w: Float,
        h: Float,
    ) {
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        mMatrix.reset()
        when (rotate) {
            90f -> {
                val sca = screenWidth / h
                mMatrix.setRotate(rotate, 0f, 0f)
                mMatrix.postTranslate(h, 0f)
                mMatrix.postScale(sca, sca)
            }

            180f -> {
                val sca = screenWidth / w
                mMatrix.setRotate(rotate, 0f, 0f)
                mMatrix.postTranslate(w, h)
                mMatrix.postScale(sca, sca)
            }

            270f -> {
                val sca = screenWidth / h
                mMatrix.setRotate(rotate, 0f, 0f)
                mMatrix.postTranslate(0f, w)
                mMatrix.postScale(sca, sca)
            }

            else -> {
                val sca = screenWidth / w
                mMatrix.postScale(sca, sca)
            }
        }
    }

    fun doDraw(
        bitmap: Bitmap?,
        shutterFlag: Int,
    ) {
        synchronized(this) {
            if (isLockImage || !isPrepare || null == bitmap || shutterFlag == 1) {
                return@doDraw
            }
            mCanvas = mHolder?.lockCanvas()
            try {
                mCanvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
//                mCanvas?.drawBitmap(bitmap, mMatrix, p)
                if (openLut) {
                    mColorMatrixEnhance.setSaturation(saturation * 0.01f * 2.5f + 1f)
                    p.colorFilter = ColorMatrixColorFilter(mColorMatrixEnhance)
                } else {
                    p.colorFilter = ColorMatrixColorFilter(mColorMatrix)
                }
                mCanvas?.drawBitmap(bitmap, mMatrix, p)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                val surface = mHolder!!.surface
                if (mCanvas != null && mHolder != null && surface != null && surface.isValid) {
                    try {
                        mHolder?.unlockCanvasAndPost(mCanvas)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private var mColorMatrix =
        ColorMatrix(
            floatArrayOf(
                1f,
                0f,
                0f,
                0f,
                0f,
                0f,
                1f,
                0f,
                0f,
                0f,
                0f,
                0f,
                1f,
                0f,
                0f,
                0f,
                0f,
                0f,
                01f,
                0f,
            ),
        )
    private var mColorMatrixLut =
        ColorMatrix(
            floatArrayOf(
                1f,
                0f,
                0f,
                0f,
                0f,
                0f,
                1.5f,
                0f,
                0f,
                25f,
                0.1f,
                0.2f,
                0.7f,
                0f,
                25f,
                0f,
                0f,
                0f,
                01f,
                0f,
            ),
        )
    private val n = 1f
    private var mColorMatrixEnhance =
        ColorMatrix(
//        floatArrayOf(
//            n, 0f, 0f, 0f, 128 * (1 - n),
//            0f, n, 0f, 0f, 128 * (1 - n),
//            0f, 0f, n, 0f, 128 * (1 - n),
//            0f, 0f, 0f, 1f, 0f
//        )
            floatArrayOf(
                1f,
                0f,
                0f,
                0f,
                0f,
                0f,
                1f,
                0f,
                0f,
                0f,
                0f,
                0f,
                1f,
                0f,
                0f,
                0f,
                0f,
                0f,
                1f,
                0f,
            ),
        )
    private var saturation = 0

    fun setOpenLut() {
//        openLut = !openLut
        openLut = true
    }

    fun setSaturationValue(saturation: Int) {
        this.saturation = saturation
    }

    fun getSaturationValue(): Int = saturation

    fun setAlpha(alpha: Int) {
        if (alpha in 0..255) {
            p?.alpha = alpha
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isPrepare = true
        if (callback != null) {
            callback!!.onSurfaceCreated()
        }
        Logger.d(TAG, "holder onSurfaceCreated")
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int,
    ) {
        Logger.d(TAG, "holder surfaceChanged")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        synchronized(this) {
            isPrepare = false
            Logger.d(TAG, "holder destroyed")
        }
    }

    companion object {
        private val TAG = "IrSurfaceView"
    }

    interface IfrCamOpenOverCallback {
        fun onSurfaceCreated()
    }
}
