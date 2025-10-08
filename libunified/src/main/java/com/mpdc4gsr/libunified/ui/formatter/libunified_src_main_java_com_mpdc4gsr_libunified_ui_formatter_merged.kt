// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\ui\formatter' directory and its subdirectories.
// Total files: 12 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\formatter\ColorFormatter.java =====

package com.mpdc4gsr.libunified.ui.formatter;

import com.mpdc4gsr.libunified.ui.data.Entry;
import com.mpdc4gsr.libunified.ui.interfaces.datasets.IDataSet;

public interface ColorFormatter {

    int getColor(int index, Entry e, IDataSet set);
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\formatter\DefaultAxisValueFormatter.java =====

package com.mpdc4gsr.libunified.ui.formatter;

import java.text.DecimalFormat;

public class DefaultAxisValueFormatter extends ValueFormatter {

    protected DecimalFormat mFormat;

    protected int digits;

    public DefaultAxisValueFormatter(int digits) {
        this.digits = digits;

        StringBuffer b = new StringBuffer();
        for (int i = 0; i < digits; i++) {
            if (i == 0)
                b.append(".");
            b.append("0");
        }

        mFormat = new DecimalFormat("###,###,###,##0" + b.toString());
    }

    @Override
    public String getFormattedValue(float value) {

        return mFormat.format(value);
    }

