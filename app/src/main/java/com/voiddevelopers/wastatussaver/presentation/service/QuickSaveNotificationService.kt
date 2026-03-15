package com.voiddevelopers.wastatussaver.presentation.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.gson.GsonBuilder
import com.voiddevelopers.wastatussaver.R
import com.voiddevelopers.wastatussaver.data.model.QuickSaveUserPref
import com.voiddevelopers.wastatussaver.data.prefdatastoremanager.DataStorePreferenceManager
import com.voiddevelopers.wastatussaver.data.prefdatastoremanager.DataStorePreferenceManagerImpl.DataStoreKeys.KEY_QUICK_SAVE_USER_PREF
import com.voiddevelopers.wastatussaver.data.utils.Constants.ACTION_FS_QUICK_SAVE_REMOVED
import com.voiddevelopers.wastatussaver.data.utils.Constants.ACTION_FS_QUICK_SAVE_SCAN
import com.voiddevelopers.wastatussaver.data.utils.Constants.ACTION_FS_QUICK_SAVE_STOP
import com.voiddevelopers.wastatussaver.data.utils.Constants.QUICK_SAVE_CHANNEL_ID
import com.voiddevelopers.wastatussaver.data.utils.Constants.QUICK_SAVE_NOTIFICATION_ID
import com.voiddevelopers.wastatussaver.domain.model.MediaType
import com.voiddevelopers.wastatussaver.domain.usecases.StatusesManagerUseCase
import com.voiddevelopers.wastatussaver.presentation.ui.main.ui.Title
import com.voiddevelopers.wastatussaver.presentation.ui.main.ui.TitleTypeAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class QuickSaveNotificationService : Service() {

    companion object {
        var isRunning = false
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    @Inject
    lateinit var statusSaverManager: StatusesManagerUseCase

    @Inject
    lateinit var dataStorePreferenceManager: DataStorePreferenceManager

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        isRunning = true
    }

    private fun createNotificationChannel() {
        val name = "Quick Save Channel"
        val descriptionText = "Channel for Quick Save Feature"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel =
            android.app.NotificationChannel(QUICK_SAVE_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_FS_QUICK_SAVE_SCAN -> handleScanAction(this)
            ACTION_FS_QUICK_SAVE_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }

            ACTION_FS_QUICK_SAVE_REMOVED -> showInitialNotification()
            else -> showInitialNotification()
        }
        return START_STICKY
    }

    private fun handleScanAction(context: Context) {
        val progressBuilder = getBaseNotificationBuilder()
            .setContentTitle("Scanning...")
            .setContentText("Scanning in progress")
            .setProgress(100, 0, true)

        startForegroundServiceCompatible(progressBuilder)

        serviceScope.launch {

            val mediaSavingJob = async(Dispatchers.IO) {
                startSavingMedia(context)
            }

            delay(1000)
            val message = mediaSavingJob.await()

            val successBuilder = getBaseNotificationBuilder()
                .setContentTitle("Notification")
                .setContentText(message)
                .setProgress(0, 0, false)

            updateNotification(successBuilder)

            delay(1000)
            showInitialNotification()

        }
    }

    private fun showInitialNotification() {
        val scanIntent = Intent(this, QuickSaveNotificationService::class.java).apply {
            action = ACTION_FS_QUICK_SAVE_SCAN
        }
        val scanPendingIntent = PendingIntent.getService(
            this, 0, scanIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = getBaseNotificationBuilder()
            .setContentTitle("Notification Action")
            .setContentText("Tap scan and save to begin")
            .addAction(android.R.drawable.ic_media_play, "Scan and save", scanPendingIntent)
            .addAction(0, "", null)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Cancel",
                getStopPendingIntent()
            )

        startForegroundServiceCompatible(builder)
    }

    private fun getStopPendingIntent(): PendingIntent {
        val stopIntent = Intent(this, QuickSaveNotificationService::class.java).apply {
            action = ACTION_FS_QUICK_SAVE_STOP
        }
        return PendingIntent.getService(
            this, 2, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getBaseNotificationBuilder(): NotificationCompat.Builder {
        val deleteIntent = Intent(this, QuickSaveNotificationService::class.java).apply {
            action = ACTION_FS_QUICK_SAVE_REMOVED
        }
        val deletePendingIntent = PendingIntent.getService(
            this, 1, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, QUICK_SAVE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setDeleteIntent(deletePendingIntent)
    }

    private fun updateNotification(builder: NotificationCompat.Builder) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(QUICK_SAVE_NOTIFICATION_ID, builder.build())
    }

    private fun startForegroundServiceCompatible(builder: NotificationCompat.Builder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                QUICK_SAVE_NOTIFICATION_ID,
                builder.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(QUICK_SAVE_NOTIFICATION_ID, builder.build())
        }
    }

    private suspend fun startSavingMedia(context: Context): String {
        if (!getQuickSaveable()) {
            return "Quick Save is Not Enabled"
        }
        val preferredTitle = getPreferredTitle()
        val isAppInstalled = context.appInstalled(preferredTitle.packageName)
        if (isAppInstalled && !statusSaverManager.hasPermission(preferredTitle.uri)) {
            return "Selected App has No Permission to Access Statuses Media Files"
        } else if (!isAppInstalled) {
            return "Selected App is Not Installed"
        }
        val responsePair = saveStatusMedia(preferredTitle)
        return if (!responsePair.first) {
            "Failed to Save Status Media Files"
        } else {
            if (responsePair.second > 0) {
                "${responsePair.second} Media Files has Saved"
            } else {
                "All Status Files Already Downloaded"
            }
        }
    }

    fun Context.appInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }


    private suspend fun saveStatusMedia(preferredTitle: Title): Pair<Boolean, Int> {
        var counter = 0
        return try {
            val quickSaveMediaType = getQuickSaveMediaType()

            val files = statusSaverManager.getFiles(
                preferredTitle.uri,
                quickSaveMediaType
            )

            files.forEach { file ->
                val fileName = file.fileName
                val alreadyDownloaded = statusSaverManager.isStatusDownloaded(fileName)

                if (!alreadyDownloaded) {
                    statusSaverManager.saveMediaFile(file, onSaveCompleted = {})
                    counter++
                }
            }

            Pair(true, counter)
        } catch (e: Exception) {
            Pair(false, 0)
        }
    }

    suspend fun getPreferredTitle(): Title {
        val json = dataStorePreferenceManager.getPreference(KEY_QUICK_SAVE_USER_PREF, "").first()
        if (json.isEmpty()) return Title.Whatsapp
        return try {
            val gson = GsonBuilder()
                .registerTypeHierarchyAdapter(Title::class.java, TitleTypeAdapter)
                .create()
            val pref = gson.fromJson(json, QuickSaveUserPref::class.java)
            pref.app ?: Title.Whatsapp
        } catch (e: Exception) {
            Title.Whatsapp
        }
    }

    private suspend fun getQuickSaveable(): Boolean {
        val json = dataStorePreferenceManager.getPreference(KEY_QUICK_SAVE_USER_PREF, "").first()
        if (json.isEmpty()) return false
        return try {
            val gson = GsonBuilder()
                .registerTypeHierarchyAdapter(Title::class.java, TitleTypeAdapter)
                .create()
            val pref = gson.fromJson(json, QuickSaveUserPref::class.java)
            pref.enable == true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun getQuickSaveMediaType(): List<MediaType> {
        val json = dataStorePreferenceManager.getPreference(KEY_QUICK_SAVE_USER_PREF, "").first()
        if (json.isEmpty()) return emptyList()
        return try {
            val gson = GsonBuilder()
                .registerTypeHierarchyAdapter(Title::class.java, TitleTypeAdapter)
                .create()
            val pref = gson.fromJson(json, QuickSaveUserPref::class.java)
            pref.mediaType ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceScope.cancel()
    }

}