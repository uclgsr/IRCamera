package com.mpdc4gsr.libunified.ui.charting.listener;

import com.mpdc4gsr.libunified.ui.charting.data.Entry;
import com.mpdc4gsr.libunified.ui.charting.highlight.Highlight;

public interface OnChartValueSelectedListener {

    void onValueSelected(Entry e, Highlight h);

    void onNothingSelected();
}
