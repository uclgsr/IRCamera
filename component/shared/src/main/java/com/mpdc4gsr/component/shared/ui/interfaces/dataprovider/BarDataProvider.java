package com.mpdc4gsr.component.shared.ui.interfaces.dataprovider;

import com.mpdc4gsr.component.shared.ui.data.BarData;

public interface BarDataProvider extends BarLineScatterCandleBubbleDataProvider {

    BarData getBarData();

    boolean isDrawBarShadowEnabled();

    boolean isDrawValueAboveBarEnabled();

    boolean isHighlightFullBarEnabled();
}


