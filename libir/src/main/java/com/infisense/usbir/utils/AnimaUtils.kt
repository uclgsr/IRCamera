package com.infisense.usbir.utils
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.view.animation.ScaleAnimation

/**
 * @author: CaiSongL
 * @date: 2022/6/9 22:14
 */
public object AnimaUtils {
    /**
     * 默认动画持续时间
     */
    const val DEFAULT_ANIMATION_DURATION: Long = 400

    /**
     * Get/Retrieve一个旋转动画
     *
     * @param fromDegrees       start角度
     * @param toDegrees         end角度
     * @param pivotXType        旋转centerpointX轴坐标相对type
     * @param pivotXValue       旋转centerpointX轴坐标
     * @param pivotYType        旋转centerpointY轴坐标相对type
     * @param pivotYValue       旋转centerpointY轴坐标
     * @param durationMillis    持续时间
     * @param animationListener 动画Listener器
     * @return 一个旋转动画
     */
    fun getRotateAnimation(
        fromDegrees: Float,
        toDegrees: Float,
        pivotXType: Int,
        pivotXValue: Float,
        pivotYType: Int,
        pivotYValue: Float,
        durationMillis: Long,
        animationListener: Animation.AnimationListener?,
    ): RotateAnimation {
        val rotateAnimation =
            RotateAnimation(
                fromDegrees,
                toDegrees,
                pivotXType,
                pivotXValue,
                pivotYType,
                pivotYValue,
            )
        rotateAnimation.duration = durationMillis
        if (animationListener != null) {
            rotateAnimation.setAnimationListener(animationListener)
        }
        return rotateAnimation
    }

    /**
     * Get/Retrieve一个根据视图自身centerpoint旋转的动画
     *
     * @param durationMillis    动画持续时间
     * @param animationListener 动画Listener器
     * @return 一个根据centerpoint旋转的动画
     */
    fun getRotateAnimationByCenter(
        durationMillis: Long,
        animationListener: Animation.AnimationListener?,
    ): RotateAnimation {
        return getRotateAnimation(
            0f,
            359f,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            durationMillis,
            animationListener,
        )
    }

    /**
     * Get/Retrieve一个根据centerpoint旋转的动画
     *
     * @param duration 动画持续时间
     * @return 一个根据centerpoint旋转的动画
     */
    fun getRotateAnimationByCenter(duration: Long): RotateAnimation {
        return getRotateAnimationByCenter(duration, null)
    }

    /**
     * Get/Retrieve一个根据视图自身centerpoint旋转的动画
     *
     * @param animationListener 动画Listener器
     * @return 一个根据centerpoint旋转的动画
     */
    fun getRotateAnimationByCenter(animationListener: Animation.AnimationListener?): RotateAnimation {
        return getRotateAnimationByCenter(
            DEFAULT_ANIMATION_DURATION,
            animationListener,
        )
    }

    /**
     * Get/Retrieve一个根据centerpoint旋转的动画
     *
     * @return 一个根据centerpoint旋转的动画，默认持续时间为DEFAULT_ANIMATION_DURATION
     */
    val rotateAnimationByCenter: RotateAnimation
        get() = getRotateAnimationByCenter(DEFAULT_ANIMATION_DURATION, null)

    /**
     * Get/Retrieve一个透明度渐变动画
     *
     * @param fromAlpha         start时的透明度
     * @param toAlpha           end时的透明度都
     * @param durationMillis    持续时间
     * @param animationListener 动画Listener器
     * @return 一个透明度渐变动画
     */
    fun getAlphaAnimation(
        fromAlpha: Float,
        toAlpha: Float,
        durationMillis: Long,
        animationListener: Animation.AnimationListener?,
    ): AlphaAnimation {
        val alphaAnimation = AlphaAnimation(fromAlpha, toAlpha)
        alphaAnimation.duration = durationMillis
        if (animationListener != null) {
            alphaAnimation.setAnimationListener(animationListener)
        }
        return alphaAnimation
    }

