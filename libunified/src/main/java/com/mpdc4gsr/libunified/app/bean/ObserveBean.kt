package com.mpdc4gsr.libunified.app.bean

class ObserveBean {
    companion object {
        //动态追踪
        const val TYPE_NONE = -1 //不开启动态识别
        const val TYPE_DYN_R = 0 //动态识别
        const val TYPE_TMP_H_S = 1 //高温源
        const val TYPE_TMP_L_S = 2 //低温源

        const val TYPE_MEASURE_PERSON = 10 //人
        const val TYPE_MEASURE_SHEEP = 11 //羊
        const val TYPE_MEASURE_DOG = 12 //狗
        const val TYPE_MEASURE_BIRD = 13 //鸟

        const val TYPE_TARGET_HORIZONTAL = 15 //横向
        const val TYPE_TARGET_VERTICAL = 16 //竖向
        const val TYPE_TARGET_CIRCLE = 17 //圆形

        const val TYPE_TARGET_COLOR_GREEN = 20 //绿色
        const val TYPE_TARGET_COLOR_RED = 21 //红色
        const val TYPE_TARGET_COLOR_BLUE = 22 //蓝色
        const val TYPE_TARGET_COLOR_BLACK = 23 //黑色
        const val TYPE_TARGET_COLOR_WHITE = 24 //白色
    }
}