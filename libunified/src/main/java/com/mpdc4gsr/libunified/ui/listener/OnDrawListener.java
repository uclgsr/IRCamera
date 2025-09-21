package com.mpdc4gsr.libunified.ui.charting.listener;

import com.mpdc4gsr.libunified.ui.charting.data.DataSet;
import com.mpdc4gsr.libunified.ui.charting.data.Entry;

public interface OnDrawListener {

    void onEntryAdded(Entry entry);

    void onEntryMoved(Entry entry);

    void onDrawFinished(DataSet<?> dataSet);

}
