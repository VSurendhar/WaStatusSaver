package com.voidDeveloper.wastatussaver.ui.main.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.voidDeveloper.wastatussaver.R
import com.voidDeveloper.wastatussaver.data.utils.createColoredString
import com.voidDeveloper.wastatussaver.ui.main.Title
import com.voidDeveloper.wastatussaver.ui.theme.WaStatusSaverTheme

@Composable
fun AppNotInstalledDialog(title: Title, onDownloadApp: () -> Unit, onNotNowPressed: () -> Unit) {
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
                Text(
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp),
                    text = stringResource(R.string.app_not_installed_title)
                )
                val str =
                    if (title == Title.Whatsapp) stringResource(R.string.app_not_installed_whatsapp_body) else stringResource(
                        R.string.app_not_installed_whatsapp_business_body
                    )
                Text(
                    text = createColoredString(
                        baseString = str, color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp,
                    style = MaterialTheme.typography.bodyLarge
                )
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    onClick = {
                        onDownloadApp()
                        openDialog = false
                    }) {
                    Text(
                        text = stringResource(R.string.download, stringResource(title.resId)),
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                }
                Text(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                onNotNowPressed()
                                openDialog = false
                            }
                        )
                        .padding(12.dp),
                    text = stringResource(R.string.not_now),
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
            }
        }
    }

}


@Composable
@Preview()
fun PreviewAppNotInstalledDialog() {
    WaStatusSaverTheme {
        AppNotInstalledDialog(title = Title.Whatsapp, {}, {})
    }
}