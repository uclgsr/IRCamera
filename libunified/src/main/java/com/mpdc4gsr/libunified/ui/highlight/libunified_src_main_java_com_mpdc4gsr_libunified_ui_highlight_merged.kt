// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\ui\highlight' directory and its subdirectories.
// Total files: 10 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\highlight\BarHighlighter.java =====

package com.mpdc4gsr.libunified.ui.highlight;

import com.mpdc4gsr.libunified.ui.data.BarData;
import com.mpdc4gsr.libunified.ui.data.BarEntry;
import com.mpdc4gsr.libunified.ui.data.BarLineScatterCandleBubbleData;
import com.mpdc4gsr.libunified.ui.interfaces.dataprovider.BarDataProvider;
import com.mpdc4gsr.libunified.ui.interfaces.datasets.IBarDataSet;
import com.mpdc4gsr.libunified.ui.utils.MPPointD;

public class BarHighlighter extends ChartHighlighter<BarDataProvider> {

    public BarHighlighter(BarDataProvider chart) {
        super(chart);
    }

    @Override
    public Highlight getHighlight(float x, float y) {
        Highlight high = super.getHighlight(x, y);

        if (high == null) {
            return null;
        }

        MPPointD pos = getValsForTouch(x, y);

        BarData barData = mChart.getBarData();

        IBarDataSet set = barData.getDataSetByIndex(high.getDataSetIndex());
        if (set.isStacked()) {

            return getStackedHighlight(high,
                    set,
                    (float) pos.x,
                    (float) pos.y);
        }

        MPPointD.recycleInstance(pos);

        return high;
    }

    public Highlight getStackedHighlight(Highlight high, IBarDataSet set, float xVal, float yVal) {

        BarEntry entry = set.getEntryForXValue(xVal, yVal);

        if (entry == null)
            return null;

        if (entry.getYVals() == null) {
            return high;
        } else {
            Range[] ranges = entry.getRanges();

            if (ranges.length > 0) {
                int stackIndex = getClosestStackIndex(ranges, yVal);

                MPPointD pixels = mChart.getTransformer(set.getAxisDependency()).getPixelForValues(high.getX(), ranges[stackIndex].to);

                Highlight stackedHigh = new Highlight(
                        entry.getX(),
                        entry.getY(),
                        (float) pixels.x,
                        (float) pixels.y,
                        high.getDataSetIndex(),
                        stackIndex,
                        high.getAxis()
                );

                MPPointD.recycleInstance(pixels);

                return stackedHigh;
            }
        }

        return null;
    }

    protected int getClosestStackIndex(Range[] ranges, float value) {

        if (ranges == null || ranges.length == 0)
            return 0;

        int stackIndex = 0;

        for (Range range : ranges) {
            if (range.contains(value))
                return stackIndex;
            else
                stackIndex++;
        }

        int length = Math.max(ranges.length - 1, 0);

        return (value > ranges[length].to) ? length : 0;
    }

    @Override
    protected float getDistance(float x1, float y1, float x2, float y2) {
        return Math.abs(x1 - x2);
    }

