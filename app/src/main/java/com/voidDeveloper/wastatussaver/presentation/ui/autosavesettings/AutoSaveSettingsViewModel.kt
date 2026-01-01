package com.voidDeveloper.wastatussaver.presentation.ui.autosavesettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voidDeveloper.wastatussaver.data.datastore.proto.App
import com.voidDeveloper.wastatussaver.data.datastore.proto.AutoSaveInterval
import com.voidDeveloper.wastatussaver.data.datastore.proto.AutoSaveUserPref
import com.voidDeveloper.wastatussaver.data.datastore.proto.MediaType
import com.voidDeveloper.wastatussaver.data.protodatastoremanager.AutoSaveProtoDataStoreManager
import com.voidDeveloper.wastatussaver.data.utils.extentions.getInterval
import com.voidDeveloper.wastatussaver.data.utils.getMillisFromNow
import com.voidDeveloper.wastatussaver.data.utils.helpers.ScheduleAutoSave
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AutoSaveSettingsViewModel @Inject constructor(
    private val scheduleAutoSave: ScheduleAutoSave,
    private val autoSaveProtoDataStoreManager: AutoSaveProtoDataStoreManager,
) : ViewModel() {

    private val _toastInfoChannel = Channel<String?>()
    val toastInfoChannel = _toastInfoChannel.receiveAsFlow()

    private val _saved = MutableSharedFlow<Boolean>()
    val saved = _saved.asSharedFlow()
    private val _previousAutoSavePref: MutableStateFlow<AutoSaveUserPref?> = MutableStateFlow(null)
    val previousAutoSavePref = _previousAutoSavePref.asStateFlow()

    init {
        viewModelScope.launch {
            _previousAutoSavePref.value = getAutoSavePref()
        }
    }

    suspend fun getAutoSavePref(): AutoSaveUserPref {
        val autoSaveUserPref = autoSaveProtoDataStoreManager.readAutoSaveUserPref()
        return autoSaveUserPref
    }

    fun saveAutoSaveUserPref(
        enable: Boolean,
        scheduledInterval: Int,
        selectedTitle: App,
        selectedMediaTypes: List<MediaType>,
    ) {
        viewModelScope.launch {
            try {
                val previousPref = _previousAutoSavePref.value

                saveAutoSaveData(
                    enable = enable,
                    selectedTitle = selectedTitle,
                    selectedMediaTypes = selectedMediaTypes
                )

                if (!enable) {
                    scheduleAutoSave.cancelAllAlarm()
                    _saved.emit(true)
                    return@launch
                }

                val previousInterval = previousPref?.getInterval()
                val isSameConfig =
                    previousPref?.enable == true &&
                            previousInterval == scheduledInterval &&
                            previousPref.app == selectedTitle &&
                            previousPref.mediaTypeList == selectedMediaTypes

                if (isSameConfig) {
                    _toastInfoChannel.send("Interval already set")
                    _saved.emit(true)
                    return@launch
                }

                saveAutoSaveData(interval = scheduledInterval)

                val triggerAtMillis = getMillisFromNow(scheduledInterval)
                scheduleAutoSave.scheduleAutoSaveWorkAlarm(triggerAtMillis)

                _saved.emit(true)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun saveAutoSaveData(
        interval: Int? = null,
        enable: Boolean? = null,
        selectedTitle: App? = null,
        selectedMediaTypes: List<MediaType>? = null,
    ) {
        /*
                val gson = Gson()
                val userPrefAutoSave = dataStorePreferenceManager.getPreference(
                    USER_PREF_AUTO_SAVE, defaultValue = ""
                ).first()
                val ufAutoSave: AutoSave =
                    gson.fromJson(userPrefAutoSave, AutoSave::class.java) ?: AutoSave()
                if (interval != null) ufAutoSave.interval = interval
                if (enable != null) {
                    ufAutoSave.isAutoSaveEnable = enable
                    if (!enable) ufAutoSave.interval = null
                }
                val userPrefNewAutoSave = gson.toJson(ufAutoSave)
                dataStorePreferenceManager.putPreference(USER_PREF_AUTO_SAVE, userPrefNewAutoSave)
        */
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

    /*
        private suspend fun getAutoSaveInterval(): Int? {
            val autoSaveUserPref = autoSaveProtoDataStoreManager.readAutoSaveUserPref()
            val interval = autoSaveUserPref.getInterval()
            */
    /*        val userPrefAutoSave = dataStorePreferenceManager.getPreference(
                        USER_PREF_AUTO_SAVE, defaultValue = ""
                    ).first()
                    val gson = Gson()
                    val ufAutoSave = gson.fromJson(userPrefAutoSave, AutoSave::class.java)
                    return ufAutoSave?.interval*//*

        return interval
    }
*/

}
