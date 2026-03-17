package com.voiddevelopers.wastatussaver.presentation.ui.backup

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.voiddevelopers.wastatussaver.R
import com.voiddevelopers.wastatussaver.data.utils.extentions.singleClick
import com.voiddevelopers.wastatussaver.domain.model.BackupInterval
import com.voiddevelopers.wastatussaver.domain.model.MediaType
import com.voiddevelopers.wastatussaver.presentation.theme.WaStatusSaverTheme
import com.voiddevelopers.wastatussaver.presentation.ui.backup.dialog.SwitchAccountWarningDialog
import com.voiddevelopers.wastatussaver.presentation.ui.main.dialog.NotificationPermissionDialog
import com.voiddevelopers.wastatussaver.presentation.ui.main.dialog.NotificationPermissionSettingsDialog

@Composable
fun BackupScreen(
    onBack: () -> Unit,
) {

    val viewModel: BackUpViewModel = hiltViewModel()

    val state by viewModel.state.collectAsStateWithLifecycle()
    val primaryAlpha80 = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
    val intervals = BackupInterval.entries
    val mediaMediaTypes = listOf(
        MediaType.IMAGE, MediaType.VIDEO, MediaType.AUDIO
    )
    val context = LocalContext.current
    val activity = LocalActivity.current
    var notificationDialogAction by remember { mutableStateOf<BackupAction?>(null) }
    var showSavedMessage by remember { mutableStateOf<Boolean?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val signInLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                RESULT_OK -> {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    try {
                        val account = task.getResult(ApiException::class.java)
                        viewModel.onEvent(BackupEvent.SelectAccount(account?.email))
                    } catch (e: ApiException) {
                    }
                }
            }
        }

    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE)).build()

        GoogleSignIn.getClient(context, gso)
    }

    fun triggerSignIn() {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null) {
            viewModel.onEvent(BackupEvent.ShowAccountSwitchWarningDialog(true))
        } else {
            signInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.i("Surendhar TAG", "BackupScreen: $isGranted")
        if (!isGranted) {
            val shouldShowRationale =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    activity?.shouldShowRequestPermissionRationale(
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == true
                } else {
                    false
                }
            Log.i("Surendhar TAG", "BackupScreen: $shouldShowRationale")
            if (shouldShowRationale) {
                viewModel.onEvent(BackupEvent.ShowRationale)
            } else {
                viewModel.onEvent(BackupEvent.ShowNotificationSettingsDialog)
            }
        }
        viewModel.onEvent(BackupEvent.StartWork)
    }

    LaunchedEffect(Unit) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        viewModel.onEvent(BackupEvent.SelectAccount(account?.email))
    }

    LaunchedEffect(Unit) {
        viewModel.event.collect { action ->
            when (action) {
                is BackupAction.GoBack -> {
                    onBack()
                }

                BackupAction.RequestNotificationPermission -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.onEvent(BackupEvent.StartWork)
                    }
                }

                BackupAction.ShowNotificationPermissionSettingsDialog -> {
                    notificationDialogAction = action
                }

                BackupAction.ShowNotificationRationale -> {
                    notificationDialogAction = action
                }

                is BackupAction.ShowToast -> {
                    Toast.makeText(context, action.message, Toast.LENGTH_SHORT).show()
                }

                is BackupAction.ShowSavedMessage -> {
                    showSavedMessage = true
                }

            }
        }
    }


    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = Color.White
                )
            }
        },
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
                                .singleClick { onBack() })
                        Text(
                            text = "Backup",
                            modifier = Modifier.height(30.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
        }) { paddingValues ->

        if (notificationDialogAction == BackupAction.ShowNotificationRationale) {
            NotificationPermissionDialog(
                title = stringResource(R.string.backup_notification_title),
                bodyText = stringResource(R.string.backup_notification_body), onOkPressed = {
                    viewModel.onEvent(BackupEvent.RequestPermission)
                }, onDialogDismissed = {
                    notificationDialogAction = null
                })
        } else if (notificationDialogAction == BackupAction.ShowNotificationPermissionSettingsDialog) {
            NotificationPermissionSettingsDialog(onGoToSettingsPressed = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }, onCancelPressed = {
                notificationDialogAction = null
            })
        }

        LaunchedEffect(showSavedMessage) {
            if (showSavedMessage == true) {
                snackbarHostState.showSnackbar(
                    message = "Back Up Work Scheduled and will be Executed",
                    duration = SnackbarDuration.Short
                )
                showSavedMessage = null
            }
        }

        fun cancelDialog() {
            viewModel.onEvent(
                BackupEvent.ShowAccountSwitchWarningDialog(
                    false
                )
            )
        }

        if (state.showAccountSwitchWarningDialog) {
            SwitchAccountWarningDialog(
                onOkayPressed = {
                    GoogleSignIn.getClient(
                        context,
                        GoogleSignInOptions.DEFAULT_SIGN_IN
                    ).signOut().addOnCompleteListener {
                        viewModel.onEvent(BackupEvent.SelectAccount(null))
                        signInLauncher.launch(googleSignInClient.signInIntent)
                    }
                    cancelDialog()
                },
                onCancelPressed = { cancelDialog() }
            )
        }
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            BackupNote()

            BackupSettingsRow {
                Column {
                    Text(
                        text = "Turn on Backup", style = MaterialTheme.typography.titleMedium.copy(
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
                    onCheckedChange = { viewModel.onEvent(BackupEvent.ToggleBackup(it)) })
            }

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
                        .padding(horizontal = 8.dp)
                        .clickable {
                            triggerSignIn()
                        },
                    text = state.selectedGoogleAccount ?: "–",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = if (state.selectedGoogleAccount != null) MaterialTheme.colorScheme.primary
                    else Color(0xFF7E7D82)
                )
            }


            if (state.isBackupOn) {

                Text(
                    text = "Automatic Backups",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize * 0.7f)
                )

                ScrollableTabRow(
                    selectedTabIndex = intervals.indexOf(state.backupInterval),
                    edgePadding = 0.dp,
                    divider = {},
                    indicator = {}) {
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
                                1.dp, if (state.backupInterval == interval) Color.Transparent
                                else Color(0xFF7E7D82)
                            )
                        )
                    }
                }


            }

            Box(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    Text("Media Files to Save", style = MaterialTheme.typography.titleMedium)

                    mediaMediaTypes.forEach { fileType ->
                        CheckboxRow(
                            label = fileType.name.lowercase()
                                .replaceFirstChar { it.uppercase() },
                            checked = state.selectedMediaTypes.contains(fileType),
                            onCheckedChange = {
                                viewModel.onEvent(BackupEvent.ToggleMediaType(fileType))
                            })
                    }

                }

                Text(
                    text = "Upload Now",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            viewModel.onEvent(BackupEvent.SetWorkType(WorkType.EXPEDITED))
                            viewModel.onEvent(BackupEvent.RequestPermission)
                        },
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
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
                        onCheckedChange = { viewModel.onEvent(BackupEvent.SetCellularUsage(it)) })
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (state.selectedMediaTypes.isNotEmpty() || !state.isBackupOn) {
                        viewModel.onEvent(BackupEvent.SetWorkType(WorkType.PERIODIC))
                        if (state.isBackupOn) {
                            viewModel.onEvent(BackupEvent.RequestPermission)
                        }
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
fun BackupNote() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                shape = MaterialTheme.shapes.medium
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Note",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "• Backup runs silently in the background.",
            style = MaterialTheme.typography.bodySmall
        )

        Text(
            text = "• Only saved media files are eligible for backup — unsaved media will be skipped.",
            style = MaterialTheme.typography.bodySmall
        )

        Text(
            text = "• Backed up files are uploaded to the selected Google account.",
            style = MaterialTheme.typography.bodySmall
        )

        Text(
            text = "• If no media files are saved, nothing will be backed up.",
            style = MaterialTheme.typography.bodySmall
        )

        Text(
            text = "• Backup requires an active internet connection — make sure you're connected to the internet for backup to run at the scheduled interval.",
            style = MaterialTheme.typography.bodySmall
        )

        Text(
            text = "• If the selected Google account is not available, backup will be disabled automatically.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
@Preview
fun PreviewBackupScreen() {
    WaStatusSaverTheme {
        BackupScreen(onBack = {})
    }
}