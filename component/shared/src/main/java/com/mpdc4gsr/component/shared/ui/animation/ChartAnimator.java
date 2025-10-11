package com.mpdc4gsr.component.shared.ui.animation;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.view.View;

import androidx.annotation.RequiresApi;

import com.mpdc4gsr.component.shared.ui.animation.Easing.EasingFunction;

public class ChartAnimator {

    @SuppressWarnings("WeakerAccess")
    protected float mPhaseY = 1f;

    @SuppressWarnings("WeakerAccess")
    protected float mPhaseX = 1f;

    private AnimatorUpdateListener mListener;
    private View mView;

    public ChartAnimator() {
    }

    @RequiresApi(11)
    public ChartAnimator(AnimatorUpdateListener listener) {
        mListener = listener;
    }

    public void setView(View view) {
        mView = view;
    }

    @RequiresApi(11)
    private ObjectAnimator xAnimator(int duration, EasingFunction easing) {

        ObjectAnimator animatorX = ObjectAnimator.ofFloat(this, "phaseX", 0f, 1f);
        animatorX.setInterpolator(easing);
        animatorX.setDuration(duration);

        return animatorX;
    }

    @RequiresApi(11)
    private ObjectAnimator yAnimator(int duration, EasingFunction easing) {

        ObjectAnimator animatorY = ObjectAnimator.ofFloat(this, "phaseY", 0f, 1f);
        animatorY.setInterpolator(easing);
        animatorY.setDuration(duration);

        return animatorY;
    }

    @RequiresApi(11)
    public void animateX(int durationMillis) {
        animateX(durationMillis, Easing.Linear);
    }

    @RequiresApi(11)
    public void animateX(int durationMillis, EasingFunction easing) {
        if (mView != null && !mView.isAttachedToWindow()) {
            return;
        }
        ObjectAnimator animatorX = xAnimator(durationMillis, easing);
        animatorX.addUpdateListener(mListener);
        animatorX.start();
    }

    @RequiresApi(11)
    public void animateXY(int durationMillisX, int durationMillisY) {
        animateXY(durationMillisX, durationMillisY, Easing.Linear, Easing.Linear);
    }

    @RequiresApi(11)
    public void animateXY(int durationMillisX, int durationMillisY, EasingFunction easing) {
        if (mView != null && !mView.isAttachedToWindow()) {
            return;
        }
        ObjectAnimator xAnimator = xAnimator(durationMillisX, easing);
        ObjectAnimator yAnimator = yAnimator(durationMillisY, easing);

        if (durationMillisX > durationMillisY) {
            xAnimator.addUpdateListener(mListener);
        } else {
            yAnimator.addUpdateListener(mListener);
        }

        xAnimator.start();
        yAnimator.start();
    }

    @RequiresApi(11)
    public void animateXY(int durationMillisX, int durationMillisY, EasingFunction easingX,
                          EasingFunction easingY) {
        if (mView != null && !mView.isAttachedToWindow()) {
            return;
        }
        ObjectAnimator xAnimator = xAnimator(durationMillisX, easingX);
        ObjectAnimator yAnimator = yAnimator(durationMillisY, easingY);

        if (durationMillisX > durationMillisY) {
            xAnimator.addUpdateListener(mListener);
        } else {
            yAnimator.addUpdateListener(mListener);
        }

        xAnimator.start();
        yAnimator.start();
    }

    @RequiresApi(11)
    public void animateY(int durationMillis) {
        animateY(durationMillis, Easing.Linear);
    }

    @RequiresApi(11)
    public void animateY(int durationMillis, EasingFunction easing) {
        if (mView != null && !mView.isAttachedToWindow()) {
            return;
        }
        ObjectAnimator animatorY = yAnimator(durationMillis, easing);
        animatorY.addUpdateListener(mListener);
        animatorY.start();
    }

    public float getPhaseY() {
        return mPhaseY;
    }

    public void setPhaseY(float phase) {
        if (phase > 1f) {
            phase = 1f;
        } else if (phase < 0f) {
            phase = 0f;
        }
        mPhaseY = phase;
    }

    public float getPhaseX() {
        return mPhaseX;
    }

    public void setPhaseX(float phase) {
        if (phase > 1f) {
            phase = 1f;
        } else if (phase < 0f) {
            phase = 0f;
        }
        mPhaseX = phase;
    }
}


