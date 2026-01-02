package com.voidDeveloper.wastatussaver.presentation.ui.player.ui

import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.media3.common.util.UnstableApi
import com.voidDeveloper.wastatussaver.data.datastore.proto.MediaType
import com.voidDeveloper.wastatussaver.domain.model.MediaInfo
import com.voidDeveloper.wastatussaver.presentation.theme.WaStatusSaverTheme
import com.voidDeveloper.wastatussaver.presentation.ui.player.ui.imagePlayerRoot.ImageViewer
import com.voidDeveloper.wastatussaver.presentation.ui.player.ui.videoAudioPlayerRoot.AudioVideoPlayer
import dagger.hilt.android.AndroidEntryPoint

@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@AndroidEntryPoint
class PlayerActivity : ComponentActivity() {

    private var mediaInfo: MediaInfo? = null

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        mediaInfo = savedInstanceState?.getParcelable("mediaInfo") ?: run {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra("mediaInfo", MediaInfo::class.java)
            } else {
                intent.getParcelableExtra("mediaInfo")
            }
        }

        setContent {
            WaStatusSaverTheme {
                if (mediaInfo?.mediaType == MediaType.IMAGE) {
                    ImageViewer(intent)
                } else {
                    AudioVideoPlayer(intent)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mediaInfo?.let { outState.putParcelable("mediaInfo", it) }
    }

}
