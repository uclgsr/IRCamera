package com.mpdc4gsr.component.shared.ui.data;

import com.mpdc4gsr.component.shared.ui.interfaces.datasets.ILineDataSet;

import java.util.List;

public class LineData extends BarLineScatterCandleBubbleData<ILineDataSet> {

    public LineData() {
        super();
    }

    public LineData(ILineDataSet... dataSets) {
        super(dataSets);
    }

    public LineData(List<ILineDataSet> dataSets) {
        super(dataSets);
    }
}


