package com.sensorlockdown.app.security

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.io.File

/**
 * Checks device integrity and developer environment state.
 * Honestly discloses indicators of root or USB debugging.
 * Does not falsely claim infallibility against sophisticated kernel root-cloaks.
 */
class DeviceIntegrityChecker(private val context: Context) {

    data class IntegrityResult(
        val isRootSuspected: Boolean,
        val isAdbEnabled: Boolean,
        val isTestKeysBuild: Boolean,
        val findings: List<String>
    )

    fun checkIntegrity(): IntegrityResult {
        val findings = mutableListOf<String>()

        // 1. Check build tags
        val isTestKeys = Build.TAGS != null && Build.TAGS.contains("test-keys")
        if (isTestKeys) {
            findings.add("OS build signed with test-keys (unofficial or custom ROM).")
        }

        // 2. Check known su binary paths
        val suPaths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        var suBinaryFound = false
        for (path in suPaths) {
            if (File(path).exists()) {
                suBinaryFound = true
                findings.add("SU binary detected at: $path")
                break
            }
        }

        // 3. Check ADB / Developer debugging state
        val adbEnabled = try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.ADB_ENABLED,
                0
            ) == 1
        } catch (e: Exception) {
            false
        }
        if (adbEnabled) {
            findings.add("USB Debugging (ADB) is currently enabled.")
        }

        return IntegrityResult(
            isRootSuspected = isTestKeys || suBinaryFound,
            isAdbEnabled = adbEnabled,
            isTestKeysBuild = isTestKeys,
            findings = findings
        )
    }
}

