package com.mpdc4gsr.component.shared.ui.data;

import com.mpdc4gsr.component.shared.ui.highlight.Highlight;
import com.mpdc4gsr.component.shared.ui.interfaces.datasets.IPieDataSet;

public class PieData extends ChartData<IPieDataSet> {

    public PieData() {
        super();
    }

    public PieData(IPieDataSet dataSet) {
        super(dataSet);
    }

    public IPieDataSet getDataSet() {
        return mDataSets.get(0);
    }

    public void setDataSet(IPieDataSet dataSet) {
        mDataSets.clear();
        mDataSets.add(dataSet);
        notifyDataChanged();
    }

    @Override
    public IPieDataSet getDataSetByIndex(int index) {
        return index == 0 ? getDataSet() : null;
    }

    @Override
    public IPieDataSet getDataSetByLabel(String label, boolean ignorecase) {
        return ignorecase ? label.equalsIgnoreCase(mDataSets.get(0).getLabel()) ? mDataSets.get(0)
                : null : label.equals(mDataSets.get(0).getLabel()) ? mDataSets.get(0) : null;
    }

    @Override
    public Entry getEntryForHighlight(Highlight highlight) {
        return getDataSet().getEntryForIndex((int) highlight.getX());
    }

    public float getYValueSum() {

        float sum = 0;

        for (int i = 0; i < getDataSet().getEntryCount(); i++)
            sum += getDataSet().getEntryForIndex(i).getY();

        return sum;
    }
}


