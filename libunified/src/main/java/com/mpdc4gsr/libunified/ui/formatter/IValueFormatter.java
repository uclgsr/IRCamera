package com.mpdc4gsr.libunified.ui.formatter;

import com.mpdc4gsr.libunified.ui.data.Entry;
import com.mpdc4gsr.libunified.ui.utils.ViewPortHandler;

@Deprecated
public interface IValueFormatter {

    @Deprecated
    String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler);
}
