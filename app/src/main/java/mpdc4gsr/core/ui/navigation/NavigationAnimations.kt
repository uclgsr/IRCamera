package mpdc4gsr.core.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween

object NavigationAnimations {
    private const val ANIMATION_DURATION_MS = 300
    private const val FAST_ANIMATION_DURATION_MS = 200

    private fun <S> AnimatedContentTransitionScope<S>.slideTransition(
        direction: AnimatedContentTransitionScope.SlideDirection,
        duration: Int,
        isEnter: Boolean,
    ): Any =
        if (isEnter) {
            slideIntoContainer(
                towards = direction,
                animationSpec = tween(duration),
            )
        } else {
            slideOutOfContainer(
                towards = direction,
                animationSpec = tween(duration),
            )
        }

    fun <S> AnimatedContentTransitionScope<S>.slideInFromRight(): EnterTransition =
        slideTransition(
            AnimatedContentTransitionScope.SlideDirection.Left,
            ANIMATION_DURATION_MS,
            true,
        ) as EnterTransition

    fun <S> AnimatedContentTransitionScope<S>.slideOutToLeft(): ExitTransition =
        slideTransition(
            AnimatedContentTransitionScope.SlideDirection.Left,
            ANIMATION_DURATION_MS,
            false,
        ) as ExitTransition

    fun <S> AnimatedContentTransitionScope<S>.slideInFromLeft(): EnterTransition =
        slideTransition(
            AnimatedContentTransitionScope.SlideDirection.Right,
            ANIMATION_DURATION_MS,
            true,
        ) as EnterTransition

    fun <S> AnimatedContentTransitionScope<S>.slideOutToRight(): ExitTransition =
        slideTransition(
            AnimatedContentTransitionScope.SlideDirection.Right,
            ANIMATION_DURATION_MS,
            false,
        ) as ExitTransition

    fun <S> AnimatedContentTransitionScope<S>.fastSlideInFromRight(): EnterTransition =
        slideTransition(
            AnimatedContentTransitionScope.SlideDirection.Left,
            FAST_ANIMATION_DURATION_MS,
            true,
        ) as EnterTransition

    fun <S> AnimatedContentTransitionScope<S>.fastSlideOutToLeft(): ExitTransition =
        slideTransition(
            AnimatedContentTransitionScope.SlideDirection.Left,
            FAST_ANIMATION_DURATION_MS,
            false,
        ) as ExitTransition

    fun <S> AnimatedContentTransitionScope<S>.fastSlideInFromLeft(): EnterTransition =
        slideTransition(
            AnimatedContentTransitionScope.SlideDirection.Right,
            FAST_ANIMATION_DURATION_MS,
            true,
        ) as EnterTransition

    fun <S> AnimatedContentTransitionScope<S>.fastSlideOutToRight(): ExitTransition =
        slideTransition(
            AnimatedContentTransitionScope.SlideDirection.Right,
            FAST_ANIMATION_DURATION_MS,
            false,
        ) as ExitTransition
}
