package com.mpdc4gsr.component.shared.ui.charts;

import android.content.Context;
import android.util.AttributeSet;

import com.mpdc4gsr.component.shared.ui.data.LineData;
import com.mpdc4gsr.component.shared.ui.interfaces.dataprovider.LineDataProvider;
import com.mpdc4gsr.component.shared.ui.renderer.LineChartRenderer;

public class LineChart extends BarLineChartBase<LineData> implements LineDataProvider {

    public LineChart(Context context) {
        super(context);
    }

    public LineChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LineChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void init() {
        super.init();

        mRenderer = new LineChartRenderer(this, mAnimator, mViewPortHandler);
    }

    @Override
    public LineData getLineData() {
        return mData;
    }

    @Override
    protected void onDetachedFromWindow() {

        if (mRenderer != null && mRenderer instanceof LineChartRenderer) {
            ((LineChartRenderer) mRenderer).releaseBitmap();
        }
        super.onDetachedFromWindow();
    }
}


