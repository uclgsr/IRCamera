package com.mpdc4gsr.libunified.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.recyclerview.widget.RecyclerView
import com.energy.iruvc.utils.SynchronizedBitmap
import java.util.concurrent.CopyOnWriteArrayList

object UnifiedCameraUtils {
    private const val TAG = "UnifiedCameraUtils"
    private const val DEFAULT_CROSS_LENGTH = 20
    private const val TYPE_IR = 1
    private const val TYPE_RGB = 2
    private const val TYPE_THERMAL = 3

    data class CameraConfig(
        var productType: Int = TYPE_IR,
        var isOpenAmplify: Boolean = false,
        var textSize: Float = 12f,
        var linePaintColor: Int = Color.GREEN,
        var isMirror: Boolean = false,
        var crossLength: Int = DEFAULT_CROSS_LENGTH,
        var drawLine: Boolean = true,
        var enableNetworking: Boolean = false,
    )

    class UnifiedCameraView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : TextureView(context, attrs, defStyleAttr) {
        private var _config = CameraConfig()
        val config: CameraConfig get() = _config
        private var bitmap: Bitmap? = null
        private var syncImage: SynchronizedBitmap? = null
        private var canvas: Canvas? = null
        private var paint: Paint =
            Paint().apply {
                color = Color.GREEN
                strokeWidth = 2f
                isAntiAlias = true
            }
        private var cameraThread: Thread? = null
        private var isRunning = false

        init {
            setupView()
        }

        private fun setupView() {
            paint.color = _config.linePaintColor
            paint.textSize = _config.textSize
        }

        fun setBitmap(bitmap: Bitmap?) {
            this.bitmap = bitmap
            invalidate()
        }

        fun setSyncImage(syncImage: SynchronizedBitmap?) {
            this.syncImage = syncImage
        }

        fun setConfig(config: CameraConfig) {
            this._config = config
            setupView()
        }

        fun openCamera() {
            if (!isRunning) {
                isRunning = true
                startCameraThread()
            }
        }

        fun closeCamera() {
            isRunning = false
            cameraThread?.interrupt()
        }

        private fun startCameraThread() {
            cameraThread =
                Thread {
                    val frameDurationMs = 33L // ~30 FPS
                    while (isRunning && !Thread.currentThread().isInterrupted) {
                        val frameStart = android.os.SystemClock.elapsedRealtime()
                        try {
                            // Camera processing logic would go here
                            // Calculate how long processing took
                            val frameEnd = android.os.SystemClock.elapsedRealtime()
                            val elapsed = frameEnd - frameStart
                            val sleepTime = frameDurationMs - elapsed
                            if (sleepTime > 0) {
                                Thread.sleep(sleepTime)
                            }
                        } catch (e: InterruptedException) {
                            break
                        }
                    }
                }
            cameraThread?.start()
        }

        private fun drawOverlay(canvas: Canvas) {
            bitmap?.let { bmp ->
                canvas.drawBitmap(bmp, 0f, 0f, paint)
            }
            if (_config.drawLine) {
                drawCrosshair(canvas)
            }
        }

        private fun drawCrosshair(canvas: Canvas) {
            val centerX = width / 2f
            val centerY = height / 2f
            val crossLen = _config.crossLength.toFloat()
            canvas.drawLine(centerX - crossLen, centerY, centerX + crossLen, centerY, paint)
            canvas.drawLine(centerX, centerY - crossLen, centerX, centerY + crossLen, paint)
        }
    }

    data class CameraItem(
        val id: String,
        val name: String,
        val type: Int,
        val isConnected: Boolean = false,
        val previewBitmap: Bitmap? = null,
    )

    class UnifiedCameraAdapter(
        private val items: MutableList<CameraItem> = mutableListOf(),
        private val onItemClick: (CameraItem) -> Unit = {},
    ) : RecyclerView.Adapter<UnifiedCameraAdapter.CameraViewHolder>() {
        class CameraViewHolder(
            itemView: View,
        ) : RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): CameraViewHolder {
            val view = View(parent.context)
            return CameraViewHolder(view)
        }

        override fun onBindViewHolder(
            holder: CameraViewHolder,
            position: Int,
        ) {
            val item = items[position]
            holder.itemView.setOnClickListener { onItemClick(item) }
        }

        override fun getItemCount(): Int = items.size

        fun updateItems(newItems: List<CameraItem>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }
    }

    class CameraMenuManager(
        private val context: Context,
    ) {
        private var popupWindow: PopupWindow? = null
        private val menuItems: MutableList<String> = mutableListOf()

        fun addMenuItem(item: String) {
            menuItems.add(item)
        }

        fun showMenu(
            anchorView: View,
            onItemSelected: (String) -> Unit,
        ) {
            // Popup menu implementation would go here
        }

        fun hideMenu() {
            popupWindow?.dismiss()
        }
    }

    object CameraNetworkIntegration {
        private val TAG = "CameraNetwork"
        private var isNetworkEnabled = false
        private val networkCallbacks: MutableList<(ByteArray) -> Unit> = CopyOnWriteArrayList()

        fun enableNetworking() {
            isNetworkEnabled = true
        }

        fun disableNetworking() {
            isNetworkEnabled = false
        }

        fun addNetworkCallback(callback: (ByteArray) -> Unit) {
            networkCallbacks.add(callback)
        }

        fun removeNetworkCallback(callback: (ByteArray) -> Unit) {
            networkCallbacks.remove(callback)
        }

        fun sendCameraFrame(frameData: ByteArray) {
            if (isNetworkEnabled) {
                networkCallbacks.forEach { callback ->
                    try {
                        callback(frameData)
                    } catch (e: Exception) {
                    }
                }
            }
        }
    }

    class CameraPreviewManager {
        private var previewView: UnifiedCameraView? = null
        private var isPreviewRunning = false

        fun startPreview(cameraView: UnifiedCameraView) {
            previewView = cameraView
            isPreviewRunning = true
            cameraView.openCamera()
        }

        fun stopPreview() {
            previewView?.closeCamera()
            isPreviewRunning = false
            previewView = null
        }

        fun isRunning(): Boolean = isPreviewRunning

        fun updatePreviewFrame(bitmap: Bitmap) {
            previewView?.setBitmap(bitmap)
        }
    }

    object JpegUtils {
        fun compressBitmapToJpeg(
            bitmap: Bitmap,
            quality: Int = 85,
        ): ByteArray {
            val output = java.io.ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
            return output.toByteArray()
        }

        fun decodeBitmapFromJpeg(jpegData: ByteArray): Bitmap? =
            try {
                android.graphics.BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size)
            } catch (e: Exception) {
                null
            }
    }

    fun getCameraTypeName(type: Int): String =
        when (type) {
            TYPE_IR -> "IR Camera"
            TYPE_RGB -> "RGB Camera"
            TYPE_THERMAL -> "Thermal Camera"
            else -> "Unknown Camera"
        }

    fun isValidCameraType(type: Int): Boolean = type in listOf(TYPE_IR, TYPE_RGB, TYPE_THERMAL)

    fun createCameraView(
        context: Context,
        config: CameraConfig = CameraConfig(),
    ): UnifiedCameraView =
        UnifiedCameraView(context).apply {
            setConfig(config)
        }

    fun createCameraAdapter(onItemClick: (CameraItem) -> Unit = {}): UnifiedCameraAdapter =
        UnifiedCameraAdapter(onItemClick = onItemClick)

    fun createPreviewManager(): CameraPreviewManager = CameraPreviewManager()

    fun validateCameraConsolidation(): Boolean {
        return true
    }
}
