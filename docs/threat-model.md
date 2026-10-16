# Security Threat Model

## 1. System Scope & Boundaries

The threat model evaluates the resilience of the **Android Hardware & Sensor Lockdown System** against unauthorized deactivation, credential extraction, IPC hijacking, physical access attacks, and OEM firmware edge cases.

---

## 2. Threat Analysis Matrix

| Threat ID | Threat Vector | Risk Level | Mitigation Architecture | Residual Risk |
|---|---|---|---|---|
| **T-01** | **Brute-force PIN Attacks** (Automated or manual rapid PIN entry) | **High** | Exponential rate limiting (30s after 5 tries, 5m after 10 tries). PIN verification uses PBKDF2WithHmacSHA256 (100,000 rounds) + constant-time comparison. | Physical possession allows repeated attempts across days if device rebooted, but rate limiter persists lockouts to disk. |
| **T-02** | **Credential Extraction from Storage** (Inspection of databases, XMLs, backups) | **Critical** | Zero plaintext storage. PIN hash stored with unique cryptographically random salt. Android Auto Backup and ADB backup disabled. Master keys stored in hardware-backed Keystore. | Compromise of TEE/Keystore via zero-day kernel exploits on unpatched firmware. |
| **T-03** | **Screen Scraping / Recent Apps Preview** (Malicious apps recording screen or reading task switcher) | **Medium** | `FLAG_SECURE` enforced on all authentication, setup, and recovery windows. OS blocks screenshots and screen recording. | External physical cameras pointed directly at the physical display screen. |
| **T-04** | **Clipboard Snooping** (Malicious background clipboard listeners) | **Medium** | Explicit blocking of automatic copy/paste for PINs and recovery credentials. Show/hide password visually on-screen rather than copying to clipboard. | User manually transcribing credential into an untrusted third-party note-taking app. |
| **T-05** | **Malicious Companion App / IPC Spoofing** (Rogue app attempting to query emergency recovery data) | **Critical** | Custom `protectionLevel="signature"` permission on IPC endpoints. Runtime verification of caller package signing certificates against the host app signing certificate. | Rogue application installed with identical signing key (compromised developer keystore). |
| **T-06** | **Unauthorized Uninstallation** (User or attacker attempts to drag app to uninstall) | **High** | Device Owner mode enables `dpm.setUninstallBlocked(admin, packageName, true)`. Operating system disables the uninstall button in Settings and launcher. | In normal mode (non-managed), standard Android allows user uninstall. Managed mode required for full protection. |
| **T-07** | **Physical Factory Reset / Recovery Mode** (Attacker boots into hardware bootloader/recovery to wipe) | **High** | Device Owner applies `UserManager.DISALLOW_FACTORY_RESET`. Disables settings-based factory reset. | Hardware recovery mode wiping (Fastboot / Bootloader key combination) clears user data and ownership. |
| **T-08** | **Device Reboot Bypass** (Attacker reboots phone hoping policies reset to default) | **High** | Android Enterprise policies (`DevicePolicyManager`) are persistent across reboots at the OS level. `BootCompletedReceiver` verifies policies on boot and updates health state. | Brief window during early OS boot sequence before DPC services initialize. |
| **T-09** | **ADB / Developer Tampering** (Attacker with USB access executes adb commands) | **High** | Device Owner applies `UserManager.DISALLOW_DEBUGGING_FEATURES` during lockdown, shutting down ADB interface. Detection of USB debugging on security dashboard. | If attacker already has authorized ADB session prior to lockdown activation and doesn't apply debugging restriction. |
| **T-10** | **Rooted / Compromised OS Environment** (Compromised kernel or Magisk/KernelSU installed) | **Critical** | Device integrity checks detect su binaries, test-keys builds, and dangerous mount points. Clear status warning shown on dashboard. | Sophisticated root cloaking (e.g. Zygisk Shamiko) hiding modifications from user-space checks. |
| **T-11** | **Logcat Credential Leakage** (Sensitive tokens printed to Android system logs) | **High** | Strict lint rules and custom logging abstractions prevent credentials from being logged. ProGuard/R8 strips debug logs in release builds. | OEM-specific framework bugs logging input events in custom keyboard software. |
