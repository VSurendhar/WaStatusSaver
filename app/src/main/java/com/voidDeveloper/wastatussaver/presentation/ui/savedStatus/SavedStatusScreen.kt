package com.voidDeveloper.wastatussaver.presentation.ui.savedStatus

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voidDeveloper.wastatussaver.R
import com.voidDeveloper.wastatussaver.data.datastore.proto.MediaType
import com.voidDeveloper.wastatussaver.data.utils.createColoredString
import com.voidDeveloper.wastatussaver.data.utils.extentions.singleClick
import com.voidDeveloper.wastatussaver.domain.model.AudioFile
import com.voidDeveloper.wastatussaver.domain.model.ImageFile
import com.voidDeveloper.wastatussaver.domain.model.MediaFile
import com.voidDeveloper.wastatussaver.domain.model.VideoFile
import com.voidDeveloper.wastatussaver.presentation.ui.main.ui.MissingSetupInfo
import com.voidDeveloper.wastatussaver.presentation.ui.main.ui.PreviewItem
import com.voidDeveloper.wastatussaver.presentation.ui.main.ui.TabsRow
import com.voidDeveloper.wastatussaver.presentation.ui.main.ui.toDomainMediaInfo
import com.voidDeveloper.wastatussaver.presentation.ui.player.ui.PlayerActivity
import com.voidDeveloper.wastatussaver.presentation.ui.player.ui.videoAudioPlayerRoot.DownloadState

@Composable
fun SavedStatusScreen(onBack: () -> Boolean) {

    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val mediaTypeData = listOf(MediaType.IMAGE, MediaType.VIDEO, MediaType.AUDIO)
    val viewModel = hiltViewModel<SavedStatusViewModel>()
    val savedStatus by viewModel.savedStatus.collectAsStateWithLifecycle()
    var refreshStatus by remember { mutableStateOf(true) }

    if (refreshStatus) {
        viewModel.getSavedStatusMedia()
        refreshStatus = false
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        topBar = {
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
                                    onBack()
                                })
                        Text(
                            text = "Saved Status Media",
                            modifier = Modifier.height(30.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(modifier = Modifier.size(50.dp), onClick = {
                            refreshStatus = true
                        }) {
                            Icon(
                                tint = Color.White,
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Refresh",
                            )
                        }
                    }
                    TabsRow(pagerState, scope = scope, mediaTypeData = mediaTypeData)
                }
            }
        }) { innerPadding ->
        MainBody(
            modifier = Modifier.padding(innerPadding),
            pagerState = pagerState,
            mediaFiles = savedStatus,
        )
    }

}

@Composable
private fun MainBody(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    mediaFiles: List<MediaFile>,
) {
    val context = LocalContext.current
    val audioMediaFiles = mediaFiles.filter { it is AudioFile }
    val videoMediaFiles = mediaFiles.filter { it is VideoFile }
    val imageMediaFiles = mediaFiles.filter { it is ImageFile }
    HorizontalPager(
        state = pagerState, modifier = modifier.fillMaxSize()
    ) { page ->
        when (page) {
            0 -> {
                FilePreviewPage(imageFiles = imageMediaFiles, onPreviewClick = { mediaFile ->
                    val intent = Intent(context, PlayerActivity::class.java)
                    intent.putExtra(
                        "mediaInfo", mediaFile.toDomainMediaInfo().apply {
                            downloadStatus = DownloadState.DOWNLOADED
                        })
                    context.startActivity(intent)
                }, mediaType = MediaType.IMAGE)
            }

            1 -> {
                FilePreviewPage(
                    imageFiles = videoMediaFiles,
                    onPreviewClick = { mediaFile ->
                        val intent = Intent(context, PlayerActivity::class.java)
                        intent.putExtra(
                            "mediaInfo", mediaFile.toDomainMediaInfo().apply {
                                downloadStatus = DownloadState.DOWNLOADED
                            })
                        context.startActivity(intent)
                    },
                    mediaType = MediaType.VIDEO
                )
            }

            2 -> {
                FilePreviewPage(
                    imageFiles = audioMediaFiles,
                    onPreviewClick = { mediaFile ->
                        val intent = Intent(context, PlayerActivity::class.java)
                        intent.putExtra(
                            "mediaInfo", mediaFile.toDomainMediaInfo().apply {
                                downloadStatus = DownloadState.DOWNLOADED
                            })
                        context.startActivity(intent)
                    },
                    mediaType = MediaType.AUDIO
                )
            }

        }
    }
}

@Composable
private fun FilePreviewPage(
    imageFiles: List<MediaFile>,
    onPreviewClick: (MediaFile) -> Unit,
    mediaType: MediaType,
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        color = Color.White,
    ) {
        if (imageFiles.isEmpty()) {
            val displayName = mediaType.name
                .lowercase()
                .replaceFirstChar { it.uppercase() }
            MissingSetupInfo(
                imageId = R.drawable.ic_empty_folder, title = createColoredString(
                    stringResource(R.string.no_status_files_available),
                    MaterialTheme.colorScheme.primary
                ), description = createColoredString(
                    "No **$displayName** statuses have been saved. Please save a status from the Home screen, then return here to view it.",
                    MaterialTheme.colorScheme.primary
                )
            )
        } else {
            LazyVerticalGrid(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items = imageFiles, key = { it.id }) { mediaFile ->
                    PreviewItem(
                        mediaFile,
                        onPreviewClick = {
                            onPreviewClick(mediaFile)
                        },
                        showDownloadIcon = false
                    )
                }
            }
        }
    }
}