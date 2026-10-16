package com.sensorlockdown.app.devicepolicy

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.UserManager

/**
 * Encapsulates official Android Enterprise DevicePolicyManager and UserManager capabilities.
 * Every policy application is verified using native Android system queries.
 */
class DevicePolicyController(private val context: Context) {

    private val dpm: DevicePolicyManager? =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager

    private val userManager: UserManager? =
        context.getSystemService(Context.USER_SERVICE) as? UserManager

    val adminComponent: ComponentName = LockdownDeviceAdminReceiver.getComponentName(context)

    fun getManagementState(): DeviceManagementState {
        val manager = dpm ?: return DeviceManagementState.NORMAL_APP
        return when {
            manager.isDeviceOwnerApp(context.packageName) -> DeviceManagementState.DEVICE_OWNER
            manager.isProfileOwnerApp(context.packageName) -> DeviceManagementState.PROFILE_OWNER
            manager.isAdminActive(adminComponent) -> DeviceManagementState.DEVICE_ADMIN
            else -> DeviceManagementState.NORMAL_APP
        }
    }

    fun isDeviceOwner(): Boolean {
        return dpm?.isDeviceOwnerApp(context.packageName) == true
    }

    fun isAdminActive(): Boolean {
        return dpm?.isAdminActive(adminComponent) == true
    }

