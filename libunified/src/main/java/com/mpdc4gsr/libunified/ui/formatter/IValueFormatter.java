package com.mpdc4gsr.libunified.ui.charting.formatter;

import com.mpdc4gsr.libunified.ui.charting.data.Entry;
import com.mpdc4gsr.libunified.ui.charting.utils.ViewPortHandler;

@Deprecated
public interface IValueFormatter {

    @Deprecated
    String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler);
}
