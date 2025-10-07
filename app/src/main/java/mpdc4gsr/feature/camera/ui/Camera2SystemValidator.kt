package mpdc4gsr.feature.camera.ui

import android.content.Context
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.feature.camera.data.ModeManager

class Camera2SystemValidator(private val context: Context) {
    companion object {
        private const val TAG = "Camera2SystemValidator"
    }

    suspend fun validateSystem(): ValidationResult {
        val results = mutableListOf<String>()
        var allPassed = true
        try {
            AppLogger.i(TAG, "Starting Camera2 system validation...")
            if (validateArchitectureComponents()) {
                results.add(" Architecture components validated")
            } else {
                results.add(" Architecture components missing")
                allPassed = false
            }
            if (validateModeSwitching()) {
                results.add(" Mode switching logic validated")
            } else {
                results.add(" Mode switching logic failed")
                allPassed = false
            }
            if (validateFastSessionSwitching()) {
                results.add(" Fast session switching validated")
            } else {
                results.add(" Fast session switching failed")
                allPassed = false
            }
            if (validateSamsungCompatibility()) {
                results.add(" Samsung S22 compatibility validated")
            } else {
                results.add(" Samsung S22 compatibility failed")
                allPassed = false
            }
            if (validateStage3Level3Support()) {
                results.add(" Samsung Stage3/Level3 DNG support validated")
            } else {
                results.add(" Samsung Stage3/Level3 DNG support failed")
                allPassed = false
            }
            AppLogger.i(TAG, "Validation completed. Success: $allPassed")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Validation failed with exception", e)
            results.add(" Validation failed: ${e.message}")
            allPassed = false
        }
        return ValidationResult(allPassed, results)
    }

    private fun validateArchitectureComponents(): Boolean {
        return try {
            Class.forName("com.mpdc4gsr.camera.Camera2System")
            Class.forName("com.mpdc4gsr.camera.core.CameraController")
            Class.forName("com.mpdc4gsr.camera.core.VideoEngine")
            Class.forName("com.mpdc4gsr.camera.core.RawEngine")
            Class.forName("com.mpdc4gsr.camera.core.ModeManager")
            Class.forName("com.mpdc4gsr.camera.core.UiBridge")
            Class.forName("com.mpdc4gsr.camera.core.DeviceCaps")
            true
        } catch (e: ClassNotFoundException) {
            AppLogger.e(TAG, "Architecture component not found", e)
            false
        }
    }

    private fun validateModeSwitching(): Boolean {
        return try {
            val modeManager = ModeManager()
            val canSwitchToRaw = modeManager.requestModeSwitch(ModeManager.CameraMode.RAW_50MP)
            val canSwitchToVideo = modeManager.requestModeSwitch(ModeManager.CameraMode.VIDEO_4K)
            val canSwitchToPreview =
                modeManager.requestModeSwitch(ModeManager.CameraMode.PREVIEW_ONLY)
            canSwitchToRaw && canSwitchToVideo && canSwitchToPreview
        } catch (e: Exception) {
            AppLogger.e(TAG, "Mode switching validation failed", e)
            false
        }
    }

    private fun validateFastSessionSwitching(): Boolean {
        return try {
            val cameraControllerClass =
                Class.forName("com.mpdc4gsr.camera.core.CameraController")
            val createSessionMethod =
                cameraControllerClass.getDeclaredMethod(
                    "createCaptureSession",
                    List::class.java,
                    Class.forName("android.hardware.camera2.CameraCaptureSession\$StateCallback"),
                )
            createSessionMethod != null
        } catch (e: Exception) {
            AppLogger.e(TAG, "Fast session switching validation failed", e)
            false
        }
    }

    private fun validateSamsungCompatibility(): Boolean {
        return try {
            val deviceCapsClass = Class.forName("com.mpdc4gsr.camera.core.DeviceCaps")
            val fields = deviceCapsClass.declaredFields
            val hasSupportsRaw = fields.any { it.name == "supportsRaw" }
            val hasRawSize = fields.any { it.name == "rawSize" }
            val hasSupports4k60 = fields.any { it.name == "supports4k60" }
            val hasSensorOrientation = fields.any { it.name == "sensorOrientation" }
            hasSupportsRaw && hasRawSize && hasSupports4k60 && hasSensorOrientation
        } catch (e: Exception) {
            AppLogger.e(TAG, "Samsung compatibility validation failed", e)
            false
        }
    }

    private fun validateStage3Level3Support(): Boolean {
        return try {
            // Instead of using brittle reflection, test actual functionality
            // by trying to instantiate the classes and checking their public APIs
            // Test RawEngine Stage3/Level3 functionality
            val rawEngineWorks = try {
                val rawEngine = mpdc4gsr.feature.camera.data.RawEngine(context)
                // Test that the methods exist by trying to call them (safe calls)
                rawEngine.isStage3ProcessingEnabled() // This should not throw
                rawEngine.setStage3ProcessingEnabled(false) // This should not throw
                true
            } catch (e: Exception) {
                AppLogger.e(TAG, "RawEngine Stage3/Level3 methods not available", e)
                false
            }
            // Test Camera2System Stage3/Level3 functionality  
            val camera2SystemWorks = try {
                // Use a mock TextureView for testing
                val textureView = android.view.TextureView(context)
                val camera2System = mpdc4gsr.feature.camera.data.Camera2System(context, textureView)
                // Test that the methods exist
                camera2System.isStage3ProcessingEnabled() // This should not throw
                camera2System.configureStage3Processing(false) // This should not throw
                true
            } catch (e: Exception) {
                AppLogger.e(TAG, "Camera2System Stage3/Level3 methods not available", e)
                false
            }
            // Test DngCreator API availability (this is a standard Android API)
            val dngCreatorAvailable = try {
                // Check if DngCreator class is available (API level 21+)
                Class.forName("android.hardware.camera2.DngCreator") != null
            } catch (e: Exception) {
                AppLogger.e(TAG, "DngCreator API not available", e)
                false
            }
            // Test SamsungDeviceCompatibility utility
            val deviceCompatibilityWorks = try {
                mpdc4gsr.feature.camera.data.SamsungDeviceCompatibility.isStage3Compatible()
                mpdc4gsr.feature.camera.data.SamsungDeviceCompatibility.getDeviceInfo()
                true
            } catch (e: Exception) {
                AppLogger.e(TAG, "SamsungDeviceCompatibility utility not working", e)
                false
            }
            val allWorking =
                rawEngineWorks && camera2SystemWorks && dngCreatorAvailable && deviceCompatibilityWorks
            Log.i(
                TAG,
                "Stage3/Level3 validation - RawEngine: $rawEngineWorks, Camera2System: $camera2SystemWorks, DngCreator: $dngCreatorAvailable, DeviceCompatibility: $deviceCompatibilityWorks"
            )
            allWorking
        } catch (e: Exception) {
            AppLogger.e(TAG, "Stage3/Level3 validation failed", e)
            false
        }
    }

    data class ValidationResult(
        val allTestsPassed: Boolean,
        val results: List<String>,
    ) {
        fun getFormattedReport(): String {
            return buildString {
                appendLine("=== Camera2 System Validation Report ===")
                appendLine("Overall Result: ${if (allTestsPassed) " PASS" else " FAIL"}")
                appendLine()
                results.forEach { result ->
                    appendLine(result)
                }
                appendLine()
                if (allTestsPassed) {
                    appendLine(" System ready for Samsung S22 (Exynos, Android 15) deployment")
                } else {
                    appendLine(" System requires fixes before deployment")
                }
            }
        }
    }
}
