package com.sensorlockdown.launcher.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * DeviceAdminReceiver for Secure Launcher.
 * Prevents outside uninstallation and provides device lock capabilities.
 */
class LauncherAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i(TAG, "Secure Launcher Device Administrator enabled")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.w(TAG, "Secure Launcher Device Administrator disabled")
    }

    companion object {
        private const val TAG = "LauncherAdminReceiver"
    }
}
