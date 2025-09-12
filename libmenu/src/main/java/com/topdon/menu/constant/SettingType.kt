package com.topdon.menu.constant

/**
 * Temperature measurement mode - Menu 5 - Settings/Observation mode - Menu 6 - Settings menu type.
 *
 * Created by LCG on 2024/11/28.
 */
enum class SettingType {
    /** Pseudo color bar */
    PSEUDO_BAR,

    /** Contrast */
    CONTRAST,

    /** Sharpness (detail) */
    DETAIL,

    /** Rotate */
    ROTATE,

    /** Mirror */
    MIRROR,

    /** Alert */
    ALARM,

    /** Font */
    FONT,

    /** Compass (observation mode only) */
    COMPASS,

    /** Watermark (2D editing only) */
    WATERMARK,
}