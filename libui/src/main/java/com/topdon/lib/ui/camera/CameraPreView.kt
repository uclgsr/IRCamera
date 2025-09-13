package com.topdon.lib.ui.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.NonNull
import androidx.constraintlayout.widget.ConstraintSet
import com.blankj.utilcode.util.SizeUtils
import com.blankj.utilcode.util.ToastUtils
import com.elvishew.xlog.XLog
import com.topdon.lib.core.listener.BitmapViewListener
import com.topdon.lib.ui.databinding.CameraLayBinding
import java.util.Collections

/**
 * camera预览
 */
/**
 * Custom Camera pre view for thermal imaging display.
 * Provides specialized rendering and interaction capabilities.
 */
/**
 * CameraPreView implements custom user interface component functionality.
 *
 * @author IRCamera Development Team
 * @since 1.0
 */
class CameraPreView :
    LinearLayout,
    ScaleGestureDetector.OnScaleGestureListener,
    BitmapViewListener {
    private lateinit var binding: CameraLayBinding
    private var cameraCharacteristics: CameraCharacteristics? = null
    private var isReverse: Boolean = false
    private var cameraWidth = 0

    var isPreviewing = false

    var cameraPreViewCloseListener: (() -> Unit)? = null

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    )

    /**
     * Initializes the component with default configuration.
     */
    private fun initView() {
        binding = CameraLayBinding.inflate(LayoutInflater.from(context), this, true)
        binding.cameraTexture.post { cameraWidth = binding.cameraTexture.width }
        lis = ScaleGestureDetector(context, this)
        onResumeView()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isPreviewing = false
        mCameraDevice?.close()
        mCameraHandler?.removeCallbacksAndMessages(null)
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

    private lateinit var lis: ScaleGestureDetector

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isScale && event.action != MotionEvent.ACTION_UP) {
            return lis.onTouchEvent(event)
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                scaleW = binding.cameraTexture.width * (scale - 1) / 2f
                scaleH = binding.cameraTexture.height * (scale - 1) / 2f
                startX = event.x - binding.cameraTexture.x
                startY = event.y - binding.cameraTexture.y
                val view: View = binding.cameraTexture.parent as View
                parentViewW = view.width.toFloat()
                parentViewH = view.height.toFloat()
            }
            MotionEvent.ACTION_MOVE -> {
                
                moveX = event.x - startX
                moveY = event.y - startY
                // 根据移动情况，不Visible时候close
//                if (moveX-scaleW < -mTextureView.width ||
//                    moveX+scaleW > parentViewW ||
//                    moveY - scaleH < -mTextureView.height ||
//                    moveY + scaleH > parentViewH){
//                    cameraPreViewCloseListener?.invoke()
//                }

                
//                if (moveX - scaleW < 0f) moveX = 0f + scaleW
//                if (moveY - scaleH < 0f) moveY = 0f + scaleH
//                if (moveX + scaleW > parentViewW - mTextureView.width) {
//                    moveX = parentViewW - mTextureView.width - scaleW
//                }
//                if (moveY + scaleH > parentViewH - mTextureView.height) {
//                    moveY = parentViewH - mTextureView.height - scaleH
//                }
//                Log.e("Test---","/"+(moveX + scaleW)+"///"+(parentViewW - mTextureView.width))
                binding.cameraTexture.x = moveX
                binding.cameraTexture.y = moveY
            }
            MotionEvent.ACTION_UP -> {
                isScale = false 
                val startX = viewX
                val startY = viewY
//                Log.e("Test","/"+(startX)+"///"+startY+"///"+(mTextureView.width)+"//"+mTextureView.width * scale)
                if ((viewX < 0 && startX < -binding.cameraTexture.width * scale + SizeUtils.dp2px(10f)) ||
                    (startX > 0 && startX > parentViewW - SizeUtils.dp2px(10f)) ||
                    (startY < 0 && startY < -binding.cameraTexture.height * scale + SizeUtils.dp2px(10f)) ||
                    (startY > 0 && startY > parentViewH - SizeUtils.dp2px(10f))
                ) {
                    cameraPreViewCloseListener?.invoke()
                }
            }
        }
        return lis.onTouchEvent(event)
    }

    /**
     * savedimage
     */
    public fun getBitmap(): Bitmap? {
        return binding.cameraTexture.bitmap
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        
        isScale = true
        detector?.let {
            val scaleFactor = it.scaleFactor - 1
            if (scaleFactor < 0) {
                if (scale > 0.1) {
                    scale += scaleFactor
                    binding.cameraTexture.scaleX = scale
                    binding.cameraTexture.scaleY = scale
                }
            } else {
                scale += scaleFactor
                binding.cameraTexture.scaleX = scale
                binding.cameraTexture.scaleY = scale
            }
        }
        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        isScale = true
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
    }

    /**
     * Callback method triggered when resume occurs.
     */
    fun onResume() {
        // processingswitch后台，Open系统camera后，回到app导致预览不update画area的问题
        if (mCameraDevice != null) {
            mCameraDevice?.close()
            openCamera()
        }
    }

