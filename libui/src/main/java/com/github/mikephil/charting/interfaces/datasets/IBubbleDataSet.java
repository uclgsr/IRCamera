package com.github.mikephil.charting.interfaces.datasets;

import com.github.mikephil.charting.data.BubbleEntry;

public interface IBubbleDataSet extends IBarLineScatterCandleBubbleDataSet<BubbleEntry> {

    float getMaxSize();

    boolean isNormalizeSizeEnabled();

    float getHighlightCircleWidth();

    void setHighlightCircleWidth(float width);
}
