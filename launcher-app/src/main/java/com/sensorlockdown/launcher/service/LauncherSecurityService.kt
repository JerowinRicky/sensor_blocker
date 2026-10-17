package com.sensorlockdown.launcher.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.telecom.TelecomManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.sensorlockdown.launcher.data.LauncherPreferences
import com.sensorlockdown.launcher.ui.LauncherActivity

/**
 * Core security accessibility service for Secure Launcher.
 *
 * Enforces two primary system-level boundary restrictions:
 * 1. Blocks unauthorized application launches resulting from notification clicks, deep links,
 *    or quick settings shortcuts.
 * 2. Restricts Power Off / Restart actions until authenticated with the Master PIN.
 */
class LauncherSecurityService : AccessibilityService() {

    private lateinit var preferences: LauncherPreferences

    override fun onCreate() {
        super.onCreate()
        preferences = LauncherPreferences(this)
        instance = this
        Log.i(TAG, "LauncherSecurityService created")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 50L
        }
        serviceInfo = info
        Log.i(TAG, "LauncherSecurityService connected and configured")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkgName = event.packageName?.toString() ?: return
        val className = event.className?.toString() ?: ""

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleWindowStateChanged(pkgName, className, event)
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                handleViewClicked(pkgName, event)
            }
        }
    }

    private fun handleWindowStateChanged(
        packageName: String,
        className: String,
        event: AccessibilityEvent
    ) {
        // 1. Power Menu Detection
        if (isPowerMenuWindow(packageName, className, event)) {
            if (preferences.isPowerMenuProtected() && !isPowerAuthorized()) {
                Log.w(TAG, "Intercepted unauthorized Power Menu window: $className")
                dismissPowerMenu()
                requestPowerAuth()
                return
            }
        }

        // 2. Notification Shade / Quick Settings Restriction
        if (isNotificationShadeWindow(packageName, className)) {
            if (preferences.isNotificationDrawerBlocked() && isLauncherForegroundOrLocked()) {
                Log.i(TAG, "Collapsing notification shade while launcher is locked or active")
                dismissNotificationShade()
                return
            }
        }

        // 3. Unauthorized Package Interception (from notifications, deep links, etc.)
        if (!isPackageAllowed(packageName)) {
            Log.w(TAG, "Blocked unauthorized package launch attempt: $packageName")
            blockAndReturnToLauncher(packageName)
        }
    }

    private fun handleViewClicked(packageName: String, event: AccessibilityEvent) {
        val textContent = buildList {
            event.contentDescription?.let { add(it.toString()) }
            event.text.forEach { add(it.toString()) }
        }.joinToString(" ").lowercase()

        val isPowerAction = textContent.contains("power off") ||
                textContent.contains("restart") ||
                textContent.contains("reboot") ||
                textContent.contains("shut down") ||
                textContent.contains("turn off")

        if (isPowerAction && preferences.isPowerMenuProtected() && !isPowerAuthorized()) {
            Log.w(TAG, "Intercepted power action click: $textContent")
            dismissPowerMenu()
            requestPowerAuth()
        }
    }

    private fun isPackageAllowed(targetPackage: String): Boolean {
        // Always allow this launcher app (production or debug)
        if (targetPackage == packageName || targetPackage.startsWith("com.sensorlockdown.launcher")) {
            return true
        }

        // Always allow Android core UI and framework
        if (targetPackage == "android") {
            return true
        }

        // System UI is allowed (window/shade specifics handled separately)
        if (targetPackage == "com.android.systemui") {
            return true
        }

        // Whitelisted apps selected by the user
        if (preferences.isPackageAllowed(targetPackage)) {
            return true
        }

        // Allow active soft keyboard (IME)
        val defaultIme = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        val defaultImePkg = defaultIme?.split("/")?.firstOrNull()
        if (defaultImePkg != null && targetPackage == defaultImePkg) {
            return true
        }

        // Allow in-call UI for incoming or ongoing telephone calls
        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        try {
            if (telecomManager?.isInCall == true) {
                if (targetPackage.contains("incallui") ||
                    targetPackage.contains("dialer") ||
                    targetPackage.contains("telecom")
                ) {
                    return true
                }
            }
        } catch (_: SecurityException) {
            // Permission check fallback
        }

        // Allow package installer if user legitimately initiates uninstall from launcher
        if (targetPackage.contains("packageinstaller")) {
            return true
        }

        return false
    }

    private fun isPowerMenuWindow(
        packageName: String,
        className: String,
        event: AccessibilityEvent
    ): Boolean {
        if (packageName == "com.android.systemui" || packageName == "android") {
            if (className.contains("GlobalActions", ignoreCase = true) ||
                className.contains("PowerDialog", ignoreCase = true) ||
                className.contains("Shutdown", ignoreCase = true)
            ) {
                return true
            }

            val text = (event.contentDescription?.toString() ?: "") + " " + event.text.joinToString(" ")
            if (text.contains("Power off", ignoreCase = true) ||
                text.contains("Restart", ignoreCase = true) ||
                text.contains("Reboot", ignoreCase = true)
            ) {
                return true
            }
        }
        return false
    }

    private fun isNotificationShadeWindow(packageName: String, className: String): Boolean {
        if (packageName == "com.android.systemui") {
            return className.contains("NotificationShade", ignoreCase = true) ||
                    className.contains("NotificationPanel", ignoreCase = true) ||
                    className.contains("ShadeWindow", ignoreCase = true)
        }
        return false
    }

    private fun blockAndReturnToLauncher(blockedPackage: String) {
        performGlobalAction(GLOBAL_ACTION_HOME)

        val intent = Intent(this, LauncherActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(LauncherActivity.EXTRA_BLOCKED_PACKAGE, blockedPackage)
        }
        startActivity(intent)
    }

    private fun dismissPowerMenu() {
        performGlobalAction(GLOBAL_ACTION_BACK)
        try {
            @Suppress("DEPRECATION")
            sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
        } catch (_: Exception) {
            // Ignore on newer API restrictions
        }
    }

    private fun requestPowerAuth() {
        val intent = Intent(this, LauncherActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(LauncherActivity.EXTRA_REQUEST_POWER_AUTH, true)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        Log.w(TAG, "LauncherSecurityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        Log.i(TAG, "LauncherSecurityService destroyed")
    }

    companion object {
        private const val TAG = "LauncherSecurityService"

        @Volatile
        var instance: LauncherSecurityService? = null
            private set

        @Volatile
        private var powerAuthUntilTimestamp: Long = 0L

        @Volatile
        var isLauncherForegroundOrLockedState: Boolean = true

        fun isRunning(): Boolean = instance != null

        fun authorizePowerActions(durationMs: Long = 30_000L) {
            powerAuthUntilTimestamp = System.currentTimeMillis() + durationMs
            Log.i(TAG, "Power actions authorized until $powerAuthUntilTimestamp")
        }

        fun isPowerAuthorized(): Boolean {
            return System.currentTimeMillis() < powerAuthUntilTimestamp
        }

        fun revokePowerAuthorization() {
            powerAuthUntilTimestamp = 0L
        }

        fun triggerPowerDialog() {
            instance?.performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
        }

        fun dismissNotificationShade() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                instance?.performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
            } else {
                instance?.performGlobalAction(GLOBAL_ACTION_BACK)
            }
        }

        private fun isLauncherForegroundOrLocked(): Boolean {
            return isLauncherForegroundOrLockedState
        }
    }
}
