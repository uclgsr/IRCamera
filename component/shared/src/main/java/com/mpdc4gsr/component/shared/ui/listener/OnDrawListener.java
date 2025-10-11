package com.mpdc4gsr.component.shared.ui.listener;

import com.mpdc4gsr.component.shared.ui.data.DataSet;
import com.mpdc4gsr.component.shared.ui.data.Entry;

public interface OnDrawListener {

    void onEntryAdded(Entry entry);

    void onEntryMoved(Entry entry);

    void onDrawFinished(DataSet<?> dataSet);

}


