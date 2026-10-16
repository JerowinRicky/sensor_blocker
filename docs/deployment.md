# Enterprise Deployment & Build Guide

## 1. Build Prerequisites

- **Java Development Kit**: JDK 17 or higher (JDK 17 recommended for Android Gradle Plugin 8.4+).
- **Android SDK**: API Level 34 (Android 14) or API Level 35 (Android 15) compile SDK.
- **Gradle**: 8.7+ (provided via `gradlew` wrapper).
- **Target Architecture**: `arm64-v8a`, `armeabi-v7a`, `x86_64`.

---

## 2. CLI Build Commands

### Clean Workspace:
```bash
# Linux / macOS
./gradlew clean

# Windows
gradlew.bat clean
```

### Run Unit & Security Test Suites:
```bash
# Linux / macOS
./gradlew test

# Windows
gradlew.bat test
```

### Run Code Quality & Android Lint:
```bash
# Linux / macOS
./gradlew lint

# Windows
gradlew.bat lint
```

### Build Debug APK:
```bash
# Linux / macOS
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```
*Outputs generated at:*
- `app/build/outputs/apk/debug/app-debug.apk`
- `emergency-app/build/outputs/apk/debug/emergency-app-debug.apk`

### Build Production Release:
```bash
# Linux / macOS
./gradlew assembleRelease
./gradlew bundleRelease

# Windows
gradlew.bat assembleRelease
gradlew.bat bundleRelease
```

---

## 3. Production Signing Configuration

Release builds must be signed with a production keystore configured via environment variables. **Never check keystore files or passwords into version control.**

### Required Environment Variables:
```bash
export ANDROID_KEYSTORE_PATH="/secure/path/to/release.jks"
export ANDROID_KEYSTORE_PASSWORD="<master-keystore-password>"
export ANDROID_KEY_ALIAS="sensorlockdown"
export ANDROID_KEY_PASSWORD="<alias-password>"
```

---

## 4. Device Fleet Deployment Options

### Option A: Clean ADB Provisioning (Development & Staging)
```bash
# Install main application
adb install -r -g app/build/outputs/apk/debug/app-debug.apk

# Install companion emergency recovery application
adb install -r -g emergency-app/build/outputs/apk/debug/emergency-app-debug.apk

# Set main application as Device Owner
adb shell dpm set-device-owner com.sensorlockdown.app/.devicepolicy.LockdownDeviceAdminReceiver
```

### Option B: Zero-Touch / QR Code Enrollment (Enterprise Production)
Enterprise MDMs (Google Workspace, Microsoft Intune, VMware Workspace ONE) can distribute the signed release APK during initial zero-touch enrollment.