    @Override
    protected BarLineScatterCandleBubbleData getData() {
        return mChart.getBarData();
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\highlight\ChartHighlighter.java =====

package com.mpdc4gsr.libunified.ui.highlight;

import com.mpdc4gsr.libunified.ui.components.YAxis;
import com.mpdc4gsr.libunified.ui.data.BarLineScatterCandleBubbleData;
import com.mpdc4gsr.libunified.ui.data.DataSet;
import com.mpdc4gsr.libunified.ui.data.Entry;
import com.mpdc4gsr.libunified.ui.interfaces.dataprovider.BarLineScatterCandleBubbleDataProvider;
import com.mpdc4gsr.libunified.ui.interfaces.datasets.IDataSet;
import com.mpdc4gsr.libunified.ui.utils.MPPointD;

import java.util.ArrayList;
import java.util.List;

public class ChartHighlighter<T extends BarLineScatterCandleBubbleDataProvider> implements IHighlighter {

    protected T mChart;

    protected List<Highlight> mHighlightBuffer = new ArrayList<Highlight>();

    public ChartHighlighter(T chart) {
        this.mChart = chart;
    }

    @Override
    public Highlight getHighlight(float x, float y) {

        MPPointD pos = getValsForTouch(x, y);
        float xVal = (float) pos.x;
        MPPointD.recycleInstance(pos);

        Highlight high = getHighlightForX(xVal, x, y);
        return high;
    }

    protected MPPointD getValsForTouch(float x, float y) {

        MPPointD pos = mChart.getTransformer(YAxis.AxisDependency.LEFT).getValuesByTouchPoint(x, y);
        return pos;
    }

    protected Highlight getHighlightForX(float xVal, float x, float y) {

        List<Highlight> closestValues = getHighlightsAtXValue(xVal, x, y);

        if (closestValues.isEmpty()) {
            return null;
        }

        float leftAxisMinDist = getMinimumDistance(closestValues, y, YAxis.AxisDependency.LEFT);
        float rightAxisMinDist = getMinimumDistance(closestValues, y, YAxis.AxisDependency.RIGHT);

        YAxis.AxisDependency axis = leftAxisMinDist < rightAxisMinDist ? YAxis.AxisDependency.LEFT : YAxis.AxisDependency.RIGHT;

        Highlight detail = getClosestHighlightByPixel(closestValues, x, y, axis, mChart.getMaxHighlightDistance());

        return detail;
    }

    protected float getMinimumDistance(List<Highlight> closestValues, float pos, YAxis.AxisDependency axis) {

        float distance = Float.MAX_VALUE;

        for (int i = 0; i < closestValues.size(); i++) {

            Highlight high = closestValues.get(i);

            if (high.getAxis() == axis) {

                float tempDistance = Math.abs(getHighlightPos(high) - pos);
                if (tempDistance < distance) {
                    distance = tempDistance;
                }
            }
        }

        return distance;
    }

    protected float getHighlightPos(Highlight h) {
        return h.getYPx();
    }

    protected List<Highlight> getHighlightsAtXValue(float xVal, float x, float y) {

        mHighlightBuffer.clear();

        BarLineScatterCandleBubbleData data = getData();

        if (data == null)
            return mHighlightBuffer;

        for (int i = 0, dataSetCount = data.getDataSetCount(); i < dataSetCount; i++) {

            IDataSet dataSet = data.getDataSetByIndex(i);

            if (!dataSet.isHighlightEnabled())
                continue;

            mHighlightBuffer.addAll(buildHighlights(dataSet, i, xVal, DataSet.Rounding.CLOSEST));
        }

        return mHighlightBuffer;
    }

    protected List<Highlight> buildHighlights(IDataSet set, int dataSetIndex, float xVal, DataSet.Rounding rounding) {

        ArrayList<Highlight> highlights = new ArrayList<>();

        List<Entry> entries = set.getEntriesForXValue(xVal);
        if (entries.size() == 0) {

            final Entry closest = set.getEntryForXValue(xVal, Float.NaN, rounding);
            if (closest != null) {

                entries = set.getEntriesForXValue(closest.getX());
            }
        }

        if (entries.size() == 0)
            return highlights;

        for (Entry e : entries) {
            MPPointD pixels = mChart.getTransformer(
                    set.getAxisDependency()).getPixelForValues(e.getX(), e.getY());

            highlights.add(new Highlight(
                    e.getX(), e.getY(),
                    (float) pixels.x, (float) pixels.y,
                    dataSetIndex, set.getAxisDependency()));
        }

        return highlights;
    }

    public Highlight getClosestHighlightByPixel(List<Highlight> closestValues, float x, float y,
                                                YAxis.AxisDependency axis, float minSelectionDistance) {

        Highlight closest = null;
        float distance = minSelectionDistance;

        for (int i = 0; i < closestValues.size(); i++) {

            Highlight high = closestValues.get(i);

            if (axis == null || high.getAxis() == axis) {

                float cDistance = getDistance(x, y, high.getXPx(), high.getYPx());

                if (cDistance < distance) {
                    closest = high;
                    distance = cDistance;
                }
            }
        }

        return closest;
    }

    protected float getDistance(float x1, float y1, float x2, float y2) {

        return (float) Math.hypot(x1 - x2, y1 - y2);
    }

    protected BarLineScatterCandleBubbleData getData() {
        return mChart.getData();
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\highlight\CombinedHighlighter.java =====

package com.mpdc4gsr.libunified.ui.highlight;

import com.mpdc4gsr.libunified.ui.data.BarData;
import com.mpdc4gsr.libunified.ui.data.BarLineScatterCandleBubbleData;
import com.mpdc4gsr.libunified.ui.data.ChartData;
import com.mpdc4gsr.libunified.ui.data.DataSet;
import com.mpdc4gsr.libunified.ui.interfaces.dataprovider.BarDataProvider;
import com.mpdc4gsr.libunified.ui.interfaces.dataprovider.CombinedDataProvider;
import com.mpdc4gsr.libunified.ui.interfaces.datasets.IDataSet;

import java.util.List;

public class CombinedHighlighter extends ChartHighlighter<CombinedDataProvider> implements IHighlighter {

    protected BarHighlighter barHighlighter;

    public CombinedHighlighter(CombinedDataProvider chart, BarDataProvider barChart) {
        super(chart);

        barHighlighter = barChart.getBarData() == null ? null : new BarHighlighter(barChart);
    }

    @Override
    protected List<Highlight> getHighlightsAtXValue(float xVal, float x, float y) {

        mHighlightBuffer.clear();

        List<BarLineScatterCandleBubbleData> dataObjects = mChart.getCombinedData().getAllData();

        for (int i = 0; i < dataObjects.size(); i++) {

            ChartData dataObject = dataObjects.get(i);

            if (barHighlighter != null && dataObject instanceof BarData) {
                Highlight high = barHighlighter.getHighlight(x, y);

                if (high != null) {
                    high.setDataIndex(i);
                    mHighlightBuffer.add(high);
                }
            } else {

                for (int j = 0, dataSetCount = dataObject.getDataSetCount(); j < dataSetCount; j++) {

                    IDataSet dataSet = dataObjects.get(i).getDataSetByIndex(j);

                    if (!dataSet.isHighlightEnabled())
                        continue;

                    List<Highlight> highs = buildHighlights(dataSet, j, xVal, DataSet.Rounding.CLOSEST);
                    for (Highlight high : highs) {
                        high.setDataIndex(i);
                        mHighlightBuffer.add(high);
                    }
                }
            }
        }

        return mHighlightBuffer;
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\highlight\Highlight.java =====

package com.mpdc4gsr.libunified.ui.highlight;

import com.mpdc4gsr.libunified.ui.components.YAxis;

public class Highlight {

    private float mX = Float.NaN;

    private float mY = Float.NaN;

    private float mXPx;

    private float mYPx;

    private int mDataIndex = -1;

    private int mDataSetIndex;

    private int mStackIndex = -1;

    private YAxis.AxisDependency axis;

    private float mDrawX;

    private float mDrawY;

    public Highlight(float x, float y, int dataSetIndex) {
        this.mX = x;
        this.mY = y;
        this.mDataSetIndex = dataSetIndex;
    }

    public Highlight(float x, int dataSetIndex, int stackIndex) {
        this(x, Float.NaN, dataSetIndex);
        this.mStackIndex = stackIndex;
    }

    public Highlight(float x, float y, float xPx, float yPx, int dataSetIndex, YAxis.AxisDependency axis) {
        this.mX = x;
        this.mY = y;
        this.mXPx = xPx;
        this.mYPx = yPx;
        this.mDataSetIndex = dataSetIndex;
        this.axis = axis;
    }

    public Highlight(float x, float y, float xPx, float yPx, int dataSetIndex, int stackIndex, YAxis.AxisDependency axis) {
        this(x, y, xPx, yPx, dataSetIndex, axis);
        this.mStackIndex = stackIndex;
    }

    public float getX() {
        return mX;
    }

    public float getY() {
        return mY;
    }

    public float getXPx() {
        return mXPx;
    }

    public float getYPx() {
        return mYPx;
    }

    public int getDataIndex() {
        return mDataIndex;
    }

    public void setDataIndex(int mDataIndex) {
        this.mDataIndex = mDataIndex;
    }

    public int getDataSetIndex() {
        return mDataSetIndex;
    }

    public int getStackIndex() {
        return mStackIndex;
    }

    public boolean isStacked() {
        return mStackIndex >= 0;
    }

    public YAxis.AxisDependency getAxis() {
        return axis;
    }

    public void setDraw(float x, float y) {
        this.mDrawX = x;
        this.mDrawY = y;
    }

    public float getDrawX() {
        return mDrawX;
    }

    public float getDrawY() {
        return mDrawY;
    }

    public boolean equalTo(Highlight h) {

        if (h == null)
            return false;
        else {
            if (this.mDataSetIndex == h.mDataSetIndex && this.mX == h.mX
                    && this.mStackIndex == h.mStackIndex && this.mDataIndex == h.mDataIndex)
                return true;
            else
                return false;
        }
    }

    @Override
    public String toString() {
        return "Highlight, x: " + mX + ", y: " + mY + ", dataSetIndex: " + mDataSetIndex
                + ", stackIndex (only stacked barentry): " + mStackIndex;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\highlight\HorizontalBarHighlighter.java =====

package com.mpdc4gsr.libunified.ui.highlight;

import com.mpdc4gsr.libunified.ui.data.BarData;
import com.mpdc4gsr.libunified.ui.data.DataSet;
import com.mpdc4gsr.libunified.ui.data.Entry;
import com.mpdc4gsr.libunified.ui.interfaces.dataprovider.BarDataProvider;
import com.mpdc4gsr.libunified.ui.interfaces.datasets.IBarDataSet;
import com.mpdc4gsr.libunified.ui.interfaces.datasets.IDataSet;
import com.mpdc4gsr.libunified.ui.utils.MPPointD;

import java.util.ArrayList;
import java.util.List;

public class HorizontalBarHighlighter extends BarHighlighter {

    public HorizontalBarHighlighter(BarDataProvider chart) {
        super(chart);
    }

    @Override
    public Highlight getHighlight(float x, float y) {

        BarData barData = mChart.getBarData();

        MPPointD pos = getValsForTouch(y, x);

        Highlight high = getHighlightForX((float) pos.y, y, x);
        if (high == null)
            return null;

        IBarDataSet set = barData.getDataSetByIndex(high.getDataSetIndex());
        if (set.isStacked()) {

            return getStackedHighlight(high,
                    set,
                    (float) pos.y,
                    (float) pos.x);
        }

        MPPointD.recycleInstance(pos);

        return high;
    }

    @Override
    protected List<Highlight> buildHighlights(IDataSet set, int dataSetIndex, float xVal, DataSet.Rounding rounding) {

        ArrayList<Highlight> highlights = new ArrayList<>();

        List<Entry> entries = set.getEntriesForXValue(xVal);
        if (entries.size() == 0) {

            final Entry closest = set.getEntryForXValue(xVal, Float.NaN, rounding);
            if (closest != null) {

                entries = set.getEntriesForXValue(closest.getX());
            }
        }

        if (entries.size() == 0)
            return highlights;

        for (Entry e : entries) {
            MPPointD pixels = mChart.getTransformer(
                    set.getAxisDependency()).getPixelForValues(e.getY(), e.getX());

            highlights.add(new Highlight(
                    e.getX(), e.getY(),
                    (float) pixels.x, (float) pixels.y,
                    dataSetIndex, set.getAxisDependency()));
        }

        return highlights;
    }

    @Override
    protected float getDistance(float x1, float y1, float x2, float y2) {
        return Math.abs(y1 - y2);
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\highlight\IHighlighter.java =====

package com.mpdc4gsr.libunified.ui.highlight;

public interface IHighlighter {

    Highlight getHighlight(float x, float y);
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\highlight\PieHighlighter.java =====

package com.mpdc4gsr.libunified.ui.highlight;

import com.mpdc4gsr.libunified.ui.charts.PieChart;
import com.mpdc4gsr.libunified.ui.data.Entry;
import com.mpdc4gsr.libunified.ui.interfaces.datasets.IPieDataSet;

public class PieHighlighter extends PieRadarHighlighter<PieChart> {

    public PieHighlighter(PieChart chart) {
        super(chart);
    }

    @Override
    protected Highlight getClosestHighlight(int index, float x, float y) {

        IPieDataSet set = mChart.getData().getDataSet();

        final Entry entry = set.getEntryForIndex(index);

        return new Highlight(index, entry.getY(), x, y, 0, set.getAxisDependency());
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\highlight\PieRadarHighlighter.java =====

package com.mpdc4gsr.libunified.ui.highlight;

import com.mpdc4gsr.libunified.ui.charts.PieChart;
import com.mpdc4gsr.libunified.ui.charts.PieRadarChartBase;

import java.util.ArrayList;
import java.util.List;

public abstract class PieRadarHighlighter<T extends PieRadarChartBase> implements IHighlighter {

    protected T mChart;

    protected List<Highlight> mHighlightBuffer = new ArrayList<Highlight>();

    public PieRadarHighlighter(T chart) {
        this.mChart = chart;
    }

    @Override
    public Highlight getHighlight(float x, float y) {

        float touchDistanceToCenter = mChart.distanceToCenter(x, y);

        if (touchDistanceToCenter > mChart.getRadius()) {

            return null;

        } else {

            float angle = mChart.getAngleForPoint(x, y);

            if (mChart instanceof PieChart) {
                angle /= mChart.getAnimator().getPhaseY();
            }

            int index = mChart.getIndexForAngle(angle);

            if (index < 0 || index >= mChart.getData().getMaxEntryCountSet().getEntryCount()) {
                return null;

            } else {
                return getClosestHighlight(index, x, y);
            }
        }
    }

    protected abstract Highlight getClosestHighlight(int index, float x, float y);
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\highlight\RadarHighlighter.java =====

package com.mpdc4gsr.libunified.ui.highlight;

import com.mpdc4gsr.libunified.ui.charts.RadarChart;
import com.mpdc4gsr.libunified.ui.data.Entry;
import com.mpdc4gsr.libunified.ui.interfaces.datasets.IDataSet;
import com.mpdc4gsr.libunified.ui.utils.MPPointF;
import com.mpdc4gsr.libunified.ui.utils.Utils;

import java.util.List;

public class RadarHighlighter extends PieRadarHighlighter<RadarChart> {

    public RadarHighlighter(RadarChart chart) {
        super(chart);
    }

    @Override
    protected Highlight getClosestHighlight(int index, float x, float y) {

        List<Highlight> highlights = getHighlightsAtIndex(index);

        float distanceToCenter = mChart.distanceToCenter(x, y) / mChart.getFactor();

        Highlight closest = null;
        float distance = Float.MAX_VALUE;

        for (int i = 0; i < highlights.size(); i++) {

            Highlight high = highlights.get(i);

            float cdistance = Math.abs(high.getY() - distanceToCenter);
            if (cdistance < distance) {
                closest = high;
                distance = cdistance;
            }
        }

        return closest;
    }

    protected List<Highlight> getHighlightsAtIndex(int index) {

        mHighlightBuffer.clear();

        float phaseX = mChart.getAnimator().getPhaseX();
        float phaseY = mChart.getAnimator().getPhaseY();
        float sliceangle = mChart.getSliceAngle();
        float factor = mChart.getFactor();

        MPPointF pOut = MPPointF.getInstance(0, 0);
        for (int i = 0; i < mChart.getData().getDataSetCount(); i++) {

            IDataSet<?> dataSet = mChart.getData().getDataSetByIndex(i);

            final Entry entry = dataSet.getEntryForIndex(index);

            float y = (entry.getY() - mChart.getYChartMin());

            Utils.getPosition(
                    mChart.getCenterOffsets(), y * factor * phaseY,
                    sliceangle * index * phaseX + mChart.getRotationAngle(), pOut);

            mHighlightBuffer.add(new Highlight(index, entry.getY(), pOut.x, pOut.y, i, dataSet.getAxisDependency()));
        }

        return mHighlightBuffer;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\highlight\Range.java =====

package com.mpdc4gsr.libunified.ui.highlight;

public final class Range {

    public float from;
    public float to;

    public Range(float from, float to) {
        this.from = from;
        this.to = to;
    }

    public boolean contains(float value) {

        if (value > from && value <= to)
            return true;
        else
            return false;
    }

    public boolean isLarger(float value) {
        return value > to;
    }

    public boolean isSmaller(float value) {
        return value < from;
    }
}