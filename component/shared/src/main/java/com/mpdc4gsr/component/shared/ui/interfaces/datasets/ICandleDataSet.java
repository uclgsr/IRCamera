package com.mpdc4gsr.component.shared.ui.interfaces.datasets;

import android.graphics.Paint;

import com.mpdc4gsr.component.shared.ui.data.CandleEntry;

public interface ICandleDataSet extends ILineScatterCandleRadarDataSet<CandleEntry> {

    float getBarSpace();

    boolean getShowCandleBar();

    float getShadowWidth();

    int getShadowColor();

    int getNeutralColor();

    int getIncreasingColor();

    int getDecreasingColor();

    Paint.Style getIncreasingPaintStyle();

    Paint.Style getDecreasingPaintStyle();

    boolean getShadowColorSameAsCandle();
}


