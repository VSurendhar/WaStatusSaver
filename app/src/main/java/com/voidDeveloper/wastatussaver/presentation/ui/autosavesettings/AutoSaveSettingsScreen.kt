package com.voidDeveloper.wastatussaver.presentation.ui.autosavesettings

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voidDeveloper.wastatussaver.data.datastore.proto.MediaType
import com.voidDeveloper.wastatussaver.data.utils.extentions.safeAutoSaveIntervalDomain
import com.voidDeveloper.wastatussaver.data.utils.extentions.singleClick
import com.voidDeveloper.wastatussaver.data.utils.extentions.toApp
import com.voidDeveloper.wastatussaver.data.utils.extentions.toAutoSaveIntervalDomain
import com.voidDeveloper.wastatussaver.data.utils.extentions.toTitle
import com.voidDeveloper.wastatussaver.domain.model.AutoSaveIntervalDomain
import com.voidDeveloper.wastatussaver.presentation.theme.WaStatusSaverTheme
import com.voidDeveloper.wastatussaver.presentation.ui.main.ui.Title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoSaveSettingsScreen(onBackClick: () -> Unit) {

    val viewModel = hiltViewModel<AutoSaveSettingsViewModel>()
    var isAutoSaveEnabled by remember { mutableStateOf(false) }
    var selectedTitle by remember { mutableStateOf<Title>(Title.Whatsapp) }
    var selectedInterval by remember {
        mutableStateOf(AutoSaveIntervalDomain.TWENTY_FOUR_HOURS)
    }

    val mediaMediaTypes = listOf(
        MediaType.IMAGE, MediaType.VIDEO, MediaType.AUDIO
    )

    var selectedMediaTypes by remember {
        mutableStateOf(
            setOf(
                MediaType.IMAGE, MediaType.VIDEO, MediaType.AUDIO
            )
        )
    }

    val appTitles = listOf(
        Title.Whatsapp, Title.WhatsappBusiness
    )

    val intervals = AutoSaveIntervalDomain.entries
    val primaryAlpha80 = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
    val toastInfo by viewModel.toastInfoChannel.collectAsStateWithLifecycle(null)
    val saved by viewModel.saved.collectAsStateWithLifecycle(false)
    val previousAutoSavePref by viewModel.previousAutoSavePref.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(toastInfo) {
        if (toastInfo != null) {
            Toast.makeText(context, toastInfo, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(previousAutoSavePref) {
        if (previousAutoSavePref != null) {
            isAutoSaveEnabled = previousAutoSavePref!!.enable
            selectedTitle = previousAutoSavePref!!.app.toTitle()
            selectedInterval = previousAutoSavePref!!.autoSaveInterval.toAutoSaveIntervalDomain()
                .safeAutoSaveIntervalDomain()
            selectedMediaTypes = previousAutoSavePref!!
                .mediaTypeList
                .toSet()
        }
    }


    LaunchedEffect(saved) {
        if (saved) {
            onBackClick()
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
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(12.dp)
                                .height(30.dp)
                                .singleClick {
                                    onBackClick()
                                }
                        )
                        Text(
                            text = "Auto Save Settings",
                            modifier = Modifier.height(30.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
        }) { paddingValues ->
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
                        text = "Enable Auto-save",
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
                    ), checked = isAutoSaveEnabled, onCheckedChange = { isAutoSaveEnabled = it })
            }

            if (isAutoSaveEnabled) {
                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Application Data Source", style = MaterialTheme.typography.titleMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        appTitles.forEach { title ->
                            AppFilterChip(
                                label = stringResource(id = title.resId),
                                isSelected = selectedTitle == title,
                                onClick = { selectedTitle = title })
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Auto-save Interval", style = MaterialTheme.typography.titleMedium)
                    ScrollableTabRow(
                        selectedTabIndex = intervals.indexOf(selectedInterval),
                        edgePadding = 0.dp,
                        divider = {},
                        indicator = {}) {
                        intervals.forEach { interval ->
                            SuggestionChip(
                                onClick = { selectedInterval = interval },
                                label = {
                                    Text(
                                        text = interval.label,
                                        color = if (selectedInterval == interval) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            Color(0xFF1C1C1C)
                                        }
                                    )
                                },
                                modifier = Modifier.padding(horizontal = 4.dp),
                                colors = if (selectedInterval == interval) {
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
                                    1.dp, if (selectedInterval == interval) {
                                        Color.Transparent
                                    } else {
                                        Color(0xFF7E7D82)
                                    }
                                )
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Media Files to Save", style = MaterialTheme.typography.titleMedium)

                    mediaMediaTypes.forEach { fileType ->
                        CheckboxRow(
                            label = fileType.name.lowercase().replaceFirstChar { it.uppercase() },
                            checked = selectedMediaTypes.contains(fileType),
                            onCheckedChange = { isChecked ->
                                selectedMediaTypes = if (isChecked) {
                                    selectedMediaTypes + fileType
                                } else {
                                    selectedMediaTypes - fileType
                                }
                            })
                    }
                }

            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (selectedMediaTypes.isNotEmpty() || !isAutoSaveEnabled) {
                        viewModel.saveAutoSaveUserPref(
                            enable = isAutoSaveEnabled,
                            scheduledInterval = selectedInterval.hours,
                            selectedMediaTypes = selectedMediaTypes.toList(),
                            selectedTitle = selectedTitle.toApp()
                        )
                    } else {
                        Toast.makeText(
                            context,
                            "Please select at least one media type",
                            Toast.LENGTH_SHORT
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
fun PreviewAutoSaveSettingsScreen() {
    WaStatusSaverTheme {
        AutoSaveSettingsScreen({})
    }
}