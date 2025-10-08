// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\ui\jobs' directory and its subdirectories.
// Total files: 6 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\jobs\AnimatedMoveViewJob.java =====

package com.mpdc4gsr.libunified.ui.jobs;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.view.View;

import com.mpdc4gsr.libunified.ui.utils.ObjectPool;
import com.mpdc4gsr.libunified.ui.utils.Transformer;
import com.mpdc4gsr.libunified.ui.utils.ViewPortHandler;

@SuppressLint("NewApi")
public class AnimatedMoveViewJob extends AnimatedViewPortJob {

    private static ObjectPool<AnimatedMoveViewJob> pool;

    static {
        pool = ObjectPool.create(4, new AnimatedMoveViewJob(null, 0, 0, null, null, 0, 0, 0));
        pool.setReplenishPercentage(0.5f);
    }

    public AnimatedMoveViewJob(ViewPortHandler viewPortHandler, float xValue, float yValue, Transformer trans, View v, float xOrigin, float yOrigin, long duration) {
        super(viewPortHandler, xValue, yValue, trans, v, xOrigin, yOrigin, duration);
    }

    public static AnimatedMoveViewJob getInstance(ViewPortHandler viewPortHandler, float xValue, float yValue, Transformer trans, View v, float xOrigin, float yOrigin, long duration) {
        AnimatedMoveViewJob result = pool.get();
        result.mViewPortHandler = viewPortHandler;
        result.xValue = xValue;
        result.yValue = yValue;
        result.mTrans = trans;
        result.view = v;
        result.xOrigin = xOrigin;
        result.yOrigin = yOrigin;

        result.animator.setDuration(duration);
        return result;
    }

    public static void recycleInstance(AnimatedMoveViewJob instance) {
        pool.recycle(instance);
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        if (view == null || !view.isAttachedToWindow()) {
            return;
        }

        try {
            pts[0] = xOrigin + (xValue - xOrigin) * phase;
            pts[1] = yOrigin + (yValue - yOrigin) * phase;

            mTrans.pointValuesToPixel(pts);
            mViewPortHandler.centerViewPort(pts, view);
        } catch (IllegalStateException e) {
            // View may have been detached during update
        }
    }

    public void recycleSelf() {
        recycleInstance(this);
    }

