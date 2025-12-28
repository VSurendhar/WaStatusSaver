package com.voidDeveloper.wastatussaver.presentation.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.gson.Gson
import com.voidDeveloper.wastatussaver.data.datastoremanager.DataStoreManager.DataStoreKeys.LAST_ALARM_SET_MILLIS_KEY
import com.voidDeveloper.wastatussaver.data.datastoremanager.DataStoreManager.DataStoreKeys.USER_PREF_AUTO_SAVE
import com.voidDeveloper.wastatussaver.data.datastoremanager.DataStorePreferenceManager
import com.voidDeveloper.wastatussaver.data.utils.Constants.AUTO_SAVE_ACTION
import com.voidDeveloper.wastatussaver.data.utils.Constants.DEFAULT_AUTO_SAVE_INTERVAL
import com.voidDeveloper.wastatussaver.data.utils.helpers.ScheduleAutoSave
import com.voidDeveloper.wastatussaver.domain.model.AutoSave
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
    lateinit var telegramLogUseCase: TelegramLogUseCase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context?, intent: Intent?) {
        val pendingResult = goAsync()

        Log.d("AutoSaveReceiver", "onReceive triggered")
        Log.d("AutoSaveReceiver", "Intent action: ${intent?.action}")

        scope.launch {
            try {
                when (intent?.action) {

                    Intent.ACTION_BOOT_COMPLETED -> {
                        Log.d("AutoSaveReceiver", "BOOT_COMPLETED received")
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

                            Log.d(
                                "AutoSaveReceiver",
                                "Last alarm time: $lastAlarmSetTimeMillis, Interval(min): $autoSaveInterval"
                            )

                            var timeToTrigger =
                                lastAlarmSetTimeMillis +
                                        TimeUnit.MINUTES.toMillis(autoSaveInterval.toLong())

                            if (timeToTrigger <= System.currentTimeMillis()) {
                                Log.d(
                                    "AutoSaveReceiver",
                                    "Calculated trigger time is in the past. Adjusting…"
                                )

                                timeToTrigger =
                                    System.currentTimeMillis() +
                                            TimeUnit.MINUTES.toMillis(autoSaveInterval.toLong())
                            }

                            Log.d(
                                "AutoSaveReceiver",
                                "Scheduling alarm at: $timeToTrigger"
                            )

                            scheduleAutoSave.scheduleAutoSaveWorkAlarm(timeToTrigger)
                        }
                    }

                    AUTO_SAVE_ACTION -> {
                        val isAutoSaveEnable = isAutoSaveEnable()
                        if (isAutoSaveEnable) {
                            Log.d("AutoSaveReceiver", "AUTO_SAVE_ACTION received")

                            val now = System.currentTimeMillis()
                            dataStorePreferenceManager.putPreference(
                                LAST_ALARM_SET_MILLIS_KEY,
                                now
                            )

                            Log.d(
                                "AutoSaveReceiver",
                                "Updated LAST_ALARM_SET_MILLIS_KEY = $now"
                            )

                            scheduleAutoSave.scheduleAutoSaveWorkManager()

                            Log.d(
                                "AutoSaveReceiver",
                                "WorkManager auto-save scheduled"
                            )
                        }
                    }

                    else -> {
                        Log.w(
                            "AutoSaveReceiver",
                            "Unhandled intent action: ${intent?.action}"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(
                    "AutoSaveReceiver",
                    "onReceive exception",
                    e
                )
            } finally {
                Log.d("AutoSaveReceiver", "Broadcast processing finished")
                pendingResult.finish()
            }
        }
    }

    private suspend fun getAutoSaveInterval(): Int {
        val userPrefAutoSave = dataStorePreferenceManager.getPreference(
            USER_PREF_AUTO_SAVE, defaultValue = ""
        ).first()
        val gson = Gson()
        val ufAutoSave = gson.fromJson(userPrefAutoSave, AutoSave::class.java)
        return ufAutoSave?.interval ?: DEFAULT_AUTO_SAVE_INTERVAL
    }

    private suspend fun isAutoSaveEnable(): Boolean {
        val userPrefAutoSave = dataStorePreferenceManager.getPreference(
            USER_PREF_AUTO_SAVE, defaultValue = ""
        ).first()
        val gson = Gson()
        val ufAutoSave = gson.fromJson(userPrefAutoSave, AutoSave::class.java)
        return ufAutoSave?.isAutoSaveEnable ?: false
    }

}