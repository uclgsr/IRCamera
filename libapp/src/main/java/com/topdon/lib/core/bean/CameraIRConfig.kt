package com.topdon.lib.core.bean


data class ContinuousBean(var isOpen: Boolean = false, var continuaTime: Long = 1000, var count: Int = 3)

class ObserveBean {
    companion object {
        // 动态追踪
        const val TYPE_NONE = -1 // 不开启dynamic recognition
        const val TYPE_DYN_R = 0 // dynamic recognition
        const val TYPE_TMP_H_S = 1 // high temperature source
        const val TYPE_TMP_L_S = 2 // low temperature source

        const val TYPE_MEASURE_PERSON = 10 // human
        const val TYPE_MEASURE_SHEEP = 11 // sheep
        const val TYPE_MEASURE_DOG = 12 // dog
        const val TYPE_MEASURE_BIRD = 13 // bird

    const val TYPE_TARGET_HORIZONTAL = 15 // 横向
    const val TYPE_TARGET_VERTICAL = 16 // 竖向
    const val TYPE_TARGET_CIRCLE = 17 // 圆形

    const val TYPE_TARGET_COLOR_GREEN = 20 // 绿色
    const val TYPE_TARGET_COLOR_RED = 21 // 红色
    const val TYPE_TARGET_COLOR_BLUE = 22 // 蓝色
    const val TYPE_TARGET_COLOR_BLACK = 23 // 黑色
    const val TYPE_TARGET_COLOR_WHITE = 24 // 白色
    }
}

data class CameraItemBean(
    var name: String = "delayed",
    var type: Int = 0,
    var time: Int = DELAY_TIME_0,
    var isSel: Boolean = false,
) {
    fun changeDelayType() {
    if (type == TYPE_DELAY) {
    when (time) {
    DELAY_TIME_0 -> {
    time = DELAY_TIME_3
    }

    DELAY_TIME_3 -> {
    time = DELAY_TIME_6
    }

    DELAY_TIME_6 -> {
    time = DELAY_TIME_0
    }
    }
    }
    }

    companion object {
    const val TYPE_DELAY = 0
    const val TYPE_ZDKM = 1
    const val TYPE_SDKM = 2
    const val TYPE_AUDIO = 3
    const val TYPE_SETTING = 4

        const val DELAY_TIME_0 = 0 // delayed3秒
        const val DELAY_TIME_3 = 3 // delayed3秒
        const val DELAY_TIME_6 = 6 // delayed6秒

        // 温度模式
        const val TYPE_TMP_ZD = -1 // 自动识别模式
        const val TYPE_TMP_C = 1 // normal temperature模式
        const val TYPE_TMP_H = 0 // 高温模式
    }
}
