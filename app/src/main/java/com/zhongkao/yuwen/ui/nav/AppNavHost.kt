package com.zhongkao.yuwen.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zhongkao.yuwen.AppContainer
import com.zhongkao.yuwen.ui.dashboard.DashboardScreen
import com.zhongkao.yuwen.ui.exercise.ExerciseScreen
import com.zhongkao.yuwen.ui.settings.SettingsScreen
import com.zhongkao.yuwen.ui.setup.SetupScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
    const val SETUP = "setup"
    const val EXERCISE = "exercise"
}

@Composable
fun AppNavHost(container: AppContainer) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.DASHBOARD) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                textBankRepository = container.textBankRepository,
                onStartPractice = { nav.navigate(Routes.SETUP) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                keyStore = container.secureKeyStore,
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.SETUP) {
            SetupScreen(
                onStart = { req ->
                    container.pendingRequest = req
                    nav.navigate(Routes.EXERCISE)
                },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.EXERCISE) {
            ExerciseScreen(
                container = container,
                onExit = {
                    nav.popBackStack(Routes.DASHBOARD, inclusive = false)
                }
            )
        }
    }
}
