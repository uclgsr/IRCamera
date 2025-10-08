// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\ui\animation' directory and its subdirectories.
// Total files: 2 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\animation\ChartAnimator.java =====

package com.mpdc4gsr.libunified.ui.animation;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.view.View;

import androidx.annotation.RequiresApi;

import com.mpdc4gsr.libunified.ui.animation.Easing.EasingFunction;

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


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\animation\Easing.java =====

package com.mpdc4gsr.libunified.ui.animation;

import android.animation.TimeInterpolator;

import androidx.annotation.RequiresApi;

@SuppressWarnings("WeakerAccess")
@RequiresApi(11)
public class Easing {

    @SuppressWarnings("unused")
    public static final EasingFunction Linear = new EasingFunction() {
        public float getInterpolation(float input) {
            return input;
        }
    };
    @SuppressWarnings("unused")
    public static final EasingFunction EaseInQuad = new EasingFunction() {
        public float getInterpolation(float input) {
            return input * input;
        }
    };
    @SuppressWarnings("unused")
    public static final EasingFunction EaseOutQuad = new EasingFunction() {
        public float getInterpolation(float input) {
            return -input * (input - 2f);
        }
    };
    @SuppressWarnings("unused")
    public static final EasingFunction EaseInOutQuad = new EasingFunction() {
        public float getInterpolation(float input) {
            input *= 2f;

            if (input < 1f) {
                return 0.5f * input * input;
            }

            return -0.5f * ((--input) * (input - 2f) - 1f);
        }
    };
    @SuppressWarnings("unused")
    public static final EasingFunction EaseInCubic = new EasingFunction() {
        public float getInterpolation(float input) {
            return (float) Math.pow(input, 3);
        }
    };
    @SuppressWarnings("unused")
    public static final EasingFunction EaseOutCubic = new EasingFunction() {
        public float getInterpolation(float input) {
            input--;
            return (float) Math.pow(input, 3) + 1f;
        }
    };
    @SuppressWarnings("unused")
    public static final EasingFunction EaseInOutCubic = new EasingFunction() {
        public float getInterpolation(float input) {
            input *= 2f;
            if (input < 1f) {
                return 0.5f * (float) Math.pow(input, 3);
            }
            input -= 2f;
            return 0.5f * ((float) Math.pow(input, 3) + 2f);
        }
    };
    @SuppressWarnings("unused")
    public static final EasingFunction EaseInQuart = new EasingFunction() {

        public float getInterpolation(float input) {
            return (float) Math.pow(input, 4);
        }
    };
    @SuppressWarnings("unused")
    public static final EasingFunction EaseOutQuart = new EasingFunction() {
        public float getInterpolation(float input) {
            input--;
            return -((float) Math.pow(input, 4) - 1f);
        }
    };
    @SuppressWarnings("unused")
    public static final EasingFunction EaseInOutQuart = new EasingFunction() {
        public float getInterpolation(float input) {
            input *= 2f;
            if (input < 1f) {
                return 0.5f * (float) Math.pow(input, 4);
            }
            input -= 2f;
            return -0.5f * ((float) Math.pow(input, 4) - 2f);
        }
    };
    @SuppressWarnings("unused")
    public static final EasingFunction EaseInSine = new EasingFunction() {
        public float getInterpolation(float input) {
            return -(float) Math.cos(input * (Math.PI / 2f)) + 1f;
        }
    };
    @SuppressWarnings("unused")
    public static final EasingFunction EaseOutSine = new EasingFunction() {
        public float getInterpolation(float input) {
            return (float) Math.sin(input * (Math.PI / 2f));
        }
    };
    @SuppressWarnings("unused")
    public static final EasingFunction EaseInOutSine = new EasingFunction() {
        public float getInterpolation(float input) {
            return -0.5f * ((float) Math.cos(Math.PI * input) - 1f);
        }
    };
    @SuppressWarnings("unused")
    public static final EasingFunction EaseInExpo = new EasingFunction() {
        public float getInterpolation(float input) {
            return (input == 0) ? 0f : (float) Math.pow(2f, 10f * (input - 1f));
        }
    };
    @SuppressWarnings("unused")
    public static final EasingFunction EaseOutExpo = new EasingFunction() {
        public float getInterpolation(float input) {
            return (input == 1f) ? 1f : (-(float) Math.pow(2f, -10f * (input + 1f)));
        }
    };
    @SuppressWarnings("unused")
    public static final EasingFunction EaseInOutExpo = new EasingFunction() {
        public float getInterpolation(float input) {
            if (input == 0) {
                return 0f;
            } else if (input == 1f) {
                return 1f;
            }

            input *= 2f;
            if (input < 1f) {
                return 0.5f * (float) Math.pow(2f, 10f * (input - 1f));
            }
            return 0.5f * (-(float) Math.pow(2f, -10f * --input) + 2f);
        }
    };
    @SuppressWarnings("unused")
    public static final EasingFunction EaseInCirc = new EasingFunction() {
        public float getInterpolation(float input) {
            return -((float) Math.sqrt(1f - input * input) - 1f);
        }
    };
    @SuppressWarnings("unused")
    public static final EasingFunction EaseOutCirc = new EasingFunction() {
        public float getInterpolation(float input) {
            input--;
            return (float) Math.sqrt(1f - input * input);
        }
    };
    @SuppressWarnings("unused")
    public static final EasingFunction EaseInOutCirc = new EasingFunction() {
        public float getInterpolation(float input) {
            input *= 2f;
            if (input < 1f) {
                return -0.5f * ((float) Math.sqrt(1f - input * input) - 1f);
            }
            return 0.5f * ((float) Math.sqrt(1f - (input -= 2f) * input) + 1f);
        }
    };
    @SuppressWarnings("unused")
    public static final EasingFunction EaseInBack = new EasingFunction() {
        public float getInterpolation(float input) {
            final float s = 1.70158f;
            return input * input * ((s + 1f) * input - s);
        }
    };
    @SuppressWarnings("unused")
    public static final EasingFunction EaseOutBack = new EasingFunction() {
        public float getInterpolation(float input) {
            final float s = 1.70158f;
            input--;
            return (input * input * ((s + 1f) * input + s) + 1f);
        }
    };
    @SuppressWarnings("unused")
    public static final EasingFunction EaseInOutBack = new EasingFunction() {
        public float getInterpolation(float input) {
            float s = 1.70158f;
            input *= 2f;
            if (input < 1f) {
                return 0.5f * (input * input * (((s *= (1.525f)) + 1f) * input - s));
            }
            return 0.5f * ((input -= 2f) * input * (((s *= (1.525f)) + 1f) * input + s) + 2f);
        }
    };
    @SuppressWarnings("unused")
    public static final EasingFunction EaseOutBounce = new EasingFunction() {
        public float getInterpolation(float input) {
            float s = 7.5625f;
            if (input < (1f / 2.75f)) {
                return s * input * input;
            } else if (input < (2f / 2.75f)) {
                return s * (input -= (1.5f / 2.75f)) * input + 0.75f;
            } else if (input < (2.5f / 2.75f)) {
                return s * (input -= (2.25f / 2.75f)) * input + 0.9375f;
            }
            return s * (input -= (2.625f / 2.75f)) * input + 0.984375f;
        }
    };
    @SuppressWarnings("unused")
    public static final EasingFunction EaseInBounce = new EasingFunction() {
        public float getInterpolation(float input) {
            return 1f - EaseOutBounce.getInterpolation(1f - input);
        }
    };
    @SuppressWarnings("unused")
    public static final EasingFunction EaseInOutBounce = new EasingFunction() {
        public float getInterpolation(float input) {
            if (input < 0.5f) {
                return EaseInBounce.getInterpolation(input * 2f) * 0.5f;
            }
            return EaseOutBounce.getInterpolation(input * 2f - 1f) * 0.5f + 0.5f;
        }
    };
    private static final float DOUBLE_PI = 2f * (float) Math.PI;
    @SuppressWarnings("unused")
    public static final EasingFunction EaseInElastic = new EasingFunction() {
        public float getInterpolation(float input) {
            if (input == 0) {
                return 0f;
            } else if (input == 1) {
                return 1f;
            }

            float p = 0.3f;
            float s = p / DOUBLE_PI * (float) Math.asin(1f);
            return -((float) Math.pow(2f, 10f * (input -= 1f))
                    * (float) Math.sin((input - s) * DOUBLE_PI / p));
        }
    };
    @SuppressWarnings("unused")
    public static final EasingFunction EaseOutElastic = new EasingFunction() {
        public float getInterpolation(float input) {
            if (input == 0) {
                return 0f;
            } else if (input == 1) {
                return 1f;
            }

            float p = 0.3f;
            float s = p / DOUBLE_PI * (float) Math.asin(1f);
            return 1f
                    + (float) Math.pow(2f, -10f * input)
                    * (float) Math.sin((input - s) * DOUBLE_PI / p);
        }
    };
    @SuppressWarnings("unused")
    public static final EasingFunction EaseInOutElastic = new EasingFunction() {
        public float getInterpolation(float input) {
            if (input == 0) {
                return 0f;
            }

            input *= 2f;
            if (input == 2) {
                return 1f;
            }

            float p = 1f / 0.45f;
            float s = 0.45f / DOUBLE_PI * (float) Math.asin(1f);
            if (input < 1f) {
                return -0.5f
                        * ((float) Math.pow(2f, 10f * (input -= 1f))
                        * (float) Math.sin((input * 1f - s) * DOUBLE_PI * p));
            }
            return 1f + 0.5f
                    * (float) Math.pow(2f, -10f * (input -= 1f))
                    * (float) Math.sin((input * 1f - s) * DOUBLE_PI * p);
        }
    };

    public interface EasingFunction extends TimeInterpolator {
        @Override
        float getInterpolation(float input);
    }

}