    @Override
    protected ObjectPool.Poolable instantiate() {
        return new AnimatedMoveViewJob(null, 0, 0, null, null, 0, 0, 0);
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\jobs\AnimatedViewPortJob.java =====

package com.mpdc4gsr.libunified.ui.jobs;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.view.View;

import com.mpdc4gsr.libunified.ui.utils.Transformer;
import com.mpdc4gsr.libunified.ui.utils.ViewPortHandler;

@SuppressLint("NewApi")
public abstract class AnimatedViewPortJob extends ViewPortJob implements ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {

    protected ObjectAnimator animator;

    protected float phase;

    protected float xOrigin;
    protected float yOrigin;

    public AnimatedViewPortJob(ViewPortHandler viewPortHandler, float xValue, float yValue, Transformer trans, View v, float xOrigin, float yOrigin, long duration) {
        super(viewPortHandler, xValue, yValue, trans, v);
        this.xOrigin = xOrigin;
        this.yOrigin = yOrigin;
        animator = ObjectAnimator.ofFloat(this, "phase", 0f, 1f);
        animator.setDuration(duration);
        animator.addUpdateListener(this);
        animator.addListener(this);
    }

    @SuppressLint("NewApi")
    @Override
    public void run() {
        if (view != null && view.isAttachedToWindow()) {
            try {
                animator.start();
            } catch (IllegalStateException e) {
                // View may have been detached between check and start
            }
        }
    }

    public float getPhase() {
        return phase;
    }

    public void setPhase(float phase) {
        this.phase = phase;
    }

    public float getXOrigin() {
        return xOrigin;
    }

    public float getYOrigin() {
        return yOrigin;
    }

    public abstract void recycleSelf();

    protected void resetAnimator() {
        animator.removeAllListeners();
        animator.removeAllUpdateListeners();
        animator.reverse();
        animator.addUpdateListener(this);
        animator.addListener(this);
    }

    @Override
    public void onAnimationStart(Animator animation) {

    }

    @Override
    public void onAnimationEnd(Animator animation) {
        try {
            recycleSelf();
        } catch (IllegalArgumentException e) {

        } catch (IllegalStateException e) {

        }
    }

    @Override
    public void onAnimationCancel(Animator animation) {
        try {
            recycleSelf();
        } catch (IllegalArgumentException e) {

        } catch (IllegalStateException e) {

        }
    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {

    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\jobs\AnimatedZoomJob.java =====

package com.mpdc4gsr.libunified.ui.jobs;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.graphics.Matrix;
import android.view.View;

import com.mpdc4gsr.libunified.ui.charts.BarLineChartBase;
import com.mpdc4gsr.libunified.ui.components.YAxis;
import com.mpdc4gsr.libunified.ui.utils.ObjectPool;
import com.mpdc4gsr.libunified.ui.utils.Transformer;
import com.mpdc4gsr.libunified.ui.utils.ViewPortHandler;

@SuppressLint("NewApi")
public class AnimatedZoomJob extends AnimatedViewPortJob implements Animator.AnimatorListener {

    private static ObjectPool<AnimatedZoomJob> pool;

    static {
        pool = ObjectPool.create(8, new AnimatedZoomJob(null, null, null, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
    }

    protected float zoomOriginX;
    protected float zoomOriginY;
    protected float zoomCenterX;
    protected float zoomCenterY;
    protected YAxis yAxis;
    protected float xAxisRange;
    protected Matrix mOnAnimationUpdateMatrixBuffer = new Matrix();

    @SuppressLint("NewApi")
    public AnimatedZoomJob(ViewPortHandler viewPortHandler, View v, Transformer trans, YAxis axis, float xAxisRange, float scaleX, float scaleY, float xOrigin, float yOrigin, float zoomCenterX, float zoomCenterY, float zoomOriginX, float zoomOriginY, long duration) {
        super(viewPortHandler, scaleX, scaleY, trans, v, xOrigin, yOrigin, duration);

        this.zoomCenterX = zoomCenterX;
        this.zoomCenterY = zoomCenterY;
        this.zoomOriginX = zoomOriginX;
        this.zoomOriginY = zoomOriginY;
        this.animator.addListener(this);
        this.yAxis = axis;
        this.xAxisRange = xAxisRange;
    }

    public static AnimatedZoomJob getInstance(ViewPortHandler viewPortHandler, View v, Transformer trans, YAxis axis, float xAxisRange, float scaleX, float scaleY, float xOrigin, float yOrigin, float zoomCenterX, float zoomCenterY, float zoomOriginX, float zoomOriginY, long duration) {
        AnimatedZoomJob result = pool.get();
        result.mViewPortHandler = viewPortHandler;
        result.xValue = scaleX;
        result.yValue = scaleY;
        result.mTrans = trans;
        result.view = v;
        result.xOrigin = xOrigin;
        result.yOrigin = yOrigin;
        result.yAxis = axis;
        result.xAxisRange = xAxisRange;
        result.resetAnimator();
        result.animator.setDuration(duration);
        return result;
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        if (view == null || !view.isAttachedToWindow()) {
            return;
        }

        try {
            float scaleX = xOrigin + (xValue - xOrigin) * phase;
            float scaleY = yOrigin + (yValue - yOrigin) * phase;

            Matrix save = mOnAnimationUpdateMatrixBuffer;
            mViewPortHandler.setZoom(scaleX, scaleY, save);
            mViewPortHandler.refresh(save, view, false);

            float valsInView = yAxis.mAxisRange / mViewPortHandler.getScaleY();
            float xsInView = xAxisRange / mViewPortHandler.getScaleX();

            pts[0] = zoomOriginX + ((zoomCenterX - xsInView / 2f) - zoomOriginX) * phase;
            pts[1] = zoomOriginY + ((zoomCenterY + valsInView / 2f) - zoomOriginY) * phase;

            mTrans.pointValuesToPixel(pts);

            mViewPortHandler.translate(pts, save);
            mViewPortHandler.refresh(save, view, true);
        } catch (IllegalStateException e) {
            // View may have been detached during update
        }
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        if (view != null && view.isAttachedToWindow()) {
            try {
                ((BarLineChartBase) view).calculateOffsets();
                view.postInvalidate();
            } catch (IllegalStateException e) {
                // View may have been detached during animation end
            }
        }
    }

    @Override
    public void onAnimationCancel(Animator animation) {

    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }

    @Override
    public void recycleSelf() {

    }

    @Override
    public void onAnimationStart(Animator animation) {

    }

    @Override
    protected ObjectPool.Poolable instantiate() {
        return new AnimatedZoomJob(null, null, null, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\jobs\MoveViewJob.java =====

package com.mpdc4gsr.libunified.ui.jobs;

import android.view.View;

import com.mpdc4gsr.libunified.ui.utils.ObjectPool;
import com.mpdc4gsr.libunified.ui.utils.Transformer;
import com.mpdc4gsr.libunified.ui.utils.ViewPortHandler;

public class MoveViewJob extends ViewPortJob {

    private static ObjectPool<MoveViewJob> pool;

    static {
        pool = ObjectPool.create(2, new MoveViewJob(null, 0, 0, null, null));
        pool.setReplenishPercentage(0.5f);
    }

    public MoveViewJob(ViewPortHandler viewPortHandler, float xValue, float yValue, Transformer trans, View v) {
        super(viewPortHandler, xValue, yValue, trans, v);
    }

    public static MoveViewJob getInstance(ViewPortHandler viewPortHandler, float xValue, float yValue, Transformer trans, View v) {
        MoveViewJob result = pool.get();
        result.mViewPortHandler = viewPortHandler;
        result.xValue = xValue;
        result.yValue = yValue;
        result.mTrans = trans;
        result.view = v;
        return result;
    }

    public static void recycleInstance(MoveViewJob instance) {
        pool.recycle(instance);
    }

    @Override
    public void run() {

        pts[0] = xValue;
        pts[1] = yValue;

        mTrans.pointValuesToPixel(pts);
        mViewPortHandler.centerViewPort(pts, view);

        this.recycleInstance(this);
    }

    @Override
    protected ObjectPool.Poolable instantiate() {
        return new MoveViewJob(mViewPortHandler, xValue, yValue, mTrans, view);
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\jobs\ViewPortJob.java =====

package com.mpdc4gsr.libunified.ui.jobs;

import android.view.View;

import com.mpdc4gsr.libunified.ui.utils.ObjectPool;
import com.mpdc4gsr.libunified.ui.utils.Transformer;
import com.mpdc4gsr.libunified.ui.utils.ViewPortHandler;

public abstract class ViewPortJob extends ObjectPool.Poolable implements Runnable {

    protected float[] pts = new float[2];

    protected ViewPortHandler mViewPortHandler;
    protected float xValue = 0f;
    protected float yValue = 0f;
    protected Transformer mTrans;
    protected View view;

    public ViewPortJob(ViewPortHandler viewPortHandler, float xValue, float yValue,
                       Transformer trans, View v) {

        this.mViewPortHandler = viewPortHandler;
        this.xValue = xValue;
        this.yValue = yValue;
        this.mTrans = trans;
        this.view = v;

    }

    public float getXValue() {
        return xValue;
    }

    public float getYValue() {
        return yValue;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\jobs\ZoomJob.java =====

package com.mpdc4gsr.libunified.ui.jobs;

import android.graphics.Matrix;
import android.view.View;

import com.mpdc4gsr.libunified.ui.charts.BarLineChartBase;
import com.mpdc4gsr.libunified.ui.components.YAxis;
import com.mpdc4gsr.libunified.ui.utils.ObjectPool;
import com.mpdc4gsr.libunified.ui.utils.Transformer;
import com.mpdc4gsr.libunified.ui.utils.ViewPortHandler;

public class ZoomJob extends ViewPortJob {

    private static ObjectPool<ZoomJob> pool;

    static {
        pool = ObjectPool.create(1, new ZoomJob(null, 0, 0, 0, 0, null, null, null));
        pool.setReplenishPercentage(0.5f);
    }

    protected float scaleX;
    protected float scaleY;
    protected YAxis.AxisDependency axisDependency;
    protected Matrix mRunMatrixBuffer = new Matrix();

    public ZoomJob(ViewPortHandler viewPortHandler, float scaleX, float scaleY, float xValue, float yValue, Transformer trans,
                   YAxis.AxisDependency axis, View v) {
        super(viewPortHandler, xValue, yValue, trans, v);

        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.axisDependency = axis;
    }

    public static ZoomJob getInstance(ViewPortHandler viewPortHandler, float scaleX, float scaleY, float xValue, float yValue,
                                      Transformer trans, YAxis.AxisDependency axis, View v) {
        ZoomJob result = pool.get();
        result.xValue = xValue;
        result.yValue = yValue;
        result.scaleX = scaleX;
        result.scaleY = scaleY;
        result.mViewPortHandler = viewPortHandler;
        result.mTrans = trans;
        result.axisDependency = axis;
        result.view = v;
        return result;
    }

    public static void recycleInstance(ZoomJob instance) {
        pool.recycle(instance);
    }

    @Override
    public void run() {

        Matrix save = mRunMatrixBuffer;
        mViewPortHandler.zoom(scaleX, scaleY, save);
        mViewPortHandler.refresh(save, view, false);

        float yValsInView = ((BarLineChartBase) view).getAxis(axisDependency).mAxisRange / mViewPortHandler.getScaleY();
        float xValsInView = ((BarLineChartBase) view).getXAxis().mAxisRange / mViewPortHandler.getScaleX();

        pts[0] = xValue - xValsInView / 2f;
        pts[1] = yValue + yValsInView / 2f;

        mTrans.pointValuesToPixel(pts);

        mViewPortHandler.translate(pts, save);
        mViewPortHandler.refresh(save, view, false);

        ((BarLineChartBase) view).calculateOffsets();
        view.postInvalidate();

        recycleInstance(this);
    }

    @Override
    protected ObjectPool.Poolable instantiate() {
        return new ZoomJob(null, 0, 0, 0, 0, null, null, null);
    }
}