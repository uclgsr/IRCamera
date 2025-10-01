package mpdc4gsr.feature.camera.data

import android.os.Build

/**
 * Utility class for Samsung device compatibility checks
 * Centralizes device model checking logic for Stage3/Level3 processing
 */
object SamsungDeviceCompatibility {

    /**
     * Device models that support Samsung Stage3/Level3 RAW DNG processing
     */
    private val STAGE3_COMPATIBLE_MODELS = setOf(
        "SM-S906B", // Galaxy S22+
        "SM-S916B", // Galaxy S23+
        "SM-S908B", // Galaxy S22 Ultra
        "SM-S901B", // Galaxy S22
        "SM-S911B", // Galaxy S23
        "SM-S918B"  // Galaxy S23 Ultra
    )

    /**
     * Check if the current device supports Samsung Stage3/Level3 processing
     * @return true if device supports Stage3/Level3 processing
     */
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

    /**
     * Get device information string for logging
     * @return formatted device info string
     */
    fun getDeviceInfo(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }

    /**
     * Check if device is Samsung (regardless of Stage3/Level3 support)
     * @return true if device is Samsung
     */
    fun isSamsungDevice(): Boolean {
        return Build.MANUFACTURER.equals("samsung", ignoreCase = true)
    }
}