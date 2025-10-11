package com.mpdc4gsr.component.shared.ui.interfaces.dataprovider;

import com.mpdc4gsr.component.shared.ui.components.YAxis.AxisDependency;
import com.mpdc4gsr.component.shared.ui.data.BarLineScatterCandleBubbleData;
import com.mpdc4gsr.component.shared.ui.utils.Transformer;

public interface BarLineScatterCandleBubbleDataProvider extends ChartInterface {

    Transformer getTransformer(AxisDependency axis);

    boolean isInverted(AxisDependency axis);

    float getLowestVisibleX();

    float getHighestVisibleX();

    BarLineScatterCandleBubbleData getData();
}


