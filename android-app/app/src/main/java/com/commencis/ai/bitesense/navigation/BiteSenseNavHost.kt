package com.commencis.ai.bitesense.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navOptions
import androidx.navigation.toRoute
import com.commencis.ai.bitesense.ui.chat.ChatScreen
import com.commencis.ai.bitesense.ui.result.ResultScreenRoute
import com.commencis.ai.bitesense.ui.dashboard.DashboardRoute as DashboardScreen

@Composable
fun BiteSenseNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = DashboardRoute,
        modifier = modifier
    ) {
        // Dashboard Screen (Main screen with tabs)
        composable<DashboardRoute> {
            DashboardScreen(
                onNavigateToResult = { imageUri, analysisMode ->
                    navController.navigateSingleTop(
                        ResultRoute(
                            imageUri = imageUri.toString(),
                            analysisMode = analysisMode
                        )
                    )
                },
                onNavigateToChat = {
                    navController.navigateSingleTop(ChatRoute())
                },
                onNavigateToBiteDetail = { biteRecord ->
                    navController.navigateSingleTop(
                        ResultRoute(
                            imageUri = biteRecord.imageUri,
                            biteRecordId = biteRecord.id
                        )
                    )
                }
            )
        }

        // Result Screen with slide animation
        composable<ResultRoute>(
            enterTransition = NavigationAnimations.slideInFromBottom(),
            exitTransition = NavigationAnimations.slideOutToBottom(),
            popEnterTransition = NavigationAnimations.slideInFromBottom(),
            popExitTransition = NavigationAnimations.slideOutToBottom()
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<ResultRoute>()

            ResultScreenRoute(
                imageUri = route.imageUri.toUri(),
                biteRecordId = route.biteRecordId,
                analysisMode = route.analysisMode,
                onNavigateBack = {
                    navController.popBackStackSafely()
                },
                onNavigateToChat = { recordId ->
                    navController.navigateSafely(
                        route = ChatRoute(
                            imageUri = route.imageUri,
                            biteRecordId = recordId
                        ),
                        navOptions = navOptions {
                            launchSingleTop = true
                            popUpTo<ResultRoute> {
                                inclusive = true
                            }
                        }
                    )
                }
            )
        }

        // Chat Screen with slide animation
        composable<ChatRoute>(
            enterTransition = NavigationAnimations.slideInFromBottom(),
            exitTransition = NavigationAnimations.slideOutToBottom(),
            popEnterTransition = NavigationAnimations.slideInFromBottom(),
            popExitTransition = NavigationAnimations.slideOutToBottom()
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<ChatRoute>()

            ChatScreen(
                capturedImageUri = route.imageUri?.toUri(),
                biteRecordId = route.biteRecordId,
                onClose = {
                    navController.navigateSafely(
                        route = DashboardRoute,
                        navOptions = navOptions {
                            launchSingleTop = true
                            popUpTo<ChatRoute> {
                                inclusive = true
                            }
                        }
                    )
                }
            )
        }
    }
}