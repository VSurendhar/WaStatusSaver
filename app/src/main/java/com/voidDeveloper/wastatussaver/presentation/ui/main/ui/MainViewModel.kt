package com.voidDeveloper.wastatussaver.presentation.ui.main.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.voidDeveloper.wastatussaver.R
import com.voidDeveloper.wastatussaver.data.datastoremanager.DataStoreManager.DataStoreKeys.KEY_AUTO_SAVE_INTERVAL
import com.voidDeveloper.wastatussaver.data.datastoremanager.DataStoreManager.DataStoreKeys.KEY_PREFERRED_TITLE
import com.voidDeveloper.wastatussaver.data.datastoremanager.DataStoreManager.DataStoreKeys.KEY_SHOULD_SHOW_ONBOARDING_UI
import com.voidDeveloper.wastatussaver.data.datastoremanager.DataStorePreferenceManager
import com.voidDeveloper.wastatussaver.data.utils.Constants.TAG
import com.voidDeveloper.wastatussaver.data.utils.compressBitmapQuality
import com.voidDeveloper.wastatussaver.data.utils.getMillisFromNow
import com.voidDeveloper.wastatussaver.data.utils.helpers.ScheduleAutoSave
import com.voidDeveloper.wastatussaver.domain.usecases.StatusesManagerUseCase
import com.voidDeveloper.wastatussaver.domain.usecases.AppInstallCheckerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val dataStorePreferenceManager: DataStorePreferenceManager,
    private val statusesManagerUseCase: StatusesManagerUseCase,
    private val appInstallChecker: AppInstallCheckerUseCase,
    private val scheduleAutoSave: ScheduleAutoSave,
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
        return (if (appInstalled) statusesManagerUseCase.hasPermission(preferredTitle.uri) else null) == true
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
                    val hasSafPermission by lazy { statusesManagerUseCase.hasPermission(event.title.uri) }
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

            is Event.OnDownloadClick -> {
                _uiState.update {
                    it?.copy(
                        onGoingDownload = (it.onGoingDownload + event.mediaFile).distinct()
                            .toMutableList()
                    )
                }
                saveMediaFile(event.mediaFile)
            }

            is Event.ShowAutoSaveDialog -> {
                viewModelScope.launch {
                    _uiState.update {
                        it?.copy(
                            showAutoSaveDialog = true, savedAutoSaveInterval = getAutoSaveInterval()
                        )
                    }
                }
            }

            is Event.SaveAutoSaveInterval -> {
                viewModelScope.launch {
                    saveAutoSaveInterval(event.interval)
                    scheduleAutoSave.scheduleAutoSaveWorkAlarm(getMillisFromNow(event.interval))
                }
            }

            is Event.ShowNotificationPermissionDialog -> {
                _uiState.update {
                    it?.copy(
                        showNotificationPermissionDialog = true,
                    )
                }
            }

            is Event.ShowNotificationPermissionSettingsDialog -> {
                _uiState.update {
                    it?.copy(
                        showNotificationPermissionSettingsDialog = true,
                    )
                }
            }

            is Event.NotificationPermissionDialogDismiss -> {
                _uiState.update {
                    it?.copy(
                        showNotificationPermissionDialog = false,
                    )
                }
            }

            Event.NotificationSettingsDialogDismiss -> {
                _uiState.update {
                    it?.copy(
                        showNotificationPermissionSettingsDialog = false
                    )
                }
            }

            Event.AutoSaveDialogDismiss -> {
                _uiState.update {
                    it?.copy(
                        showAutoSaveDialog = false,
                    )
                }
            }

        }

    }


    private suspend fun getAutoSaveInterval(): Int {
        return dataStorePreferenceManager.getPreference(
            KEY_AUTO_SAVE_INTERVAL, defaultValue = 1
        ).first()
    }

    private suspend fun saveAutoSaveInterval(interval: Int) {
        return dataStorePreferenceManager.putPreference(
            KEY_AUTO_SAVE_INTERVAL, interval
        )
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
                    if (uiState?.hasSafAccessPermission == true && uiState.shouldShowOnBoardingUi == false && uiState.appInstalled == true && uiState.title != null) {
                        val files = getFiles(uiState.title, true)
                        uiState = uiState.copy(mediaFiles = files)
                    }
                    uiState
                }
                uiState
            }
        }
    }


    fun saveMediaFile(mediaFile: MediaFile) {
        viewModelScope.launch {
            statusesManagerUseCase.saveMediaFile(mediaFile, onSaveCompleted = {
                mediaFile.isDownloaded = true
                _uiState.update {
                    it?.copy(
                        onGoingDownload = (it.onGoingDownload - mediaFile).distinct()
                            .toMutableList()
                    )
                }
            })
        }
    }

    private suspend fun setPreferredTitle(title: Title) {
        dataStorePreferenceManager.putPreference(KEY_PREFERRED_TITLE, title.packageName)
    }

    private fun getFiles(title: Title, shouldRefresh: Boolean = false): List<MediaFile> {
        val files = statusesManagerUseCase.getFiles(title.uri, shouldRefresh)
        return files
    }


}

