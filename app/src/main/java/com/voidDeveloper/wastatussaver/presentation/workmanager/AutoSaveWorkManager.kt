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
import com.google.gson.Gson
import com.voidDeveloper.wastatussaver.R
import com.voidDeveloper.wastatussaver.data.datastoremanager.DataStoreManager.DataStoreKeys.KEY_PREFERRED_TITLE
import com.voidDeveloper.wastatussaver.data.datastoremanager.DataStoreManager.DataStoreKeys.LAST_ALARM_SET_MILLIS_KEY
import com.voidDeveloper.wastatussaver.data.datastoremanager.DataStoreManager.DataStoreKeys.USER_PREF_AUTO_SAVE
import com.voidDeveloper.wastatussaver.data.datastoremanager.DataStorePreferenceManager
import com.voidDeveloper.wastatussaver.data.utils.Constants
import com.voidDeveloper.wastatussaver.data.utils.Constants.DEFAULT_AUTO_SAVE_INTERVAL
import com.voidDeveloper.wastatussaver.data.utils.getMillisFromNow
import com.voidDeveloper.wastatussaver.data.utils.helpers.ScheduleAutoSave
import com.voidDeveloper.wastatussaver.domain.model.AutoSave
import com.voidDeveloper.wastatussaver.domain.usecases.StatusesManagerUseCase
import com.voidDeveloper.wastatussaver.presentation.ui.main.ui.Title
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class AutoSaveWorkManager @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val dataStorePreferenceManager: DataStorePreferenceManager,
    private val statusSaverManager: StatusesManagerUseCase,
    private val scheduleAutoSave: ScheduleAutoSave,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {

        return try {
            val preferredTitle = getPreferredTitle()

            val isAppInstalled = context.appInstalled(preferredTitle.packageName)
            if (isAppInstalled && !statusSaverManager.hasPermission(preferredTitle.uri)) {
                sendNotification(
                    status = false,
                    message = "Tied to Auto Save Status. Failed Due to Unfulfilled Permission"
                )
                return Result.failure()
            } else if (!isAppInstalled) {
                val appName = context.getString(preferredTitle.resId)
                sendNotification(
                    status = false,
                    message = "Tied to Auto Save Status. Failed Due to $appName App Not Installed"
                )
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
                sendNotification(
                    true,
                    "Saved ${responsePair.second} Statuses in Downloads"
                )
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

    private suspend fun getAutoSaveRefreshInterval(): Int {
        val userPrefAutoSave = dataStorePreferenceManager.getPreference(
            USER_PREF_AUTO_SAVE, defaultValue = ""
        ).first()
        val gson = Gson()
        val ufAutoSave = gson.fromJson(userPrefAutoSave, AutoSave::class.java)
        return ufAutoSave?.interval ?: DEFAULT_AUTO_SAVE_INTERVAL
    }

    private fun sendNotification(status: Boolean, message: String) {
        val notificationId = System.currentTimeMillis().toInt()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
        }

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
            val files =
                statusSaverManager.getFiles(
                    preferredTitle.uri,
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

    fun Context.appInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

}

