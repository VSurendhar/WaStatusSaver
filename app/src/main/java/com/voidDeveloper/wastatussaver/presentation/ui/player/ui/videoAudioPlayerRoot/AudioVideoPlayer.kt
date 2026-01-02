package com.voidDeveloper.wastatussaver.presentation.ui.player.ui.videoAudioPlayerRoot

import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.voidDeveloper.wastatussaver.R
import com.voidDeveloper.wastatussaver.data.datastore.proto.MediaType
import com.voidDeveloper.wastatussaver.domain.model.toMediaFile
import com.voidDeveloper.wastatussaver.presentation.receivers.NoisyAudioReceiver
import com.voidDeveloper.wastatussaver.presentation.ui.player.components.CustomVideoControls
import com.voidDeveloper.wastatussaver.presentation.ui.player.ui.component.ActionButton
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun AudioVideoPlayer(intent: Intent) {

    val viewModel = hiltViewModel<AudioVideoPlayerViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        viewModel.handleIntent(intent)
    }

    DisposableEffect(Unit) {
        val receiver = NoisyAudioReceiver(onNoisy = {
            viewModel.pause()
        })
        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        context.registerReceiver(receiver, filter)
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    var lifecycle by remember {
        mutableStateOf(Lifecycle.Event.ON_CREATE)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            lifecycle = event
            when (event) {

                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.persistPlaybackPosition()
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(state.isPlaying) {
        if (state.isPlaying) {
            while (true) {
                viewModel.updateProgress()
                delay(500)
            }
        }
    }

    LaunchedEffect(state.mediaInfo.uri) {
        if (state.mediaInfo.uri.isNotEmpty()) {
            viewModel.setMediaItem()
            viewModel.updateProgress()
        }
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
                        .padding(12.dp), verticalAlignment = Alignment.CenterVertically
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
            val TAG = "MediaViewer"
            if (state.mediaInfo.mediaType == MediaType.AUDIO) {

                AudioViewer(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }) {
                            viewModel.toggleControls()
                        },
                    onPause = {
                        viewModel.pause()
                        viewModel.persistPlaybackPosition()
                    },
                )
            } else {
                Log.d(TAG, "Rendering VIDEO viewer")

                AndroidView(
                    factory = { context ->
                        Log.d(TAG, "Creating PlayerView")

                        PlayerView(context).also {
                            it.player = viewModel.player
                            it.useController = false
                            it.keepScreenOn = true
                            it.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    }, update = { playerView ->
                        when (lifecycle) {

                            Lifecycle.Event.ON_PAUSE -> {
                                Log.d(
                                    TAG, "Lifecycle ON_PAUSE → pause player & persist position"
                                )
                                playerView.onPause()
                                playerView.player?.pause()
                                viewModel.persistPlaybackPosition()
                            }

                            Lifecycle.Event.ON_RESUME -> {
                                Log.d(TAG, "Lifecycle ON_RESUME → resume PlayerView")
                                playerView.onResume()
                            }

                            else -> Unit
                        }
                    }, modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }) {
                            viewModel.toggleControls()
                        }
                        .padding(top = 12.dp))
            }
            if (state.showControls && (state.mediaInfo.mediaType != MediaType.IMAGE)) {
                CustomVideoControls(
                    isPlaying = state.isPlaying,
                    currentPosition = state.progressPercent,
                    duration = state.durationMs,
                    onPlayPauseClick = {
                        Log.i(
                            "Play Status", "onCreate: ${viewModel.isPlayerPlaying()}"
                        )
                        if (state.isPlaying) {
                            viewModel.pause()
                        } else {
                            if (state.mediaEnded) {
                                viewModel.playerSeekTo(0)
                            }
                            viewModel.play()
                        }
                    },
                    onSeek = { position ->
                        val value = viewModel.getPlayerDuration() * position / 100
                        viewModel.playerSeekTo(value)
                        viewModel.updateProgress()
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }

}

@Composable
fun AudioViewer(
    modifier: Modifier = Modifier,
    onPause: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    onPause()
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    Box(
        modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_audio_preview),
            contentDescription = "Audio Preview",
            modifier = Modifier.size(200.dp),
        )
    }
}
