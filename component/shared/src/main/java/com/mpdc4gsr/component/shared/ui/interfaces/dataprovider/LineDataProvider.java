package com.mpdc4gsr.component.shared.ui.interfaces.dataprovider;

import com.mpdc4gsr.component.shared.ui.components.YAxis;
import com.mpdc4gsr.component.shared.ui.data.LineData;

public interface LineDataProvider extends BarLineScatterCandleBubbleDataProvider {

    LineData getLineData();

    YAxis getAxis(YAxis.AxisDependency dependency);
}


