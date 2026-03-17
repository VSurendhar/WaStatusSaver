package com.voiddevelopers.wastatussaver.presentation.ui.quicksave

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voiddevelopers.wastatussaver.data.utils.Constants.ACTION_FS_QUICK_SAVE_STOP
import com.voiddevelopers.wastatussaver.data.utils.extentions.singleClick
import com.voiddevelopers.wastatussaver.domain.model.MediaType
import com.voiddevelopers.wastatussaver.presentation.theme.WaStatusSaverTheme
import com.voiddevelopers.wastatussaver.presentation.ui.main.dialog.NotificationPermissionDialog
import com.voiddevelopers.wastatussaver.presentation.ui.main.dialog.NotificationPermissionSettingsDialog
import com.voiddevelopers.wastatussaver.presentation.service.QuickSaveNotificationService
import com.voiddevelopers.wastatussaver.presentation.ui.main.ui.Title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSaveSettingsScreen(onBack: () -> Unit) {

    val activity = LocalActivity.current
    val context = LocalContext.current
    val viewModel = hiltViewModel<QuickSaveSettingsViewModel>()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val appTitles = listOf(
        Title.Whatsapp, Title.WhatsappBusiness
    )

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            val shouldShowRationale =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        activity?.shouldShowRequestPermissionRationale(
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == true
                } else {
                    false
                }
            if (shouldShowRationale) {
                viewModel.onEvent(QuickSaveEvent.ShowRationale)
            }else{
                viewModel.onEvent(QuickSaveEvent.ShowNotificationSettingsDialog)
            }
        } else {
            viewModel.onEvent(QuickSaveEvent.SaveIfPossibleAndBack)
        }
    }

    val mediaMediaTypes = listOf(
        MediaType.IMAGE, MediaType.VIDEO, MediaType.AUDIO
    )

    var notificationDialogAction by remember { mutableStateOf<QuickSaveAction?>(null) }

    val primaryAlpha80 = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)

    LaunchedEffect(Unit) {
        viewModel.effect.collect { action ->
            when (action) {
                is QuickSaveAction.GoBack -> {
                    if (action.startForegroundService) {
                        val intent = Intent(context, QuickSaveNotificationService::class.java)
                        ContextCompat.startForegroundService(context, intent)
                    }
                    onBack()
                }

                QuickSaveAction.RequestNotificationPermission -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                QuickSaveAction.ShowNotificationPermissionSettingsDialog -> {
                    notificationDialogAction = action
                }

                QuickSaveAction.ShowNotificationRationale -> {
                    notificationDialogAction = action
                }

                is QuickSaveAction.ShowToast -> {
                    Toast.makeText(context, action.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary), topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .statusBarsPadding()
                    .wrapContentHeight(),
                color = MaterialTheme.colorScheme.primary
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier
                                .height(30.dp)
                                .padding(end = 12.dp)
                                .singleClick {
                                    onBack()
                                })
                        Text(
                            text = "Quick Save Settings",
                            modifier = Modifier.height(30.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
        }) { paddingValues ->
        if (notificationDialogAction == QuickSaveAction.ShowNotificationRationale) {
            NotificationPermissionDialog(onOkPressed = {
                viewModel.onEvent(QuickSaveEvent.RequestPermission)
            }, onDialogDismissed = {
                notificationDialogAction = null
            })
        } else if (notificationDialogAction == QuickSaveAction.ShowNotificationPermissionSettingsDialog) {
            NotificationPermissionSettingsDialog(onGoToSettingsPressed = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }, onCancelPressed = {
                notificationDialogAction = null
            })
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SettingsRow {
                Column {
                    Text(
                        text = "Enable Quick-save",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = MaterialTheme.typography.titleLarge.fontSize * 0.8f
                        )
                    )
                    Text(
                        "Automatically download status updates",
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
                    ), checked = uiState.isQuickSaveEnabled, onCheckedChange = {
                        viewModel.onEvent(QuickSaveEvent.OnQuickSaveSettingsChanged(isEnabled = it))
                    })
            }
            if (uiState.isQuickSaveEnabled) {
                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Application Data Source", style = MaterialTheme.typography.titleMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        appTitles.forEach { title ->
                            AppFilterChip(
                                label = stringResource(id = title.resId),
                                isSelected = uiState.selectedTitle == title,
                                onClick = {
                                    viewModel.onEvent(
                                        QuickSaveEvent.OnQuickSaveSettingsChanged(
                                            title = title
                                        )
                                    )
                                })
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Media Files to Save", style = MaterialTheme.typography.titleMedium)

                    mediaMediaTypes.forEach { fileType ->
                        CheckboxRow(
                            label = fileType.name.lowercase().replaceFirstChar { it.uppercase() },
                            checked = uiState.selectedMediaTypes.contains(fileType),
                            onCheckedChange = {
                                viewModel.onEvent(
                                    QuickSaveEvent.OnQuickSaveSettingsChanged(
                                        fileType = Pair(fileType, it)
                                    )
                                )
                            })
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_DENIED
                    ) {
                        viewModel.onEvent(QuickSaveEvent.RequestPermission)
                    } else {
                        viewModel.onEvent(QuickSaveEvent.SaveIfPossibleAndBack)
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
fun SettingsRow(content: @Composable RowScope.() -> Unit) {
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
                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                disabledCheckedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
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
fun AppFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = isSelected, onClick = onClick, label = {
            Text(
                text = label, color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    Color(0xFF1C1C1C)
                }
            )
        }, colors = if (isSelected) {
            FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
        } else {
            FilterChipDefaults.filterChipColors(
                containerColor = Color.White
            )
        }, border = BorderStroke(
            1.dp, if (isSelected) Color.Transparent else Color(0xFF7E7D82)
        )
    )
}

@Composable
@Preview
fun PreviewQuickSaveSettingsScreen() {
    WaStatusSaverTheme {
        QuickSaveSettingsScreen {}
    }
}