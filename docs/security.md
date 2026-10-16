# Security Architecture & Implementation Standards

## 1. Principles of Operation

1. **Honest State Enforcement**: No mock toggles or client-only assertions. Every control must reflect true device-level verification.
2. **Zero Plaintext Credentials**: Credentials (PINs and Recovery tokens) are never stored or logged in plaintext.
3. **Defense in Depth**: Multiple security layers (Keystore, PBKDF2, rate limiting, signature permissions, audit logging, window security).
4. **Local-First Isolation**: No reliance on remote backend servers for core authentication or lockdown enforcement.
5. **Least Privilege**: Only necessary Android permissions are declared.

---

## 2. Cryptographic Architecture

### 2.1 PIN Derivation & Storage
- **Algorithm**: PBKDF2WithHmacSHA256 (Password-Based Key Derivation Function 2 with HMAC-SHA-256).
- **Work Factor**: 100,000 iterations.
- **Salt Generation**: 32 bytes (256 bits) from `java.security.SecureRandom`.
- **Derived Key Length**: 256 bits.
- **Verification**: Constant-time byte array comparison (`MessageDigest.isEqual`) to eliminate timing-attack side-channels.

### 2.2 Keystore Integration
- Master encryption keys are generated inside the Android Keystore system:
  - Key Alias: `SensorLockdownMasterKey`
  - Purpose: `KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT`
  - Block Mode: `KeyProperties.BLOCK_MODE_GCM`
  - Encryption Padding: `KeyProperties.ENCRYPTION_PADDING_NONE`
  - Key Size: 256 bits (AES)

---

## 3. Rate Limiting & Anti-Brute Force

| Failed Attempts | Action / Lockout Duration | Audit Action |
|---|---|---|
| 1 – 4 | Normal retry with incremental warning | `LOGIN_FAILED` recorded |
| 5 – 9 | 30-second exponential lockout | `RATE_LIMIT_WARNING` |
| 10 – 14 | 5-minute lockout | `SECURITY_LOCKOUT_TRIGGERED` |
| 15+ | 30-minute lockout + Emergency Recovery Prompt | `EXCESSIVE_FAILURES_FLAGGED` |

Lockout timestamps and failed counts are securely tracked in persistent storage with monotonically increasing counter checks.

---

## 4. Screen & Memory Protections

- **FLAG_SECURE**:
  `window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)`
  Enforced on all authentication, recovery, and security setting screens. Blocks OS screen capture, screen casting, video recording, and task switcher thumbnail previews.
- **Clipboard Isolation**:
  Sensitive credentials (PINs, raw recovery material) are explicitly blocked from clipboard operations to prevent leakage through third-party clipboard monitors.
- **Android Backup Restriction**:
  `android:allowBackup="false"` and `android:fullBackupContent="false"` configured in `AndroidManifest.xml` to prevent ADB backup extraction or automated cloud sync of encrypted credential stores.

---

## 5. Secure Inter-Process Communication (IPC)

The emergency recovery companion app interacts with the lockdown system via an authenticated IPC channel:

1. **Signature Permission**:
   ```xml
   <permission
       android:name="com.sensorlockdown.app.permission.EMERGENCY_RECOVERY"
       android:protectionLevel="signature" />
   ```
2. **Mutual Identity Verification**:
   The `EmergencyRecoveryProvider` verifies caller identity at runtime by comparing the calling application's signing certificate with the hosting application's certificate via `PackageManager.hasSigningEngine` / `SigningInfo`.
3. **Dedicated Recovery Secret**:
   The primary 6-digit PIN is irreversible. Recovery functions use a cryptographically distinct emergency credential generated during setup.
4. **Audit Trail**:
   Every access attempt emits an immutable `EMERGENCY_ACCESS_ATTEMPT` audit log with caller package verification status.

---

## 6. Audit Logging Standards

All security-sensitive operations are appended to an immutable Room audit table:
- Events logged:
  - `PIN_CREATED`
  - `PIN_CHANGED`
  - `LOGIN_SUCCESS`
  - `LOGIN_FAILED`
  - `LOCKDOWN_ACTIVATED`
  - `LOCKDOWN_DEACTIVATED`
  - `POLICY_APPLIED`
  - `POLICY_FAILED`
  - `POLICY_VERIFIED`
  - `DEVICE_REBOOT_DETECTED`
  - `EMERGENCY_ACCESS`
  - `UNAUTHORIZED_ATTEMPT`
  - `DEVICE_OWNER_STATUS_CHANGED`
- **Sanitization Guarantee**: Plaintext PINs, recovery passwords, and encryption keys are strictly scrubbed prior to log serialization.
