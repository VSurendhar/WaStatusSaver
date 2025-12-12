package com.voidDeveloper.wastatussaver.presentation.ui.main.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.voidDeveloper.wastatussaver.R
import kotlinx.coroutines.launch

@Composable
fun NotificationPermissionDialog(onOkPressed: () -> Unit, onDialogDismissed: () -> Unit) {
    var openDialog by remember { mutableStateOf(true) }
    if (openDialog) {
        Dialog(
            onDismissRequest = { openDialog = false },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OnBoardingBody(
                    imageRes = R.drawable.ic_notfication_permission,
                    title = "Auto-Save Notifications",
                    bodyText = "We'd like to notify you when a status is auto-saved successfully or fails due to an issue."
                )
                Text(
                    text = "Okay",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 12.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            openDialog = false
                            onOkPressed()
                        },
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    } else {
        onDialogDismissed()
    }
}