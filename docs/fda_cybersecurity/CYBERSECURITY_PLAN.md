# IronSide Connect — Cybersecurity Plan
**Document ID:** CSP-001  
**Version:** 1.0.0  
**Reference:** FDA "Cybersecurity in Medical Devices: Quality System Considerations and Content of Premarket Submissions" (September 2023)  
**Reference:** NIST SP 800-175B Rev. 1, NIST SP 800-63B  

---

## 1. Overview

This document describes the cybersecurity architecture of **IronSide Connect** in accordance with FDA pre-market cybersecurity guidance. It identifies threats, mitigations, and the Cybersecurity Bill of Materials (CBOM).

---

## 2. Threat Model (STRIDE)

### 2.1 Attack Surface

| Surface | Protocol | Exposure |
|---|---|---|
| BLE link | Bluetooth LE 4.2 | Local proximity only |
| Local storage | SQLite (encrypted) | On-device |
| Settings | DataStore (encrypted) | On-device |
| OTA firmware | HTTPS + cert pinning | Internet |
| Exported files | JSON in app-private storage | Controlled by user |

### 2.2 STRIDE Analysis

| Threat | Category | Mitigation |
|---|---|---|
| BLE replay attack | Spoofing | HMAC-SHA256 challenge-response per session; nonce prevents replay |
| BLE MITM | Tampering | BLE pairing with out-of-band confirmation; device-level auth |
| PHI data theft (device lost) | Info disclosure | SQLCipher encryption + Keystore-backed key; key invalidated on lock-screen removal |
| Screenshot of PHI | Info disclosure | `FLAG_SECURE` on all windows; OS-level screenshot block |
| Audit log tampering | Tampering | SHA-256 hash chain; any deletion/edit breaks the chain |
| Malicious firmware flash | Elevation of privilege | Signed firmware (ECDSA P-256); app verifies signature before OTA write |
| Backup exfiltration | Info disclosure | `android:allowBackup="false"`; `data-extraction-rules.xml` excludes all domains |
| Cleartext network traffic | Info disclosure | `network_security_config.xml` blocks all cleartext; cert pinning on API domain |
| App cloning / sideload | Elevation | APK signed with hardware-backed release key; Play Integrity API check at launch |

---

## 3. Implemented Controls

### 3.1 Authentication
- **Biometric / PIN** at app launch (Android BiometricPrompt)
- **Re-authentication** required on foreground resume (configurable, default ON)
- **Device-level auth** via HMAC-SHA256 challenge-response over BLE before any data flows

### 3.2 Authorization
- Single-user model per device install
- No remote access path; all data access is local

### 3.3 Cryptography
| Use | Algorithm | Key Length | Key Storage |
|---|---|---|---|
| Database encryption | AES-256-GCM (SQLCipher) | 256-bit | Android Keystore |
| BLE session HMAC | HMAC-SHA256 | 256-bit | Android Keystore |
| OTA signature verify | ECDSA P-256 | 256-bit | Bundled public cert |
| Audit chain hash | SHA-256 | N/A | Computed, stored |
| TLS to API | TLS 1.3 | RSA-2048 / ECDSA P-256 | Certificate pinning |

All keys generated inside **Android Keystore** (hardware-backed where StrongBox is available). Keys are **never exported** into application heap memory. Key material is **invalidated by biometric enrollment changes** (`setInvalidatedByBiometricEnrollment(true)`).

### 3.4 Data Protection
- PHI encrypted at rest via SQLCipher with Keystore-derived passphrase
- DataStore preferences encrypted with `EncryptedDataStore`
- `FLAG_SECURE` prevents screenshots and screen recording
- `allowBackup=false` prevents cloud backup of PHI
- Exported files written to app-private storage (`Context.filesDir`); sharing requires FileProvider + user intent

### 3.5 Secure Communication
- All API traffic over TLS 1.3; TLS 1.1 and below disabled
- Certificate pinning for `api.ironsidemedical.com` with backup pin
- Pins expire annually; rotation managed in CI with 30-day advance notice
- `network_security_config.xml` blocks cleartext across all domains

### 3.6 Audit Logging
- All security-relevant events are logged to an append-only, hash-chained audit table
- Log entries cannot be edited or deleted without breaking the hash chain
- Events logged include: all auth attempts, BLE connect/disconnect, data export, settings changes, firmware updates
- Logs retained on-device for 90 days; older entries archived on export

### 3.7 Software Bill of Materials (SBOM)
Full SBOM generated at build time in CycloneDX format. See `/build/outputs/sbom/`.

---

## 4. Vulnerability Management

- CVE monitoring via GitHub Dependabot for all direct and transitive dependencies
- Critical CVEs patched within 30 days; exploited-in-wild CVEs patched within 7 days
- Security patches released as hotfix builds outside the normal release cadence
- Coordinated disclosure policy: `security@ironsidemedical.com`; 90-day disclosure window

---

## 5. Post-Market Monitoring

- Crash reports (without PHI) collected via Firebase Crashlytics
- Security-relevant crashes trigger automated alert to security team
- Annual penetration test by qualified third party
- Device network traffic baseline monitored for anomalies

---

## 6. Labeling (FDA §524B)

Per 21st Century Cures Act §524B and FDA final rule, the following information is included in device labeling:
- List of all cybersecurity controls (this document, incorporated by reference)
- End-of-support date for each software version
- Process for reporting cybersecurity vulnerabilities
- OTA update instructions

---

## 7. Document History

| Version | Date | Author | Summary |
|---|---|---|---|
| 1.0.0 | 2026-04-29 | Engineering | Initial draft |
