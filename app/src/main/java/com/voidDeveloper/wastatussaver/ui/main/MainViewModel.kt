package com.voidDeveloper.wastatussaver.ui.main

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.voidDeveloper.wastatussaver.R
import com.voidDeveloper.wastatussaver.data.datastoremanager.DataStoreManager.DataStoreKeys.KEY_PREFERRED_TITLE
import com.voidDeveloper.wastatussaver.data.datastoremanager.DataStoreManager.DataStoreKeys.KEY_SHOULD_SHOW_ONBOARDING_UI
import com.voidDeveloper.wastatussaver.data.datastoremanager.DataStorePreferenceManager
import com.voidDeveloper.wastatussaver.ui.main.componenets.AppInstallChecker
import com.voidDeveloper.wastatussaver.ui.main.componenets.StatusesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import androidx.core.graphics.scale
import com.voidDeveloper.wastatussaver.data.utils.compressBitmapQuality

@HiltViewModel
class MainViewModel @Inject constructor(
    private val dataStorePreferenceManager: DataStorePreferenceManager,
    private val statusesManager: StatusesManager,
    private val appInstallChecker: AppInstallChecker,
) : ViewModel() {

    private val _uiState: MutableStateFlow<UiState?> = MutableStateFlow(
        null
    )

    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val preferredTitle = getPreferredTitle()
            val shouldShowOnBoardingUi = shouldShowOnBoardingUi()
            val appInstalled = getAppInstalledStatus(preferredTitle)
            val hasSafAccessPermission = hasSafAccessPermission(appInstalled, preferredTitle)
            _uiState.value = UiState(
                title = preferredTitle,
                appInstalled = appInstalled,
                shouldShowOnBoardingUi = shouldShowOnBoardingUi,
                hasSafAccessPermission = hasSafAccessPermission,
                selectionMode = SelectionMode.SINGLE_SELECT,
                currentFileType = FileType.IMAGES,
                mediaFiles = if (!shouldShowOnBoardingUi && appInstalled && hasSafAccessPermission) {
                    getFiles(preferredTitle)
                } else {
                    emptyList()
                }
            )
        }
    }


    private fun hasSafAccessPermission(appInstalled: Boolean, preferredTitle: Title): Boolean {
        return (if (appInstalled) statusesManager.hasPermission(preferredTitle.uri) else null) == true
    }

    private fun getAppInstalledStatus(preferredTitle: Title): Boolean {
        return appInstallChecker.isInstalled(preferredTitle.packageName)
    }

    private suspend fun shouldShowOnBoardingUi(): Boolean {
        return dataStorePreferenceManager.getPreference(
            KEY_SHOULD_SHOW_ONBOARDING_UI, defaultValue = true
        ).first()
    }

    suspend fun getPreferredTitle(): Title {

        val preferredId = try {
            dataStorePreferenceManager.getPreference(
                KEY_PREFERRED_TITLE, defaultValue = Title.Whatsapp.packageName
            ).first()
        } catch (e: Exception) {
            Title.Whatsapp.packageName
        }

        val title = when (preferredId) {
            Title.WhatsappBusiness.packageName -> {
                Title.WhatsappBusiness
            }

            else -> {
                Title.Whatsapp
            }
        }

        return title
    }

    fun onEvent(event: Event) {
        when (event) {
            is Event.ChangeTab -> {
                _uiState.update { it?.copy(currentFileType = event.fileType) }
            }

            is Event.ChangeTitle -> {
                viewModelScope.launch {
                    val appInstalled by lazy { appInstallChecker.isInstalled(event.title.packageName) }
                    val hasSafPermission by lazy { statusesManager.hasPermission(event.title.uri) }
                    setPreferredTitle(event.title)
                    if (appInstalled && hasSafPermission) {
                        val files = getFiles(event.title)
                        _uiState.update {
                            it?.copy(
                                title = event.title,
                                hasSafAccessPermission = true,
                                appInstalled = true,
                                mediaFiles = files
                            )
                        }
                    } else if (!appInstalled) {
                        _uiState.update {
                            it?.copy(
                                title = event.title,
                                hasSafAccessPermission = null,
                                mediaFiles = emptyList(),
                                appInstalled = false
                            )
                        }
                    } else {
                        _uiState.update {
                            it?.copy(
                                title = event.title,
                                hasSafAccessPermission = false,
                                mediaFiles = emptyList(),
                                appInstalled = true
                            )
                        }
                    }
                }
            }

            is Event.ChangeSelectionMode -> {
                _uiState.update { it?.copy(selectionMode = event.mode) }
            }

            is Event.ChangeSAFAccessPermission -> {
                _uiState.update {
                    if (event.hasSafAccessPermission == true) {
                        val files = getFiles(uiState.value?.title!!)
                        it?.copy(mediaFiles = files)
                    }
                    it?.copy(hasSafAccessPermission = event.hasSafAccessPermission)
                }
            }

            is Event.ChangeShowOnBoardingUiStatus -> {
                viewModelScope.launch {
                    _uiState.update { it?.copy(shouldShowOnBoardingUi = event.status) }
                    dataStorePreferenceManager.putPreference(
                        KEY_SHOULD_SHOW_ONBOARDING_UI, event.status
                    )
                }
            }

            is Event.ChangeAppInstalledStatus -> {
                _uiState.update {
                    it?.copy(
                        appInstalled = event.status, hasSafAccessPermission = null
                    )
                }
            }

            is Event.RefreshUiState -> {
                refreshUiState()
            }

        }
    }

    private fun refreshUiState() {
        viewModelScope.launch {
            if (_uiState.value != null) {
                val preferredTitle = getPreferredTitle()
                val shouldShowOnBoardingUi = shouldShowOnBoardingUi()
                val appInstalled = getAppInstalledStatus(preferredTitle)
                val hasSafAccessPermission = hasSafAccessPermission(appInstalled, preferredTitle)

                _uiState.update { current ->
                    var uiState = current

                    if (shouldShowOnBoardingUi) {
                        uiState = uiState?.copy(shouldShowOnBoardingUi = true)
                    }
                    if (hasSafAccessPermission) {
                        uiState = uiState?.copy(hasSafAccessPermission = true)
                    }
                    if (appInstalled) {
                        uiState = uiState?.copy(
                            appInstalled = true, hasSafAccessPermission = hasSafAccessPermission
                        )
                    }
                    uiState
                }
            }
        }
    }

    private suspend fun setPreferredTitle(title: Title) {
        dataStorePreferenceManager.putPreference(KEY_PREFERRED_TITLE, title.packageName)
    }

    private fun getFiles(title: Title): List<MediaFile> {
        val files = statusesManager.getFiles(title.uri)
        return files
    }

}

