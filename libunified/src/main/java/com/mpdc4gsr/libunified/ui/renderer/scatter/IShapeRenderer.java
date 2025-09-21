package com.mpdc4gsr.libunified.ui.charting.renderer.scatter;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.mpdc4gsr.libunified.ui.charting.interfaces.datasets.IScatterDataSet;
import com.mpdc4gsr.libunified.ui.charting.utils.ViewPortHandler;

public interface IShapeRenderer {

    void renderShape(Canvas c, IScatterDataSet dataSet, ViewPortHandler viewPortHandler,
                     float posX, float posY, Paint renderPaint);
}
