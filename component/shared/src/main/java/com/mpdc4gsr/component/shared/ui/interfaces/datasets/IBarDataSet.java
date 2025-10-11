package com.mpdc4gsr.component.shared.ui.interfaces.datasets;

import com.mpdc4gsr.component.shared.ui.data.BarEntry;

public interface IBarDataSet extends IBarLineScatterCandleBubbleDataSet<BarEntry> {

    boolean isStacked();

    int getStackSize();

    int getBarShadowColor();

    float getBarBorderWidth();

    int getBarBorderColor();

    int getHighLightAlpha();

    String[] getStackLabels();
}


