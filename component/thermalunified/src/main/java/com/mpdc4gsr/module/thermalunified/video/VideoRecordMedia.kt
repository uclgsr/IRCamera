package com.mpdc4gsr.module.thermal.ir.video

import android.graphics.Bitmap
import com.mpdc4gsr.libunified.ir.view.CameraView
import com.mpdc4gsr.libunified.ir.view.TemperatureView
import com.mpdc4gsr.libunified.app.config.FileConfig
import com.mpdc4gsr.libunified.app.utils.BitmapUtils
import com.mpdc4gsr.module.thermal.ir.video.media.Encoder
import com.mpdc4gsr.module.thermal.ir.video.media.MP4Encoder
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.io.File
import java.util.Date
import java.util.concurrent.TimeUnit

class VideoRecordMedia(
    private var cameraView: CameraView,
    private var temperatureView: TemperatureView,
) : VideoRecord() {
    private lateinit var exportDisposable: Disposable
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

        exportDisposable =
            Observable.interval(50, TimeUnit.MILLISECONDS)
                .map {
                    createBitmapFromView()
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    encoder.addFrame(it)
                }
    }

    override fun startRecord(fileDir: String) {
    }

    override fun stopRecord() {
        if (isRunning) {
            encoder.stopEncode()
            exportDisposable.dispose()
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
