// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\ui\interfaces' directory and its subdirectories.
// Total files: 19 | Generated on: 2025-10-08 01:42:39


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


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\interfaces\datasets\IBarDataSet.java =====

package com.mpdc4gsr.libunified.ui.interfaces.datasets;

import com.mpdc4gsr.libunified.ui.data.BarEntry;

public interface IBarDataSet extends IBarLineScatterCandleBubbleDataSet<BarEntry> {

    boolean isStacked();

    int getStackSize();

    int getBarShadowColor();

    float getBarBorderWidth();

    int getBarBorderColor();

    int getHighLightAlpha();

    String[] getStackLabels();
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\interfaces\datasets\IBarLineScatterCandleBubbleDataSet.java =====

package com.mpdc4gsr.libunified.ui.interfaces.datasets;

import com.mpdc4gsr.libunified.ui.data.Entry;

public interface IBarLineScatterCandleBubbleDataSet<T extends Entry> extends IDataSet<T> {

    int getHighLightColor();
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\interfaces\datasets\IBubbleDataSet.java =====

package com.mpdc4gsr.libunified.ui.interfaces.datasets;

import com.mpdc4gsr.libunified.ui.data.BubbleEntry;

public interface IBubbleDataSet extends IBarLineScatterCandleBubbleDataSet<BubbleEntry> {

    float getMaxSize();

    boolean isNormalizeSizeEnabled();

    float getHighlightCircleWidth();

    void setHighlightCircleWidth(float width);
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\interfaces\datasets\ICandleDataSet.java =====

package com.mpdc4gsr.libunified.ui.interfaces.datasets;

import android.graphics.Paint;

import com.mpdc4gsr.libunified.ui.data.CandleEntry;

public interface ICandleDataSet extends ILineScatterCandleRadarDataSet<CandleEntry> {

    float getBarSpace();

    boolean getShowCandleBar();

    float getShadowWidth();

    int getShadowColor();

    int getNeutralColor();

    int getIncreasingColor();

    int getDecreasingColor();

    Paint.Style getIncreasingPaintStyle();

    Paint.Style getDecreasingPaintStyle();

    boolean getShadowColorSameAsCandle();
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\interfaces\datasets\IDataSet.java =====

package com.mpdc4gsr.libunified.ui.interfaces.datasets;

import android.graphics.DashPathEffect;
import android.graphics.Typeface;

import com.mpdc4gsr.libunified.ui.components.Legend;
import com.mpdc4gsr.libunified.ui.components.YAxis;
import com.mpdc4gsr.libunified.ui.data.DataSet;
import com.mpdc4gsr.libunified.ui.data.Entry;
import com.mpdc4gsr.libunified.ui.formatter.ValueFormatter;
import com.mpdc4gsr.libunified.ui.model.GradientColor;
import com.mpdc4gsr.libunified.ui.utils.MPPointF;

import java.util.List;

public interface IDataSet<T extends Entry> {

    float getYMin();

    float getYMax();

    float getXMin();

    float getXMax();

    int getEntryCount();

    void calcMinMax();

    void calcMinMaxY(float fromX, float toX);

    T getEntryForXValue(float xValue, float closestToY, DataSet.Rounding rounding);

    T getEntryForXValue(float xValue, float closestToY);

    List<T> getEntriesForXValue(float xValue);

    T getEntryForIndex(int index);

    int getEntryIndex(float xValue, float closestToY, DataSet.Rounding rounding);

    int getEntryIndex(T e);

    int getIndexInEntries(int xIndex);

    boolean addEntry(T e);

    void addEntryOrdered(T e);

    boolean removeFirst();

    boolean removeLast();

    boolean removeEntry(T e);

    boolean removeEntryByXValue(float xValue);

    boolean removeEntry(int index);

    boolean contains(T entry);

    void clear();

    String getLabel();

    void setLabel(String label);

    YAxis.AxisDependency getAxisDependency();

    void setAxisDependency(YAxis.AxisDependency dependency);

    List<Integer> getColors();

    int getColor();

    GradientColor getGradientColor();

    List<GradientColor> getGradientColors();

    GradientColor getGradientColor(int index);

    int getColor(int index);

    boolean isHighlightEnabled();

    void setHighlightEnabled(boolean enabled);

    ValueFormatter getValueFormatter();

    void setValueFormatter(ValueFormatter f);

    boolean needsFormatter();

    void setValueTextColors(List<Integer> colors);

    int getValueTextColor();

    void setValueTextColor(int color);

    int getValueTextColor(int index);

    Typeface getValueTypeface();

    void setValueTypeface(Typeface tf);

    float getValueTextSize();

    void setValueTextSize(float size);

    Legend.LegendForm getForm();

