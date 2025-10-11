package com.mpdc4gsr.component.shared.ui.data;

import com.mpdc4gsr.component.shared.ui.interfaces.datasets.IBubbleDataSet;

import java.util.List;

public class BubbleData extends BarLineScatterCandleBubbleData<IBubbleDataSet> {

    public BubbleData() {
        super();
    }

    public BubbleData(IBubbleDataSet... dataSets) {
        super(dataSets);
    }

    public BubbleData(List<IBubbleDataSet> dataSets) {
        super(dataSets);
    }

    public void setHighlightCircleWidth(float width) {
        for (IBubbleDataSet set : mDataSets) {
            set.setHighlightCircleWidth(width);
        }
    }
}


