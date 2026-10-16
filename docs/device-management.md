# Device Management & Provisioning Guide

## 1. Operating Modes Overview

The application supports two distinct operating tiers:

```
┌─────────────────────────────────┐      ┌──────────────────────────────────┐
│           Normal Mode           │      │        Device Owner Mode         │
│  - Physical sensor discovery    │      │  - Full Hardware Lockdown        │
│  - App-level permissions        │      │  - Camera system-wide disable    │
│  - Read-only capability audits  │      │  - Screen capture block          │
│  - Diagnostic reporting         │      │  - USB data transfer block       │
│  - Security alerts              │      │  - Bluetooth / Wi-Fi policy      │
│                                 │      │  - Uninstall protection          │
│  Limitations:                   │      │  - Location sharing restriction  │
│  Cannot enforce system-wide     │      │                                  │
│  restrictions on other apps.    │      │                                  │
└─────────────────────────────────┘      └──────────────────────────────────┘
```

---

## 2. Device Owner Provisioning via ADB

Android requires that Device Owner mode is provisioned on a **clean device** (factory reset or newly unboxed) with **no pre-existing Google or user accounts**.

### Step 1: Prepare Clean Test Device
1. Factory reset the target Android device.
2. Advance through the initial setup wizard without adding a Google account, email account, or secondary user.
3. Enable **Developer Options**:
   - Navigate to `Settings` -> `About phone`.
   - Tap `Build number` seven (7) times.
4. Enable **USB Debugging** in `Settings` -> `System` -> `Developer options`.

### Step 2: Install Lockdown Application
Connect the device to your development workstation and execute:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Step 3: Set Application as Device Owner
Execute the Android `dpm` command:
```bash
adb shell dpm set-device-owner com.sensorlockdown.app/.devicepolicy.LockdownDeviceAdminReceiver
```

### Expected Success Output:
```text
Success: Device owner set to package com.sensorlockdown.app
Active admin set to component {com.sensorlockdown.app/com.sensorlockdown.app.devicepolicy.LockdownDeviceAdminReceiver}
```

### Common Provisioning Errors & Troubleshooting:
- `java.lang.IllegalStateException: Not allowed to set the device owner because there are already some accounts on the device`:
  - **Resolution**: Remove all user accounts via `Settings` -> `Passwords & accounts` or perform a clean factory reset.
- `java.lang.IllegalStateException: Not allowed to set the device owner because there are already some users on the device`:
  - **Resolution**: Remove all guest or secondary user profiles.

---

## 3. QR Code & NFC Provisioning (Enterprise / Production)

For enterprise rollouts, the application can be provisioned via standard Android Enterprise enrollment mechanisms:

### QR Code Provisioning Format:
```json
{
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": "com.sensorlockdown.app/.devicepolicy.LockdownDeviceAdminReceiver",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": "https://enterprise.example.com/downloads/sensor-lockdown.apk",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM": "<SHA-256-CHECKSUM>",
  "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": false
}
```

---

## 4. Policy Enforcement Matrix

| Policy Target | API Mechanism | Enforceability | Verified Via |
|---|---|---|---|
| **Camera** | `dpm.setCameraDisabled()` | Fully Enforced (DO & DA) | `dpm.getCameraDisabled()` |
| **Screen Capture** | `dpm.setScreenCaptureDisabled()` | Fully Enforced (DO & DA) | `dpm.getScreenCaptureDisabled()` |
| **USB File Transfer** | `UserManager.DISALLOW_USB_FILE_TRANSFER` | Fully Enforced (DO) | `dpm.getUserRestrictions()` |
| **USB Debugging** | `UserManager.DISALLOW_DEBUGGING_FEATURES` | Fully Enforced (DO) | `dpm.getUserRestrictions()` |
| **Bluetooth** | `UserManager.DISALLOW_BLUETOOTH` | Fully Enforced (DO) | `dpm.getUserRestrictions()` |
| **Wi-Fi Config** | `UserManager.DISALLOW_CONFIG_WIFI` | Fully Enforced (DO) | `dpm.getUserRestrictions()` |
| **Location Sharing**| `UserManager.DISALLOW_SHARE_LOCATION` | Fully Enforced (DO) | `dpm.getUserRestrictions()` |
| **App Uninstall** | `dpm.setUninstallBlocked()` | Fully Enforced (DO) | `dpm.isUninstallBlocked()` |
| **Factory Reset** | `UserManager.DISALLOW_FACTORY_RESET` | Fully Enforced (DO) | `dpm.getUserRestrictions()` |
| **Safe Boot** | `UserManager.DISALLOW_SAFE_BOOT` | Fully Enforced (DO) | `dpm.getUserRestrictions()` |
| **Raw Sensors** | N/A (Hardware Bus) | **UNSUPPORTED in AOSP** | Honest System Disclosure |

