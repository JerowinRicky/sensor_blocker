# Production-Grade Android Device Privacy & Hardware Lockdown System

![Android Build CI](https://github.com/organization/android-sensor-lockdown/actions/workflows/android-build.yml/badge.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-blue.svg)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-green.svg)
![Min SDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-orange.svg)
![Target SDK](https://img.shields.io/badge/Target%20SDK-34%20(Android%2014)-brightgreen.svg)
![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)

A production-grade, security-hardened Android application designed to enforce device-wide hardware, sensor, and privacy lockdowns using native Android Enterprise (`DevicePolicyManager`), Android Keystore, Jetpack Compose, and modern Android SDK capabilities.

---

## 1. Core Principles

> [!IMPORTANT]
> **No Fake Security**: Every toggle in this application either enforces a real operating system restriction via official Android APIs or explicitly discloses that the control is unsupported or partially supported. The application never claims a hardware sensor is disabled merely by detaching an in-app listener.

### Status Classifications:
* **`ENFORCED`**: The policy was verified via Android system APIs after application.
* **`PARTIALLY_ENFORCED`**: A platform-level restriction was applied, but some attack vectors or background paths cannot be completely cut off by AOSP.
* **`REQUESTED`**: The user requested the policy, awaiting system execution.
* **`UNSUPPORTED`**: Android AOSP or the hardware OEM does not expose a public device-wide disable API for this component.
* **`NOT_AVAILABLE`**: The device hardware component is disabled or restricted at the kernel level.
* **`NOT_PRESENT`**: The physical sensor was not found during hardware enumeration.
* **`ERROR`**: An unexpected security or platform exception occurred during policy application.

---

## 2. Features

* **6-Digit Strict Numeric PIN**:
  * Mandatory 6-digit confirmation with interactive show/hide toggle.
  * PBKDF2WithHmacSHA256 (100,000 iterations) with 32-byte cryptographically secure salt.
  * Android Keystore master key encryption.
  * Zero plaintext credentials across database, logs, preferences, or memory dumps.
* **Intelligent Rate Limiting**:
  * Exponential backoff for failed PIN attempts (30s after 5 failures, 5m after 10 failures).
  * Monotonically increasing lockout timers persisted across app restarts.
* **Device & Sensor Capability Discovery**:
  * Real-time hardware enumeration via `SensorManager.getSensorList(Sensor.TYPE_ALL)`.
  * Accurate identification of physical presence for accelerometer, gyroscope, magnetometer, barometer, proximity, ambient light, etc.
  * Clear explanation of Android AOSP sensor bus limitations.
* **Device Policy Controller (DPC) & Device Owner**:
  * Real hardware enforcement for Camera, Screen Capture, USB file transfer, USB debugging, Bluetooth, Location sharing, and Application Removal.
* **Lockdown Engine**:
  * Transaction-like application with post-enforcement verification.
  * Composite health assessment: `FULLY_ACTIVE`, `PARTIALLY_ACTIVE`, or `FAILED`.
* **Tamper-Evident Local Audit Trail**:
  * Structured security event logging (Room database).
  * Plaintext credentials strictly sanitized.
* **Window & Screen Protections**:
  * `FLAG_SECURE` prevents screenshots, screen recording, and task switcher preview leaks.
* **Reboot Resilience**:
  * `BootReceiver` verifies active device policies upon device reboot without continuous battery-draining services.
* **Companion Emergency Recovery App**:
  * Standalone companion recovery application (`emergency-app`).
  * Dedicated high-entropy emergency credential distinct from the primary PIN.
  * Inter-Process Communication (IPC) protected by signature-level permissions and mutual signing certificate validation.

---

## 3. Architecture Overview

```text
Presentation Layer (Jetpack Compose + Material 3)
         │
         ▼
ViewModel (StateFlow Streams)
         │
         ▼
Domain / Use Cases (LockdownEngine, PinManager, CapabilityEvaluator)
         │
         ├───────────────────────────────┐
         ▼                               ▼
Repositories (Policy & Audit)    Device Policy Layer (DPC)
         │                               │
         ▼                               ▼
Secure Storage (Room + Keystore)  Android Framework (APIs & Sensors)
```

Detailed architectural specifications are located in [`docs/architecture.md`](docs/architecture.md).

---

## 4. Getting Started & Development Setup

### Prerequisites
- JDK 17+
- Android SDK with Platform 34
- Git

### Clone and Build
```bash
git clone https://github.com/organization/android-sensor-lockdown.git
cd android-sensor-lockdown

# Run Unit Tests
./gradlew test

# Run Lint
./gradlew lint

# Build Debug APKs
./gradlew assembleDebug
```

---

## 5. Provisioning Device Owner on a Test Device

To exercise full device-wide hardware lockdown, the application must be designated as the Android **Device Owner**.

```bash
# 1. Install debug APK
adb install -r -g app/build/outputs/apk/debug/app-debug.apk

# 2. Designate application as Device Owner (requires clean device with no accounts)
adb shell dpm set-device-owner com.sensorlockdown.app/.devicepolicy.LockdownDeviceAdminReceiver
```

For detailed troubleshooting and OEM notes (Google Pixel, Samsung Knox, Xiaomi, Motorola), refer to [`docs/device-management.md`](docs/device-management.md) and [`docs/supported-devices.md`](docs/supported-devices.md).

---

## 6. Security & Threat Model

For details on brute-force mitigations, IPC authorization, screen capture defenses, and root detection, refer to [`docs/threat-model.md`](docs/threat-model.md) and [`docs/security.md`](docs/security.md).

---

## 7. License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.
