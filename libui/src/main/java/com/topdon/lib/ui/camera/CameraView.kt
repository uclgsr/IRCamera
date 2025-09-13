package com.topdon.lib.ui.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.Image
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
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.NonNull
import com.blankj.utilcode.util.ThreadUtils.runOnUiThread
import com.blankj.utilcode.util.ToastUtils
import com.topdon.lib.core.utils.ScreenUtil
import com.topdon.lib.ui.databinding.CameraLayBinding
import java.nio.ByteBuffer
import java.util.Collections
import kotlin.concurrent.thread

/**
 * Custom Camera view for thermal imaging display.
 * Provides specialized rendering and interaction capabilities.
 */
/**
 * CameraView implements custom user interface component functionality.
 *
 * @author IRCamera Development Team
 * @since 1.0
 */
class CameraView : LinearLayout, ScaleGestureDetector.OnScaleGestureListener {
    /**预览 */
    lateinit var mTextureView: TextureView
    private lateinit var binding: CameraLayBinding

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
        mTextureView = binding.cameraTexture
        mTextureView.alpha = 0.4f
        lis = ScaleGestureDetector(context, this)

        onResumeView()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mCameraDevice?.close()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isScale && event.action != MotionEvent.ACTION_UP) {
            return lis.onTouchEvent(event)
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                scaleW = mTextureView.width * (scale - 1) / 2f
                scaleH = mTextureView.height * (scale - 1) / 2f
                startX = event.x - mTextureView.x
                startY = event.y - mTextureView.y
                val view: View = mTextureView.parent as View
                parentViewW = view.width.toFloat()
                parentViewH = view.height.toFloat()
            }
            MotionEvent.ACTION_MOVE -> {
                
                moveX = event.x - startX
                moveY = event.y - startY
                if (moveX - scaleW < 0f) moveX = 0f + scaleW
                if (moveY - scaleH < 0f) moveY = 0f + scaleH
                if (moveX + scaleW > parentViewW - mTextureView.width) {
                    moveX = parentViewW - mTextureView.width - scaleW
                }
                if (moveY + scaleH > parentViewH - mTextureView.height) {
                    moveY = parentViewH - mTextureView.height - scaleH
                }
                mTextureView.x = moveX
                mTextureView.y = moveY
            }
            MotionEvent.ACTION_UP -> {
                isScale = false 
            }
        }
        return lis.onTouchEvent(event)
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

