package io.github.imove.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            // HomeScreen will be implemented in Task 19
        }
        composable(
            route = Screen.Transfer.route,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType },
                navArgument("startDate") { type = NavType.LongType; defaultValue = 0L },
                navArgument("endDate") { type = NavType.LongType; defaultValue = 0L }
            )
        ) { _ ->
            // TransferScreen will be implemented in Task 20
        }
        composable(
            route = Screen.Preview.route,
            arguments = listOf(
                navArgument("fileIndex") { type = NavType.IntType }
            )
        ) { _ ->
            // PreviewScreen will be implemented in Task 21
        }
        composable(Screen.Settings.route) {
            // SettingsScreen will be implemented in Task 22
        }
    }
}