    /**
     * Creates system intent to prompt user to activate Device Administrator directly on their phone.
     */
    fun createAddDeviceAdminIntent(): android.content.Intent {
        return android.content.Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Activate Device Administrator to allow Device Lockdown to disable camera hardware and enforce system-wide screen capture protection."
            )
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    // ==========================================
    // Camera Policy
    // ==========================================
    fun setCameraDisabled(disabled: Boolean): Boolean {
        val manager = dpm ?: return false
        if (!isAdminActive()) return false
        return try {
            manager.setCameraDisabled(adminComponent, disabled)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun isCameraDisabled(): Boolean {
        val manager = dpm ?: return false
        if (!isAdminActive()) return false
        return try {
            manager.getCameraDisabled(adminComponent)
        } catch (e: Exception) {
            false
        }
    }

    // ==========================================
    // Screen Capture Policy
    // ==========================================
    fun setScreenCaptureDisabled(disabled: Boolean): Boolean {
        val manager = dpm ?: return false
        if (!isAdminActive()) return false
        return try {
            manager.setScreenCaptureDisabled(adminComponent, disabled)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun isScreenCaptureDisabled(): Boolean {
        val manager = dpm ?: return false
        if (!isAdminActive()) return false
        return try {
            manager.getScreenCaptureDisabled(adminComponent)
        } catch (e: Exception) {
            false
        }
    }

    // ==========================================
    // USB Restrictions (File Transfer & Debugging)
    // ==========================================
    fun setUsbRestrictions(restricted: Boolean): Boolean {
        if (!isDeviceOwner()) return false
        val manager = dpm ?: return false
        return try {
            if (restricted) {
                manager.addUserRestriction(adminComponent, UserManager.DISALLOW_USB_FILE_TRANSFER)
                manager.addUserRestriction(adminComponent, UserManager.DISALLOW_DEBUGGING_FEATURES)
            } else {
                manager.clearUserRestriction(adminComponent, UserManager.DISALLOW_USB_FILE_TRANSFER)
                manager.clearUserRestriction(adminComponent, UserManager.DISALLOW_DEBUGGING_FEATURES)
            }
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun isUsbRestricted(): Boolean {
        return hasUserRestriction(UserManager.DISALLOW_USB_FILE_TRANSFER)
    }

    // ==========================================
    // Bluetooth Restriction
    // ==========================================
    fun setBluetoothRestricted(restricted: Boolean): Boolean {
        if (!isDeviceOwner()) return false
        val manager = dpm ?: return false
        return try {
            if (restricted) {
                manager.addUserRestriction(adminComponent, UserManager.DISALLOW_BLUETOOTH)
                manager.addUserRestriction(adminComponent, UserManager.DISALLOW_BLUETOOTH_SHARING)
            } else {
                manager.clearUserRestriction(adminComponent, UserManager.DISALLOW_BLUETOOTH)
                manager.clearUserRestriction(adminComponent, UserManager.DISALLOW_BLUETOOTH_SHARING)
            }
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun isBluetoothRestricted(): Boolean {
        return hasUserRestriction(UserManager.DISALLOW_BLUETOOTH)
    }

    // ==========================================
    // Wi-Fi Config Restriction
    // ==========================================
    fun setWifiConfigRestricted(restricted: Boolean): Boolean {
        if (!isDeviceOwner()) return false
        val manager = dpm ?: return false
        return try {
            if (restricted) {
                manager.addUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_WIFI)
            } else {
                manager.clearUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_WIFI)
            }
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun isWifiConfigRestricted(): Boolean {
        return hasUserRestriction(UserManager.DISALLOW_CONFIG_WIFI)
    }

    // ==========================================
    // Location Sharing Restriction
    // ==========================================
    fun setLocationRestricted(restricted: Boolean): Boolean {
        if (!isDeviceOwner()) return false
        val manager = dpm ?: return false
        return try {
            if (restricted) {
                manager.addUserRestriction(adminComponent, UserManager.DISALLOW_SHARE_LOCATION)
            } else {
                manager.clearUserRestriction(adminComponent, UserManager.DISALLOW_SHARE_LOCATION)
            }
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun isLocationRestricted(): Boolean {
        return hasUserRestriction(UserManager.DISALLOW_SHARE_LOCATION)
    }

    // ==========================================
    // App Install / Uninstall Restrictions
    // ==========================================
    fun setAppInstallUninstallRestricted(restricted: Boolean): Boolean {
        if (!isDeviceOwner()) return false
        val manager = dpm ?: return false
        return try {
            if (restricted) {
                manager.addUserRestriction(adminComponent, UserManager.DISALLOW_INSTALL_APPS)
                manager.addUserRestriction(adminComponent, UserManager.DISALLOW_UNINSTALL_APPS)
            } else {
                manager.clearUserRestriction(adminComponent, UserManager.DISALLOW_INSTALL_APPS)
                manager.clearUserRestriction(adminComponent, UserManager.DISALLOW_UNINSTALL_APPS)
            }
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun isAppInstallRestricted(): Boolean {
        return hasUserRestriction(UserManager.DISALLOW_INSTALL_APPS)
    }

    // ==========================================
    // Uninstall Block for this App
    // ==========================================
    fun setUninstallBlocked(blocked: Boolean): Boolean {
        if (!isDeviceOwner()) return false
        val manager = dpm ?: return false
        return try {
            manager.setUninstallBlocked(adminComponent, context.packageName, blocked)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun isUninstallBlocked(): Boolean {
        val manager = dpm ?: return false
        if (!isDeviceOwner()) return false
        return try {
            manager.isUninstallBlocked(adminComponent, context.packageName)
        } catch (e: Exception) {
            false
        }
    }

    // ==========================================
    // Factory Reset Restriction
    // ==========================================
    fun setFactoryResetRestricted(restricted: Boolean): Boolean {
        if (!isDeviceOwner()) return false
        val manager = dpm ?: return false
        return try {
            if (restricted) {
                manager.addUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET)
            } else {
                manager.clearUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET)
            }
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun isFactoryResetRestricted(): Boolean {
        return hasUserRestriction(UserManager.DISALLOW_FACTORY_RESET)
    }

    // ==========================================
    // Safe Boot Restriction
    // ==========================================
    fun setSafeBootRestricted(restricted: Boolean): Boolean {
        if (!isDeviceOwner()) return false
        val manager = dpm ?: return false
        return try {
            if (restricted) {
                manager.addUserRestriction(adminComponent, UserManager.DISALLOW_SAFE_BOOT)
            } else {
                manager.clearUserRestriction(adminComponent, UserManager.DISALLOW_SAFE_BOOT)
            }
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun isSafeBootRestricted(): Boolean {
        return hasUserRestriction(UserManager.DISALLOW_SAFE_BOOT)
    }

    private fun hasUserRestriction(restrictionKey: String): Boolean {
        val manager = dpm ?: return false
        return try {
            if (isAdminActive()) {
                val bundle = manager.getUserRestrictions(adminComponent)
                bundle.getBoolean(restrictionKey, false)
            } else {
                userManager?.hasUserRestriction(restrictionKey) == true
            }
        } catch (e: Exception) {
            false
        }
    }

    fun removeActiveAdmin(): Boolean {
        val manager = dpm ?: return false
        return try {
            if (isAdminActive()) {
                manager.removeActiveAdmin(adminComponent)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}

