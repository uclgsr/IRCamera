package com.mpdc4gsr.component.shared.ui.interfaces.datasets;

import android.graphics.DashPathEffect;

import com.mpdc4gsr.component.shared.ui.data.Entry;

public interface ILineScatterCandleRadarDataSet<T extends Entry> extends IBarLineScatterCandleBubbleDataSet<T> {

    boolean isVerticalHighlightIndicatorEnabled();

    boolean isHorizontalHighlightIndicatorEnabled();

    float getHighlightLineWidth();

    DashPathEffect getDashPathEffectHighlight();
}


