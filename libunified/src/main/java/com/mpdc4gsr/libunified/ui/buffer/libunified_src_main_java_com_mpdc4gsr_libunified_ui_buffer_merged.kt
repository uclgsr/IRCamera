// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\ui\buffer' directory and its subdirectories.
// Total files: 3 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\buffer\AbstractBuffer.java =====

package com.mpdc4gsr.libunified.ui.buffer;

public abstract class AbstractBuffer<T> {

    public final float[] buffer;

    protected int index = 0;

    protected float phaseX = 1f;

    protected float phaseY = 1f;

    protected int mFrom = 0;

    protected int mTo = 0;

    public AbstractBuffer(int size) {
        index = 0;
        buffer = new float[size];
    }

    public void limitFrom(int from) {
        if (from < 0)
            from = 0;
        mFrom = from;
    }

    public void limitTo(int to) {
        if (to < 0)
            to = 0;
        mTo = to;
    }

    public void reset() {
        index = 0;
    }

    public int size() {
        return buffer.length;
    }

    public void setPhases(float phaseX, float phaseY) {
        this.phaseX = phaseX;
        this.phaseY = phaseY;
    }

    public abstract void feed(T data);
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\buffer\BarBuffer.java =====

package com.mpdc4gsr.libunified.ui.buffer;

import com.mpdc4gsr.libunified.ui.data.BarEntry;
import com.mpdc4gsr.libunified.ui.interfaces.datasets.IBarDataSet;

public class BarBuffer extends AbstractBuffer<IBarDataSet> {

    protected int mDataSetIndex = 0;
    protected int mDataSetCount = 1;
    protected boolean mContainsStacks = false;
    protected boolean mInverted = false;

    protected float mBarWidth = 1f;

    public BarBuffer(int size, int dataSetCount, boolean containsStacks) {
        super(size);
        this.mDataSetCount = dataSetCount;
        this.mContainsStacks = containsStacks;
    }

    public void setBarWidth(float barWidth) {
        this.mBarWidth = barWidth;
    }

    public void setDataSet(int index) {
        this.mDataSetIndex = index;
    }

    public void setInverted(boolean inverted) {
        this.mInverted = inverted;
    }

    protected void addBar(float left, float top, float right, float bottom) {

        buffer[index++] = left;
        buffer[index++] = top;
        buffer[index++] = right;
        buffer[index++] = bottom;
    }

    @Override
    public void feed(IBarDataSet data) {

        float size = data.getEntryCount() * phaseX;
        float barWidthHalf = mBarWidth / 2f;

        for (int i = 0; i < size; i++) {

            BarEntry e = data.getEntryForIndex(i);

            if (e == null)
                continue;

            float x = e.getX();
            float y = e.getY();
            float[] vals = e.getYVals();

            if (!mContainsStacks || vals == null) {

                float left = x - barWidthHalf;
                float right = x + barWidthHalf;
                float bottom, top;

                if (mInverted) {
                    bottom = y >= 0 ? y : 0;
                    top = y <= 0 ? y : 0;
                } else {
                    top = y >= 0 ? y : 0;
                    bottom = y <= 0 ? y : 0;
                }

                if (top > 0)
                    top *= phaseY;
                else
                    bottom *= phaseY;

                addBar(left, top, right, bottom);

            } else {

                float posY = 0f;
                float negY = -e.getNegativeSum();
                float yStart = 0f;

                for (int k = 0; k < vals.length; k++) {

                    float value = vals[k];

                    if (value == 0.0f && (posY == 0.0f || negY == 0.0f)) {

                        y = value;
                        yStart = y;
                    } else if (value >= 0.0f) {
                        y = posY;
                        yStart = posY + value;
                        posY = yStart;
                    } else {
                        y = negY;
                        yStart = negY + Math.abs(value);
                        negY += Math.abs(value);
                    }

                    float left = x - barWidthHalf;
                    float right = x + barWidthHalf;
                    float bottom, top;

                    if (mInverted) {
                        bottom = y >= yStart ? y : yStart;
                        top = y <= yStart ? y : yStart;
                    } else {
                        top = y >= yStart ? y : yStart;
                        bottom = y <= yStart ? y : yStart;
                    }

                    top *= phaseY;
                    bottom *= phaseY;

                    addBar(left, top, right, bottom);
                }
            }
        }

        reset();
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\buffer\HorizontalBarBuffer.java =====

package com.mpdc4gsr.libunified.ui.buffer;

import com.mpdc4gsr.libunified.ui.data.BarEntry;
import com.mpdc4gsr.libunified.ui.interfaces.datasets.IBarDataSet;

public class HorizontalBarBuffer extends BarBuffer {

    public HorizontalBarBuffer(int size, int dataSetCount, boolean containsStacks) {
        super(size, dataSetCount, containsStacks);
    }

    @Override
    public void feed(IBarDataSet data) {

        float size = data.getEntryCount() * phaseX;
        float barWidthHalf = mBarWidth / 2f;

        for (int i = 0; i < size; i++) {

            BarEntry e = data.getEntryForIndex(i);

            if (e == null)
                continue;

            float x = e.getX();
            float y = e.getY();
            float[] vals = e.getYVals();

            if (!mContainsStacks || vals == null) {

                float bottom = x - barWidthHalf;
                float top = x + barWidthHalf;
                float left, right;
                if (mInverted) {
                    left = y >= 0 ? y : 0;
                    right = y <= 0 ? y : 0;
                } else {
                    right = y >= 0 ? y : 0;
                    left = y <= 0 ? y : 0;
                }

                if (right > 0)
                    right *= phaseY;
                else
                    left *= phaseY;

                addBar(left, top, right, bottom);

            } else {

                float posY = 0f;
                float negY = -e.getNegativeSum();
                float yStart = 0f;

                for (int k = 0; k < vals.length; k++) {

                    float value = vals[k];

                    if (value >= 0f) {
                        y = posY;
                        yStart = posY + value;
                        posY = yStart;
                    } else {
                        y = negY;
                        yStart = negY + Math.abs(value);
                        negY += Math.abs(value);
                    }

                    float bottom = x - barWidthHalf;
                    float top = x + barWidthHalf;
                    float left, right;
                    if (mInverted) {
                        left = y >= yStart ? y : yStart;
                        right = y <= yStart ? y : yStart;
                    } else {
                        right = y >= yStart ? y : yStart;
                        left = y <= yStart ? y : yStart;
                    }

                    right *= phaseY;
                    left *= phaseY;

                    addBar(left, top, right, bottom);
                }
            }
        }

        reset();
    }
}