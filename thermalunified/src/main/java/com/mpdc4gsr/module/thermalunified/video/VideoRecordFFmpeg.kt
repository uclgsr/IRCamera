package com.mpdc4gsr.module.thermalunified.video

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioRecord.RECORDSTATE_RECORDING
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.view.TextureView
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.view.drawToBitmap
import androidx.core.view.isVisible
import com.infisense.usbir.view.CameraView
import com.mpdc4gsr.libunified.app.comm.view.TempLayout
import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.libunified.app.compose.dialogs.TipDialogState
import com.mpdc4gsr.libunified.app.config.FileConfig
import com.mpdc4gsr.libunified.app.tools.TimeTools
import com.mpdc4gsr.libunified.app.utils.BitmapUtils
import com.mpdc4gsr.libunified.app.utils.LibraryLogger
import com.mpdc4gsr.libunified.ir.usbdual.camera.DualViewWithExternalCameraCommonApi
import com.mpdc4gsr.libunified.ir.view.TemperatureView
import com.mpdc4gsr.libunified.ui.camera.CameraPreView
import com.mpdc4gsr.libunified.ui.widget.BitmapConstraintLayout
import com.mpdc4gsr.libunified.ui.widget.LiteSurfaceView
import com.mpdc4gsr.module.thermalunified.compat.ContextProvider
import com.mpdc4gsr.module.thermalunified.compat.dpToPx
import com.mpdc4gsr.module.thermalunified.compat.spToPx
import com.mpdc4gsr.module.thermalunified.view.HikSurfaceView
import com.mpdc4gsr.module.thermalunified.view.TemperatureHikView
import com.mpdc4gsr.module.thermalunified.view.compass.LinearCompassView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.javacv.FFmpegFrameRecorder
import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.FrameFilter
import org.bytedeco.opencv.opencv_core.IplImage
import java.io.File
import java.nio.ByteBuffer
import java.nio.ShortBuffer
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import com.mpdc4gsr.libunified.R as LibcoreR