    /**
     * Get/Retrieve一个透明度渐变动画
     *
     * @param fromAlpha      start时的透明度
     * @param toAlpha        end时的透明度都
     * @param durationMillis 持续时间
     * @return 一个透明度渐变动画
     */
    fun getAlphaAnimation(
        fromAlpha: Float,
        toAlpha: Float,
        durationMillis: Long,
    ): AlphaAnimation {
        return getAlphaAnimation(fromAlpha, toAlpha, durationMillis, null)
    }

    /**
     * Get/Retrieve一个透明度渐变动画
     *
     * @param fromAlpha         start时的透明度
     * @param toAlpha           end时的透明度都
     * @param animationListener 动画Listener器
     * @return 一个透明度渐变动画，默认持续时间为DEFAULT_ANIMATION_DURATION
     */
    fun getAlphaAnimation(
        fromAlpha: Float,
        toAlpha: Float,
        animationListener: Animation.AnimationListener?,
    ): AlphaAnimation {
        return getAlphaAnimation(
            fromAlpha,
            toAlpha,
            DEFAULT_ANIMATION_DURATION,
            animationListener,
        )
    }

    /**
     * Get/Retrieve一个透明度渐变动画
     *
     * @param fromAlpha start时的透明度
     * @param toAlpha   end时的透明度都
     * @return 一个透明度渐变动画，默认持续时间为DEFAULT_ANIMATION_DURATION
     */
    fun getAlphaAnimation(
        fromAlpha: Float,
        toAlpha: Float,
    ): AlphaAnimation {
        return getAlphaAnimation(
            fromAlpha,
            toAlpha,
            DEFAULT_ANIMATION_DURATION,
            null,
        )
    }

    /**
     * Get/Retrieve一个由完全Show/Display变为不Visible的透明度渐变动画
     *
     * @param durationMillis    持续时间
     * @param animationListener 动画Listener器
     * @return 一个由完全Show/Display变为不Visible的透明度渐变动画
     */
    fun getHiddenAlphaAnimation(
        durationMillis: Long,
        animationListener: Animation.AnimationListener?,
    ): AlphaAnimation {
        return getAlphaAnimation(1.0f, 0.0f, durationMillis, animationListener)
    }

    /**
     * Get/Retrieve一个由完全Show/Display变为不Visible的透明度渐变动画
     *
     * @param durationMillis 持续时间
     * @return 一个由完全Show/Display变为不Visible的透明度渐变动画
     */
    fun getHiddenAlphaAnimation(durationMillis: Long): AlphaAnimation {
        return getHiddenAlphaAnimation(durationMillis, null)
    }

    /**
     * Get/Retrieve一个由完全Show/Display变为不Visible的透明度渐变动画
     *
     * @param animationListener 动画Listener器
     * @return 一个由完全Show/Display变为不Visible的透明度渐变动画，默认持续时间为DEFAULT_ANIMATION_DURATION
     */
    fun getHiddenAlphaAnimation(animationListener: Animation.AnimationListener?): AlphaAnimation {
        return getHiddenAlphaAnimation(
            DEFAULT_ANIMATION_DURATION,
            animationListener,
        )
    }

    /**
     * Get/Retrieve一个由完全Show/Display变为不Visible的透明度渐变动画
     *
     * @return 一个由完全Show/Display变为不Visible的透明度渐变动画，默认持续时间为DEFAULT_ANIMATION_DURATION
     */
    val hiddenAlphaAnimation: AlphaAnimation
        get() = getHiddenAlphaAnimation(DEFAULT_ANIMATION_DURATION, null)

    /**
     * Get/Retrieve一个由不Visible变为完全Show/Display的透明度渐变动画
     *
     * @param durationMillis    持续时间
     * @param animationListener 动画Listener器
     * @return 一个由不Visible变为完全Show/Display的透明度渐变动画
     */
    fun getShowAlphaAnimation(
        durationMillis: Long,
        animationListener: Animation.AnimationListener?,
    ): AlphaAnimation {
        return getAlphaAnimation(0.0f, 1.0f, durationMillis, animationListener)
    }

