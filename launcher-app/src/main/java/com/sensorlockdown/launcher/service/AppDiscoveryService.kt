package com.sensorlockdown.launcher.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.sensorlockdown.launcher.data.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service responsible for discovering installed applications, launching approved apps,
 * and handling system home launcher settings.
 */
class AppDiscoveryService(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    suspend fun loadInstalledApps(allowedPackages: Set<String>): List<AppInfo> = withContext(Dispatchers.IO) {
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(0)
            )
        } else {
            packageManager.queryIntentActivities(launcherIntent, 0)
        }

        val apps = mutableListOf<AppInfo>()
        val seenPackages = mutableSetOf<String>()

        for (resolveInfo in resolveInfos) {
            val pkg = resolveInfo.activityInfo.packageName
            // Exclude our own launcher from being listed in the drawer
            if (pkg == context.packageName || seenPackages.contains(pkg)) {
                continue
            }
            seenPackages.add(pkg)

            val label = resolveInfo.loadLabel(packageManager).toString()
            val icon = try {
                resolveInfo.loadIcon(packageManager)
            } catch (e: Exception) {
                null
            }
            val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            apps.add(
                AppInfo(
                    packageName = pkg,
                    activityName = resolveInfo.activityInfo.name,
                    label = label,
                    icon = icon,
                    isAllowed = allowedPackages.contains(pkg),
                    isSystemApp = isSystem
                )
            )
        }

        apps.sortedBy { it.label.lowercase() }
    }

    fun launchApp(packageName: String, activityName: String? = null): Boolean {
        return try {
            val intent = if (!activityName.isNullOrEmpty()) {
                Intent().apply {
                    component = ComponentName(packageName, activityName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                packageManager.getLaunchIntentForPackage(packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            if (intent != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun requestUninstall(packageName: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Opens the system Home app selection screen to allow changing the default launcher.
     */
    fun openDefaultHomeSettings(): Boolean {
        return try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(Settings.ACTION_HOME_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            } catch (ignored: Exception) {
                false
            }
        }
    }
}
