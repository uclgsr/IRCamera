package com.mpdc4gsr.component.shared.ui.jobs;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.view.View;

import com.mpdc4gsr.component.shared.ui.utils.ObjectPool;
import com.mpdc4gsr.component.shared.ui.utils.Transformer;
import com.mpdc4gsr.component.shared.ui.utils.ViewPortHandler;

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


