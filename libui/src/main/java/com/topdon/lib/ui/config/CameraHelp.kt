package com.topdon.lib.ui.config

/**
 * 管理摄像头的property值
 * @author: CaiSongL
 * @date: 2023/4/4 9:57
 */

/**
 * Camera help utility class for thermal imaging operations.
 * Provides helper functions and common functionality.
 */
object CameraHelp {
    /**
     * pseudo color条
     */
    val TYPE_SET_PSEUDOCOLOR = 4

    /**
     * contrast
     */
    val TYPE_SET_ParamLevelContrast = 3

    /**
     * 锐度（细节）
     */
    val TYPE_SET_ParamLevelDde = 2

    /**
     * warning
     */
    val TYPE_SET_ALARM = 12 
    /**
     * 旋转
     */
    val TYPE_SET_ROTATE = 1

    /**
     * font
     */
    val TYPE_SET_COLOR = 13 
    /**
     * 镜像
     */
    val TYPE_SET_MIRROR = 14 
    /**
     * 仅 2D 编辑：watermark
     */
    val TYPE_SET_WATERMARK = 15 
    /**
     * 仅 TS001-observation：指南针
     */
    val TYPE_SET_COMPASS = 23 
    // TS001 -- calibrationmode
    val TYPE_SET_HIGHTEMP = 20 
    val TYPE_SET_LOWTEMP = 21 
    val TYPE_SET_DETELE = 22 
    // TS001 -- targetmenu
    val TYPE_SET_TARGET_MODE = 30 
    val TYPE_SET_TARGET_ZOOM = 31 
    val TYPE_SET_MEASURE_MODE = 32 // measurement mode
    val TYPE_SET_TARGET_COLOR = 33 
    val TYPE_SET_TARGET_DELETE = 34 
    val TYPE_SET_TARGET_HELP = 35 
}
