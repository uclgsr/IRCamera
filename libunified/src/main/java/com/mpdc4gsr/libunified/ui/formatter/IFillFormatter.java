package com.mpdc4gsr.libunified.ui.formatter;

import com.mpdc4gsr.libunified.ui.interfaces.dataprovider.LineDataProvider;
import com.mpdc4gsr.libunified.ui.interfaces.datasets.ILineDataSet;

public interface IFillFormatter {

    float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider);
}
