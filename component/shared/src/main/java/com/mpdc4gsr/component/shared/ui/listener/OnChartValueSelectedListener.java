package com.mpdc4gsr.component.shared.ui.listener;

import com.mpdc4gsr.component.shared.ui.data.Entry;
import com.mpdc4gsr.component.shared.ui.highlight.Highlight;

public interface OnChartValueSelectedListener {

    void onValueSelected(Entry e, Highlight h);

    void onNothingSelected();
}


