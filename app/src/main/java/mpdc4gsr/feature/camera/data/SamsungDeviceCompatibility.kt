package mpdc4gsr.feature.camera.data

import android.os.Build
import java.util.Locale

/**
 * Encapsulates the set of Samsung-specific capability checks that we rely on for
 * Stage 3 / Level 3 RAW capture and high-end recording pathways.
 */
object SamsungDeviceCompatibility {
    private val stage3Models =
        setOf(
            "SM-S906B", // S22+
            "SM-S916B", // S23+
            "SM-S908B", // S22 Ultra
            "SM-S901B", // S22
            "SM-S911B", // S23
            "SM-S918B", // S23 Ultra
            "SM-S928B", // S24 Ultra
        )

    private val highFrameRateModels =
        stage3Models +
            setOf(
                "SM-G998B", // S21 Ultra
                "SM-G996B", // S21+
                "SM-G991B", // S21
            )

    fun getDeviceInfo(): String =
        buildString {
            append(Build.MANUFACTURER).append(' ')
            append(Build.MODEL)
            append(" (Android ").append(Build.VERSION.RELEASE).append(')')
        }

    fun isStage3Compatible(
        model: String = Build.MODEL,
        manufacturer: String = Build.MANUFACTURER,
    ): Boolean {
        if (!manufacturer.equals("samsung", ignoreCase = true)) {
            return false
        }
        val normalisedModel = model.uppercase(Locale.US)
        return normalisedModel in stage3Models
    }

    fun supportsRawCapture(): Boolean = isStage3Compatible()

    fun supports4kVideo(
        model: String = Build.MODEL,
        manufacturer: String = Build.MANUFACTURER,
    ): Boolean {
        if (!manufacturer.equals("samsung", ignoreCase = true)) {
            return false
        }
        val normalisedModel = model.uppercase(Locale.US)
        return normalisedModel in stage3Models ||
            normalisedModel.startsWith("SM-S9") ||
            normalisedModel.startsWith("SM-G99")
    }

    fun supportsHighFrameRateVideo(
        model: String = Build.MODEL,
        manufacturer: String = Build.MANUFACTURER,
    ): Boolean {
        if (!manufacturer.equals("samsung", ignoreCase = true)) {
            return false
        }
        val normalisedModel = model.uppercase(Locale.US)
        return normalisedModel in highFrameRateModels ||
            normalisedModel.startsWith("SM-S91") ||
            normalisedModel.startsWith("SM-S92")
    }

    fun isSamsungDevice(): Boolean = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
}
