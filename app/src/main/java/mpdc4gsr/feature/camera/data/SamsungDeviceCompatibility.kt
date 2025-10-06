package mpdc4gsr.feature.camera.data

import android.os.Build

object SamsungDeviceCompatibility {

    private val STAGE3_COMPATIBLE_MODELS = setOf(
        "SM-S906B", // Galaxy S22+
        "SM-S916B", // Galaxy S23+
        "SM-S908B", // Galaxy S22 Ultra
        "SM-S901B", // Galaxy S22
        "SM-S911B", // Galaxy S23
        "SM-S918B"  // Galaxy S23 Ultra
    )

    fun isStage3Compatible(): Boolean {
        val deviceModel = Build.MODEL
        val deviceManufacturer = Build.MANUFACTURER
        // Check exact model match first
        if (STAGE3_COMPATIBLE_MODELS.contains(deviceModel)) {
            return true
        }
        // Check broader Samsung Galaxy S22/S23 series compatibility
        return deviceManufacturer.equals("samsung", ignoreCase = true) &&
                (deviceModel.contains("SM-S9", ignoreCase = true) ||
                        deviceModel.contains("SM-S22", ignoreCase = true))
    }

    fun getDeviceInfo(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }

    fun isSamsungDevice(): Boolean {
        return Build.MANUFACTURER.equals("samsung", ignoreCase = true)
    }
}