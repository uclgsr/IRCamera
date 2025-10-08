// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\seekbar' directory and its subdirectories.
// Total files: 8 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\seekbar\OnRangeChangedListener.java =====

package com.mpdc4gsr.libunified.ui.widget.seekbar;

import androidx.annotation.NonNull;

public interface OnRangeChangedListener {
    void onRangeChanged(@NonNull RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser, int tempMode);

    void onStartTrackingTouch(@NonNull RangeSeekBar view, boolean isLeft);

    void onStopTrackingTouch(@NonNull RangeSeekBar view, boolean isLeft);
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\seekbar\RangeSeekBar.java =====

package com.mpdc4gsr.libunified.ui.widget.seekbar;

import static com.mpdc4gsr.libunified.ui.widget.seekbar.SeekBar.INDICATOR_ALWAYS_HIDE;
import static com.mpdc4gsr.libunified.ui.widget.seekbar.SeekBar.INDICATOR_ALWAYS_SHOW;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import com.mpdc4gsr.libunified.R;
import com.mpdc4gsr.libunified.app.menu.util.PseudoColorConfig;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class RangeSeekBar extends View {

    public final static int SEEKBAR_MODE_SINGLE = 1;
    public final static int SEEKBAR_MODE_RANGE = 2;
    public final static int TEMP_MODE_CLOSE = 0;//close
    public final static int TEMP_MODE_MAX = 2;//
    public final static int TEMP_MODE_MIN = 1;//
    public final static int TEMP_MODE_INTERVAL = 3;//
    public final static int TRICK_MARK_MODE_NUMBER = 0;
    public final static int TRICK_MARK_MODE_OTHER = 1;
    public final static int TICK_MARK_GRAVITY_LEFT = 0;
    public final static int TICK_MARK_GRAVITY_CENTER = 1;
    public final static int TICK_MARK_GRAVITY_RIGHT = 2;
    private final static int MIN_INTERCEPT_DISTANCE = 100;
    float touchDownX, touchDownY;
    float reservePercent;
    boolean isScaleThumb = false;
    Paint paint = new Paint();
    RectF progressDefaultDstRect = new RectF();
    RectF progressDstRect = new RectF();
    Rect progressSrcRect = new Rect();
    RectF stepDivRect = new RectF();
    Rect tickMarkTextRect = new Rect();
    SeekBar leftSB;
    SeekBar rightSB;
    SeekBar currTouchSB;
    Bitmap progressBitmap;
    Bitmap progressDefaultBitmap;
    List<Bitmap> stepsBitmaps = new ArrayList<>();
    Long updateTime = System.currentTimeMillis();
    private int pseudocode = 3;
    private boolean noNegativeNumber = false;
    private int tempMode = TEMP_MODE_CLOSE;
    private int progressTop, progressBottom, progressLeft, progressRight;
    private int seekBarMode;
    private int tickMarkMode;
    private int tickMarkTextMargin;
    private int tickMarkTextSize;
    private int tickMarkGravity;
    private int tickMarkLayoutGravity;
    private int tickMarkTextColor;
    private int tickMarkInRangeTextColor;
    private CharSequence[] tickMarkTextArray;
    private float progressRadius;
    private int progressColor;
    private int progressDefaultColor;
    private int progressDrawableId;
    private int progressDefaultDrawableId;
    private int progressHeight;
    private int progressWidth;
    private float minInterval;
    private int gravity;
    private boolean enableThumbOverlap;
    private int stepsColor;
    private float stepsWidth;
    private float stepsHeight;
    private float stepsRadius;
    private int steps;
    private boolean stepsAutoBonding;
    private int stepsDrawableId;
    private float minProgress, maxProgress;
    private boolean isEnable = true;
    private int progressPaddingRight;
    private OnRangeChangedListener callback;
    @Nullable
    private int[] colorList;
    @Nullable
    private float[] places;

    public RangeSeekBar(Context context) {
        this(context, null);
    }

    public RangeSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(attrs);
        initPaint();
        initSeekBar(attrs);
        initStepsBitmap();
    }

    public int getTempMode() {
        return tempMode;
    }

    public void setTempMode(int tempMode) {
        this.tempMode = tempMode;
    }

    private void updateTempModeState() {
        if (tempMode == TEMP_MODE_CLOSE) {
            if (currTouchSB == leftSB) {
                tempMode = TEMP_MODE_MIN;
            } else if (currTouchSB == rightSB) {
                tempMode = TEMP_MODE_MAX;
            }
        } else if (tempMode == TEMP_MODE_MIN) {
            if (currTouchSB == rightSB) {
                tempMode = TEMP_MODE_INTERVAL;
            }
        } else if (tempMode == TEMP_MODE_MAX) {
            if (currTouchSB == leftSB) {
                tempMode = TEMP_MODE_INTERVAL;
            }
        }
    }

    private void initProgressBitmap() {
        if (progressBitmap == null) {
            progressBitmap = Utils.drawableToBitmap(getContext(), progressWidth, progressHeight, progressDrawableId);
        }
        if (progressDefaultBitmap == null) {
            progressDefaultBitmap = Utils.drawableToBitmap(getContext(), progressWidth, progressHeight, progressDefaultDrawableId);
        }
    }

    private boolean verifyStepsMode() {
        if (steps < 1 || stepsHeight <= 0 || stepsWidth <= 0) return false;
        return true;
    }

    private void initStepsBitmap() {
        if (!verifyStepsMode() || stepsDrawableId == 0) return;
        if (stepsBitmaps.isEmpty()) {
            Bitmap bitmap = Utils.drawableToBitmap(getContext(), (int) stepsWidth, (int) stepsHeight, stepsDrawableId);
            for (int i = 0; i <= steps; i++) {
                stepsBitmaps.add(bitmap);
            }
        }
    }

    private void initSeekBar(AttributeSet attrs) {
        leftSB = new SeekBar(this, attrs, true);
        rightSB = new SeekBar(this, attrs, false);
        rightSB.setVisible(seekBarMode != SEEKBAR_MODE_SINGLE);
    }

