package com.mpdc4gsr.libunified.app.utils

import android.graphics.PointF
import android.graphics.RectF
import com.mpdc4gsr.libunified.app.bean.ObserveBean

/**
 * Utilities for target calculation and manipulation
 */
object TargetUtils {

    fun calculateTargetBounds(observeBean: ObserveBean): RectF {
        return RectF(
            observeBean.observeX,
            observeBean.observeY,
            observeBean.observeX + observeBean.observeWidth,
            observeBean.observeY + observeBean.observeHeight
        )
    }

    fun isPointInTarget(x: Float, y: Float, observeBean: ObserveBean): Boolean {
        val bounds = calculateTargetBounds(observeBean)
        return bounds.contains(x, y)
    }

    fun calculateTargetCenter(observeBean: ObserveBean): PointF {
        return PointF(
            observeBean.observeX + observeBean.observeWidth / 2,
            observeBean.observeY + observeBean.observeHeight / 2
        )
    }

    fun calculateTargetArea(observeBean: ObserveBean): Float {
        return observeBean.observeWidth * observeBean.observeHeight
    }

    fun updateTargetTemperature(observeBean: ObserveBean, maxTemp: Float, minTemp: Float, avgTemp: Float) {
        observeBean.maxTemp = maxTemp
        observeBean.minTemp = minTemp
        observeBean.avgTemp = avgTemp
    }

    fun scaleTarget(observeBean: ObserveBean, scaleX: Float, scaleY: Float) {
        observeBean.observeX *= scaleX
        observeBean.observeY *= scaleY
        observeBean.observeWidth *= scaleX
        observeBean.observeHeight *= scaleY
    }

    fun moveTarget(observeBean: ObserveBean, deltaX: Float, deltaY: Float) {
        observeBean.observeX += deltaX
        observeBean.observeY += deltaY
    }
}