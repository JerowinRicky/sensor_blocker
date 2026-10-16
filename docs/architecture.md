# Architecture Specification

## 1. System Overview

The **Android Hardware, Sensor & Device Privacy Lockdown System** is designed with a defense-in-depth, local-first clean architecture. The system differentiates between requested policies and actually verified operating system state, enforcing hardware and sensor restrictions via native Android Enterprise (Device Policy Controller / Device Owner) and privacy frameworks.

```
┌────────────────────────────────────────────────────────┐
│                   Presentation Layer                   │
│   Jetpack Compose UI (Material 3) + ViewModels         │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│                   Domain / Use Cases                   │
│   Lockdown Engine | Auth Service | Capability Evaluator│
└─────────────┬───────────────────────────┬──────────────┘
              │                           │
┌─────────────▼─────────────┐ ┌───────────▼──────────────┐
│       Repositories        │ │     Device Policy Layer  │
│  PolicyRepo | AuditRepo   │ │   DevicePolicyManager    │
└─────────────┬─────────────┘ │   SensorPrivacyManager   │
              │               └───────────┬──────────────┘
┌─────────────▼─────────────┐             │
│      Secure Storage       │             │
│  Keystore | Room | EncPrefs│             │
└───────────────────────────┘             │
              │                           │
┌─────────────▼───────────────────────────▼──────────────┐
│           Android Framework & Hardware Bus             │
└────────────────────────────────────────────────────────┘
```

## 2. Core Architectural Components

### 2.1 UI / Presentation Layer
- **Jetpack Compose & Material 3**: Declarative UI state models. No fake animations or misleading UI toggles.
- **FLAG_SECURE**: Dynamically attached to window managers on security-sensitive screens (PIN creation, PIN verification, Recovery credential screens) to prevent screenshots, screen recordings, and recent-app previews.
- **ViewModels**: Expose read-only `StateFlow` streams. UI reacts only to verified system states.

### 2.2 Authentication & Credential Subsystem
- **6-Digit Strict Numeric PIN**: Strictly enforced length and numeric constraints.
- **Cryptographic Key Derivation**: PBKDF2WithHmacSHA256 with 100,000 iterations and a cryptographically random 32-byte salt generated via `java.security.SecureRandom`.
- **Zero Plaintext Storage**: Plaintext PIN never touches disk, SQLite, SharedPreferences, logs, crash reports, or clipboard.
- **Hardware-Backed Protection**: Master encryption keys stored in Android Keystore (`AndroidKeyStore` provider) using AES-256-GCM.
- **Configurable Rate Limiting**: Exponential backoff upon repeated failures (attempts 1–4: normal retry; attempt 5: 30s lockout; attempt 10: 5m lockout; repeated failures scale exponentially).

### 2.3 Device & Sensor Discovery Subsystem
- **Hardware Enumeration**: Dynamic inspection of physical sensors via `SensorManager.getSensorList(Sensor.TYPE_ALL)`.
- **Honest Capability Reporting**: Distinguishes between physical presence (`PRESENT` / `NOT_PRESENT`) and system controllability.
- **AOSP Sensor Bus Reality**: Android AOSP does not provide a public, third-party system-wide API to halt physical sensor buses (e.g. raw I2C/SPI accelerometer, gyroscope registers) without root or OEM kernel modifications. The system honestly labels these controls as `UNSUPPORTED` with detailed technical explanations rather than faking security by merely unregistering the app's own listener.

### 2.4 Device Policy Controller (DPC) & Management
- **Device Owner Detection**: Checks `DevicePolicyManager.isDeviceOwnerApp()`.
- **Profile Owner Detection**: Checks `DevicePolicyManager.isProfileOwnerApp()`.
- **Device Admin Receiver**: Responds to system policy events (`ACTION_DEVICE_ADMIN_ENABLED`, `ACTION_DEVICE_ADMIN_DISABLE_REQUESTED`).
- **Enforced System Policies**:
  - Camera: `setCameraDisabled(admin, true)`
  - Screen Capture: `setScreenCaptureDisabled(admin, true)`
  - USB Data Transfer: `addUserRestriction(admin, UserManager.DISALLOW_USB_FILE_TRANSFER)`
  - USB Debugging: `addUserRestriction(admin, UserManager.DISALLOW_DEBUGGING_FEATURES)`
  - Bluetooth: `addUserRestriction(admin, UserManager.DISALLOW_BLUETOOTH)`
  - Wi-Fi Config: `addUserRestriction(admin, UserManager.DISALLOW_CONFIG_WIFI)`
  - Location Sharing: `addUserRestriction(admin, UserManager.DISALLOW_SHARE_LOCATION)`
  - Application Removal Block: `setUninstallBlocked(admin, packageName, true)`
  - Factory Reset Restriction: `addUserRestriction(admin, UserManager.DISALLOW_FACTORY_RESET)`
  - Safe Boot Restriction: `addUserRestriction(admin, UserManager.DISALLOW_SAFE_BOOT)`

### 2.5 Lockdown Engine
- **Transaction-Like Execution**:
  1. Read requested policy specification.
  2. Evaluate hardware & management capabilities.
  3. Sequentially apply enforceable restrictions.
  4. Immediately query Android APIs to verify actual enforcement.
  5. Calculate composite health state (`FULLY_ACTIVE`, `PARTIALLY_ACTIVE`, `FAILED`).
  6. Atomically persist verified state to local storage.
  7. Write tamper-evident audit record.

### 2.6 Persistence & Boot Recovery
- **Room Database**: Structured persistence for policy state and immutable audit logs.
- **Encrypted Preferences**: Stores cryptographic salts, password verification hashes, and rate-limiting lockout state.
- **BootCompletedReceiver**: Listens for `ACTION_BOOT_COMPLETED`. Re-evaluates verified system policies on startup without running battery-draining continuous background services.

### 2.7 Emergency Recovery Companion Subsystem
- **Separate Companion App**: `Lockdown Emergency` (`com.sensorlockdown.emergency`).
- **Cryptographic Separation**: Emergency recovery uses a dedicated high-entropy recovery secret generated at initial setup, distinct from the primary authentication PIN.
- **Secure IPC**: Inter-Process Communication via protected `ContentProvider` requiring:
  - Custom signature-level permission (`protectionLevel="signature"`).
  - Mutual certificate fingerprint verification (`PackageManager.hasSigningEngine` / `SigningInfo`).
  - Strict audit logging of every emergency access attempt.
