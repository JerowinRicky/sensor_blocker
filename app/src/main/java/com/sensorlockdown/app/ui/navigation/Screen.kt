package com.sensorlockdown.app.ui.navigation

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object PinSetup : Screen("pin_setup")
    object PinLogin : Screen("pin_login")
    object Dashboard : Screen("dashboard")
    object LockdownResult : Screen("lockdown_result")
    object SecuritySettings : Screen("security_settings")
    object AuditLogs : Screen("audit_logs")
    object DeviceManagementInfo : Screen("device_management_info")
}

