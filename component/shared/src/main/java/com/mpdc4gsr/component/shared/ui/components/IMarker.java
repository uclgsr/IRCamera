package com.mpdc4gsr.component.shared.ui.components;

import android.graphics.Canvas;

import com.mpdc4gsr.component.shared.ui.data.Entry;
import com.mpdc4gsr.component.shared.ui.highlight.Highlight;
import com.mpdc4gsr.component.shared.ui.utils.MPPointF;

public interface IMarker {

    MPPointF getOffset();

    MPPointF getOffsetForDrawingAtPoint(float posX, float posY);

    void refreshContent(Entry e, Highlight highlight);

    void draw(Canvas canvas, float posX, float posY);
}


