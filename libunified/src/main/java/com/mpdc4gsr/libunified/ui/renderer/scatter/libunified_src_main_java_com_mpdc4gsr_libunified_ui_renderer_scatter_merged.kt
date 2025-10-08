// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\ui\renderer\scatter' directory and its subdirectories.
// Total files: 8 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\renderer\scatter\ChevronDownShapeRenderer.java =====

package com.mpdc4gsr.libunified.ui.renderer.scatter;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.mpdc4gsr.libunified.ui.interfaces.datasets.IScatterDataSet;
import com.mpdc4gsr.libunified.ui.utils.Utils;
import com.mpdc4gsr.libunified.ui.utils.ViewPortHandler;

public class ChevronDownShapeRenderer implements IShapeRenderer {

    @Override
    public void renderShape(Canvas c, IScatterDataSet dataSet, ViewPortHandler viewPortHandler,
                            float posX, float posY, Paint renderPaint) {

        final float shapeHalf = dataSet.getScatterShapeSize() / 2f;

        renderPaint.setStyle(Paint.Style.STROKE);
        renderPaint.setStrokeWidth(Utils.convertDpToPixel(1f));

        c.drawLine(
                posX,
                posY + (2 * shapeHalf),
                posX + (2 * shapeHalf),
                posY,
                renderPaint);

        c.drawLine(
                posX,
                posY + (2 * shapeHalf),
                posX - (2 * shapeHalf),
                posY,
                renderPaint);
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\renderer\scatter\ChevronUpShapeRenderer.java =====

package com.mpdc4gsr.libunified.ui.renderer.scatter;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.mpdc4gsr.libunified.ui.interfaces.datasets.IScatterDataSet;
import com.mpdc4gsr.libunified.ui.utils.Utils;
import com.mpdc4gsr.libunified.ui.utils.ViewPortHandler;

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


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\renderer\scatter\CircleShapeRenderer.java =====

package com.mpdc4gsr.libunified.ui.renderer.scatter;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.mpdc4gsr.libunified.ui.interfaces.datasets.IScatterDataSet;
import com.mpdc4gsr.libunified.ui.utils.ColorTemplate;
import com.mpdc4gsr.libunified.ui.utils.Utils;
import com.mpdc4gsr.libunified.ui.utils.ViewPortHandler;

public class CircleShapeRenderer implements IShapeRenderer {

    @Override
    public void renderShape(Canvas c, IScatterDataSet dataSet, ViewPortHandler viewPortHandler,
                            float posX, float posY, Paint renderPaint) {

        final float shapeSize = dataSet.getScatterShapeSize();
        final float shapeHalf = shapeSize / 2f;
        final float shapeHoleSizeHalf = Utils.convertDpToPixel(dataSet.getScatterShapeHoleRadius());
        final float shapeHoleSize = shapeHoleSizeHalf * 2.f;
        final float shapeStrokeSize = (shapeSize - shapeHoleSize) / 2.f;
        final float shapeStrokeSizeHalf = shapeStrokeSize / 2.f;

        final int shapeHoleColor = dataSet.getScatterShapeHoleColor();

        if (shapeSize > 0.0) {
            renderPaint.setStyle(Paint.Style.STROKE);
            renderPaint.setStrokeWidth(shapeStrokeSize);

            c.drawCircle(
                    posX,
                    posY,
                    shapeHoleSizeHalf + shapeStrokeSizeHalf,
                    renderPaint);

            if (shapeHoleColor != ColorTemplate.COLOR_NONE) {
                renderPaint.setStyle(Paint.Style.FILL);

                renderPaint.setColor(shapeHoleColor);
                c.drawCircle(
                        posX,
                        posY,
                        shapeHoleSizeHalf,
                        renderPaint);
            }
        } else {
            renderPaint.setStyle(Paint.Style.FILL);

            c.drawCircle(
                    posX,
                    posY,
                    shapeHalf,
                    renderPaint);
        }

    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\renderer\scatter\CrossShapeRenderer.java =====

package com.mpdc4gsr.libunified.ui.renderer.scatter;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.mpdc4gsr.libunified.ui.interfaces.datasets.IScatterDataSet;
import com.mpdc4gsr.libunified.ui.utils.Utils;
import com.mpdc4gsr.libunified.ui.utils.ViewPortHandler;

public class CrossShapeRenderer implements IShapeRenderer {

    @Override
    public void renderShape(Canvas c, IScatterDataSet dataSet, ViewPortHandler viewPortHandler,
                            float posX, float posY, Paint renderPaint) {

        final float shapeHalf = dataSet.getScatterShapeSize() / 2f;

        renderPaint.setStyle(Paint.Style.STROKE);
        renderPaint.setStrokeWidth(Utils.convertDpToPixel(1f));

        c.drawLine(
                posX - shapeHalf,
                posY,
                posX + shapeHalf,
                posY,
                renderPaint);
        c.drawLine(
                posX,
                posY - shapeHalf,
                posX,
                posY + shapeHalf,
                renderPaint);

    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\renderer\scatter\IShapeRenderer.java =====

package com.mpdc4gsr.libunified.ui.renderer.scatter;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.mpdc4gsr.libunified.ui.interfaces.datasets.IScatterDataSet;
import com.mpdc4gsr.libunified.ui.utils.ViewPortHandler;

public interface IShapeRenderer {

    void renderShape(Canvas c, IScatterDataSet dataSet, ViewPortHandler viewPortHandler,
                     float posX, float posY, Paint renderPaint);
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\renderer\scatter\SquareShapeRenderer.java =====

package com.mpdc4gsr.libunified.ui.renderer.scatter;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.mpdc4gsr.libunified.ui.interfaces.datasets.IScatterDataSet;
import com.mpdc4gsr.libunified.ui.utils.ColorTemplate;
import com.mpdc4gsr.libunified.ui.utils.Utils;
import com.mpdc4gsr.libunified.ui.utils.ViewPortHandler;

public class SquareShapeRenderer implements IShapeRenderer {

    @Override
    public void renderShape(Canvas c, IScatterDataSet dataSet, ViewPortHandler viewPortHandler,
                            float posX, float posY, Paint renderPaint) {

        final float shapeSize = dataSet.getScatterShapeSize();
        final float shapeHalf = shapeSize / 2f;
        final float shapeHoleSizeHalf = Utils.convertDpToPixel(dataSet.getScatterShapeHoleRadius());
        final float shapeHoleSize = shapeHoleSizeHalf * 2.f;
        final float shapeStrokeSize = (shapeSize - shapeHoleSize) / 2.f;
        final float shapeStrokeSizeHalf = shapeStrokeSize / 2.f;

        final int shapeHoleColor = dataSet.getScatterShapeHoleColor();

        if (shapeSize > 0.0) {
            renderPaint.setStyle(Paint.Style.STROKE);
            renderPaint.setStrokeWidth(shapeStrokeSize);

            c.drawRect(posX - shapeHoleSizeHalf - shapeStrokeSizeHalf,
                    posY - shapeHoleSizeHalf - shapeStrokeSizeHalf,
                    posX + shapeHoleSizeHalf + shapeStrokeSizeHalf,
                    posY + shapeHoleSizeHalf + shapeStrokeSizeHalf,
                    renderPaint);

            if (shapeHoleColor != ColorTemplate.COLOR_NONE) {
                renderPaint.setStyle(Paint.Style.FILL);

                renderPaint.setColor(shapeHoleColor);
                c.drawRect(posX - shapeHoleSizeHalf,
                        posY - shapeHoleSizeHalf,
                        posX + shapeHoleSizeHalf,
                        posY + shapeHoleSizeHalf,
                        renderPaint);
            }

        } else {
            renderPaint.setStyle(Paint.Style.FILL);

            c.drawRect(posX - shapeHalf,
                    posY - shapeHalf,
                    posX + shapeHalf,
                    posY + shapeHalf,
                    renderPaint);
        }
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\renderer\scatter\TriangleShapeRenderer.java =====

package com.mpdc4gsr.libunified.ui.renderer.scatter;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import com.mpdc4gsr.libunified.ui.interfaces.datasets.IScatterDataSet;
import com.mpdc4gsr.libunified.ui.utils.ColorTemplate;
import com.mpdc4gsr.libunified.ui.utils.Utils;
import com.mpdc4gsr.libunified.ui.utils.ViewPortHandler;

public class TriangleShapeRenderer implements IShapeRenderer {

    protected Path mTrianglePathBuffer = new Path();

    @Override
    public void renderShape(Canvas c, IScatterDataSet dataSet, ViewPortHandler viewPortHandler,
                            float posX, float posY, Paint renderPaint) {

        final float shapeSize = dataSet.getScatterShapeSize();
        final float shapeHalf = shapeSize / 2f;
        final float shapeHoleSizeHalf = Utils.convertDpToPixel(dataSet.getScatterShapeHoleRadius());
        final float shapeHoleSize = shapeHoleSizeHalf * 2.f;
        final float shapeStrokeSize = (shapeSize - shapeHoleSize) / 2.f;

        final int shapeHoleColor = dataSet.getScatterShapeHoleColor();

        renderPaint.setStyle(Paint.Style.FILL);

        Path tri = mTrianglePathBuffer;
        tri.reset();

        tri.moveTo(posX, posY - shapeHalf);
        tri.lineTo(posX + shapeHalf, posY + shapeHalf);
        tri.lineTo(posX - shapeHalf, posY + shapeHalf);

        if (shapeSize > 0.0) {
            tri.lineTo(posX, posY - shapeHalf);

            tri.moveTo(posX - shapeHalf + shapeStrokeSize,
                    posY + shapeHalf - shapeStrokeSize);
            tri.lineTo(posX + shapeHalf - shapeStrokeSize,
                    posY + shapeHalf - shapeStrokeSize);
            tri.lineTo(posX,
                    posY - shapeHalf + shapeStrokeSize);
            tri.lineTo(posX - shapeHalf + shapeStrokeSize,
                    posY + shapeHalf - shapeStrokeSize);
        }

        tri.close();

        c.drawPath(tri, renderPaint);
        tri.reset();

        if (shapeSize > 0.0 &&
                shapeHoleColor != ColorTemplate.COLOR_NONE) {

            renderPaint.setColor(shapeHoleColor);

            tri.moveTo(posX,
                    posY - shapeHalf + shapeStrokeSize);
            tri.lineTo(posX + shapeHalf - shapeStrokeSize,
                    posY + shapeHalf - shapeStrokeSize);
            tri.lineTo(posX - shapeHalf + shapeStrokeSize,
                    posY + shapeHalf - shapeStrokeSize);
            tri.close();

            c.drawPath(tri, renderPaint);
            tri.reset();
        }

    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\renderer\scatter\XShapeRenderer.java =====

package com.mpdc4gsr.libunified.ui.renderer.scatter;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.mpdc4gsr.libunified.ui.interfaces.datasets.IScatterDataSet;
import com.mpdc4gsr.libunified.ui.utils.Utils;
import com.mpdc4gsr.libunified.ui.utils.ViewPortHandler;

public class XShapeRenderer implements IShapeRenderer {

    @Override
    public void renderShape(Canvas c, IScatterDataSet dataSet, ViewPortHandler viewPortHandler,
                            float posX, float posY, Paint renderPaint) {

        final float shapeHalf = dataSet.getScatterShapeSize() / 2f;

        renderPaint.setStyle(Paint.Style.STROKE);
        renderPaint.setStrokeWidth(Utils.convertDpToPixel(1f));

        c.drawLine(
                posX - shapeHalf,
                posY - shapeHalf,
                posX + shapeHalf,
                posY + shapeHalf,
                renderPaint);
        c.drawLine(
                posX + shapeHalf,
                posY - shapeHalf,
                posX - shapeHalf,
                posY + shapeHalf,
                renderPaint);

    }

}