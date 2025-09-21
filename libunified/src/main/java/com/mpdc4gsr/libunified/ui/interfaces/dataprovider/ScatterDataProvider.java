package com.mpdc4gsr.libunified.ui.charting.interfaces.dataprovider;

import com.mpdc4gsr.libunified.ui.charting.data.ScatterData;

public interface ScatterDataProvider extends BarLineScatterCandleBubbleDataProvider {

    ScatterData getScatterData();
}