    float getFormSize();

    float getFormLineWidth();

    DashPathEffect getFormLineDashEffect();

    void setDrawValues(boolean enabled);

    boolean isDrawValuesEnabled();

    void setDrawIcons(boolean enabled);

    boolean isDrawIconsEnabled();

    MPPointF getIconsOffset();

    void setIconsOffset(MPPointF offset);

    boolean isVisible();

    void setVisible(boolean visible);
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\interfaces\datasets\ILineDataSet.java =====

package com.mpdc4gsr.libunified.ui.interfaces.datasets;

import android.graphics.DashPathEffect;

import com.mpdc4gsr.libunified.ui.data.Entry;
import com.mpdc4gsr.libunified.ui.data.LineDataSet;
import com.mpdc4gsr.libunified.ui.formatter.IFillFormatter;

public interface ILineDataSet extends ILineRadarDataSet<Entry> {

    LineDataSet.Mode getMode();

    float getCubicIntensity();

    @Deprecated
    boolean isDrawCubicEnabled();

    @Deprecated
    boolean isDrawSteppedEnabled();

    float getCircleRadius();

    float getCircleHoleRadius();

    int getCircleColor(int index);

    int getCircleColorCount();

    boolean isDrawCirclesEnabled();

    int getCircleHoleColor();

    boolean isDrawCircleHoleEnabled();

    DashPathEffect getDashPathEffect();

    boolean isDashedLineEnabled();

    IFillFormatter getFillFormatter();
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\interfaces\datasets\ILineRadarDataSet.java =====

package com.mpdc4gsr.libunified.ui.interfaces.datasets;

import android.graphics.drawable.Drawable;

import com.mpdc4gsr.libunified.ui.data.Entry;

public interface ILineRadarDataSet<T extends Entry> extends ILineScatterCandleRadarDataSet<T> {

    int getFillColor();

    Drawable getFillDrawable();

    int getFillAlpha();

    float getLineWidth();

    boolean isDrawFilledEnabled();

    void setDrawFilled(boolean enabled);
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\interfaces\datasets\ILineScatterCandleRadarDataSet.java =====

package com.mpdc4gsr.libunified.ui.interfaces.datasets;

import android.graphics.DashPathEffect;

import com.mpdc4gsr.libunified.ui.data.Entry;

public interface ILineScatterCandleRadarDataSet<T extends Entry> extends IBarLineScatterCandleBubbleDataSet<T> {

    boolean isVerticalHighlightIndicatorEnabled();

    boolean isHorizontalHighlightIndicatorEnabled();

    float getHighlightLineWidth();

    DashPathEffect getDashPathEffectHighlight();
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\interfaces\datasets\IPieDataSet.java =====

package com.mpdc4gsr.libunified.ui.interfaces.datasets;

import com.mpdc4gsr.libunified.ui.data.PieDataSet;
import com.mpdc4gsr.libunified.ui.data.PieEntry;

public interface IPieDataSet extends IDataSet<PieEntry> {

    float getSliceSpace();

    boolean isAutomaticallyDisableSliceSpacingEnabled();

    float getSelectionShift();

    PieDataSet.ValuePosition getXValuePosition();

    PieDataSet.ValuePosition getYValuePosition();

    boolean isUsingSliceColorAsValueLineColor();

    int getValueLineColor();

    float getValueLineWidth();

    float getValueLinePart1OffsetPercentage();

    float getValueLinePart1Length();

    float getValueLinePart2Length();

    boolean isValueLineVariableLength();

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\interfaces\datasets\IRadarDataSet.java =====

package com.mpdc4gsr.libunified.ui.interfaces.datasets;

import com.mpdc4gsr.libunified.ui.data.RadarEntry;

public interface IRadarDataSet extends ILineRadarDataSet<RadarEntry> {

    boolean isDrawHighlightCircleEnabled();

    void setDrawHighlightCircleEnabled(boolean enabled);

    int getHighlightCircleFillColor();

    int getHighlightCircleStrokeColor();

    int getHighlightCircleStrokeAlpha();

    float getHighlightCircleInnerRadius();

    float getHighlightCircleOuterRadius();

    float getHighlightCircleStrokeWidth();

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\interfaces\datasets\IScatterDataSet.java =====

package com.mpdc4gsr.libunified.ui.interfaces.datasets;

import com.mpdc4gsr.libunified.ui.data.Entry;
import com.mpdc4gsr.libunified.ui.renderer.scatter.IShapeRenderer;

public interface IScatterDataSet extends ILineScatterCandleRadarDataSet<Entry> {

    float getScatterShapeSize();

    float getScatterShapeHoleRadius();

    int getScatterShapeHoleColor();

    IShapeRenderer getShapeRenderer();
}