package com.mpdc4gsr.component.shared.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.mpdc4gsr.component.shared.R;

public class IndicateView extends View {
    private int selectColor = Color.parseColor("#06AAFF");
    private int unSelectColor = Color.parseColor("#80FFFFFF");
    private Paint paint;
    private int count = 0;
    private int currentIndex = 0;
    private float radius = 6f;

    public IndicateView(Context context) {
        this(context, null);
    }

    public IndicateView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IndicateView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttributes(context, attrs);
        initPaint();
    }

    private void initAttributes(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.IndicateView);
            selectColor = typedArray.getColor(R.styleable.IndicateView_selectColor, Color.parseColor("#06AAFF"));
            typedArray.recycle();
        }
    }

    private void initPaint() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
    }

    public void setCount(int count) {
        this.count = count;
        invalidate();
    }

    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = count * (int) (radius * 2) + (count - 1) * (int) (radius * 2);
        int height = (int) (radius * 2);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (count <= 0) return;

        float centerY = getHeight() / 2f;
        float startX = radius;

        for (int i = 0; i < count; i++) {
            paint.setColor(i == currentIndex ? selectColor : unSelectColor);
            float centerX = startX + i * (radius * 4);
            canvas.drawCircle(centerX, centerY, radius, paint);
        }
    }
}

