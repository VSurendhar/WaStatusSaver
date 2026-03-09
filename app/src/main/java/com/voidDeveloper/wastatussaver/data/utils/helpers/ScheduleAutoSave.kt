package com.voidDeveloper.wastatussaver.data.utils.helpers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.voidDeveloper.wastatussaver.data.utils.Constants.AUTO_SAVE_ACTION
import com.voidDeveloper.wastatussaver.data.utils.Constants.AUTO_SAVE_WORK_MANAGER_NAME
import com.voidDeveloper.wastatussaver.data.utils.Constants.REQUEST_CODE_ALARM_WIDGET_REFRESH
import com.voidDeveloper.wastatussaver.presentation.receivers.AutoSaveReceiver
import com.voidDeveloper.wastatussaver.presentation.workmanager.AutoSaveWorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ScheduleAutoSave @Inject constructor(
    @ApplicationContext private val context: Context,
//    private val telegramLogUseCase: TelegramLogUseCase,
) {

    fun scheduleAutoSaveWorkManager() {

        val workRequest = OneTimeWorkRequestBuilder<AutoSaveWorkManager>().setConstraints(
            Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build()
        ).build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            AUTO_SAVE_WORK_MANAGER_NAME, ExistingWorkPolicy.KEEP, workRequest
        )

    }

    fun scheduleAutoSaveWorkAlarm(triggerAtMillis: Long) {
        /*
                val scope = CoroutineScope(Dispatchers.IO)
                scope.launch {
                    val message = "Trigger time millis: ${formatTime(triggerAtMillis)}"
                    val escapedMessage = escapeForMarkdownV2(message)
                    telegramLogUseCase.sendLogs(escapedMessage)
                }
        */

        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AutoSaveReceiver::class.java).apply {
            action = AUTO_SAVE_ACTION
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM_WIDGET_REFRESH,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        cancelAllAlarm()

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
        )

    }

    fun cancelAllAlarm() {
        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AutoSaveReceiver::class.java).apply {
            action = AUTO_SAVE_ACTION
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM_WIDGET_REFRESH,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.cancel(pendingIntent)

    }


}