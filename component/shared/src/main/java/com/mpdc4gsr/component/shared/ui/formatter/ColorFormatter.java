package com.mpdc4gsr.component.shared.ui.formatter;

import com.mpdc4gsr.component.shared.ui.data.Entry;
import com.mpdc4gsr.component.shared.ui.interfaces.datasets.IDataSet;

public interface ColorFormatter {

    int getColor(int index, Entry e, IDataSet set);
}


