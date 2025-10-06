package com.mpdc4gsr.libunified.app.utils

import android.graphics.PointF
import android.graphics.RectF
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.app.bean.ObserveBean

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

    fun updateTargetTemperature(
        observeBean: ObserveBean,
        maxTemp: Float,
        minTemp: Float,
        avgTemp: Float
    ) {
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

    fun getMeasureSize(targetMeasureMode: Int): Float {
        return when (targetMeasureMode) {
            ObserveBean.TYPE_MEASURE_PERSON -> 180f
            ObserveBean.TYPE_MEASURE_SHEEP -> 120f
            ObserveBean.TYPE_MEASURE_DOG -> 100f
            ObserveBean.TYPE_MEASURE_BIRD -> 80f
            else -> 180f
        }
    }

    fun getSelectTargetDraw(targetMeasureMode: Int, targetType: Int, targetColorType: Int): Int {
        return when {
            // Circle targets
            targetType == ObserveBean.TYPE_TARGET_CIRCLE -> {
                when (targetColorType) {
                    ObserveBean.TYPE_TARGET_COLOR_GREEN -> when (targetMeasureMode) {
                        ObserveBean.TYPE_MEASURE_SHEEP -> R.drawable.ic_target_circle_sheep_green
                        else -> R.drawable.ic_target_circle_person_green
                    }

                    ObserveBean.TYPE_TARGET_COLOR_RED -> when (targetMeasureMode) {
                        ObserveBean.TYPE_MEASURE_SHEEP -> R.drawable.ic_target_circle_sheep_red
                        else -> R.drawable.ic_target_circle_person_red
                    }

                    ObserveBean.TYPE_TARGET_COLOR_BLUE -> when (targetMeasureMode) {
                        ObserveBean.TYPE_MEASURE_SHEEP -> R.drawable.ic_target_circle_sheep_blue
                        else -> R.drawable.ic_target_circle_person_blue
                    }

                    ObserveBean.TYPE_TARGET_COLOR_BLACK -> when (targetMeasureMode) {
                        ObserveBean.TYPE_MEASURE_SHEEP -> R.drawable.ic_target_circle_sheep_black
                        else -> R.drawable.ic_target_circle_person_black
                    }

                    ObserveBean.TYPE_TARGET_COLOR_WHITE -> when (targetMeasureMode) {
                        ObserveBean.TYPE_MEASURE_SHEEP -> R.drawable.ic_target_circle_sheep_white
                        else -> R.drawable.ic_target_circle_person_white
                    }

                    else -> R.drawable.ic_target_circle_person_green
                }
            }
            // Vertical targets
            targetType == ObserveBean.TYPE_TARGET_VERTICAL -> {
                when (targetColorType) {
                    ObserveBean.TYPE_TARGET_COLOR_GREEN -> when (targetMeasureMode) {
                        ObserveBean.TYPE_MEASURE_SHEEP -> R.drawable.ic_target_vertical_sheep_green
                        else -> R.drawable.ic_target_vertical_person_green
                    }

                    ObserveBean.TYPE_TARGET_COLOR_RED -> when (targetMeasureMode) {
                        ObserveBean.TYPE_MEASURE_SHEEP -> R.drawable.ic_target_vertical_sheep_red
                        else -> R.drawable.ic_target_vertical_person_red
                    }

                    ObserveBean.TYPE_TARGET_COLOR_BLUE -> when (targetMeasureMode) {
                        ObserveBean.TYPE_MEASURE_SHEEP -> R.drawable.ic_target_vertical_sheep_blue
                        else -> R.drawable.ic_target_vertical_person_blue
                    }

                    ObserveBean.TYPE_TARGET_COLOR_BLACK -> when (targetMeasureMode) {
                        ObserveBean.TYPE_MEASURE_SHEEP -> R.drawable.ic_target_vertical_sheep_black
                        else -> R.drawable.ic_target_vertical_person_black
                    }

                    ObserveBean.TYPE_TARGET_COLOR_WHITE -> when (targetMeasureMode) {
                        ObserveBean.TYPE_MEASURE_SHEEP -> R.drawable.ic_target_vertical_sheep_white
                        else -> R.drawable.ic_target_vertical_person_white
                    }

                    else -> R.drawable.ic_target_vertical_person_green
                }
            }
            // Horizontal targets (default)
            else -> {
                when (targetColorType) {
                    ObserveBean.TYPE_TARGET_COLOR_GREEN -> when (targetMeasureMode) {
                        ObserveBean.TYPE_MEASURE_SHEEP -> R.drawable.ic_target_horizontal_sheep_green
                        else -> R.drawable.svg_ic_target_horizontal_person_green
                    }

                    ObserveBean.TYPE_TARGET_COLOR_RED -> when (targetMeasureMode) {
                        ObserveBean.TYPE_MEASURE_SHEEP -> R.drawable.ic_target_horizontal_sheep_red
                        else -> R.drawable.ic_target_horizontal_person_red
                    }

                    ObserveBean.TYPE_TARGET_COLOR_BLUE -> when (targetMeasureMode) {
                        ObserveBean.TYPE_MEASURE_SHEEP -> R.drawable.ic_target_horizontal_sheep_blue
                        else -> R.drawable.ic_target_horizontal_person_blue
                    }

                    ObserveBean.TYPE_TARGET_COLOR_BLACK -> when (targetMeasureMode) {
                        ObserveBean.TYPE_MEASURE_SHEEP -> R.drawable.ic_target_horizontal_sheep_black
                        else -> R.drawable.ic_target_horizontal_person_black
                    }

                    ObserveBean.TYPE_TARGET_COLOR_WHITE -> when (targetMeasureMode) {
                        ObserveBean.TYPE_MEASURE_SHEEP -> R.drawable.ic_target_horizontal_sheep_white
                        else -> R.drawable.ic_target_horizontal_person_white
                    }

                    else -> R.drawable.svg_ic_target_horizontal_person_green
                }
            }
        }
    }
}