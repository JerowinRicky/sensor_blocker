# Supported Devices & OEM Compatibility Matrix

## 1. Overview of Platform Variations

Android hardware controls depend heavily on the underlying OEM implementation, Android OS version, and whether Google Play Services or proprietary enterprise frameworks (e.g., Samsung Knox) are present.

The application adheres strictly to standard AOSP Device Policy Controller APIs. Where an OEM deviates or disables standard enterprise capabilities, the capability scanner detects and reports this state honestly.

---

## 2. Tested Device Matrix

| Device Model | Android Version | Device Owner Supported | Camera Control | Microphone Privacy | Location Control | Bluetooth Restriction | NFC Control | Raw Sensor Control | Known Limitations |
|---|---|---|---|---|---|---|---|---|---|
| **Google Pixel 7 / 8 / 9** | Android 13 - 15 | Yes (Native AOSP) | Enforced | Enforced (Android 12+ SensorPrivacyManager) | Enforced | Enforced | Unsupported (AOSP lack of system API) | Unsupported (No AOSP bus disable API) | Reference baseline behavior. |
| **Samsung Galaxy S22 / S23 / S24** | One UI 5 - 6 (Android 13 - 14) | Yes (Knox + AOSP) | Enforced | Enforced | Enforced | Enforced | Unsupported without Knox SDK | Unsupported without Knox SDK | Knox background service optimizations must be disabled for boot receiver. |
| **Motorola Edge 40 / 50** | Android 13 - 14 | Yes (Near-Stock) | Enforced | Enforced | Enforced | Enforced | Unsupported | Unsupported | Stock AOSP behavior. |
| **OnePlus 11 / 12** | OxygenOS 13 - 14 | Yes | Enforced | Partially Enforced | Enforced | Enforced | Unsupported | Unsupported | Aggressive OEM battery killers require manual exclusion. |
| **Xiaomi / Redmi / Poco** | HyperOS / MIUI 14 | Partial (Requires custom settings) | Enforced | Partially Enforced | Enforced | Enforced | Unsupported | Unsupported | "MIUI Optimization" setting interferes with standard DPC provisioning; must be turned off during ADB setup. |

---

## 3. Sensor Bus vs. Sensor Privacy API

### The Hardware Reality of Raw Sensors:
- Motion and environmental sensors (Accelerometer, Gyroscope, Magnetometer, Barometer) operate on shared internal hardware buses ($I^2C$ / SPI).
- In standard Android AOSP, there is **no public API** to cut power or zero out readings system-wide for non-system apps without low-level OEM kernel customization or root permissions.
- Developer toggles (e.g. Android Quick Settings "Sensors Off" tile) rely on hidden private system permissions (`android.permission.MANAGE_SENSOR_PRIVACY`) reserved exclusively for the system UI and system server.
- **The Lockdown Application will never claim raw sensors are disabled when only app-local listeners are detached.** When raw sensor lockdown is requested, the system reports `UNSUPPORTED: Android does not expose a public device-wide API for disabling this sensor on this device.`

---

## 4. OEM-Specific Provisioning Quirks

### Samsung Devices:
Samsung Knox adds additional security layers. Standard AOSP `dpm set-device-owner` works reliably after factory reset. Knox Enterprise policies may override certain restrictions if managed by external MDMs.

### Xiaomi / HyperOS Devices:
1. Enable `USB debugging (Security settings)` in Developer options (requires SIM card and Mi Account on some firmware).
2. Disable `MIUI optimization` in Developer options if the command errors with `SecurityException`.
3. Execute the ADB command immediately after initial device setup.

