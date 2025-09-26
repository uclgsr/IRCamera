package com.mpdc4gsr.libunified.ir.utils

import com.mpdc4gsr.libunified.app.bean.ObserveBean

object TargetUtils {
    
    fun getMeasureSize(targetMeasureMode: Int): Float {
        var mMeasureSize = 180f
        when (targetMeasureMode) {
            ObserveBean.TYPE_MEASURE_PERSON -> mMeasureSize = 180f
            ObserveBean.TYPE_MEASURE_SHEEP -> mMeasureSize = 100f
            ObserveBean.TYPE_MEASURE_DOG -> mMeasureSize = 50f
            ObserveBean.TYPE_MEASURE_BIRD -> mMeasureSize = 20f
        }
        return mMeasureSize
    }

    fun isScaleMode(targetMeasureMode: Int): Boolean {
        return targetMeasureMode == ObserveBean.TYPE_MEASURE_DOG ||
                targetMeasureMode == ObserveBean.TYPE_MEASURE_BIRD
    }
    
    fun getSelectTargetDraw(targetMeasureMode: Int, targetType: Int, targetColorType: Int): Int {
        // Return drawable resource ID based on parameters
        // This is a placeholder - should return appropriate drawable resource ID
        return android.R.drawable.ic_menu_camera
    }
}