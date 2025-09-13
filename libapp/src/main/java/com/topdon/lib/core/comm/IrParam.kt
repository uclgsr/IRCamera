package com.topdon.lib.core.comm

/**
 * des:
 * author: CaiSongL
 * date: 2024/4/30 10:16
 **/
/**
 * IrParam manages camera operations and image capture functionality.
 *
 * @author IRCamera Development Team
 * @since 1.0
 */
enum class IrParam {
    ParamLevel, 
    ParamAlarm, 
    ParamSharpness, 
    ParamTempFont, 
    ParamRotate, 
    ParamColor, // pseudo color
    ParamMirror, 
    ParamCompass, 
    ParamPColor, // pseudo color样式
    ParamTemperature, // temperaturemode、高低gain
}

data class TempFont(val textSize: Int, val textColor: Int)
