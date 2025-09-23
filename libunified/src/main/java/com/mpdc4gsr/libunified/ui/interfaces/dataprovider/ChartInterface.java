package com.mpdc4gsr.libunified.ui.interfaces.dataprovider;

import android.graphics.RectF;

import com.mpdc4gsr.libunified.ui.data.ChartData;
import com.mpdc4gsr.libunified.ui.formatter.ValueFormatter;
import com.mpdc4gsr.libunified.ui.utils.MPPointF;

public interface ChartInterface {

    float getXChartMin();

    float getXChartMax();

    float getXRange();

    float getYChartMin();

    float getYChartMax();

    float getMaxHighlightDistance();

    int getWidth();

    int getHeight();

    MPPointF getCenterOfView();

    MPPointF getCenterOffsets();

    RectF getContentRect();

    ValueFormatter getDefaultValueFormatter();

    ChartData getData();

    int getMaxVisibleCount();
}
