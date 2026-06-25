package io.github.imove.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import io.github.imove.R
import io.github.imove.domain.model.StorageDevice
import io.github.imove.ui.components.TransferModeCard
import io.github.imove.ui.util.readableTreePath

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    connectedDevice: StorageDevice?,
    isDetecting: Boolean,
    isRestoring: Boolean,
    targetDirectory: String,
    onSelectSourceDirectory: () -> Unit,
    onSetTargetDirectory: () -> Unit,
    onTransferToday: () -> Unit,
    onTransferThreeDays: () -> Unit,
    onTransferTenDays: () -> Unit,
    onTransferCustom: () -> Unit,
    onSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("iMove") },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                }
            )
        }
    ) { padding ->
        val device = connectedDevice

        if (device != null && !device.isLocal && device.sourcePath.isEmpty() && isRestoring) {
            // USB device connected, restoring its saved configuration
            CenteredColumn(padding) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.restoring_config),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (device != null && !device.isLocal && device.sourcePath.isEmpty()) {
            // USB device connected, pick the photo/video directory on the device
            CenteredColumn(padding) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "${stringResource(R.string.connected)}: ${device.volumeLabel}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.select_photo_dir),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onSelectSourceDirectory) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.select_directory))
                }
            }
        } else if (device != null && device.sourcePath.isNotEmpty() && targetDirectory.isNotEmpty()) {
            // Source + save folder both ready -> the card grid
            val sourceLabel = if (device.isLocal) {
                readableTreePath(device.sourcePath, stringResource(R.string.internal_storage))
            } else {
                device.volumeLabel
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        TransferModeCard(
                            icon = Icons.Default.CalendarToday,
                            label = stringResource(R.string.transfer_today),
                            onClick = onTransferToday
                        )
                    }
                    item {
                        TransferModeCard(
                            icon = Icons.Default.DateRange,
                            label = stringResource(R.string.transfer_three_days),
                            onClick = onTransferThreeDays
                        )
                    }
                    item {
                        TransferModeCard(
                            icon = Icons.Default.DateRange,
                            label = stringResource(R.string.transfer_ten_days),
                            onClick = onTransferTenDays
                        )
                    }
                    item {
                        TransferModeCard(
                            icon = Icons.Default.FolderOpen,
                            label = stringResource(R.string.transfer_custom),
                            onClick = onTransferCustom
                        )
                    }
                }
                // Source directory info + change button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = sourceLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onSelectSourceDirectory) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.change_directory))
                    }
                }
            }
        } else if (device != null && !device.isLocal && targetDirectory.isEmpty()) {
            // USB source set, only the save folder is still missing
            CenteredColumn(padding) {
                Button(onClick = onSetTargetDirectory) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.select_save_directory))
                }
            }
        } else {
            // No USB device: unified setup screen (pick a local source and/or a save folder)
            CenteredColumn(padding) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(colorResource(R.color.ic_launcher_background)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))

                val groupWidth = 240.dp

                // Group 1: pick a source — insert a USB device OR choose a local folder
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.width(groupWidth)
                ) {
                    if (isDetecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Usb, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.connect_device))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.or_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onSelectSourceDirectory,
                    modifier = Modifier.width(groupWidth)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.select_local_directory))
                }
                if (device != null && device.isLocal) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = readableTreePath(device.sourcePath, stringResource(R.string.internal_storage)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))
                HorizontalDivider(modifier = Modifier.width(groupWidth))
                Spacer(modifier = Modifier.height(28.dp))

                // Group 2: pick where files are saved
                Button(
                    onClick = onSetTargetDirectory,
                    modifier = Modifier.width(groupWidth)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.select_save_directory))
                }
                if (targetDirectory.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = readableTreePath(targetDirectory, stringResource(R.string.internal_storage)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CenteredColumn(
    padding: PaddingValues,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content
    )
}