    private void initAttrs(AttributeSet attrs) {
        try {
            TypedArray t = getContext().obtainStyledAttributes(attrs, R.styleable.RangeSeekBar);
            seekBarMode = t.getInt(R.styleable.RangeSeekBar_rsb_mode, SEEKBAR_MODE_RANGE);
            minProgress = t.getFloat(R.styleable.RangeSeekBar_rsb_min, 0);
            maxProgress = t.getFloat(R.styleable.RangeSeekBar_rsb_max, 100);
            minInterval = t.getFloat(R.styleable.RangeSeekBar_rsb_min_interval, 0);
            gravity = t.getInt(R.styleable.RangeSeekBar_rsb_gravity, Gravity.TOP);
            progressColor = t.getColor(R.styleable.RangeSeekBar_rsb_progress_color, -1);
            progressRadius = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_progress_radius, -1);
            progressDefaultColor = t.getColor(R.styleable.RangeSeekBar_rsb_progress_default_color, -1);
            progressDrawableId = t.getResourceId(R.styleable.RangeSeekBar_rsb_progress_drawable, 0);
            progressDefaultDrawableId = t.getResourceId(R.styleable.RangeSeekBar_rsb_progress_drawable_default, 0);
            progressHeight = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_progress_height, Utils.dp2px(getContext(), 2));
            tickMarkMode = t.getInt(R.styleable.RangeSeekBar_rsb_tick_mark_mode, TRICK_MARK_MODE_NUMBER);
            tickMarkGravity = t.getInt(R.styleable.RangeSeekBar_rsb_tick_mark_gravity, TICK_MARK_GRAVITY_CENTER);
            tickMarkLayoutGravity = t.getInt(R.styleable.RangeSeekBar_rsb_tick_mark_layout_gravity, Gravity.TOP);
            tickMarkTextArray = t.getTextArray(R.styleable.RangeSeekBar_rsb_tick_mark_text_array);
            tickMarkTextMargin = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_tick_mark_text_margin, Utils.dp2px(getContext(), 7));
            tickMarkTextSize = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_tick_mark_text_size, Utils.dp2px(getContext(), 12));
            tickMarkTextColor = t.getColor(R.styleable.RangeSeekBar_rsb_tick_mark_text_color, progressDefaultColor);
            tickMarkInRangeTextColor = t.getColor(R.styleable.RangeSeekBar_rsb_tick_mark_in_range_text_color, progressColor);
            steps = t.getInt(R.styleable.RangeSeekBar_rsb_steps, 0);
            stepsColor = t.getColor(R.styleable.RangeSeekBar_rsb_step_color, 0xFF9d9d9d);
            stepsRadius = t.getDimension(R.styleable.RangeSeekBar_rsb_step_radius, 0);
            stepsWidth = t.getDimension(R.styleable.RangeSeekBar_rsb_step_width, 0);
            stepsHeight = t.getDimension(R.styleable.RangeSeekBar_rsb_step_height, 0);
            stepsDrawableId = t.getResourceId(R.styleable.RangeSeekBar_rsb_step_drawable, 0);
            stepsAutoBonding = t.getBoolean(R.styleable.RangeSeekBar_rsb_step_auto_bonding, true);
            t.recycle();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    protected void onMeasureProgress(int w, int h) {
        int viewHeight = h - getPaddingBottom() - getPaddingTop();
        if (h <= 0) return;

        if (gravity == Gravity.TOP) {

            float maxIndicatorHeight = 0;
            if (leftSB.getIndicatorShowMode() != INDICATOR_ALWAYS_HIDE
                    || rightSB.getIndicatorShowMode() != INDICATOR_ALWAYS_HIDE) {
                maxIndicatorHeight = Math.max(leftSB.getIndicatorRawHeight(), rightSB.getIndicatorRawHeight());
            }
            float thumbHeight = Math.max(leftSB.getThumbScaleHeight(), rightSB.getThumbScaleHeight());
            thumbHeight -= progressHeight / 2f;

            progressTop = (int) (maxIndicatorHeight + (thumbHeight - progressHeight) / 2f);
            if (tickMarkTextArray != null && tickMarkLayoutGravity == Gravity.TOP) {
                progressTop = (int) Math.max(getTickMarkRawHeight(), maxIndicatorHeight + (thumbHeight - progressHeight) / 2f);
            }
            progressBottom = progressTop + progressHeight;
        } else if (gravity == Gravity.BOTTOM) {
            if (tickMarkTextArray != null && tickMarkLayoutGravity == Gravity.BOTTOM) {
                progressBottom = viewHeight - getTickMarkRawHeight();
            } else {
                progressBottom = (int) (viewHeight - Math.max(leftSB.getThumbScaleHeight(), rightSB.getThumbScaleHeight()) / 2f
                        + progressHeight / 2f);
            }
            progressTop = progressBottom - progressHeight;
        } else {
            progressTop = (viewHeight - progressHeight) / 2;
            progressBottom = progressTop + progressHeight;
        }

        int maxThumbWidth = (int) Math.max(leftSB.getThumbScaleWidth(), rightSB.getThumbScaleWidth());
        progressLeft = maxThumbWidth / 2 + getPaddingLeft();
        progressRight = w - maxThumbWidth / 2 - getPaddingRight();
        progressWidth = progressRight - progressLeft;
        progressDefaultDstRect.set(getProgressLeft(), getProgressTop(), getProgressRight(), getProgressBottom());
        progressPaddingRight = w - progressRight;

        if (progressRadius <= 0) {
            progressRadius = (int) ((getProgressBottom() - getProgressTop()) * 0.15f);
        }
        initProgressBitmap();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        /*
         * onMeasurewidthMeasureSpecheightMeasureSpecï¼Œ
         * MeasureSpec.EXACTLY
         * MeasureSpec.AT_MOST
         * MeasureSpec.UNSPECIFIED
         */

        if (heightMode == MeasureSpec.EXACTLY) {
            heightSize = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY);
        } else if (heightMode == MeasureSpec.AT_MOST && getParent() instanceof ViewGroup
                && heightSize == ViewGroup.LayoutParams.MATCH_PARENT) {
            heightSize = MeasureSpec.makeMeasureSpec(((ViewGroup) getParent()).getMeasuredHeight(), MeasureSpec.AT_MOST);
        } else {
            int heightNeeded;
            if (gravity == Gravity.CENTER) {
                if (tickMarkTextArray != null && tickMarkLayoutGravity == Gravity.BOTTOM) {
                    heightNeeded = (int) (2 * (getRawHeight() - getTickMarkRawHeight()));
                } else {
                    heightNeeded = (int) (2 * (getRawHeight() - Math.max(leftSB.getThumbScaleHeight(), rightSB.getThumbScaleHeight()) / 2));
                }
            } else {
                heightNeeded = (int) getRawHeight();
            }
            heightSize = MeasureSpec.makeMeasureSpec(heightNeeded, MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightSize);
    }

    protected int getTickMarkRawHeight() {
        if (tickMarkTextArray != null && tickMarkTextArray.length > 0) {
            return tickMarkTextMargin + Utils.measureText(String.valueOf(tickMarkTextArray[0]), tickMarkTextSize).height() + 3;
        }
        return 0;
    }

    protected float getRawHeight() {
        float rawHeight;
        if (seekBarMode == SEEKBAR_MODE_SINGLE) {
            rawHeight = leftSB.getRawHeight();
            if (tickMarkLayoutGravity == Gravity.BOTTOM && tickMarkTextArray != null) {
                float h = Math.max((leftSB.getThumbScaleHeight() - progressHeight) / 2, getTickMarkRawHeight());
                rawHeight = rawHeight - leftSB.getThumbScaleHeight() / 2 + progressHeight / 2f + h;
            }
        } else {
            rawHeight = Math.max(leftSB.getRawHeight(), rightSB.getRawHeight());
            if (tickMarkLayoutGravity == Gravity.BOTTOM && tickMarkTextArray != null) {
                float thumbHeight = Math.max(leftSB.getThumbScaleHeight(), rightSB.getThumbScaleHeight());
                float h = Math.max((thumbHeight - progressHeight) / 2, getTickMarkRawHeight());
                rawHeight = rawHeight - thumbHeight / 2 + progressHeight / 2f + h;
            }
        }
        return rawHeight;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        onMeasureProgress(w, h);

        setRange(minProgress, maxProgress, minInterval);

        int lineCenterY = (getProgressBottom() + getProgressTop()) / 2;
        leftSB.onSizeChanged(getProgressLeft(), lineCenterY);
        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            rightSB.onSizeChanged(getProgressLeft(), lineCenterY);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        onDrawTickMark(canvas, paint); //
        onDrawProgressBar(canvas, paint); //
        onDrawSteps(canvas, paint);
        onDrawSeekBar(canvas); //
    }

    protected void onDrawTickMark(Canvas canvas, Paint paint) {
        if (tickMarkTextArray != null) {
            int trickPartWidth = progressWidth / (tickMarkTextArray.length - 1);
            for (int i = 0; i < tickMarkTextArray.length; i++) {
                final String text2Draw = tickMarkTextArray[i].toString();
                if (TextUtils.isEmpty(text2Draw)) continue;
                paint.getTextBounds(text2Draw, 0, text2Draw.length(), tickMarkTextRect);
                paint.setColor(tickMarkTextColor);

                float x;
                if (tickMarkMode == TRICK_MARK_MODE_OTHER) {
                    if (tickMarkGravity == TICK_MARK_GRAVITY_RIGHT) {
                        x = getProgressLeft() + i * trickPartWidth - tickMarkTextRect.width();
                    } else if (tickMarkGravity == TICK_MARK_GRAVITY_CENTER) {
                        x = getProgressLeft() + i * trickPartWidth - tickMarkTextRect.width() / 2f;
                    } else {
                        x = getProgressLeft() + i * trickPartWidth;
                    }
                } else {
                    float num = Utils.parseFloat(text2Draw);
                    SeekBarState[] states = getRangeSeekBarState();
                    if (Utils.compareFloat(num, states[0].value) != -1 && Utils.compareFloat(num, states[1].value) != 1 && (seekBarMode == SEEKBAR_MODE_RANGE)) {
                        paint.setColor(tickMarkInRangeTextColor);
                    }

                    x = getProgressLeft() + progressWidth * (num - minProgress) / (maxProgress - minProgress)
                            - tickMarkTextRect.width() / 2f;
                }
                float y;
                if (tickMarkLayoutGravity == Gravity.TOP) {
                    y = getProgressTop() - tickMarkTextMargin;
                } else {
                    y = getProgressBottom() + tickMarkTextMargin + tickMarkTextRect.height();
                }
                canvas.drawText(text2Draw, x, y, paint);
            }
        }
    }

    protected void onDrawProgressBar(Canvas canvas, Paint paint) {

        paint.setShader(null);
        if (Utils.verifyBitmap(progressDefaultBitmap)) {
            canvas.drawBitmap(progressDefaultBitmap, null, progressDefaultDstRect, paint);
        } else {
            if (progressDefaultColor == -1) {
                int[] colors = PseudoColorConfig.getSeekBarColors();
                float[] positions = PseudoColorConfig.getSeekBarAlpha();
                paint.setShader(new LinearGradient(progressWidth, 0f, 0f, 0f, colors, positions, Shader.TileMode.CLAMP));
            } else {
                paint.setColor(progressDefaultColor);
            }
            canvas.drawRoundRect(progressDefaultDstRect, progressRadius, progressRadius, paint);
        }

        if (seekBarMode == SEEKBAR_MODE_RANGE) {

            progressDstRect.top = getProgressTop();
            progressDstRect.left = leftSB.left + leftSB.getThumbScaleWidth() / 2f + progressWidth * leftSB.currPercent;
            progressDstRect.right = rightSB.left + rightSB.getThumbScaleWidth() / 2f + progressWidth * rightSB.currPercent;
            progressDstRect.bottom = getProgressBottom();
        } else {
            progressDstRect.top = getProgressTop();
            progressDstRect.left = leftSB.left + leftSB.getThumbScaleWidth() / 2f;
            progressDstRect.right = leftSB.left + leftSB.getThumbScaleWidth() / 2f + progressWidth * leftSB.currPercent;
            progressDstRect.bottom = getProgressBottom();
        }
        if (colorList != null) {
            paint.setShader(new LinearGradient(progressWidth, 0f, 0f, 0f, colorList, places, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(progressDstRect, progressRadius, progressRadius, paint);
        } else if (progressColor == -1) {
            int[] colors = PseudoColorConfig.getColors(pseudocode);
            float[] positions = PseudoColorConfig.getPositions(pseudocode);
            paint.setShader(new LinearGradient(progressWidth, 0f, 0f, 0f, colors, positions, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(progressDstRect, progressRadius, progressRadius, paint);
        } else {
            if (Utils.verifyBitmap(progressBitmap)) {
                progressSrcRect.top = 0;
                progressSrcRect.bottom = progressBitmap.getHeight();
                int bitmapWidth = progressBitmap.getWidth();
                if (seekBarMode == SEEKBAR_MODE_RANGE) {
                    progressSrcRect.left = (int) (bitmapWidth * leftSB.currPercent);
                    progressSrcRect.right = (int) (bitmapWidth * rightSB.currPercent);
                } else {
                    progressSrcRect.left = 0;
                    progressSrcRect.right = (int) (bitmapWidth * leftSB.currPercent);
                }
                canvas.drawBitmap(progressBitmap, progressSrcRect, progressDstRect, null);
            } else {
                paint.setColor(progressColor);
                canvas.drawRoundRect(progressDstRect, progressRadius, progressRadius, paint);
            }
        }

    }

    protected void onDrawSteps(Canvas canvas, Paint paint) {
        if (!verifyStepsMode()) return;
        int stepMarks = getProgressWidth() / (steps);
        float extHeight = (stepsHeight - getProgressHeight()) / 2f;
        for (int k = 0; k <= steps; k++) {
            float x = getProgressLeft() + k * stepMarks - stepsWidth / 2f;
            stepDivRect.set(x, getProgressTop() - extHeight, x + stepsWidth, getProgressBottom() + extHeight);
            if (stepsBitmaps.isEmpty() || stepsBitmaps.size() <= k) {
                paint.setColor(stepsColor);
                canvas.drawRoundRect(stepDivRect, stepsRadius, stepsRadius, paint);
            } else {
                canvas.drawBitmap(stepsBitmaps.get(k), null, stepDivRect, paint);
            }
        }
    }

    protected void onDrawSeekBar(Canvas canvas) {

        if (leftSB.getIndicatorShowMode() == INDICATOR_ALWAYS_SHOW) {
            leftSB.setShowIndicatorEnable(true);
        }
        leftSB.draw(canvas, true);

        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            if (rightSB.getIndicatorShowMode() == INDICATOR_ALWAYS_SHOW) {
                rightSB.setShowIndicatorEnable(true);
            }
            rightSB.draw(canvas, false);
        }
    }

    private void initPaint() {
        paint.setStyle(Paint.Style.FILL);

        if (progressDefaultColor == -1) {
            int[] colors = PseudoColorConfig.getSeekBarColors();
            float[] positions = PseudoColorConfig.getSeekBarAlpha();
            paint.setShader(new LinearGradient(progressWidth, 0f, 0f, 0f, colors, positions, Shader.TileMode.CLAMP));
        } else {
            paint.setColor(progressDefaultColor);
        }

        paint.setTextSize(tickMarkTextSize);
    }

    private void changeThumbActivateState(boolean hasActivate) {
        if (hasActivate && currTouchSB != null) {
            boolean state = currTouchSB == leftSB;
            leftSB.setActivate(state);
            if (seekBarMode == SEEKBAR_MODE_RANGE)
                rightSB.setActivate(!state);
        } else {
            leftSB.setActivate(false);
            if (seekBarMode == SEEKBAR_MODE_RANGE)
                rightSB.setActivate(false);
        }
    }

    protected float getEventX(MotionEvent event) {
        return event.getX();
    }

    protected float getEventY(MotionEvent event) {
        return event.getY();
    }

    private void scaleCurrentSeekBarThumb() {
        if (currTouchSB != null && currTouchSB.getThumbScaleRatio() > 1f && !isScaleThumb) {
            isScaleThumb = true;
            currTouchSB.scaleThumb();
        }
    }

    private void resetCurrentSeekBarThumb() {
        if (currTouchSB != null && currTouchSB.getThumbScaleRatio() > 1f && isScaleThumb) {
            isScaleThumb = false;
            currTouchSB.resetThumb();
        }
    }

    protected float calculateCurrentSeekBarPercent(float touchDownX) {
        if (currTouchSB == null) return 0;
        float percent = (touchDownX - getProgressLeft()) * 1f / (progressWidth);
        if (touchDownX < getProgressLeft()) {
            percent = 0;
        } else if (touchDownX > getProgressRight()) {
            percent = 1;
        }

        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            if (currTouchSB == leftSB) {
                if (percent > rightSB.currPercent - reservePercent) {
                    percent = rightSB.currPercent - reservePercent;
                }
            } else if (currTouchSB == rightSB) {
                if (percent < leftSB.currPercent + reservePercent) {
                    percent = leftSB.currPercent + reservePercent;
                }
            }
        }
        return percent;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnable) return false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchDownX = getEventX(event);
                touchDownY = getEventY(event);
                if (seekBarMode == SEEKBAR_MODE_RANGE) {
                    if (rightSB.currPercent >= 1 && leftSB.collide(getEventX(event), getEventY(event))) {
                        currTouchSB = leftSB;
                        scaleCurrentSeekBarThumb();
                    } else if (rightSB.collide(getEventX(event), getEventY(event))) {
                        currTouchSB = rightSB;
                        scaleCurrentSeekBarThumb();
                    } else {
                        float performClick = (touchDownX - getProgressLeft()) * 1f / (progressWidth);
                        float distanceLeft = Math.abs(leftSB.currPercent - performClick);
                        float distanceRight = Math.abs(rightSB.currPercent - performClick);
                        if (distanceLeft < distanceRight) {
                            currTouchSB = leftSB;
                        } else {
                            currTouchSB = rightSB;
                        }
                        performClick = calculateCurrentSeekBarPercent(touchDownX);
                        currTouchSB.slide(performClick);
                    }
                } else {
                    currTouchSB = leftSB;
                    scaleCurrentSeekBarThumb();
                }

                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (callback != null) {
                    callback.onStartTrackingTouch(this, currTouchSB == leftSB);
                }
                changeThumbActivateState(true);
                return true;
            case MotionEvent.ACTION_MOVE:
                float x = getEventX(event);
                if ((seekBarMode == SEEKBAR_MODE_RANGE) && leftSB.currPercent == rightSB.currPercent) {
                    currTouchSB.materialRestore();
                    if (callback != null) {
                        callback.onStopTrackingTouch(this, currTouchSB == leftSB);
                    }
                    if (x - touchDownX > 0) {

                        if (currTouchSB != rightSB) {
                            currTouchSB.setShowIndicatorEnable(false);
                            resetCurrentSeekBarThumb();
                            currTouchSB = rightSB;
                        }
                    } else {

                        if (currTouchSB != leftSB) {
                            currTouchSB.setShowIndicatorEnable(false);
                            resetCurrentSeekBarThumb();
                            currTouchSB = leftSB;
                        }
                    }
                    if (callback != null) {
                        callback.onStartTrackingTouch(this, currTouchSB == leftSB);
                    }
                }
                scaleCurrentSeekBarThumb();
                currTouchSB.material = currTouchSB.material >= 1 ? 1 : currTouchSB.material + 0.1f;
                touchDownX = x;
                currTouchSB.slide(calculateCurrentSeekBarPercent(touchDownX));
                currTouchSB.setShowIndicatorEnable(true);

                if (callback != null) {
                    SeekBarState[] states = getRangeSeekBarState();
                    updateTempModeState();
                    callback.onRangeChanged(this, states[0].value, states[1].value, true, tempMode);
                }
                invalidate();

                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                changeThumbActivateState(true);
                break;
            case MotionEvent.ACTION_CANCEL:
                if (seekBarMode == SEEKBAR_MODE_RANGE) {
                    rightSB.setShowIndicatorEnable(false);
                }
                if (currTouchSB == leftSB) {
                    resetCurrentSeekBarThumb();
                } else if (currTouchSB == rightSB) {
                    resetCurrentSeekBarThumb();
                }
                leftSB.setShowIndicatorEnable(false);
                if (callback != null) {
                    SeekBarState[] states = getRangeSeekBarState();
                    updateTempModeState();
                    callback.onRangeChanged(this, states[0].value, states[1].value, false, tempMode);
                }

                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                changeThumbActivateState(false);
                break;
            case MotionEvent.ACTION_UP:
                if (verifyStepsMode() && stepsAutoBonding) {
                    float percent = calculateCurrentSeekBarPercent(getEventX(event));
                    float stepPercent = 1.0f / steps;
                    int stepSelected = new BigDecimal(percent / stepPercent).setScale(0, RoundingMode.HALF_UP).intValue();
                    currTouchSB.slide(stepSelected * stepPercent);
                }

                if (seekBarMode == SEEKBAR_MODE_RANGE) {
                    rightSB.setShowIndicatorEnable(false);
                }
                leftSB.setShowIndicatorEnable(false);
                currTouchSB.materialRestore();
                resetCurrentSeekBarThumb();
                if (callback != null) {
                    SeekBarState[] states = getRangeSeekBarState();
                    updateTempModeState();
                    callback.onRangeChanged(this, states[0].value, states[1].value, false, tempMode);
                }

                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (callback != null) {
                    callback.onStopTrackingTouch(this, currTouchSB == leftSB);
                }
                changeThumbActivateState(false);
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.minValue = minProgress;
        ss.maxValue = maxProgress;
        ss.rangeInterval = minInterval;
        SeekBarState[] results = getRangeSeekBarState();
        ss.currSelectedMin = results[0].value;
        ss.currSelectedMax = results[1].value;
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        try {
            SavedState ss = (SavedState) state;
            super.onRestoreInstanceState(ss.getSuperState());
            float min = ss.minValue;
            float max = ss.maxValue;
            float rangeInterval = ss.rangeInterval;
            setRange(min, max, rangeInterval);
            float currSelectedMin = ss.currSelectedMin;
            float currSelectedMax = ss.currSelectedMax;
            setProgress(currSelectedMin, currSelectedMax);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void setNoNegativeNumber(Boolean noNegativeNumber) {
        this.noNegativeNumber = noNegativeNumber;
        if (leftSB != null) {
            leftSB.setNoNegativeNumber(noNegativeNumber);
        }
        if (rightSB != null) {
            rightSB.setNoNegativeNumber(noNegativeNumber);
        }
    }

    public void setOnRangeChangedListener(OnRangeChangedListener listener) {
        callback = listener;
    }

    public void setProgress(float value) {
        setProgress(value, maxProgress);
    }

    public void setProgressNoCallBack(float leftValue, float rightValue) {
        leftValue = Math.min(leftValue, rightValue);
        rightValue = Math.max(leftValue, rightValue);
        if (rightValue - leftValue < minInterval) {
            if (leftValue - minProgress > maxProgress - rightValue) {
                leftValue = rightValue - minInterval;
            } else {
                rightValue = leftValue + minInterval;
            }
        }

        if (leftValue < minProgress) {
            leftValue = minProgress;
        }

        if (rightValue > maxProgress) {
            rightValue = maxProgress;
        }
        float range = maxProgress - minProgress;
        leftSB.currPercent = Math.abs(leftValue - minProgress) / range;
        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            rightSB.currPercent = Math.abs(rightValue - minProgress) / range;
        }

        postInvalidate();
    }

    @Override
    public void invalidate() {
        if (System.currentTimeMillis() - updateTime < 50) {
            return;
        }
        super.invalidate();
        updateTime = System.currentTimeMillis();
    }

    public void setProgress(float leftValue, float rightValue) {
        leftValue = Math.min(leftValue, rightValue);
        rightValue = Math.max(leftValue, rightValue);
        if (rightValue - leftValue < minInterval) {
            if (leftValue - minProgress > maxProgress - rightValue) {
                leftValue = rightValue - minInterval;
            } else {
                rightValue = leftValue + minInterval;
            }
        }

        if (leftValue < minProgress) {
            leftValue = minProgress;
        }

        if (rightValue > maxProgress) {
            rightValue = maxProgress;
        }
        float range = maxProgress - minProgress;
        leftSB.currPercent = Math.abs(leftValue - minProgress) / range;
        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            rightSB.currPercent = Math.abs(rightValue - minProgress) / range;
        }
        if (callback != null) {
            callback.onRangeChanged(this, leftValue, rightValue, false, tempMode);
        }
        invalidate();
    }

    public void setRange(float min, float max) {
        setRange(min, max, minInterval);
        setProgress(getLeftSeekBar().left, getRightSeekBar().right);
    }

    public void setRangeAndPro(float editMin, float editMax, float realLeftValue, float realRightValue) {
        if (editMin == Float.MIN_VALUE && editMax == Float.MAX_VALUE) {
            setRangeNoInvalidate(realLeftValue, realRightValue, 0.1f);
            setProgressNoCallBack(realLeftValue, realRightValue);
            return;
        }
        setRangeNoInvalidate(realLeftValue, realRightValue, 0.1f);
        if (editMax <= realRightValue && editMin >= realLeftValue) {

            setProgressNoCallBack(editMin, editMax);
        } else if (editMax > realRightValue && editMin < realLeftValue) {

            setProgressNoCallBack(realLeftValue, realRightValue);
        } else if (editMax > realRightValue && editMin > realRightValue) {

            setProgressNoCallBack(realRightValue, realRightValue);
        } else if (editMax < realLeftValue && editMin < realLeftValue) {

            setProgressNoCallBack(realLeftValue, realLeftValue);
        } else if (editMax <= realRightValue && editMin < realLeftValue) {

            setProgressNoCallBack(realLeftValue, editMax);
        } else if (editMax > realRightValue && editMin >= realLeftValue) {

            setProgressNoCallBack(editMin, realRightValue);
        }
    }

    public void setRange(float min, float max, float minInterval) {

        if (maxProgress == max && min == minProgress) {

            return;
        }
        maxProgress = max;
        minProgress = min;
        this.minInterval = minInterval;
        reservePercent = minInterval / (max - min);

        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            if (leftSB.currPercent + reservePercent <= 1 && leftSB.currPercent + reservePercent > rightSB.currPercent) {
                rightSB.currPercent = leftSB.currPercent + reservePercent;
            } else if (rightSB.currPercent - reservePercent >= 0 && rightSB.currPercent - reservePercent < leftSB.currPercent) {
                leftSB.currPercent = rightSB.currPercent - reservePercent;
            }
        }
        postInvalidate();
    }

    public void setRangeNoInvalidate(float min, float max, float minInterval) {

        if (maxProgress == max && min == minProgress) {

            return;
        }
        maxProgress = max;
        minProgress = min;
        this.minInterval = minInterval;
        reservePercent = minInterval / (max - min);

        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            if (leftSB.currPercent + reservePercent <= 1 && leftSB.currPercent + reservePercent > rightSB.currPercent) {
                rightSB.currPercent = leftSB.currPercent + reservePercent;
            } else if (rightSB.currPercent - reservePercent >= 0 && rightSB.currPercent - reservePercent < leftSB.currPercent) {
                leftSB.currPercent = rightSB.currPercent - reservePercent;
            }
        }
    }

    public SeekBarState[] getRangeSeekBarState() {
        SeekBarState leftSeekBarState = new SeekBarState();
        leftSeekBarState.value = leftSB.getProgress();

        leftSeekBarState.indicatorText = String.valueOf(leftSeekBarState.value);
        if (Utils.compareFloat(leftSeekBarState.value, minProgress) == 0) {
            leftSeekBarState.isMin = true;
        } else if (Utils.compareFloat(leftSeekBarState.value, maxProgress) == 0) {
            leftSeekBarState.isMax = true;
        }

        SeekBarState rightSeekBarState = new SeekBarState();
        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            rightSeekBarState.value = rightSB.getProgress();
            rightSeekBarState.indicatorText = String.valueOf(rightSeekBarState.value);
            if (Utils.compareFloat(rightSB.currPercent, minProgress) == 0) {
                rightSeekBarState.isMin = true;
            } else if (Utils.compareFloat(rightSB.currPercent, maxProgress) == 0) {
                rightSeekBarState.isMax = true;
            }
        }

        return new SeekBarState[]{leftSeekBarState, rightSeekBarState};
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.isEnable = enabled;
    }

    public void setIndicatorText(String progress) {
        leftSB.setIndicatorText(progress);
        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            rightSB.setIndicatorText(progress);
        }
    }

    public void setIndicatorTextDecimalFormat(String formatPattern) {
        leftSB.setIndicatorTextDecimalFormat(formatPattern);
        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            rightSB.setIndicatorTextDecimalFormat(formatPattern);
        }
    }

    public void setIndicatorTextStringFormat(String formatPattern) {
        leftSB.setIndicatorTextStringFormat(formatPattern);
        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            rightSB.setIndicatorTextStringFormat(formatPattern);
        }
    }

    public SeekBar getLeftSeekBar() {
        return leftSB;
    }

    public SeekBar getRightSeekBar() {
        return rightSB;
    }

    public int getProgressTop() {
        return progressTop;
    }

    public void setProgressTop(int progressTop) {
        this.progressTop = progressTop;
    }

    public int getProgressBottom() {
        return progressBottom;
    }

    public void setProgressBottom(int progressBottom) {
        this.progressBottom = progressBottom;
    }

    public int getProgressLeft() {
        return progressLeft;
    }

    public void setProgressLeft(int progressLeft) {
        this.progressLeft = progressLeft;
    }

    public int getProgressRight() {
        return progressRight;
    }

    public void setProgressRight(int progressRight) {
        this.progressRight = progressRight;
    }

    public int getProgressPaddingRight() {
        return progressPaddingRight;
    }

    public int getProgressHeight() {
        return progressHeight;
    }

    public void setProgressHeight(int progressHeight) {
        this.progressHeight = progressHeight;
    }

    public float getMinProgress() {
        return minProgress;
    }

    public float getMaxProgress() {
        return maxProgress;
    }

    public void setProgressColor(@ColorInt int progressDefaultColor, @ColorInt int progressColor) {
        this.progressDefaultColor = progressDefaultColor;
        this.progressColor = progressColor;
    }

    public int getTickMarkTextColor() {
        return tickMarkTextColor;
    }

    public void setTickMarkTextColor(@ColorInt int tickMarkTextColor) {
        this.tickMarkTextColor = tickMarkTextColor;
    }

    public int getTickMarkInRangeTextColor() {
        return tickMarkInRangeTextColor;
    }

    public void setTickMarkInRangeTextColor(@ColorInt int tickMarkInRangeTextColor) {
        this.tickMarkInRangeTextColor = tickMarkInRangeTextColor;
    }

    public int getSeekBarMode() {
        return seekBarMode;
    }

    public void setSeekBarMode(@SeekBarModeDef int seekBarMode) {
        this.seekBarMode = seekBarMode;
        rightSB.setVisible(seekBarMode != SEEKBAR_MODE_SINGLE);
    }

    public int getTickMarkMode() {
        return tickMarkMode;
    }

    public void setTickMarkMode(@TickMarkModeDef int tickMarkMode) {
        this.tickMarkMode = tickMarkMode;
    }

    public int getTickMarkTextMargin() {
        return tickMarkTextMargin;
    }

    public void setTickMarkTextMargin(int tickMarkTextMargin) {
        this.tickMarkTextMargin = tickMarkTextMargin;
    }

    public int getTickMarkTextSize() {
        return tickMarkTextSize;
    }

    public void setTickMarkTextSize(int tickMarkTextSize) {
        this.tickMarkTextSize = tickMarkTextSize;
    }

    public int getTickMarkGravity() {
        return tickMarkGravity;
    }

    public void setTickMarkGravity(@TickMarkGravityDef int tickMarkGravity) {
        this.tickMarkGravity = tickMarkGravity;
    }

    public CharSequence[] getTickMarkTextArray() {
        return tickMarkTextArray;
    }

    public void setTickMarkTextArray(CharSequence[] tickMarkTextArray) {
        this.tickMarkTextArray = tickMarkTextArray;
    }

    public float getMinInterval() {
        return minInterval;
    }

    public float getProgressRadius() {
        return progressRadius;
    }

    public void setProgressRadius(float progressRadius) {
        this.progressRadius = progressRadius;
    }

    public int getProgressColor() {
        return progressColor;
    }

    public void setProgressColor(@ColorInt int progressColor) {
        this.progressColor = progressColor;
    }

    public int getProgressDefaultColor() {
        return progressDefaultColor;
    }

    public void setProgressDefaultColor(@ColorInt int progressDefaultColor) {
        this.progressDefaultColor = progressDefaultColor;
    }

    public int getProgressDrawableId() {
        return progressDrawableId;
    }

    public void setProgressDrawableId(@DrawableRes int progressDrawableId) {
        this.progressDrawableId = progressDrawableId;
        progressBitmap = null;
        initProgressBitmap();
    }

    public int getProgressDefaultDrawableId() {
        return progressDefaultDrawableId;
    }

    public void setProgressDefaultDrawableId(@DrawableRes int progressDefaultDrawableId) {
        this.progressDefaultDrawableId = progressDefaultDrawableId;
        progressDefaultBitmap = null;
        initProgressBitmap();
    }

    public int getProgressWidth() {
        return progressWidth;
    }

    public void setProgressWidth(int progressWidth) {
        this.progressWidth = progressWidth;
    }

    public void setTypeface(Typeface typeFace) {
        paint.setTypeface(typeFace);
    }

    public boolean isEnableThumbOverlap() {
        return enableThumbOverlap;
    }

    public void setEnableThumbOverlap(boolean enableThumbOverlap) {
        this.enableThumbOverlap = enableThumbOverlap;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    public int getStepsColor() {
        return stepsColor;
    }

    public void setStepsColor(@ColorInt int stepsColor) {
        this.stepsColor = stepsColor;
    }

    public float getStepsWidth() {
        return stepsWidth;
    }

    public void setStepsWidth(float stepsWidth) {
        this.stepsWidth = stepsWidth;
    }

    public float getStepsHeight() {
        return stepsHeight;
    }

    public void setStepsHeight(float stepsHeight) {
        this.stepsHeight = stepsHeight;
    }

    public float getStepsRadius() {
        return stepsRadius;
    }

    public void setStepsRadius(float stepsRadius) {
        this.stepsRadius = stepsRadius;
    }

    public int getTickMarkLayoutGravity() {
        return tickMarkLayoutGravity;
    }

    public void setTickMarkLayoutGravity(@TickMarkLayoutGravityDef int tickMarkLayoutGravity) {
        this.tickMarkLayoutGravity = tickMarkLayoutGravity;
    }

    public int getGravity() {
        return gravity;
    }

    public void setGravity(@GravityDef int gravity) {
        this.gravity = gravity;
    }

    public boolean isStepsAutoBonding() {
        return stepsAutoBonding;
    }

    public void setStepsAutoBonding(boolean stepsAutoBonding) {
        this.stepsAutoBonding = stepsAutoBonding;
    }

    public int getStepsDrawableId() {
        return stepsDrawableId;
    }

    public void setStepsDrawableId(@DrawableRes int stepsDrawableId) {
        this.stepsBitmaps.clear();
        this.stepsDrawableId = stepsDrawableId;
        initStepsBitmap();
    }

    public List<Bitmap> getStepsBitmaps() {
        return stepsBitmaps;
    }

    public void setStepsBitmaps(List<Bitmap> stepsBitmaps) {

        this.stepsBitmaps.clear();
        this.stepsBitmaps.addAll(stepsBitmaps);
    }

    public void setStepsDrawable(List<Integer> stepsDrawableIds) {

        if (!verifyStepsMode()) {
            throw new IllegalArgumentException("stepsWidth must > 0, stepsHeight must > 0,steps must > 0 First!!");
        }
        List<Bitmap> stepsBitmaps = new ArrayList<>();
        for (int i = 0; i < stepsDrawableIds.size(); i++) {
            stepsBitmaps.add(Utils.drawableToBitmap(getContext(), (int) stepsWidth, (int) stepsHeight, stepsDrawableIds.get(i)));
        }
        setStepsBitmaps(stepsBitmaps);
    }

    public void setPseudocode(int pseudocode) {
        this.pseudocode = pseudocode;
        invalidate();
    }

    public void setColorList(@Nullable int[] colorList) {
        this.colorList = colorList;
        invalidate();
    }

    public void setPlaces(@Nullable float[] newPlaces) {
        if (newPlaces == null) {
            places = null;
        } else {
            if (places == null || places.length != newPlaces.length) {
                places = new float[newPlaces.length];
            }
            for (int i = 0; i < newPlaces.length; i++) {
                places[places.length - 1 - i] = 1 - newPlaces[i];
            }
        }
        invalidate();
    }

    public void drawIndPath(boolean isEnabled) {
        // Placeholder implementation
        invalidate();
    }

    @IntDef({SEEKBAR_MODE_SINGLE, SEEKBAR_MODE_RANGE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SeekBarModeDef {
    }

    @IntDef({TRICK_MARK_MODE_NUMBER, TRICK_MARK_MODE_OTHER})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TickMarkModeDef {
    }

    @IntDef({TICK_MARK_GRAVITY_LEFT, TICK_MARK_GRAVITY_CENTER, TICK_MARK_GRAVITY_RIGHT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TickMarkGravityDef {
    }

    @IntDef({Gravity.TOP, Gravity.BOTTOM})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TickMarkLayoutGravityDef {
    }

    @IntDef({Gravity.TOP, Gravity.CENTER, Gravity.BOTTOM})
    @Retention(RetentionPolicy.SOURCE)
    public @interface GravityDef {
    }

    public static class Gravity {
        public final static int TOP = 0;
        public final static int BOTTOM = 1;
        public final static int CENTER = 2;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\seekbar\SavedState.java =====

package com.mpdc4gsr.libunified.ui.widget.seekbar;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

public class SavedState extends View.BaseSavedState {
    public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
        public SavedState createFromParcel(Parcel in) {
            return new SavedState(in);
        }

        public SavedState[] newArray(int size) {
            return new SavedState[size];
        }
    };
    public float minValue;
    public float maxValue;
    public float rangeInterval;
    public int tickNumber;
    public float currSelectedMin;
    public float currSelectedMax;

    public SavedState(Parcelable superState) {
        super(superState);
    }

    private SavedState(Parcel in) {
        super(in);
        minValue = in.readFloat();
        maxValue = in.readFloat();
        rangeInterval = in.readFloat();
        tickNumber = in.readInt();
        currSelectedMin = in.readFloat();
        currSelectedMax = in.readFloat();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeFloat(minValue);
        out.writeFloat(maxValue);
        out.writeFloat(rangeInterval);
        out.writeInt(tickNumber);
        out.writeFloat(currSelectedMin);
        out.writeFloat(currSelectedMax);
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\seekbar\SeekBar.java =====

package com.mpdc4gsr.libunified.ui.widget.seekbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.*;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.mpdc4gsr.libunified.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.DecimalFormat;
import java.util.Locale;

public class SeekBar {

    public static final int INDICATOR_SHOW_WHEN_TOUCH = 0;
    public static final int INDICATOR_ALWAYS_HIDE = 1;
    public static final int INDICATOR_ALWAYS_SHOW_AFTER_TOUCH = 2;
    public static final int INDICATOR_ALWAYS_SHOW = 3;
    public static final int WRAP_CONTENT = -1;
    public static final int MATCH_PARENT = -2;
    float thumbScaleRatio;
    int left, right, top, bottom;
    float currPercent;
    float material = 0;
    boolean isLeft;
    Bitmap thumbBitmap;
    Bitmap thumbInactivatedBitmap;
    Bitmap indicatorBitmap;
    ValueAnimator anim;
    String userText2Draw;
    boolean isActivate = false;
    boolean isVisible = true;
    RangeSeekBar rangeSeekBar;
    String indicatorTextStringFormat;
    Path indicatorArrowPath = new Path();
    Rect indicatorTextRect = new Rect();
    Rect indicatorRect = new Rect();
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    DecimalFormat indicatorTextDecimalFormat;
    int scaleThumbWidth;
    int scaleThumbHeight;
    private boolean thumbShow;
    private int indicatorShowMode;
    private int indicatorHeight;
    private int indicatorWidth;
    private int indicatorMargin;
    private int indicatorDrawableId;
    private int indicatorArrowSize;
    private int indicatorTextSize;
    private int indicatorTextColor;
    private float indicatorRadius;
    private int indicatorBackgroundColor;
    private int indicatorPaddingLeft, indicatorPaddingRight, indicatorPaddingTop, indicatorPaddingBottom;
    private int thumbDrawableId;
    private int thumbInactivatedDrawableId;
    private int thumbWidth;
    private int thumbHeight;
    private boolean isShowIndicator;
    private boolean noNegativeNumber = false;

    public SeekBar(RangeSeekBar rangeSeekBar, AttributeSet attrs, boolean isLeft) {
        this.rangeSeekBar = rangeSeekBar;
        this.isLeft = isLeft;
        initAttrs(attrs);
        initBitmap();
        initVariables();
    }

    private void initAttrs(AttributeSet attrs) {
        TypedArray t = getContext().obtainStyledAttributes(attrs, R.styleable.RangeSeekBar);
        if (t == null) return;
        indicatorMargin = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_indicator_margin, 0);
        indicatorDrawableId = t.getResourceId(R.styleable.RangeSeekBar_rsb_indicator_drawable, 0);
        indicatorShowMode = t.getInt(R.styleable.RangeSeekBar_rsb_indicator_show_mode, INDICATOR_ALWAYS_HIDE);
        indicatorHeight = t.getLayoutDimension(R.styleable.RangeSeekBar_rsb_indicator_height, WRAP_CONTENT);
        indicatorWidth = t.getLayoutDimension(R.styleable.RangeSeekBar_rsb_indicator_width, WRAP_CONTENT);
        indicatorTextSize = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_indicator_text_size, Utils.dp2px(getContext(), 14));
        indicatorTextColor = t.getColor(R.styleable.RangeSeekBar_rsb_indicator_text_color, Color.WHITE);
        indicatorBackgroundColor = t.getColor(R.styleable.RangeSeekBar_rsb_indicator_background_color, ContextCompat.getColor(getContext(), R.color.colorAccent));
        indicatorPaddingLeft = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_indicator_padding_left, 0);
        indicatorPaddingRight = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_indicator_padding_right, 0);
        indicatorPaddingTop = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_indicator_padding_top, 0);
        indicatorPaddingBottom = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_indicator_padding_bottom, 0);
        indicatorArrowSize = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_indicator_arrow_size, 0);
        thumbDrawableId = t.getResourceId(R.styleable.RangeSeekBar_rsb_thumb_drawable, R.drawable.rsb_default_thumb);
        thumbShow = t.getBoolean(R.styleable.RangeSeekBar_rsb_show_thumb, false);
        thumbInactivatedDrawableId = t.getResourceId(R.styleable.RangeSeekBar_rsb_thumb_inactivated_drawable, 0);
        thumbWidth = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_thumb_width, Utils.dp2px(getContext(), 26));
        thumbHeight = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_thumb_height, Utils.dp2px(getContext(), 26));
        thumbScaleRatio = t.getFloat(R.styleable.RangeSeekBar_rsb_thumb_scale_ratio, 1f);
        indicatorRadius = t.getDimension(R.styleable.RangeSeekBar_rsb_indicator_radius, 0f);
        t.recycle();
    }

    protected void initVariables() {
        scaleThumbWidth = thumbWidth;
        scaleThumbHeight = thumbHeight;
        if (indicatorHeight == WRAP_CONTENT) {
            indicatorHeight = Utils.measureText("8", indicatorTextSize).height() + indicatorPaddingTop + indicatorPaddingBottom;
        }
        if (indicatorArrowSize <= 0) {
            indicatorArrowSize = (int) (thumbWidth / 4);
        }
    }

    public Context getContext() {
        return rangeSeekBar.getContext();
    }

    public Resources getResources() {
        if (getContext() != null) return getContext().getResources();
        return null;
    }

    private void initBitmap() {
        setIndicatorDrawableId(indicatorDrawableId);
        setThumbDrawableId(thumbDrawableId, thumbWidth, thumbHeight);
        setThumbInactivatedDrawableId(thumbInactivatedDrawableId, thumbWidth, thumbHeight);
    }

    protected void onSizeChanged(int x, int y) {
        initVariables();
        initBitmap();
        left = (int) (x - getThumbScaleWidth() / 2);
        right = (int) (x + getThumbScaleWidth() / 2);
        top = y - getThumbHeight() / 2;
        bottom = y + getThumbHeight() / 2;
    }

    public void scaleThumb() {
        scaleThumbWidth = (int) getThumbScaleWidth();
        scaleThumbHeight = (int) getThumbScaleHeight();
        int y = rangeSeekBar.getProgressBottom();
        top = y - scaleThumbHeight / 2;
        bottom = y + scaleThumbHeight / 2;
        setThumbDrawableId(thumbDrawableId, scaleThumbWidth, scaleThumbHeight);
    }

    public void resetThumb() {
        scaleThumbWidth = getThumbWidth();
        scaleThumbHeight = getThumbHeight();
        int y = rangeSeekBar.getProgressBottom();
        top = y - scaleThumbHeight / 2;
        bottom = y + scaleThumbHeight / 2;
        setThumbDrawableId(thumbDrawableId, scaleThumbWidth, scaleThumbHeight);
    }

    public float getRawHeight() {
        return getIndicatorHeight() + getIndicatorArrowSize() + getIndicatorMargin() + getThumbScaleHeight();
    }

    public void setNoNegativeNumber(Boolean noNegativeNumber) {
        this.noNegativeNumber = noNegativeNumber;
    }

    protected void draw(Canvas canvas, boolean isLeft) {
        if (!isVisible) {
            return;
        }
        int offset = (int) (rangeSeekBar.getProgressWidth() * currPercent);
        canvas.save();
        canvas.translate(offset, 0);

        canvas.translate(left, 0);
        if (isShowIndicator) {
            onDrawIndicator(canvas, paint, formatCurrentIndicatorText(userText2Draw)); //
        }

        if (thumbShow) {
            onDrawThumb(canvas);
        } else {
            onDrawThumb(canvas, isLeft); //
        }
        canvas.restore();
    }

    protected void onDrawThumb(Canvas canvas) {
        if (thumbInactivatedBitmap != null && !isActivate) {
            canvas.drawBitmap(thumbInactivatedBitmap, 0, rangeSeekBar.getProgressTop() + (rangeSeekBar.getProgressHeight() - scaleThumbHeight) / 2f, null);
        } else if (thumbBitmap != null) {

            canvas.drawBitmap(thumbBitmap, 0, rangeSeekBar.getProgressTop() + (rangeSeekBar.getProgressHeight() - scaleThumbHeight) / 2f, null);
        }
    }

    protected void onDrawThumb(Canvas canvas, Boolean isLeft) {
        if (thumbInactivatedBitmap != null && !isActivate) {

        } else if (thumbBitmap != null) {

            Matrix matrix = new Matrix();
            int offX = thumbBitmap.getWidth() / 2;
            int offY = thumbBitmap.getHeight() / 2;
            matrix.postTranslate(-offX, -offY);
            if (isLeft) {
                matrix.postRotate(90);
                offX = offX - 5;
            } else {
                matrix.postRotate(270);
                offX = offX + 5;
            }

        }
    }

    protected String formatCurrentIndicatorText(String text2Draw) {
        SeekBarState[] states = rangeSeekBar.getRangeSeekBarState();
        if (TextUtils.isEmpty(text2Draw)) {
            if (isLeft) {
                if (indicatorTextDecimalFormat != null) {
                    text2Draw = indicatorTextDecimalFormat.format(states[0].value);
                } else {
                    text2Draw = states[0].indicatorText;
                }
            } else {
                if (indicatorTextDecimalFormat != null) {
                    text2Draw = indicatorTextDecimalFormat.format(states[1].value);
                } else {
                    text2Draw = states[1].indicatorText;
                }
            }
        }
        if (indicatorTextStringFormat != null) {

            text2Draw = String.format(Locale.ENGLISH, indicatorTextStringFormat, Float.parseFloat(text2Draw));
        }
        return text2Draw;
    }

    protected void onDrawIndicator(Canvas canvas, Paint paint, String text2Draw) {
        try {
            if (text2Draw == null) return;
            paint.setTextSize(indicatorTextSize);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(indicatorBackgroundColor);
            if (noNegativeNumber) {
                text2Draw = text2Draw.replace("-", "");
            }
            paint.getTextBounds(text2Draw, 0, text2Draw.length(), indicatorTextRect);
            int realIndicatorWidth = indicatorWidth + indicatorPaddingLeft + indicatorPaddingRight;
            if (indicatorWidth > realIndicatorWidth) {
                realIndicatorWidth = indicatorWidth;
            }

            int realIndicatorHeight = indicatorTextRect.height() + indicatorPaddingTop + indicatorPaddingBottom;
            if (indicatorHeight > realIndicatorHeight) {
                realIndicatorHeight = indicatorHeight;
            }

            indicatorRect.left = (int) (scaleThumbWidth / 2f - realIndicatorWidth / 2f);
            indicatorRect.top = bottom - realIndicatorHeight - scaleThumbHeight - indicatorMargin;
            indicatorRect.right = indicatorRect.left + realIndicatorWidth;
            indicatorRect.bottom = indicatorRect.top + realIndicatorHeight;

            if (indicatorBitmap == null) {

                int ax = scaleThumbWidth / 2;
                int ay = indicatorRect.bottom;
                int bx = ax - indicatorArrowSize;
                int by = ay - indicatorArrowSize;
                int cx = ax + indicatorArrowSize;
                indicatorArrowPath.reset();
                indicatorArrowPath.moveTo(ax, ay);
                indicatorArrowPath.lineTo(bx, by);
                indicatorArrowPath.lineTo(cx, by);
                indicatorArrowPath.close();
                canvas.drawPath(indicatorArrowPath, paint);
                indicatorRect.bottom -= indicatorArrowSize;
                indicatorRect.top -= indicatorArrowSize;
                Log.w("pseudo colorrefresh", "///");
            }

            int defaultPaddingOffset = Utils.dp2px(getContext(), 1);
            int leftOffset = indicatorRect.width() / 2 - (int) (rangeSeekBar.getProgressWidth() * currPercent) - rangeSeekBar.getProgressLeft() + defaultPaddingOffset;
            int rightOffset = indicatorRect.width() / 2 - (int) (rangeSeekBar.getProgressWidth() * (1 - currPercent)) - rangeSeekBar.getProgressPaddingRight() + defaultPaddingOffset;

            if (leftOffset > 0) {
                indicatorRect.left += leftOffset;
                indicatorRect.right += leftOffset;
            } else if (rightOffset > 0) {
                indicatorRect.left -= rightOffset;
                indicatorRect.right -= rightOffset;
            }

            if (indicatorBitmap != null) {
                int offset = (int) (rangeSeekBar.getProgressWidth() * currPercent);

                Rect rect = new Rect(indicatorRect.left, indicatorRect.top, indicatorWidth, indicatorRect.bottom);
                Utils.drawBitmap(canvas, paint, indicatorBitmap, rect);
            } else if (indicatorRadius > 0f) {
                canvas.drawRoundRect(new RectF(indicatorRect), indicatorRadius, indicatorRadius, paint);
            } else {
                canvas.drawRect(indicatorRect, paint);
            }

            int tx, ty;
            if (indicatorPaddingLeft > 0) {
                tx = indicatorRect.left + indicatorPaddingLeft;
            } else if (indicatorPaddingRight > 0) {
                tx = indicatorRect.right - indicatorPaddingRight - indicatorTextRect.width();
            } else {
                tx = indicatorRect.left + (realIndicatorWidth - indicatorTextRect.width()) / 2;
            }

            if (indicatorPaddingTop > 0) {
                ty = indicatorRect.top + indicatorTextRect.height() + indicatorPaddingTop;
            } else if (indicatorPaddingBottom > 0) {
                ty = indicatorRect.bottom - indicatorTextRect.height() - indicatorPaddingBottom;
            } else {
                ty = indicatorRect.bottom - (realIndicatorHeight - indicatorTextRect.height()) / 2 + 1;
            }

            paint.setColor(indicatorTextColor);
            canvas.drawText(text2Draw, tx, ty, paint);
        } catch (Exception e) {
            Log.w("", e.getMessage() + "");
        }
    }

    protected boolean collide(float x, float y) {
        int offset = (int) (rangeSeekBar.getProgressWidth() * currPercent);
        return x > left + offset && x < right + offset && y > top && y < bottom;
    }

    protected void slide(float percent) {
        if (percent < 0) percent = 0;
        else if (percent > 1) percent = 1;
        currPercent = percent;
    }

    protected void setShowIndicatorEnable(boolean isEnable) {
        switch (indicatorShowMode) {
            case INDICATOR_SHOW_WHEN_TOUCH:
                isShowIndicator = isEnable;
                break;
            case INDICATOR_ALWAYS_SHOW:
            case INDICATOR_ALWAYS_SHOW_AFTER_TOUCH:
                isShowIndicator = true;
                break;
            case INDICATOR_ALWAYS_HIDE:
                isShowIndicator = false;
                break;
        }
    }

    public void materialRestore() {
        if (rangeSeekBar != null && !rangeSeekBar.isAttachedToWindow()) {
            return;
        }
        if (anim != null) anim.cancel();
        anim = ValueAnimator.ofFloat(material, 0);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                material = (float) animation.getAnimatedValue();
                if (rangeSeekBar != null) rangeSeekBar.invalidate();
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                material = 0;
                if (rangeSeekBar != null) rangeSeekBar.invalidate();
            }
        });
        try {
            anim.start();
        } catch (IllegalStateException e) {
            Log.w("SeekBar", "Failed to start material restore animation: " + e.getMessage());
        }
    }

    public void setIndicatorText(String text) {
        userText2Draw = text;
    }

    public DecimalFormat getIndicatorTextDecimalFormat() {
        return indicatorTextDecimalFormat;
    }

    public void setIndicatorTextDecimalFormat(String formatPattern) {
        indicatorTextDecimalFormat = new DecimalFormat(formatPattern);
    }

    public void setIndicatorTextStringFormat(String formatPattern) {
        indicatorTextStringFormat = formatPattern;
    }

    public int getIndicatorDrawableId() {
        return indicatorDrawableId;
    }

    public void setIndicatorDrawableId(@DrawableRes int indicatorDrawableId) {
        if (indicatorDrawableId != 0) {
            this.indicatorDrawableId = indicatorDrawableId;
            indicatorBitmap = BitmapFactory.decodeResource(getResources(), indicatorDrawableId);
            if (indicatorBitmap == null) {
                indicatorBitmap = Utils.drawableToBitmap(indicatorWidth, indicatorHeight,
                        ResourcesCompat.getDrawable(getResources(), indicatorDrawableId, getContext().getTheme()));
            }
        }
    }

    public int getIndicatorArrowSize() {
        return indicatorArrowSize;
    }

    public void setIndicatorArrowSize(int indicatorArrowSize) {
        this.indicatorArrowSize = indicatorArrowSize;
    }

    public int getIndicatorPaddingLeft() {
        return indicatorPaddingLeft;
    }

    public void setIndicatorPaddingLeft(int indicatorPaddingLeft) {
        this.indicatorPaddingLeft = indicatorPaddingLeft;
    }

    public int getIndicatorPaddingRight() {
        return indicatorPaddingRight;
    }

    public void setIndicatorPaddingRight(int indicatorPaddingRight) {
        this.indicatorPaddingRight = indicatorPaddingRight;
    }

    public int getIndicatorPaddingTop() {
        return indicatorPaddingTop;
    }

    public void setIndicatorPaddingTop(int indicatorPaddingTop) {
        this.indicatorPaddingTop = indicatorPaddingTop;
    }

    public int getIndicatorPaddingBottom() {
        return indicatorPaddingBottom;
    }

    public void setIndicatorPaddingBottom(int indicatorPaddingBottom) {
        this.indicatorPaddingBottom = indicatorPaddingBottom;
    }

    public int getIndicatorMargin() {
        return indicatorMargin;
    }

    public void setIndicatorMargin(int indicatorMargin) {
        this.indicatorMargin = indicatorMargin;
    }

    public int getIndicatorShowMode() {
        return indicatorShowMode;
    }

    public void setIndicatorShowMode(@IndicatorModeDef int indicatorShowMode) {
        this.indicatorShowMode = indicatorShowMode;
    }

    public void showIndicator(boolean isShown) {
        isShowIndicator = isShown;
    }

    public boolean isShowIndicator() {
        return isShowIndicator;
    }

    public int getIndicatorRawHeight() {
        if (indicatorHeight > 0) {
            if (indicatorBitmap != null) {
                return indicatorHeight + indicatorMargin;
            } else {
                return indicatorHeight + indicatorArrowSize + indicatorMargin;
            }
        } else {
            if (indicatorBitmap != null) {
                return Utils.measureText("8", indicatorTextSize).height() + indicatorPaddingTop + indicatorPaddingBottom + indicatorMargin;
            } else {
                return Utils.measureText("8", indicatorTextSize).height() + indicatorPaddingTop + indicatorPaddingBottom + indicatorMargin + indicatorArrowSize;
            }
        }
    }

    public int getIndicatorHeight() {
        return indicatorHeight;
    }

    public void setIndicatorHeight(int indicatorHeight) {
        this.indicatorHeight = indicatorHeight;
    }

    public int getIndicatorWidth() {
        return indicatorWidth;
    }

    public void setIndicatorWidth(int indicatorWidth) {
        this.indicatorWidth = indicatorWidth;
    }

    public int getIndicatorTextSize() {
        return indicatorTextSize;
    }

    public void setIndicatorTextSize(int indicatorTextSize) {
        this.indicatorTextSize = indicatorTextSize;
    }

    public int getIndicatorTextColor() {
        return indicatorTextColor;
    }

    public void setIndicatorTextColor(@ColorInt int indicatorTextColor) {
        this.indicatorTextColor = indicatorTextColor;
    }

    public int getIndicatorBackgroundColor() {
        return indicatorBackgroundColor;
    }

    public void setIndicatorBackgroundColor(@ColorInt int indicatorBackgroundColor) {
        this.indicatorBackgroundColor = indicatorBackgroundColor;
    }

    public int getThumbInactivatedDrawableId() {
        return thumbInactivatedDrawableId;
    }

    public void setThumbInactivatedDrawableId(@DrawableRes int thumbInactivatedDrawableId, int width, int height) {
        if (thumbInactivatedDrawableId != 0 && getResources() != null) {
            this.thumbInactivatedDrawableId = thumbInactivatedDrawableId;
            thumbInactivatedBitmap = Utils.drawableToBitmap(width, height,
                    ResourcesCompat.getDrawable(getResources(), thumbInactivatedDrawableId, getContext().getTheme()));
        }
    }

    public int getThumbDrawableId() {
        return thumbDrawableId;
    }

    public void setThumbDrawableId(@DrawableRes int thumbDrawableId) {
        if (thumbWidth <= 0 || thumbHeight <= 0) {
            throw new IllegalArgumentException("please set thumbWidth and thumbHeight first!");
        }
        if (thumbDrawableId != 0 && getResources() != null) {
            this.thumbDrawableId = thumbDrawableId;
            thumbBitmap = Utils.drawableToBitmap(thumbWidth, thumbHeight,
                    ResourcesCompat.getDrawable(getResources(), thumbDrawableId, getContext().getTheme()));
        }
    }

    public void setThumbDrawableId(@DrawableRes int thumbDrawableId, int width, int height) {
        if (thumbDrawableId != 0 && getResources() != null && width > 0 && height > 0) {
            this.thumbDrawableId = thumbDrawableId;
            thumbBitmap = Utils.drawableToBitmap(width, height,
                    ResourcesCompat.getDrawable(getResources(), thumbDrawableId, getContext().getTheme()));
        }
    }

    public int getThumbWidth() {
        return thumbWidth;
    }

    public void setThumbWidth(int thumbWidth) {
        this.thumbWidth = thumbWidth;
    }

    public float getThumbScaleHeight() {
        return thumbHeight * thumbScaleRatio;
    }

    public float getThumbScaleWidth() {
        return thumbWidth * thumbScaleRatio;
    }

    public int getThumbHeight() {
        return thumbHeight;
    }

    public void setThumbHeight(int thumbHeight) {
        this.thumbHeight = thumbHeight;
    }

    public float getIndicatorRadius() {
        return indicatorRadius;
    }

    public void setIndicatorRadius(float indicatorRadius) {
        this.indicatorRadius = indicatorRadius;
    }

    protected boolean getActivate() {
        return isActivate;
    }

    protected void setActivate(boolean activate) {
        isActivate = activate;
    }

    public void setTypeface(Typeface typeFace) {
        paint.setTypeface(typeFace);
    }

    public float getThumbScaleRatio() {
        return thumbScaleRatio;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    public float getProgress() {
        float range = rangeSeekBar.getMaxProgress() - rangeSeekBar.getMinProgress();
        return rangeSeekBar.getMinProgress() + range * currPercent;
    }

    @IntDef({INDICATOR_SHOW_WHEN_TOUCH, INDICATOR_ALWAYS_HIDE, INDICATOR_ALWAYS_SHOW_AFTER_TOUCH, INDICATOR_ALWAYS_SHOW})
    @Retention(RetentionPolicy.SOURCE)
    public @interface IndicatorModeDef {
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\seekbar\SeekBarState.java =====

package com.mpdc4gsr.libunified.ui.widget.seekbar;

public class SeekBarState {
    public String indicatorText;
    public float value; //now progress value
    public boolean isMin;
    public boolean isMax;

    @Override
    public String toString() {
        return "indicatorText: " + indicatorText + " ,isMin: " + isMin + " ,isMax: " + isMax;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\seekbar\Utils.java =====

package com.mpdc4gsr.libunified.ui.widget.seekbar;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

public class Utils {

    private static final String TAG = "RangeSeekBar";

    public static void print(String log) {
        Log.d(TAG, log);
    }

    public static void print(Object... logs) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Object log : logs) {
            stringBuilder.append(log);
        }
        Log.d(TAG, stringBuilder.toString());
    }

    public static Bitmap drawableToBitmap(Context context, int width, int height, int drawableId) {
        if (context == null || width <= 0 || height <= 0 || drawableId == 0) return null;
        Drawable drawable = ResourcesCompat.getDrawable(context.getResources(), drawableId, context.getTheme());
        return Utils.drawableToBitmap(width, height, drawable);
    }

    public static Bitmap drawableToBitmap(int width, int height, Drawable drawable) {
        Bitmap bitmap = null;
        try {
            if (drawable instanceof BitmapDrawable) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
                bitmap = bitmapDrawable.getBitmap();
                if (bitmap != null && bitmap.getHeight() > 0) {
                    Matrix matrix = new Matrix();
                    float scaleWidth = width * 1.0f / bitmap.getWidth();
                    float scaleHeight = height * 1.0f / bitmap.getHeight();
                    matrix.postScale(scaleWidth, scaleHeight);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    return bitmap;
                }
            }
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    public static void drawNinePath(Canvas canvas, Bitmap bmp, Rect rect) {
        NinePatch.isNinePatchChunk(bmp.getNinePatchChunk());
        NinePatch patch = new NinePatch(bmp, bmp.getNinePatchChunk(), null);
        patch.draw(canvas, rect);
    }

    public static void drawBitmap(Canvas canvas, Paint paint, Bitmap bmp, Rect rect) {
        try {
            if (NinePatch.isNinePatchChunk(bmp.getNinePatchChunk())) {
                drawNinePath(canvas, bmp, rect);
                return;
            }
        } catch (Exception e) {
        }
        canvas.drawBitmap(bmp, rect.left, rect.top, paint);
    }

    public static int dp2px(Context context, float dpValue) {
        if (context == null || compareFloat(0f, dpValue) == 0) return 0;
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public static int compareFloat(float a, float b) {
        int ta = Math.round(a * 1000000);
        int tb = Math.round(b * 1000000);
        if (ta > tb) {
            return 1;
        } else if (ta < tb) {
            return -1;
        } else {
            return 0;
        }
    }

    public static int compareFloat(float a, float b, int degree) {
        if (Math.abs(a - b) < Math.pow(0.1, degree)) {
            return 0;
        } else {
            if (a < b) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    public static float parseFloat(String s) {
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    public static Rect measureText(String text, float textSize) {
        Paint paint = new Paint();
        Rect textRect = new Rect();
        paint.setTextSize(textSize);
        paint.getTextBounds(text, 0, text.length(), textRect);
        paint.reset();
        return textRect;
    }

    public static boolean verifyBitmap(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled() || bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0) {
            return false;
        }
        return true;
    }

    public static int getColor(Context context, @ColorRes int colorId) {
        if (context != null) {
            return ContextCompat.getColor(context.getApplicationContext(), colorId);
        }
        return Color.WHITE;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\seekbar\VerticalRangeSeekBar.java =====

package com.mpdc4gsr.libunified.ui.widget.seekbar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;

import androidx.annotation.IntDef;

import com.mpdc4gsr.libunified.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class VerticalRangeSeekBar extends RangeSeekBar {

    public final static int TEXT_DIRECTION_VERTICAL = 1;
    public final static int TEXT_DIRECTION_HORIZONTAL = 2;
    public final static int DIRECTION_LEFT = 1;
    public final static int DIRECTION_RIGHT = 2;
    private int orientation = DIRECTION_LEFT;
    private int tickMarkDirection = TEXT_DIRECTION_VERTICAL;
    private int maxTickMarkWidth;
    private boolean noNegativeNumber = false;

    public VerticalRangeSeekBar(Context context) {
        this(context, null);
    }

    public VerticalRangeSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(attrs);
        initSeekBar(attrs);
    }

    private void initAttrs(AttributeSet attrs) {
        try {
            TypedArray t = getContext().obtainStyledAttributes(attrs, R.styleable.VerticalRangeSeekBar);
            orientation = t.getInt(R.styleable.VerticalRangeSeekBar_rsb_orientation, DIRECTION_LEFT);
            tickMarkDirection = t.getInt(R.styleable.VerticalRangeSeekBar_rsb_tick_mark_orientation, TEXT_DIRECTION_VERTICAL);
            t.recycle();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void initSeekBar(AttributeSet attrs) {
        leftSB = new VerticalSeekBar(this, attrs, true);
        rightSB = new VerticalSeekBar(this, attrs, false);
        rightSB.setVisible(getSeekBarMode() != SEEKBAR_MODE_SINGLE);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(h, w, oldh, oldw);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        /*
         * onMeasurewidthMeasureSpecheightMeasureSpecï¼Œ
         * MeasureSpec.EXACTLY
         * MeasureSpec.AT_MOST
         * MeasureSpec.UNSPECIFIED
         */

        if (widthMode == MeasureSpec.EXACTLY) {
            widthSize = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);
        } else if (widthMode == MeasureSpec.AT_MOST && getParent() instanceof ViewGroup
                && widthSize == ViewGroup.LayoutParams.MATCH_PARENT) {
            widthSize = MeasureSpec.makeMeasureSpec(((ViewGroup) getParent()).getMeasuredHeight(), MeasureSpec.AT_MOST);
        } else {
            int heightNeeded;
            if (getGravity() == Gravity.CENTER) {
                heightNeeded = 2 * getProgressTop() + getProgressHeight();
            } else {
                heightNeeded = (int) getRawHeight();
            }
            widthSize = MeasureSpec.makeMeasureSpec(heightNeeded, MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthSize, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (orientation == DIRECTION_LEFT) {
            canvas.rotate(-90);
            canvas.translate(-getHeight(), 0);
        } else {
            canvas.rotate(90);
            canvas.translate(0, -getWidth());
        }
        super.onDraw(canvas);
    }

    @Override
    protected void onDrawTickMark(Canvas canvas, Paint paint) {
        if (getTickMarkTextArray() != null) {
            int arrayLength = getTickMarkTextArray().length;
            int trickPartWidth = getProgressWidth() / (arrayLength - 1);
            for (int i = 0; i < arrayLength; i++) {
                final String text2Draw = getTickMarkTextArray()[i].toString();
                if (TextUtils.isEmpty(text2Draw)) continue;
                paint.getTextBounds(text2Draw, 0, text2Draw.length(), tickMarkTextRect);
                paint.setColor(getTickMarkTextColor());

                float x;
                if (getTickMarkMode() == TRICK_MARK_MODE_OTHER) {
                    if (getTickMarkGravity() == TICK_MARK_GRAVITY_RIGHT) {
                        x = getProgressLeft() + i * trickPartWidth - tickMarkTextRect.width();
                    } else if (getTickMarkGravity() == TICK_MARK_GRAVITY_CENTER) {
                        x = getProgressLeft() + i * trickPartWidth - tickMarkTextRect.width() / 2f;
                    } else {
                        x = getProgressLeft() + i * trickPartWidth;
                    }
                } else {
                    float num = Utils.parseFloat(text2Draw);
                    SeekBarState[] states = getRangeSeekBarState();
                    if (Utils.compareFloat(num, states[0].value) != -1 && Utils.compareFloat(num, states[1].value) != 1 && (getSeekBarMode() == SEEKBAR_MODE_RANGE)) {
                        paint.setColor(getTickMarkInRangeTextColor());
                    }

                    x = getProgressLeft() + getProgressWidth() * (num - getMinProgress()) / (getMaxProgress() - getMinProgress())
                            - tickMarkTextRect.width() / 2f;
                }
                float y;
                if (getTickMarkLayoutGravity() == Gravity.TOP) {
                    y = getProgressTop() - getTickMarkTextMargin();
                } else {
                    y = getProgressBottom() + getTickMarkTextMargin() + tickMarkTextRect.height();
                }
                int degrees = 0;
                float rotateX = (x + tickMarkTextRect.width() / 2f);
                float rotateY = (y - tickMarkTextRect.height() / 2f);
                if (tickMarkDirection == TEXT_DIRECTION_VERTICAL) {
                    if (orientation == DIRECTION_LEFT) {
                        degrees = 90;
                    } else if (orientation == DIRECTION_RIGHT) {
                        degrees = -90;
                    }
                }
                if (degrees != 0) {
                    canvas.rotate(degrees, rotateX, rotateY);
                }
                canvas.drawText(text2Draw, x, y, paint);
                if (degrees != 0) {
                    canvas.rotate(-degrees, rotateX, rotateY);
                }
            }
        }

    }

    @Override
    protected int getTickMarkRawHeight() {
        if (maxTickMarkWidth > 0) return getTickMarkTextMargin() + maxTickMarkWidth;
        if (getTickMarkTextArray() != null && getTickMarkTextArray().length > 0) {
            int arrayLength = getTickMarkTextArray().length;
            maxTickMarkWidth = Utils.measureText(String.valueOf(getTickMarkTextArray()[0]), getTickMarkTextSize()).width();
            for (int i = 1; i < arrayLength; i++) {
                int width = Utils.measureText(String.valueOf(getTickMarkTextArray()[i]), getTickMarkTextSize()).width();
                if (maxTickMarkWidth < width) {
                    maxTickMarkWidth = width;
                }
            }
            return getTickMarkTextMargin() + maxTickMarkWidth;
        }
        return 0;
    }

    public void setNoNegativeNumber(Boolean noNegativeNumber) {
        this.noNegativeNumber = noNegativeNumber;
        if (leftSB != null) {
            leftSB.setNoNegativeNumber(noNegativeNumber);
        }
        if (rightSB != null) {
            rightSB.setNoNegativeNumber(noNegativeNumber);
        }
    }

    @Override
    public void setTickMarkTextSize(int tickMarkTextSize) {
        super.setTickMarkTextSize(tickMarkTextSize);
        maxTickMarkWidth = 0;
    }

    @Override
    public void setTickMarkTextArray(CharSequence[] tickMarkTextArray) {
        super.setTickMarkTextArray(tickMarkTextArray);
        maxTickMarkWidth = 0;
    }

    @Override
    protected float getEventX(MotionEvent event) {
        if (orientation == DIRECTION_LEFT) {
            return getHeight() - event.getY();
        } else {
            return event.getY();
        }
    }

    @Override
    protected float getEventY(MotionEvent event) {
        if (orientation == DIRECTION_LEFT) {
            return event.getX();
        } else {
            return -event.getX() + getWidth();
        }
    }

    public void drawIndPath(boolean draw) {
        if (leftSB != null && leftSB instanceof VerticalSeekBar) {
            getLeftSeekBar().setDrawIndPathBg(draw);
        }
        if (rightSB != null && rightSB instanceof VerticalSeekBar) {
            getRightSeekBar().setDrawIndPathBg(draw);
        }
    }

    public VerticalSeekBar getLeftSeekBar() {
        return (VerticalSeekBar) leftSB;
    }

    public VerticalSeekBar getRightSeekBar() {
        return (VerticalSeekBar) rightSB;
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(@DirectionDef int orientation) {
        this.orientation = orientation;
    }

    public int getTickMarkDirection() {
        return tickMarkDirection;
    }

    public void setTickMarkDirection(@TextDirectionDef int tickMarkDirection) {
        this.tickMarkDirection = tickMarkDirection;
    }

    @IntDef({TEXT_DIRECTION_VERTICAL, TEXT_DIRECTION_HORIZONTAL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TextDirectionDef {
    }

    @IntDef({DIRECTION_LEFT, DIRECTION_RIGHT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DirectionDef {
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\seekbar\VerticalSeekBar.java =====

package com.mpdc4gsr.libunified.ui.widget.seekbar;

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