// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\view' directory and its subdirectories.
// Total files: 3 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\view\BreatheInterpolator.java =====

package com.mpdc4gsr.libunified.app.comm.view;

import android.animation.TimeInterpolator;

class BreatheInterpolator implements TimeInterpolator {

    @Override

    public float getInterpolation(float input) {

        float x = 6 * input;

        float k = 1.0f / 3;

        int t = 6;

        int n = 1;//[CHINESE_TEXT]ï¼Œ[CHINESE_TEXT]

        float PI = 3.1416f;

        float output = 0;

        if (x >= ((n - 1) * t) && x < ((n - (1 - k)) * t)) {

            output = (float) (0.5 * Math.sin((PI / (k * t)) * ((x - k * t / 2) - (n - 1) * t)) + 0.5);

        } else if (x >= (n - (1 - k)) * t && x < n * t) {

            output = (float) Math.pow((0.5 * Math.sin((PI / ((1 - k) * t)) * ((x - (3 - k) * t / 2) - (n - 1) * t)) + 0.5), 2);

        }

        return output;

    }

    public void updateTime() {
        String a = "";
        String[] as = a.split("");
    }

}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\view\CommLoadMoreView.kt =====

package com.mpdc4gsr.libunified.app.comm.view

import android.view.View
import android.view.ViewGroup
import com.chad.library.adapter.base.loadmore.BaseLoadMoreView
import com.chad.library.adapter.base.util.getItemView
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.mpdc4gsr.libunified.R

class CommLoadMoreView : BaseLoadMoreView() {
    override fun getRootView(parent: ViewGroup): View =
        parent.getItemView(R.layout.layout_load_more_view)

    override fun getLoadingView(holder: BaseViewHolder): View =
        holder.getView(R.id.load_more_loading_view)

    override fun getLoadComplete(holder: BaseViewHolder): View =
        holder.getView(R.id.load_more_load_complete_view)

    override fun getLoadEndView(holder: BaseViewHolder): View =
        holder.getView(R.id.load_more_load_end_view)

    override fun getLoadFailView(holder: BaseViewHolder): View =
        holder.getView(R.id.load_more_load_fail_view)
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\comm\view\TempLayout.kt =====

package com.mpdc4gsr.libunified.app.comm.view

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.mpdc4gsr.libunified.R

class TempLayout : LinearLayout {
    companion object {
        val TYPE_HOT = 1
        val TYPE_LT = 2
        val TYPE_A = 3
    }

    private var alphaAnimator: ObjectAnimator? = null
    var rootV: View? = null
    var bg: View? = null
    var isHot: Boolean = true
    var type = -1

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView()
    }

    var animatorAlpha = 1f
    private fun initView() {
        rootV = LayoutInflater.from(context).inflate(R.layout.layout_temp_bg, this)
        bg = rootV?.findViewById(R.id.bg)
        alphaAnimator = ObjectAnimator.ofFloat(this, "alpha", 0f, 1f)
        alphaAnimator?.duration = 500
        alphaAnimator?.interpolator =
            BreatheInterpolator()
        alphaAnimator?.addUpdateListener {
            animatorAlpha = it.getAnimatedValue("alpha") as Float
        }
        alphaAnimator?.repeatCount = ValueAnimator.INFINITE
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    )

    fun startAnimation(type: Int) {
        this.visibility = View.VISIBLE
        if (this.type != type) {
            alphaAnimator?.cancel()
            alphaAnimator?.removeAllListeners()
            when (type) {
                TYPE_HOT -> {
                    isHot = true
                    alphaAnimator?.repeatCount = ValueAnimator.INFINITE
                    bg?.setBackgroundResource(R.drawable.ic_ir_read_bg)
                }

                TYPE_A -> {
                    alphaAnimator?.repeatCount = 0
                    alphaAnimator?.addListener(animatorListener)
                }

                else -> {
                    alphaAnimator?.repeatCount = ValueAnimator.INFINITE
                    isHot = false
                    bg?.setBackgroundResource(R.drawable.ic_ir_blue_bg)
                }
            }
            if (isAttachedToWindow) {
                try {
                    alphaAnimator?.start()
                } catch (e: IllegalStateException) {
                    Log.w("TempLayout", "Failed to start animator: ${e.message}")
                }
            }
            this.type = type
        }
    }

    var animatorListener: Animator.AnimatorListener =
        object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
            }

            override fun onAnimationEnd(animation: Animator) {
                if (this@TempLayout.visibility == View.VISIBLE && isAttachedToWindow) {
                    isHot = !isHot
                    if (isHot) {
                        bg?.setBackgroundResource(R.drawable.ic_ir_read_bg)
                    } else {
                        bg?.setBackgroundResource(R.drawable.ic_ir_blue_bg)
                    }
                    try {
                        alphaAnimator?.start()
                    } catch (e: IllegalStateException) {
                        Log.w("TempLayout", "Failed to restart animator in onAnimationEnd: ${e.message}")
                    }
                }
            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        }

    fun stopAnimation() {
        this.type = -1
        alphaAnimator?.removeAllListeners()
        this.visibility = View.GONE
        alphaAnimator?.cancel()
    }

    fun startAlphaBreathAnimation() {
        if (isAttachedToWindow) {
            try {
                alphaAnimator?.start()
            } catch (e: IllegalStateException) {
                Log.w("TempLayout", "Failed to start breath animation: ${e.message}")
            }
        }
    }
}