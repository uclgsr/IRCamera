package com.mpdc4gsr.component.thermal.video

import android.graphics.Bitmap
import com.infisense.usbir.view.CameraView
import com.mpdc4gsr.component.shared.app.config.FileConfig
import com.mpdc4gsr.component.shared.app.utils.BitmapUtils
import com.mpdc4gsr.component.shared.ir.view.TemperatureView
import com.mpdc4gsr.component.thermal.video.media.Encoder
import com.mpdc4gsr.component.thermal.video.media.MP4Encoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class VideoRecordMedia(
    private var cameraView: CameraView,
    private var temperatureView: TemperatureView,
) : VideoRecord() {
    private var exportJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var encoder: Encoder = MP4Encoder()
    private var isRunning = false
    var width = 480
    var height = 640

    init {
        encoder.setFrameDelay(25)
        width = 480
        height = width * cameraView.height / cameraView.width
        if (height % 2 == 1) {
            height -= 1
        }
    }

    override fun startRecord() {
        val downloadDir = FileConfig.lineGalleryDir
        val exportedFile = File(downloadDir, "${Date().time}.mp4")
        if (exportedFile.exists()) {
            exportedFile.delete()
        }
        encoder.setOutputFilePath(exportedFile.path)
        encoder.setOutputSize(width, height)
        encoder.startEncode()
        isRunning = true
        exportJob =
            coroutineScope.launch {
                while (isActive && isRunning) {
                    val bitmap = createBitmapFromView()
                    encoder.addFrame(bitmap)
                    delay(50)
                }
            }
    }

    override fun startRecord(fileDir: String) {
    }

    override fun stopRecord() {
        if (isRunning) {
            encoder.stopEncode()
            exportJob?.cancel()
        }
        isRunning = false
    }

    override fun updateAudioState(audioRecord: Boolean) {
    }

    private fun createBitmapFromView(): Bitmap {
        var cameraViewBitmap = cameraView.bitmap
        if (temperatureView.temperatureRegionMode != TemperatureView.REGION_MODE_CLEAN) {
            cameraViewBitmap =
                BitmapUtils.mergeBitmap(
                    cameraViewBitmap,
                    temperatureView.regionAndValueBitmap,
                    0,
                    0,
                )
        }
        val dstBitmap =
            if (cameraViewBitmap != null) {
                Bitmap.createScaledBitmap(cameraViewBitmap, width, height, true)
            } else {
                Bitmap.createBitmap(
                    width,
                    height,
                    Bitmap.Config.ARGB_8888,
                )
            }
        return dstBitmap
    }
}



