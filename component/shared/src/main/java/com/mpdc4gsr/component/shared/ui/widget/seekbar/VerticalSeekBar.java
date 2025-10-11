package com.mpdc4gsr.component.shared.ui.widget.seekbar;

import android.graphics.Canvas;
import android.util.AttributeSet;

public class VerticalSeekBar extends SeekBar {

    public VerticalSeekBar(VerticalRangeSeekBar parent, AttributeSet attrs, boolean isLeft) {
        super(parent, attrs, isLeft);
    }

    public void setNoNegativeNumber(boolean noNegativeNumber) {
        // Implementation for vertical specific logic
    }

    public void setDrawIndPathBg(boolean draw) {
        // Implementation for draw indicator path background
    }

    @Override
    public void draw(Canvas canvas, boolean isLeft) {
        // Save canvas state for rotation
        canvas.save();

        // Rotate canvas for vertical drawing
        canvas.rotate(-90);

        // Call parent draw method
        super.draw(canvas, isLeft);

        // Restore canvas state
        canvas.restore();
    }
}

