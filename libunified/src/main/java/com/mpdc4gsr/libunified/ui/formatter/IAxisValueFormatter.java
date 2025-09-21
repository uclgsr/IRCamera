package com.mpdc4gsr.libunified.ui.charting.formatter;

import com.mpdc4gsr.libunified.ui.charting.components.AxisBase;

@Deprecated
public interface IAxisValueFormatter {

    @Deprecated
    String getFormattedValue(float value, AxisBase axis);
}
