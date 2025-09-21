package com.mpdc4gsr.libunified.ui.charting.interfaces.datasets;

import com.mpdc4gsr.libunified.ui.charting.data.Entry;

public interface IBarLineScatterCandleBubbleDataSet<T extends Entry> extends IDataSet<T> {

    int getHighLightColor();
}
