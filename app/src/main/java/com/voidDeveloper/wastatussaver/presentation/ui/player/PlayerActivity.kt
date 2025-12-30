package com.voidDeveloper.wastatussaver.presentation.ui.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.voidDeveloper.wastatussaver.R
import com.voidDeveloper.wastatussaver.presentation.theme.WaStatusSaverTheme
import com.voidDeveloper.wastatussaver.presentation.ui.player.components.CustomVideoControls
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@AndroidEntryPoint
class PlayerActivity : ComponentActivity() {
    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val videoUri = "android.resource://${packageName}/${R.raw.samplevideo}"
        val videoUri1 = "android.resource://${packageName}/${R.raw.video1}"
        val videoUri2 = "android.resource://${packageName}/${R.raw.video2}"
        val videoUri3 = "android.resource://${packageName}/${R.raw.video3}"
        val videoUri4 = "android.resource://${packageName}/${R.raw.video4}"
        val videoUri5 = "android.resource://${packageName}/${R.raw.video5}"
        val videoUri6 = "android.resource://${packageName}/${R.raw.video6}"
        val videoUri7 = "android.resource://${packageName}/${R.raw.video7}"
        setContent {
            WaStatusSaverTheme {
                val viewModel = hiltViewModel<PlayerViewModel>()
                var isPlaying by remember { mutableStateOf(true) }
                var currentPosition by remember { mutableLongStateOf(0L) }
                var duration by remember { mutableLongStateOf(0L) }
                var showControls by remember { mutableStateOf(true) }
                val mediaInfo by viewModel.mediaInfo.collectAsStateWithLifecycle()
                val context = LocalContext.current
                LaunchedEffect(Unit) {
                    val uri = intent.getStringExtra("videoUri") ?: ""
                    println("URI $uri")
                    viewModel.addVideoUri(uri, "")
                }
//                viewModel.addVideoUri(videoUri1.toUri())
//                viewModel.addVideoUri(videoUri2.toUri())
//                viewModel.addVideoUri(videoUri3.toUri())
//                viewModel.addVideoUri(videoUri4.toUri())
//                viewModel.addVideoUri(videoUri5.toUri())
//                viewModel.addVideoUri(videoUri6.toUri())
//                viewModel.addVideoUri(videoUri7.toUri())
                val scope = CoroutineScope(Dispatchers.Main)
                var lifecycle by remember {
                    mutableStateOf(Lifecycle.Event.ON_CREATE)
                }
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        lifecycle = event
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)

                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }
                LaunchedEffect(viewModel.player) {
                    while (isActive) {
                        currentPosition =
                            ((viewModel.player.currentPosition.toFloat() / viewModel.player.duration.toFloat()) * 100).toLong()
                        duration = viewModel.player.duration.coerceAtLeast(0L)
                        delay(100)
                    }
                }
                LaunchedEffect(mediaInfo.uri) {
                    if (mediaInfo.uri.isNotEmpty()) {
                        viewModel.playVideo()
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

                                    else -> Unit
                                }
                            }, modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }) {
                                    showControls = !showControls
                                }
                                .padding(bottom = 24.dp)
                        )
                        if (showControls) {
                            CustomVideoControls(
                                isPlaying = isPlaying,
                                currentPosition = currentPosition,
                                duration = duration,
                                onPlayPauseClick = {
                                    if (viewModel.player.isPlaying) {
                                        viewModel.player.pause()
                                        isPlaying = false
                                    } else {
                                        viewModel.player.play()
                                        isPlaying = true
                                    }
                                },
                                onSeek = { position ->
                                    println("Seeking to position: $position, Current: ${viewModel.player.currentPosition}, Duration: ${viewModel.player.duration}")
                                    val value = viewModel.player.duration * position / 100
                                    viewModel.player.seekTo(value)
                                },
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!", modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WaStatusSaverTheme {
        Greeting("Android")
    }
}