    public int getDecimalDigits() {
        return digits;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\formatter\DefaultFillFormatter.java =====

package com.mpdc4gsr.libunified.ui.formatter;

import com.mpdc4gsr.libunified.ui.data.LineData;
import com.mpdc4gsr.libunified.ui.interfaces.dataprovider.LineDataProvider;
import com.mpdc4gsr.libunified.ui.interfaces.datasets.ILineDataSet;

public class DefaultFillFormatter implements IFillFormatter {

    @Override
    public float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider) {

        float fillMin = 0f;
        float chartMaxY = dataProvider.getYChartMax();
        float chartMinY = dataProvider.getYChartMin();

        LineData data = dataProvider.getLineData();

        if (dataSet.getYMax() > 0 && dataSet.getYMin() < 0) {
            fillMin = 0f;
        } else {

            float max, min;

            if (data.getYMax() > 0)
                max = 0f;
            else
                max = chartMaxY;
            if (data.getYMin() < 0)
                min = 0f;
            else
                min = chartMinY;

            fillMin = dataSet.getYMin() >= 0 ? min : max;
        }

        return fillMin;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\formatter\DefaultValueFormatter.java =====

package com.mpdc4gsr.libunified.ui.formatter;

import java.text.DecimalFormat;

public class DefaultValueFormatter extends ValueFormatter {

    protected DecimalFormat mFormat;

    protected int mDecimalDigits;

    public DefaultValueFormatter(int digits) {
        setup(digits);
    }

    public void setup(int digits) {

        this.mDecimalDigits = digits;

        StringBuffer b = new StringBuffer();
        for (int i = 0; i < digits; i++) {
            if (i == 0)
                b.append(".");
            b.append("0");
        }

        mFormat = new DecimalFormat("###,###,###,##0" + b.toString());
    }

    @Override
    public String getFormattedValue(float value) {

        return mFormat.format(value);
    }

    public int getDecimalDigits() {
        return mDecimalDigits;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\formatter\IAxisValueFormatter.java =====

package com.mpdc4gsr.libunified.ui.formatter;

import com.mpdc4gsr.libunified.ui.components.AxisBase;

@Deprecated
public interface IAxisValueFormatter {

    @Deprecated
    String getFormattedValue(float value, AxisBase axis);
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\formatter\IFillFormatter.java =====

package com.mpdc4gsr.libunified.ui.formatter;

import com.mpdc4gsr.libunified.ui.interfaces.dataprovider.LineDataProvider;
import com.mpdc4gsr.libunified.ui.interfaces.datasets.ILineDataSet;

public interface IFillFormatter {

    float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider);
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\formatter\IndexAxisValueFormatter.java =====

package com.mpdc4gsr.libunified.ui.formatter;

import java.util.Collection;

public class IndexAxisValueFormatter extends ValueFormatter {
    private String[] mValues = new String[]{};
    private int mValueCount = 0;

    public IndexAxisValueFormatter() {
    }

    public IndexAxisValueFormatter(String[] values) {
        if (values != null)
            setValues(values);
    }

    public IndexAxisValueFormatter(Collection<String> values) {
        if (values != null)
            setValues(values.toArray(new String[values.size()]));
    }

    @Override
    public String getFormattedValue(float value) {
        int index = Math.round(value);

        if (index < 0 || index >= mValueCount || index != (int) value)
            return "";

        return mValues[index];
    }

    public String[] getValues() {
        return mValues;
    }

    public void setValues(String[] values) {
        if (values == null)
            values = new String[]{};

        this.mValues = values;
        this.mValueCount = values.length;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\formatter\IValueFormatter.java =====

package com.mpdc4gsr.libunified.ui.formatter;

import com.mpdc4gsr.libunified.ui.data.Entry;
import com.mpdc4gsr.libunified.ui.utils.ViewPortHandler;

@Deprecated
public interface IValueFormatter {

    @Deprecated
    String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler);
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\formatter\LargeValueFormatter.java =====

package com.mpdc4gsr.libunified.ui.formatter;

import java.text.DecimalFormat;

public class LargeValueFormatter extends ValueFormatter {

    private String[] mSuffix = new String[]{
            "", "k", "m", "b", "t"
    };
    private int mMaxLength = 5;
    private DecimalFormat mFormat;
    private String mText = "";

    public LargeValueFormatter() {
        mFormat = new DecimalFormat("###E00");
    }

    public LargeValueFormatter(String appendix) {
        this();
        mText = appendix;
    }

    @Override
    public String getFormattedValue(float value) {
        return makePretty(value) + mText;
    }

    public void setAppendix(String appendix) {
        this.mText = appendix;
    }

    public void setSuffix(String[] suffix) {
        this.mSuffix = suffix;
    }

    public void setMaxLength(int maxLength) {
        this.mMaxLength = maxLength;
    }

    private String makePretty(double number) {

        String r = mFormat.format(number);

        int numericValue1 = Character.getNumericValue(r.charAt(r.length() - 1));
        int numericValue2 = Character.getNumericValue(r.charAt(r.length() - 2));
        int combined = Integer.valueOf(numericValue2 + "" + numericValue1);

        r = r.replaceAll("E[0-9][0-9]", mSuffix[combined / 3]);

        while (r.length() > mMaxLength || r.matches("[0-9]+\\.[a-z]")) {
            r = r.substring(0, r.length() - 2) + r.substring(r.length() - 1);
        }

        return r;
    }

    public int getDecimalDigits() {
        return 0;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\formatter\PercentFormatter.java =====

package com.mpdc4gsr.libunified.ui.formatter;

import com.mpdc4gsr.libunified.ui.charts.PieChart;
import com.mpdc4gsr.libunified.ui.data.PieEntry;

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


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\formatter\StackedValueFormatter.java =====

package com.mpdc4gsr.libunified.ui.formatter;

import com.mpdc4gsr.libunified.ui.data.BarEntry;

import java.text.DecimalFormat;

public class StackedValueFormatter extends ValueFormatter {

    private boolean mDrawWholeStack;

    private String mSuffix;

    private DecimalFormat mFormat;

    public StackedValueFormatter(boolean drawWholeStack, String suffix, int decimals) {
        this.mDrawWholeStack = drawWholeStack;
        this.mSuffix = suffix;

        StringBuffer b = new StringBuffer();
        for (int i = 0; i < decimals; i++) {
            if (i == 0)
                b.append(".");
            b.append("0");
        }

        this.mFormat = new DecimalFormat("###,###,###,##0" + b.toString());
    }

    @Override
    public String getBarStackedLabel(float value, BarEntry entry) {
        if (!mDrawWholeStack) {

            float[] vals = entry.getYVals();

            if (vals != null) {

                if (vals[vals.length - 1] == value) {

                    return mFormat.format(entry.getY()) + mSuffix;
                } else {
                    return "";
                }
            }
        }

        return mFormat.format(value) + mSuffix;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\formatter\ValueFormatter.java =====

package com.mpdc4gsr.libunified.ui.formatter;

import com.mpdc4gsr.libunified.ui.components.AxisBase;
import com.mpdc4gsr.libunified.ui.data.*;
import com.mpdc4gsr.libunified.ui.utils.ViewPortHandler;

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