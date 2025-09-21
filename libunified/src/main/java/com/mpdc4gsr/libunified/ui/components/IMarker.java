package com.mpdc4gsr.libunified.ui.charting.components;

import android.graphics.Canvas;

import com.mpdc4gsr.libunified.ui.charting.data.Entry;
import com.mpdc4gsr.libunified.ui.charting.highlight.Highlight;
import com.mpdc4gsr.libunified.ui.charting.utils.MPPointF;

public interface IMarker {

    MPPointF getOffset();

    MPPointF getOffsetForDrawingAtPoint(float posX, float posY);

    void refreshContent(Entry e, Highlight highlight);

    void draw(Canvas canvas, float posX, float posY);
}
