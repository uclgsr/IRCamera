package com.mpdc4gsr.libunified.ir.extension

import com.energy.iruvc.ircmd.IRCMD

enum class ColorPalette(
    val value: Int,
) {
    IRONBOW(0),
    RAINBOW(1),
    WHITEHOT(2),
    BLACKHOT(3),
    LAVA(4),
    ARCTIC(5),
    MEDICAL(6),
    AMBER(7),
    GLOWBOW(8),
    INSTALERT(9),
    GRADED_FIRE(10),
}

enum class AgcMode(
    val value: Int,
) {
    AUTO(0),
    MANUAL(1),
    LINEAR(2),
    HISTOGRAM(3),
}

fun IRCMD.setMirror(enabled: Boolean) {
    nativeSetProperty("mirror", if (enabled) 1 else 0)
}

fun IRCMD.setAutoShutter(enabled: Boolean) {
    nativeSetProperty("auto_shutter", if (enabled) 1 else 0)
}

fun IRCMD.setPropDdeLevel(level: Int) {
    nativeSetProperty("dde_level", level.coerceIn(0, 255))
}

fun IRCMD.setContrast(level: Int) {
    nativeSetProperty("contrast", level.coerceIn(0, 255))
}

fun IRCMD.setBrightness(level: Int) {
    nativeSetProperty("brightness", level.coerceIn(0, 255))
}

fun IRCMD.setSharpness(level: Int) {
    nativeSetProperty("sharpness", level.coerceIn(0, 255))
}

fun IRCMD.setGamma(level: Int) {
    nativeSetProperty("gamma", level.coerceIn(0, 255))
}

fun IRCMD.setColorPalette(palette: ColorPalette) {
    nativeSetProperty("pseudo_color", palette.value)
}

fun IRCMD.setAgcMode(mode: AgcMode) {
    nativeSetProperty("agc_mode", mode.value)
}

fun IRCMD.setEmissivity(value: Float) {
    val emissivityInt = (value.coerceIn(0.01f, 1.0f) * 100).toInt()
    nativeSetProperty("emissivity", emissivityInt)
}

fun IRCMD.setDistance(meters: Float) {
    val distanceCm = (meters.coerceIn(0.1f, 100.0f) * 100).toInt()
    nativeSetProperty("distance", distanceCm)
}

fun IRCMD.setReflectedTemperature(tempCelsius: Float) {
    val tempInt = (tempCelsius.coerceIn(-40f, 100f) * 10).toInt()
    nativeSetProperty("reflected_temp", tempInt)
}

fun IRCMD.enableISP(enabled: Boolean) {
    nativeSetProperty("isp_enable", if (enabled) 1 else 0)
}

fun IRCMD.setTNRLevel(level: Int) {
    nativeSetProperty("tnr_level", level.coerceIn(0, 10))
}

fun IRCMD.performFFC() {
    nativeSetProperty("ffc_trigger", 1)
}

fun IRCMD.performNUC() {
    nativeSetProperty("nuc_trigger", 1)
}

fun IRCMD.setManualAgcMin(tempCelsius: Float) {
    val tempInt = (tempCelsius * 10).toInt()
    nativeSetProperty("agc_manual_min", tempInt)
}

fun IRCMD.setManualAgcMax(tempCelsius: Float) {
    val tempInt = (tempCelsius * 10).toInt()
    nativeSetProperty("agc_manual_max", tempInt)
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