abstract class MediaFile {
    val id: String = UUID.randomUUID().toString()
    var isDownloaded: Boolean = false
    abstract val fileType: FileType
    protected val width: Int = 150
    protected val height: Int = 300
    protected var thumbnailBitmap: Bitmap? = null
    abstract fun getThumbNailBitMap(context: Context): Bitmap?
}

@Suppress("DEPRECATION")
data class ImageFile(
    val uri: Uri = "".toUri(),
    override val fileType: FileType = FileType.IMAGES,
) : MediaFile() {
    override fun getThumbNailBitMap(context: Context): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.setTargetSize(width, height)
            }.compressBitmapQuality()
        } else {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            bitmap.scale(width, height).compressBitmapQuality()
        }
    }
}

@Suppress("DEPRECATION")
data class AudioFile(
    val name: String, val uri: Uri,
    override val fileType: FileType = FileType.AUDIO,
) : MediaFile() {
    private var mediaItem: MediaItem? = null

    fun getMediaItem(context: Context): MediaItem {
        if (mediaItem == null) {
            val artworkUri =
                "android.resource://${context.packageName}/${R.drawable.ic_audio_preview}".toUri()
            mediaItem = MediaItem.Builder().setUri(uri).setMediaMetadata(
                MediaMetadata.Builder().setDisplayTitle(name).setArtworkUri(artworkUri).build()
            ).build()
        }
        return mediaItem!!
    }

    override fun getThumbNailBitMap(context: Context): Bitmap? {
        if (thumbnailBitmap != null) {
            return thumbnailBitmap
        }
        val uri =
            "android.resource://${context.packageName}/${R.drawable.ic_audio_thumbnail}".toUri()
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.setTargetSize(width, height)
            }.compressBitmapQuality()
        } else {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            bitmap.scale(width, height).compressBitmapQuality()
        }
        thumbnailBitmap = bitmap
        return bitmap
    }

}


data class VideoFile(
    val uri: Uri,
    override val fileType: FileType = FileType.VIDEOS,
) : MediaFile() {
    private var mediaItem: MediaItem = MediaItem.fromUri(uri)

    fun getMediaItem(): MediaItem {
        return mediaItem
    }

    override fun getThumbNailBitMap(context: Context): Bitmap? {
        if (thumbnailBitmap != null) {
            return thumbnailBitmap
        }
        val retriever = MediaMetadataRetriever()
        val bitmap = try {
            retriever.setDataSource(context, uri)
            retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?.compressBitmapQuality()
        } catch (e: Exception) {
            e.printStackTrace()
            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_failed_document)
            val bitmap = (drawable as BitmapDrawable).bitmap
            bitmap.compressBitmapQuality()
        } finally {
            retriever.release()
        }
        thumbnailBitmap = bitmap
        return bitmap
    }


}

data class UnknownFile(val uri: Uri, override val fileType: FileType = FileType.UNSPECIFIED) :
    MediaFile() {
    override fun getThumbNailBitMap(context: Context): Bitmap? {
        if (thumbnailBitmap != null) {
            return thumbnailBitmap
        }
        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_failed_document)
        val bitmap = (drawable as BitmapDrawable).bitmap
        thumbnailBitmap = bitmap
        return bitmap
    }
}
