package com.sensorlockdown.launcher.data

import android.graphics.drawable.Drawable

/**
 * Represents an installed application discovered on the device.
 */
data class AppInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: Drawable? = null,
    val isAllowed: Boolean = false,
    val isSystemApp: Boolean = false
)
