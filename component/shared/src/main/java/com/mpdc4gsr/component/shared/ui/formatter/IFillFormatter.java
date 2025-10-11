package com.mpdc4gsr.component.shared.ui.formatter;

import com.mpdc4gsr.component.shared.ui.interfaces.dataprovider.LineDataProvider;
import com.mpdc4gsr.component.shared.ui.interfaces.datasets.ILineDataSet;

public interface IFillFormatter {

    float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider);
}


