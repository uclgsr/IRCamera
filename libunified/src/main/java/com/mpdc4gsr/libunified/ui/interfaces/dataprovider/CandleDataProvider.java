package com.mpdc4gsr.libunified.ui.charting.interfaces.dataprovider;

import com.mpdc4gsr.libunified.ui.charting.data.CandleData;

public interface CandleDataProvider extends BarLineScatterCandleBubbleDataProvider {

    CandleData getCandleData();
}
