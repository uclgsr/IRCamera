package com.mpdc4gsr.libunified.ui.charting.interfaces.dataprovider;

import com.mpdc4gsr.libunified.ui.charting.components.YAxis;
import com.mpdc4gsr.libunified.ui.charting.data.LineData;

public interface LineDataProvider extends BarLineScatterCandleBubbleDataProvider {

    LineData getLineData();

    YAxis getAxis(YAxis.AxisDependency dependency);
}
