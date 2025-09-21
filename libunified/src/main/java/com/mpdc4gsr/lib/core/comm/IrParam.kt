package com.mpdc4gsr.lib.core.comm


enum class IrParam {
    ParamLevel,
    ParamAlarm,
    ParamSharpness,
    ParamTempFont,
    ParamRotate,
    ParamColor,
    ParamMirror,
    ParamCompass,
    ParamPColor,
    ParamTemperature,
}

data class TempFont(val textSize: Int, val textColor: Int)
