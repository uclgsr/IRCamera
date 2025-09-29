package com.mpdc4gsr.libunified.ui.renderer;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * BaseChartRenderer - Common chart rendering patterns
 * 
 * This class consolidates the duplicate methods found in 8+ chart renderer files:
 * - PieChartRenderer.java
 * - BarChartRenderer.java  
 * - LineChartRenderer.java
 * - ScatterChartRenderer.java
 * - CandleStickChartRenderer.java
 * - BubbleChartRenderer.java
 * - RadarChartRenderer.java
 * - HorizontalBarChartRenderer.java
 * 
 * All these files had identical drawValue methods.
 */
public abstract class BaseChartRenderer extends DataRenderer {

    protected Paint mValuePaint;

    public BaseChartRenderer() {
        super();
        // Initialize common paint objects
        mValuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    /**
     * Common drawValue implementation found in 8+ chart renderer files
     * This method was duplicated identically across all chart types
     */
    @Override
    public void drawValue(Canvas c, String valueText, float x, float y, int color) {
        mValuePaint.setColor(color);
        c.drawText(valueText, x, y, mValuePaint);
    }

    /**
     * Helper method for consistent value text rendering
     */
    protected void drawStandardValue(Canvas c, String valueText, float x, float y, int color) {
        drawValue(c, valueText, x, y, color);
    }

    /**
     * Helper method for setting text size consistently
     */
    protected void setValueTextSize(float textSize) {
        if (mValuePaint != null) {
            mValuePaint.setTextSize(textSize);
        }
    }

    /**
     * Helper method for setting text alignment consistently
     */
    protected void setValueTextAlign(Paint.Align align) {
        if (mValuePaint != null) {
            mValuePaint.setTextAlign(align);
        }
    }
}