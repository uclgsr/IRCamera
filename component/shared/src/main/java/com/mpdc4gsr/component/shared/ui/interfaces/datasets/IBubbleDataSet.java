package com.mpdc4gsr.component.shared.ui.interfaces.datasets;

import com.mpdc4gsr.component.shared.ui.data.BubbleEntry;

public interface IBubbleDataSet extends IBarLineScatterCandleBubbleDataSet<BubbleEntry> {

    float getMaxSize();

    boolean isNormalizeSizeEnabled();

    float getHighlightCircleWidth();

    void setHighlightCircleWidth(float width);
}


