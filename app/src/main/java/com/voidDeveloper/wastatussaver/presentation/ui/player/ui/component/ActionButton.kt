package com.voidDeveloper.wastatussaver.presentation.ui.player.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.voidDeveloper.wastatussaver.R
import com.voidDeveloper.wastatussaver.presentation.ui.player.ui.videoAudioPlayerRoot.DownloadState


@Composable
    fun ActionButton(
        onSave: () -> Unit,
        onRepost: () -> Unit,
        downloadStatus: DownloadState,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black.copy(alpha = 0.65f))
                .animateContentSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            when (downloadStatus) {

                DownloadState.NOT_DOWNLOADED -> {
                    Icon(
                        painter = painterResource(R.drawable.ic_download),
                        contentDescription = "Download",
                        tint = Color.White,
                        modifier = Modifier
                            .size(25.dp)
                            .padding(bottom = 2.dp)
                            .clickable { onSave() })
                }

                DownloadState.DOWNLOADING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(25.dp), color = Color.White, strokeWidth = 2.dp
                    )
                }

                DownloadState.DOWNLOADED -> {
                    Icon(
                        painter = painterResource(R.drawable.ic_tick),
                        contentDescription = "Finished Download",
                        modifier = Modifier.size(25.dp),
                        tint = Color.White
                    )
                }

            }

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share",
                tint = Color.White,
                modifier = Modifier
                    .size(30.dp)
                    .clickable { onRepost() })

        }
    }
