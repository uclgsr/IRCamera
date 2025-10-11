package com.mpdc4gsr.component.shared.ui.interfaces.dataprovider;

import android.graphics.RectF;

import com.mpdc4gsr.component.shared.ui.data.ChartData;
import com.mpdc4gsr.component.shared.ui.formatter.ValueFormatter;
import com.mpdc4gsr.component.shared.ui.utils.MPPointF;

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


