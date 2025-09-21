package com.mpdc4gsr.libunified.ui.charting.interfaces.dataprovider;

import com.mpdc4gsr.libunified.ui.charting.data.CombinedData;

public interface CombinedDataProvider extends LineDataProvider, BarDataProvider, BubbleDataProvider, CandleDataProvider, ScatterDataProvider {

    CombinedData getCombinedData();
}
