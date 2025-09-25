package com.mpdc4gsr.libunified.ui.formatter;

import com.mpdc4gsr.libunified.ui.data.Entry;
import com.mpdc4gsr.libunified.ui.interfaces.datasets.IDataSet;

public interface ColorFormatter {

    int getColor(int index, Entry e, IDataSet set);
}
