package com.sensorlockdown.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.sensorlockdown.app.ui.MainActivity

/**
 * Accessibility Service designed to protect the application from unauthorized external uninstallation
 * from the Android App Drawer, Home Screen Launcher, or System Settings.
 *
 * When an external uninstallation or deactivation attempt targeting this app is detected,
 * it immediately cancels the dialog and presents the 6-digit PIN verification modal.
 */
class UninstallProtectionAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // If an authorized uninstallation was unlocked with PIN recently, permit it to proceed
        if (isUninstallAuthorized()) {
            return
        }

        val packageName = event.packageName?.toString() ?: return
        if (!isPackageInstallerOrSettings(packageName)) return

        val rootNode = rootInActiveWindow ?: return
        if (containsOurAppAndUninstallKeyword(rootNode)) {
            // Immediately dismiss uninstaller dialog / return to home
            performGlobalAction(GLOBAL_ACTION_BACK)

            // Bring up our app to prompt for PIN
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(EXTRA_PROMPT_UNINSTALL_PIN, true)
            }
            startActivity(intent)
        }
    }

    override fun onInterrupt() {}

    private fun isPackageInstallerOrSettings(packageName: String): Boolean {
        val lower = packageName.lowercase()
        return lower.contains("packageinstaller") ||
               lower.contains("settings") ||
               lower.contains("securitycenter") ||
               lower.contains("launcher")
    }

    private fun containsOurAppAndUninstallKeyword(node: AccessibilityNodeInfo): Boolean {
        var mentionsOurApp = false
        var mentionsUninstallOrDeactivate = false

        fun traverse(n: AccessibilityNodeInfo?) {
            if (n == null) return
            val text = n.text?.toString()?.lowercase() ?: ""
            val desc = n.contentDescription?.toString()?.lowercase() ?: ""

            if (text.contains("device lockdown") || text.contains("sensor lockdown") || text.contains("sensor_blocker") ||
                desc.contains("device lockdown") || desc.contains("sensor lockdown") || desc.contains("sensor_blocker")
            ) {
                mentionsOurApp = true
            }

            if (text.contains("uninstall") || text.contains("deactivate") || text.contains("delete") || text.contains("remove") ||
                desc.contains("uninstall") || desc.contains("deactivate") || desc.contains("delete") || desc.contains("remove")
            ) {
                mentionsUninstallOrDeactivate = true
            }

            for (i in 0 until n.childCount) {
                traverse(n.getChild(i))
            }
        }

        traverse(node)
        return mentionsOurApp && mentionsUninstallOrDeactivate
    }

    companion object {
        const val EXTRA_PROMPT_UNINSTALL_PIN = "PROMPT_UNINSTALL_PIN"

        private var authorizedUntilMillis: Long = 0L

        fun authorizeUninstall(durationMillis: Long = 60_000L) {
            authorizedUntilMillis = System.currentTimeMillis() + durationMillis
        }

        fun isUninstallAuthorized(): Boolean {
            return System.currentTimeMillis() < authorizedUntilMillis
        }

        fun isServiceEnabled(context: Context): Boolean {
            val expectedComponentName = "${context.packageName}/${UninstallProtectionAccessibilityService::class.java.name}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.contains(expectedComponentName) || enabledServices.contains(context.packageName)
        }

        fun createOpenAccessibilitySettingsIntent(): Intent {
            return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }
}
