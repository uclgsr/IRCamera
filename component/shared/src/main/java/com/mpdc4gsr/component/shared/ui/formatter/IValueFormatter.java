package com.mpdc4gsr.component.shared.ui.formatter;

import com.mpdc4gsr.component.shared.ui.data.Entry;
import com.mpdc4gsr.component.shared.ui.utils.ViewPortHandler;

@Deprecated
public interface IValueFormatter {

    @Deprecated
    String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler);
}


