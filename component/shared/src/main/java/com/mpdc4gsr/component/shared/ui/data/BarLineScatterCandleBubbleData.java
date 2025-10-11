package com.mpdc4gsr.component.shared.ui.data;

import com.mpdc4gsr.component.shared.ui.interfaces.datasets.IBarLineScatterCandleBubbleDataSet;

import java.util.List;

public abstract class BarLineScatterCandleBubbleData<T extends IBarLineScatterCandleBubbleDataSet<? extends Entry>>
        extends ChartData<T> {

    public BarLineScatterCandleBubbleData() {
        super();
    }

    public BarLineScatterCandleBubbleData(T... sets) {
        super(sets);
    }

    public BarLineScatterCandleBubbleData(List<T> sets) {
        super(sets);
    }
}


