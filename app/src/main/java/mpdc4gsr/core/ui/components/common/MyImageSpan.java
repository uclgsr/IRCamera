package mpdc4gsr.core.ui.components.common;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.style.ImageSpan;

/**
 * Custom ImageSpan that vertically centers the drawable relative to text.
 */
public class MyImageSpan extends ImageSpan {
    public MyImageSpan(Drawable drawable) {
        super(drawable);
    }

    @Override
    public void draw(
            Canvas canvas,
            CharSequence text,
            int start,
            int end,
            float x,
            int top,
            int y,
            int bottom,
            Paint paint
    ) {
        Drawable drawable = getDrawable();
        Paint.FontMetricsInt fontMetrics = paint.getFontMetricsInt();
        int transY = (y + fontMetrics.descent + y + fontMetrics.ascent) / 2 - drawable.getBounds().bottom / 2;
        canvas.save();
        canvas.translate(x, transY);
        drawable.draw(canvas);
        canvas.restore();
    }
}
