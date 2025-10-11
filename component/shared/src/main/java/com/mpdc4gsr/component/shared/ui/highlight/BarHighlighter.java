package com.mpdc4gsr.component.shared.ui.highlight;

import com.mpdc4gsr.component.shared.ui.data.BarData;
import com.mpdc4gsr.component.shared.ui.data.BarEntry;
import com.mpdc4gsr.component.shared.ui.data.BarLineScatterCandleBubbleData;
import com.mpdc4gsr.component.shared.ui.interfaces.dataprovider.BarDataProvider;
import com.mpdc4gsr.component.shared.ui.interfaces.datasets.IBarDataSet;
import com.mpdc4gsr.component.shared.ui.utils.MPPointD;

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


