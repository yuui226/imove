package io.github.imove.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import io.github.imove.ui.home.HomeScreen
import io.github.imove.ui.preview.PreviewScreen
import io.github.imove.ui.settings.SettingsScreen
import io.github.imove.ui.transfer.TransferScreen
import io.github.imove.viewmodel.HomeViewModel
import io.github.imove.viewmodel.PreviewViewModel
import io.github.imove.viewmodel.SettingsViewModel
import io.github.imove.viewmodel.TransferViewModel

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                connectedDevice = viewModel.connectedDevice.value,
                onTransferToday = {
                    navController.navigate(Screen.Transfer.createRoute("today"))
                },
                onTransferThreeDays = {
                    navController.navigate(Screen.Transfer.createRoute("three_days"))
                },
                onTransferCustom = {
                    navController.navigate(Screen.Transfer.createRoute("custom"))
                },
                onTransferAll = {
                    navController.navigate(Screen.Transfer.createRoute("all"))
                },
                onSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        composable(
            route = Screen.Transfer.route,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType },
                navArgument("startDate") { type = NavType.LongType; defaultValue = 0L },
                navArgument("endDate") { type = NavType.LongType; defaultValue = 0L }
            )
        ) { backStackEntry ->
            val viewModel: TransferViewModel = hiltViewModel()
            val mode = backStackEntry.arguments?.getString("mode") ?: "all"
            val startDate = backStackEntry.arguments?.getLong("startDate") ?: 0L
            val endDate = backStackEntry.arguments?.getLong("endDate") ?: 0L
            TransferScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onPreview = { index ->
                    navController.navigate(Screen.Preview.createRoute(index))
                }
            )
        }
        composable(
            route = Screen.Preview.route,
            arguments = listOf(
                navArgument("fileIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val viewModel: PreviewViewModel = hiltViewModel()
            val fileIndex = backStackEntry.arguments?.getInt("fileIndex") ?: 0
            PreviewScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
