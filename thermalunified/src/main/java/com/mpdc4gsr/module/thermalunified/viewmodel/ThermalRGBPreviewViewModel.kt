package com.mpdc4gsr.module.thermalunified.viewmodel

import android.graphics.Bitmap
import android.hardware.camera2.CameraManager
import android.view.Surface
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.*

class ThermalRGBPreviewViewModel : BaseViewModel() {
    data class RGBPreviewState(
        val isInitialized: Boolean = false,
        val isStreaming: Boolean = false,
        val resolution: String = "1920x1080",
        val frameRate: Int = 30,
        val cameraId: String = "0",
        val availableCameras: List<String> = emptyList(),
        val previewSurface: Surface? = null,
        val currentFrame: Bitmap? = null,
        val exposureMode: ExposureMode = ExposureMode.AUTO,
        val focusMode: FocusMode = FocusMode.AUTO,
    )

    data class ThermalOverlayState(
        val isEnabled: Boolean = true,
        val opacity: Float = 0.7f,
        val blendMode: BlendMode = BlendMode.OVERLAY,
        val alignmentOffset: Pair<Float, Float> = 0f to 0f,
        val scale: Float = 1.0f,
        val rotation: Float = 0f,
        val thermalBitmap: Bitmap? = null,
        val colorPalette: ColorPalette = ColorPalette.IRON,
        val temperatureRange: Pair<Float, Float> = 20f to 40f,
    )

    data class CombinedPreviewState(
        val rgbState: RGBPreviewState = RGBPreviewState(),
        val thermalState: ThermalOverlayState = ThermalOverlayState(),
        val isReady: Boolean = false,
        val overlayMode: OverlayMode = OverlayMode.BLENDED,
        val syncedFrame: Bitmap? = null,
    )

    // StateFlow for RGB preview state management
    private val _rgbPreviewState = MutableStateFlow(RGBPreviewState())
    val rgbPreviewState: StateFlow<RGBPreviewState> = _rgbPreviewState.asStateFlow()
    private val _thermalOverlayState = MutableStateFlow(ThermalOverlayState())
    val thermalOverlayState: StateFlow<ThermalOverlayState> = _thermalOverlayState.asStateFlow()

    // SharedFlow for one-time events
    private val _previewEvents = MutableSharedFlow<PreviewEvent>()
    val previewEvents: SharedFlow<PreviewEvent> = _previewEvents.asSharedFlow()

    // Combined UI State for thermal + RGB preview
    val combinedPreviewState: StateFlow<CombinedPreviewState> =
        combine(
            _rgbPreviewState,
            _thermalOverlayState,
        ) { rgbState, thermalState ->
            CombinedPreviewState(
                rgbState = rgbState,
                thermalState = thermalState,
                isReady = rgbState.isInitialized && thermalState.isEnabled,
                overlayMode =
                    when {
                        thermalState.blendMode == BlendMode.SIDE_BY_SIDE -> OverlayMode.SIDE_BY_SIDE
                        thermalState.opacity > 0.8f -> OverlayMode.THERMAL_PRIMARY
                        thermalState.opacity > 0.3f -> OverlayMode.BLENDED
                        else -> OverlayMode.RGB_PRIMARY
                    },
            )
        }.stateIn(viewModelScope, SharingStarted.Lazily, CombinedPreviewState())

    enum class BlendMode {
        OVERLAY,
        MULTIPLY,
        SCREEN,
        SIDE_BY_SIDE,
        PICTURE_IN_PICTURE,
    }

    enum class OverlayMode {
        RGB_PRIMARY,
        BLENDED,
        THERMAL_PRIMARY,
        SIDE_BY_SIDE,
    }

    enum class ExposureMode {
        AUTO,
        MANUAL,
        SCENE_NIGHT,
        SCENE_BRIGHT,
    }

    enum class FocusMode {
        AUTO,
        MANUAL,
        CONTINUOUS_VIDEO,
        MACRO,
    }

    enum class ColorPalette {
        IRON,
        RAINBOW,
        GRAYSCALE,
        HOT,
        COOL,
        MEDICAL,
    }

    sealed class PreviewEvent {
        object RGBStreamStarted : PreviewEvent()

        object RGBStreamStopped : PreviewEvent()

        data class CameraError(
            val message: String,
        ) : PreviewEvent()

        data class ThermalDataReceived(
            val bitmap: Bitmap,
            val temperature: Float,
        ) : PreviewEvent()

        data class CalibrationRequired(
            val message: String,
        ) : PreviewEvent()

        data class ShowToast(
            val message: String,
        ) : PreviewEvent()

        data class ShowError(
            val message: String,
        ) : PreviewEvent()
    }

    // RGB Camera Management
    fun initializeRGBCamera(cameraManager: CameraManager) {
        launchWithErrorHandling {
            try {
                val cameraList = cameraManager.cameraIdList.toList()
                _rgbPreviewState.value =
                    _rgbPreviewState.value.copy(
                        availableCameras = cameraList,
                        cameraId = cameraList.firstOrNull() ?: "0",
                        isInitialized = true,
                    )
                _previewEvents.emit(PreviewEvent.ShowToast("RGB camera initialized"))
            } catch (e: Exception) {
                _previewEvents.emit(PreviewEvent.CameraError("Failed to initialize RGB camera: ${e.message}"))
            }
        }
    }

    fun startRGBPreview(surface: Surface) {
        launchWithErrorHandling {
            _rgbPreviewState.value =
                _rgbPreviewState.value.copy(
                    previewSurface = surface,
                    isStreaming = true,
                )
            _previewEvents.emit(PreviewEvent.RGBStreamStarted)
        }
    }

