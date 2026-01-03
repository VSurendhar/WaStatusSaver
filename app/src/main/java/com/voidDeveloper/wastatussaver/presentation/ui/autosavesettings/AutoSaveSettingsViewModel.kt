package com.voidDeveloper.wastatussaver.presentation.ui.autosavesettings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voidDeveloper.wastatussaver.data.datastore.proto.App
import com.voidDeveloper.wastatussaver.data.datastore.proto.AutoSaveInterval
import com.voidDeveloper.wastatussaver.data.datastore.proto.AutoSaveUserPref
import com.voidDeveloper.wastatussaver.data.datastore.proto.MediaType
import com.voidDeveloper.wastatussaver.data.protodatastoremanager.AutoSaveProtoDataStoreManager
import com.voidDeveloper.wastatussaver.data.utils.extentions.getInterval
import com.voidDeveloper.wastatussaver.data.utils.extentions.safeAutoSaveIntervalDomain
import com.voidDeveloper.wastatussaver.data.utils.extentions.toApp
import com.voidDeveloper.wastatussaver.data.utils.extentions.toAutoSaveIntervalDomain
import com.voidDeveloper.wastatussaver.data.utils.extentions.toTitle
import com.voidDeveloper.wastatussaver.data.utils.getMillisFromNow
import com.voidDeveloper.wastatussaver.data.utils.helpers.ScheduleAutoSave
import com.voidDeveloper.wastatussaver.domain.model.AutoSaveIntervalDomain
import com.voidDeveloper.wastatussaver.domain.usecases.AppInstallCheckerUseCase
import com.voidDeveloper.wastatussaver.presentation.ui.main.ui.Title
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AutoSaveSettingsViewModel @Inject constructor(
    private val scheduleAutoSave: ScheduleAutoSave,
    private val appInstallCheckerUseCase: AppInstallCheckerUseCase,
    private val autoSaveProtoDataStoreManager: AutoSaveProtoDataStoreManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AutoSaveSettingsUiState())

    val uiState: StateFlow<AutoSaveSettingsUiState> = _uiState.asStateFlow()

    private val _effect = Channel<AutoSaveSettingsEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var previousAutoSavePref: AutoSaveUserPref? = null


    fun onEvent(event: AutoSaveSettingsEvent) {
        when (event) {

            is AutoSaveSettingsEvent.ToggleAutoSave -> updateState { copy(isAutoSaveEnabled = event.enabled) }

            is AutoSaveSettingsEvent.SelectTitle -> updateState {
                copy(
                    selectedTitle = event.title
                )
            }

            is AutoSaveSettingsEvent.SelectInterval -> updateState { copy(selectedInterval = event.interval) }

            is AutoSaveSettingsEvent.ToggleMediaType -> updateState {
                copy(
                    selectedMediaTypes = if (event.enabled) selectedMediaTypes + event.mediaType
                    else selectedMediaTypes - event.mediaType
                )
            }

            is AutoSaveSettingsEvent.NotificationPermissionDenied -> {
                viewModelScope.launch {
                    if (event.shouldShowRationale) {
                        updateState { copy(showNotificationPermissionDialog = true) }
                    } else {
                        emitEffect(AutoSaveSettingsEffect.NavigateBack)
                    }
                }
            }

            AutoSaveSettingsEvent.NotificationPermissionDialogDismiss -> {
                viewModelScope.launch {
                    updateState { copy(showNotificationPermissionDialog = false) }
                    emitEffect(AutoSaveSettingsEffect.NavigateBack)
                }
            }

            AutoSaveSettingsEvent.NotificationSettingsDialogDismiss -> {
                viewModelScope.launch {
                    updateState { copy(showNotificationPermissionSettingsDialog = false) }
                    emitEffect(AutoSaveSettingsEffect.NavigateBack)
                }
            }

            AutoSaveSettingsEvent.NotificationPermissionOkClicked -> {
                viewModelScope.launch {
                    updateState { copy(showNotificationPermissionDialog = false) }
                    emitEffect(AutoSaveSettingsEffect.RequestNotificationPermission)
                }
            }

            AutoSaveSettingsEvent.SaveClicked -> saveAutoSaveUserPref()

        }
    }


    init {
        loadPreviousPreferences()
    }


    private fun loadPreviousPreferences() {
        viewModelScope.launch {
            try {
                val pref = getAutoSavePref()

                previousAutoSavePref = pref

                _uiState.update {
                    it.copy(
                        isAutoSaveEnabled = pref.enable,
                        selectedTitle = ensureInstalledTitle(pref.app.toTitle()), // Will Return this if installed or the installed Title
                        selectedInterval = pref.autoSaveInterval.toAutoSaveIntervalDomain()
                            .safeAutoSaveIntervalDomain(),
                        selectedMediaTypes = pref.mediaTypeList.toSet()
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun ensureInstalledTitle(title: Title): Title {
        return if (appInstalled(title.packageName)) {
            title
        } else {
            if (appInstalled(Title.Whatsapp.packageName)) {
                Title.Whatsapp
            } else {
                Title.WhatsappBusiness
            }
        }
    }

    suspend fun getAutoSavePref(): AutoSaveUserPref {
        val autoSaveUserPref = autoSaveProtoDataStoreManager.readAutoSaveUserPref()
        return autoSaveUserPref
    }

    private fun saveAutoSaveUserPref() {
        viewModelScope.launch {
            try {
                val state = uiState.value
                val previousPref = previousAutoSavePref

                val enable = state.isAutoSaveEnabled
                val scheduledInterval = state.selectedInterval.hours
                val selectedTitle = state.selectedTitle.toApp()
                val selectedMediaTypes = state.selectedMediaTypes.toList()

                if(!appInstalled(selectedTitle.toTitle().packageName)){
                    emitEffect(AutoSaveSettingsEffect.ShowToast("Please Install the Application to Continue"))
                    return@launch
                }else if(!hasSafPermission(selectedTitle.toTitle().uri)){
                    emitEffect(AutoSaveSettingsEffect.ShowToast("Please grant SAF permission"))
                    return@launch
                }

                saveAutoSaveData(
                    enable = enable,
                    selectedTitle = selectedTitle,
                    selectedMediaTypes = selectedMediaTypes
                )

                if (!enable) {
                    scheduleAutoSave.cancelAllAlarm()
                    emitEffect(AutoSaveSettingsEffect.NavigateBack)
                    return@launch
                }

                val previousInterval = previousPref?.getInterval()
                val isSameConfig =
                    previousPref?.enable == true && previousInterval == scheduledInterval && previousPref.app == selectedTitle && previousPref.mediaTypeList == selectedMediaTypes

                if (isSameConfig) {
                    emitEffect(
                        AutoSaveSettingsEffect.ShowToast("Interval already set")
                    )
                    emitEffect(AutoSaveSettingsEffect.RequestNotificationPermission)
                    return@launch
                }

                saveAutoSaveData(interval = scheduledInterval)

                val triggerAtMillis = getMillisFromNow(scheduledInterval)
                scheduleAutoSave.scheduleAutoSaveWorkAlarm(triggerAtMillis)

                emitEffect(AutoSaveSettingsEffect.RequestNotificationPermission)

            } catch (e: Exception) {
                e.printStackTrace()
                emitEffect(
                    AutoSaveSettingsEffect.ShowToast(
                        "Something went wrong while saving"
                    )
                )
            }
        }
    }

    private suspend fun saveAutoSaveData(
        interval: Int? = null,
        enable: Boolean? = null,
        selectedTitle: App? = null,
        selectedMediaTypes: List<MediaType>? = null,
    ) {
        autoSaveProtoDataStoreManager.updateAutoSaveUserPref { autoSaveUserPref ->
            if (interval != null) {
                val autoSaveInterval: AutoSaveInterval = when (interval) {
                    12 -> AutoSaveInterval.TWELVE
                    1 -> AutoSaveInterval.ONE
                    6 -> AutoSaveInterval.SIX
                    24 -> AutoSaveInterval.TWENTY_FOUR
                    else -> AutoSaveInterval.TWENTY_FOUR
                }
                autoSaveUserPref.setAutoSaveInterval(autoSaveInterval)
            }
            if (enable != null) {
                autoSaveUserPref.enable = enable
                if (!enable) autoSaveUserPref.setAutoSaveInterval(AutoSaveInterval.UNKOWN)
            }
            if (selectedTitle != null) {
                autoSaveUserPref.setApp(selectedTitle)
            }
            if (selectedMediaTypes != null) {
                autoSaveUserPref.clearMediaType()
                autoSaveUserPref.addAllMediaType(selectedMediaTypes)
            }
        }
    }


    private fun updateState(
        reducer: AutoSaveSettingsUiState.() -> AutoSaveSettingsUiState,
    ) {
        _uiState.update(reducer)
    }

    private suspend fun emitEffect(effect: AutoSaveSettingsEffect) {
        _effect.send(effect)
    }

    fun hasSafPermission(uri: Uri): Boolean {
        return appInstallCheckerUseCase.hasSafAccessPermission(uri)
    }

    fun appInstalled(packageName: String): Boolean {
        return appInstallCheckerUseCase.isInstalled(packageName)
    }

}

data class AutoSaveSettingsUiState(
    val isAutoSaveEnabled: Boolean = false,
    val selectedTitle: Title = Title.Whatsapp,
    val selectedInterval: AutoSaveIntervalDomain = AutoSaveIntervalDomain.TWENTY_FOUR_HOURS,
    val selectedMediaTypes: Set<MediaType> = setOf(
        MediaType.IMAGE, MediaType.VIDEO, MediaType.AUDIO
    ),
    val showNotificationPermissionDialog: Boolean = false,
    val showNotificationPermissionSettingsDialog: Boolean = false,
)

sealed interface AutoSaveSettingsEvent {

    data class ToggleAutoSave(val enabled: Boolean) : AutoSaveSettingsEvent

    data class SelectTitle(val title: Title) : AutoSaveSettingsEvent
    data class SelectInterval(val interval: AutoSaveIntervalDomain) : AutoSaveSettingsEvent

    data class ToggleMediaType(
        val mediaType: MediaType,
        val enabled: Boolean,
    ) : AutoSaveSettingsEvent

    data class NotificationPermissionDenied(val shouldShowRationale: Boolean) :
        AutoSaveSettingsEvent

    object NotificationPermissionDialogDismiss : AutoSaveSettingsEvent
    object NotificationSettingsDialogDismiss : AutoSaveSettingsEvent
    object NotificationPermissionOkClicked : AutoSaveSettingsEvent

    object SaveClicked : AutoSaveSettingsEvent
}

sealed interface AutoSaveSettingsEffect {
    data class ShowToast(val message: String) : AutoSaveSettingsEffect
    object NavigateBack : AutoSaveSettingsEffect
    object RequestNotificationPermission : AutoSaveSettingsEffect
    object OpenNotificationSettings : AutoSaveSettingsEffect
}