// ////////////////

    /**cameraPermission请求标识 */
    private val REQUEST_CAMERA_CODE = 0x100

    /**image */
    private var mImageView: ImageView? = null

    /**照cameraID，标识前置后置 */
    private lateinit var mCameraId: String

    /**camera尺寸 */
    private var mCaptureSize: Size? = null

    /**image读取者 */
    private var mImageReader: ImageReader? = null

    /**image主line程Handler */
    private var mCameraHandler: Handler? = null

    /**cameradevice */
    private var mCameraDevice: CameraDevice? = null

    /**预览大小 */
    private var mPreviewSize: Size? = null

    /**camera请求 */
    private lateinit var mCaptureBuilder: CaptureRequest.Builder

    /**cameracapture捕获会话 */
    private var mCameraCaptureSession: CameraCaptureSession? = null

    /**camera管理者 */
    private var mCameraManager: CameraManager? = null

    /**cameradevicestateCallback */
    private val mStateCallback: CameraDevice.StateCallback =
        object : CameraDevice.StateCallback() {
            override fun onOpened(
                @NonNull camera: CameraDevice,
            ) {
                
                XLog.i("开启预览")
                mCameraDevice = camera
                takePreview()
            }

            override fun onDisconnected(
                @NonNull camera: CameraDevice,
            ) {
                
                XLog.i("close预览")
                isPreviewing = false
//                camera.close()
//                mCameraDevice = null
            }

            override fun onError(
                @NonNull camera: CameraDevice,
                error: Int,
            ) {
                
                isPreviewing = false
                camera.close()
                mCameraDevice = null
                XLog.e("预览exception error: $error")
            }
        }

    fun setRotation(isReverse: Boolean) {
        this.isReverse = isReverse
        updateRotation()
    }

    /**
     * Updates the rotation with new data.
     */
    private fun updateRotation() {
        if (isReverse) {
            binding.cameraTexture.rotation = 180f
        } else {
            binding.cameraTexture.rotation = 0f
        }
    }

    /**
     * 预览
     * click开启camera后触发
     */
    private fun takePreview() {
//        mTextureView.rotation = 270f
//        mTextureView.rotation = 0f
        updateRotation()
//        val layoutParams = mTextureView.layoutParams
//        layoutParams.width = cameraWidth / 2
//        mTextureView.layoutParams = layoutParams
        val surfaceTexture = binding.cameraTexture.surfaceTexture
        
        surfaceTexture?.setDefaultBufferSize(mPreviewSize!!.width, mPreviewSize!!.height)
        
        val previewSurface = Surface(surfaceTexture)
        try {
            
            mCaptureBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            
            mCaptureBuilder.addTarget(previewSurface)
            
            @Suppress("DEPRECATION")
            mCameraDevice!!.createCaptureSession(
                listOf(previewSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(
                        @NonNull session: CameraCaptureSession,
                    ) {
                        try {
                            
                            val captureRequest = mCaptureBuilder.build()
                            
                            mCameraCaptureSession = session
                            
                            mCameraCaptureSession?.setRepeatingRequest(
                                captureRequest,
                                null,
                                mCameraHandler,
                            )
                        } catch (e: CameraAccessException) {
                            XLog.e("cameraexception：${e.printStackTrace()}")
                        }
                    }

                    override fun onConfigureFailed(
                        @NonNull session: CameraCaptureSession,
                    ) {
                        
                        XLog.e("configurationfailed")
                    }
                },
                mCameraHandler,
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    /**
     * Callback method triggered when resumeview occurs.
     */
    private fun onResumeView() {
        binding.cameraTexture.surfaceTextureListener =
            object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int,
                ) {
                    
                    XLog.w("width:$width, height:$height")
                    setUpCamera(width, height)
                }

                override fun onSurfaceTextureSizeChanged(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int,
                ) {
                    
                }

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                    // SurfaceTexture destroy
                    return false
                }

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                    // SurfaceTexture update
                }
            }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }

    /**
     * settingscameraparameter
     * @param width 宽度
     * @param height 高度
     */
    private fun setUpCamera(
        width: Int,
        height: Int,
    ) {
        
        mCameraHandler = Handler(Looper.getMainLooper())
        // Get/Retrieve摄像头的管理者
        mCameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            // 遍历所有摄像头,找到一个Cancel遍历
            for (cameraId in mCameraManager!!.cameraIdList) {
                XLog.i("camera id: $cameraId")
                cameraCharacteristics = mCameraManager!!.getCameraCharacteristics(cameraId)
                // Get/Retrieve摄像头是前置还是后置
                val facing = cameraCharacteristics?.get(CameraCharacteristics.LENS_FACING)
                
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) continue
                // Get/RetrieveStreamConfigurationMap，管理摄像头支持的所有输出format和尺寸
                val map = cameraCharacteristics?.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                
                val mapList = map.getOutputSizes(SurfaceTexture::class.java)

                mPreviewSize = getOptimalSize(mapList, width, height)
                val constraintSet = ConstraintSet()
                constraintSet.clone(binding.cameraLayRoot)
                constraintSet.constrainHeight(binding.cameraTexture.id, width * mPreviewSize!!.width / mPreviewSize!!.height)
                constraintSet.applyTo(binding.cameraLayRoot)
                XLog.w("mPreviewSize:$mPreviewSize")
                // Get/Retrievecamera支持的最大capture尺寸
                val sizes = map.getOutputSizes(ImageFormat.JPEG)
                XLog.w("size:${sizes.toList()}")
                val w = 1000
                val h = w * sizes[0].height / sizes[0].width
//                mCaptureSize = Size(w, h)
                XLog.w("选取比例 w:${sizes[0].width}, h:${sizes[0].height}")
                XLog.w("Adjust后 w: $w, h:$h")
                
//                setupImageReader()
                
                mCameraId = cameraId
                break
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
            Log.e("123", "settingscameraparameter:${e.message}")
        }
    }

    /**
     * selectionSizeMap中大于并且最接近width和height的size
     * @param sizeMap 可选的尺寸
     * @param width 宽
     * @param height 高
     * @return 最接近width和height的size
     */
    private fun getOptimalSize(
        sizeMap: Array<Size>,
        width: Int,
        height: Int,
    ): Size {
        
        val sizeList: MutableList<Size> = ArrayList()
        
        for (option in sizeMap) {
            
            if (width > height) {
                if (option.width > width && option.height > height) {
                    sizeList.add(option)
                }
            } else {
                if (option.width > height && option.height > width) {
                    sizeList.add(option)
                }
            }
        }
        
        return if (sizeList.size > 0) {
            Collections.min(sizeList) { lhs, rhs ->
                java.lang.Long.signum((lhs.width * lhs.height - rhs.width * rhs.height).toLong())
            }
        } else {
            sizeMap[0]
        }
    }

    /**
     * Opencamera
     */
    @SuppressLint("MissingPermission")
    /**
     * Executes opencamera functionality.
     */
    fun openCamera() {
        isPreviewing = true
        try {
            mCameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            mCameraManager!!.openCamera(mCameraId, mStateCallback, mCameraHandler)
        } catch (e: Exception) {
            isPreviewing = false
            XLog.e("Opencamerafailed:${e.message}")
            ToastUtils.showShort("Opencamerafailed")
        }
    }

    /**
     * closecamera
     */
    @SuppressLint("MissingPermission")
    /**
     * Executes closecamera functionality.
     */
    fun closeCamera() {
        isPreviewing = false
        try {
            mCameraDevice?.close()
            
            binding.cameraTexture.x = 0f
            binding.cameraTexture.y = 0f
            binding.cameraTexture.scaleX = 1f
            binding.cameraTexture.scaleY = 1f
            scale = 1f
//            isReverse = false
        } catch (e: Exception) {
            XLog.e("closecamerafailed:${e.message}")
            ToastUtils.showShort("closecamerafailed")
        }
    }

    override val viewX: Float
        get() = binding.cameraTexture.x - (viewWidth - binding.cameraTexture.width) / 2
    override val viewY: Float
        get() = binding.cameraTexture.y - (viewHeight - binding.cameraTexture.height) / 2
    override val viewAlpha: Float
        get() = binding.cameraTexture.alpha
    override val viewWidth: Float
        get() = binding.cameraTexture.width * scale
    override val viewHeight: Float
        get() = binding.cameraTexture.height * scale
    override val viewScale: Float
        get() = scale

    fun setCameraAlpha(alpha: Float) {
        binding.cameraTexture.alpha = 1 - alpha
    }

    fun setZoom(zoomLeve: Int) {
        scale = zoomLeve * 0.5f
        binding.cameraTexture.scaleX = scale
        binding.cameraTexture.scaleY = scale
        invalidate()
    }
}
