package com.voidDeveloper.wastatussaver.domain.usecases

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import javax.inject.Inject

class AppInstallCheckerUseCase @Inject constructor(
    private val appContext: Application,
) {
    fun isInstalled(packageName: String?): Boolean {
        if (packageName == null) {
            return false
        }
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0L)
                )
            } else {
                appContext.packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}