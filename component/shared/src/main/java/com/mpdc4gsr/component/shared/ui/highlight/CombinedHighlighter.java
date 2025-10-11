package com.mpdc4gsr.component.shared.ui.highlight;

import com.mpdc4gsr.component.shared.ui.data.BarData;
import com.mpdc4gsr.component.shared.ui.data.BarLineScatterCandleBubbleData;
import com.mpdc4gsr.component.shared.ui.data.ChartData;
import com.mpdc4gsr.component.shared.ui.data.DataSet;
import com.mpdc4gsr.component.shared.ui.interfaces.dataprovider.BarDataProvider;
import com.mpdc4gsr.component.shared.ui.interfaces.dataprovider.CombinedDataProvider;
import com.mpdc4gsr.component.shared.ui.interfaces.datasets.IDataSet;

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


