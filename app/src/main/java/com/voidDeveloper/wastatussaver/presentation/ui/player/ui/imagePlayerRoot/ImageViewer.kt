package com.voidDeveloper.wastatussaver.presentation.ui.player.ui.imagePlayerRoot

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.voidDeveloper.wastatussaver.data.datastore.proto.MediaType
import com.voidDeveloper.wastatussaver.domain.model.toMediaFile
import com.voidDeveloper.wastatussaver.presentation.ui.player.ui.component.ActionButton
import com.voidDeveloper.wastatussaver.presentation.ui.player.ui.videoAudioPlayerRoot.DownloadState

@Composable
fun ImageViewer(
    intent: Intent
) {

    val viewModel = hiltViewModel<ImageViewerViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.handleIntent(intent)
    }

    Scaffold(
        modifier = Modifier
            .background(Color.Black)
            .fillMaxSize()
            .systemBarsPadding()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .background(Color.Black)
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (state.showControls) {
                Row(
                    modifier = Modifier
                        .zIndex(1f)
                        .fillMaxWidth()
                        .background(Color.Black.copy(0.7f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.mediaInfo.fileName,
                        color = Color.White,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    ActionButton(downloadStatus = state.downloadState, onSave = {
                        viewModel.onDownloadClick(state.mediaInfo.toMediaFile())
                    }, onRepost = {
                        val mediaFolder = when (state.mediaInfo.mediaType) {
                            MediaType.AUDIO -> "Audio"
                            MediaType.IMAGE -> "Image"
                            MediaType.VIDEO -> "Video"
                            else -> "*"
                        }
                        val uri = if (state.downloadState == DownloadState.DOWNLOADED) {
                            viewModel.getMediaUriFromDownloads(
                                fileName = state.mediaInfo.fileName,
                                mediaFolder = mediaFolder
                            )
                        } else {
                            viewModel.saveCache(
                                uri = state.mediaInfo.uri,
                                context = context,
                                mediaType = state.mediaInfo.mediaType
                            )
                        }
                        val mediaType = when (state.mediaInfo.mediaType) {
                            MediaType.AUDIO -> "audio/*"
                            MediaType.IMAGE -> "jpg/*"
                            MediaType.VIDEO -> "video/*"
                            else -> "*"
                        }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = mediaType
                            putExtra(Intent.EXTRA_STREAM, uri)
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                        context.startActivity(
                            Intent.createChooser(
                                intent, "Choose app to share this"
                            )
                        )
                    })
                }
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = state.mediaInfo.uri,
                    contentDescription = "Image Preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
