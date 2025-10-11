package com.mpdc4gsr.component.shared.ui.formatter;

import com.mpdc4gsr.component.shared.ui.charts.PieChart;
import com.mpdc4gsr.component.shared.ui.data.PieEntry;

import java.text.DecimalFormat;

public class PercentFormatter extends ValueFormatter {

    public DecimalFormat mFormat;
    private PieChart pieChart;

    public PercentFormatter() {
        mFormat = new DecimalFormat("###,###,##0.0");
    }

    public PercentFormatter(PieChart pieChart) {
        this();
        this.pieChart = pieChart;
    }

    @Override
    public String getFormattedValue(float value) {
        return mFormat.format(value) + " %";
    }

    @Override
    public String getPieLabel(float value, PieEntry pieEntry) {
        if (pieChart != null && pieChart.isUsePercentValuesEnabled()) {

            return getFormattedValue(value);
        } else {

            return mFormat.format(value);
        }
    }

}


