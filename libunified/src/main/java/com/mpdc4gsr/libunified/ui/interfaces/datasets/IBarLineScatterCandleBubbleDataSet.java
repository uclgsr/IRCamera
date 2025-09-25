package com.mpdc4gsr.libunified.ui.interfaces.datasets;

import com.mpdc4gsr.libunified.ui.data.Entry;

public interface IBarLineScatterCandleBubbleDataSet<T extends Entry> extends IDataSet<T> {

    int getHighLightColor();
}
