package com.sensorlockdown.launcher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sensorlockdown.launcher.service.LauncherSecurityService
import com.sensorlockdown.launcher.ui.LauncherActivity

/**
 * Ensures the Secure Launcher is triggered immediately upon system boot,
 * enforcing PIN entry on startup or after device restart.
 */
class LauncherBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.i(TAG, "Device boot event received: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            LauncherSecurityService.isLauncherForegroundOrLockedState = true

            val launchIntent = Intent(context, LauncherActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(LauncherActivity.EXTRA_LOCKED_ON_BOOT, true)
            }
            context.startActivity(launchIntent)
        }
    }

    companion object {
        private const val TAG = "LauncherBootReceiver"
    }
}
