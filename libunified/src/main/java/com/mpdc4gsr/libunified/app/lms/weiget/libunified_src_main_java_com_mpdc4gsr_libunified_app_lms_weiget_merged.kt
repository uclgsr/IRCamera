// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\weiget' directory and its subdirectories.
// Total files: 3 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\weiget\LmsLoadDialog.java =====

package com.mpdc4gsr.libunified.app.lms.weiget;

import android.app.Dialog;
import android.content.Context;

public class LmsLoadDialog extends Dialog {
    public LmsLoadDialog(Context context) {
        super(context);
    }

    public void setMessage(String message) {
    }

    public void setCancelable(boolean cancelable) {
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\weiget\LmsLoadView.java =====

package com.mpdc4gsr.libunified.app.lms.weiget;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class LmsLoadView extends View {
    private static final int DEFAULT_DOT_COUNT = 3;
    private static final int DEFAULT_DOT_RADIUS = 5;
    private static final int DEFAULT_DOT_SPACING = 6;
    private static final float DEFAULT_MOVE_RATE = 0.75f;
    private static final int DEFAULT_DOT_COLOR = 0xFFFFFFFF; // White

    private Paint dotPaint;
    private int dotCount = DEFAULT_DOT_COUNT;
    private float dotRadius = DEFAULT_DOT_RADIUS;
    private float dotSpacing = DEFAULT_DOT_SPACING;
    private float moveRate = DEFAULT_MOVE_RATE;
    private int dotColor = DEFAULT_DOT_COLOR;

    private ObjectAnimator animator;
    private float animationProgress = 0f;

    public LmsLoadView(Context context) {
        this(context, null);
    }

    public LmsLoadView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LmsLoadView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttributes(context, attrs);
        initPaint();
        initAnimation();
    }

    private void initAttributes(Context context, AttributeSet attrs) {
        if (attrs != null) {
            // Convert dp to pixels for default values
            float density = context.getResources().getDisplayMetrics().density;
            dotRadius = DEFAULT_DOT_RADIUS * density;
            dotSpacing = DEFAULT_DOT_SPACING * density;

            // If we had styleable attributes, we would parse them here
            // For now, using defaults and hardcoded values
        }
    }

    private void initPaint() {
        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(dotColor);
        dotPaint.setStyle(Paint.Style.FILL);
    }

    private void initAnimation() {
        animator = ObjectAnimator.ofFloat(this, "animationProgress", 0f, 1f);
        animator.setDuration(1500);
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.setRepeatMode(ObjectAnimator.RESTART);
        animator.setInterpolator(new LinearInterpolator());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float totalWidth = (dotCount * dotRadius * 2) + ((dotCount - 1) * dotSpacing);
        float totalHeight = dotRadius * 2;

        int width = (int) totalWidth;
        int height = (int) totalHeight;

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float centerY = getHeight() / 2f;
        float startX = dotRadius;

        for (int i = 0; i < dotCount; i++) {
            float centerX = startX + (i * (dotRadius * 2 + dotSpacing));

            // Calculate alpha based on animation progress
            float phase = (animationProgress + (i * 0.2f)) % 1f;
            int alpha = (int) (255 * (0.3f + 0.7f * Math.sin(phase * Math.PI * 2)));

            dotPaint.setAlpha(Math.max(50, Math.min(255, alpha)));
            canvas.drawCircle(centerX, centerY, dotRadius, dotPaint);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
    }

    public void startAnimation() {
        if (animator != null && !animator.isStarted() && isAttachedToWindow()) {
            animator.start();
        }
    }

    public void stopAnimation() {
        if (animator != null && animator.isStarted()) {
            animator.cancel();
        }
    }

    // Getters for animation progress
    public float getAnimationProgress() {
        return animationProgress;
    }

    // Setter for ObjectAnimator
    public void setAnimationProgress(float progress) {
        this.animationProgress = progress;
        invalidate();
    }

    // Public setters for customization
    public void setDotColor(int color) {
        this.dotColor = color;
        dotPaint.setColor(color);
        invalidate();
    }

    public void setDotRadius(float radius) {
        this.dotRadius = radius;
        requestLayout();
    }

    public void setDotSpacing(float spacing) {
        this.dotSpacing = spacing;
        requestLayout();
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\lms\weiget\TToast.java =====

package com.mpdc4gsr.libunified.app.lms.weiget;

import android.content.Context;

import com.mpdc4gsr.libunified.app.compose.components.ComposeToastHelper;

public class TToast {
    private static final long SHORT_DURATION = 2000L;
    private static final long LONG_DURATION = 3500L;

    public static void shortToast(Context context, String message) {
        if (context != null && message != null && !message.isEmpty()) {
            ComposeToastHelper.INSTANCE.show(context, message, SHORT_DURATION);
        }
    }

    public static void shortToast(Context context, int resId) {
        if (context != null) {
            ComposeToastHelper.INSTANCE.show(context, resId, SHORT_DURATION);
        }
    }

    public static void longToast(Context context, String message) {
        if (context != null && message != null && !message.isEmpty()) {
            ComposeToastHelper.INSTANCE.show(context, message, LONG_DURATION);
        }
    }

    public static void longToast(Context context, int resId) {
        if (context != null) {
            ComposeToastHelper.INSTANCE.show(context, resId, LONG_DURATION);
        }
    }

    public static void show(Context context, String message) {
        shortToast(context, message);
    }

    public static void show(Context context, int resId) {
        shortToast(context, resId);
    }
}