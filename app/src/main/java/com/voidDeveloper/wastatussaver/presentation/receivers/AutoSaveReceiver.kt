package com.voidDeveloper.wastatussaver.presentation.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.voidDeveloper.wastatussaver.data.datastore.proto.AutoSaveUserPref
import com.voidDeveloper.wastatussaver.data.prefdatastoremanager.DataStorePreferenceManager
import com.voidDeveloper.wastatussaver.data.prefdatastoremanager.DataStorePreferenceManagerImpl.DataStoreKeys.LAST_ALARM_SET_MILLIS_KEY
import com.voidDeveloper.wastatussaver.data.protodatastoremanager.AutoSaveProtoDataStoreManager
import com.voidDeveloper.wastatussaver.data.utils.Constants.AUTO_SAVE_ACTION
import com.voidDeveloper.wastatussaver.data.utils.extentions.getInterval
import com.voidDeveloper.wastatussaver.data.utils.helpers.ScheduleAutoSave
import com.voidDeveloper.wastatussaver.domain.usecases.TelegramLogUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject


@AndroidEntryPoint
class AutoSaveReceiver : BroadcastReceiver() {

    @Inject
    lateinit var scheduleAutoSave: ScheduleAutoSave

    @Inject
    lateinit var dataStorePreferenceManager: DataStorePreferenceManager

    @Inject
    lateinit var autoSaveProtoDataStoreManager: AutoSaveProtoDataStoreManager

    @Inject
    lateinit var telegramLogUseCase: TelegramLogUseCase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context?, intent: Intent?) {
        val pendingResult = goAsync()

        scope.launch {
            try {
                when (intent?.action) {

                    Intent.ACTION_BOOT_COMPLETED -> {
                        val isAutoSaveEnable = isAutoSaveEnable()
                        if (isAutoSaveEnable) {
                            val lastAlarmSetTimeMillis =
                                dataStorePreferenceManager
                                    .getPreference(
                                        LAST_ALARM_SET_MILLIS_KEY,
                                        System.currentTimeMillis()
                                    )
                                    .first()


                            val autoSaveInterval = getAutoSaveInterval()

                            var timeToTrigger =
                                lastAlarmSetTimeMillis +
                                        TimeUnit.HOURS.toMillis(autoSaveInterval.toLong())

                            if (timeToTrigger <= System.currentTimeMillis()) {
                                timeToTrigger =
                                    System.currentTimeMillis() +
                                            TimeUnit.HOURS.toMillis(autoSaveInterval.toLong())
                            }

                            scheduleAutoSave.scheduleAutoSaveWorkAlarm(timeToTrigger)
                        }
                    }

                    AUTO_SAVE_ACTION -> {
                        val isAutoSaveEnable = isAutoSaveEnable()
                        if (isAutoSaveEnable) {
                            val now = System.currentTimeMillis()
                            dataStorePreferenceManager.putPreference(
                                LAST_ALARM_SET_MILLIS_KEY,
                                now
                            )

                            scheduleAutoSave.scheduleAutoSaveWorkManager()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun getAutoSaveInterval(): Int {
        val autoSaveUserPref = autoSaveProtoDataStoreManager.readAutoSaveUserPref()
        val interval = autoSaveUserPref.getInterval()
        return interval
    }

    private suspend fun isAutoSaveEnable(): Boolean {
        val autoSaveUserPref = autoSaveProtoDataStoreManager.readAutoSaveUserPref()
        val enable = autoSaveUserPref.enable
        return enable
    }

    private suspend fun getAutoSaveUserPref(): AutoSaveUserPref {
        return autoSaveProtoDataStoreManager.readAutoSaveUserPref()
    }

}