package com.zhongkao.yuwen.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zhongkao.yuwen.AppContainer
import com.zhongkao.yuwen.ui.dashboard.DashboardScreen
import com.zhongkao.yuwen.ui.settings.SettingsScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavHost(container: AppContainer) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.DASHBOARD) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                textBankRepository = container.textBankRepository,
                onOpenSettings = { nav.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                keyStore = container.secureKeyStore,
                onBack = { nav.popBackStack() }
            )
        }
    }
}
