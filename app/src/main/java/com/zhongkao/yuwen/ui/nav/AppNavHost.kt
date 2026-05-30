package com.zhongkao.yuwen.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zhongkao.yuwen.core.security.SecureKeyStore
import com.zhongkao.yuwen.ui.dashboard.DashboardScreen
import com.zhongkao.yuwen.ui.settings.SettingsScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavHost(keyStore: SecureKeyStore) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.DASHBOARD) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(onOpenSettings = { nav.navigate(Routes.SETTINGS) })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                keyStore = keyStore,
                onBack = { nav.popBackStack() }
            )
        }
    }
}
