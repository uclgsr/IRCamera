package com.mpdc4gsr.libunified.ui.interfaces.datasets;

import android.graphics.DashPathEffect;

import com.mpdc4gsr.libunified.ui.data.Entry;
import com.mpdc4gsr.libunified.ui.data.LineDataSet;
import com.mpdc4gsr.libunified.ui.formatter.IFillFormatter;

public interface ILineDataSet extends ILineRadarDataSet<Entry> {

    LineDataSet.Mode getMode();

    float getCubicIntensity();

    @Deprecated
    boolean isDrawCubicEnabled();

    @Deprecated
    boolean isDrawSteppedEnabled();

    float getCircleRadius();

    float getCircleHoleRadius();

    int getCircleColor(int index);

    int getCircleColorCount();

    boolean isDrawCirclesEnabled();

    int getCircleHoleColor();

    boolean isDrawCircleHoleEnabled();

    DashPathEffect getDashPathEffect();

    boolean isDashedLineEnabled();

    IFillFormatter getFillFormatter();
}
