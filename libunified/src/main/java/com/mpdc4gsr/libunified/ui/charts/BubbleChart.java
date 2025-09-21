package com.mpdc4gsr.libunified.ui.charting.charts;

import android.content.Context;
import android.util.AttributeSet;

import com.mpdc4gsr.libunified.ui.charting.data.BubbleData;
import com.mpdc4gsr.libunified.ui.charting.interfaces.dataprovider.BubbleDataProvider;
import com.mpdc4gsr.libunified.ui.charting.renderer.BubbleChartRenderer;

public class BubbleChart extends BarLineChartBase<BubbleData> implements BubbleDataProvider {

    public BubbleChart(Context context) {
        super(context);
    }

    public BubbleChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BubbleChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void init() {
        super.init();

        mRenderer = new BubbleChartRenderer(this, mAnimator, mViewPortHandler);
    }

    public BubbleData getBubbleData() {
        return mData;
    }
}
