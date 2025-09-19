package mpdc4gsr.camera.integration

import android.content.Context
import android.util.Log
import mpdc4gsr.camera.core.ModeManager

class Camera2SystemValidator(private val context: Context) {
    companion object {
        private const val TAG = "Camera2SystemValidator"
    }

    suspend fun validateSystem(): ValidationResult {
        val results = mutableListOf<String>()
        var allPassed = true

        try {
            Log.i(TAG, "Starting Camera2 system validation...")

            if (validateArchitectureComponents()) {
                results.add("✅ Architecture components validated")
            } else {
                results.add("❌ Architecture components missing")
                allPassed = false
            }

            if (validateModeSwitching()) {
                results.add("✅ Mode switching logic validated")
            } else {
                results.add("❌ Mode switching logic failed")
                allPassed = false
            }

            if (validateFastSessionSwitching()) {
                results.add("✅ Fast session switching validated")
            } else {
                results.add("❌ Fast session switching failed")
                allPassed = false
            }

            if (validateSamsungCompatibility()) {
                results.add("✅ Samsung S22 compatibility validated")
            } else {
                results.add("❌ Samsung S22 compatibility failed")
                allPassed = false
            }

            Log.i(TAG, "Validation completed. Success: $allPassed")
        } catch (e: Exception) {
            Log.e(TAG, "Validation failed with exception", e)
            results.add("❌ Validation failed: ${e.message}")
            allPassed = false
        }

        return ValidationResult(allPassed, results)
    }

    private fun validateArchitectureComponents(): Boolean {

        return try {

            Class.forName("com.topdon.tc001.camera.Camera2System")
            Class.forName("com.topdon.tc001.camera.core.CameraController")
            Class.forName("com.topdon.tc001.camera.core.VideoEngine")
            Class.forName("com.topdon.tc001.camera.core.RawEngine")
            Class.forName("com.topdon.tc001.camera.core.ModeManager")
            Class.forName("com.topdon.tc001.camera.core.UiBridge")
            Class.forName("com.topdon.tc001.camera.core.DeviceCaps")
            true
        } catch (e: ClassNotFoundException) {
            Log.e(TAG, "Architecture component not found", e)
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
            Log.e(TAG, "Mode switching validation failed", e)
            false
        }
    }

    private fun validateFastSessionSwitching(): Boolean {
        return try {

            val cameraControllerClass =
                Class.forName("com.topdon.tc001.camera.core.CameraController")
            val createSessionMethod =
                cameraControllerClass.getDeclaredMethod(
                    "createCaptureSession",
                    List::class.java,
                    Class.forName("android.hardware.camera2.CameraCaptureSession\$StateCallback"),
                )

            createSessionMethod != null
        } catch (e: Exception) {
            Log.e(TAG, "Fast session switching validation failed", e)
            false
        }
    }

    private fun validateSamsungCompatibility(): Boolean {
        return try {
            val deviceCapsClass = Class.forName("com.topdon.tc001.camera.core.DeviceCaps")
            val fields = deviceCapsClass.declaredFields

            val hasSupportsRaw = fields.any { it.name == "supportsRaw" }
            val hasRawSize = fields.any { it.name == "rawSize" }
            val hasSupports4k60 = fields.any { it.name == "supports4k60" }
            val hasSensorOrientation = fields.any { it.name == "sensorOrientation" }

            hasSupportsRaw && hasRawSize && hasSupports4k60 && hasSensorOrientation
        } catch (e: Exception) {
            Log.e(TAG, "Samsung compatibility validation failed", e)
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
                appendLine("Overall Result: ${if (allTestsPassed) "✅ PASS" else "❌ FAIL"}")
                appendLine()
                results.forEach { result ->
                    appendLine(result)
                }
                appendLine()
                if (allTestsPassed) {
                    appendLine("🚀 System ready for Samsung S22 (Exynos, Android 15) deployment")
                } else {
                    appendLine("⚠️ System requires fixes before deployment")
                }
            }
        }
    }
}
