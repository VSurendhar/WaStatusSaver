package com.voidDeveloper.wastatussaver.presentation.ui.main.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.voidDeveloper.wastatussaver.R

@Composable
fun AutoSaveDialog(
    selectedInterval: Int = 1,
    autoSaveEnable: Boolean = false,
    onApplyPressed: (Boolean, Int) -> Unit,
    onAutoSaveDialogDismissPressed: () -> Unit,
) {
    var openDialog by remember { mutableStateOf(true) }
    var autoSaveEnabled by remember { mutableStateOf(autoSaveEnable) }
    val intervals = listOf(1, 6, 12, 24)
    var selectedInterval by remember { mutableStateOf(selectedInterval) }

    if (openDialog) {
        Dialog(
            onDismissRequest = {
                onAutoSaveDialogDismissPressed()
                openDialog = false
            },
            properties = DialogProperties(
                dismissOnBackPress = true, dismissOnClickOutside = true
            ),
        ) {
            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Icon(
                    modifier = Modifier
                        .size(130.dp)
                        .padding(horizontal = 12.dp),
                    painter = painterResource(R.drawable.ic_auto_save),
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = "Auto Save Image",
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Autosave",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    modifier = Modifier.padding(horizontal = 6.dp),
                    text = "Automatically saves your statuses at the interval you choose.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Left,
                    color = Color.Gray,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enable Autosave",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )

                    Switch(
                        checked = autoSaveEnabled,
                        onCheckedChange = { autoSaveEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.primary,
                            uncheckedTrackColor = MaterialTheme.colorScheme.onSecondary,
                            uncheckedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    text = "Select Autosave Interval",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (autoSaveEnabled) Color.Black else Color.Gray
                )

                Spacer(modifier = Modifier.height(12.dp))

                var expanded by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (autoSaveEnabled) Color.Gray else Color.LightGray,
                            RoundedCornerShape(8.dp)
                        )
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = autoSaveEnabled) { expanded = true }
                        .padding(12.dp)
                ) {
                    Text(
                        text = constructInterval(selectedInterval),
                        color = if (autoSaveEnabled) Color.Black else Color.Gray
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        intervals.forEach { interval ->
                            DropdownMenuItem(
                                text = { Text(constructInterval(interval)) },
                                onClick = {
                                    selectedInterval = interval
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        openDialog = false
                        onApplyPressed(autoSaveEnabled, selectedInterval)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply")
                }
            }
        }
    }
}

private fun constructInterval(interval: Int): String {
    return if (interval == 1) {
        "$interval hour"
    } else {
        "$interval hours"
    }
}
