package com.mpdc4gsr.libunified.ui.charting.interfaces.dataprovider;

import com.mpdc4gsr.libunified.ui.charting.data.BarData;

public interface BarDataProvider extends BarLineScatterCandleBubbleDataProvider {

    BarData getBarData();

    boolean isDrawBarShadowEnabled();

    boolean isDrawValueAboveBarEnabled();

    boolean isHighlightFullBarEnabled();
}
