package io.github.imove.ui.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.github.imove.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val preferences by viewModel.preferences.collectAsState()

    val targetDirLauncher = rememberLauncherForActivityResult(
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Target directory
            ListItem(
                headlineContent = { Text("保存位置") },
                supportingContent = {
                    Text(
                        text = if (preferences.targetDirectory.isBlank()) "未设置"
                        else preferences.targetDirectory.substringAfterLast('/').ifBlank { preferences.targetDirectory },
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                trailingContent = {
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        }
                        targetDirLauncher.launch(intent)
                    }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "选择目录")
                    }
                }
            )

            // Grid columns
            ListItem(
                headlineContent = { Text("网格列数") },
                supportingContent = {
                    SingleChoiceSegmentedButtonRow {
                        listOf(1, 2, 3, 4).forEachIndexed { index, cols ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = 4),
                                onClick = { viewModel.updateGridColumns(cols) },
                                selected = preferences.gridColumns == cols
                            ) {
                                Text("$cols")
                            }
                        }
                    }
                }
            )

            // Language
            ListItem(
                headlineContent = { Text("语言") },
                supportingContent = {
                    SingleChoiceSegmentedButtonRow {
                        val options = listOf("system" to "系统", "zh" to "中文", "en" to "EN")
                        options.forEachIndexed { index, (value, label) ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                                onClick = { viewModel.updateLanguage(value) },
                                selected = preferences.language == value
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
            )

            // Dark mode
            ListItem(
                headlineContent = { Text("深色模式") },
                supportingContent = {
                    SingleChoiceSegmentedButtonRow {
                        val options = listOf("system" to "系统", "light" to "浅色", "dark" to "深色")
                        options.forEachIndexed { index, (value, label) ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                                onClick = { viewModel.updateDarkMode(value) },
                                selected = preferences.darkMode == value
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
            )
        }
    }
}
