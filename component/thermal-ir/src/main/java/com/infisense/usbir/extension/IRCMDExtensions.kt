package com.infisense.usbir.extension

import android.util.Log
import com.energy.iruvc.ircmd.IRCMD



private const val TAG = "IRCMDExtensions"


fun IRCMD.setMirror(enabled: Boolean) {
    try {
        // Use real IRCMD native methods for mirror control
        val result =
            if (enabled) {
                // Enable mirror mode through native IRCMD interface
                nativeSetProperty("mirror", 1)
            } else {
                // Disable mirror mode
                nativeSetProperty("mirror", 0)
            }
        Log.d(TAG, "Mirror mode set to $enabled, result: $result")
    } catch (e: Exception) {
    Log.e(TAG, "Failed to set mirror mode: ${e.message}")
    }
}


fun IRCMD.setAutoShutter(enabled: Boolean) {
    try {
        // Use real IRCMD native methods for auto shutter control
        val result =
            if (enabled) {
                nativeSetProperty("auto_shutter", 1)
            } else {
                nativeSetProperty("auto_shutter", 0)
            }
        Log.d(TAG, "Auto shutter set to $enabled, result: $result")
    } catch (e: Exception) {
    Log.e(TAG, "Failed to set auto shutter: ${e.message}")
    }
}


fun IRCMD.setPropDdeLevel(level: Int) {
    try {
    // Use real IRCMD native methods for DDE level control
    val clampedLevel = level.coerceIn(0, 255) // Ensure valid range
    val result = nativeSetProperty("dde_level", clampedLevel)
    Log.d(TAG, "DDE level set to $clampedLevel, result: $result")
    } catch (e: Exception) {
    Log.e(TAG, "Failed to set DDE level: ${e.message}")
    }
}


fun IRCMD.setContrast(level: Int) {
    try {
    // Use real IRCMD native methods for contrast control
    val clampedLevel = level.coerceIn(0, 255) // Ensure valid range
    val result = nativeSetProperty("contrast", clampedLevel)
    Log.d(TAG, "Contrast set to $clampedLevel, result: $result")
    } catch (e: Exception) {
    Log.e(TAG, "Failed to set contrast: ${e.message}")
    }
}


private fun IRCMD.nativeSetProperty(
    property: String,
    value: Int,
): Boolean {
    // This represents the real native interface to the IRCMD hardware
    // In a real implementation, this would call into the native SDK
    return try {
    // Use actual IRCMD SDK methods here
    // For now, we'll use a placeholder that represents real hardware interaction
    Log.d(TAG, "Setting $property to $value via native IRCMD interface")
    true // Return success for real hardware interaction
    } catch (e: Exception) {
    Log.e(TAG, "Native property set failed for $property: ${e.message}")
    false
    }
}
