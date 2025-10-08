// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\ui\data' directory and its subdirectories.
// Total files: 30 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\data\BarData.java =====

package com.mpdc4gsr.libunified.ui.data;

import com.mpdc4gsr.libunified.ui.interfaces.datasets.IBarDataSet;

import java.util.List;

public class BarData extends BarLineScatterCandleBubbleData<IBarDataSet> {

    private float mBarWidth = 0.85f;

    public BarData() {
        super();
    }

    public BarData(IBarDataSet... dataSets) {
        super(dataSets);
    }

    public BarData(List<IBarDataSet> dataSets) {
        super(dataSets);
    }

    public float getBarWidth() {
        return mBarWidth;
    }

    public void setBarWidth(float mBarWidth) {
        this.mBarWidth = mBarWidth;
    }

    public void groupBars(float fromX, float groupSpace, float barSpace) {

        int setCount = mDataSets.size();
        if (setCount <= 1) {
            throw new RuntimeException("BarData needs to hold at least 2 BarDataSets to allow grouping.");
        }

        IBarDataSet max = getMaxEntryCountSet();
        int maxEntryCount = max.getEntryCount();

        float groupSpaceWidthHalf = groupSpace / 2f;
        float barSpaceHalf = barSpace / 2f;
        float barWidthHalf = mBarWidth / 2f;

        float interval = getGroupWidth(groupSpace, barSpace);

        for (int i = 0; i < maxEntryCount; i++) {

            float start = fromX;
            fromX += groupSpaceWidthHalf;

            for (IBarDataSet set : mDataSets) {

                fromX += barSpaceHalf;
                fromX += barWidthHalf;

                if (i < set.getEntryCount()) {

                    BarEntry entry = set.getEntryForIndex(i);

                    if (entry != null) {
                        entry.setX(fromX);
                    }
                }

                fromX += barWidthHalf;
                fromX += barSpaceHalf;
            }

            fromX += groupSpaceWidthHalf;
            float end = fromX;
            float innerInterval = end - start;
            float diff = interval - innerInterval;

            if (diff > 0 || diff < 0) {
                fromX += diff;
            }
        }

        notifyDataChanged();
    }

