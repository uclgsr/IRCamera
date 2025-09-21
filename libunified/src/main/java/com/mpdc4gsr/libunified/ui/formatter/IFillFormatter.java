package com.mpdc4gsr.libunified.ui.charting.formatter;

import com.mpdc4gsr.libunified.ui.charting.interfaces.dataprovider.LineDataProvider;
import com.mpdc4gsr.libunified.ui.charting.interfaces.datasets.ILineDataSet;

public interface IFillFormatter {

    float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider);
}
