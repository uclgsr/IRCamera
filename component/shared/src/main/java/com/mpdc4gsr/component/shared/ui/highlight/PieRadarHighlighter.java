package com.mpdc4gsr.component.shared.ui.highlight;

import com.mpdc4gsr.component.shared.ui.charts.PieChart;
import com.mpdc4gsr.component.shared.ui.charts.PieRadarChartBase;

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


