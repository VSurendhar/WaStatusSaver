package com.voiddevelopers.wastatussaver.presentation.ui.backup

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.InputStreamContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.gson.Gson
import com.voiddevelopers.wastatussaver.data.model.BackUpUserPref
import com.voiddevelopers.wastatussaver.data.prefdatastoremanager.DataStorePreferenceManager
import com.voiddevelopers.wastatussaver.data.prefdatastoremanager.DataStorePreferenceManagerImpl.DataStoreKeys.KEY_BACK_UP_USER_PREF
import com.voiddevelopers.wastatussaver.data.utils.Constants.QUICK_BACKUP_CHANNEL_ID
import com.voiddevelopers.wastatussaver.data.utils.Constants.QUICK_BACKUP_NOTIFICATION_ID
import com.voiddevelopers.wastatussaver.domain.model.BackupInterval
import com.voiddevelopers.wastatussaver.domain.model.MediaFile
import com.voiddevelopers.wastatussaver.domain.model.MediaType
import com.voiddevelopers.wastatussaver.domain.model.toMediaFile
import com.voiddevelopers.wastatussaver.domain.usecases.SavedMediaHandlingUserCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted params: WorkerParameters,
    val savedMediaHandlingUserCase: SavedMediaHandlingUserCase,
    val dataStorePreferenceManager: DataStorePreferenceManager,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val isExpedited = inputData.getBoolean("is_expedited", false)

            if (isExpedited) {
                try {
                    setForeground(getForegroundInfo())
                } catch (e: Exception) {
                }
            }

            performBackup(context)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = createNotification()
        return ForegroundInfo(
            QUICK_BACKUP_NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }


    private suspend fun performBackup(context: Context) {
        val mediaFiles = savedMediaHandlingUserCase.getSavedMediaFiles().map {
            it.toMediaFile()
        }

        val selectedMediaTypesStrings = inputData.getStringArray("selectedMediaTypes")

        val selectedMediaTypes = selectedMediaTypesStrings
            ?.mapNotNull {
                try {
                    enumValueOf<MediaType>(it)
                } catch (e: Exception) {
                    null
                }
            }
            ?: emptyList()

        val filteredMediaFiles =
            mediaFiles.filter { mediaFile -> selectedMediaTypes.contains(mediaFile.mediaType) }

        val account = GoogleSignIn.getLastSignedInAccount(context)

        if (account != null) {
            filteredMediaFiles.forEach {
                uploadFile(context, account, it)
            }
        } else {
            dataStorePreferenceManager.putPreference(
                KEY_BACK_UP_USER_PREF,
                Gson().toJson(BackUpUserPref(allowBackUp = false))
            )
        }
    }

    private fun uploadFile(
        context: Context,
        account: GoogleSignInAccount,
        mediaFile: MediaFile,
    ) {
        try {
            val mimeType = context.contentResolver.getType(mediaFile.uri)
                ?: when (mediaFile.mediaType) {
                    MediaType.IMAGE -> "image/*"
                    MediaType.VIDEO -> "video/*"
                    MediaType.AUDIO -> "audio/*"
                    else -> "*/*"
                }

            val credential = GoogleAccountCredential
                .usingOAuth2(context, listOf(DriveScopes.DRIVE_FILE))
            credential.selectedAccount = account.account

            val driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("WaStatusSaver").build()

            val fileName = mediaFile.fileName
            val fileMetadata = File().apply {
                name = fileName
                this.mimeType = mimeType
            }

            val inputStream = context.contentResolver.openInputStream(mediaFile.uri)
                ?: run {
                    return
                }
            val mediaContent = InputStreamContent(mimeType, inputStream)

            driveService.files()
                .create(fileMetadata, mediaContent)
                .setFields("id, name, webViewLink")
                .execute()

        } catch (e: GoogleJsonResponseException) {
        } catch (e: IOException) {
        } catch (e: Exception) {
        }
    }

    private fun createNotification(): Notification {
        val channel = NotificationChannel(
            QUICK_BACKUP_CHANNEL_ID,
            "Backup Notification",
            NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager =
            applicationContext.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        return NotificationCompat.Builder(applicationContext, QUICK_BACKUP_CHANNEL_ID)
            .setContentTitle("Backup in Progress")
            .setContentText("Your backup is running...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setProgress(100, 0, true)
            .build()
    }

}


class BackupWorkerScheduler @Inject constructor() {

    private val TAG = "Surendhar TAG"

    fun cancelAllWorkers(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("periodic_backup_work")
    }

    fun setExpeditedWorker(context: Context, selectedMediaTypes: List<MediaType>) {
        Log.i(TAG, "setExpeditedWorker: ")
        val expeditedRequest = OneTimeWorkRequestBuilder<BackupWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(
                workDataOf(
                    "selectedMediaTypes" to selectedMediaTypes.map { it.name }.toTypedArray(),
                    "is_expedited" to true
                )
            )
            .build()

        WorkManager.getInstance(context).enqueue(expeditedRequest)
    }

    fun setPeriodicWorker(
        context: Context,
        interval: BackupInterval,
        selectedMediaTypes: List<MediaType>,
        canUseCellular: Boolean,
    ) {
        Log.i(TAG, "setPeriodicWorker: ")
        val builder = when (interval) {
            BackupInterval.DAILY -> PeriodicWorkRequestBuilder<BackupWorker>(24, TimeUnit.HOURS)
            BackupInterval.WEEKLY -> PeriodicWorkRequestBuilder<BackupWorker>(
                24 * 7,
                TimeUnit.HOURS
            )

            BackupInterval.MONTHLY -> PeriodicWorkRequestBuilder<BackupWorker>(
                24 * 28,
                TimeUnit.HOURS
            )
        }

        val networkType = if (canUseCellular) {
            NetworkType.CONNECTED
        } else {
            NetworkType.UNMETERED
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(networkType)
            .build()

        val periodicRequest = builder
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    "selectedMediaTypes" to selectedMediaTypes.map { it.name }.toTypedArray(),
                    "is_expedited" to false
                )
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "periodic_backup_work",
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicRequest
        )
    }

}