// ////////////////
    /**cameraPermission请求标识 */
    private val REQUEST_CAMERA_CODE = 0x100

    /**capturebutton */
    private var mBtnTake: Button? = null

    /**image */
    private var mImageView: ImageView? = null

    /**照cameraID，标识前置后置 */
    private lateinit var mCameraId: String

    /**camera尺寸 */
    private var mCaptureSize: Size? = null

    /**image读取者 */
    private lateinit var mImageReader: ImageReader

    /**image主line程Handler */
    private lateinit var mCameraHandler: Handler

    /**cameradevice */
    private var mCameraDevice: CameraDevice? = null

    /**预览大小 */
    private var mPreviewSize: Size? = null

    /**camera请求 */
    private lateinit var mCameraCaptureBuilder: CaptureRequest.Builder

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
                
                mCameraDevice = camera
                
                takePreview()
            }

            override fun onDisconnected(
                @NonNull camera: CameraDevice,
            ) {
                
                camera.close()
                mCameraDevice = null
            }

            override fun onError(
                @NonNull camera: CameraDevice,
                error: Int,
            ) {
                
                camera.close()
                mCameraDevice = null
            }
        }

    /**
     * 预览
     */
    private fun takePreview() {
//        mTextureView.rotation = 270f
        mTextureView.rotation = 0f
        // Get/RetrieveSurfaceTexture
        val surfaceTexture = mTextureView.surfaceTexture
        
        surfaceTexture!!.setDefaultBufferSize(mPreviewSize!!.width, mPreviewSize!!.height)
        
        val previewSurface = Surface(surfaceTexture)
        try {
            
            mCameraCaptureBuilder =
                mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            
            mCameraCaptureBuilder.addTarget(previewSurface)
            
            @Suppress("DEPRECATION")
            mCameraDevice!!.createCaptureSession(
                listOf(
                    previewSurface,
                    mImageReader.surface,
                ),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(
                        @NonNull session: CameraCaptureSession,
                    ) {
                        try {
                            
                            val captureRequest = mCameraCaptureBuilder.build()
                            
                            mCameraCaptureSession = session
                            
                            mCameraCaptureSession!!.setRepeatingRequest(
                                captureRequest,
                                null,
                                mCameraHandler,
                            )
                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onConfigureFailed(
                        @NonNull session: CameraCaptureSession,
                    ) {
                        
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
        mTextureView.surfaceTextureListener =
            object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int,
                ) {
                    
                    
                    Log.w("123", "width:$width, height:$height")
                    // w:h = 1 / 1.33
                    setUpCamera(width, height)
//                openCamera()
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

    /**
     * Opencamera
     */
    @SuppressLint("MissingPermission")
    /**
     * Executes opencamera functionality.
     */
    fun openCamera() {
        // Get/Retrieve照camera管理者
        try {
            mCameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager?
            mCameraManager!!.openCamera(mCameraId, mStateCallback, mCameraHandler)
        } catch (e: Exception) {
            Log.e("123", "Opencamerafailed:${e.message}")
            ToastUtils.showShort("Opencamerafailed")
        }
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
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            
            for (cameraId in cameraManager.cameraIdList) {
                
                val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
                // Get/Retrieve摄像头是前置还是后置
                val facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
                
                if (null != facing && CameraCharacteristics.LENS_FACING_FRONT == facing) continue
                // Get/RetrieveStreamConfigurationMap，管理摄像头支持的所有输出format和尺寸
                val map =
                    cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                
                mPreviewSize =
                    getOptimalSize(
                        map.getOutputSizes(SurfaceTexture::class.java),
                        width,
                        height,
                    )
                // Get/Retrievecamera支持的最大capture尺寸
                val sizes = map.getOutputSizes(ImageFormat.JPEG)
                val w = 1000
                val h = w * sizes[0].height / sizes[0].width
                mCaptureSize = Size(w, h)
                Log.w("123", "w:${sizes[0].width}, h:${sizes[0].height}")
                Log.w("123", "Adjust后w:$w, h:$h")
//                mCaptureSize = Size(1000, 1000)
//                mCaptureSize =
//                    Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG))) { lhs, rhs ->
//                        java.lang.Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getHeight() * rhs.getWidth())
//                    }
                
                setupImageReader()
                
                mCameraId = cameraId
                break
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
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
            Collections.min(
                sizeList,
            ) { lhs, rhs -> java.lang.Long.signum((lhs.width * lhs.height - rhs.width * rhs.height).toLong()) }
        } else {
            sizeMap[0]
        }
    }

    private var flag = 0

    /**
     * settingsImageReader
     */
    private fun setupImageReader() {
        // 2代表ImageReader中最多可以Get/Retrieve两帧image流
        mImageReader =
            ImageReader.newInstance(
                mCaptureSize!!.width,
                mCaptureSize!!.height,
                ImageFormat.JPEG,
                1,
            )
        
        mImageReader.setOnImageAvailableListener({ reader ->
            flag = 1
            // Get/Retrieveimage
            val image: Image = reader.acquireLatestImage()
            // 提交task，savedimage
            mCameraHandler.post(ImageSaver(image))
            
            runOnUiThread { // Get/Retrieve字节buffer区
                val buffer: ByteBuffer = image.planes[0].buffer
                // createarray之前调用此method，restore默认settings
                buffer.rewind()
                
                val bytes = ByteArray(buffer.remaining())
                // 从buffer区存入字节array,读取complete之后position在末尾
                buffer[bytes]
                // Get/RetrieveBitmapimage
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                // Show/Display
                if (null != bitmap) {
                    val h = bitmap.height
                    val w = bitmap.width
                    mImageView?.let {
                        val sw = ScreenUtil.getScreenWidth(context)
                        it.layoutParams.height = sw / 2 * w / h
                        it.layoutParams.width = sw / 2
                        it.setImageBitmap(bitmap)
                    }
                }
                flag++
                thread {
                    while (flag < 3) {
//                        delay(100)
                        Thread.sleep(100)
                    }
                    flag = 0
                    image.close()
                }
            }
        }, mCameraHandler)
    }

    /**
     * savedimagetask
     */
    private inner class ImageSaver(image: Image) : Runnable {
        /**image */
        private val mImage: Image = image

        override fun run() {
//            ImageSaverTool().save(mImage)
            flag++
        }
    }
}
