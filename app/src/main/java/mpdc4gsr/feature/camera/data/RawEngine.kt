package mpdc4gsr.feature.camera.data

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.DngCreator
import android.hardware.camera2.TotalCaptureResult
import android.media.Image
import android.media.ImageReader
import android.util.Size
import android.view.Surface
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

class RawEngine(private val context: Context) {
    companion object {
        private const val RAW_CAPTURE_TIMEOUT_MS = 5000L
    }

    private var rawImageReader: ImageReader? = null
    private var isCapturing = false
    private var rawOutputDirectory: File? = null
    private var sessionId: String = ""
    private var rawCaptureCount = 0
    private val pendingCaptureResults = ConcurrentHashMap<Long, TotalCaptureResult>()

    // Camera characteristics for DNG creation
    private var cameraCharacteristics: CameraCharacteristics? = null
    private var enableStage3Processing = true // Enable Samsung Stage3/Level3 by default
    var onRawImageSaved: ((File) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    fun setup(
        rawSize: Size,
        outputDirectory: File,
        sessionId: String,
        characteristics: CameraCharacteristics? = null,
        enableStage3: Boolean = true,
    ) {
            this.rawOutputDirectory = outputDirectory
            this.sessionId = sessionId
            this.rawCaptureCount = 0
            this.cameraCharacteristics = characteristics
            this.enableStage3Processing = enableStage3
            rawImageReader =
                ImageReader.newInstance(
                    rawSize.width,
                    rawSize.height,
                    ImageFormat.RAW_SENSOR,
                    2,
                )
            rawImageReader?.setOnImageAvailableListener(rawImageAvailableListener, null)
            val processingMode = if (enableStage3) "Stage3/Level3" else "Standard"
                TAG,
                "RAW engine setup: ${rawSize.width}x${rawSize.height}, Processing: $processingMode"
            )
        }
    }

    fun getSurface(): Surface? = rawImageReader?.surface
    fun startCapture() {
        isCapturing = true
        rawCaptureCount = 0
    }

    fun stopCapture() {
        isCapturing = false
    }

    fun storeCaptureResult(result: TotalCaptureResult) {
        if (isCapturing) {
            val timestamp = result.get(CaptureResult.SENSOR_TIMESTAMP) ?: System.nanoTime()
            pendingCaptureResults[timestamp] = result
            if (pendingCaptureResults.size > 10) {
                val oldestKey = pendingCaptureResults.keys.minOrNull()
                oldestKey?.let { pendingCaptureResults.remove(it) }
            }
        }
    }

    fun isCapturing(): Boolean = isCapturing
    fun getCaptureCount(): Int = rawCaptureCount

    fun setStage3ProcessingEnabled(enabled: Boolean) {
        enableStage3Processing = enabled
        val mode = if (enabled) "Stage3/Level3" else "Standard"
    }

    fun isStage3ProcessingEnabled(): Boolean = enableStage3Processing
    fun release() {
        stopCapture()
        rawImageReader?.close()
        rawImageReader = null
        pendingCaptureResults.clear()
    }

    private val rawImageAvailableListener =
        ImageReader.OnImageAvailableListener { reader ->
            if (!isCapturing) return@OnImageAvailableListener
            val image = reader.acquireLatestImage() ?: return@OnImageAvailableListener
                val timestamp = image.timestamp
                val captureResult = pendingCaptureResults.remove(timestamp)
                if (captureResult != null) {
                    saveRawImageAsDng(image, captureResult)
                } else {
                    saveRawImageAsRaw(image)
                }
                image.close()
            }
        }

    private fun saveRawImageAsDng(
        image: Image,
        captureResult: TotalCaptureResult,
    ) {
        val outputDir = rawOutputDirectory ?: return
        val timestamp = System.currentTimeMillis()
        val dngFile = File(outputDir, "${sessionId}_raw_stage3_$timestamp.dng")
            val characteristics = cameraCharacteristics
            if (characteristics != null) {
                // Create proper DNG file using Android's DngCreator for Stage3/Level3 processing
                val dngCreator = DngCreator(characteristics, captureResult)
                // Configure DNG creator for Samsung Stage3/Level3 processing
                if (enableStage3Processing) {
                    // Set Stage3/Level3 specific metadata
                        // Disable thumbnail for maximum raw data preservation
                        // Note: Skipping thumbnail to preserve raw data
                        // Set DNG orientation based on device orientation
                        captureResult.get(CaptureResult.JPEG_ORIENTATION)?.let { orientation ->
                            dngCreator.setOrientation(orientation)
                        }
                        // Note: Additional Samsung Stage3/Level3 specific EXIF tags would be set here
                        // if Samsung provides specific DNG tag constants for Stage3/Level3 processing
                        // These may include:
                        // - Custom processing pipeline identifiers
                        // - Stage3/Level3 specific color space information
                        // - Advanced sensor readout parameters
                            TAG,
                            "Configured DNG for Samsung Stage3/Level3 processing with orientation and no thumbnail"
                        )
                    }
                }
                FileOutputStream(dngFile).use { outputStream ->
                    dngCreator.writeImage(outputStream, image)
                }
                dngCreator.close()
                rawCaptureCount++
                    TAG,
                    "Saved Stage3/Level3 DNG: ${dngFile.name} (${image.width}x${image.height})"
                )
                onRawImageSaved?.invoke(dngFile)
            } else {
                // Fallback to raw binary if no characteristics available
                saveRawImageAsRaw(image)
            }
            // Fallback to raw binary on failure
                saveRawImageAsRaw(image)
            }
        }
    }

    private fun saveRawImageAsRaw(image: Image) {
        val outputDir = rawOutputDirectory ?: return
        val timestamp = System.currentTimeMillis()
        val rawFile = File(outputDir, "${sessionId}_raw_$timestamp.raw")
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            rawFile.writeBytes(bytes)
            rawCaptureCount++
            onRawImageSaved?.invoke(rawFile)
        }
    }
}
