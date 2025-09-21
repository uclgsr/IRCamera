package com.mpdc4gsr.libunified.ui.charting.interfaces.datasets;

import android.graphics.drawable.Drawable;

import com.mpdc4gsr.libunified.ui.charting.data.Entry;

public interface ILineRadarDataSet<T extends Entry> extends ILineScatterCandleRadarDataSet<T> {

    int getFillColor();

    Drawable getFillDrawable();

    int getFillAlpha();

    float getLineWidth();

    boolean isDrawFilledEnabled();

    void setDrawFilled(boolean enabled);
}
