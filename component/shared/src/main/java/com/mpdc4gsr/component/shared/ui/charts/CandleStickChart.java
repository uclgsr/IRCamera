package com.mpdc4gsr.component.shared.ui.charts;

import android.content.Context;
import android.util.AttributeSet;

import com.mpdc4gsr.component.shared.ui.data.CandleData;
import com.mpdc4gsr.component.shared.ui.interfaces.dataprovider.CandleDataProvider;
import com.mpdc4gsr.component.shared.ui.renderer.CandleStickChartRenderer;

public class CandleStickChart extends BarLineChartBase<CandleData> implements CandleDataProvider {

    public CandleStickChart(Context context) {
        super(context);
    }

    public CandleStickChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CandleStickChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void init() {
        super.init();

        mRenderer = new CandleStickChartRenderer(this, mAnimator, mViewPortHandler);

        getXAxis().setSpaceMin(0.5f);
        getXAxis().setSpaceMax(0.5f);
    }

    @Override
    public CandleData getCandleData() {
        return mData;
    }
}