@SuppressLint("MissingPermission")
class VideoRecordFFmpeg(
    private val cameraView: View,
    private val cameraPreview: CameraPreView?,
    private val temperatureView: View?,
    private val isRecordTemp: Boolean,
    private val thermalPseudoBarView: BitmapConstraintLayout?,
    private val tempBg: TempLayout?,
    private val compassView: LinearCompassView? = null,
    private val dualView: DualViewWithExternalCameraCommonApi? = null,
    private val isTC007: Boolean = false,
    private val carView: View? = null,
    private var customFrameRate: Int = 25,
    private var customBitrate: Int = 1500000,
) : VideoRecord() {
    companion object {
        const val TAG = "VideoRecordFFmpeg"
        const val FORMAT = "mp4"
        const val RATE = 25
        const val VIDEO_BITRATE = 1500000
        var VIDEO_CODEC = avcodec.AV_CODEC_ID_MPEG4
        const val SAMPLE_AUDIO_RETE_INHZ = 44100
        const val AUDIO_CHANNELS = 1
        private const val MAX_RECORDING_DURATION_MS = 60L * 60 * 1000 // One hour in milliseconds

        fun canStartVideoRecord(
            context: Context,
            videoFile: File? = null,
        ): Boolean {
            val availableSpace = context.getExternalFilesDir(null)?.usableSpace ?: 0L
            val canStart = (availableSpace - (videoFile?.length() ?: 0)) > (500L * 1000 * 1000)
            if (!canStart) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    val tipDialogState = TipDialogState(context)
                    tipDialogState.show(
                        title = context.getString(LibcoreR.string.app_tip),
                        message = context.getString(LibcoreR.string.album_report_aleart),
                        showCancel = false,
                        positiveText = context.getString(LibcoreR.string.app_confirm),
                        cancelable = true,
                        onPositive = { },
                    )
                }
            }
            return canStart
        }
    }

    private var alphaPaint: Paint? = null

    @Volatile
    private var isBitmapChangeTime: Long = 0L
    private var audioJob: Job? = null
    private var bitmapJob: Job? = null
    private var recorder: FFmpegFrameRecorder? = null
    private var exportJob: Job? = null
    private val recordScope = CoroutineScope(Dispatchers.IO)

    @Volatile
    private var isRunning = false
    private var exportedFile: File? = null
    private var width = 640
    private var height = 480

    @Volatile
    private var openAudioRecord = true
    private var bufferSize = 0
    private var audioRecord: AudioRecord? = null
    private var audioData: ShortBuffer? = null
    private var tmpAudioData: ShortBuffer? = null
    private var bufferReadResult: Int = 0
    var stopVideoRecordListener: ((shoVideoTip: Boolean) -> Unit)? = null
    val bitmapExecutor = Executors.newScheduledThreadPool(1)
    val recordExecutor = Executors.newScheduledThreadPool(1)
    val audioExecutor = Executors.newScheduledThreadPool(1)
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private var rectText = Rect()
    private val pix20 by lazy { 20f.dpToPx(ContextProvider.getContext()) }
    private val pix10 by lazy { 10f.dpToPx(ContextProvider.getContext()) }
    private val pix6 by lazy { 6f.dpToPx(ContextProvider.getContext()) }
    private val pixArray = ByteArray(width * height * 4)
    private val bufferRef: AtomicReference<ByteBuffer> =
        AtomicReference(ByteBuffer.allocate(pixArray.size))

    private fun readByteBuffer(): ByteBuffer? = bufferRef.get()?.duplicate()

    private fun setBitmap(bitmap: Bitmap) {
        val byteCount = bitmap.byteCount
        val newPixels = ByteBuffer.allocate(byteCount)
        bitmap.copyPixelsToBuffer(newPixels)
        newPixels.flip()
        bitmap.recycle()
        bufferRef.set(newPixels)
    }

    private fun getVideoCodec(): Int =
        if (Build.BRAND == "motorola" && Build.MODEL == "XT2201-2") {
            avcodec.AV_CODEC_ID_H264
        } else {
            avcodec.AV_CODEC_ID_MPEG4
        }

    init {
        if ((cameraView.parent as ViewGroup).height > (cameraView.parent as ViewGroup).width) {
            width = 480
            height =
                width * (cameraView.parent as ViewGroup).height / (cameraView.parent as ViewGroup).width
        } else {
            width = 640
            height =
                width * (cameraView.parent as ViewGroup).height / (cameraView.parent as ViewGroup).width
        }
        if (height % 2 == 1) {
            height -= 1
        }
        VIDEO_CODEC = getVideoCodec()
        bufferSize =
            AudioRecord.getMinBufferSize(
                SAMPLE_AUDIO_RETE_INHZ,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
        audioRecord =
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_AUDIO_RETE_INHZ,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
            )
        paint.color = Color.WHITE
        paint.textSize = 6f.spToPx(ContextProvider.getContext()).toFloat()
        paint.isDither = true
        paint.isFilterBitmap = true
        paint.getTextBounds(
            "[ph][ph][ph][ph][ph][ph]",
            0,
            "[ph][ph][ph][ph][ph][ph]".length,
            rectText,
        )
    }

    var startTime: Long = 0L

    override fun startRecord() {
        startRecord(FileConfig.lineGalleryDir)
    }

    override fun startRecord(fileDir: String) {
        try {
            exportedFile = File(fileDir, "${Date().time}.mp4")
            if (exportedFile!!.exists()) {
                exportedFile!!.delete()
            }
            recorder =
                FFmpegFrameRecorder(
                    exportedFile!!.absolutePath,
                    width,
                    height,
                    AUDIO_CHANNELS,
                )
            recorder!!.format = FORMAT
            recorder!!.frameRate = customFrameRate.toDouble()
            recorder!!.videoBitrate = customBitrate
            recorder!!.videoCodec = VIDEO_CODEC
            recorder!!.sampleRate = SAMPLE_AUDIO_RETE_INHZ
            recorder!!.timestamp = 0L
            recorder!!.start()
            isRunning = true
            isBitmapChangeTime = System.currentTimeMillis()
            if (openAudioRecord &&
                ActivityCompat.checkSelfPermission(
                    cameraView.context,
                    Manifest.permission.RECORD_AUDIO,
                )
                == PackageManager.PERMISSION_GRANTED
            ) {
                startAudioRecording()
            }
            if (audioData == null) {
                audioData = ShortBuffer.allocate(bufferSize / 2)
            }
            if (tmpAudioData == null) {
                tmpAudioData = ShortBuffer.allocate((bufferSize / 2))
            }
            setBitmap(createBitmapFromView())
            val fTime = 1000L / RATE
            bitmapJob =
                recordScope.launch(Dispatchers.IO) {
                    while (isActive && isRunning) {
                        try {
                            val tmp = createBitmapFromView()
                            tmp?.let {
                                setBitmap(it)
                            }
                        } catch (e: Exception) {
                        }
                        delay(fTime)
                    }
                }
            if (audioRecord == null) {
                audioRecord =
                    AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        SAMPLE_AUDIO_RETE_INHZ,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize,
                    )
            }
            startTime = System.currentTimeMillis()
            exportJob =
                recordScope.launch(Dispatchers.IO) {
                    while (isActive && isRunning) {
                        try {
                            val currentTimestamp =
                                1000L * (System.currentTimeMillis() - startTime)
                            val frame = Frame(width, height, Frame.DEPTH_BYTE, 4)
                            frame.image[0] = readByteBuffer()
                            val t = 1000L * (System.currentTimeMillis() - startTime)
                            if (t > (recorder?.timestamp ?: 0)) {
                                recorder!!.timestamp = t
                            }
                            recorder!!.record(frame)
                            frame.close()
                            if (System.currentTimeMillis() - queTime > 60 * 1000) {
                                if (!canStartVideoRecord(cameraView.context, exportedFile)) {
                                    exportJob?.cancel()
                                    stopVideoRecordListener?.invoke(false)
                                    break
                                }
                                queTime = System.currentTimeMillis()
                            }
                            val timestamp = recorder?.timestamp
                            if (timestamp != null && timestamp / 1000 > MAX_RECORDING_DURATION_MS) {
                                exportJob?.cancel()
                                stopVideoRecordListener?.invoke(true)
                                break
                            }
                            if (audioRecord != null) {
                                val audioTime = System.currentTimeMillis()
                                if (openAudioRecord) {
                                    bufferReadResult =
                                        audioRecord?.read(
                                            audioData!!.array(),
                                            0,
                                            audioData!!.capacity(),
                                        )
                                            ?: 0
                                    if (bufferReadResult > 0) {
                                        audioData?.limit(bufferReadResult)
                                        if (currentTimestamp > (recorder?.timestamp ?: 0)) {
                                            recorder!!.timestamp = currentTimestamp
                                        }
                                        recorder?.recordSamples(
                                            SAMPLE_AUDIO_RETE_INHZ,
                                            AUDIO_CHANNELS,
                                            audioData,
                                        )
                                    }
                                } else {
                                    for (i in 0 until tmpAudioData!!.capacity()) {
                                        tmpAudioData!!.put(i, 1.toShort())
                                    }
                                    if (currentTimestamp > (recorder?.timestamp ?: 0)) {
                                        recorder!!.timestamp = currentTimestamp
                                    }
                                    recorder?.recordSamples(
                                        SAMPLE_AUDIO_RETE_INHZ,
                                        AUDIO_CHANNELS,
                                        tmpAudioData,
                                    )
                                }
                            }
                        } catch (e: Exception) {
                        }
                        delay(fTime)
                    }
                }
        } catch (e: Exception) {
            exportJob?.cancel()
            stopVideoRecordListener?.invoke(false)
            e.printStackTrace()
        }
    }

    private class FrameInterpolationFilter(
        private val interpolationFactor: Int,
    ) : FrameFilter() {
        private var previousFrame: Frame? = null

        override fun start() {
            previousFrame = null
        }

        override fun stop() {
            previousFrame = null
        }

        override fun push(frame: Frame) {
            previousFrame = frame.clone()
        }

        override fun pull(): Frame? {
            if (previousFrame == null) {
                return null
            }
            val interpolatedFrame = previousFrame!!.clone()
            interpolatedFrame.timestamp += (1.0 / interpolationFactor).toLong()
            return interpolatedFrame
        }

        override fun release() {
        }

        fun filter(
            image: IplImage?,
            image2: IplImage?,
        ): IplImage? = null
    }

    fun startAudioRecording() {
        audioRecord =
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_AUDIO_RETE_INHZ,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
            )
        audioRecord!!.startRecording()
    }

    fun stopAudioRecording() {
        try {
            if (RECORDSTATE_RECORDING == audioRecord?.recordingState) {
                audioRecord?.stop()
                audioRecord?.release()
                audioRecord =
                    AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        SAMPLE_AUDIO_RETE_INHZ,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize,
                    )
            }
        } catch (e: Exception) {
        }
    }

    fun canStartVideoRecord(videoFile: File?): Boolean {
        val availableSpace = cameraView.context.getExternalFilesDir(null)?.usableSpace ?: 0L
        val canStart = (availableSpace - (videoFile?.length() ?: 0)) > (500L * 1000 * 1000)
        if (!canStart) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                val tipDialogState = TipDialogState(cameraView.context)
                tipDialogState.show(
                    title = cameraView.context.getString(LibcoreR.string.app_tip),
                    message = cameraView.context.getString(LibcoreR.string.album_report_aleart),
                    showCancel = false,
                    positiveText = cameraView.context.getString(LibcoreR.string.app_confirm),
                    cancelable = true,
                    onPositive = { },
                )
            }
        }
        return canStart
    }

    var queTime = 0L

    override fun stopRecord() {
        CoroutineScope(Dispatchers.IO).launch {
            if (isRunning) {
                try {
                    launch(Dispatchers.Main) {
                        exportJob?.cancel()
                        bitmapJob?.cancel()
                        if (RECORDSTATE_RECORDING == audioRecord?.recordingState) {
                            audioRecord?.stop()
                            audioRecord?.release()
                            audioRecord = null
                        }
                        bitmapRecycle()
                        audioJob?.cancel()
                    }
                    bitmapExecutor.shutdown()
                    recordExecutor.shutdown()
                    audioExecutor.shutdown()
                    delay(500)
                    recorder?.stop()
                    delay(300)
                    refreshAlbum()
                } catch (e: Exception) {
                }
            }
            isRunning = false
        }
    }

    private fun bitmapRecycle() {
        tempBitmap?.let {
            if (!it.isRecycled) {
                it.recycle()
            }
            tempBitmap = null
        }
        cameraBitmap?.let {
            if (!it.isRecycled) {
                it.recycle()
            }
            cameraBitmap = null
        }
    }

    override fun updateAudioState(audioRecord: Boolean) {
        if (this@VideoRecordFFmpeg.openAudioRecord == audioRecord) {
            return
        }
        try {
            if (audioRecord && isRunning) {
                startAudioRecording()
            } else {
                stopAudioRecording()
            }
            this@VideoRecordFFmpeg.openAudioRecord = audioRecord
        } catch (exception: Exception) {
            LibraryLogger.e("VideoRecordFFmpeg", "Unexpected Exception in VideoRecordFFmpeg catch block", exception)
        }
    }

    private fun createBitmapFromView(): Bitmap {
        var cameraViewBitmap: Bitmap
        when (cameraView) {
            is CameraView ->
                cameraViewBitmap =
                    if (dualView == null) {
                        cameraView.getScaledBitmap()
                            ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                    } else {
                        dualView.scaledBitmap
                            ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                    }

            is TextureView -> {
                cameraViewBitmap =
                    Bitmap.createBitmap(
                        cameraView.width,
                        cameraView.height,
                        Bitmap.Config.ARGB_8888,
                    )
                cameraView.getBitmap(cameraViewBitmap)
            }

            is LiteSurfaceView ->
                cameraViewBitmap =
                    cameraView.scaleBitmap() ?: Bitmap.createBitmap(
                        cameraView.width,
                        cameraView.height,
                        Bitmap.Config.ARGB_8888,
                    )

            is HikSurfaceView -> cameraViewBitmap = cameraView.getScaleBitmap()
            else ->
                cameraViewBitmap =
                    Bitmap.createBitmap(cameraView.width, cameraView.height, Bitmap.Config.ARGB_8888)
        }
        when (temperatureView) {
            is TemperatureView -> {
                if (isRecordTemp) {
                    if (temperatureView.temperatureRegionMode != TemperatureView.REGION_MODE_CLEAN) {
                        cameraViewBitmap =
                            BitmapUtils.mergeBitmap(
                                cameraViewBitmap,
                                temperatureView.regionBitmap,
                                0,
                                0,
                            )
                    }
                } else {
                    if (temperatureView.temperatureRegionMode == TemperatureView.REGION_MODE_RESET) {
                        cameraViewBitmap =
                            BitmapUtils.mergeBitmap(
                                cameraViewBitmap,
                                temperatureView.regionBitmap,
                                0,
                                0,
                            )
                    }
                }
            }

            is TemperatureHikView -> {
                temperatureView.draw(Canvas(cameraViewBitmap))
            }
        }
        if (thermalPseudoBarView?.visibility == VISIBLE) {
            try {
                thermalPseudoBarView.drawToBitmap()?.let { bitmap ->
                    cameraViewBitmap =
                        BitmapUtils.mergeBitmap(
                            cameraViewBitmap,
                            bitmap,
                            cameraViewBitmap!!.width - bitmap.width,
                            (cameraViewBitmap!!.height - bitmap.height) / 2,
                        )
                }
            } catch (e: Exception) {
                LibraryLogger.e("VideoRecordFFmpeg", "Unexpected Exception in VideoRecordFFmpeg catch block", e)
            }
        }
        if (true == tempBg?.isVisible) {
            if (alphaPaint == null) {
                alphaPaint = Paint()
            }
            alphaPaint?.alpha = (tempBg!!.animatorAlpha * 255).toInt()
            cameraViewBitmap =
                BitmapUtils.mergeBitmapAlpha(
                    cameraViewBitmap,
                    tempBg!!.drawToBitmap(),
                    alphaPaint,
                    0,
                    0,
                )
        }
        if (carView?.isVisible == true) {
            cameraViewBitmap =
                BitmapUtils.mergeBitmap(
                    cameraViewBitmap,
                    carView?.drawToBitmap(),
                    0,
                    0,
                )
        }
        compassView?.let {
            if (it.isVisible) {
                try {
                    val bitmap = it.curBitmap
                    cameraViewBitmap =
                        BitmapUtils.mergeBitmap(
                            cameraViewBitmap,
                            bitmap,
                            ((cameraView.parent as ViewGroup).width - it.width) / 2,
                            20f.dpToPx(ContextProvider.getContext()).toInt(),
                        )
                } catch (e: Exception) {
                }
            }
        }
        cameraPreview?.let { preview ->
            if (preview.isVisible) {
                val bitmapFromView = preview.getBitmap()
                bitmapFromView?.let { bitmap ->
                    // Simple bitmap overlay instead of BitmapUtils.mergeBitmapByView
                    cameraViewBitmap?.let { baseBitmap ->
                        val canvas = Canvas(baseBitmap)
                        canvas.drawBitmap(bitmap, 0f, 0f, null)
                        cameraViewBitmap = baseBitmap
                    }
                }
            }
        }
        var dstBitmap = Bitmap.createScaledBitmap(cameraViewBitmap, width, height, true)
        val watermarkBean =
            if (isTC007) {
                SharedManager.wifiWatermarkBean
            } else {
                SharedManager.watermarkBean
            }
        if (watermarkBean.isOpen) {
            dstBitmap =
                drawCenterLable(
                    dstBitmap!!,
                    watermarkBean.title,
                    watermarkBean.address,
                    if (watermarkBean.isAddTime) TimeTools.getNowTime() else "",
                )!!
        }
        return dstBitmap
    }

    private var cameraBitmap: Bitmap? = null
    private var tempBitmap: Bitmap? = null

    fun drawCenterLable(
        bmp: Bitmap,
        title: String,
        address: String,
        time: String?,
    ): Bitmap {
        val newBmp = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(newBmp)
        canvas.drawBitmap(bmp, 0f, 0f, null)
        canvas.save()
        val beginX = pix10.toDouble()
        var beginY = (bmp.height - pix10).toDouble()
        paint.getTextBounds(
            "[ph][ph][ph][ph][ph][ph]",
            0,
            "[ph][ph][ph][ph][ph][ph]".length,
            rectText,
        )
        if (!TextUtils.isEmpty(time)) {
            beginY = beginY - (rectText.bottom - rectText.top)
            canvas.drawText(time!!, beginX.toInt().toFloat(), beginY.toInt().toFloat(), paint)
            beginY -= pix6.toDouble()
        }
        if (!TextUtils.isEmpty(address)) {
            val textHeight = (rectText.bottom - rectText.top)
            paint.getTextBounds(address, 0, address.length, rectText)
            if (rectText.width() > bmp.width - pix20) {
                val staticLayout =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        StaticLayout.Builder
                            .obtain(
                                address,
                                0,
                                address.length,
                                paint,
                                bmp.width - pix20.toInt(),
                            ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(0.0f, 1.0f)
                            .setIncludePad(false)
                            .build()
                    } else {
                        @Suppress("DEPRECATION")
                        StaticLayout(
                            address,
                            paint,
                            bmp.width - pix20.toInt(),
                            Layout.Alignment.ALIGN_NORMAL,
                            1.0f,
                            0.0f,
                            false,
                        )
                    }
                beginY = beginY - (textHeight + 1f.dpToPx(ContextProvider.getContext())) * staticLayout.lineCount
                canvas.save()
                canvas.translate(beginX.toInt().toFloat(), (beginY.toInt() - textHeight).toFloat())
                staticLayout.draw(canvas)
                canvas.restore()
            } else {
                beginY = beginY - textHeight
                canvas.drawText(address, beginX.toInt().toFloat(), beginY.toInt().toFloat(), paint)
            }
            beginY -= pix6.toDouble()
        }
        if (!TextUtils.isEmpty(title)) {
            val textHeight = rectText.bottom - rectText.top
            paint.getTextBounds(title, 0, title.length, rectText)
            if (rectText.width() > bmp.width - pix20) {
                val staticLayout =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        StaticLayout.Builder
                            .obtain(
                                title,
                                0,
                                title.length,
                                paint,
                                bmp.width - pix20.toInt(),
                            ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(0.0f, 1.0f)
                            .setIncludePad(false)
                            .build()
                    } else {
                        @Suppress("DEPRECATION")
                        StaticLayout(
                            title,
                            paint,
                            bmp.width - pix20.toInt(),
                            Layout.Alignment.ALIGN_NORMAL,
                            1.0f,
                            0.0f,
                            false,
                        )
                    }
                beginY = beginY - textHeight * staticLayout.lineCount
                canvas.save()
                canvas.translate(beginX.toInt().toFloat(), (beginY.toInt() - textHeight).toFloat())
                staticLayout.draw(canvas)
                canvas.restore()
            } else {
                beginY = beginY - textHeight
                canvas.drawText(title, beginX.toInt().toFloat(), beginY.toInt().toFloat(), paint)
            }
            beginY -= pix6.toDouble()
        }
        canvas.restore()
        if (!bmp.isRecycled) {
            bmp.recycle()
        }
        return newBmp
    }

    private fun refreshAlbum() {
        exportedFile?.let {
            MediaScannerConnection.scanFile(ContextProvider.getContext(), arrayOf(it.toString()), null, null)
        }
    }
}
