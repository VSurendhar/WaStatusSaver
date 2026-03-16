package com.voiddevelopers.wastatussaver.presentation.ui.backup

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voiddevelopers.wastatussaver.data.utils.extentions.singleClick
import com.voiddevelopers.wastatussaver.domain.model.BackupInterval
import com.voiddevelopers.wastatussaver.domain.model.MediaType
import com.voiddevelopers.wastatussaver.presentation.theme.WaStatusSaverTheme
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun BackupScreen(
    onBackClick: () -> Unit,
) {

    val viewModel: BackUpViewModel = hiltViewModel()

    val state by viewModel.state.collectAsStateWithLifecycle()
    val primaryAlpha80 = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
    val intervals = BackupInterval.entries
    val mediaMediaTypes = listOf(
        MediaType.IMAGE, MediaType.VIDEO, MediaType.AUDIO
    )
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .statusBarsPadding()
                    .wrapContentHeight(),
                color = MaterialTheme.colorScheme.primary
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(12.dp)
                                .height(30.dp)
                                .singleClick { onBackClick() }
                        )
                        Text(
                            text = "Backup",
                            modifier = Modifier.height(30.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ── 1. Turn on Backup toggle ───────────────────────────────────
            BackupSettingsRow {
                Column {
                    Text(
                        text = "Turn on Backup",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = MaterialTheme.typography.titleLarge.fontSize * 0.8f
                        )
                    )
                    Text(
                        text = "Back up the media files to Google Drive",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Switch(
                    colors = SwitchDefaults.colors().copy(
                        uncheckedIconColor = primaryAlpha80,
                        uncheckedTrackColor = Color.White,
                        uncheckedThumbColor = primaryAlpha80,
                        uncheckedBorderColor = primaryAlpha80,
                        checkedTrackColor = primaryAlpha80,
                        checkedBorderColor = primaryAlpha80,
                    ),
                    checked = state.isBackupOn,
                    onCheckedChange = { viewModel.onEvent(BackupEvent.ToggleBackup(it)) }
                )
            }

            // ── 2. Selected Google Account row ────────────────────────────
            BackupSettingsRow {
                Text(
                    text = "Selected Google Account",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = MaterialTheme.typography.titleLarge.fontSize * 0.7f
                    )
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    text = state.selectedGoogleAccount ?: "–",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = if (state.selectedGoogleAccount != null)
                        MaterialTheme.colorScheme.primary
                    else
                        Color(0xFF7E7D82)
                )
            }

            // ── 3. Automatic Backups section label ────────────────────────

            if (state.isBackupOn) {

                Text(
                    text = "Automatic Backups",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize * 0.7f)
                )

                ScrollableTabRow(
                    selectedTabIndex = intervals.indexOf(state.backupInterval),
                    edgePadding = 0.dp,
                    divider = {},
                    indicator = {}
                ) {
                    intervals.forEach { interval ->
                        SuggestionChip(
                            onClick = { viewModel.onEvent(BackupEvent.ChangeInterval(interval)) },
                            label = {
                                Text(
                                    text = interval.label,
                                    color = if (state.backupInterval == interval) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        Color(0xFF1C1C1C)
                                    }
                                )
                            },
                            modifier = Modifier.padding(horizontal = 4.dp),
                            colors = if (state.backupInterval == interval) {
                                SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(
                                        alpha = 0.8f
                                    )
                                )
                            } else {
                                SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = Color.White
                                )
                            },
                            border = BorderStroke(
                                1.dp,
                                if (state.backupInterval == interval)
                                    Color.Transparent
                                else
                                    Color(0xFF7E7D82)
                            )
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    Text("Media Files to Save", style = MaterialTheme.typography.titleMedium)

                    mediaMediaTypes.forEach { fileType ->
                        CheckboxRow(
                            label = fileType.name.lowercase().replaceFirstChar { it.uppercase() },
                            checked = state.selectedMediaTypes.contains(fileType),
                            onCheckedChange = {
                                viewModel.onEvent(BackupEvent.ToggleMediaType(fileType))
                            })
                    }

                }

            }

            if (state.isBackupOn) {
                BackupSettingsRow {
                    Column {
                        Text(
                            text = "Backup using cellular",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize * 0.7f)
                        )
                        Text(
                            text = "Use mobile data to back up",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize * 0.6f)
                        )
                    }
                    Switch(
                        colors = SwitchDefaults.colors().copy(
                            uncheckedIconColor = primaryAlpha80,
                            uncheckedTrackColor = Color.White,
                            uncheckedThumbColor = primaryAlpha80,
                            uncheckedBorderColor = primaryAlpha80,
                            checkedTrackColor = primaryAlpha80,
                            checkedBorderColor = primaryAlpha80,
                        ),
                        checked = state.canUseCellular,
                        onCheckedChange = { viewModel.onEvent(BackupEvent.SetCellularUsage(it)) }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (state.selectedMediaTypes.isNotEmpty() || !state.isBackupOn) {
                        viewModel.onEvent(BackupEvent.SaveClicked)
                    } else {
                        Toast.makeText(
                            context, "Please select at least one media type", Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save Configuration")
            }

        }
    }
}


@Composable
fun BackupSettingsRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun CheckboxRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked, onCheckedChange = onCheckedChange, colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}


@Composable
@Preview
fun PreviewBackupScreen() {
    WaStatusSaverTheme {
        BackupScreen(onBackClick = {})
    }
}