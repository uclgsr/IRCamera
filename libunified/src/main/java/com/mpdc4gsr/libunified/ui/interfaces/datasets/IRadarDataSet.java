package com.mpdc4gsr.libunified.ui.charting.interfaces.datasets;

import com.mpdc4gsr.libunified.ui.charting.data.RadarEntry;

public interface IRadarDataSet extends ILineRadarDataSet<RadarEntry> {

    boolean isDrawHighlightCircleEnabled();

    void setDrawHighlightCircleEnabled(boolean enabled);

    int getHighlightCircleFillColor();


    int getHighlightCircleStrokeColor();

    int getHighlightCircleStrokeAlpha();

    float getHighlightCircleInnerRadius();

    float getHighlightCircleOuterRadius();

    float getHighlightCircleStrokeWidth();

}
