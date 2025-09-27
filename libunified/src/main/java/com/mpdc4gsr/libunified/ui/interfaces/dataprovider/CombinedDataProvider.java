package com.mpdc4gsr.libunified.ui.interfaces.dataprovider;

import com.mpdc4gsr.libunified.ui.data.CombinedData;

public interface CombinedDataProvider extends LineDataProvider, BarDataProvider, BubbleDataProvider, CandleDataProvider, ScatterDataProvider {

    CombinedData getCombinedData();
}
