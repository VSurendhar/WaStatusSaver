package com.voiddevelopers.wastatussaver.presentation.ui.main.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voiddevelopers.wastatussaver.core.analytics.AppAnalytics
import com.voiddevelopers.wastatussaver.core.analytics.DownloadSource
import com.voiddevelopers.wastatussaver.core.analytics.MediaTabSource
import com.voiddevelopers.wastatussaver.core.analytics.OnboardingSource
import com.voiddevelopers.wastatussaver.data.prefdatastoremanager.DataStorePreferenceManager
import com.voiddevelopers.wastatussaver.data.prefdatastoremanager.DataStorePreferenceManagerImpl.DataStoreKeys.KEY_PREFERRED_TITLE
import com.voiddevelopers.wastatussaver.data.prefdatastoremanager.DataStorePreferenceManagerImpl.DataStoreKeys.KEY_SHOULD_SHOW_ONBOARDING_UI
import com.voiddevelopers.wastatussaver.domain.model.MediaFile
import com.voiddevelopers.wastatussaver.domain.model.MediaType
import com.voiddevelopers.wastatussaver.domain.usecases.AppInstallCheckerUseCase
import com.voiddevelopers.wastatussaver.domain.usecases.StatusesManagerUseCase
import com.voiddevelopers.wastatussaver.presentation.ui.player.ui.videoAudioPlayerRoot.DownloadState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val dataStorePreferenceManager: DataStorePreferenceManager,
    private val statusesManagerUseCase: StatusesManagerUseCase,
    private val appInstallChecker: AppInstallCheckerUseCase,
    private val appAnalytics: AppAnalytics
) : ViewModel() {

    private val _uiState: MutableStateFlow<UiState?> = MutableStateFlow(
        null
    )

    val uiState = _uiState.asStateFlow()

    private var onboardingSource = OnboardingSource.INITIAL_LAUNCH

    // Tab currently in view on Home, for tab analytics. Null until the first tab.
    private var currentTab: MediaType? = null

    init {
        observeSuperProperties()
        observeOnboardingShown()
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
                currentMediaType = MediaType.IMAGE,
                mediaFiles = if (!shouldShowOnBoardingUi && appInstalled &&
                    hasSafAccessPermission
                ) {
                    getFiles(preferredTitle)
                } else {
                    emptyList()
                }
            )
        }
    }

    private fun hasSafAccessPermission(appInstalled: Boolean, preferredTitle: Title): Boolean =
        (if (appInstalled) statusesManagerUseCase.hasPermission(preferredTitle.uri) else null) ==
            true

    private fun getAppInstalledStatus(preferredTitle: Title): Boolean =
        appInstallChecker.isInstalled(preferredTitle.packageName)

    private suspend fun shouldShowOnBoardingUi(): Boolean =
        dataStorePreferenceManager.getPreference(
            KEY_SHOULD_SHOW_ONBOARDING_UI,
            defaultValue = true
        ).first()

    suspend fun getPreferredTitle(): Title {
        val preferredId = try {
            dataStorePreferenceManager.getPreference(
                KEY_PREFERRED_TITLE,
                defaultValue = Title.Whatsapp.packageName
            ).first()
        } catch (e: Exception) {
            e.printStackTrace()
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
                _uiState.update { it?.copy(currentMediaType = event.mediaType) }
                if (currentTab == event.mediaType) return
                currentTab = event.mediaType
                appAnalytics.trackMediaTabViewed(event.mediaType, MediaTabSource.HOME)
            }

            is Event.ChangeTitle -> {
                val previousTitle = uiState.value?.title
                viewModelScope.launch {
                    val appInstalled by lazy {
                        appInstallChecker.isInstalled(event.title.packageName)
                    }
                    val hasSafPermission by lazy {
                        statusesManagerUseCase.hasPermission(event.title.uri)
                    }
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
                                hasSafAccessPermission = false,
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
                    if (event.title != previousTitle) {
                        uiState.value?.let { updateSuperProperties(it) }
                        appAnalytics.trackTitleChanged()
                    }
                }
            }

            is Event.ChangeSelectionMode -> {
                _uiState.update { it?.copy(selectionMode = event.mode) }
            }

            is Event.OnGrantStorageAccessClick -> {
                appAnalytics.trackGrantStorageAccessClicked()
            }

            is Event.SafAccessDenied -> {
                appAnalytics.trackStorageAccessDenied(event.reason)
            }

            is Event.OnDownloadAppClick -> {
                appAnalytics.trackDownloadAppClicked()
            }

            is Event.SafAccessGranted -> {
                appAnalytics.trackStorageAccessGranted()
                viewModelScope.launch {
                    val title = uiState.value?.title ?: return@launch
                    val files = getFiles(title)
                    _uiState.update {
                        it?.copy(hasSafAccessPermission = true, mediaFiles = files)
                    }
                }
            }

            is Event.ChangeShowOnBoardingUiStatus -> {
                if (event.status) onboardingSource = OnboardingSource.HOW_TO_USE
                viewModelScope.launch {
                    _uiState.update { it?.copy(shouldShowOnBoardingUi = event.status) }
                    dataStorePreferenceManager.putPreference(
                        KEY_SHOULD_SHOW_ONBOARDING_UI,
                        event.status
                    )
                }
            }

            is Event.OnOnboardingNextClick -> {
                appAnalytics.trackOnboardingNextClicked(event.page, onboardingSource)
            }

            is Event.OnOnboardingSkipClick -> {
                appAnalytics.trackOnboardingSkipClicked(event.page, onboardingSource)
            }

            is Event.RefreshUiState -> {
                refreshUiState()
            }

            is Event.OnRefreshClick -> {
                appAnalytics.trackStatusMediaRefreshClicked()
                refreshUiState()
            }

            is Event.OnDownloadClick -> {
                saveMediaFile(event.mediaFile)
            }

            is Event.OnDrawerOptionClick -> {
                appAnalytics.trackNavDrawerOptionClicked(event.option.analyticsName)
            }

            is Event.OnDrawerStateChanged -> {
                if (event.isOpen) {
                    appAnalytics.trackNavDrawerOpened()
                } else {
                    appAnalytics.trackNavDrawerClosed()
                }
            }
        }
    }

    private fun refreshUiState() {
        viewModelScope.launch {
            val preferredTitle = getPreferredTitle()
            val shouldShowOnBoardingUi = shouldShowOnBoardingUi()
            val appInstalled = getAppInstalledStatus(preferredTitle)
            val hasSafAccessPermission = hasSafAccessPermission(appInstalled, preferredTitle)
            _uiState.update { current ->
                val updated = current?.copy(
                    shouldShowOnBoardingUi = shouldShowOnBoardingUi,
                    hasSafAccessPermission = hasSafAccessPermission,
                    appInstalled = appInstalled,
                    mediaFiles = if (hasSafAccessPermission && !shouldShowOnBoardingUi &&
                        appInstalled &&
                        current.title != null
                    ) {
                        val files = getFiles(current.title).toList()
                        files
                    } else {
                        current.mediaFiles
                    }
                )

                updated
            }
        }
    }

    fun saveMediaFile(mediaFile: MediaFile) {
        viewModelScope.launch {
            mediaFile.downloadState = DownloadState.DOWNLOADING
            statusesManagerUseCase.saveMediaFile(mediaFile, onSaveCompleted = {
                mediaFile.downloadState = DownloadState.DOWNLOADED
                appAnalytics.trackMediaDownloadCompleted(mediaFile.mediaType, DownloadSource.HOME)
            })
        }
    }

    private suspend fun setPreferredTitle(title: Title) {
        dataStorePreferenceManager.putPreference(KEY_PREFERRED_TITLE, title.packageName)
    }

    private suspend fun getFiles(title: Title): List<MediaFile> {
        val files = statusesManagerUseCase.getFiles(title.uri)
        return files
    }

    private fun observeSuperProperties() {
        viewModelScope.launch {
            uiState.filterNotNull()
                .mapNotNull { state ->
                    val title = state.title ?: return@mapNotNull null
                    Triple(title.analyticsName, state.hasSafAccessPermission, state.appInstalled)
                }
                .distinctUntilChanged()
                .collect { (title, hasSafPermission, isAppInstalled) ->
                    appAnalytics.updateSuperProperties(title, hasSafPermission, isAppInstalled)
                }
        }
    }

    private fun observeOnboardingShown() {
        viewModelScope.launch {
            uiState.map { it?.shouldShowOnBoardingUi == true }
                .distinctUntilChanged()
                .filter { it }
                .collect { appAnalytics.trackOnboardingViewed(onboardingSource) }
        }
    }

    private fun updateSuperProperties(state: UiState) {
        val title = state.title ?: return
        appAnalytics.updateSuperProperties(
            title.analyticsName,
            state.hasSafAccessPermission,
            state.appInstalled
        )
    }
}
