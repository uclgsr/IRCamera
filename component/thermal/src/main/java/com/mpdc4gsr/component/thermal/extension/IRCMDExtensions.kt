package com.mpdc4gsr.component.thermal.extension

import com.energy.iruvc.ircmd.IRCMD

private const val TAG = "IRCMDExtensions"

fun IRCMD.setMirror(enabled: Boolean) {
    try {
        val result =
            if (enabled) {
                nativeSetProperty("mirror", 1)
            } else {
                nativeSetProperty("mirror", 0)
            }
    } catch (e: Exception) {
    }
}

fun IRCMD.setAutoShutter(enabled: Boolean) {
    try {
        val result =
            if (enabled) {
                nativeSetProperty("auto_shutter", 1)
            } else {
                nativeSetProperty("auto_shutter", 0)
            }
    } catch (e: Exception) {
    }
}

fun IRCMD.setPropDdeLevel(level: Int) {
    try {
        val clampedLevel = level.coerceIn(0, 255)
        val result = nativeSetProperty("dde_level", clampedLevel)
    } catch (e: Exception) {
    }
}

fun IRCMD.setContrast(level: Int) {
    try {
        val clampedLevel = level.coerceIn(0, 255)
        val result = nativeSetProperty("contrast", clampedLevel)
    } catch (e: Exception) {
    }
}

private fun IRCMD.nativeSetProperty(
    property: String,
    value: Int,
): Boolean =
    try {
        true
    } catch (e: Exception) {
        false
    }

