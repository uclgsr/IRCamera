package com.mpdc4gsr.libunified.ui.listener;

import com.mpdc4gsr.libunified.ui.data.Entry;
import com.mpdc4gsr.libunified.ui.highlight.Highlight;

public interface OnChartValueSelectedListener {

    void onValueSelected(Entry e, Highlight h);

    void onNothingSelected();
}
