package com.sensorlockdown.launcher.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages persisted user preferences for the Secure Launcher, including whitelisted apps.
 */
class LauncherPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAllowedPackages(): Set<String> {
        return prefs.getStringSet(KEY_ALLOWED_PACKAGES, emptySet()) ?: emptySet()
    }

    fun setAllowedPackages(packages: Set<String>) {
        prefs.edit().putStringSet(KEY_ALLOWED_PACKAGES, packages).apply()
    }

    fun addAllowedPackage(packageName: String) {
        val current = getAllowedPackages().toMutableSet()
        current.add(packageName)
        setAllowedPackages(current)
    }

    fun removeAllowedPackage(packageName: String) {
        val current = getAllowedPackages().toMutableSet()
        current.remove(packageName)
        setAllowedPackages(current)
    }

    fun isPackageAllowed(packageName: String): Boolean {
        return getAllowedPackages().contains(packageName)
    }

    fun isInitialSetupCompleted(): Boolean {
        return prefs.getBoolean(KEY_SETUP_COMPLETED, false)
    }

    fun setInitialSetupCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_SETUP_COMPLETED, completed).apply()
    }

    companion object {
        private const val PREFS_NAME = "secure_launcher_app_prefs"
        private const val KEY_ALLOWED_PACKAGES = "allowed_packages"
        private const val KEY_SETUP_COMPLETED = "initial_setup_completed"
    }
}