abstract class MediaFile(open val uri: Uri, fileName1: String) {
    val id: String = UUID.randomUUID().toString()
    var isDownloaded: Boolean = false
    abstract val fileType: FileType
    protected val width: Int = 150
    protected val height: Int = 300
    open val fileName: String? = null
    protected var thumbnailBitmap: Bitmap? = null
    abstract fun getThumbNailBitMap(context: Context): Bitmap?
}

@Suppress("DEPRECATION")
data class ImageFile(
    override val uri: Uri,
    override val fileName: String,
    override val fileType: FileType = FileType.IMAGES,
) : MediaFile(uri, fileName) {
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
    override val uri: Uri,
    override val fileName: String,
    override val fileType: FileType = FileType.AUDIO,
) : MediaFile(uri, fileName) {
    private var mediaItem: MediaItem? = null

    fun getMediaItem(context: Context): MediaItem {
        if (mediaItem == null) {
            val artworkUri =
                "android.resource://${context.packageName}/${R.drawable.ic_audio_preview}".toUri()
            mediaItem = MediaItem.Builder().setUri(uri).setMediaMetadata(
                MediaMetadata.Builder().setDisplayTitle(fileName).setArtworkUri(artworkUri).build()
            ).build()
        }
        return mediaItem!!
    }

    override fun getThumbNailBitMap(context: Context): Bitmap? {
        if (thumbnailBitmap != null) {
            return thumbnailBitmap
        }
        val bitmap = try {
            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_audio_thumbnail)

            val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(resultBitmap)

            canvas.drawColor(0x1A007F68)

            drawable?.let {
                val drawableWidth = it.intrinsicWidth
                val drawableHeight = it.intrinsicHeight

                val scaleX = (width.toFloat() / drawableWidth) * 0.8f
                val scaleY = height.toFloat() / drawableHeight
                val scale = minOf(scaleX, scaleY, 1f)

                val scaledWidth = (drawableWidth * scale).toInt()
                val scaledHeight = (drawableHeight * scale).toInt()

                val left = (width - scaledWidth) / 2
                val top = (height - scaledHeight) / 2
                val right = left + scaledWidth
                val bottom = top + scaledHeight

                it.setBounds(left, top, right, bottom)
                it.draw(canvas)
            }

            resultBitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error creating audio thumbnail", e)
            null
        }
        thumbnailBitmap = bitmap
        return bitmap
    }

}


data class VideoFile(
    override val uri: Uri,
    override val fileName: String,
    override val fileType: FileType = FileType.VIDEOS,
) : MediaFile(uri, fileName) {
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

data class UnknownFile(
    override val uri: Uri,
    override val fileType: FileType = FileType.UNSPECIFIED,
) : MediaFile(uri, "unknownFile") {
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
