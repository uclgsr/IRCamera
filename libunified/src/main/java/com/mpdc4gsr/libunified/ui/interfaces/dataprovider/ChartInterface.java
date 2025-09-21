package com.mpdc4gsr.libunified.ui.charting.interfaces.dataprovider;

import android.graphics.RectF;

import com.mpdc4gsr.libunified.ui.charting.data.ChartData;
import com.mpdc4gsr.libunified.ui.charting.formatter.ValueFormatter;
import com.mpdc4gsr.libunified.ui.charting.utils.MPPointF;

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
