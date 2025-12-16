package com.voidDeveloper.wastatussaver.presentation.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.voidDeveloper.wastatussaver.data.datastoremanager.DataStoreManager.DataStoreKeys.LAST_ALARM_SET_MILLIS_KEY
import com.voidDeveloper.wastatussaver.data.datastoremanager.DataStoreManager.DataStoreKeys.USER_PREF_WIDGET_REFRESH_INTERVAL_KEY
import com.voidDeveloper.wastatussaver.data.datastoremanager.DataStorePreferenceManager
import com.voidDeveloper.wastatussaver.data.utils.Constants.AUTO_SAVE_ACTION
import com.voidDeveloper.wastatussaver.data.utils.Constants.DEFAULT_WIDGET_REFRESH_INTERVAL
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
class AutoSaveReceiver() : BroadcastReceiver() {
    @Inject
    lateinit var scheduleAutoSave: ScheduleAutoSave

    @Inject
    lateinit var dataStorePreferenceManager: DataStorePreferenceManager

    @Inject
    lateinit var telegramLogUseCase: TelegramLogUseCase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context?, intent: Intent?) {
        val pendingResult = goAsync()

        scope.launch {
            try {

                if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
                    val lastAlarmSetTimeMillis =
                        dataStorePreferenceManager.getPreference(
                            LAST_ALARM_SET_MILLIS_KEY,
                            System.currentTimeMillis()
                        ).first()
                    val autoSaveInterval = dataStorePreferenceManager.getPreference(
                        USER_PREF_WIDGET_REFRESH_INTERVAL_KEY,
                        DEFAULT_WIDGET_REFRESH_INTERVAL
                    ).first()
                    var timeToTrigger =
                        lastAlarmSetTimeMillis + TimeUnit.MINUTES.toMillis(autoSaveInterval.toLong())

                    if (timeToTrigger <= System.currentTimeMillis()) {
                        timeToTrigger =
                            System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(autoSaveInterval.toLong())
                    }

                    scheduleAutoSave.scheduleAutoSaveWorkAlarm(timeToTrigger)

                } else if (intent?.action == AUTO_SAVE_ACTION) {
                    dataStorePreferenceManager.putPreference(
                        LAST_ALARM_SET_MILLIS_KEY,
                        System.currentTimeMillis()
                    )
                    scheduleAutoSave.scheduleAutoSaveWorkManager()
                }

            } catch (e: Exception) {
                Log.e("WidgetRefreshReceiver", "onReceive Exception: ${e.message}")
            } finally {
                pendingResult.finish()
            }

        }

    }


}