package com.mpdc4gsr.component.shared.ui.formatter;

import com.mpdc4gsr.component.shared.ui.components.AxisBase;
import com.mpdc4gsr.component.shared.ui.data.*;
import com.mpdc4gsr.component.shared.ui.utils.ViewPortHandler;

public abstract class ValueFormatter implements IAxisValueFormatter, IValueFormatter {

    @Override
    @Deprecated
    public String getFormattedValue(float value, AxisBase axis) {
        return getFormattedValue(value);
    }

    @Override
    @Deprecated
    public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
        return getFormattedValue(value);
    }

    public String getFormattedValue(float value) {
        return String.valueOf(value);
    }

    public String getAxisLabel(float value, AxisBase axis) {
        return getFormattedValue(value);
    }

    public String getBarLabel(BarEntry barEntry) {
        return getFormattedValue(barEntry.getY());
    }

    public String getBarStackedLabel(float value, BarEntry stackedEntry) {
        return getFormattedValue(value);
    }

    public String getPointLabel(Entry entry) {
        return getFormattedValue(entry.getY());
    }

    public String getPieLabel(float value, PieEntry pieEntry) {
        return getFormattedValue(value);
    }

    public String getRadarLabel(RadarEntry radarEntry) {
        return getFormattedValue(radarEntry.getY());
    }

    public String getBubbleLabel(BubbleEntry bubbleEntry) {
        return getFormattedValue(bubbleEntry.getSize());
    }

    public String getCandleLabel(CandleEntry candleEntry) {
        return getFormattedValue(candleEntry.getHigh());
    }

}


