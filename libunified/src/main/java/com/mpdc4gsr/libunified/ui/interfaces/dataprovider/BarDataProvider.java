package com.mpdc4gsr.libunified.ui.interfaces.dataprovider;

import com.mpdc4gsr.libunified.ui.data.BarData;

public interface BarDataProvider extends BarLineScatterCandleBubbleDataProvider {

    BarData getBarData();

    boolean isDrawBarShadowEnabled();

    boolean isDrawValueAboveBarEnabled();

    boolean isHighlightFullBarEnabled();
}