    /**
     * Get/Retrieve一个由不Visible变为完全Show/Display的透明度渐变动画
     *
     * @param durationMillis 持续时间
     * @return 一个由不Visible变为完全Show/Display的透明度渐变动画
     */
    fun getShowAlphaAnimation(durationMillis: Long): AlphaAnimation {
        return getAlphaAnimation(0.0f, 1.0f, durationMillis, null)
    }

    /**
     * Get/Retrieve一个由不Visible变为完全Show/Display的透明度渐变动画
     *
     * @param animationListener 动画Listener器
     * @return 一个由不Visible变为完全Show/Display的透明度渐变动画，默认持续时间为DEFAULT_ANIMATION_DURATION
     */
    fun getShowAlphaAnimation(animationListener: Animation.AnimationListener?): AlphaAnimation {
        return getAlphaAnimation(
            0.0f,
            1.0f,
            DEFAULT_ANIMATION_DURATION,
            animationListener,
        )
    }

    /**
     * Get/Retrieve一个由不Visible变为完全Show/Display的透明度渐变动画
     *
     * @return 一个由不Visible变为完全Show/Display的透明度渐变动画，默认持续时间为DEFAULT_ANIMATION_DURATION
     */
    val showAlphaAnimation: AlphaAnimation
        get() = getAlphaAnimation(0.0f, 1.0f, DEFAULT_ANIMATION_DURATION, null)

    /**
     * Get/Retrieve一个缩小动画
     *
     * @param durationMillis   时间
     * @param animationListener  Listener
     * @return 一个缩小动画
     */
    fun getLessenScaleAnimation(
        durationMillis: Long,
        animationListener: Animation.AnimationListener?,
    ): ScaleAnimation {
        val scaleAnimation =
            ScaleAnimation(
                1.0f,
                0.0f,
                1.0f,
                0.0f,
                ScaleAnimation.RELATIVE_TO_SELF.toFloat(),
                ScaleAnimation.RELATIVE_TO_SELF.toFloat(),
            )
        scaleAnimation.duration = durationMillis
        scaleAnimation.setAnimationListener(animationListener)
        return scaleAnimation
    }

    /**
     * Get/Retrieve一个缩小动画
     *
     * @param durationMillis 时间
     * @return 一个缩小动画
     */
    fun getLessenScaleAnimation(durationMillis: Long): ScaleAnimation {
        return getLessenScaleAnimation(durationMillis, null)
    }

    /**
     * Get/Retrieve一个缩小动画
     *
     * @param animationListener  Listener
     * @return Return一个缩小的动画
     */
    fun getLessenScaleAnimation(animationListener: Animation.AnimationListener?): ScaleAnimation {
        return getLessenScaleAnimation(
            DEFAULT_ANIMATION_DURATION,
            animationListener,
        )
    }

    /**
     * Get/Retrieve一个放大动画
     * @param durationMillis   时间
     * @param animationListener  Listener
     *
     * @return Return一个放大的效果
     */
    fun getAmplificationAnimation(
        durationMillis: Long,
        animationListener: Animation.AnimationListener?,
    ): ScaleAnimation {
        val scaleAnimation =
            ScaleAnimation(
                0.0f,
                1.0f,
                0.0f,
                1.0f,
                ScaleAnimation.RELATIVE_TO_SELF.toFloat(),
                ScaleAnimation.RELATIVE_TO_SELF.toFloat(),
            )
        scaleAnimation.duration = durationMillis
        scaleAnimation.setAnimationListener(animationListener)
        return scaleAnimation
    }

    /**
     * Get/Retrieve一个放大动画
     *
     * @param durationMillis   时间
     *
     * @return Return一个放大的效果
     */
    fun getAmplificationAnimation(durationMillis: Long): ScaleAnimation {
        return getAmplificationAnimation(durationMillis, null)
    }

    /**
     * Get/Retrieve一个放大动画
     *
     * @param animationListener  Listener
     * @return Return一个放大的效果
     */
    fun getAmplificationAnimation(animationListener: Animation.AnimationListener?): ScaleAnimation {
        return getAmplificationAnimation(
            DEFAULT_ANIMATION_DURATION,
            animationListener,
        )
    }
}
