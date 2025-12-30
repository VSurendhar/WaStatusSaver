package com.voidDeveloper.wastatussaver.presentation.ui.player.ui

import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import coil.compose.AsyncImage
import com.voidDeveloper.wastatussaver.R
import com.voidDeveloper.wastatussaver.domain.model.toMediaFile
import com.voidDeveloper.wastatussaver.presentation.receivers.NoisyAudioReceiver
import com.voidDeveloper.wastatussaver.presentation.theme.WaStatusSaverTheme
import com.voidDeveloper.wastatussaver.presentation.ui.main.ui.FileType
import com.voidDeveloper.wastatussaver.presentation.ui.player.components.CustomVideoControls
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToLong

@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@AndroidEntryPoint
class PlayerActivity : ComponentActivity() {
    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WaStatusSaverTheme {

                var currentPosition by remember { mutableLongStateOf(0L) }
                var duration by remember { mutableLongStateOf(0L) }
                var showControls by remember { mutableStateOf(true) }

                val viewModel = hiltViewModel<PlayerViewModel>()
                val mediaInfo by viewModel.mediaInfo.collectAsStateWithLifecycle()
                val mediaEnded by viewModel.mediaEnded.collectAsStateWithLifecycle()
                val viewPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
                val onRestore by viewModel.onRestore.collectAsStateWithLifecycle()
                val isDownloaded by viewModel.isDownloadStatus.collectAsStateWithLifecycle()
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

                LaunchedEffect(viewPlaying) {
                    while (isActive && viewPlaying) {

                        val totalDuration = viewModel.getPlayerDuration()
                        val position = viewModel.getPlayerCurrentPosition()

                        val progressPercent: Long =
                            if (duration > 0) {
                                ((position.toDouble() / duration.toDouble()) * 100)
                                    .roundToLong()
                            } else {
                                0L
                            }

                        currentPosition = progressPercent
                        duration = totalDuration.coerceAtLeast(0L)

                        delay(250)
                    }
                }

                LaunchedEffect(mediaInfo.uri) {
                    if (mediaInfo.uri.isNotEmpty()) {
                        viewModel.setMediaItem()

                        val totalDuration = viewModel.getPlayerDuration()
                        val position = viewModel.getPlayerCurrentPosition()

                        val progressPercent: Long =
                            if (duration > 0) {
                                ((position.toDouble() / duration.toDouble()) * 100)
                                    .roundToLong()
                            } else {
                                0L
                            }


                        currentPosition = progressPercent
                        duration = totalDuration.coerceAtLeast(0L)

                        delay(250)
                    }
                }

                if (onRestore) {
                    currentPosition = ((viewModel.getPlayerCurrentPosition()
                        .toFloat() / viewModel.getPlayerDuration().toFloat()) * 100).toLong()
                    duration = viewModel.getPlayerDuration().coerceAtLeast(0L)
                    viewModel.onRestored()
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
                        if (showControls) {
                            Row(
                                modifier = Modifier
                                    .zIndex(1f)
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(0.7f))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = mediaInfo.fileName,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                ActionButton(downloadStatus = isDownloaded, onSave = {
                                    viewModel.onDownloadClick(mediaInfo.toMediaFile())
                                }, onRepost = {
                                    val mediaFolder = when (mediaInfo.fileType) {
                                        FileType.AUDIO -> "Audio"
                                        FileType.IMAGES -> "Image"
                                        FileType.VIDEOS -> "Video"
                                        else -> "*"
                                    }
                                    val uri = if (isDownloaded == DownloadState.DOWNLOADED) {
                                        viewModel.getMediaUriFromDownloads(
                                            context = context,
                                            fileName = mediaInfo.fileName,
                                            mediaFolder = mediaFolder
                                        )
                                    } else {
                                        viewModel.saveCache(
                                            uri = mediaInfo.uri,
                                            context = context,
                                            fileType = mediaInfo.fileType
                                        )
                                    }
                                    val fileType = when (mediaInfo.fileType) {
                                        FileType.AUDIO -> "audio/*"
                                        FileType.IMAGES -> "jpg/*"
                                        FileType.VIDEOS -> "video/*"
                                        else -> "*"
                                    }
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = fileType
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
                        if (mediaInfo.fileType == FileType.AUDIO) {
                            AudioViewer(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }) {
                                        showControls = !showControls
                                    },
                                onPause = {
                                    viewModel.pause()
                                    viewModel.persistPlaybackPosition()
                                },
                            )
                        } else if (mediaInfo.fileType == FileType.IMAGES) {
                            ImageViewer(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }) {
                                        showControls = !showControls
                                    },
                                uri = mediaInfo.uri
                            )
                        } else {
                            AndroidView(
                                factory = { context ->
                                    PlayerView(context).also {
                                        it.player = viewModel.player
                                        it.useController = false
                                        it.keepScreenOn = true
                                        it.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    }
                                }, update = {
                                    when (lifecycle) {
                                        Lifecycle.Event.ON_PAUSE -> {
                                            it.onPause()
                                            it.player?.pause()
                                            viewModel.persistPlaybackPosition()
                                        }

                                        Lifecycle.Event.ON_RESUME -> {
                                            it.onResume()
                                        }

                                        else -> Unit
                                    }
                                }, modifier = Modifier
                                    .fillMaxSize()
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }) {
                                        showControls = !showControls
                                    }
                                    .padding(top = 12.dp))
                        }
                        if (showControls && (mediaInfo.fileType != FileType.IMAGES)) {
                            CustomVideoControls(
                                isPlaying = viewPlaying,
                                currentPosition = currentPosition,
                                duration = duration,
                                onPlayPauseClick = {
                                    Log.i(
                                        "Play Status", "onCreate: ${viewModel.isPlayerPlaying()}"
                                    )
                                    if (viewModel.isPlayerPlaying()) {
                                        viewModel.pause()
                                    } else {
                                        if (mediaEnded) {
                                            viewModel.playerSeekTo(0)
                                        }
                                        viewModel.play()
                                    }
                                },
                                onSeek = { position ->
                                    val value = viewModel.getPlayerDuration() * position / 100
                                    viewModel.playerSeekTo(value)

                                    currentPosition = ((viewModel.getPlayerCurrentPosition()
                                        .toFloat() / viewModel.getPlayerDuration()
                                        .toFloat()) * 100).toLong()
                                    duration = viewModel.getPlayerDuration().coerceAtLeast(0L)

                                },
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                    }
                }
            }
        }
    }

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

    @Composable
    fun ImageViewer(
        modifier: Modifier = Modifier,
        uri: String,
    ) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = uri,
                contentDescription = "Image Preview",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentScale = ContentScale.Fit
            )
        }
    }

}