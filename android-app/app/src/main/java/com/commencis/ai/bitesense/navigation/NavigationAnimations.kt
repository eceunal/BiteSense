package com.commencis.ai.bitesense.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry

/**
 * Slide animations for navigation transitions
 */
object NavigationAnimations {
    const val ANIMATION_DURATION = 300
    
    fun slideInFromBottom(): (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition) = {
        slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(ANIMATION_DURATION)
        )
    }
    
    fun slideOutToBottom(): (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition) = {
        slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(ANIMATION_DURATION)
        )
    }
    
    fun slideInFromTop(): (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition) = {
        slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(ANIMATION_DURATION)
        )
    }
    
    fun slideOutToTop(): (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition) = {
        slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(ANIMATION_DURATION)
        )
    }
}