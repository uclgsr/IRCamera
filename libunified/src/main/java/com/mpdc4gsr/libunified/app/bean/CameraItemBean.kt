package com.mpdc4gsr.libunified.app.bean

data class CameraItemBean(
    var name: String = "延迟",
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

        const val DELAY_TIME_0 = 0//延迟3秒
        const val DELAY_TIME_3 = 3//延迟3秒
        const val DELAY_TIME_6 = 6//延迟6秒

        //温度模式
        const val TYPE_TMP_ZD = -1 //自动识别模式
        const val TYPE_TMP_C = 1 // 常温模式
        const val TYPE_TMP_H = 0 //高温模式

    }

}