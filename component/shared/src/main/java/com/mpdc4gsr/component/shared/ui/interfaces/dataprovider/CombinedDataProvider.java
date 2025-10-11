package com.mpdc4gsr.component.shared.ui.interfaces.dataprovider;

import com.mpdc4gsr.component.shared.ui.data.CombinedData;

public interface CombinedDataProvider extends LineDataProvider, BarDataProvider, BubbleDataProvider, CandleDataProvider, ScatterDataProvider {

    CombinedData getCombinedData();
}


