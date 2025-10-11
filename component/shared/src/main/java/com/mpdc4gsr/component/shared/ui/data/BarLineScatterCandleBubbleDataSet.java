package com.mpdc4gsr.component.shared.ui.data;

import android.graphics.Color;

import com.mpdc4gsr.component.shared.ui.interfaces.datasets.IBarLineScatterCandleBubbleDataSet;

import java.util.List;

public abstract class BarLineScatterCandleBubbleDataSet<T extends Entry>
        extends DataSet<T>
        implements IBarLineScatterCandleBubbleDataSet<T> {

    protected int mHighLightColor = Color.rgb(255, 187, 115);

    public BarLineScatterCandleBubbleDataSet(List<T> yVals, String label) {
        super(yVals, label);
    }

    @Override
    public int getHighLightColor() {
        return mHighLightColor;
    }

    public void setHighLightColor(int color) {
        mHighLightColor = color;
    }

    protected void copy(BarLineScatterCandleBubbleDataSet barLineScatterCandleBubbleDataSet) {
        super.copy(barLineScatterCandleBubbleDataSet);
        barLineScatterCandleBubbleDataSet.mHighLightColor = mHighLightColor;
    }
}