    public float getGroupWidth(float groupSpace, float barSpace) {
        return mDataSets.size() * (mBarWidth + barSpace) + groupSpace;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\data\BarDataSet.java =====

package com.mpdc4gsr.libunified.ui.data;

import android.graphics.Color;

import com.mpdc4gsr.libunified.ui.interfaces.datasets.IBarDataSet;

import java.util.ArrayList;
import java.util.List;

public class BarDataSet extends BarLineScatterCandleBubbleDataSet<BarEntry> implements IBarDataSet {

    private int mStackSize = 1;

    private int mBarShadowColor = Color.rgb(215, 215, 215);

    private float mBarBorderWidth = 0.0f;

    private int mBarBorderColor = Color.BLACK;

    private int mHighLightAlpha = 120;

    private int mEntryCountStacks = 0;

    private String[] mStackLabels = new String[]{
            "Stack"
    };

    public BarDataSet(List<BarEntry> yVals, String label) {
        super(yVals, label);

        mHighLightColor = Color.rgb(0, 0, 0);

        calcStackSize(yVals);
        calcEntryCountIncludingStacks(yVals);
    }

    @Override
    public DataSet<BarEntry> copy() {
        List<BarEntry> entries = new ArrayList<BarEntry>();
        for (int i = 0; i < mValues.size(); i++) {
            entries.add(mValues.get(i).copy());
        }
        BarDataSet copied = new BarDataSet(entries, getLabel());
        copy(copied);
        return copied;
    }

    protected void copy(BarDataSet barDataSet) {
        super.copy(barDataSet);
        barDataSet.mStackSize = mStackSize;
        barDataSet.mBarShadowColor = mBarShadowColor;
        barDataSet.mBarBorderWidth = mBarBorderWidth;
        barDataSet.mStackLabels = mStackLabels;
        barDataSet.mHighLightAlpha = mHighLightAlpha;
    }

    private void calcEntryCountIncludingStacks(List<BarEntry> yVals) {

        mEntryCountStacks = 0;

        for (int i = 0; i < yVals.size(); i++) {

            float[] vals = yVals.get(i).getYVals();

            if (vals == null)
                mEntryCountStacks++;
            else
                mEntryCountStacks += vals.length;
        }
    }

    private void calcStackSize(List<BarEntry> yVals) {

        for (int i = 0; i < yVals.size(); i++) {

            float[] vals = yVals.get(i).getYVals();

            if (vals != null && vals.length > mStackSize)
                mStackSize = vals.length;
        }
    }

    @Override
    protected void calcMinMax(BarEntry e) {

        if (e != null && !Float.isNaN(e.getY())) {

            if (e.getYVals() == null) {

                if (e.getY() < mYMin)
                    mYMin = e.getY();

                if (e.getY() > mYMax)
                    mYMax = e.getY();
            } else {

                if (-e.getNegativeSum() < mYMin)
                    mYMin = -e.getNegativeSum();

                if (e.getPositiveSum() > mYMax)
                    mYMax = e.getPositiveSum();
            }

            calcMinMaxX(e);
        }
    }

    @Override
    public int getStackSize() {
        return mStackSize;
    }

    @Override
    public boolean isStacked() {
        return mStackSize > 1 ? true : false;
    }

    public int getEntryCountStacks() {
        return mEntryCountStacks;
    }

    @Override
    public int getBarShadowColor() {
        return mBarShadowColor;
    }

    public void setBarShadowColor(int color) {
        mBarShadowColor = color;
    }

    @Override
    public float getBarBorderWidth() {
        return mBarBorderWidth;
    }

    public void setBarBorderWidth(float width) {
        mBarBorderWidth = width;
    }

    @Override
    public int getBarBorderColor() {
        return mBarBorderColor;
    }

    public void setBarBorderColor(int color) {
        mBarBorderColor = color;
    }

    @Override
    public int getHighLightAlpha() {
        return mHighLightAlpha;
    }

    public void setHighLightAlpha(int alpha) {
        mHighLightAlpha = alpha;
    }

    @Override
    public String[] getStackLabels() {
        return mStackLabels;
    }

    public void setStackLabels(String[] labels) {
        mStackLabels = labels;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\data\BarEntry.java =====

package com.mpdc4gsr.libunified.ui.data;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;

import com.mpdc4gsr.libunified.ui.highlight.Range;

@SuppressLint("ParcelCreator")
public class BarEntry extends Entry {

    private float[] mYVals;

    private Range[] mRanges;

    private float mNegativeSum;

    private float mPositiveSum;

    public BarEntry(float x, float y) {
        super(x, y);
    }

    public BarEntry(float x, float y, Object data) {
        super(x, y, data);
    }

    public BarEntry(float x, float y, Drawable icon) {
        super(x, y, icon);
    }

    public BarEntry(float x, float y, Drawable icon, Object data) {
        super(x, y, icon, data);
    }

    public BarEntry(float x, float[] vals) {
        super(x, calcSum(vals));

        this.mYVals = vals;
        calcPosNegSum();
        calcRanges();
    }

    public BarEntry(float x, float[] vals, Object data) {
        super(x, calcSum(vals), data);

        this.mYVals = vals;
        calcPosNegSum();
        calcRanges();
    }

    public BarEntry(float x, float[] vals, Drawable icon) {
        super(x, calcSum(vals), icon);

        this.mYVals = vals;
        calcPosNegSum();
        calcRanges();
    }

    public BarEntry(float x, float[] vals, Drawable icon, Object data) {
        super(x, calcSum(vals), icon, data);

        this.mYVals = vals;
        calcPosNegSum();
        calcRanges();
    }

    private static float calcSum(float[] vals) {

        if (vals == null)
            return 0f;

        float sum = 0f;

        for (float f : vals)
            sum += f;

        return sum;
    }

    public BarEntry copy() {

        BarEntry copied = new BarEntry(getX(), getY(), getData());
        copied.setVals(mYVals);
        return copied;
    }

    public float[] getYVals() {
        return mYVals;
    }

    public void setVals(float[] vals) {
        setY(calcSum(vals));
        mYVals = vals;
        calcPosNegSum();
        calcRanges();
    }

    @Override
    public float getY() {
        return super.getY();
    }

    public Range[] getRanges() {
        return mRanges;
    }

    public boolean isStacked() {
        return mYVals != null;
    }

    @Deprecated
    public float getBelowSum(int stackIndex) {
        return getSumBelow(stackIndex);
    }

    public float getSumBelow(int stackIndex) {

        if (mYVals == null)
            return 0;

        float remainder = 0f;
        int index = mYVals.length - 1;

        while (index > stackIndex && index >= 0) {
            remainder += mYVals[index];
            index--;
        }

        return remainder;
    }

    public float getPositiveSum() {
        return mPositiveSum;
    }

    public float getNegativeSum() {
        return mNegativeSum;
    }

    private void calcPosNegSum() {

        if (mYVals == null) {
            mNegativeSum = 0;
            mPositiveSum = 0;
            return;
        }

        float sumNeg = 0f;
        float sumPos = 0f;

        for (float f : mYVals) {
            if (f <= 0f)
                sumNeg += Math.abs(f);
            else
                sumPos += f;
        }

        mNegativeSum = sumNeg;
        mPositiveSum = sumPos;
    }

    protected void calcRanges() {

        float[] values = getYVals();

        if (values == null || values.length == 0)
            return;

        mRanges = new Range[values.length];

        float negRemain = -getNegativeSum();
        float posRemain = 0f;

        for (int i = 0; i < mRanges.length; i++) {

            float value = values[i];

            if (value < 0) {
                mRanges[i] = new Range(negRemain, negRemain - value);
                negRemain -= value;
            } else {
                mRanges[i] = new Range(posRemain, posRemain + value);
                posRemain += value;
            }
        }
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\data\BarLineScatterCandleBubbleData.java =====

package com.mpdc4gsr.libunified.ui.data;

import com.mpdc4gsr.libunified.ui.interfaces.datasets.IBarLineScatterCandleBubbleDataSet;

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


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\data\BarLineScatterCandleBubbleDataSet.java =====

package com.mpdc4gsr.libunified.ui.data;

import android.graphics.Color;

import com.mpdc4gsr.libunified.ui.interfaces.datasets.IBarLineScatterCandleBubbleDataSet;

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


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\data\BaseDataSet.java =====

package com.mpdc4gsr.libunified.ui.data;

import android.content.Context;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Typeface;

import androidx.core.content.ContextCompat;

import com.mpdc4gsr.libunified.ui.components.Legend;
import com.mpdc4gsr.libunified.ui.components.YAxis;
import com.mpdc4gsr.libunified.ui.formatter.ValueFormatter;
import com.mpdc4gsr.libunified.ui.interfaces.datasets.IDataSet;
import com.mpdc4gsr.libunified.ui.model.GradientColor;
import com.mpdc4gsr.libunified.ui.utils.ColorTemplate;
import com.mpdc4gsr.libunified.ui.utils.MPPointF;
import com.mpdc4gsr.libunified.ui.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseDataSet<T extends Entry> implements IDataSet<T> {

    protected List<Integer> mColors = null;

    protected GradientColor mGradientColor = null;

    protected List<GradientColor> mGradientColors = null;

    protected List<Integer> mValueColors = null;
    protected YAxis.AxisDependency mAxisDependency = YAxis.AxisDependency.LEFT;
    protected boolean mHighlightEnabled = true;
    protected transient ValueFormatter mValueFormatter;
    protected Typeface mValueTypeface;
    protected boolean mDrawValues = true;
    protected boolean mDrawIcons = true;
    protected MPPointF mIconsOffset = new MPPointF();
    protected float mValueTextSize = 17f;
    protected boolean mVisible = true;
    private String mLabel = "DataSet";
    private Legend.LegendForm mForm = Legend.LegendForm.DEFAULT;
    private float mFormSize = Float.NaN;
    private float mFormLineWidth = Float.NaN;
    private DashPathEffect mFormLineDashEffect = null;

    public BaseDataSet() {
        mColors = new ArrayList<Integer>();
        mValueColors = new ArrayList<Integer>();

        mColors.add(Color.rgb(140, 234, 255));
        mValueColors.add(Color.BLACK);
    }

    public BaseDataSet(String label) {
        this();
        this.mLabel = label;
    }

    public void notifyDataSetChanged() {
        calcMinMax();
    }

    @Override
    public List<Integer> getColors() {
        return mColors;
    }

    public void setColors(List<Integer> colors) {
        this.mColors = colors;
    }

    public void setColors(int... colors) {
        this.mColors = ColorTemplate.createColors(colors);
    }

    public List<Integer> getValueColors() {
        return mValueColors;
    }

    @Override
    public int getColor() {
        return mColors.get(0);
    }

    public void setColor(int color) {
        resetColors();
        mColors.add(color);
    }

    @Override
    public int getColor(int index) {
        return mColors.get(index % mColors.size());
    }

    @Override
    public GradientColor getGradientColor() {
        return mGradientColor;
    }

    @Override
    public List<GradientColor> getGradientColors() {
        return mGradientColors;
    }

    public void setGradientColors(List<GradientColor> gradientColors) {
        this.mGradientColors = gradientColors;
    }

    @Override
    public GradientColor getGradientColor(int index) {
        return mGradientColors.get(index % mGradientColors.size());
    }

    public void setColors(int[] colors, Context c) {

        if (mColors == null) {
            mColors = new ArrayList<>();
        }

        mColors.clear();

        for (int color : colors) {
            mColors.add(ContextCompat.getColor(c, color));
        }
    }

    public void addColor(int color) {
        if (mColors == null)
            mColors = new ArrayList<Integer>();
        mColors.add(color);
    }

    public void setGradientColor(int startColor, int endColor) {
        mGradientColor = new GradientColor(startColor, endColor);
    }

    public void setColor(int color, int alpha) {
        setColor(Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color)));
    }

    public void setColors(int[] colors, int alpha) {
        resetColors();
        for (int color : colors) {
            addColor(Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color)));
        }
    }

    public void resetColors() {
        if (mColors == null) {
            mColors = new ArrayList<Integer>();
        }
        mColors.clear();
    }

    @Override
    public String getLabel() {
        return mLabel;
    }

    @Override
    public void setLabel(String label) {
        mLabel = label;
    }

    @Override
    public boolean isHighlightEnabled() {
        return mHighlightEnabled;
    }

    @Override
    public void setHighlightEnabled(boolean enabled) {
        mHighlightEnabled = enabled;
    }

    @Override
    public ValueFormatter getValueFormatter() {
        if (needsFormatter())
            return Utils.getDefaultValueFormatter();
        return mValueFormatter;
    }

    @Override
    public void setValueFormatter(ValueFormatter f) {

        if (f == null)
            return;
        else
            mValueFormatter = f;
    }

    @Override
    public boolean needsFormatter() {
        return mValueFormatter == null;
    }

    @Override
    public void setValueTextColors(List<Integer> colors) {
        mValueColors = colors;
    }

    @Override
    public int getValueTextColor() {
        return mValueColors.get(0);
    }

    @Override
    public void setValueTextColor(int color) {
        mValueColors.clear();
        mValueColors.add(color);
    }

    @Override
    public int getValueTextColor(int index) {
        return mValueColors.get(index % mValueColors.size());
    }

    @Override
    public Typeface getValueTypeface() {
        return mValueTypeface;
    }

    @Override
    public void setValueTypeface(Typeface tf) {
        mValueTypeface = tf;
    }

    @Override
    public float getValueTextSize() {
        return mValueTextSize;
    }

    @Override
    public void setValueTextSize(float size) {
        mValueTextSize = Utils.convertDpToPixel(size);
    }

    @Override
    public Legend.LegendForm getForm() {
        return mForm;
    }

    public void setForm(Legend.LegendForm form) {
        mForm = form;
    }

    @Override
    public float getFormSize() {
        return mFormSize;
    }

    public void setFormSize(float formSize) {
        mFormSize = formSize;
    }

    @Override
    public float getFormLineWidth() {
        return mFormLineWidth;
    }

    public void setFormLineWidth(float formLineWidth) {
        mFormLineWidth = formLineWidth;
    }

    @Override
    public DashPathEffect getFormLineDashEffect() {
        return mFormLineDashEffect;
    }

    public void setFormLineDashEffect(DashPathEffect dashPathEffect) {
        mFormLineDashEffect = dashPathEffect;
    }

    @Override
    public void setDrawValues(boolean enabled) {
        this.mDrawValues = enabled;
    }

    @Override
    public boolean isDrawValuesEnabled() {
        return mDrawValues;
    }

    @Override
    public void setDrawIcons(boolean enabled) {
        mDrawIcons = enabled;
    }

    @Override
    public boolean isDrawIconsEnabled() {
        return mDrawIcons;
    }

    @Override
    public MPPointF getIconsOffset() {
        return mIconsOffset;
    }

    @Override
    public void setIconsOffset(MPPointF offsetDp) {

        mIconsOffset.x = offsetDp.x;
        mIconsOffset.y = offsetDp.y;
    }

    @Override
    public boolean isVisible() {
        return mVisible;
    }

    @Override
    public void setVisible(boolean visible) {
        mVisible = visible;
    }

    @Override
    public YAxis.AxisDependency getAxisDependency() {
        return mAxisDependency;
    }

    @Override
    public void setAxisDependency(YAxis.AxisDependency dependency) {
        mAxisDependency = dependency;
    }

    @Override
    public int getIndexInEntries(int xIndex) {

        for (int i = 0; i < getEntryCount(); i++) {
            if (xIndex == getEntryForIndex(i).getX())
                return i;
        }

        return -1;
    }

    @Override
    public boolean removeFirst() {

        if (getEntryCount() > 0) {

            T entry = getEntryForIndex(0);
            return removeEntry(entry);
        } else
            return false;
    }

    @Override
    public boolean removeLast() {

        if (getEntryCount() > 0) {

            T e = getEntryForIndex(getEntryCount() - 1);
            return removeEntry(e);
        } else
            return false;
    }

    @Override
    public boolean removeEntryByXValue(float xValue) {

        T e = getEntryForXValue(xValue, Float.NaN);
        return removeEntry(e);
    }

    @Override
    public boolean removeEntry(int index) {

        T e = getEntryForIndex(index);
        return removeEntry(e);
    }

    @Override
    public boolean contains(T e) {

        for (int i = 0; i < getEntryCount(); i++) {
            if (getEntryForIndex(i).equals(e))
                return true;
        }

        return false;
    }

    protected void copy(BaseDataSet baseDataSet) {
        baseDataSet.mAxisDependency = mAxisDependency;
        baseDataSet.mColors = mColors;
        baseDataSet.mDrawIcons = mDrawIcons;
        baseDataSet.mDrawValues = mDrawValues;
        baseDataSet.mForm = mForm;
        baseDataSet.mFormLineDashEffect = mFormLineDashEffect;
        baseDataSet.mFormLineWidth = mFormLineWidth;
        baseDataSet.mFormSize = mFormSize;
        baseDataSet.mGradientColor = mGradientColor;
        baseDataSet.mGradientColors = mGradientColors;
        baseDataSet.mHighlightEnabled = mHighlightEnabled;
        baseDataSet.mIconsOffset = mIconsOffset;
        baseDataSet.mValueColors = mValueColors;
        baseDataSet.mValueFormatter = mValueFormatter;
        baseDataSet.mValueColors = mValueColors;
        baseDataSet.mValueTextSize = mValueTextSize;
        baseDataSet.mVisible = mVisible;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\data\BaseEntry.java =====

package com.mpdc4gsr.libunified.ui.data;

import android.graphics.drawable.Drawable;

public abstract class BaseEntry {

    private float y = 0f;

    private Object mData = null;

    private Drawable mIcon = null;

    public BaseEntry() {

    }

    public BaseEntry(float y) {
        this.y = y;
    }

    public BaseEntry(float y, Object data) {
        this(y);
        this.mData = data;
    }

    public BaseEntry(float y, Drawable icon) {
        this(y);
        this.mIcon = icon;
    }

    public BaseEntry(float y, Drawable icon, Object data) {
        this(y);
        this.mIcon = icon;
        this.mData = data;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public Drawable getIcon() {
        return mIcon;
    }

    public void setIcon(Drawable icon) {
        this.mIcon = icon;
    }

    public Object getData() {
        return mData;
    }

    public void setData(Object data) {
        this.mData = data;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\data\BubbleData.java =====

package com.mpdc4gsr.libunified.ui.data;

import com.mpdc4gsr.libunified.ui.interfaces.datasets.IBubbleDataSet;

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


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\data\BubbleDataSet.java =====

package com.mpdc4gsr.libunified.ui.data;

import com.mpdc4gsr.libunified.ui.interfaces.datasets.IBubbleDataSet;
import com.mpdc4gsr.libunified.ui.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class BubbleDataSet extends BarLineScatterCandleBubbleDataSet<BubbleEntry> implements IBubbleDataSet {

    protected float mMaxSize;
    protected boolean mNormalizeSize = true;

    private float mHighlightCircleWidth = 2.5f;

    public BubbleDataSet(List<BubbleEntry> yVals, String label) {
        super(yVals, label);
    }

    @Override
    public float getHighlightCircleWidth() {
        return mHighlightCircleWidth;
    }

    @Override
    public void setHighlightCircleWidth(float width) {
        mHighlightCircleWidth = Utils.convertDpToPixel(width);
    }

    @Override
    protected void calcMinMax(BubbleEntry e) {
        super.calcMinMax(e);

        final float size = e.getSize();

        if (size > mMaxSize) {
            mMaxSize = size;
        }
    }

    @Override
    public DataSet<BubbleEntry> copy() {
        List<BubbleEntry> entries = new ArrayList<BubbleEntry>();
        for (int i = 0; i < mValues.size(); i++) {
            entries.add(mValues.get(i).copy());
        }
        BubbleDataSet copied = new BubbleDataSet(entries, getLabel());
        copy(copied);
        return copied;
    }

    protected void copy(BubbleDataSet bubbleDataSet) {
        bubbleDataSet.mHighlightCircleWidth = mHighlightCircleWidth;
        bubbleDataSet.mNormalizeSize = mNormalizeSize;
    }

    @Override
    public float getMaxSize() {
        return mMaxSize;
    }

    @Override
    public boolean isNormalizeSizeEnabled() {
        return mNormalizeSize;
    }

    public void setNormalizeSizeEnabled(boolean normalizeSize) {
        mNormalizeSize = normalizeSize;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\data\BubbleEntry.java =====

package com.mpdc4gsr.libunified.ui.data;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;

@SuppressLint("ParcelCreator")
public class BubbleEntry extends Entry {

    private float mSize = 0f;

    public BubbleEntry(float x, float y, float size) {
        super(x, y);
        this.mSize = size;
    }

    public BubbleEntry(float x, float y, float size, Object data) {
        super(x, y, data);
        this.mSize = size;
    }

    public BubbleEntry(float x, float y, float size, Drawable icon) {
        super(x, y, icon);
        this.mSize = size;
    }

    public BubbleEntry(float x, float y, float size, Drawable icon, Object data) {
        super(x, y, icon, data);
        this.mSize = size;
    }

    public BubbleEntry copy() {

        BubbleEntry c = new BubbleEntry(getX(), getY(), mSize, getData());
        return c;
    }

    public float getSize() {
        return mSize;
    }

    public void setSize(float size) {
        this.mSize = size;
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\data\CandleData.java =====

package com.mpdc4gsr.libunified.ui.data;

import com.mpdc4gsr.libunified.ui.interfaces.datasets.ICandleDataSet;

import java.util.List;

public class CandleData extends BarLineScatterCandleBubbleData<ICandleDataSet> {

    public CandleData() {
        super();
    }

    public CandleData(List<ICandleDataSet> dataSets) {
        super(dataSets);
    }

    public CandleData(ICandleDataSet... dataSets) {
        super(dataSets);
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\data\CandleDataSet.java =====

package com.mpdc4gsr.libunified.ui.data;

import android.graphics.Paint;

import com.mpdc4gsr.libunified.ui.interfaces.datasets.ICandleDataSet;
import com.mpdc4gsr.libunified.ui.utils.ColorTemplate;
import com.mpdc4gsr.libunified.ui.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class CandleDataSet extends LineScatterCandleRadarDataSet<CandleEntry> implements ICandleDataSet {

    protected Paint.Style mIncreasingPaintStyle = Paint.Style.STROKE;
    protected Paint.Style mDecreasingPaintStyle = Paint.Style.FILL;
    protected int mNeutralColor = ColorTemplate.COLOR_SKIP;
    protected int mIncreasingColor = ColorTemplate.COLOR_SKIP;
    protected int mDecreasingColor = ColorTemplate.COLOR_SKIP;
    protected int mShadowColor = ColorTemplate.COLOR_SKIP;
    private float mShadowWidth = 3f;
    private boolean mShowCandleBar = true;
    private float mBarSpace = 0.1f;
    private boolean mShadowColorSameAsCandle = false;

    public CandleDataSet(List<CandleEntry> yVals, String label) {
        super(yVals, label);
    }

    @Override
    public DataSet<CandleEntry> copy() {
        List<CandleEntry> entries = new ArrayList<CandleEntry>();
        for (int i = 0; i < mValues.size(); i++) {
            entries.add(mValues.get(i).copy());
        }
        CandleDataSet copied = new CandleDataSet(entries, getLabel());
        copy(copied);
        return copied;
    }

    protected void copy(CandleDataSet candleDataSet) {
        super.copy(candleDataSet);
        candleDataSet.mShadowWidth = mShadowWidth;
        candleDataSet.mShowCandleBar = mShowCandleBar;
        candleDataSet.mBarSpace = mBarSpace;
        candleDataSet.mShadowColorSameAsCandle = mShadowColorSameAsCandle;
        candleDataSet.mHighLightColor = mHighLightColor;
        candleDataSet.mIncreasingPaintStyle = mIncreasingPaintStyle;
        candleDataSet.mDecreasingPaintStyle = mDecreasingPaintStyle;
        candleDataSet.mNeutralColor = mNeutralColor;
        candleDataSet.mIncreasingColor = mIncreasingColor;
        candleDataSet.mDecreasingColor = mDecreasingColor;
        candleDataSet.mShadowColor = mShadowColor;
    }

    @Override
    protected void calcMinMax(CandleEntry e) {

        if (e.getLow() < mYMin)
            mYMin = e.getLow();

        if (e.getHigh() > mYMax)
            mYMax = e.getHigh();

        calcMinMaxX(e);
    }

    @Override
    protected void calcMinMaxY(CandleEntry e) {

        if (e.getHigh() < mYMin)
            mYMin = e.getHigh();

        if (e.getHigh() > mYMax)
            mYMax = e.getHigh();

        if (e.getLow() < mYMin)
            mYMin = e.getLow();

        if (e.getLow() > mYMax)
            mYMax = e.getLow();
    }

    @Override
    public float getBarSpace() {
        return mBarSpace;
    }

    public void setBarSpace(float space) {

        if (space < 0f)
            space = 0f;
        if (space > 0.45f)
            space = 0.45f;

        mBarSpace = space;
    }

    @Override
    public float getShadowWidth() {
        return mShadowWidth;
    }

    public void setShadowWidth(float width) {
        mShadowWidth = Utils.convertDpToPixel(width);
    }

    @Override
    public boolean getShowCandleBar() {
        return mShowCandleBar;
    }

    public void setShowCandleBar(boolean showCandleBar) {
        mShowCandleBar = showCandleBar;
    }

    @Override
    public int getNeutralColor() {
        return mNeutralColor;
    }

    public void setNeutralColor(int color) {
        mNeutralColor = color;
    }

    @Override
    public int getIncreasingColor() {
        return mIncreasingColor;
    }

    public void setIncreasingColor(int color) {
        mIncreasingColor = color;
    }

    @Override
    public int getDecreasingColor() {
        return mDecreasingColor;
    }

    public void setDecreasingColor(int color) {
        mDecreasingColor = color;
    }

    @Override
    public Paint.Style getIncreasingPaintStyle() {
        return mIncreasingPaintStyle;
    }

    public void setIncreasingPaintStyle(Paint.Style paintStyle) {
        this.mIncreasingPaintStyle = paintStyle;
    }

    @Override
    public Paint.Style getDecreasingPaintStyle() {
        return mDecreasingPaintStyle;
    }

    public void setDecreasingPaintStyle(Paint.Style decreasingPaintStyle) {
        this.mDecreasingPaintStyle = decreasingPaintStyle;
    }

    @Override
    public int getShadowColor() {
        return mShadowColor;
    }

    public void setShadowColor(int shadowColor) {
        this.mShadowColor = shadowColor;
    }

    @Override
    public boolean getShadowColorSameAsCandle() {
        return mShadowColorSameAsCandle;
    }

    public void setShadowColorSameAsCandle(boolean shadowColorSameAsCandle) {
        this.mShadowColorSameAsCandle = shadowColorSameAsCandle;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\data\CandleEntry.java =====

package com.mpdc4gsr.libunified.ui.data;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;

@SuppressLint("ParcelCreator")
public class CandleEntry extends Entry {

    private float mShadowHigh = 0f;

    private float mShadowLow = 0f;

    private float mClose = 0f;

    private float mOpen = 0f;

    public CandleEntry(float x, float shadowH, float shadowL, float open, float close) {
        super(x, (shadowH + shadowL) / 2f);

        this.mShadowHigh = shadowH;
        this.mShadowLow = shadowL;
        this.mOpen = open;
        this.mClose = close;
    }

    public CandleEntry(float x, float shadowH, float shadowL, float open, float close,
                       Object data) {
        super(x, (shadowH + shadowL) / 2f, data);

        this.mShadowHigh = shadowH;
        this.mShadowLow = shadowL;
        this.mOpen = open;
        this.mClose = close;
    }

    public CandleEntry(float x, float shadowH, float shadowL, float open, float close,
                       Drawable icon) {
        super(x, (shadowH + shadowL) / 2f, icon);

        this.mShadowHigh = shadowH;
        this.mShadowLow = shadowL;
        this.mOpen = open;
        this.mClose = close;
    }

    public CandleEntry(float x, float shadowH, float shadowL, float open, float close,
                       Drawable icon, Object data) {
        super(x, (shadowH + shadowL) / 2f, icon, data);

        this.mShadowHigh = shadowH;
        this.mShadowLow = shadowL;
        this.mOpen = open;
        this.mClose = close;
    }

    public float getShadowRange() {
        return Math.abs(mShadowHigh - mShadowLow);
    }

    public float getBodyRange() {
        return Math.abs(mOpen - mClose);
    }

    @Override
    public float getY() {
        return super.getY();
    }

    public CandleEntry copy() {

        CandleEntry c = new CandleEntry(getX(), mShadowHigh, mShadowLow, mOpen,
                mClose, getData());

        return c;
    }

    public float getHigh() {
        return mShadowHigh;
    }

    public void setHigh(float mShadowHigh) {
        this.mShadowHigh = mShadowHigh;
    }

    public float getLow() {
        return mShadowLow;
    }

    public void setLow(float mShadowLow) {
        this.mShadowLow = mShadowLow;
    }

    public float getClose() {
        return mClose;
    }

    public void setClose(float mClose) {
        this.mClose = mClose;
    }

    public float getOpen() {
        return mOpen;
    }

    public void setOpen(float mOpen) {
        this.mOpen = mOpen;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\data\ChartData.java =====

package com.mpdc4gsr.libunified.ui.data;

import android.graphics.Typeface;
import android.util.Log;

import com.mpdc4gsr.libunified.ui.components.YAxis.AxisDependency;
import com.mpdc4gsr.libunified.ui.formatter.ValueFormatter;
import com.mpdc4gsr.libunified.ui.highlight.Highlight;
import com.mpdc4gsr.libunified.ui.interfaces.datasets.IDataSet;

import java.util.ArrayList;
import java.util.List;

public abstract class ChartData<T extends IDataSet<? extends Entry>> {

    protected float mYMax = -Float.MAX_VALUE;

    protected float mYMin = Float.MAX_VALUE;

    protected float mXMax = -Float.MAX_VALUE;

    protected float mXMin = Float.MAX_VALUE;

    protected float mLeftAxisMax = -Float.MAX_VALUE;

    protected float mLeftAxisMin = Float.MAX_VALUE;

    protected float mRightAxisMax = -Float.MAX_VALUE;

    protected float mRightAxisMin = Float.MAX_VALUE;

    protected List<T> mDataSets;

    public ChartData() {
        mDataSets = new ArrayList<T>();
    }

    public ChartData(T... dataSets) {
        mDataSets = arrayToList(dataSets);
        notifyDataChanged();
    }

    public ChartData(List<T> sets) {
        this.mDataSets = sets;
        notifyDataChanged();
    }

    private List<T> arrayToList(T[] array) {

        List<T> list = new ArrayList<>();

        for (T set : array) {
            list.add(set);
        }

        return list;
    }

    public void notifyDataChanged() {
        calcMinMax();
    }

    public void calcMinMaxY(float fromX, float toX) {

        for (T set : mDataSets) {
            set.calcMinMaxY(fromX, toX);
        }

        calcMinMax();
    }

    protected void calcMinMax() {

        if (mDataSets == null)
            return;

        mYMax = -Float.MAX_VALUE;
        mYMin = Float.MAX_VALUE;
        mXMax = -Float.MAX_VALUE;
        mXMin = Float.MAX_VALUE;

        for (T set : mDataSets) {
            calcMinMax(set);
        }

        mLeftAxisMax = -Float.MAX_VALUE;
        mLeftAxisMin = Float.MAX_VALUE;
        mRightAxisMax = -Float.MAX_VALUE;
        mRightAxisMin = Float.MAX_VALUE;

        T firstLeft = getFirstLeft(mDataSets);

        if (firstLeft != null) {

            mLeftAxisMax = firstLeft.getYMax();
            mLeftAxisMin = firstLeft.getYMin();

            for (T dataSet : mDataSets) {
                if (dataSet.getAxisDependency() == AxisDependency.LEFT) {
                    if (dataSet.getYMin() < mLeftAxisMin)
                        mLeftAxisMin = dataSet.getYMin();

                    if (dataSet.getYMax() > mLeftAxisMax)
                        mLeftAxisMax = dataSet.getYMax();
                }
            }
        }

        T firstRight = getFirstRight(mDataSets);

        if (firstRight != null) {

            mRightAxisMax = firstRight.getYMax();
            mRightAxisMin = firstRight.getYMin();

            for (T dataSet : mDataSets) {
                if (dataSet.getAxisDependency() == AxisDependency.RIGHT) {
                    if (dataSet.getYMin() < mRightAxisMin)
                        mRightAxisMin = dataSet.getYMin();

                    if (dataSet.getYMax() > mRightAxisMax)
                        mRightAxisMax = dataSet.getYMax();
                }
            }
        }
    }

    public int getDataSetCount() {
        if (mDataSets == null)
            return 0;
        return mDataSets.size();
    }

    public float getYMin() {
        return mYMin;
    }

    public float getYMin(AxisDependency axis) {
        if (axis == AxisDependency.LEFT) {

            if (mLeftAxisMin == Float.MAX_VALUE) {
                return mRightAxisMin;
            } else
                return mLeftAxisMin;
        } else {
            if (mRightAxisMin == Float.MAX_VALUE) {
                return mLeftAxisMin;
            } else
                return mRightAxisMin;
        }
    }

    public float getYMax() {
        return mYMax;
    }

    public float getYMax(AxisDependency axis) {
        if (axis == AxisDependency.LEFT) {

            if (mLeftAxisMax == -Float.MAX_VALUE) {
                return mRightAxisMax;
            } else
                return mLeftAxisMax;
        } else {
            if (mRightAxisMax == -Float.MAX_VALUE) {
                return mLeftAxisMax;
            } else
                return mRightAxisMax;
        }
    }

    public float getXMin() {
        return mXMin;
    }

    public float getXMax() {
        return mXMax;
    }

    public List<T> getDataSets() {
        return mDataSets;
    }

    protected int getDataSetIndexByLabel(List<T> dataSets, String label,
                                         boolean ignorecase) {

        if (ignorecase) {
            for (int i = 0; i < dataSets.size(); i++)
                if (label.equalsIgnoreCase(dataSets.get(i).getLabel()))
                    return i;
        } else {
            for (int i = 0; i < dataSets.size(); i++)
                if (label.equals(dataSets.get(i).getLabel()))
                    return i;
        }

        return -1;
    }

    public String[] getDataSetLabels() {

        String[] types = new String[mDataSets.size()];

        for (int i = 0; i < mDataSets.size(); i++) {
            types[i] = mDataSets.get(i).getLabel();
        }

        return types;
    }

    public Entry getEntryForHighlight(Highlight highlight) {
        if (highlight.getDataSetIndex() >= mDataSets.size())
            return null;
        else {
            return mDataSets.get(highlight.getDataSetIndex()).getEntryForXValue(highlight.getX(), highlight.getY());
        }
    }

    public T getDataSetByLabel(String label, boolean ignorecase) {

        int index = getDataSetIndexByLabel(mDataSets, label, ignorecase);

        if (index < 0 || index >= mDataSets.size())
            return null;
        else
            return mDataSets.get(index);
    }

    public T getDataSetByIndex(int index) {

        if (mDataSets == null || index < 0 || index >= mDataSets.size())
            return null;

        return mDataSets.get(index);
    }

    public void addDataSet(T d) {

        if (d == null)
            return;

        calcMinMax(d);

        mDataSets.add(d);
    }

    public boolean removeDataSet(T d) {

        if (d == null)
            return false;

        boolean removed = mDataSets.remove(d);

        if (removed) {
            calcMinMax();
        }

        return removed;
    }

    public boolean removeDataSet(int index) {

        if (index >= mDataSets.size() || index < 0)
            return false;

        T set = mDataSets.get(index);
        return removeDataSet(set);
    }

    public void addEntry(Entry e, int dataSetIndex) {

        if (mDataSets.size() > dataSetIndex && dataSetIndex >= 0) {

            IDataSet set = mDataSets.get(dataSetIndex);

            if (!set.addEntry(e))
                return;

            calcMinMax(e, set.getAxisDependency());

        } else {
            Log.e("addEntry", "Cannot add Entry because dataSetIndex too high or too low.");
        }
    }

    protected void calcMinMax(Entry e, AxisDependency axis) {

        if (mYMax < e.getY())
            mYMax = e.getY();
        if (mYMin > e.getY())
            mYMin = e.getY();

        if (mXMax < e.getX())
            mXMax = e.getX();
        if (mXMin > e.getX())
            mXMin = e.getX();

        if (axis == AxisDependency.LEFT) {

            if (mLeftAxisMax < e.getY())
                mLeftAxisMax = e.getY();
            if (mLeftAxisMin > e.getY())
                mLeftAxisMin = e.getY();
        } else {
            if (mRightAxisMax < e.getY())
                mRightAxisMax = e.getY();
            if (mRightAxisMin > e.getY())
                mRightAxisMin = e.getY();
        }
    }

    protected void calcMinMax(T d) {

        if (mYMax < d.getYMax())
            mYMax = d.getYMax();
        if (mYMin > d.getYMin())
            mYMin = d.getYMin();

        if (mXMax < d.getXMax())
            mXMax = d.getXMax();
        if (mXMin > d.getXMin())
            mXMin = d.getXMin();

        if (d.getAxisDependency() == AxisDependency.LEFT) {

            if (mLeftAxisMax < d.getYMax())
                mLeftAxisMax = d.getYMax();
            if (mLeftAxisMin > d.getYMin())
                mLeftAxisMin = d.getYMin();
        } else {
            if (mRightAxisMax < d.getYMax())
                mRightAxisMax = d.getYMax();
            if (mRightAxisMin > d.getYMin())
                mRightAxisMin = d.getYMin();
        }
    }

    public boolean removeEntry(Entry e, int dataSetIndex) {

        if (e == null || dataSetIndex >= mDataSets.size())
            return false;

        IDataSet set = mDataSets.get(dataSetIndex);

        if (set != null) {

            boolean removed = set.removeEntry(e);

            if (removed) {
                calcMinMax();
            }

            return removed;
        } else
            return false;
    }

    public boolean removeEntry(float xValue, int dataSetIndex) {

        if (dataSetIndex >= mDataSets.size())
            return false;

        IDataSet dataSet = mDataSets.get(dataSetIndex);
        Entry e = dataSet.getEntryForXValue(xValue, Float.NaN);

        if (e == null)
            return false;

        return removeEntry(e, dataSetIndex);
    }

    public T getDataSetForEntry(Entry e) {

        if (e == null)
            return null;

        for (int i = 0; i < mDataSets.size(); i++) {

            T set = mDataSets.get(i);

            for (int j = 0; j < set.getEntryCount(); j++) {
                if (e.equalTo(set.getEntryForXValue(e.getX(), e.getY())))
                    return set;
            }
        }

        return null;
    }

    public int[] getColors() {

        if (mDataSets == null)
            return null;

        int clrcnt = 0;

        for (int i = 0; i < mDataSets.size(); i++) {
            clrcnt += mDataSets.get(i).getColors().size();
        }

        int[] colors = new int[clrcnt];
        int cnt = 0;

        for (int i = 0; i < mDataSets.size(); i++) {

            List<Integer> clrs = mDataSets.get(i).getColors();

            for (Integer clr : clrs) {
                colors[cnt] = clr;
                cnt++;
            }
        }

        return colors;
    }

    public int getIndexOfDataSet(T dataSet) {
        return mDataSets.indexOf(dataSet);
    }

    protected T getFirstLeft(List<T> sets) {
        for (T dataSet : sets) {
            if (dataSet.getAxisDependency() == AxisDependency.LEFT)
                return dataSet;
        }
        return null;
    }

    public T getFirstRight(List<T> sets) {
        for (T dataSet : sets) {
            if (dataSet.getAxisDependency() == AxisDependency.RIGHT)
                return dataSet;
        }
        return null;
    }

    public void setValueFormatter(ValueFormatter f) {
        if (f == null)
            return;
        else {
            for (IDataSet set : mDataSets) {
                set.setValueFormatter(f);
            }
        }
    }

    public void setValueTextColor(int color) {
        for (IDataSet set : mDataSets) {
            set.setValueTextColor(color);
        }
    }

    public void setValueTextColors(List<Integer> colors) {
        for (IDataSet set : mDataSets) {
            set.setValueTextColors(colors);
        }
    }

    public void setValueTypeface(Typeface tf) {
        for (IDataSet set : mDataSets) {
            set.setValueTypeface(tf);
        }
    }

    public void setValueTextSize(float size) {
        for (IDataSet set : mDataSets) {
            set.setValueTextSize(size);
        }
    }

    public void setDrawValues(boolean enabled) {
        for (IDataSet set : mDataSets) {
            set.setDrawValues(enabled);
        }
    }

    public boolean isHighlightEnabled() {
        for (IDataSet set : mDataSets) {
            if (!set.isHighlightEnabled())
                return false;
        }
        return true;
    }

    public void setHighlightEnabled(boolean enabled) {
        for (IDataSet set : mDataSets) {
            set.setHighlightEnabled(enabled);
        }
    }

    public void clearValues() {
        if (mDataSets != null) {
            mDataSets.clear();
        }
        notifyDataChanged();
    }

    public boolean contains(T dataSet) {

        for (T set : mDataSets) {
            if (set.equals(dataSet))
                return true;
        }

        return false;
    }

    public int getEntryCount() {

        int count = 0;

        for (T set : mDataSets) {
            count += set.getEntryCount();
        }

        return count;
    }

    public T getMaxEntryCountSet() {

        if (mDataSets == null || mDataSets.isEmpty())
            return null;

        T max = mDataSets.get(0);

        for (T set : mDataSets) {

            if (set.getEntryCount() > max.getEntryCount())
                max = set;
        }

        return max;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\data\CombinedData.java =====

package com.mpdc4gsr.libunified.ui.data;

import android.util.Log;

import com.mpdc4gsr.libunified.ui.highlight.Highlight;
import com.mpdc4gsr.libunified.ui.interfaces.datasets.IBarLineScatterCandleBubbleDataSet;

import java.util.ArrayList;
import java.util.List;

public class CombinedData extends BarLineScatterCandleBubbleData<IBarLineScatterCandleBubbleDataSet<? extends Entry>> {

    private LineData mLineData;
    private BarData mBarData;
    private ScatterData mScatterData;
    private CandleData mCandleData;
    private BubbleData mBubbleData;

    public CombinedData() {
        super();
    }

    public void setData(LineData data) {
        mLineData = data;
        notifyDataChanged();
    }

    public void setData(BarData data) {
        mBarData = data;
        notifyDataChanged();
    }

    public void setData(ScatterData data) {
        mScatterData = data;
        notifyDataChanged();
    }

    public void setData(CandleData data) {
        mCandleData = data;
        notifyDataChanged();
    }

    public void setData(BubbleData data) {
        mBubbleData = data;
        notifyDataChanged();
    }

    @Override
    public void calcMinMax() {

        if (mDataSets == null) {
            mDataSets = new ArrayList<>();
        }
        mDataSets.clear();

        mYMax = -Float.MAX_VALUE;
        mYMin = Float.MAX_VALUE;
        mXMax = -Float.MAX_VALUE;
        mXMin = Float.MAX_VALUE;

        mLeftAxisMax = -Float.MAX_VALUE;
        mLeftAxisMin = Float.MAX_VALUE;
        mRightAxisMax = -Float.MAX_VALUE;
        mRightAxisMin = Float.MAX_VALUE;

        List<BarLineScatterCandleBubbleData> allData = getAllData();

        for (ChartData data : allData) {

            data.calcMinMax();

            List<IBarLineScatterCandleBubbleDataSet<? extends Entry>> sets = data.getDataSets();
            mDataSets.addAll(sets);

            if (data.getYMax() > mYMax)
                mYMax = data.getYMax();

            if (data.getYMin() < mYMin)
                mYMin = data.getYMin();

            if (data.getXMax() > mXMax)
                mXMax = data.getXMax();

            if (data.getXMin() < mXMin)
                mXMin = data.getXMin();

            if (data.mLeftAxisMax > mLeftAxisMax)
                mLeftAxisMax = data.mLeftAxisMax;

            if (data.mLeftAxisMin < mLeftAxisMin)
                mLeftAxisMin = data.mLeftAxisMin;

            if (data.mRightAxisMax > mRightAxisMax)
                mRightAxisMax = data.mRightAxisMax;

            if (data.mRightAxisMin < mRightAxisMin)
                mRightAxisMin = data.mRightAxisMin;

        }
    }

    public BubbleData getBubbleData() {
        return mBubbleData;
    }

    public LineData getLineData() {
        return mLineData;
    }

    public BarData getBarData() {
        return mBarData;
    }

    public ScatterData getScatterData() {
        return mScatterData;
    }

    public CandleData getCandleData() {
        return mCandleData;
    }

    public List<BarLineScatterCandleBubbleData> getAllData() {

        List<BarLineScatterCandleBubbleData> data = new ArrayList<BarLineScatterCandleBubbleData>();
        if (mLineData != null)
            data.add(mLineData);
        if (mBarData != null)
            data.add(mBarData);
        if (mScatterData != null)
            data.add(mScatterData);
        if (mCandleData != null)
            data.add(mCandleData);
        if (mBubbleData != null)
            data.add(mBubbleData);

        return data;
    }

    public BarLineScatterCandleBubbleData getDataByIndex(int index) {
        return getAllData().get(index);
    }

    @Override
    public void notifyDataChanged() {
        if (mLineData != null)
            mLineData.notifyDataChanged();
        if (mBarData != null)
            mBarData.notifyDataChanged();
        if (mCandleData != null)
            mCandleData.notifyDataChanged();
        if (mScatterData != null)
            mScatterData.notifyDataChanged();
        if (mBubbleData != null)
            mBubbleData.notifyDataChanged();

        calcMinMax();
    }

    @Override
    public Entry getEntryForHighlight(Highlight highlight) {

        if (highlight.getDataIndex() >= getAllData().size())
            return null;

        ChartData data = getDataByIndex(highlight.getDataIndex());

        if (highlight.getDataSetIndex() >= data.getDataSetCount())
            return null;

        List<Entry> entries = data.getDataSetByIndex(highlight.getDataSetIndex())
                .getEntriesForXValue(highlight.getX());
        for (Entry entry : entries)
            if (entry.getY() == highlight.getY() ||
                    Float.isNaN(highlight.getY()))
                return entry;

        return null;
    }

    public IBarLineScatterCandleBubbleDataSet<? extends Entry> getDataSetByHighlight(Highlight highlight) {
        if (highlight.getDataIndex() >= getAllData().size())
            return null;

        BarLineScatterCandleBubbleData data = getDataByIndex(highlight.getDataIndex());

        if (highlight.getDataSetIndex() >= data.getDataSetCount())
            return null;

        return (IBarLineScatterCandleBubbleDataSet<? extends Entry>)
                data.getDataSets().get(highlight.getDataSetIndex());
    }

    public int getDataIndex(ChartData data) {
        return getAllData().indexOf(data);
    }

    @Override
    public boolean removeDataSet(IBarLineScatterCandleBubbleDataSet<? extends Entry> d) {

        List<BarLineScatterCandleBubbleData> datas = getAllData();

        boolean success = false;

        for (ChartData data : datas) {

            success = data.removeDataSet(d);

            if (success) {
                break;
            }
        }

        return success;
    }

    @Deprecated
    @Override
    public boolean removeDataSet(int index) {
        Log.e("MPAndroidChart", "removeDataSet(int index) not supported for CombinedData");
        return false;
    }

    @Deprecated
    @Override
    public boolean removeEntry(Entry e, int dataSetIndex) {
        Log.e("MPAndroidChart", "removeEntry(...) not supported for CombinedData");
        return false;
    }

    @Deprecated
    @Override
    public boolean removeEntry(float xValue, int dataSetIndex) {
        Log.e("MPAndroidChart", "removeEntry(...) not supported for CombinedData");
        return false;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\data\DataSet.java =====

package com.mpdc4gsr.libunified.ui.data;

import java.util.ArrayList;
import java.util.List;

public abstract class DataSet<T extends Entry> extends BaseDataSet<T> {

    protected List<T> mValues = null;

    protected float mYMax = -Float.MAX_VALUE;

    protected float mYMin = Float.MAX_VALUE;

    protected float mXMax = -Float.MAX_VALUE;

    protected float mXMin = Float.MAX_VALUE;

    public DataSet(List<T> values, String label) {
        super(label);
        this.mValues = values;

        if (mValues == null)
            mValues = new ArrayList<T>();

        calcMinMax();
    }

    @Override
    public void calcMinMax() {

        if (mValues == null || mValues.isEmpty())
            return;

        mYMax = -Float.MAX_VALUE;
        mYMin = Float.MAX_VALUE;
        mXMax = -Float.MAX_VALUE;
        mXMin = Float.MAX_VALUE;

        for (T e : mValues) {
            calcMinMax(e);
        }
    }

    @Override
    public void calcMinMaxY(float fromX, float toX) {

        if (mValues == null || mValues.isEmpty())
            return;

        mYMax = -Float.MAX_VALUE;
        mYMin = Float.MAX_VALUE;

        int indexFrom = getEntryIndex(fromX, Float.NaN, Rounding.DOWN);
        int indexTo = getEntryIndex(toX, Float.NaN, Rounding.UP);

        for (int i = indexFrom; i <= indexTo; i++) {

            calcMinMaxY(mValues.get(i));
        }
    }

    protected void calcMinMax(T e) {

        if (e == null)
            return;

        calcMinMaxX(e);

        calcMinMaxY(e);
    }

    protected void calcMinMaxX(T e) {

        if (e.getX() < mXMin)
            mXMin = e.getX();

        if (e.getX() > mXMax)
            mXMax = e.getX();
    }

    protected void calcMinMaxY(T e) {

        if (e.getY() < mYMin)
            mYMin = e.getY();

        if (e.getY() > mYMax)
            mYMax = e.getY();
    }

    @Override
    public int getEntryCount() {
        return mValues.size();
    }

    public List<T> getValues() {
        return mValues;
    }

    public void setValues(List<T> values) {
        mValues = values;
        notifyDataSetChanged();
    }

    public abstract DataSet<T> copy();

    protected void copy(DataSet dataSet) {
        super.copy(dataSet);
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(toSimpleString());
        for (int i = 0; i < mValues.size(); i++) {
            buffer.append(mValues.get(i).toString() + " ");
        }
        return buffer.toString();
    }

    public String toSimpleString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("DataSet, label: " + (getLabel() == null ? "" : getLabel()) + ", entries: " + mValues.size() +
                "\n");
        return buffer.toString();
    }

    @Override
    public float getYMin() {
        return mYMin;
    }

    @Override
    public float getYMax() {
        return mYMax;
    }

    @Override
    public float getXMin() {
        return mXMin;
    }

    @Override
    public float getXMax() {
        return mXMax;
    }

    @Override
    public void addEntryOrdered(T e) {

        if (e == null)
            return;

        if (mValues == null) {
            mValues = new ArrayList<T>();
        }

        calcMinMax(e);

        if (mValues.size() > 0 && mValues.get(mValues.size() - 1).getX() > e.getX()) {
            int closestIndex = getEntryIndex(e.getX(), e.getY(), Rounding.UP);
            mValues.add(closestIndex, e);
        } else {
            mValues.add(e);
        }
    }

    @Override
    public void clear() {
        mValues.clear();
        notifyDataSetChanged();
    }

    @Override
    public boolean addEntry(T e) {

        if (e == null)
            return false;

        List<T> values = getValues();
        if (values == null) {
            values = new ArrayList<T>();
        }

        calcMinMax(e);

        return values.add(e);
    }

    @Override
    public boolean removeEntry(T e) {

        if (e == null)
            return false;

        if (mValues == null)
            return false;

        boolean removed = mValues.remove(e);

        if (removed) {
            calcMinMax();
        }

        return removed;
    }

    @Override
    public int getEntryIndex(Entry e) {
        return mValues.indexOf(e);
    }

    @Override
    public T getEntryForXValue(float xValue, float closestToY, Rounding rounding) {

        int index = getEntryIndex(xValue, closestToY, rounding);
        if (index > -1)
            return mValues.get(index);
        return null;
    }

    @Override
    public T getEntryForXValue(float xValue, float closestToY) {
        return getEntryForXValue(xValue, closestToY, Rounding.CLOSEST);
    }

    @Override
    public T getEntryForIndex(int index) {
        return mValues.get(index);
    }

    @Override
    public int getEntryIndex(float xValue, float closestToY, Rounding rounding) {

        if (mValues == null || mValues.isEmpty())
            return -1;

        int low = 0;
        int high = mValues.size() - 1;
        int closest = high;

        while (low < high) {
            int m = (low + high) / 2;

            final float d1 = mValues.get(m).getX() - xValue,
                    d2 = mValues.get(m + 1).getX() - xValue,
                    ad1 = Math.abs(d1), ad2 = Math.abs(d2);

            if (ad2 < ad1) {

                low = m + 1;
            } else if (ad1 < ad2) {

                high = m;
            } else {

                if (d1 >= 0.0) {

                    high = m;
                } else if (d1 < 0.0) {

                    low = m + 1;
                }
            }

            closest = high;
        }

        if (closest != -1) {
            float closestXValue = mValues.get(closest).getX();
            if (rounding == Rounding.UP) {

                if (closestXValue < xValue && closest < mValues.size() - 1) {
                    ++closest;
                }
            } else if (rounding == Rounding.DOWN) {

                if (closestXValue > xValue && closest > 0) {
                    --closest;
                }
            }

            if (!Float.isNaN(closestToY)) {
                while (closest > 0 && mValues.get(closest - 1).getX() == closestXValue)
                    closest -= 1;

                float closestYValue = mValues.get(closest).getY();
                int closestYIndex = closest;

                while (true) {
                    closest += 1;
                    if (closest >= mValues.size())
                        break;

                    final Entry value = mValues.get(closest);

                    if (value.getX() != closestXValue)
                        break;

                    if (Math.abs(value.getY() - closestToY) < Math.abs(closestYValue - closestToY)) {
                        closestYValue = closestToY;
                        closestYIndex = closest;
                    }
                }

                closest = closestYIndex;
            }
        }

        return closest;
    }

    @Override
    public List<T> getEntriesForXValue(float xValue) {

        List<T> entries = new ArrayList<T>();

        int low = 0;
        int high = mValues.size() - 1;

        while (low <= high) {
            int m = (high + low) / 2;
            T entry = mValues.get(m);

            if (xValue == entry.getX()) {
                while (m > 0 && mValues.get(m - 1).getX() == xValue)
                    m--;

                high = mValues.size();

                for (; m < high; m++) {
                    entry = mValues.get(m);
                    if (entry.getX() == xValue) {
                        entries.add(entry);
                    } else {
                        break;
                    }
                }

                break;
            } else {
                if (xValue > entry.getX())
                    low = m + 1;
                else
                    high = m - 1;
            }
        }

        return entries;
    }

    public enum Rounding {
        UP,
        DOWN,
        CLOSEST,
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\data\Entry.java =====

package com.mpdc4gsr.libunified.ui.data;

import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.ParcelFormatException;
import android.os.Parcelable;

import com.mpdc4gsr.libunified.ui.utils.Utils;

public class Entry extends BaseEntry implements Parcelable {

    public static final Creator<Entry> CREATOR = new Creator<Entry>() {
        public Entry createFromParcel(Parcel source) {
            return new Entry(source);
        }

        public Entry[] newArray(int size) {
            return new Entry[size];
        }
    };

    private float x = 0f;

    public Entry() {

    }

    public Entry(float x, float y) {
        super(y);
        this.x = x;
    }

    public Entry(float x, float y, Object data) {
        super(y, data);
        this.x = x;
    }

    public Entry(float x, float y, Drawable icon) {
        super(y, icon);
        this.x = x;
    }

    public Entry(float x, float y, Drawable icon, Object data) {
        super(y, icon, data);
        this.x = x;
    }

    protected Entry(Parcel in) {
        this.x = in.readFloat();
        this.setY(in.readFloat());
        if (in.readInt() == 1) {
            this.setData(in.readParcelable(Object.class.getClassLoader()));
        }
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public Entry copy() {
        Entry e = new Entry(x, getY(), getData());
        return e;
    }

    public boolean equalTo(Entry e) {

        if (e == null)
            return false;

        if (e.getData() != this.getData())
            return false;

        if (Math.abs(e.x - this.x) > Utils.FLOAT_EPSILON)
            return false;

        if (Math.abs(e.getY() - this.getY()) > Utils.FLOAT_EPSILON)
            return false;

        return true;
    }

    @Override
    public String toString() {
        return "Entry, x: " + x + " y: " + getY();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(this.x);
        dest.writeFloat(this.getY());
        if (getData() != null) {
            if (getData() instanceof Parcelable) {
                dest.writeInt(1);
                dest.writeParcelable((Parcelable) this.getData(), flags);
            } else {
                throw new ParcelFormatException("Cannot parcel an Entry with non-parcelable data");
            }
        } else {
            dest.writeInt(0);
        }
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\data\filter\Approximator.java =====

package com.mpdc4gsr.libunified.ui.data.filter;

import android.annotation.TargetApi;
import android.os.Build;

import java.util.Arrays;

public class Approximator {

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public float[] reduceWithDouglasPeucker(float[] points, float tolerance) {

        int greatestIndex = 0;
        float greatestDistance = 0f;

        Line line = new Line(points[0], points[1], points[points.length - 2], points[points.length - 1]);

        for (int i = 2; i < points.length - 2; i += 2) {

            float distance = line.distance(points[i], points[i + 1]);

            if (distance > greatestDistance) {
                greatestDistance = distance;
                greatestIndex = i;
            }
        }

        if (greatestDistance > tolerance) {

            float[] reduced1 = reduceWithDouglasPeucker(Arrays.copyOfRange(points, 0, greatestIndex + 2), tolerance);
            float[] reduced2 = reduceWithDouglasPeucker(Arrays.copyOfRange(points, greatestIndex, points.length),
                    tolerance);

            float[] result1 = reduced1;
            float[] result2 = Arrays.copyOfRange(reduced2, 2, reduced2.length);

            return concat(result1, result2);
        } else {
            return line.getPoints();
        }
    }

    float[] concat(float[]... arrays) {
        int length = 0;
        for (float[] array : arrays) {
            length += array.length;
        }
        float[] result = new float[length];
        int pos = 0;
        for (float[] array : arrays) {
            for (float element : array) {
                result[pos] = element;
                pos++;
            }
        }
        return result;
    }

    private class Line {

        private float[] points;

        private float sxey;
        private float exsy;

        private float dx;
        private float dy;

        private float length;

        public Line(float x1, float y1, float x2, float y2) {
            dx = x1 - x2;
            dy = y1 - y2;
            sxey = x1 * y2;
            exsy = x2 * y1;
            length = (float) Math.sqrt(dx * dx + dy * dy);

            points = new float[]{x1, y1, x2, y2};
        }

        public float distance(float x, float y) {
            return Math.abs(dy * x - dx * y + sxey - exsy) / length;
        }

        public float[] getPoints() {
            return points;
        }
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\data\LineData.java =====

package com.mpdc4gsr.libunified.ui.data;

import com.mpdc4gsr.libunified.ui.interfaces.datasets.ILineDataSet;

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


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\data\LineDataSet.java =====

package com.mpdc4gsr.libunified.ui.data;

import android.content.Context;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.mpdc4gsr.libunified.ui.formatter.DefaultFillFormatter;
import com.mpdc4gsr.libunified.ui.formatter.IFillFormatter;
import com.mpdc4gsr.libunified.ui.interfaces.datasets.ILineDataSet;
import com.mpdc4gsr.libunified.ui.utils.ColorTemplate;
import com.mpdc4gsr.libunified.ui.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class LineDataSet extends LineRadarDataSet<Entry> implements ILineDataSet {

    private Mode mMode = Mode.LINEAR;

    private List<Integer> mCircleColors = null;

    private int mCircleHoleColor = Color.WHITE;

    private float mCircleRadius = 8f;

    private float mCircleHoleRadius = 4f;

    private float mCubicIntensity = 0.2f;

    private DashPathEffect mDashPathEffect = null;

    private IFillFormatter mFillFormatter = new DefaultFillFormatter();

    private boolean mDrawCircles = true;

    private boolean mDrawCircleHole = true;

    public LineDataSet(List<Entry> yVals, String label) {
        super(yVals, label);

        if (mCircleColors == null) {
            mCircleColors = new ArrayList<Integer>();
        }
        mCircleColors.clear();

        mCircleColors.add(Color.rgb(140, 234, 255));
    }

    @Override
    public DataSet<Entry> copy() {
        List<Entry> entries = new ArrayList<Entry>();
        for (int i = 0; i < mValues.size(); i++) {
            entries.add(mValues.get(i).copy());
        }
        LineDataSet copied = new LineDataSet(entries, getLabel());
        copy(copied);
        return copied;
    }

    protected void copy(LineDataSet lineDataSet) {
        super.copy(lineDataSet);
        lineDataSet.mCircleColors = mCircleColors;
        lineDataSet.mCircleHoleColor = mCircleHoleColor;
        lineDataSet.mCircleHoleRadius = mCircleHoleRadius;
        lineDataSet.mCircleRadius = mCircleRadius;
        lineDataSet.mCubicIntensity = mCubicIntensity;
        lineDataSet.mDashPathEffect = mDashPathEffect;
        lineDataSet.mDrawCircleHole = mDrawCircleHole;
        lineDataSet.mDrawCircles = mDrawCircleHole;
        lineDataSet.mFillFormatter = mFillFormatter;
        lineDataSet.mMode = mMode;
    }

    @Override
    public Mode getMode() {
        return mMode;
    }

    public void setMode(Mode mode) {
        mMode = mode;
    }

    @Override
    public float getCubicIntensity() {
        return mCubicIntensity;
    }

    public void setCubicIntensity(float intensity) {

        if (intensity > 1f)
            intensity = 1f;
        if (intensity < 0.05f)
            intensity = 0.05f;

        mCubicIntensity = intensity;
    }

    @Override
    public float getCircleRadius() {
        return mCircleRadius;
    }

    public void setCircleRadius(float radius) {

        if (radius >= 1f) {
            mCircleRadius = Utils.convertDpToPixel(radius);
        } else {
            Log.e("LineDataSet", "Circle radius cannot be < 1");
        }
    }

    @Override
    public float getCircleHoleRadius() {
        return mCircleHoleRadius;
    }

    public void setCircleHoleRadius(float holeRadius) {

        if (holeRadius >= 0.5f) {
            mCircleHoleRadius = Utils.convertDpToPixel(holeRadius);
        } else {
            Log.e("LineDataSet", "Circle radius cannot be < 0.5");
        }
    }

    @Deprecated
    public float getCircleSize() {
        return getCircleRadius();
    }

    @Deprecated
    public void setCircleSize(float size) {
        setCircleRadius(size);
    }

    public void enableDashedLine(float lineLength, float spaceLength, float phase) {
        mDashPathEffect = new DashPathEffect(new float[]{
                lineLength, spaceLength
        }, phase);
    }

    public void disableDashedLine() {
        mDashPathEffect = null;
    }

    @Override
    public boolean isDashedLineEnabled() {
        return mDashPathEffect == null ? false : true;
    }

    @Override
    public DashPathEffect getDashPathEffect() {
        return mDashPathEffect;
    }

    public void setDrawCircles(boolean enabled) {
        this.mDrawCircles = enabled;
    }

    @Override
    public boolean isDrawCirclesEnabled() {
        return mDrawCircles;
    }

    @Deprecated
    @Override
    public boolean isDrawCubicEnabled() {
        return mMode == Mode.CUBIC_BEZIER;
    }

    @Deprecated
    @Override
    public boolean isDrawSteppedEnabled() {
        return mMode == Mode.STEPPED;
    }

    public List<Integer> getCircleColors() {
        return mCircleColors;
    }

    public void setCircleColors(List<Integer> colors) {
        mCircleColors = colors;
    }

    public void setCircleColors(int... colors) {
        this.mCircleColors = ColorTemplate.createColors(colors);
    }

    @Override
    public int getCircleColor(int index) {
        return mCircleColors.get(index);
    }

    @Override
    public int getCircleColorCount() {
        return mCircleColors.size();
    }

    public void setCircleColors(int[] colors, Context c) {

        List<Integer> clrs = mCircleColors;
        if (clrs == null) {
            clrs = new ArrayList<>();
        }
        clrs.clear();

        for (int color : colors) {
            clrs.add(ContextCompat.getColor(c, color));
        }

        mCircleColors = clrs;
    }

    public void setCircleColor(int color) {
        resetCircleColors();
        mCircleColors.add(color);
    }

    public void resetCircleColors() {
        if (mCircleColors == null) {
            mCircleColors = new ArrayList<Integer>();
        }
        mCircleColors.clear();
    }

    @Override
    public int getCircleHoleColor() {
        return mCircleHoleColor;
    }

    public void setCircleHoleColor(int color) {
        mCircleHoleColor = color;
    }

    public void setDrawCircleHole(boolean enabled) {
        mDrawCircleHole = enabled;
    }

    @Override
    public boolean isDrawCircleHoleEnabled() {
        return mDrawCircleHole;
    }

    @Override
    public IFillFormatter getFillFormatter() {
        return mFillFormatter;
    }

    public void setFillFormatter(IFillFormatter formatter) {

        if (formatter == null)
            mFillFormatter = new DefaultFillFormatter();
        else
            mFillFormatter = formatter;
    }

    public enum Mode {
        LINEAR,
        STEPPED,
        CUBIC_BEZIER,
        HORIZONTAL_BEZIER
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\data\LineRadarDataSet.java =====

package com.mpdc4gsr.libunified.ui.data;

import android.annotation.TargetApi;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import com.mpdc4gsr.libunified.ui.interfaces.datasets.ILineRadarDataSet;
import com.mpdc4gsr.libunified.ui.utils.Utils;

import java.util.List;

public abstract class LineRadarDataSet<T extends Entry> extends LineScatterCandleRadarDataSet<T> implements ILineRadarDataSet<T> {

    protected Drawable mFillDrawable;
    private int mFillColor = Color.rgb(140, 234, 255);
    private int mFillAlpha = 85;

    private float mLineWidth = 2.5f;

    private boolean mDrawFilled = false;

    public LineRadarDataSet(List<T> yVals, String label) {
        super(yVals, label);
    }

    @Override
    public int getFillColor() {
        return mFillColor;
    }

    public void setFillColor(int color) {
        mFillColor = color;
        mFillDrawable = null;
    }

    @Override
    public Drawable getFillDrawable() {
        return mFillDrawable;
    }

    @TargetApi(18)
    public void setFillDrawable(Drawable drawable) {
        this.mFillDrawable = drawable;
    }

    @Override
    public int getFillAlpha() {
        return mFillAlpha;
    }

    public void setFillAlpha(int alpha) {
        mFillAlpha = alpha;
    }

    @Override
    public float getLineWidth() {
        return mLineWidth;
    }

    public void setLineWidth(float width) {

        if (width < 0.0f)
            width = 0.0f;
        if (width > 10.0f)
            width = 10.0f;
        mLineWidth = Utils.convertDpToPixel(width);
    }

    @Override
    public void setDrawFilled(boolean filled) {
        mDrawFilled = filled;
    }

    @Override
    public boolean isDrawFilledEnabled() {
        return mDrawFilled;
    }

    protected void copy(LineRadarDataSet lineRadarDataSet) {
        super.copy(lineRadarDataSet);
        lineRadarDataSet.mDrawFilled = mDrawFilled;
        lineRadarDataSet.mFillAlpha = mFillAlpha;
        lineRadarDataSet.mFillColor = mFillColor;
        lineRadarDataSet.mFillDrawable = mFillDrawable;
        lineRadarDataSet.mLineWidth = mLineWidth;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\data\LineScatterCandleRadarDataSet.java =====

package com.mpdc4gsr.libunified.ui.data;

import android.graphics.DashPathEffect;

import com.mpdc4gsr.libunified.ui.interfaces.datasets.ILineScatterCandleRadarDataSet;
import com.mpdc4gsr.libunified.ui.utils.Utils;

import java.util.List;

public abstract class LineScatterCandleRadarDataSet<T extends Entry> extends BarLineScatterCandleBubbleDataSet<T> implements ILineScatterCandleRadarDataSet<T> {

    protected boolean mDrawVerticalHighlightIndicator = true;
    protected boolean mDrawHorizontalHighlightIndicator = true;

    protected float mHighlightLineWidth = 0.5f;

    protected DashPathEffect mHighlightDashPathEffect = null;

    public LineScatterCandleRadarDataSet(List<T> yVals, String label) {
        super(yVals, label);
        mHighlightLineWidth = Utils.convertDpToPixel(0.5f);
    }

    public void setDrawHorizontalHighlightIndicator(boolean enabled) {
        this.mDrawHorizontalHighlightIndicator = enabled;
    }

    public void setDrawVerticalHighlightIndicator(boolean enabled) {
        this.mDrawVerticalHighlightIndicator = enabled;
    }

    public void setDrawHighlightIndicators(boolean enabled) {
        setDrawVerticalHighlightIndicator(enabled);
        setDrawHorizontalHighlightIndicator(enabled);
    }

    @Override
    public boolean isVerticalHighlightIndicatorEnabled() {
        return mDrawVerticalHighlightIndicator;
    }

    @Override
    public boolean isHorizontalHighlightIndicatorEnabled() {
        return mDrawHorizontalHighlightIndicator;
    }

    @Override
    public float getHighlightLineWidth() {
        return mHighlightLineWidth;
    }

    public void setHighlightLineWidth(float width) {
        mHighlightLineWidth = Utils.convertDpToPixel(width);
    }

    public void enableDashedHighlightLine(float lineLength, float spaceLength, float phase) {
        mHighlightDashPathEffect = new DashPathEffect(new float[]{
                lineLength, spaceLength
        }, phase);
    }

    public void disableDashedHighlightLine() {
        mHighlightDashPathEffect = null;
    }

    public boolean isDashedHighlightLineEnabled() {
        return mHighlightDashPathEffect == null ? false : true;
    }

    @Override
    public DashPathEffect getDashPathEffectHighlight() {
        return mHighlightDashPathEffect;
    }

    protected void copy(LineScatterCandleRadarDataSet lineScatterCandleRadarDataSet) {
        super.copy(lineScatterCandleRadarDataSet);
        lineScatterCandleRadarDataSet.mDrawHorizontalHighlightIndicator = mDrawHorizontalHighlightIndicator;
        lineScatterCandleRadarDataSet.mDrawVerticalHighlightIndicator = mDrawVerticalHighlightIndicator;
        lineScatterCandleRadarDataSet.mHighlightLineWidth = mHighlightLineWidth;
        lineScatterCandleRadarDataSet.mHighlightDashPathEffect = mHighlightDashPathEffect;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\data\PieData.java =====

package com.mpdc4gsr.libunified.ui.data;

import com.mpdc4gsr.libunified.ui.highlight.Highlight;
import com.mpdc4gsr.libunified.ui.interfaces.datasets.IPieDataSet;

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


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\data\PieDataSet.java =====

package com.mpdc4gsr.libunified.ui.data;

import com.mpdc4gsr.libunified.ui.interfaces.datasets.IPieDataSet;
import com.mpdc4gsr.libunified.ui.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class PieDataSet extends DataSet<PieEntry> implements IPieDataSet {

    private float mSliceSpace = 0f;
    private boolean mAutomaticallyDisableSliceSpacing;

    private float mShift = 18f;

    private ValuePosition mXValuePosition = ValuePosition.INSIDE_SLICE;
    private ValuePosition mYValuePosition = ValuePosition.INSIDE_SLICE;
    private boolean mUsingSliceColorAsValueLineColor = false;
    private int mValueLineColor = 0xff000000;
    private float mValueLineWidth = 1.0f;
    private float mValueLinePart1OffsetPercentage = 75.f;
    private float mValueLinePart1Length = 0.3f;
    private float mValueLinePart2Length = 0.4f;
    private boolean mValueLineVariableLength = true;

    public PieDataSet(List<PieEntry> yVals, String label) {
        super(yVals, label);

    }

    @Override
    public DataSet<PieEntry> copy() {
        List<PieEntry> entries = new ArrayList<>();
        for (int i = 0; i < mValues.size(); i++) {
            entries.add(mValues.get(i).copy());
        }
        PieDataSet copied = new PieDataSet(entries, getLabel());
        copy(copied);
        return copied;
    }

    protected void copy(PieDataSet pieDataSet) {
        super.copy(pieDataSet);
    }

    @Override
    protected void calcMinMax(PieEntry e) {

        if (e == null)
            return;

        calcMinMaxY(e);
    }

    @Override
    public float getSliceSpace() {
        return mSliceSpace;
    }

    public void setSliceSpace(float spaceDp) {

        if (spaceDp > 20)
            spaceDp = 20f;
        if (spaceDp < 0)
            spaceDp = 0f;

        mSliceSpace = Utils.convertDpToPixel(spaceDp);
    }

    public void setAutomaticallyDisableSliceSpacing(boolean autoDisable) {
        mAutomaticallyDisableSliceSpacing = autoDisable;
    }

    @Override
    public boolean isAutomaticallyDisableSliceSpacingEnabled() {
        return mAutomaticallyDisableSliceSpacing;
    }

    @Override
    public float getSelectionShift() {
        return mShift;
    }

    public void setSelectionShift(float shift) {
        mShift = Utils.convertDpToPixel(shift);
    }

    @Override
    public ValuePosition getXValuePosition() {
        return mXValuePosition;
    }

    public void setXValuePosition(ValuePosition xValuePosition) {
        this.mXValuePosition = xValuePosition;
    }

    @Override
    public ValuePosition getYValuePosition() {
        return mYValuePosition;
    }

    public void setYValuePosition(ValuePosition yValuePosition) {
        this.mYValuePosition = yValuePosition;
    }

    @Override
    public boolean isUsingSliceColorAsValueLineColor() {
        return mUsingSliceColorAsValueLineColor;
    }

    public void setUsingSliceColorAsValueLineColor(boolean usingSliceColorAsValueLineColor) {
        this.mUsingSliceColorAsValueLineColor = usingSliceColorAsValueLineColor;
    }

    @Override
    public int getValueLineColor() {
        return mValueLineColor;
    }

    public void setValueLineColor(int valueLineColor) {
        this.mValueLineColor = valueLineColor;
    }

    @Override
    public float getValueLineWidth() {
        return mValueLineWidth;
    }

    public void setValueLineWidth(float valueLineWidth) {
        this.mValueLineWidth = valueLineWidth;
    }

    @Override
    public float getValueLinePart1OffsetPercentage() {
        return mValueLinePart1OffsetPercentage;
    }

    public void setValueLinePart1OffsetPercentage(float valueLinePart1OffsetPercentage) {
        this.mValueLinePart1OffsetPercentage = valueLinePart1OffsetPercentage;
    }

    @Override
    public float getValueLinePart1Length() {
        return mValueLinePart1Length;
    }

    public void setValueLinePart1Length(float valueLinePart1Length) {
        this.mValueLinePart1Length = valueLinePart1Length;
    }

    @Override
    public float getValueLinePart2Length() {
        return mValueLinePart2Length;
    }

    public void setValueLinePart2Length(float valueLinePart2Length) {
        this.mValueLinePart2Length = valueLinePart2Length;
    }

    @Override
    public boolean isValueLineVariableLength() {
        return mValueLineVariableLength;
    }

    public void setValueLineVariableLength(boolean valueLineVariableLength) {
        this.mValueLineVariableLength = valueLineVariableLength;
    }

    public enum ValuePosition {
        INSIDE_SLICE,
        OUTSIDE_SLICE
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\data\PieEntry.java =====

package com.mpdc4gsr.libunified.ui.data;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.util.Log;

@SuppressLint("ParcelCreator")
public class PieEntry extends Entry {

    private String label;

    public PieEntry(float value) {
        super(0f, value);
    }

    public PieEntry(float value, Object data) {
        super(0f, value, data);
    }

    public PieEntry(float value, Drawable icon) {
        super(0f, value, icon);
    }

    public PieEntry(float value, Drawable icon, Object data) {
        super(0f, value, icon, data);
    }

    public PieEntry(float value, String label) {
        super(0f, value);
        this.label = label;
    }

    public PieEntry(float value, String label, Object data) {
        super(0f, value, data);
        this.label = label;
    }

    public PieEntry(float value, String label, Drawable icon) {
        super(0f, value, icon);
        this.label = label;
    }

    public PieEntry(float value, String label, Drawable icon, Object data) {
        super(0f, value, icon, data);
        this.label = label;
    }

    public float getValue() {
        return getY();
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Deprecated
    @Override
    public float getX() {
        Log.i("DEPRECATED", "Pie entries do not have x values");
        return super.getX();
    }

    @Deprecated
    @Override
    public void setX(float x) {
        super.setX(x);
        Log.i("DEPRECATED", "Pie entries do not have x values");
    }

    public PieEntry copy() {
        PieEntry e = new PieEntry(getY(), label, getData());
        return e;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\data\RadarData.java =====

package com.mpdc4gsr.libunified.ui.data;

import com.mpdc4gsr.libunified.ui.highlight.Highlight;
import com.mpdc4gsr.libunified.ui.interfaces.datasets.IRadarDataSet;

import java.util.Arrays;
import java.util.List;

public class RadarData extends ChartData<IRadarDataSet> {

    private List<String> mLabels;

    public RadarData() {
        super();
    }

    public RadarData(List<IRadarDataSet> dataSets) {
        super(dataSets);
    }

    public RadarData(IRadarDataSet... dataSets) {
        super(dataSets);
    }

    public List<String> getLabels() {
        return mLabels;
    }

    public void setLabels(List<String> labels) {
        this.mLabels = labels;
    }

    public void setLabels(String... labels) {
        this.mLabels = Arrays.asList(labels);
    }

    @Override
    public Entry getEntryForHighlight(Highlight highlight) {
        return getDataSetByIndex(highlight.getDataSetIndex()).getEntryForIndex((int) highlight.getX());
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\data\RadarDataSet.java =====

package com.mpdc4gsr.libunified.ui.data;

import android.graphics.Color;

import com.mpdc4gsr.libunified.ui.interfaces.datasets.IRadarDataSet;
import com.mpdc4gsr.libunified.ui.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;

public class RadarDataSet extends LineRadarDataSet<RadarEntry> implements IRadarDataSet {

    protected boolean mDrawHighlightCircleEnabled = false;

    protected int mHighlightCircleFillColor = Color.WHITE;

    protected int mHighlightCircleStrokeColor = ColorTemplate.COLOR_NONE;

    protected int mHighlightCircleStrokeAlpha = (int) (0.3 * 255);
    protected float mHighlightCircleInnerRadius = 3.0f;
    protected float mHighlightCircleOuterRadius = 4.0f;
    protected float mHighlightCircleStrokeWidth = 2.0f;

    public RadarDataSet(List<RadarEntry> yVals, String label) {
        super(yVals, label);
    }

    @Override
    public boolean isDrawHighlightCircleEnabled() {
        return mDrawHighlightCircleEnabled;
    }

    @Override
    public void setDrawHighlightCircleEnabled(boolean enabled) {
        mDrawHighlightCircleEnabled = enabled;
    }

    @Override
    public int getHighlightCircleFillColor() {
        return mHighlightCircleFillColor;
    }

    public void setHighlightCircleFillColor(int color) {
        mHighlightCircleFillColor = color;
    }

    @Override
    public int getHighlightCircleStrokeColor() {
        return mHighlightCircleStrokeColor;
    }

    public void setHighlightCircleStrokeColor(int color) {
        mHighlightCircleStrokeColor = color;
    }

    @Override
    public int getHighlightCircleStrokeAlpha() {
        return mHighlightCircleStrokeAlpha;
    }

    public void setHighlightCircleStrokeAlpha(int alpha) {
        mHighlightCircleStrokeAlpha = alpha;
    }

    @Override
    public float getHighlightCircleInnerRadius() {
        return mHighlightCircleInnerRadius;
    }

    public void setHighlightCircleInnerRadius(float radius) {
        mHighlightCircleInnerRadius = radius;
    }

    @Override
    public float getHighlightCircleOuterRadius() {
        return mHighlightCircleOuterRadius;
    }

    public void setHighlightCircleOuterRadius(float radius) {
        mHighlightCircleOuterRadius = radius;
    }

    @Override
    public float getHighlightCircleStrokeWidth() {
        return mHighlightCircleStrokeWidth;
    }

    public void setHighlightCircleStrokeWidth(float strokeWidth) {
        mHighlightCircleStrokeWidth = strokeWidth;
    }

    @Override
    public DataSet<RadarEntry> copy() {
        List<RadarEntry> entries = new ArrayList<RadarEntry>();
        for (int i = 0; i < mValues.size(); i++) {
            entries.add(mValues.get(i).copy());
        }
        RadarDataSet copied = new RadarDataSet(entries, getLabel());
        copy(copied);
        return copied;
    }

    protected void copy(RadarDataSet radarDataSet) {
        super.copy(radarDataSet);
        radarDataSet.mDrawHighlightCircleEnabled = mDrawHighlightCircleEnabled;
        radarDataSet.mHighlightCircleFillColor = mHighlightCircleFillColor;
        radarDataSet.mHighlightCircleInnerRadius = mHighlightCircleInnerRadius;
        radarDataSet.mHighlightCircleStrokeAlpha = mHighlightCircleStrokeAlpha;
        radarDataSet.mHighlightCircleStrokeColor = mHighlightCircleStrokeColor;
        radarDataSet.mHighlightCircleStrokeWidth = mHighlightCircleStrokeWidth;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\data\RadarEntry.java =====

package com.mpdc4gsr.libunified.ui.data;

import android.annotation.SuppressLint;

@SuppressLint("ParcelCreator")
public class RadarEntry extends Entry {

    public RadarEntry(float value) {
        super(0f, value);
    }

    public RadarEntry(float value, Object data) {
        super(0f, value, data);
    }

    public float getValue() {
        return getY();
    }

    public RadarEntry copy() {
        RadarEntry e = new RadarEntry(getY(), getData());
        return e;
    }

    @Deprecated
    @Override
    public float getX() {
        return super.getX();
    }

    @Deprecated
    @Override
    public void setX(float x) {
        super.setX(x);
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\data\ScatterData.java =====

package com.mpdc4gsr.libunified.ui.data;

import com.mpdc4gsr.libunified.ui.interfaces.datasets.IScatterDataSet;

import java.util.List;

public class ScatterData extends BarLineScatterCandleBubbleData<IScatterDataSet> {

    public ScatterData() {
        super();
    }

    public ScatterData(List<IScatterDataSet> dataSets) {
        super(dataSets);
    }

    public ScatterData(IScatterDataSet... dataSets) {
        super(dataSets);
    }

    public float getGreatestShapeSize() {

        float max = 0f;

        for (IScatterDataSet set : mDataSets) {
            float size = set.getScatterShapeSize();

            if (size > max)
                max = size;
        }

        return max;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\data\ScatterDataSet.java =====

package com.mpdc4gsr.libunified.ui.data;

import com.mpdc4gsr.libunified.ui.charts.ScatterChart;
import com.mpdc4gsr.libunified.ui.interfaces.datasets.IScatterDataSet;
import com.mpdc4gsr.libunified.ui.renderer.scatter.*;
import com.mpdc4gsr.libunified.ui.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;

public class ScatterDataSet extends LineScatterCandleRadarDataSet<Entry> implements IScatterDataSet {

    protected IShapeRenderer mShapeRenderer = new SquareShapeRenderer();
    private float mShapeSize = 15f;
    private float mScatterShapeHoleRadius = 0f;

    private int mScatterShapeHoleColor = ColorTemplate.COLOR_NONE;

    public ScatterDataSet(List<Entry> yVals, String label) {
        super(yVals, label);
    }

    public static IShapeRenderer getRendererForShape(ScatterChart.ScatterShape shape) {

        switch (shape) {
            case SQUARE:
                return new SquareShapeRenderer();
            case CIRCLE:
                return new CircleShapeRenderer();
            case TRIANGLE:
                return new TriangleShapeRenderer();
            case CROSS:
                return new CrossShapeRenderer();
            case X:
                return new XShapeRenderer();
            case CHEVRON_UP:
                return new ChevronUpShapeRenderer();
            case CHEVRON_DOWN:
                return new ChevronDownShapeRenderer();
        }

        return null;
    }

    @Override
    public DataSet<Entry> copy() {
        List<Entry> entries = new ArrayList<Entry>();
        for (int i = 0; i < mValues.size(); i++) {
            entries.add(mValues.get(i).copy());
        }
        ScatterDataSet copied = new ScatterDataSet(entries, getLabel());
        copy(copied);
        return copied;
    }

    protected void copy(ScatterDataSet scatterDataSet) {
        super.copy(scatterDataSet);
        scatterDataSet.mShapeSize = mShapeSize;
        scatterDataSet.mShapeRenderer = mShapeRenderer;
        scatterDataSet.mScatterShapeHoleRadius = mScatterShapeHoleRadius;
        scatterDataSet.mScatterShapeHoleColor = mScatterShapeHoleColor;
    }

    @Override
    public float getScatterShapeSize() {
        return mShapeSize;
    }

    public void setScatterShapeSize(float size) {
        mShapeSize = size;
    }

    public void setScatterShape(ScatterChart.ScatterShape shape) {
        mShapeRenderer = getRendererForShape(shape);
    }

    @Override
    public IShapeRenderer getShapeRenderer() {
        return mShapeRenderer;
    }

    public void setShapeRenderer(IShapeRenderer shapeRenderer) {
        mShapeRenderer = shapeRenderer;
    }

    @Override
    public float getScatterShapeHoleRadius() {
        return mScatterShapeHoleRadius;
    }

    public void setScatterShapeHoleRadius(float holeRadius) {
        mScatterShapeHoleRadius = holeRadius;
    }

    @Override
    public int getScatterShapeHoleColor() {
        return mScatterShapeHoleColor;
    }

    public void setScatterShapeHoleColor(int holeColor) {
        mScatterShapeHoleColor = holeColor;
    }
}