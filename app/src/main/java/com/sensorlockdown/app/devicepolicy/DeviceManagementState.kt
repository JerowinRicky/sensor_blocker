package com.sensorlockdown.app.devicepolicy

enum class DeviceManagementState(val displayName: String, val description: String) {
    DEVICE_OWNER(
        "Device Owner (Fully Managed)",
        "Highest management tier. Enforces system-wide hardware, camera, connectivity, and uninstall restrictions."
    ),
    PROFILE_OWNER(
        "Profile Owner (Work Profile)",
        "Work profile tier. Can manage policies within work container only."
    ),
    DEVICE_ADMIN(
        "Device Administrator",
        "Legacy enterprise tier. Supports camera disable, screen capture disable, and password policies."
    ),
    NORMAL_APP(
        "Standard Application (Unmanaged)",
        "Operating as a regular Android application. System-wide hardware restrictions cannot be enforced."
    )
}

