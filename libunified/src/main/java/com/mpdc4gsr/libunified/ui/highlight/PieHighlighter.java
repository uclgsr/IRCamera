package com.mpdc4gsr.libunified.ui.charting.highlight;

import com.mpdc4gsr.libunified.ui.charting.charts.PieChart;
import com.mpdc4gsr.libunified.ui.charting.data.Entry;
import com.mpdc4gsr.libunified.ui.charting.interfaces.datasets.IPieDataSet;

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
