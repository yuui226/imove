package io.github.imove.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import io.github.imove.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val preferences by viewModel.preferences.collectAsState()

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

            // Target directory
            ListItem(
                headlineContent = { Text("目标目录") },
                supportingContent = { Text(preferences.targetDirectory) }
            )
        }
    }
}
