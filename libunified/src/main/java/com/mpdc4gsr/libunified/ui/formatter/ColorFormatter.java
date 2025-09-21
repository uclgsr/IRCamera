package com.mpdc4gsr.libunified.ui.charting.formatter;

import com.mpdc4gsr.libunified.ui.charting.data.Entry;
import com.mpdc4gsr.libunified.ui.charting.interfaces.datasets.IDataSet;

public interface ColorFormatter {

    int getColor(int index, Entry e, IDataSet set);
}
