package com.zhongkao.yuwen.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zhongkao.yuwen.AppContainer
import com.zhongkao.yuwen.ui.dashboard.DashboardScreen
import com.zhongkao.yuwen.ui.exercise.ExerciseScreen
import com.zhongkao.yuwen.ui.result.ResultScreen
import com.zhongkao.yuwen.ui.review.PendingReviewScreen
import com.zhongkao.yuwen.ui.review.ReviewScreen
import com.zhongkao.yuwen.ui.settings.SettingsScreen
import com.zhongkao.yuwen.ui.setup.SetupScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
    const val SETUP = "setup"
    const val EXERCISE = "exercise"
    const val RESULT = "result"
    const val REVIEW = "review"
    const val PENDING_REVIEW = "pending_review"
    const val ARG_EXERCISE_ID = "exerciseId"
    fun result(exerciseId: Long) = "$RESULT/$exerciseId"
}

@Composable
fun AppNavHost(container: AppContainer) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.DASHBOARD) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                container = container,
                onStartPractice = { nav.navigate(Routes.SETUP) },
                onOpenReview = { nav.navigate(Routes.REVIEW) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                onOpenPendingReview = { nav.navigate(Routes.PENDING_REVIEW) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                container = container,
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.PENDING_REVIEW) {
            PendingReviewScreen(
                container = container,
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.SETUP) {
            SetupScreen(
                container = container,
                onStart = { req ->
                    container.pendingRequest = req
                    nav.navigate(Routes.EXERCISE)
                },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.REVIEW) {
            ReviewScreen(
                container = container,
                onBack = { nav.popBackStack() },
                onRePractice = {
                    // pendingFocus 已在复习页设置，进入出题设置页预填侧重考点。
                    nav.navigate(Routes.SETUP)
                }
            )
        }
        composable(Routes.EXERCISE) {
            ExerciseScreen(
                container = container,
                onExit = { nav.popBackStack(Routes.DASHBOARD, inclusive = false) },
                onFinished = { id ->
                    nav.navigate(Routes.result(id)) {
                        popUpTo(Routes.EXERCISE) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = "${Routes.RESULT}/{${Routes.ARG_EXERCISE_ID}}",
            arguments = listOf(navArgument(Routes.ARG_EXERCISE_ID) { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong(Routes.ARG_EXERCISE_ID) ?: 0L
            ResultScreen(
                container = container,
                exerciseId = id,
                onExit = { nav.popBackStack(Routes.DASHBOARD, inclusive = false) }
            )
        }
    }
}
