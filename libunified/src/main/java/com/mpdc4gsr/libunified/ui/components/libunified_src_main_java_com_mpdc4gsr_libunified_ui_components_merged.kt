// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\ui\components' directory and its subdirectories.
// Total files: 11 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\components\AxisBase.java =====

package com.mpdc4gsr.libunified.ui.components;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.util.Log;

import com.mpdc4gsr.libunified.ui.formatter.DefaultAxisValueFormatter;
import com.mpdc4gsr.libunified.ui.formatter.ValueFormatter;
import com.mpdc4gsr.libunified.ui.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public abstract class AxisBase extends ComponentBase {

    public float[] mEntries = new float[]{};
    public float[] mCenteredEntries = new float[]{};
    public int mEntryCount;
    public int mDecimals;
    public float mAxisMaximum = 0f;
    public float mAxisMinimum = 0f;
    public float mAxisRange = 0f;
    protected ValueFormatter mAxisValueFormatter;
    protected float mGranularity = 1.0f;
    protected boolean mGranularityEnabled = false;
    protected boolean mForceLabels = false;
    protected boolean mDrawGridLines = true;
    protected boolean mDrawAxisLine = true;
    protected boolean mDrawLabels = true;
    protected boolean mCenterAxisLabels = false;
    protected List<LimitLine> mLimitLines;
    protected boolean mDrawLimitLineBehindData = false;
    protected boolean mDrawGridLinesBehindData = true;
    protected float mSpaceMin = 0.f;
    protected float mSpaceMax = 0.f;
    protected boolean mCustomAxisMin = false;
    protected boolean mCustomAxisMax = false;
    private int mGridColor = Color.GRAY;
    private float mGridLineWidth = 1f;
    private int mAxisLineColor = Color.GRAY;
    private float mAxisLineWidth = 1f;
    private int mLabelCount = 6;
    private DashPathEffect mAxisLineDashPathEffect = null;
    private DashPathEffect mGridDashPathEffect = null;

    public AxisBase() {
        this.mTextSize = Utils.convertDpToPixel(10f);
        this.mXOffset = Utils.convertDpToPixel(5f);
        this.mYOffset = Utils.convertDpToPixel(5f);
        this.mLimitLines = new ArrayList<LimitLine>();
    }

    public void setDrawGridLines(boolean enabled) {
        mDrawGridLines = enabled;
    }

    public boolean isDrawGridLinesEnabled() {
        return mDrawGridLines;
    }

    public void setDrawAxisLine(boolean enabled) {
        mDrawAxisLine = enabled;
    }

    public boolean isDrawAxisLineEnabled() {
        return mDrawAxisLine;
    }

    public void setCenterAxisLabels(boolean enabled) {
        mCenterAxisLabels = enabled;
    }

    public boolean isCenterAxisLabelsEnabled() {
        return mCenterAxisLabels && mEntryCount > 0;
    }

    public int getGridColor() {
        return mGridColor;
    }

    public void setGridColor(int color) {
        mGridColor = color;
    }

    public float getAxisLineWidth() {
        return mAxisLineWidth;
    }

    public void setAxisLineWidth(float width) {
        mAxisLineWidth = Utils.convertDpToPixel(width);
    }

    public float getGridLineWidth() {
        return mGridLineWidth;
    }

    public void setGridLineWidth(float width) {
        mGridLineWidth = Utils.convertDpToPixel(width);
    }

    public int getAxisLineColor() {
        return mAxisLineColor;
    }

    public void setAxisLineColor(int color) {
        mAxisLineColor = color;
    }

    public void setDrawLabels(boolean enabled) {
        mDrawLabels = enabled;
    }

    public boolean isDrawLabelsEnabled() {
        return mDrawLabels;
    }

    public void setLabelCount(int count, boolean force) {

        setLabelCount(count);
        mForceLabels = force;
    }

    public boolean isForceLabelsEnabled() {
        return mForceLabels;
    }

    public int getLabelCount() {
        return mLabelCount;
    }

    public void setLabelCount(int count) {

        if (count > 25)
            count = 25;
        if (count < 2)
            count = 2;

        mLabelCount = count;
        mForceLabels = false;
    }

    public boolean isGranularityEnabled() {
        return mGranularityEnabled;
    }

    public void setGranularityEnabled(boolean enabled) {
        mGranularityEnabled = enabled;
    }

    public float getGranularity() {
        return mGranularity;
    }

    public void setGranularity(float granularity) {
        mGranularity = granularity;

        mGranularityEnabled = true;
    }

    public void addLimitLine(LimitLine l) {
        mLimitLines.add(l);

        if (mLimitLines.size() > 6) {
            Log.e("MPAndroiChart",
                    "Warning! You have more than 6 LimitLines on your axis, do you really want " +
                            "that?");
        }
    }

    public void removeLimitLine(LimitLine l) {
        mLimitLines.remove(l);
    }

    public void removeAllLimitLines() {
        mLimitLines.clear();
    }

    public List<LimitLine> getLimitLines() {
        return mLimitLines;
    }

    public void setDrawLimitLinesBehindData(boolean enabled) {
        mDrawLimitLineBehindData = enabled;
    }

    public boolean isDrawLimitLinesBehindDataEnabled() {
        return mDrawLimitLineBehindData;
    }

    public void setDrawGridLinesBehindData(boolean enabled) {
        mDrawGridLinesBehindData = enabled;
    }

    public boolean isDrawGridLinesBehindDataEnabled() {
        return mDrawGridLinesBehindData;
    }

    public String getLongestLabel() {

        String longest = "";

        for (int i = 0; i < mEntries.length; i++) {
            String text = getFormattedLabel(i);

            if (text != null && longest.length() < text.length())
                longest = text;
        }

        return longest;
    }

    public String getFormattedLabel(int index) {

        if (index < 0 || index >= mEntries.length)
            return "";
        else
            return getValueFormatter().getAxisLabel(mEntries[index], this);
    }

    public ValueFormatter getValueFormatter() {

        if (mAxisValueFormatter == null ||
                (mAxisValueFormatter instanceof DefaultAxisValueFormatter &&
                        ((DefaultAxisValueFormatter) mAxisValueFormatter).getDecimalDigits() != mDecimals))
            mAxisValueFormatter = new DefaultAxisValueFormatter(mDecimals);

        return mAxisValueFormatter;
    }

    public void setValueFormatter(ValueFormatter f) {

        if (f == null)
            mAxisValueFormatter = new DefaultAxisValueFormatter(mDecimals);
        else
            mAxisValueFormatter = f;
    }

    public void enableGridDashedLine(float lineLength, float spaceLength, float phase) {
        mGridDashPathEffect = new DashPathEffect(new float[]{
                lineLength, spaceLength
        }, phase);
    }

    public void setGridDashedLine(DashPathEffect effect) {
        mGridDashPathEffect = effect;
    }

    public void disableGridDashedLine() {
        mGridDashPathEffect = null;
    }

    public boolean isGridDashedLineEnabled() {
        return mGridDashPathEffect == null ? false : true;
    }

    public DashPathEffect getGridDashPathEffect() {
        return mGridDashPathEffect;
    }

    public void enableAxisLineDashedLine(float lineLength, float spaceLength, float phase) {
        mAxisLineDashPathEffect = new DashPathEffect(new float[]{
                lineLength, spaceLength
        }, phase);
    }

    public void setAxisLineDashedLine(DashPathEffect effect) {
        mAxisLineDashPathEffect = effect;
    }

    public void disableAxisLineDashedLine() {
        mAxisLineDashPathEffect = null;
    }

    public boolean isAxisLineDashedLineEnabled() {
        return mAxisLineDashPathEffect == null ? false : true;
    }

    public DashPathEffect getAxisLineDashPathEffect() {
        return mAxisLineDashPathEffect;
    }

    public float getAxisMaximum() {
        return mAxisMaximum;
    }

    public void setAxisMaximum(float max) {
        mCustomAxisMax = true;
        mAxisMaximum = max;
        this.mAxisRange = Math.abs(max - mAxisMinimum);
    }

    public float getAxisMinimum() {
        return mAxisMinimum;
    }

    public void setAxisMinimum(float min) {
        mCustomAxisMin = true;
        mAxisMinimum = min;
        this.mAxisRange = Math.abs(mAxisMaximum - min);
    }

    public void resetAxisMaximum() {
        mCustomAxisMax = false;
    }

    public boolean isAxisMaxCustom() {
        return mCustomAxisMax;
    }

    public void resetAxisMinimum() {
        mCustomAxisMin = false;
    }

    public boolean isAxisMinCustom() {
        return mCustomAxisMin;
    }

    @Deprecated
    public void setAxisMinValue(float min) {
        setAxisMinimum(min);
    }

    @Deprecated
    public void setAxisMaxValue(float max) {
        setAxisMaximum(max);
    }

    public void calculate(float dataMin, float dataMax) {

        float min = mCustomAxisMin ? mAxisMinimum : (dataMin - mSpaceMin);
        float max = mCustomAxisMax ? mAxisMaximum : (dataMax + mSpaceMax);

        float range = Math.abs(max - min);

        if (range == 0f) {
            max = max + 1f;
            min = min - 1f;
        }

        this.mAxisMinimum = min;
        this.mAxisMaximum = max;

        this.mAxisRange = Math.abs(max - min);
    }

    public float getSpaceMin() {
        return mSpaceMin;
    }

    public void setSpaceMin(float mSpaceMin) {
        this.mSpaceMin = mSpaceMin;
    }

    public float getSpaceMax() {
        return mSpaceMax;
    }

    public void setSpaceMax(float mSpaceMax) {
        this.mSpaceMax = mSpaceMax;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\components\ComponentBase.java =====

package com.mpdc4gsr.libunified.ui.components;

import android.graphics.Color;
import android.graphics.Typeface;

import com.mpdc4gsr.libunified.ui.utils.Utils;

public abstract class ComponentBase {

    protected boolean mEnabled = true;

    protected float mXOffset = 5f;

    protected float mYOffset = 5f;

    protected Typeface mTypeface = null;

    protected float mTextSize = Utils.convertDpToPixel(10f);

    protected int mTextColor = Color.BLACK;

    public ComponentBase() {

    }

    public float getXOffset() {
        return mXOffset;
    }

    public void setXOffset(float xOffset) {
        mXOffset = Utils.convertDpToPixel(xOffset);
    }

    public float getYOffset() {
        return mYOffset;
    }

    public void setYOffset(float yOffset) {
        mYOffset = Utils.convertDpToPixel(yOffset);
    }

    public Typeface getTypeface() {
        return mTypeface;
    }

    public void setTypeface(Typeface tf) {
        mTypeface = tf;
    }

    public float getTextSize() {
        return mTextSize;
    }

    public void setTextSize(float size) {

        if (size > 24f)
            size = 24f;
        if (size < 6f)
            size = 6f;

        mTextSize = Utils.convertDpToPixel(size);
    }

    public int getTextColor() {
        return mTextColor;
    }

    public void setTextColor(int color) {
        mTextColor = color;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\components\Description.java =====

package com.mpdc4gsr.libunified.ui.components;

import android.graphics.Paint;

import com.mpdc4gsr.libunified.ui.utils.MPPointF;
import com.mpdc4gsr.libunified.ui.utils.Utils;

public class Description extends ComponentBase {

    private String text = "Description Label";

    private MPPointF mPosition;

    private Paint.Align mTextAlign = Paint.Align.RIGHT;

    public Description() {
        super();

        mTextSize = Utils.convertDpToPixel(8f);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setPosition(float x, float y) {
        if (mPosition == null) {
            mPosition = MPPointF.getInstance(x, y);
        } else {
            mPosition.x = x;
            mPosition.y = y;
        }
    }

    public MPPointF getPosition() {
        return mPosition;
    }

    public Paint.Align getTextAlign() {
        return mTextAlign;
    }

    public void setTextAlign(Paint.Align align) {
        this.mTextAlign = align;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\components\IMarker.java =====

package com.mpdc4gsr.libunified.ui.components;

import android.graphics.Canvas;

import com.mpdc4gsr.libunified.ui.data.Entry;
import com.mpdc4gsr.libunified.ui.highlight.Highlight;
import com.mpdc4gsr.libunified.ui.utils.MPPointF;

public interface IMarker {

    MPPointF getOffset();

    MPPointF getOffsetForDrawingAtPoint(float posX, float posY);

    void refreshContent(Entry e, Highlight highlight);

    void draw(Canvas canvas, float posX, float posY);
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\components\Legend.java =====

package com.mpdc4gsr.libunified.ui.components;

import android.graphics.DashPathEffect;
import android.graphics.Paint;

import com.mpdc4gsr.libunified.ui.utils.ColorTemplate;
import com.mpdc4gsr.libunified.ui.utils.FSize;
import com.mpdc4gsr.libunified.ui.utils.Utils;
import com.mpdc4gsr.libunified.ui.utils.ViewPortHandler;

import java.util.ArrayList;
import java.util.List;

public class Legend extends ComponentBase {

    public float mNeededWidth = 0f;
    public float mNeededHeight = 0f;
    public float mTextHeightMax = 0f;
    public float mTextWidthMax = 0f;
    private LegendEntry[] mEntries = new LegendEntry[]{};
    private LegendEntry[] mExtraEntries;
    private boolean mIsLegendCustom = false;
    private LegendHorizontalAlignment mHorizontalAlignment = LegendHorizontalAlignment.LEFT;
    private LegendVerticalAlignment mVerticalAlignment = LegendVerticalAlignment.BOTTOM;
    private LegendOrientation mOrientation = LegendOrientation.HORIZONTAL;
    private boolean mDrawInside = false;
    private LegendDirection mDirection = LegendDirection.LEFT_TO_RIGHT;
    private LegendForm mShape = LegendForm.SQUARE;
    private float mFormSize = 8f;
    private float mFormLineWidth = 3f;
    private DashPathEffect mFormLineDashEffect = null;
    private float mXEntrySpace = 6f;
    private float mYEntrySpace = 0f;
    private float mFormToTextSpace = 5f;
    private float mStackSpace = 3f;
    private float mMaxSizePercent = 0.95f;
    private boolean mWordWrapEnabled = false;
    private List<FSize> mCalculatedLabelSizes = new ArrayList<>(16);
    private List<Boolean> mCalculatedLabelBreakPoints = new ArrayList<>(16);
    private List<FSize> mCalculatedLineSizes = new ArrayList<>(16);

    public Legend() {

        this.mTextSize = Utils.convertDpToPixel(10f);
        this.mXOffset = Utils.convertDpToPixel(5f);
        this.mYOffset = Utils.convertDpToPixel(3f);
    }

    public Legend(LegendEntry[] entries) {
        this();

        if (entries == null) {
            throw new IllegalArgumentException("entries array is NULL");
        }

        this.mEntries = entries;
    }

    public LegendEntry[] getEntries() {
        return mEntries;
    }

    public void setEntries(List<LegendEntry> entries) {
        mEntries = entries.toArray(new LegendEntry[entries.size()]);
    }

    public float getMaximumEntryWidth(Paint p) {

        float max = 0f;
        float maxFormSize = 0f;
        float formToTextSpace = Utils.convertDpToPixel(mFormToTextSpace);

        for (LegendEntry entry : mEntries) {
            final float formSize = Utils.convertDpToPixel(
                    Float.isNaN(entry.formSize)
                            ? mFormSize : entry.formSize);
            if (formSize > maxFormSize)
                maxFormSize = formSize;

            String label = entry.label;
            if (label == null) continue;

            float length = (float) Utils.calcTextWidth(p, label);

            if (length > max)
                max = length;
        }

        return max + maxFormSize + formToTextSpace;
    }

    public float getMaximumEntryHeight(Paint p) {

        float max = 0f;

        for (LegendEntry entry : mEntries) {
            String label = entry.label;
            if (label == null) continue;

            float length = (float) Utils.calcTextHeight(p, label);

            if (length > max)
                max = length;
        }

        return max;
    }

    public LegendEntry[] getExtraEntries() {

        return mExtraEntries;
    }

    public void setExtra(List<LegendEntry> entries) {
        mExtraEntries = entries.toArray(new LegendEntry[entries.size()]);
    }

    public void setExtra(LegendEntry[] entries) {
        if (entries == null)
            entries = new LegendEntry[]{};
        mExtraEntries = entries;
    }

    public void setExtra(int[] colors, String[] labels) {

        List<LegendEntry> entries = new ArrayList<>();

        for (int i = 0; i < Math.min(colors.length, labels.length); i++) {
            final LegendEntry entry = new LegendEntry();
            entry.formColor = colors[i];
            entry.label = labels[i];

            if (entry.formColor == ColorTemplate.COLOR_SKIP ||
                    entry.formColor == 0)
                entry.form = LegendForm.NONE;
            else if (entry.formColor == ColorTemplate.COLOR_NONE)
                entry.form = LegendForm.EMPTY;

            entries.add(entry);
        }

        mExtraEntries = entries.toArray(new LegendEntry[entries.size()]);
    }

    public void setCustom(LegendEntry[] entries) {

        mEntries = entries;
        mIsLegendCustom = true;
    }

    public void setCustom(List<LegendEntry> entries) {

        mEntries = entries.toArray(new LegendEntry[entries.size()]);
        mIsLegendCustom = true;
    }

    public void resetCustom() {
        mIsLegendCustom = false;
    }

    public boolean isLegendCustom() {
        return mIsLegendCustom;
    }

    public LegendHorizontalAlignment getHorizontalAlignment() {
        return mHorizontalAlignment;
    }

    public void setHorizontalAlignment(LegendHorizontalAlignment value) {
        mHorizontalAlignment = value;
    }

    public LegendVerticalAlignment getVerticalAlignment() {
        return mVerticalAlignment;
    }

    public void setVerticalAlignment(LegendVerticalAlignment value) {
        mVerticalAlignment = value;
    }

    public LegendOrientation getOrientation() {
        return mOrientation;
    }

    public void setOrientation(LegendOrientation value) {
        mOrientation = value;
    }

    public boolean isDrawInsideEnabled() {
        return mDrawInside;
    }

    public void setDrawInside(boolean value) {
        mDrawInside = value;
    }

    public LegendDirection getDirection() {
        return mDirection;
    }

    public void setDirection(LegendDirection pos) {
        mDirection = pos;
    }

    public LegendForm getForm() {
        return mShape;
    }

    public void setForm(LegendForm shape) {
        mShape = shape;
    }

    public float getFormSize() {
        return mFormSize;
    }

    public void setFormSize(float size) {
        mFormSize = size;
    }

    public float getFormLineWidth() {
        return mFormLineWidth;
    }

    public void setFormLineWidth(float size) {
        mFormLineWidth = size;
    }

    public DashPathEffect getFormLineDashEffect() {
        return mFormLineDashEffect;
    }

    public void setFormLineDashEffect(DashPathEffect dashPathEffect) {
        mFormLineDashEffect = dashPathEffect;
    }

    public float getXEntrySpace() {
        return mXEntrySpace;
    }

    public void setXEntrySpace(float space) {
        mXEntrySpace = space;
    }

    public float getYEntrySpace() {
        return mYEntrySpace;
    }

    public void setYEntrySpace(float space) {
        mYEntrySpace = space;
    }

    public float getFormToTextSpace() {
        return mFormToTextSpace;
    }

    public void setFormToTextSpace(float space) {
        this.mFormToTextSpace = space;
    }

    public float getStackSpace() {
        return mStackSpace;
    }

    public void setStackSpace(float space) {
        mStackSpace = space;
    }

    public boolean isWordWrapEnabled() {
        return mWordWrapEnabled;
    }

    public void setWordWrapEnabled(boolean enabled) {
        mWordWrapEnabled = enabled;
    }

    public float getMaxSizePercent() {
        return mMaxSizePercent;
    }

    public void setMaxSizePercent(float maxSize) {
        mMaxSizePercent = maxSize;
    }

    public List<FSize> getCalculatedLabelSizes() {
        return mCalculatedLabelSizes;
    }

    public List<Boolean> getCalculatedLabelBreakPoints() {
        return mCalculatedLabelBreakPoints;
    }

    public List<FSize> getCalculatedLineSizes() {
        return mCalculatedLineSizes;
    }

    public void calculateDimensions(Paint labelpaint, ViewPortHandler viewPortHandler) {

        float defaultFormSize = Utils.convertDpToPixel(mFormSize);
        float stackSpace = Utils.convertDpToPixel(mStackSpace);
        float formToTextSpace = Utils.convertDpToPixel(mFormToTextSpace);
        float xEntrySpace = Utils.convertDpToPixel(mXEntrySpace);
        float yEntrySpace = Utils.convertDpToPixel(mYEntrySpace);
        boolean wordWrapEnabled = mWordWrapEnabled;
        LegendEntry[] entries = mEntries;
        int entryCount = entries.length;

        mTextWidthMax = getMaximumEntryWidth(labelpaint);
        mTextHeightMax = getMaximumEntryHeight(labelpaint);

        switch (mOrientation) {
            case VERTICAL: {

                float maxWidth = 0f, maxHeight = 0f, width = 0f;
                float labelLineHeight = Utils.getLineHeight(labelpaint);
                boolean wasStacked = false;

                for (int i = 0; i < entryCount; i++) {

                    LegendEntry e = entries[i];
                    boolean drawingForm = e.form != LegendForm.NONE;
                    float formSize = Float.isNaN(e.formSize)
                            ? defaultFormSize
                            : Utils.convertDpToPixel(e.formSize);
                    String label = e.label;

                    if (!wasStacked)
                        width = 0.f;

                    if (drawingForm) {
                        if (wasStacked)
                            width += stackSpace;
                        width += formSize;
                    }

                    if (label != null) {

                        if (drawingForm && !wasStacked)
                            width += formToTextSpace;
                        else if (wasStacked) {
                            maxWidth = Math.max(maxWidth, width);
                            maxHeight += labelLineHeight + yEntrySpace;
                            width = 0.f;
                            wasStacked = false;
                        }

                        width += Utils.calcTextWidth(labelpaint, label);

                        if (i < entryCount - 1)
                            maxHeight += labelLineHeight + yEntrySpace;
                    } else {
                        wasStacked = true;
                        width += formSize;
                        if (i < entryCount - 1)
                            width += stackSpace;
                    }

                    maxWidth = Math.max(maxWidth, width);
                }

                mNeededWidth = maxWidth;
                mNeededHeight = maxHeight;

                break;
            }
            case HORIZONTAL: {

                float labelLineHeight = Utils.getLineHeight(labelpaint);
                float labelLineSpacing = Utils.getLineSpacing(labelpaint) + yEntrySpace;
                float contentWidth = viewPortHandler.contentWidth() * mMaxSizePercent;

                float maxLineWidth = 0.f;
                float currentLineWidth = 0.f;
                float requiredWidth = 0.f;
                int stackedStartIndex = -1;

                mCalculatedLabelBreakPoints.clear();
                mCalculatedLabelSizes.clear();
                mCalculatedLineSizes.clear();

                for (int i = 0; i < entryCount; i++) {

                    LegendEntry e = entries[i];
                    boolean drawingForm = e.form != LegendForm.NONE;
                    float formSize = Float.isNaN(e.formSize)
                            ? defaultFormSize
                            : Utils.convertDpToPixel(e.formSize);
                    String label = e.label;

                    mCalculatedLabelBreakPoints.add(false);

                    if (stackedStartIndex == -1) {

                        requiredWidth = 0.f;
                    } else {

                        requiredWidth += stackSpace;
                    }

                    if (label != null) {

                        mCalculatedLabelSizes.add(Utils.calcTextSize(labelpaint, label));
                        requiredWidth += drawingForm ? formToTextSpace + formSize : 0.f;
                        requiredWidth += mCalculatedLabelSizes.get(i).width;
                    } else {

                        mCalculatedLabelSizes.add(FSize.getInstance(0.f, 0.f));
                        requiredWidth += drawingForm ? formSize : 0.f;

                        if (stackedStartIndex == -1) {

                            stackedStartIndex = i;
                        }
                    }

                    if (label != null || i == entryCount - 1) {

                        float requiredSpacing = currentLineWidth == 0.f ? 0.f : xEntrySpace;

                        if (!wordWrapEnabled

                                || currentLineWidth == 0.f

                                || (contentWidth - currentLineWidth >=
                                requiredSpacing + requiredWidth)) {

                            currentLineWidth += requiredSpacing + requiredWidth;
                        } else {

                            mCalculatedLineSizes.add(FSize.getInstance(currentLineWidth, labelLineHeight));
                            maxLineWidth = Math.max(maxLineWidth, currentLineWidth);

                            mCalculatedLabelBreakPoints.set(
                                    stackedStartIndex > -1 ? stackedStartIndex
                                            : i, true);
                            currentLineWidth = requiredWidth;
                        }

                        if (i == entryCount - 1) {

                            mCalculatedLineSizes.add(FSize.getInstance(currentLineWidth, labelLineHeight));
                            maxLineWidth = Math.max(maxLineWidth, currentLineWidth);
                        }
                    }

                    stackedStartIndex = label != null ? -1 : stackedStartIndex;
                }

                mNeededWidth = maxLineWidth;
                mNeededHeight = labelLineHeight
                        * (float) (mCalculatedLineSizes.size())
                        + labelLineSpacing *
                        (float) (mCalculatedLineSizes.size() == 0
                                ? 0
                                : (mCalculatedLineSizes.size() - 1));

                break;
            }
        }

        mNeededHeight += mYOffset;
        mNeededWidth += mXOffset;
    }

    public enum LegendForm {

        NONE,

        EMPTY,

        DEFAULT,

        SQUARE,

        CIRCLE,

        LINE
    }

    public enum LegendHorizontalAlignment {
        LEFT, CENTER, RIGHT
    }

    public enum LegendVerticalAlignment {
        TOP, CENTER, BOTTOM
    }

    public enum LegendOrientation {
        HORIZONTAL, VERTICAL
    }

    public enum LegendDirection {
        LEFT_TO_RIGHT, RIGHT_TO_LEFT
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\components\LegendEntry.java =====

package com.mpdc4gsr.libunified.ui.components;

import android.graphics.DashPathEffect;

import com.mpdc4gsr.libunified.ui.utils.ColorTemplate;

public class LegendEntry {
    public String label;
    public Legend.LegendForm form = Legend.LegendForm.DEFAULT;
    public float formSize = Float.NaN;
    public float formLineWidth = Float.NaN;
    public DashPathEffect formLineDashEffect = null;
    public int formColor = ColorTemplate.COLOR_NONE;

    public LegendEntry() {

    }

    public LegendEntry(String label,
                       Legend.LegendForm form,
                       float formSize,
                       float formLineWidth,
                       DashPathEffect formLineDashEffect,
                       int formColor) {
        this.label = label;
        this.form = form;
        this.formSize = formSize;
        this.formLineWidth = formLineWidth;
        this.formLineDashEffect = formLineDashEffect;
        this.formColor = formColor;
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\components\LimitLine.java =====

package com.mpdc4gsr.libunified.ui.components;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;

import com.mpdc4gsr.libunified.ui.utils.Utils;

public class LimitLine extends ComponentBase {

    private float mLimit = 0f;

    private float mLineWidth = 2f;

    private int mLineColor = Color.rgb(237, 91, 91);

    private Paint.Style mTextStyle = Paint.Style.FILL_AND_STROKE;

    private String mLabel = "";

    private DashPathEffect mDashPathEffect = null;

    private LimitLabelPosition mLabelPosition = LimitLabelPosition.RIGHT_TOP;

    public LimitLine(float limit) {
        mLimit = limit;
    }

    public LimitLine(float limit, String label) {
        mLimit = limit;
        mLabel = label;
    }

    public float getLimit() {
        return mLimit;
    }

    public float getLineWidth() {
        return mLineWidth;
    }

    public void setLineWidth(float width) {

        if (width < 0.2f)
            width = 0.2f;
        if (width > 12.0f)
            width = 12.0f;
        mLineWidth = Utils.convertDpToPixel(width);
    }

    public int getLineColor() {
        return mLineColor;
    }

    public void setLineColor(int color) {
        mLineColor = color;
    }

    public void enableDashedLine(float lineLength, float spaceLength, float phase) {
        mDashPathEffect = new DashPathEffect(new float[]{
                lineLength, spaceLength
        }, phase);
    }

    public void disableDashedLine() {
        mDashPathEffect = null;
    }

    public boolean isDashedLineEnabled() {
        return mDashPathEffect == null ? false : true;
    }

    public DashPathEffect getDashPathEffect() {
        return mDashPathEffect;
    }

    public Paint.Style getTextStyle() {
        return mTextStyle;
    }

    public void setTextStyle(Paint.Style style) {
        this.mTextStyle = style;
    }

    public LimitLabelPosition getLabelPosition() {
        return mLabelPosition;
    }

    public void setLabelPosition(LimitLabelPosition pos) {
        mLabelPosition = pos;
    }

    public String getLabel() {
        return mLabel;
    }

    public void setLabel(String label) {
        mLabel = label;
    }

    public enum LimitLabelPosition {
        LEFT_TOP, LEFT_BOTTOM, RIGHT_TOP, RIGHT_BOTTOM
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\components\MarkerImage.java =====

package com.mpdc4gsr.libunified.ui.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.core.content.res.ResourcesCompat;

import com.mpdc4gsr.libunified.ui.charts.Chart;
import com.mpdc4gsr.libunified.ui.data.Entry;
import com.mpdc4gsr.libunified.ui.highlight.Highlight;
import com.mpdc4gsr.libunified.ui.utils.FSize;
import com.mpdc4gsr.libunified.ui.utils.MPPointF;

import java.lang.ref.WeakReference;

public class MarkerImage implements IMarker {

    private Context mContext;
    private Drawable mDrawable;

    private MPPointF mOffset = new MPPointF();
    private MPPointF mOffset2 = new MPPointF();
    private WeakReference<Chart> mWeakChart;

    private FSize mSize = new FSize();
    private Rect mDrawableBoundsCache = new Rect();

    public MarkerImage(Context context, int drawableResourceId) {
        mContext = context;
        mDrawable = ResourcesCompat.getDrawable(mContext.getResources(), drawableResourceId, mContext.getTheme());
    }

    public void setOffset(float offsetX, float offsetY) {
        mOffset.x = offsetX;
        mOffset.y = offsetY;
    }

    @Override
    public MPPointF getOffset() {
        return mOffset;
    }

    public void setOffset(MPPointF offset) {
        mOffset = offset;

        if (mOffset == null) {
            mOffset = new MPPointF();
        }
    }

    public FSize getSize() {
        return mSize;
    }

    public void setSize(FSize size) {
        mSize = size;

        if (mSize == null) {
            mSize = new FSize();
        }
    }

    public Chart getChartView() {
        return mWeakChart == null ? null : mWeakChart.get();
    }

    public void setChartView(Chart chart) {
        mWeakChart = new WeakReference<>(chart);
    }

    @Override
    public MPPointF getOffsetForDrawingAtPoint(float posX, float posY) {

        MPPointF offset = getOffset();
        mOffset2.x = offset.x;
        mOffset2.y = offset.y;

        Chart chart = getChartView();

        float width = mSize.width;
        float height = mSize.height;

        if (width == 0.f && mDrawable != null) {
            width = mDrawable.getIntrinsicWidth();
        }
        if (height == 0.f && mDrawable != null) {
            height = mDrawable.getIntrinsicHeight();
        }

        if (posX + mOffset2.x < 0) {
            mOffset2.x = -posX;
        } else if (chart != null && posX + width + mOffset2.x > chart.getWidth()) {
            mOffset2.x = chart.getWidth() - posX - width;
        }

        if (posY + mOffset2.y < 0) {
            mOffset2.y = -posY;
        } else if (chart != null && posY + height + mOffset2.y > chart.getHeight()) {
            mOffset2.y = chart.getHeight() - posY - height;
        }

        return mOffset2;
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {

    }

    @Override
    public void draw(Canvas canvas, float posX, float posY) {

        if (mDrawable == null) return;

        MPPointF offset = getOffsetForDrawingAtPoint(posX, posY);

        float width = mSize.width;
        float height = mSize.height;

        if (width == 0.f) {
            width = mDrawable.getIntrinsicWidth();
        }
        if (height == 0.f) {
            height = mDrawable.getIntrinsicHeight();
        }

        mDrawable.copyBounds(mDrawableBoundsCache);
        mDrawable.setBounds(
                mDrawableBoundsCache.left,
                mDrawableBoundsCache.top,
                mDrawableBoundsCache.left + (int) width,
                mDrawableBoundsCache.top + (int) height);

        int saveId = canvas.save();

        canvas.translate(posX + offset.x, posY + offset.y);
        mDrawable.draw(canvas);
        canvas.restoreToCount(saveId);

        mDrawable.setBounds(mDrawableBoundsCache);
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\components\MarkerView.java =====

package com.mpdc4gsr.libunified.ui.components;

import android.content.Context;
import android.graphics.Canvas;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import com.mpdc4gsr.libunified.ui.charts.Chart;
import com.mpdc4gsr.libunified.ui.data.Entry;
import com.mpdc4gsr.libunified.ui.highlight.Highlight;
import com.mpdc4gsr.libunified.ui.utils.MPPointF;

import java.lang.ref.WeakReference;

public class MarkerView extends RelativeLayout implements IMarker {

    private MPPointF mOffset = new MPPointF();
    private MPPointF mOffset2 = new MPPointF();
    private WeakReference<Chart> mWeakChart;

    public MarkerView(Context context, int layoutResource) {
        super(context);
        setupLayoutResource(layoutResource);
    }

    private void setupLayoutResource(int layoutResource) {

        View inflated = LayoutInflater.from(getContext()).inflate(layoutResource, this);

        inflated.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        inflated.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));

        inflated.layout(0, 0, inflated.getMeasuredWidth(), inflated.getMeasuredHeight());
    }

    public void setOffset(float offsetX, float offsetY) {
        mOffset.x = offsetX;
        mOffset.y = offsetY;
    }

    @Override
    public MPPointF getOffset() {
        return mOffset;
    }

    public void setOffset(MPPointF offset) {
        mOffset = offset;

        if (mOffset == null) {
            mOffset = new MPPointF();
        }
    }

    public Chart getChartView() {
        return mWeakChart == null ? null : mWeakChart.get();
    }

    public void setChartView(Chart chart) {
        mWeakChart = new WeakReference<>(chart);
    }

    @Override
    public MPPointF getOffsetForDrawingAtPoint(float posX, float posY) {

        MPPointF offset = getOffset();
        mOffset2.x = offset.x;
        mOffset2.y = offset.y;

        Chart chart = getChartView();

        float width = getWidth();
        float height = getHeight();

        if (posX + mOffset2.x < 0) {
            mOffset2.x = -posX;
        } else if (chart != null && posX + width + mOffset2.x > chart.getWidth()) {
            mOffset2.x = chart.getWidth() - posX - width;
        }

        if (posY + mOffset2.y < 0) {
            mOffset2.y = -posY;
        } else if (chart != null && posY + height + mOffset2.y > chart.getHeight()) {
            mOffset2.y = chart.getHeight() - posY - height;
        }

        return mOffset2;
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {

        measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        layout(0, 0, getMeasuredWidth(), getMeasuredHeight());

    }

    @Override
    public void draw(Canvas canvas, float posX, float posY) {

        MPPointF offset = getOffsetForDrawingAtPoint(posX, posY);

        int saveId = canvas.save();

        canvas.translate(posX + offset.x, posY + offset.y);
        draw(canvas);
        canvas.restoreToCount(saveId);
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\components\XAxis.java =====

package com.mpdc4gsr.libunified.ui.components;

import com.mpdc4gsr.libunified.ui.utils.Utils;

public class XAxis extends AxisBase {

    public int mLabelWidth = 1;

    public int mLabelHeight = 1;

    public int mLabelRotatedWidth = 1;

    public int mLabelRotatedHeight = 1;

    protected float mLabelRotationAngle = 0f;

    private boolean mAvoidFirstLastClipping = false;

    private XAxisPosition mPosition = XAxisPosition.TOP;
    private boolean isJumpFirstLabel = true;

    public XAxis() {
        super();

        mYOffset = Utils.convertDpToPixel(4.f);
    }

    public XAxisPosition getPosition() {
        return mPosition;
    }

    public void setPosition(XAxisPosition pos) {
        mPosition = pos;
    }

    public float getLabelRotationAngle() {
        return mLabelRotationAngle;
    }

    public void setLabelRotationAngle(float angle) {
        mLabelRotationAngle = angle;
    }

    public void setAvoidFirstLastClipping(boolean enabled) {
        mAvoidFirstLastClipping = enabled;
    }

    public boolean isAvoidFirstLastClippingEnabled() {
        return mAvoidFirstLastClipping;
    }

    public boolean isJumpFirstLabel() {
        return isJumpFirstLabel;
    }

    public void setJumpFirstLabel(boolean jumpFirstLabel) {
        isJumpFirstLabel = jumpFirstLabel;
    }

    public enum XAxisPosition {
        TOP, BOTTOM, BOTH_SIDED, TOP_INSIDE, BOTTOM_INSIDE
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\components\YAxis.java =====

package com.mpdc4gsr.libunified.ui.components;

import android.graphics.Color;
import android.graphics.Paint;

import com.mpdc4gsr.libunified.ui.utils.Utils;

public class YAxis extends AxisBase {

    protected boolean mInverted = false;
    protected boolean mDrawZeroLine = false;
    protected int mZeroLineColor = Color.GRAY;
    protected float mZeroLineWidth = 1f;
    protected float mSpacePercentTop = 10f;
    protected float mSpacePercentBottom = 10f;
    protected float mMinWidth = 0.f;
    protected float mMaxWidth = Float.POSITIVE_INFINITY;
    private boolean mDrawBottomYLabelEntry = true;
    private boolean mDrawTopYLabelEntry = true;
    private boolean mUseAutoScaleRestrictionMin = false;
    private boolean mUseAutoScaleRestrictionMax = false;
    private YAxisLabelPosition mPosition = YAxisLabelPosition.OUTSIDE_CHART;
    private AxisDependency mAxisDependency;

    public YAxis() {
        super();

        this.mAxisDependency = AxisDependency.LEFT;
        this.mYOffset = 0f;
    }

    public YAxis(AxisDependency position) {
        super();
        this.mAxisDependency = position;
        this.mYOffset = 0f;
    }

    public AxisDependency getAxisDependency() {
        return mAxisDependency;
    }

    public float getMinWidth() {
        return mMinWidth;
    }

    public void setMinWidth(float minWidth) {
        mMinWidth = minWidth;
    }

    public float getMaxWidth() {
        return mMaxWidth;
    }

    public void setMaxWidth(float maxWidth) {
        mMaxWidth = maxWidth;
    }

    public YAxisLabelPosition getLabelPosition() {
        return mPosition;
    }

    public void setPosition(YAxisLabelPosition pos) {
        mPosition = pos;
    }

    public boolean isDrawTopYLabelEntryEnabled() {
        return mDrawTopYLabelEntry;
    }

    public boolean isDrawBottomYLabelEntryEnabled() {
        return mDrawBottomYLabelEntry;
    }

    public void setDrawTopYLabelEntry(boolean enabled) {
        mDrawTopYLabelEntry = enabled;
    }

    public boolean isInverted() {
        return mInverted;
    }

    public void setInverted(boolean enabled) {
        mInverted = enabled;
    }

    @Deprecated
    public void setStartAtZero(boolean startAtZero) {
        if (startAtZero)
            setAxisMinimum(0f);
        else
            resetAxisMinimum();
    }

    public float getSpaceTop() {
        return mSpacePercentTop;
    }

    public void setSpaceTop(float percent) {
        mSpacePercentTop = percent;
    }

    public float getSpaceBottom() {
        return mSpacePercentBottom;
    }

    public void setSpaceBottom(float percent) {
        mSpacePercentBottom = percent;
    }

    public boolean isDrawZeroLineEnabled() {
        return mDrawZeroLine;
    }

    public void setDrawZeroLine(boolean mDrawZeroLine) {
        this.mDrawZeroLine = mDrawZeroLine;
    }

    public int getZeroLineColor() {
        return mZeroLineColor;
    }

    public void setZeroLineColor(int color) {
        mZeroLineColor = color;
    }

    public float getZeroLineWidth() {
        return mZeroLineWidth;
    }

    public void setZeroLineWidth(float width) {
        this.mZeroLineWidth = Utils.convertDpToPixel(width);
    }

    public float getRequiredWidthSpace(Paint p) {

        p.setTextSize(mTextSize);

        String label = getLongestLabel();
        float width = (float) Utils.calcTextWidth(p, label) + getXOffset() * 2f;

        float minWidth = getMinWidth();
        float maxWidth = getMaxWidth();

        if (minWidth > 0.f)
            minWidth = Utils.convertDpToPixel(minWidth);

        if (maxWidth > 0.f && maxWidth != Float.POSITIVE_INFINITY)
            maxWidth = Utils.convertDpToPixel(maxWidth);

        width = Math.max(minWidth, Math.min(width, maxWidth > 0.0 ? maxWidth : width));

        return width;
    }

    public float getRequiredHeightSpace(Paint p) {

        p.setTextSize(mTextSize);

        String label = getLongestLabel();
        return (float) Utils.calcTextHeight(p, label) + getYOffset() * 2f;
    }

    public boolean needsOffset() {
        if (isEnabled() && isDrawLabelsEnabled() && getLabelPosition() == YAxisLabelPosition
                .OUTSIDE_CHART)
            return true;
        else
            return false;
    }

    @Deprecated
    public boolean isUseAutoScaleMinRestriction() {
        return mUseAutoScaleRestrictionMin;
    }

    @Deprecated
    public void setUseAutoScaleMinRestriction(boolean isEnabled) {
        mUseAutoScaleRestrictionMin = isEnabled;
    }

    @Deprecated
    public boolean isUseAutoScaleMaxRestriction() {
        return mUseAutoScaleRestrictionMax;
    }

    @Deprecated
    public void setUseAutoScaleMaxRestriction(boolean isEnabled) {
        mUseAutoScaleRestrictionMax = isEnabled;
    }

    @Override
    public void calculate(float dataMin, float dataMax) {

        float min = dataMin;
        float max = dataMax;

        float range = Math.abs(max - min);

        if (range == 0f) {
            max = max + 1f;
            min = min - 1f;
        }

        range = Math.abs(max - min);

        this.mAxisMinimum = mCustomAxisMin ? this.mAxisMinimum : min - (range / 100f) * getSpaceBottom();
        this.mAxisMaximum = mCustomAxisMax ? this.mAxisMaximum : max + (range / 100f) * getSpaceTop();

        this.mAxisRange = Math.abs(this.mAxisMinimum - this.mAxisMaximum);
    }

    public enum YAxisLabelPosition {
        OUTSIDE_CHART, INSIDE_CHART
    }

    public enum AxisDependency {
        LEFT, RIGHT
    }
}