package com.mpdc4gsr.libunified.app.bean

data class ContinuousBean(
    var isOpen: Boolean = false,
    var continuaTime: Long = 1000,
    var count: Int = 3
)

class ObserveBean {
    companion object {

        const val TYPE_NONE = -1
        const val TYPE_DYN_R = 0
        const val TYPE_TMP_H_S = 1
        const val TYPE_TMP_L_S = 2

        const val TYPE_MEASURE_PERSON = 10
        const val TYPE_MEASURE_SHEEP = 11
        const val TYPE_MEASURE_DOG = 12
        const val TYPE_MEASURE_BIRD = 13

        const val TYPE_TARGET_HORIZONTAL = 15
        const val TYPE_TARGET_VERTICAL = 16
        const val TYPE_TARGET_CIRCLE = 17

        const val TYPE_TARGET_COLOR_GREEN = 20
        const val TYPE_TARGET_COLOR_RED = 21
        const val TYPE_TARGET_COLOR_BLUE = 22
        const val TYPE_TARGET_COLOR_WHITE = 23
        const val TYPE_TARGET_COLOR_BLACK = 24
        const val TYPE_TARGET_COLOR_ORANGE = 25
        const val TYPE_TARGET_COLOR_YELLOW = 26
        const val TYPE_TARGET_COLOR_PURPLE = 27
        const val TYPE_TARGET_COLOR_PINK = 28
        const val TYPE_TARGET_COLOR_GRAY = 29

        const val TYPE_TARGET_AREA = 30
        const val TYPE_TARGET_LINE = 31
        const val TYPE_TARGET_SPOT = 32
    }

    var observeType: Int = TYPE_NONE
    var observeX: Float = 0f
    var observeY: Float = 0f
    var observeWidth: Float = 0f
    var observeHeight: Float = 0f
    var maxTemp: Float = 0f
    var minTemp: Float = 0f
    var avgTemp: Float = 0f
    var isSelect: Boolean = false
    var colorType: Int = TYPE_TARGET_COLOR_WHITE
}

data class CameraItemBean(
    var key: String = "",
    var value: String = "",
    var type: Int = 0,
    var selected: Boolean = false
)