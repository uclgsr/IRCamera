package mpdc4gsr.feature.capture.camera.data

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.MediaRecorder
import android.util.Range
import android.util.Size
import androidx.core.content.getSystemService

/**
 * Provides consolidated access to Camera2 device capabilities.
 *
 * The legacy implementation scattered capability checks across multiple call sites and,
 * in a few cases, bound cameras solely to inspect the characteristics. The new manager
 * centralises this logic so that feature and recorder code can share a consistent view
 * of what the hardware supports while avoiding unnecessary bind/unbind churn.
 */
class CameraConfigurationManager(
    private val context: Context,
) {
    data class DeviceCapabilities(
        val cameraId: String,
        val lensFacing: Int,
        val supports4K: Boolean,
        val supportsRaw: Boolean,
        val supports60Fps: Boolean,
        val supportedVideoSizes: List<Size>,
        val supportedFpsRanges: List<Range<Int>>,
        val hardwareLevel: Int?,
    ) {
        val isHighResolutionSupported: Boolean
            get() = supports4K || supportedVideoSizes.any { it.isAtLeast(3840, 2160) }

        fun summary(): String =
            buildString {
                append("cameraId=").append(cameraId)
                append(", lensFacing=").append(lensFacing)
                append(", 4K=").append(supports4K)
                append(", RAW=").append(supportsRaw)
                append(", 60fps=").append(supports60Fps)
                append(", hardwareLevel=").append(hardwareLevel ?: "unknown")
            }
    }

    fun detectDeviceCapabilities(useFrontCamera: Boolean = false): DeviceCapabilities {
        return runCatching {
            val cameraManager =
                context.getSystemService<CameraManager>()
                    ?: return DEFAULT_CAPABILITIES
            val desiredFacing =
                if (useFrontCamera) {
                    CameraCharacteristics.LENS_FACING_FRONT
                } else {
                    CameraCharacteristics.LENS_FACING_BACK
                }

            var selectedId: String? = null
            var selectedCharacteristics: CameraCharacteristics? = null

            cameraManager.cameraIdList.forEach { cameraId ->
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)

                if (selectedId == null) {
                    selectedId = cameraId
                    selectedCharacteristics = characteristics
                }

                if (facing == desiredFacing) {
                    selectedId = cameraId
                    selectedCharacteristics = characteristics
                    return@forEach
                }
            }

            val cameraId = selectedId ?: return DEFAULT_CAPABILITIES
            val characteristics = selectedCharacteristics ?: return DEFAULT_CAPABILITIES
            val streamConfig = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            val videoSizes = extractVideoSizes(streamConfig)
            val fpsRanges =
                characteristics
                    .get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
                    ?.toList()
                    ?: emptyList()

            val supports4K = videoSizes.any { it.isAtLeast(3840, 2160) }
            val availableCaps =
                characteristics
                    .get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                    ?.toSet()
                    ?: emptySet()
            val supportsRaw =
                availableCaps.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)
            val supports60fps = fpsRanges.any { it.upper >= 60 }
            val hardwareLevel =
                characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)

            DeviceCapabilities(
                cameraId = cameraId,
                lensFacing =
                    characteristics.get(CameraCharacteristics.LENS_FACING)
                        ?: desiredFacing,
                supports4K = supports4K,
                supportsRaw = supportsRaw,
                supports60Fps = supports60fps,
                supportedVideoSizes = videoSizes,
                supportedFpsRanges = fpsRanges,
                hardwareLevel = hardwareLevel,
            )
        }.getOrElse {
            DEFAULT_CAPABILITIES
        }
    }

    fun getPreferredVideoSize(
        capabilities: DeviceCapabilities,
        prefer4k: Boolean = true,
    ): Size {
        val sizes = capabilities.supportedVideoSizes
        if (sizes.isEmpty()) {
            return DEFAULT_VIDEO_SIZE
        }
        if (prefer4k && capabilities.supports4K) {
            return sizes.firstOrNull { it.isAtLeast(3840, 2160) } ?: DEFAULT_VIDEO_SIZE
        }
        return sizes.firstOrNull { it.isAtLeast(1920, 1080) } ?: sizes.first()
    }

    fun getPreferredPreviewSize(capabilities: DeviceCapabilities): Size {
        val sizes = capabilities.supportedVideoSizes
        if (sizes.isEmpty()) {
            return DEFAULT_PREVIEW_SIZE
        }
        return sizes.firstOrNull { it.isAtLeast(1920, 1080) } ?: DEFAULT_PREVIEW_SIZE
    }

    private fun extractVideoSizes(streamConfig: StreamConfigurationMap?): List<Size> {
        if (streamConfig == null) return emptyList()
        val sizes = mutableSetOf<Size>()
        streamConfig.getOutputSizes(MediaRecorder::class.java)?.let { sizes.addAll(it) }
        @Suppress("DEPRECATION")
        streamConfig.getOutputSizes(SurfaceTexture::class.java)?.let { sizes.addAll(it) }
        return sizes
            .asSequence()
            .map { Size(it.width, it.height) }
            .distinct()
            .sortedByDescending { it.width * it.height }
            .toList()
    }

    private fun Size.isAtLeast(
        width: Int,
        height: Int,
    ): Boolean = this.width >= width && this.height >= height

    companion object {
        private val DEFAULT_VIDEO_SIZE = Size(1920, 1080)
        private val DEFAULT_PREVIEW_SIZE = Size(1280, 720)
        private val DEFAULT_CAPABILITIES =
            DeviceCapabilities(
                cameraId = "unknown",
                lensFacing = CameraCharacteristics.LENS_FACING_BACK,
                supports4K = false,
                supportsRaw = false,
                supports60Fps = false,
                supportedVideoSizes = emptyList(),
                supportedFpsRanges = emptyList(),
                hardwareLevel = null,
            )
    }
}

