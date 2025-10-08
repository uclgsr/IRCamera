// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\ui\listener' directory and its subdirectories.
// Total files: 8 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\listener\BarLineChartTouchListener.java =====

package com.mpdc4gsr.libunified.ui.listener;

import android.annotation.SuppressLint;
import android.graphics.Matrix;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.AnimationUtils;

import com.mpdc4gsr.libunified.ui.charts.BarLineChartBase;
import com.mpdc4gsr.libunified.ui.charts.HorizontalBarChart;
import com.mpdc4gsr.libunified.ui.data.BarLineScatterCandleBubbleData;
import com.mpdc4gsr.libunified.ui.data.Entry;
import com.mpdc4gsr.libunified.ui.highlight.Highlight;
import com.mpdc4gsr.libunified.ui.interfaces.datasets.IBarLineScatterCandleBubbleDataSet;
import com.mpdc4gsr.libunified.ui.interfaces.datasets.IDataSet;
import com.mpdc4gsr.libunified.ui.utils.MPPointF;
import com.mpdc4gsr.libunified.ui.utils.Utils;
import com.mpdc4gsr.libunified.ui.utils.ViewPortHandler;

public class BarLineChartTouchListener extends ChartTouchListener<BarLineChartBase<? extends BarLineScatterCandleBubbleData<?
        extends IBarLineScatterCandleBubbleDataSet<? extends Entry>>>> {

    private Matrix mMatrix = new Matrix();

    private Matrix mSavedMatrix = new Matrix();

    private MPPointF mTouchStartPoint = MPPointF.getInstance(0, 0);

    private MPPointF mTouchPointCenter = MPPointF.getInstance(0, 0);

    private float mSavedXDist = 1f;
    private float mSavedYDist = 1f;
    private float mSavedDist = 1f;

    private IDataSet mClosestDataSetToTouch;

    private VelocityTracker mVelocityTracker;

    private long mDecelerationLastTime = 0;
    private MPPointF mDecelerationCurrentPoint = MPPointF.getInstance(0, 0);
    private MPPointF mDecelerationVelocity = MPPointF.getInstance(0, 0);

    private float mDragTriggerDist;

    private float mMinScalePointerDistance;

    public BarLineChartTouchListener(BarLineChartBase<? extends BarLineScatterCandleBubbleData<? extends
            IBarLineScatterCandleBubbleDataSet<? extends Entry>>> chart, Matrix touchMatrix, float dragTriggerDistance) {
        super(chart);
        this.mMatrix = touchMatrix;

        this.mDragTriggerDist = Utils.convertDpToPixel(dragTriggerDistance);

        this.mMinScalePointerDistance = Utils.convertDpToPixel(3.5f);
    }

    private static void midPoint(MPPointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.x = (x / 2f);
        point.y = (y / 2f);
    }

    private static float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private static float getXDist(MotionEvent e) {
        float x = Math.abs(e.getX(0) - e.getX(1));
        return x;
    }

    private static float getYDist(MotionEvent e) {
        float y = Math.abs(e.getY(0) - e.getY(1));
        return y;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            if (mVelocityTracker != null) {
                mVelocityTracker.recycle();
                mVelocityTracker = null;
            }
        }

        if (mTouchMode == NONE) {
            mGestureDetector.onTouchEvent(event);
        }

        if (!mChart.isDragEnabled() && (!mChart.isScaleXEnabled() && !mChart.isScaleYEnabled()))
            return true;

        switch (event.getAction() & MotionEvent.ACTION_MASK) {

            case MotionEvent.ACTION_DOWN:

                startAction(event);

                stopDeceleration();

                saveTouchStart(event);

                break;

            case MotionEvent.ACTION_POINTER_DOWN:

                if (event.getPointerCount() >= 2) {

                    mChart.disableScroll();

                    saveTouchStart(event);

                    mSavedXDist = getXDist(event);

                    mSavedYDist = getYDist(event);

                    mSavedDist = spacing(event);

                    if (mSavedDist > 10f) {

                        if (mChart.isPinchZoomEnabled()) {
                            mTouchMode = PINCH_ZOOM;
                        } else {
                            if (mChart.isScaleXEnabled() != mChart.isScaleYEnabled()) {
                                mTouchMode = mChart.isScaleXEnabled() ? X_ZOOM : Y_ZOOM;
                            } else {
                                mTouchMode = mSavedXDist > mSavedYDist ? X_ZOOM : Y_ZOOM;
                            }
                        }
                    }

                    midPoint(mTouchPointCenter, event);
                }
                break;

            case MotionEvent.ACTION_MOVE:

                if (mTouchMode == DRAG) {

                    mChart.disableScroll();

                    float x = mChart.isDragXEnabled() ? event.getX() - mTouchStartPoint.x : 0.f;
                    float y = mChart.isDragYEnabled() ? event.getY() - mTouchStartPoint.y : 0.f;

                    performDrag(event, x, y);

                } else if (mTouchMode == X_ZOOM || mTouchMode == Y_ZOOM || mTouchMode == PINCH_ZOOM) {

                    mChart.disableScroll();

                    if (mChart.isScaleXEnabled() || mChart.isScaleYEnabled())
                        performZoom(event);

                } else if (mTouchMode == NONE
                        && Math.abs(distance(event.getX(), mTouchStartPoint.x, event.getY(),
                        mTouchStartPoint.y)) > mDragTriggerDist) {

                    if (mChart.isDragEnabled()) {

                        boolean shouldPan = !mChart.isFullyZoomedOut() ||
                                !mChart.hasNoDragOffset();

                        if (shouldPan) {

                            float distanceX = Math.abs(event.getX() - mTouchStartPoint.x);
                            float distanceY = Math.abs(event.getY() - mTouchStartPoint.y);

                            if ((mChart.isDragXEnabled() || distanceY >= distanceX) &&
                                    (mChart.isDragYEnabled() || distanceY <= distanceX)) {

                                mLastGesture = ChartGesture.DRAG;
                                mTouchMode = DRAG;
                            }

                        } else {

                            if (mChart.isHighlightPerDragEnabled()) {
                                mLastGesture = ChartGesture.DRAG;

                                if (mChart.isHighlightPerDragEnabled())
                                    performHighlightDrag(event);
                            }
                        }

                    }

                }
                break;

            case MotionEvent.ACTION_UP:

                final VelocityTracker velocityTracker = mVelocityTracker;
                final int pointerId = event.getPointerId(0);
                velocityTracker.computeCurrentVelocity(1000, Utils.getMaximumFlingVelocity());
                final float velocityY = velocityTracker.getYVelocity(pointerId);
                final float velocityX = velocityTracker.getXVelocity(pointerId);

                if (Math.abs(velocityX) > Utils.getMinimumFlingVelocity() ||
                        Math.abs(velocityY) > Utils.getMinimumFlingVelocity()) {

                    if (mTouchMode == DRAG && mChart.isDragDecelerationEnabled()) {

                        stopDeceleration();

                        mDecelerationLastTime = AnimationUtils.currentAnimationTimeMillis();

                        mDecelerationCurrentPoint.x = event.getX();
                        mDecelerationCurrentPoint.y = event.getY();

                        mDecelerationVelocity.x = velocityX;
                        mDecelerationVelocity.y = velocityY;

                        Utils.postInvalidateOnAnimation(mChart);

                    }
                }

                if (mTouchMode == X_ZOOM ||
                        mTouchMode == Y_ZOOM ||
                        mTouchMode == PINCH_ZOOM ||
                        mTouchMode == POST_ZOOM) {

                    mChart.calculateOffsets();
                    mChart.postInvalidate();
                }

                mTouchMode = NONE;
                mChart.enableScroll();

                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }

                endAction(event);

                break;
            case MotionEvent.ACTION_POINTER_UP:
                Utils.velocityTrackerPointerUpCleanUpIfNecessary(event, mVelocityTracker);

                mTouchMode = POST_ZOOM;
                break;

            case MotionEvent.ACTION_CANCEL:

                mTouchMode = NONE;
                endAction(event);
                break;
        }

        mMatrix = mChart.getViewPortHandler().refresh(mMatrix, mChart, true);

        return true;
    }

    private void saveTouchStart(MotionEvent event) {

        mSavedMatrix.set(mMatrix);
        mTouchStartPoint.x = event.getX();
        mTouchStartPoint.y = event.getY();

        mClosestDataSetToTouch = mChart.getDataSetByTouchPoint(event.getX(), event.getY());
    }

    private void performDrag(MotionEvent event, float distanceX, float distanceY) {

        mLastGesture = ChartGesture.DRAG;

        mMatrix.set(mSavedMatrix);

        OnChartGestureListener l = mChart.getOnChartGestureListener();

        if (inverted()) {

            if (mChart instanceof HorizontalBarChart) {
                distanceX = -distanceX;
            } else {
                distanceY = -distanceY;
            }
        }

        mMatrix.postTranslate(distanceX, distanceY);

        if (l != null)
            l.onChartTranslate(event, distanceX, distanceY);
    }

    private void performZoom(MotionEvent event) {

        if (event.getPointerCount() >= 2) {

            OnChartGestureListener l = mChart.getOnChartGestureListener();

            float totalDist = spacing(event);

            if (totalDist > mMinScalePointerDistance) {

                MPPointF t = getTrans(mTouchPointCenter.x, mTouchPointCenter.y);
                ViewPortHandler h = mChart.getViewPortHandler();

                if (mTouchMode == PINCH_ZOOM) {

                    mLastGesture = ChartGesture.PINCH_ZOOM;

                    float scale = totalDist / mSavedDist;

                    boolean isZoomingOut = (scale < 1);

                    boolean canZoomMoreX = isZoomingOut ?
                            h.canZoomOutMoreX() :
                            h.canZoomInMoreX();

                    boolean canZoomMoreY = isZoomingOut ?
                            h.canZoomOutMoreY() :
                            h.canZoomInMoreY();

                    float scaleX = (mChart.isScaleXEnabled()) ? scale : 1f;
                    float scaleY = (mChart.isScaleYEnabled()) ? scale : 1f;

                    if (canZoomMoreY || canZoomMoreX) {

                        mMatrix.set(mSavedMatrix);
                        mMatrix.postScale(scaleX, scaleY, t.x, t.y);

                        if (l != null)
                            l.onChartScale(event, scaleX, scaleY);
                    }

                } else if (mTouchMode == X_ZOOM && mChart.isScaleXEnabled()) {

                    mLastGesture = ChartGesture.X_ZOOM;

                    float xDist = getXDist(event);
                    float scaleX = xDist / mSavedXDist;

                    boolean isZoomingOut = (scaleX < 1);
                    boolean canZoomMoreX = isZoomingOut ?
                            h.canZoomOutMoreX() :
                            h.canZoomInMoreX();

                    if (canZoomMoreX) {

                        mMatrix.set(mSavedMatrix);
                        mMatrix.postScale(scaleX, 1f, t.x, t.y);

                        if (l != null)
                            l.onChartScale(event, scaleX, 1f);
                    }

                } else if (mTouchMode == Y_ZOOM && mChart.isScaleYEnabled()) {

                    mLastGesture = ChartGesture.Y_ZOOM;

                    float yDist = getYDist(event);
                    float scaleY = yDist / mSavedYDist;

                    boolean isZoomingOut = (scaleY < 1);
                    boolean canZoomMoreY = isZoomingOut ?
                            h.canZoomOutMoreY() :
                            h.canZoomInMoreY();

                    if (canZoomMoreY) {

                        mMatrix.set(mSavedMatrix);
                        mMatrix.postScale(1f, scaleY, t.x, t.y);

                        if (l != null)
                            l.onChartScale(event, 1f, scaleY);
                    }
                }

                MPPointF.recycleInstance(t);
            }
        }
    }

    private void performHighlightDrag(MotionEvent e) {

        Highlight h = mChart.getHighlightByTouchPoint(e.getX(), e.getY());

        if (h != null && !h.equalTo(mLastHighlighted)) {
            mLastHighlighted = h;
            mChart.highlightValue(h, true);
        }
    }

    public MPPointF getTrans(float x, float y) {

        ViewPortHandler vph = mChart.getViewPortHandler();

        float xTrans = x - vph.offsetLeft();
        float yTrans = 0f;

        if (inverted()) {
            yTrans = -(y - vph.offsetTop());
        } else {
            yTrans = -(mChart.getMeasuredHeight() - y - vph.offsetBottom());
        }

        return MPPointF.getInstance(xTrans, yTrans);
    }

    private boolean inverted() {
        return (mClosestDataSetToTouch == null && mChart.isAnyAxisInverted()) || (mClosestDataSetToTouch != null
                && mChart.isInverted(mClosestDataSetToTouch.getAxisDependency()));
    }

    public Matrix getMatrix() {
        return mMatrix;
    }

    public void setDragTriggerDist(float dragTriggerDistance) {
        this.mDragTriggerDist = Utils.convertDpToPixel(dragTriggerDistance);
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {

        mLastGesture = ChartGesture.DOUBLE_TAP;

        OnChartGestureListener l = mChart.getOnChartGestureListener();

        if (l != null) {
            l.onChartDoubleTapped(e);
        }

        if (mChart.isDoubleTapToZoomEnabled() && mChart.getData().getEntryCount() > 0) {

            MPPointF trans = getTrans(e.getX(), e.getY());

            mChart.zoom(mChart.isScaleXEnabled() ? 1.4f : 1f, mChart.isScaleYEnabled() ? 1.4f : 1f, trans.x, trans.y);

            if (mChart.isLogEnabled())
                Log.i("BarlineChartTouch", "Double-Tap, Zooming In, x: " + trans.x + ", y: "
                        + trans.y);

            MPPointF.recycleInstance(trans);
        }

        return super.onDoubleTap(e);
    }

    @Override
    public void onLongPress(MotionEvent e) {

        mLastGesture = ChartGesture.LONG_PRESS;

        OnChartGestureListener l = mChart.getOnChartGestureListener();

        if (l != null) {

            l.onChartLongPressed(e);
        }
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {

        mLastGesture = ChartGesture.SINGLE_TAP;

        OnChartGestureListener l = mChart.getOnChartGestureListener();

        if (l != null) {
            l.onChartSingleTapped(e);
        }

        if (!mChart.isHighlightPerTapEnabled()) {
            return false;
        }

        Highlight h = mChart.getHighlightByTouchPoint(e.getX(), e.getY());
        performHighlight(h, e);

        return super.onSingleTapUp(e);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

        mLastGesture = ChartGesture.FLING;

        OnChartGestureListener l = mChart.getOnChartGestureListener();

        if (l != null) {
            l.onChartFling(e1, e2, velocityX, velocityY);
        }

        return super.onFling(e1, e2, velocityX, velocityY);
    }

    public void stopDeceleration() {
        mDecelerationVelocity.x = 0;
        mDecelerationVelocity.y = 0;
    }

    public void computeScroll() {

        if (mDecelerationVelocity.x == 0.f && mDecelerationVelocity.y == 0.f)
            return;

        final long currentTime = AnimationUtils.currentAnimationTimeMillis();

        mDecelerationVelocity.x *= mChart.getDragDecelerationFrictionCoef();
        mDecelerationVelocity.y *= mChart.getDragDecelerationFrictionCoef();

        final float timeInterval = (float) (currentTime - mDecelerationLastTime) / 1000.f;

        float distanceX = mDecelerationVelocity.x * timeInterval;
        float distanceY = mDecelerationVelocity.y * timeInterval;

        mDecelerationCurrentPoint.x += distanceX;
        mDecelerationCurrentPoint.y += distanceY;

        MotionEvent event = MotionEvent.obtain(currentTime, currentTime, MotionEvent.ACTION_MOVE, mDecelerationCurrentPoint.x,
                mDecelerationCurrentPoint.y, 0);

        float dragDistanceX = mChart.isDragXEnabled() ? mDecelerationCurrentPoint.x - mTouchStartPoint.x : 0.f;
        float dragDistanceY = mChart.isDragYEnabled() ? mDecelerationCurrentPoint.y - mTouchStartPoint.y : 0.f;

        performDrag(event, dragDistanceX, dragDistanceY);

        event.recycle();
        mMatrix = mChart.getViewPortHandler().refresh(mMatrix, mChart, false);

        mDecelerationLastTime = currentTime;

        if (Math.abs(mDecelerationVelocity.x) >= 0.01 || Math.abs(mDecelerationVelocity.y) >= 0.01)
            Utils.postInvalidateOnAnimation(mChart);
        else {

            mChart.calculateOffsets();
            mChart.postInvalidate();

            stopDeceleration();
        }
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\listener\ChartTouchListener.java =====

package com.mpdc4gsr.libunified.ui.listener;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.mpdc4gsr.libunified.ui.charts.Chart;
import com.mpdc4gsr.libunified.ui.highlight.Highlight;

public abstract class ChartTouchListener<T extends Chart<?>> extends GestureDetector.SimpleOnGestureListener implements View.OnTouchListener {

    protected static final int NONE = 0;
    protected static final int DRAG = 1;
    protected static final int X_ZOOM = 2;
    protected static final int Y_ZOOM = 3;
    protected static final int PINCH_ZOOM = 4;
    protected static final int POST_ZOOM = 5;
    protected static final int ROTATE = 6;

    protected ChartGesture mLastGesture = ChartGesture.NONE;
    protected int mTouchMode = NONE;
    protected Highlight mLastHighlighted;
    protected GestureDetector mGestureDetector;
    protected T mChart;

    public ChartTouchListener(T chart) {
        this.mChart = chart;

        mGestureDetector = new GestureDetector(chart.getContext(), this);
    }

    protected static float distance(float eventX, float startX, float eventY, float startY) {
        float dx = eventX - startX;
        float dy = eventY - startY;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public void startAction(MotionEvent me) {

        OnChartGestureListener l = mChart.getOnChartGestureListener();

        if (l != null)
            l.onChartGestureStart(me, mLastGesture);
    }

    public void endAction(MotionEvent me) {

        OnChartGestureListener l = mChart.getOnChartGestureListener();

        if (l != null)
            l.onChartGestureEnd(me, mLastGesture);
    }

    public void setLastHighlighted(Highlight high) {
        mLastHighlighted = high;
    }

    public int getTouchMode() {
        return mTouchMode;
    }

    public ChartGesture getLastGesture() {
        return mLastGesture;
    }

    protected void performHighlight(Highlight h, MotionEvent e) {

        if (h == null || h.equalTo(mLastHighlighted)) {
            mChart.highlightValue(null, true);
            mLastHighlighted = null;
        } else {
            mChart.highlightValue(h, true);
            mLastHighlighted = h;
        }
    }

    public enum ChartGesture {
        NONE, DRAG, X_ZOOM, Y_ZOOM, PINCH_ZOOM, ROTATE, SINGLE_TAP, DOUBLE_TAP, LONG_PRESS, FLING
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\listener\OnChartGestureListener.java =====

package com.mpdc4gsr.libunified.ui.listener;

import android.view.MotionEvent;

public interface OnChartGestureListener {

    void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture);

    void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture);

    void onChartLongPressed(MotionEvent me);

    void onChartDoubleTapped(MotionEvent me);

    void onChartSingleTapped(MotionEvent me);

    void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY);

    void onChartScale(MotionEvent me, float scaleX, float scaleY);

    void onChartTranslate(MotionEvent me, float dX, float dY);
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\listener\OnChartValueSelectedListener.java =====

package com.mpdc4gsr.libunified.ui.listener;

import com.mpdc4gsr.libunified.ui.data.Entry;
import com.mpdc4gsr.libunified.ui.highlight.Highlight;

public interface OnChartValueSelectedListener {

    void onValueSelected(Entry e, Highlight h);

    void onNothingSelected();
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\listener\OnDrawLineChartTouchListener.java =====

package com.mpdc4gsr.libunified.ui.listener;

import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class OnDrawLineChartTouchListener extends SimpleOnGestureListener implements OnTouchListener {

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\listener\OnDrawListener.java =====

package com.mpdc4gsr.libunified.ui.listener;

import com.mpdc4gsr.libunified.ui.data.DataSet;
import com.mpdc4gsr.libunified.ui.data.Entry;

public interface OnDrawListener {

    void onEntryAdded(Entry entry);

    void onEntryMoved(Entry entry);

    void onDrawFinished(DataSet<?> dataSet);

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\listener\PieRadarChartTouchListener.java =====

package com.mpdc4gsr.libunified.ui.listener;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;

import com.mpdc4gsr.libunified.ui.charts.PieRadarChartBase;
import com.mpdc4gsr.libunified.ui.highlight.Highlight;
import com.mpdc4gsr.libunified.ui.utils.MPPointF;
import com.mpdc4gsr.libunified.ui.utils.Utils;

import java.util.ArrayList;

public class PieRadarChartTouchListener extends ChartTouchListener<PieRadarChartBase<?>> {

    private MPPointF mTouchStartPoint = MPPointF.getInstance(0, 0);

    private float mStartAngle = 0f;

    private ArrayList<AngularVelocitySample> _velocitySamples = new ArrayList<AngularVelocitySample>();

    private long mDecelerationLastTime = 0;
    private float mDecelerationAngularVelocity = 0.f;

    public PieRadarChartTouchListener(PieRadarChartBase<?> chart) {
        super(chart);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {

        if (mGestureDetector.onTouchEvent(event))
            return true;

        if (mChart.isRotationEnabled()) {

            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:

                    startAction(event);

                    stopDeceleration();

                    resetVelocity();

                    if (mChart.isDragDecelerationEnabled())
                        sampleVelocity(x, y);

                    setGestureStartAngle(x, y);
                    mTouchStartPoint.x = x;
                    mTouchStartPoint.y = y;

                    break;
                case MotionEvent.ACTION_MOVE:

                    if (mChart.isDragDecelerationEnabled())
                        sampleVelocity(x, y);

                    if (mTouchMode == NONE
                            && distance(x, mTouchStartPoint.x, y, mTouchStartPoint.y)
                            > Utils.convertDpToPixel(8f)) {
                        mLastGesture = ChartGesture.ROTATE;
                        mTouchMode = ROTATE;
                        mChart.disableScroll();
                    } else if (mTouchMode == ROTATE) {
                        updateGestureRotation(x, y);
                        mChart.invalidate();
                    }

                    endAction(event);

                    break;
                case MotionEvent.ACTION_UP:

                    if (mChart.isDragDecelerationEnabled()) {

                        stopDeceleration();

                        sampleVelocity(x, y);

                        mDecelerationAngularVelocity = calculateVelocity();

                        if (mDecelerationAngularVelocity != 0.f) {
                            mDecelerationLastTime = AnimationUtils.currentAnimationTimeMillis();

                            Utils.postInvalidateOnAnimation(mChart);
                        }
                    }

                    mChart.enableScroll();
                    mTouchMode = NONE;

                    endAction(event);

                    break;
            }
        }

        return true;
    }

    @Override
    public void onLongPress(MotionEvent me) {

        mLastGesture = ChartGesture.LONG_PRESS;

        OnChartGestureListener l = mChart.getOnChartGestureListener();

        if (l != null) {
            l.onChartLongPressed(me);
        }
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return true;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {

        mLastGesture = ChartGesture.SINGLE_TAP;

        OnChartGestureListener l = mChart.getOnChartGestureListener();

        if (l != null) {
            l.onChartSingleTapped(e);
        }

        if (!mChart.isHighlightPerTapEnabled()) {
            return false;
        }

        Highlight high = mChart.getHighlightByTouchPoint(e.getX(), e.getY());
        performHighlight(high, e);

        return true;
    }

    private void resetVelocity() {
        _velocitySamples.clear();
    }

    private void sampleVelocity(float touchLocationX, float touchLocationY) {

        long currentTime = AnimationUtils.currentAnimationTimeMillis();

        _velocitySamples.add(new AngularVelocitySample(currentTime, mChart.getAngleForPoint(touchLocationX, touchLocationY)));

        for (int i = 0, count = _velocitySamples.size(); i < count - 2; i++) {
            if (currentTime - _velocitySamples.get(i).time > 1000) {
                _velocitySamples.remove(0);
                i--;
                count--;
            } else {
                break;
            }
        }
    }

    private float calculateVelocity() {

        if (_velocitySamples.isEmpty())
            return 0.f;

        AngularVelocitySample firstSample = _velocitySamples.get(0);
        AngularVelocitySample lastSample = _velocitySamples.get(_velocitySamples.size() - 1);

        AngularVelocitySample beforeLastSample = firstSample;
        for (int i = _velocitySamples.size() - 1; i >= 0; i--) {
            beforeLastSample = _velocitySamples.get(i);
            if (beforeLastSample.angle != lastSample.angle) {
                break;
            }
        }

        float timeDelta = (lastSample.time - firstSample.time) / 1000.f;
        if (timeDelta == 0.f) {
            timeDelta = 0.1f;
        }

        boolean clockwise = lastSample.angle >= beforeLastSample.angle;
        if (Math.abs(lastSample.angle - beforeLastSample.angle) > 270.0) {
            clockwise = !clockwise;
        }

        if (lastSample.angle - firstSample.angle > 180.0) {
            firstSample.angle += 360.0;
        } else if (firstSample.angle - lastSample.angle > 180.0) {
            lastSample.angle += 360.0;
        }

        float velocity = Math.abs((lastSample.angle - firstSample.angle) / timeDelta);

        if (!clockwise) {
            velocity = -velocity;
        }

        return velocity;
    }

    public void setGestureStartAngle(float x, float y) {
        mStartAngle = mChart.getAngleForPoint(x, y) - mChart.getRawRotationAngle();
    }

    public void updateGestureRotation(float x, float y) {
        mChart.setRotationAngle(mChart.getAngleForPoint(x, y) - mStartAngle);
    }

    public void stopDeceleration() {
        mDecelerationAngularVelocity = 0.f;
    }

    public void computeScroll() {

        if (mDecelerationAngularVelocity == 0.f)
            return;

        final long currentTime = AnimationUtils.currentAnimationTimeMillis();

        mDecelerationAngularVelocity *= mChart.getDragDecelerationFrictionCoef();

        final float timeInterval = (float) (currentTime - mDecelerationLastTime) / 1000.f;

        mChart.setRotationAngle(mChart.getRotationAngle() + mDecelerationAngularVelocity * timeInterval);

        mDecelerationLastTime = currentTime;

        if (Math.abs(mDecelerationAngularVelocity) >= 0.001)
            Utils.postInvalidateOnAnimation(mChart);
        else
            stopDeceleration();
    }

    private class AngularVelocitySample {

        public long time;
        public float angle;

        public AngularVelocitySample(long time, float angle) {
            this.time = time;
            this.angle = angle;
        }
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\listener\SingleClickListener.kt =====

package com.mpdc4gsr.libunified.ui.listener

import android.view.View

abstract class SingleClickListener : View.OnClickListener {
    private var lastClickTime: Long = 0
    private val minInterval: Long = 500 // Minimum interval between clicks in milliseconds
    override fun onClick(v: View?) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime >= minInterval) {
            lastClickTime = currentTime
            onSingleClick()
        }
    }

    abstract fun onSingleClick()
}