package com.commencis.ai.bitesense.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.navOptions

/**
 * Extension functions for safer navigation
 */

/**
 * Navigate safely to avoid crashes from rapid clicks
 */
fun <T : Any> NavController.navigateSafely(
    route: T,
    navOptions: NavOptions? = null
) {
    try {
        navigate(route, navOptions)
    } catch (e: Exception) {
        // Log the error but don't crash
        android.util.Log.e("Navigation", "Navigation failed", e)
    }
}

/**
 * Navigate with single top to avoid duplicate destinations
 */
fun <T : Any> NavController.navigateSingleTop(route: T) {
    navigateSafely(
        route,
        navOptions {
            launchSingleTop = true
        }
    )
}

/**
 * Pop back stack safely
 */
fun NavController.popBackStackSafely() {
    try {
        if (currentBackStackEntry != null) {
            popBackStack()
        }
    } catch (e: Exception) {
        android.util.Log.e("Navigation", "Pop back stack failed", e)
    }
}