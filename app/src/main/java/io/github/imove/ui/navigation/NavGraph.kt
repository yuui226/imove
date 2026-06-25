package io.github.imove.ui.navigation

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import io.github.imove.data.usb.StorageAccessManager
import io.github.imove.ui.home.HomeScreen
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
            val uiState by viewModel.uiState.collectAsState()
            val isDetecting by viewModel.isDetecting.collectAsState()

            val context = LocalContext.current

            val directoryPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.data?.let { uri ->
                        viewModel.saveSourcePath(uri)
                    }
                }
            }

            val targetDirPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.data?.let { uri ->
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        viewModel.updateTargetDirectory(uri.toString())
                    }
                }
            }

            HomeScreen(
                state = uiState,
                isDetecting = isDetecting,
                onSelectSourceDirectory = {
                    val intent = storageAccessManager.createOpenDocumentTreeIntent()
                    directoryPickerLauncher.launch(intent)
                },
                onSetTargetDirectory = {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    }
                    targetDirPickerLauncher.launch(intent)
                },
                onTransferToday = {
                    navController.navigate(Screen.Transfer.createRoute("today"))
                },
                onTransferThreeDays = {
                    navController.navigate(Screen.Transfer.createRoute("three_days"))
                },
                onTransferTenDays = {
                    navController.navigate(Screen.Transfer.createRoute("ten_days"))
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
                navArgument("mode") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val viewModel: TransferViewModel = hiltViewModel()
            val mode = backStackEntry.arguments?.getString("mode") ?: "all"

            LaunchedEffect(mode) {
                val displayMode = when (mode) {
                    "today" -> "latest_day"
                    "three_days" -> "three_days"
                    "ten_days" -> "ten_days"
                    else -> "all"
                }
                viewModel.setDisplayMode(displayMode)
                viewModel.loadAllFiles()
            }

            TransferScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onPreview = { fileId ->
                    navController.navigate(Screen.Preview.createRoute(fileId))
                }
            )
        }
        composable(
            route = Screen.Preview.route,
            arguments = listOf(
                navArgument("fileId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val viewModel: PreviewViewModel = hiltViewModel()
            val fileId = android.net.Uri.decode(backStackEntry.arguments?.getString("fileId") ?: "")
            LaunchedEffect(fileId) {
                viewModel.setInitialFile(fileId)
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
