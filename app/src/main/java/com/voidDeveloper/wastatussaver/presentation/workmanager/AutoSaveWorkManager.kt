package com.voidDeveloper.wastatussaver.presentation.workmanager

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.voidDeveloper.wastatussaver.R
import com.voidDeveloper.wastatussaver.data.datastoremanager.DataStoreManager
import com.voidDeveloper.wastatussaver.data.datastoremanager.DataStoreManager.DataStoreKeys.KEY_PREFERRED_TITLE
import com.voidDeveloper.wastatussaver.data.datastoremanager.DataStoreManager.DataStoreKeys.LAST_ALARM_SET_MILLIS_KEY
import com.voidDeveloper.wastatussaver.data.datastoremanager.DataStorePreferenceManager
import com.voidDeveloper.wastatussaver.data.utils.Constants
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
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {

        return try {
            val preferredTitle = getPreferredTitle()

            if (context.appInstalled(preferredTitle.packageName) && !statusSaverManager.hasPermission(
                    preferredTitle.uri
                )
            ) {
                sendNotification(
                    status = false,
                    message = "Tied to Auto Save Status. Failed Due to Unfulfilled Permission"
                )
                return Result.failure()
            } else if (!context.appInstalled(preferredTitle.packageName)) {
                sendNotification(
                    status = false, message = "Tied to Auto Save Status. Failed Due to ${
                        context.getString(
                            preferredTitle.resId
                        )
                    } App Not Installed"
                )
                return Result.failure()
            }
            Log.d("WorkManagerStatus", "Work started")

            val responsePair = saveStatusMedia(preferredTitle)

            dataStorePreferenceManager.putPreference(
                LAST_ALARM_SET_MILLIS_KEY, System.currentTimeMillis()
            )

            if (!responsePair.first) {
                return Result.failure()
            } else {
                sendNotification(true, "Saved ${responsePair.second} Statuses in Downloads")
                return Result.success()
            }


        } catch (e: Exception) {
            Log.d("WorkManagerStatus", "Exception in doWork", e)
            Result.failure()
        }

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
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val builder =
            NotificationCompat.Builder(context, Constants.AUTO_SAVE_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(if (status) "Success" else "Error")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

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
            val files = statusSaverManager.getFiles(preferredTitle.uri, shouldRefresh = true)
            files.forEach { file ->
                file.fileName?.let { fileName ->
                    if (!statusSaverManager.isStatusDownloaded(fileName)) {
                        statusSaverManager.saveMediaFile(file)
                        counter++
                    }
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

