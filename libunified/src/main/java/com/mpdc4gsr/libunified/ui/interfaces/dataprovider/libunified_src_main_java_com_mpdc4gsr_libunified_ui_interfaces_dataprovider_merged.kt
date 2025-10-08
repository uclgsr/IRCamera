// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\ui\interfaces\dataprovider' directory and its subdirectories.
// Total files: 8 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\interfaces\dataprovider\BarDataProvider.java =====

package com.mpdc4gsr.libunified.ui.interfaces.dataprovider;

import com.mpdc4gsr.libunified.ui.data.BarData;

public interface BarDataProvider extends BarLineScatterCandleBubbleDataProvider {

    BarData getBarData();

    boolean isDrawBarShadowEnabled();

    boolean isDrawValueAboveBarEnabled();

    boolean isHighlightFullBarEnabled();
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\interfaces\dataprovider\BarLineScatterCandleBubbleDataProvider.java =====

package com.mpdc4gsr.libunified.ui.interfaces.dataprovider;

import com.mpdc4gsr.libunified.ui.components.YAxis.AxisDependency;
import com.mpdc4gsr.libunified.ui.data.BarLineScatterCandleBubbleData;
import com.mpdc4gsr.libunified.ui.utils.Transformer;

public interface BarLineScatterCandleBubbleDataProvider extends ChartInterface {

    Transformer getTransformer(AxisDependency axis);

    boolean isInverted(AxisDependency axis);

    float getLowestVisibleX();

    float getHighestVisibleX();

    BarLineScatterCandleBubbleData getData();
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\interfaces\dataprovider\BubbleDataProvider.java =====

package com.mpdc4gsr.libunified.ui.interfaces.dataprovider;

import com.mpdc4gsr.libunified.ui.data.BubbleData;

public interface BubbleDataProvider extends BarLineScatterCandleBubbleDataProvider {

    BubbleData getBubbleData();
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\interfaces\dataprovider\CandleDataProvider.java =====

package com.mpdc4gsr.libunified.ui.interfaces.dataprovider;

import com.mpdc4gsr.libunified.ui.data.CandleData;

public interface CandleDataProvider extends BarLineScatterCandleBubbleDataProvider {

    CandleData getCandleData();
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\interfaces\dataprovider\ChartInterface.java =====

package com.mpdc4gsr.libunified.ui.interfaces.dataprovider;

import android.graphics.RectF;

import com.mpdc4gsr.libunified.ui.data.ChartData;
import com.mpdc4gsr.libunified.ui.formatter.ValueFormatter;
import com.mpdc4gsr.libunified.ui.utils.MPPointF;

public interface ChartInterface {

    float getXChartMin();

    float getXChartMax();

    float getXRange();

    float getYChartMin();

    float getYChartMax();

    float getMaxHighlightDistance();

    int getWidth();

    int getHeight();

    MPPointF getCenterOfView();

    MPPointF getCenterOffsets();

    RectF getContentRect();

    ValueFormatter getDefaultValueFormatter();

    ChartData getData();

    int getMaxVisibleCount();
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\interfaces\dataprovider\CombinedDataProvider.java =====

package com.mpdc4gsr.libunified.ui.interfaces.dataprovider;

import com.mpdc4gsr.libunified.ui.data.CombinedData;

public interface CombinedDataProvider extends LineDataProvider, BarDataProvider, BubbleDataProvider, CandleDataProvider, ScatterDataProvider {

    CombinedData getCombinedData();
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\interfaces\dataprovider\LineDataProvider.java =====

package com.mpdc4gsr.libunified.ui.interfaces.dataprovider;

import com.mpdc4gsr.libunified.ui.components.YAxis;
import com.mpdc4gsr.libunified.ui.data.LineData;

public interface LineDataProvider extends BarLineScatterCandleBubbleDataProvider {

    LineData getLineData();

    YAxis getAxis(YAxis.AxisDependency dependency);
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\interfaces\dataprovider\ScatterDataProvider.java =====

package com.mpdc4gsr.libunified.ui.interfaces.dataprovider;

import com.mpdc4gsr.libunified.ui.data.ScatterData;

public interface ScatterDataProvider extends BarLineScatterCandleBubbleDataProvider {

    ScatterData getScatterData();
}