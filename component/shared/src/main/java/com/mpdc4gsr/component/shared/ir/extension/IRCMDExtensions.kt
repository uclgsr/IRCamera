package com.mpdc4gsr.component.shared.ir.extension

import com.energy.iruvc.ircmd.IRCMD
import com.energy.iruvc.utils.CommonParams

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
        when (property) {
            "mirror" ->
                succeed(
                    setPropImageParams(
                        CommonParams.PropImageParams.IMAGE_PROP_SEL_MIRROR_FLIP,
                        mirrorModeForValue(value),
                    ),
                )

            "auto_shutter" ->
                succeed(
                    setPropAutoShutterParameter(
                        CommonParams.PropAutoShutterParameter.SHUTTER_PROP_SWITCH,
                        if (value != 0) {
                            CommonParams.PropAutoShutterParameterValue.StatusSwith.ON
                        } else {
                            CommonParams.PropAutoShutterParameterValue.StatusSwith.OFF
                        },
                    ),
                )

            "dde_level" ->
                succeed(
                    setPropImageParams(
                        CommonParams.PropImageParams.IMAGE_PROP_LEVEL_DDE,
                        imageNumberValue(value),
                    ),
                )

            "contrast" ->
                succeed(
                    setPropImageParams(
                        CommonParams.PropImageParams.IMAGE_PROP_LEVEL_CONTRAST,
                        imageNumberValue(value),
                    ),
                )

            "brightness" ->
                succeed(
                    setPropImageParams(
                        CommonParams.PropImageParams.IMAGE_PROP_LEVEL_BRIGHTNESS,
                        imageNumberValue(value),
                    ),
                )

            "sharpness" ->
                succeed(
                    setPropImageParams(
                        CommonParams.PropImageParams.IMAGE_PROP_LEVEL_DDE_STR,
                        imageNumberValue(value),
                    ),
                )

            "gamma" ->
                succeed(
                    setPropImageParams(
                        CommonParams.PropImageParams.IMAGE_PROP_LEVEL_SNR,
                        imageNumberValue(value),
                    ),
                )

            "pseudo_color" ->
                succeed(setPseudoColor(pseudoColorForValue(value)))

            "agc_mode" ->
                succeed(
                    setPropImageParams(
                        CommonParams.PropImageParams.IMAGE_PROP_MODE_AGC,
                        agcModeForValue(value),
                    ),
                )

            "emissivity" ->
                succeed(
                    setPropTPDParams(
                        CommonParams.PropTPDParams.TPD_PROP_EMS,
                        tpdNumberValue(value),
                    ),
                )

            "distance" ->
                succeed(
                    setPropTPDParams(
                        CommonParams.PropTPDParams.TPD_PROP_DISTANCE,
                        tpdNumberValue(value),
                    ),
                )

            "reflected_temp" ->
                succeed(
                    setPropTPDParams(
                        CommonParams.PropTPDParams.TPD_PROP_TU,
                        tpdNumberValue(value),
                    ),
                )

            "isp_enable" ->
                succeed(
                    setPropSelfAdaptionEn(
                        if (value != 0) {
                            CommonParams.StatusSwitch.ON
                        } else {
                            CommonParams.StatusSwitch.OFF
                        },
                    ),
                )

            "tnr_level" ->
                succeed(
                    setPropImageParams(
                        CommonParams.PropImageParams.IMAGE_PROP_LEVEL_TNR,
                        imageNumberValue(value),
                    ),
                )

            "ffc_trigger" ->
                succeed(shutterUpdate())

            "nuc_trigger" ->
                succeed(updateOOCOrB(CommonParams.UpdateOOCOrBType.OOC_UPDATE))

            "agc_manual_min" ->
                succeed(
                    setPropImageParams(
                        CommonParams.PropImageParams.IMAGE_PROP_LEVEL_BOS,
                        imageNumberValue(value),
                    ),
                )

            "agc_manual_max" ->
                succeed(
                    setPropImageParams(
                        CommonParams.PropImageParams.IMAGE_PROP_LEVEL_MAX_GAIN,
                        imageNumberValue(value),
                    ),
                )

            else -> false
        }
    } catch (e: Exception) {
        false
    }

private fun mirrorModeForValue(value: Int): CommonParams.PropImageParamsValue.MirrorFlipType =
    CommonParams.PropImageParamsValue.MirrorFlipType
        .values()
        .firstOrNull { it.value == value }
        ?: when (value) {
            1 -> CommonParams.PropImageParamsValue.MirrorFlipType.ONLY_MIRROR
            2 -> CommonParams.PropImageParamsValue.MirrorFlipType.ONLY_FLIP
            3 -> CommonParams.PropImageParamsValue.MirrorFlipType.MIRROR_FLIP
            else -> CommonParams.PropImageParamsValue.MirrorFlipType.NO_MIRROR_FLIP
        }

private fun pseudoColorForValue(value: Int): CommonParams.PseudoColorType =
    CommonParams.PseudoColorType
        .values()
        .firstOrNull { it.value == value }
        ?: CommonParams.PseudoColorType.PSEUDO_1

private fun agcModeForValue(value: Int): CommonParams.PropImageParamsValue.AGCType =
    CommonParams.PropImageParamsValue.AGCType
        .values()
        .firstOrNull { it.value == value }
        ?: CommonParams.PropImageParamsValue.AGCType.AGC_0

private fun imageNumberValue(value: Int) = CommonParams.PropImageParamsValue.NumberType(value.toString())

private fun tpdNumberValue(value: Int) = CommonParams.PropTPDParamsValue.NumberType(value.toString())

private fun succeed(result: Int) = result == 0


