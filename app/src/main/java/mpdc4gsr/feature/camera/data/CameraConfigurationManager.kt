package mpdc4gsr.feature.camera.data

import android.os.Build
import android.util.Size
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder

class CameraConfigurationManager {
    companion object {

        // Video configuration constants
        private const val VIDEO_WIDTH_4K = 3840
        private const val VIDEO_HEIGHT_4K = 2160
        private const val VIDEO_WIDTH_1080P = 1920
        private const val VIDEO_HEIGHT_1080P = 1080
        private const val VIDEO_FPS_60 = 60
        private const val VIDEO_FPS_TARGET = 30
        private const val VIDEO_FPS_FALLBACK = 24
        private const val VIDEO_BITRATE_4K = 50_000_000
        private const val VIDEO_BITRATE_1080P = 20_000_000
        private const val JPEG_QUALITY = 100

        // Known devices that support specific features
        private val KNOWN_4K_DEVICES = setOf(
            "SM-S906B", "SM-S916B", "SM-S908B", "SM-S901B", "SM-S911B", "SM-S918B"
        )
        private val KNOWN_RAW_DEVICES = setOf(
            "SM-S906B", "SM-S916B", "SM-S908B", "SM-S901B", "SM-S911B", "SM-S918B"
        )
    }

    data class CameraConfiguration(
        val videoWidth: Int,
        val videoHeight: Int,
        val videoFps: Int,
        val videoBitrate: Int,
        val supports4K: Boolean,
        val supportsRAW: Boolean,
        val supports60fps: Boolean
    )

    fun detectDeviceCapabilities(): Triple<Boolean, Boolean, Boolean> {
        return (
            val deviceModel = Build.MODEL
            val manufacturer = Build.MANUFACTURER.lowercase()
            val deviceSupports4K = when {
                manufacturer == "samsung" && deviceModel in KNOWN_4K_DEVICES -> true
                manufacturer == "google" && deviceModel.startsWith("Pixel") -> true
                manufacturer == "oneplus" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> true
                else -> false
            }
            val deviceSupportsRAW = when {
                manufacturer == "samsung" && deviceModel in KNOWN_RAW_DEVICES -> true
                manufacturer == "google" && deviceModel.startsWith("Pixel") -> true
                else -> false
            }
            val supports60fps = when {
                manufacturer == "samsung" && (
                        deviceModel.startsWith("SM-S9") ||
                                deviceModel.startsWith("SM-S10") ||
                                deviceModel.startsWith("SM-G9") ||
                                deviceModel.startsWith("SM-G99")
                        ) -> true

                else -> false
            }
                TAG,
                "Device capabilities - 4K: $deviceSupports4K, RAW: $deviceSupportsRAW, 60fps: $supports60fps"
            )
            Triple(deviceSupports4K, deviceSupportsRAW, supports60fps)
            Triple(false, false, false)
        }
    }

    fun createOptimizedConfiguration(): CameraConfiguration {
        val (supports4K, supportsRAW, supports60fps) = detectDeviceCapabilities()
        return if (supports4K) {
            CameraConfiguration(
                videoWidth = VIDEO_WIDTH_4K,
                videoHeight = VIDEO_HEIGHT_4K,
                videoFps = if (supports60fps) VIDEO_FPS_60 else VIDEO_FPS_TARGET,
                videoBitrate = VIDEO_BITRATE_4K,
                supports4K = true,
                supportsRAW = supportsRAW,
                supports60fps = supports60fps
            )
        } else {
            CameraConfiguration(
                videoWidth = VIDEO_WIDTH_1080P,
                videoHeight = VIDEO_HEIGHT_1080P,
                videoFps = if (supports60fps) VIDEO_FPS_60 else VIDEO_FPS_TARGET,
                videoBitrate = VIDEO_BITRATE_1080P,
                supports4K = false,
                supportsRAW = supportsRAW,
                supports60fps = supports60fps
            )
        }
    }

    fun createOptimizedRecorder(configuration: CameraConfiguration): Recorder {
        return (
            val qualitySelector = if (configuration.supports4K) {
                QualitySelector.from(
                    Quality.UHD,
                    FallbackStrategy.lowerQualityThan(Quality.UHD)
                )
            } else {
                QualitySelector.from(
                    Quality.FHD,
                    FallbackStrategy.lowerQualityThan(Quality.FHD)
                )
            }
            Recorder.Builder()
                .setQualitySelector(qualitySelector)
                .build()
            Recorder.Builder()
                .setQualitySelector(
                    QualitySelector.from(
                        Quality.FHD,
                        FallbackStrategy.lowerQualityThan(Quality.FHD)
                    )
                )
                .build()
        }
    }

    fun createPreviewConfiguration(configuration: CameraConfiguration): Preview {
        return Preview.Builder().apply {
            val previewSize = Size(configuration.videoWidth, configuration.videoHeight)
            @Suppress("DEPRECATION")
            setTargetResolution(previewSize)
        }.build()
    }

    fun createImageCaptureConfiguration(configuration: CameraConfiguration): ImageCapture {
        return ImageCapture.Builder().apply {
            @Suppress("DEPRECATION")
            setTargetResolution(Size(configuration.videoWidth, configuration.videoHeight))
            setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            setJpegQuality(JPEG_QUALITY)
            setFlashMode(ImageCapture.FLASH_MODE_AUTO)
            if (configuration.supportsRAW) {
                    androidx.camera.camera2.interop.Camera2Interop.Extender(this)
                        .setCaptureRequestOption(
                            android.hardware.camera2.CaptureRequest.CONTROL_MODE,
                            android.hardware.camera2.CameraMetadata.CONTROL_MODE_USE_SCENE_MODE
                        )
                }
            }
        }.build()
    }

    fun getConfigurationSummary(configuration: CameraConfiguration): String {
        return buildString {
            appendLine("Camera Configuration:")
            appendLine("  Resolution: ${configuration.videoWidth}x${configuration.videoHeight}")
            appendLine("  Frame Rate: ${configuration.videoFps}fps")
            appendLine("  Bitrate: ${configuration.videoBitrate}")
            appendLine("  4K Support: ${configuration.supports4K}")
            appendLine("  RAW Support: ${configuration.supportsRAW}")
            appendLine("  60fps Support: ${configuration.supports60fps}")
        }
    }
}