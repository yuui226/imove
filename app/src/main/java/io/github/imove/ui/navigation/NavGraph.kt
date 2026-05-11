package io.github.imove.ui.navigation

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import io.github.imove.data.usb.StorageAccessManager
import io.github.imove.ui.home.HomeScreen
import io.github.imove.util.DateUtils
import io.github.imove.ui.preview.PreviewScreen
import io.github.imove.ui.settings.SettingsScreen
import io.github.imove.ui.transfer.TransferScreen
import io.github.imove.viewmodel.HomeViewModel
import io.github.imove.viewmodel.PreviewViewModel
import io.github.imove.viewmodel.SettingsViewModel
import io.github.imove.viewmodel.TransferViewModel

@Composable
fun NavGraph(navController: NavHostController, storageAccessManager: StorageAccessManager) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = hiltViewModel()
            val connectedDevice by viewModel.connectedDevice.collectAsState()
            val isDetecting by viewModel.isDetecting.collectAsState()
            val isRestoring by viewModel.isRestoring.collectAsState()

            val directoryPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.data?.let { uri ->
                        viewModel.saveSourcePath(uri)
                    }
                }
            }

            HomeScreen(
                connectedDevice = connectedDevice,
                isDetecting = isDetecting,
                isRestoring = isRestoring,
                onSelectSourceDirectory = {
                    val intent = storageAccessManager.createOpenDocumentTreeIntent()
                    directoryPickerLauncher.launch(intent)
                },
                onTransferToday = {
                    navController.navigate(Screen.Transfer.createRoute("today"))
                },
                onTransferThreeDays = {
                    navController.navigate(Screen.Transfer.createRoute("three_days"))
                },
                onTransferCustom = {
                    navController.navigate(Screen.Transfer.createRoute("custom"))
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

            LaunchedEffect(mode) {
                val now = System.currentTimeMillis()
                when (mode) {
                    "today" -> viewModel.loadLatestDay()
                    "three_days" -> viewModel.loadFiles(DateUtils.getThreeDaysAgoStart(), now)
                    "custom" -> viewModel.loadFiles(startDate, endDate)
                    else -> viewModel.loadFiles()
                }
            }

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
            android.util.Log.d("NavGraph", "Preview: fileIndex=$fileIndex")
            LaunchedEffect(fileIndex) {
                viewModel.setInitialIndex(fileIndex)
            }
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
