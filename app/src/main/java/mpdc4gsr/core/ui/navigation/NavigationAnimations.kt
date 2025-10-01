package mpdc4gsr.core.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween

object NavigationAnimations {
    private const val ANIMATION_DURATION_MS = 300
    private const val FAST_ANIMATION_DURATION_MS = 200
    
    fun <S> AnimatedContentTransitionScope<S>.slideInFromRight(): EnterTransition = slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(ANIMATION_DURATION_MS)
    )
    
    fun <S> AnimatedContentTransitionScope<S>.slideOutToLeft(): ExitTransition = slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(ANIMATION_DURATION_MS)
    )
    
    fun <S> AnimatedContentTransitionScope<S>.slideInFromLeft(): EnterTransition = slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = tween(ANIMATION_DURATION_MS)
    )
    
    fun <S> AnimatedContentTransitionScope<S>.slideOutToRight(): ExitTransition = slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = tween(ANIMATION_DURATION_MS)
    )
    
    fun <S> AnimatedContentTransitionScope<S>.fastSlideInFromRight(): EnterTransition = slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(FAST_ANIMATION_DURATION_MS)
    )
    
    fun <S> AnimatedContentTransitionScope<S>.fastSlideOutToLeft(): ExitTransition = slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(FAST_ANIMATION_DURATION_MS)
    )
}
