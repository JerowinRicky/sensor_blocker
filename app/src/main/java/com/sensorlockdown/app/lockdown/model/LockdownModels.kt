package com.sensorlockdown.app.lockdown.model

enum class Category(val displayName: String) {
    PRIVACY_INPUTS("Privacy Inputs"),
    MOTION_ENVIRONMENTAL_SENSORS("Motion & Environmental Sensors"),
    CONNECTIVITY("Connectivity & Data"),
    HARDWARE_PERIPHERALS("Hardware & Peripherals"),
    SYSTEM_CONTROLS("System Controls & Integrity")
}

enum class ControlId(val displayName: String, val category: Category) {
    // Privacy Inputs
    CAMERA("Camera", Category.PRIVACY_INPUTS),
    MICROPHONE("Microphone", Category.PRIVACY_INPUTS),
    LOCATION("Location", Category.PRIVACY_INPUTS),

    // Motion & Environmental Sensors
    ACCELEROMETER("Accelerometer", Category.MOTION_ENVIRONMENTAL_SENSORS),
    GYROSCOPE("Gyroscope", Category.MOTION_ENVIRONMENTAL_SENSORS),
    MAGNETOMETER("Magnetometer", Category.MOTION_ENVIRONMENTAL_SENSORS),
    PROXIMITY("Proximity Sensor", Category.MOTION_ENVIRONMENTAL_SENSORS),
    LIGHT_SENSOR("Ambient Light Sensor", Category.MOTION_ENVIRONMENTAL_SENSORS),
    BAROMETER("Barometer / Pressure", Category.MOTION_ENVIRONMENTAL_SENSORS),

    // Connectivity
    BLUETOOTH("Bluetooth", Category.CONNECTIVITY),
    WIFI("Wi-Fi Configuration", Category.CONNECTIVITY),
    NFC("NFC", Category.CONNECTIVITY),
    USB_DATA("USB Data / Debugging", Category.CONNECTIVITY),

    // Hardware / Peripherals
    IR_BLASTER("Infrared (IR) Transmitter", Category.HARDWARE_PERIPHERALS),
    SPEAKER("Audio Output / Speaker", Category.HARDWARE_PERIPHERALS),

    // System Controls
    SCREEN_CAPTURE("Screen Capture / Recording", Category.SYSTEM_CONTROLS),
    APP_INSTALLATION("App Installation", Category.SYSTEM_CONTROLS),
    APP_UNINSTALLATION("App Removal Protection", Category.SYSTEM_CONTROLS),
    FACTORY_RESET("Factory Reset Restriction", Category.SYSTEM_CONTROLS),
    SAFE_BOOT("Safe Boot Restriction", Category.SYSTEM_CONTROLS)
}

enum class EnforcementStatus(val label: String) {
    ENFORCED("Enforced"),
    PARTIALLY_ENFORCED("Partially Enforced"),
    REQUESTED("Requested"),
    UNSUPPORTED("Unsupported"),
    NOT_AVAILABLE("Not Available"),
    NOT_PRESENT("Not Present"),
    ERROR("Error")
}

enum class OverallLockdownStatus(val label: String) {
    INACTIVE("Lockdown Inactive"),
    FULLY_ACTIVE("Lockdown Fully Active"),
    PARTIALLY_ACTIVE("Lockdown Partially Active"),
    FAILED("Lockdown Activation Failed")
}

data class ControlPolicy(
    val controlId: ControlId,
    val title: String,
    val description: String,
    val requestedState: Boolean,
    val supportedState: Boolean,
    val enforcedState: Boolean,
    val status: EnforcementStatus,
    val reason: String? = null,
    val lastVerifiedAt: Long = 0L
)

data class LockdownPolicy(
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = false,
    val controls: Map<ControlId, ControlPolicy> = emptyMap(),
    val overallStatus: OverallLockdownStatus = OverallLockdownStatus.INACTIVE
)