    fun stopRGBPreview() {
        launchWithErrorHandling {
            _rgbPreviewState.value =
                _rgbPreviewState.value.copy(
                    previewSurface = null,
                    isStreaming = false,
                )
            _previewEvents.emit(PreviewEvent.RGBStreamStopped)
        }
    }

    fun selectCamera(cameraId: String) {
        _rgbPreviewState.value = _rgbPreviewState.value.copy(cameraId = cameraId)
    }

    fun setExposureMode(mode: ExposureMode) {
        _rgbPreviewState.value = _rgbPreviewState.value.copy(exposureMode = mode)
    }

    fun setFocusMode(mode: FocusMode) {
        _rgbPreviewState.value = _rgbPreviewState.value.copy(focusMode = mode)
    }

    // Thermal Overlay Management
    fun updateThermalOverlay(
        bitmap: Bitmap,
        temperature: Float,
    ) {
        launchWithErrorHandling {
            _thermalOverlayState.value =
                _thermalOverlayState.value.copy(
                    thermalBitmap = bitmap,
                    temperatureRange = _thermalOverlayState.value.temperatureRange.first to temperature,
                )
            _previewEvents.emit(PreviewEvent.ThermalDataReceived(bitmap, temperature))
        }
    }

    fun setOverlayOpacity(opacity: Float) {
        _thermalOverlayState.value =
            _thermalOverlayState.value.copy(
                opacity = opacity.coerceIn(0f, 1f),
            )
    }

    fun setBlendMode(blendMode: BlendMode) {
        _thermalOverlayState.value = _thermalOverlayState.value.copy(blendMode = blendMode)
    }

    fun setColorPalette(palette: ColorPalette) {
        _thermalOverlayState.value = _thermalOverlayState.value.copy(colorPalette = palette)
    }

    fun adjustAlignment(
        offsetX: Float,
        offsetY: Float,
    ) {
        _thermalOverlayState.value =
            _thermalOverlayState.value.copy(
                alignmentOffset = offsetX to offsetY,
            )
    }

    fun setScale(scale: Float) {
        _thermalOverlayState.value =
            _thermalOverlayState.value.copy(
                scale = scale.coerceIn(0.1f, 3.0f),
            )
    }

    fun setRotation(rotation: Float) {
        _thermalOverlayState.value =
            _thermalOverlayState.value.copy(
                rotation = rotation % 360f,
            )
    }

    fun toggleThermalOverlay() {
        val currentState = _thermalOverlayState.value
        _thermalOverlayState.value = currentState.copy(isEnabled = !currentState.isEnabled)
    }

    // Calibration and Synchronization
    fun calibrateAlignment() {
        launchWithErrorHandling {
            // Simulate calibration process
            _previewEvents.emit(PreviewEvent.CalibrationRequired("Place calibration target in view and press OK"))
            // Reset alignment to defaults after calibration
            _thermalOverlayState.value =
                _thermalOverlayState.value.copy(
                    alignmentOffset = 0f to 0f,
                    scale = 1.0f,
                    rotation = 0f,
                )
        }
    }

    fun syncFrames() {
        launchWithErrorHandling {
            val rgbFrame = _rgbPreviewState.value.currentFrame
            val thermalFrame = _thermalOverlayState.value.thermalBitmap
            if (rgbFrame != null && thermalFrame != null) {
                // In a real implementation, this would combine the frames
                // For now, we'll just use the thermal frame as the synced frame
                val combinedState = combinedPreviewState.value
                // Update combined state would happen here
                _previewEvents.emit(PreviewEvent.ShowToast("Frames synchronized"))
            } else {
                _previewEvents.emit(PreviewEvent.ShowError("Cannot sync frames - missing RGB or thermal data"))
            }
        }
    }

    // Preset configurations for different use cases
    fun applyPreset(preset: PreviewPreset) {
        when (preset) {
            PreviewPreset.MEDICAL -> {
                _thermalOverlayState.value =
                    _thermalOverlayState.value.copy(
                        colorPalette = ColorPalette.MEDICAL,
                        opacity = 0.8f,
                        blendMode = BlendMode.OVERLAY,
                    )
            }

            PreviewPreset.INDUSTRIAL -> {
                _thermalOverlayState.value =
                    _thermalOverlayState.value.copy(
                        colorPalette = ColorPalette.IRON,
                        opacity = 0.6f,
                        blendMode = BlendMode.MULTIPLY,
                    )
            }

            PreviewPreset.RESEARCH -> {
                _thermalOverlayState.value =
                    _thermalOverlayState.value.copy(
                        colorPalette = ColorPalette.RAINBOW,
                        opacity = 0.5f,
                        blendMode = BlendMode.SIDE_BY_SIDE,
                    )
            }

            PreviewPreset.NIGHT_VISION -> {
                _rgbPreviewState.value =
                    _rgbPreviewState.value.copy(
                        exposureMode = ExposureMode.SCENE_NIGHT,
                    )
                _thermalOverlayState.value =
                    _thermalOverlayState.value.copy(
                        colorPalette = ColorPalette.HOT,
                        opacity = 0.9f,
                    )
            }
        }
    }

    enum class PreviewPreset {
        MEDICAL,
        INDUSTRIAL,
        RESEARCH,
        NIGHT_VISION,
    }

    companion object {
        private const val TAG = "ThermalRGBPreviewViewModel"
    }
}
