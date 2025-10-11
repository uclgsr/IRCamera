package com.mpdc4gsr.component.shared.ui.renderer.scatter;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.mpdc4gsr.component.shared.ui.interfaces.datasets.IScatterDataSet;
import com.mpdc4gsr.component.shared.ui.utils.ViewPortHandler;

public interface IShapeRenderer {

    void renderShape(Canvas c, IScatterDataSet dataSet, ViewPortHandler viewPortHandler,
                     float posX, float posY, Paint renderPaint);
}


