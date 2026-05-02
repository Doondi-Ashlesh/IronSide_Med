package com.ironsidemedical.connect.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ironsidemedical.connect.presentation.screens.audit.AuditLogScreen
import com.ironsidemedical.connect.presentation.screens.dashboard.DashboardScreen
import com.ironsidemedical.connect.presentation.screens.device.DeviceScanScreen
import com.ironsidemedical.connect.presentation.screens.history.VitalHistoryScreen
import com.ironsidemedical.connect.presentation.screens.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object DeviceScan : Screen("device_scan")
    data object VitalHistory : Screen("vital_history/{sessionId}") {
        fun withSession(id: String) = "vital_history/$id"
    }
    data object Settings : Screen("settings")
    data object AuditLog : Screen("audit_log")
}

@Composable
fun IronSideNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Dashboard.route) {

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToScan = { navController.navigate(Screen.DeviceScan.route) },
                onNavigateToHistory = { id -> navController.navigate(Screen.VitalHistory.withSession(id)) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToAuditLog = { navController.navigate(Screen.AuditLog.route) },
            )
        }

        composable(Screen.DeviceScan.route) {
            DeviceScanScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.VitalHistory.route) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            VitalHistoryScreen(sessionId = sessionId, onBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.AuditLog.route) {
            AuditLogScreen(onBack = { navController.popBackStack() })
        }
    }
}
