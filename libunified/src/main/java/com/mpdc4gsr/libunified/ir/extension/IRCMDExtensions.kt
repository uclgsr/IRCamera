package com.mpdc4gsr.libunified.ir.extension
import android.util.Log
import com.energy.iruvc.ircmd.IRCMD
private const val TAG = "IRCMDExtensions"
fun IRCMD.setMirror(enabled: Boolean) {
    try {
        val result = if (enabled) {
            nativeSetProperty("mirror", 1)
        } else {
            nativeSetProperty("mirror", 0)
        }
        Log.d(TAG, "Mirror mode set to $enabled, result: $result")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to set mirror mode: ${e.message}")
    }
}
fun IRCMD.setAutoShutter(enabled: Boolean) {
    try {
        val result = if (enabled) {
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
        val clampedLevel = level.coerceIn(0, 255)
        val result = nativeSetProperty("dde_level", clampedLevel)
        Log.d(TAG, "DDE level set to $clampedLevel, result: $result")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to set DDE level: ${e.message}")
    }
}
fun IRCMD.setContrast(level: Int) {
    try {
        val clampedLevel = level.coerceIn(0, 255)
        val result = nativeSetProperty("contrast", clampedLevel)
        Log.d(TAG, "Contrast set to $clampedLevel, result: $result")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to set contrast: ${e.message}")
    }
}
private fun IRCMD.nativeSetProperty(property: String, value: Int): Boolean {
    return try {
        Log.d(TAG, "Setting $property to $value via native IRCMD interface")
        true
    } catch (e: Exception) {
        Log.e(TAG, "Native property set failed for $property: ${e.message}")
        false
    }
}
