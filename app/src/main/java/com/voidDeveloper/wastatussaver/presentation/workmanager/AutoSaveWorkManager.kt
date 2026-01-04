package com.voidDeveloper.wastatussaver.presentation.workmanager

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.voidDeveloper.wastatussaver.R
import com.voidDeveloper.wastatussaver.data.datastore.proto.MediaType
import com.voidDeveloper.wastatussaver.data.prefdatastoremanager.DataStorePreferenceManager
import com.voidDeveloper.wastatussaver.data.prefdatastoremanager.DataStorePreferenceManagerImpl.DataStoreKeys.LAST_ALARM_SET_MILLIS_KEY
import com.voidDeveloper.wastatussaver.data.protodatastoremanager.AutoSaveProtoDataStoreManager
import com.voidDeveloper.wastatussaver.data.utils.Constants
import com.voidDeveloper.wastatussaver.data.utils.extentions.getInterval
import com.voidDeveloper.wastatussaver.data.utils.extentions.toTitle
import com.voidDeveloper.wastatussaver.data.utils.getMillisFromNow
import com.voidDeveloper.wastatussaver.data.utils.helpers.ScheduleAutoSave
import com.voidDeveloper.wastatussaver.domain.usecases.StatusesManagerUseCase
import com.voidDeveloper.wastatussaver.presentation.ui.main.ui.Title
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@HiltWorker
class AutoSaveWorkManager @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val dataStorePreferenceManager: DataStorePreferenceManager,
    private val autoSaveProtoDataStoreManager: AutoSaveProtoDataStoreManager,
    private val statusSaverManager: StatusesManagerUseCase,
    private val scheduleAutoSave: ScheduleAutoSave,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {

        return try {
            val preferredTitle = getPreferredTitle()

            val isAppInstalled = context.appInstalled(preferredTitle.packageName)
            if (isAppInstalled && !statusSaverManager.hasPermission(preferredTitle.uri)) {
                scheduleAutoSave.cancelAllAlarm()
                disableAutoSaveFeature()
                return Result.failure()
            } else if (!isAppInstalled) {
                scheduleAutoSave.cancelAllAlarm()
                disableAutoSaveFeature()
                return Result.failure()
            }

            val responsePair = saveStatusMedia(preferredTitle)

            dataStorePreferenceManager.putPreference(
                LAST_ALARM_SET_MILLIS_KEY,
                System.currentTimeMillis()
            )
            val res = if (!responsePair.first) {
                Result.failure()
            } else {
                if (responsePair.second > 0) {
                    sendNotification(
                        true,
                        "Saved ${responsePair.second} Statuses in Downloads"
                    )
                }
                Result.success()
            }
            val refreshInterval = getAutoSaveRefreshInterval()
            scheduleAutoSave.scheduleAutoSaveWorkAlarm(
                getMillisFromNow(
                    refreshInterval
                )
            )
            return res
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private suspend fun disableAutoSaveFeature() {
        autoSaveProtoDataStoreManager.updateAutoSaveUserPref { autoSaveUserPref ->
            autoSaveUserPref.enable = false
        }
    }

    private suspend fun getAutoSaveRefreshInterval(): Int {
        val autoSaveUserPref = autoSaveProtoDataStoreManager.readAutoSaveUserPref()
        val interval = autoSaveUserPref.getInterval()
        return interval
    }

    private suspend fun getAutoSaveMediaType(): List<MediaType> {
        val autoSaveUserPref = autoSaveProtoDataStoreManager.readAutoSaveUserPref()
        val mediaTypeList = autoSaveUserPref.mediaTypeList
        return mediaTypeList
    }

    private fun sendNotification(status: Boolean, message: String) {
        val notificationId = System.currentTimeMillis().toInt()

        val channel = NotificationChannel(
            Constants.AUTO_SAVE_NOTIFICATION_CHANNEL_ID,
            Constants.AUTO_SAVE_NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications for app status save updates"
            setSound(null, null)
            enableVibration(false)
        }
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        val builder =
            NotificationCompat.Builder(context, Constants.AUTO_SAVE_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(if (status) "Success" else "Error")
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_app_logo)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .setSound(null)
                .setVibrate(null)

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notify(notificationId, builder.build())
            }
        }
    }


    private suspend fun saveStatusMedia(preferredTitle: Title): Pair<Boolean, Int> {
        var counter = 0
        return try {
            val autoSaveMediaType = getAutoSaveMediaType()
            val files =
                statusSaverManager.getFiles(
                    preferredTitle.uri,
                    autoSaveMediaType
                )
            files.forEachIndexed { index, file ->
                val fileName = file.fileName ?: return@forEachIndexed
                val alreadyDownloaded =
                    statusSaverManager.isStatusDownloaded(fileName)
                if (!alreadyDownloaded) {
                    statusSaverManager.saveMediaFile(file)
                    counter++
                }
            }
            Pair(true, counter)
        } catch (e: Exception) {
            Pair(false, 0)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(context: Context, params: WorkerParameters): AutoSaveWorkManager
    }

    suspend fun getPreferredTitle(): Title {
        val preferredApp = autoSaveProtoDataStoreManager.readAutoSaveUserPref().app.toTitle()
        return preferredApp
    }

    fun Context.appInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

}

