package com.mpdc4gsr.component.shared.ui.renderer.scatter;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.mpdc4gsr.component.shared.ui.interfaces.datasets.IScatterDataSet;
import com.mpdc4gsr.component.shared.ui.utils.Utils;
import com.mpdc4gsr.component.shared.ui.utils.ViewPortHandler;

public class ChevronUpShapeRenderer implements IShapeRenderer {

    @Override
    public void renderShape(Canvas c, IScatterDataSet dataSet, ViewPortHandler viewPortHandler,
                            float posX, float posY, Paint renderPaint) {

        final float shapeHalf = dataSet.getScatterShapeSize() / 2f;

        renderPaint.setStyle(Paint.Style.STROKE);
        renderPaint.setStrokeWidth(Utils.convertDpToPixel(1f));

        c.drawLine(
                posX,
                posY - (2 * shapeHalf),
                posX + (2 * shapeHalf),
                posY,
                renderPaint);

        c.drawLine(
                posX,
                posY - (2 * shapeHalf),
                posX - (2 * shapeHalf),
                posY,
                renderPaint);

    }
}


