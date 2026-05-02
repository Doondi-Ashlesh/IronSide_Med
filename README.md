# IronSide Connect

Android companion application for IronSide Medical devices.

**IEC 62304 Safety Class:** B  
**Software Version:** 1.0.0  
**Min Android:** 8.0 (API 26)  

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│               Presentation Layer                    │
│  Jetpack Compose screens + Hilt ViewModels          │
│  Dashboard · DeviceScan · VitalHistory · AuditLog  │
└───────────────────┬─────────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────────┐
│                Domain Layer                         │
│  Use Cases · Domain Models · Repository interfaces  │
│  (zero Android dependencies — fully unit-testable)  │
└───────────────────┬─────────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────────┐
│                 Data Layer                          │
│  BleGattManager · Room + SQLCipher · Keystore       │
│  AuditRepository · VitalSignsRepository             │
└─────────────────────────────────────────────────────┘
```

## Key Capabilities

### Bluetooth Hardware Integration
- BLE device discovery with `SCAN_MODE_LOW_LATENCY` and device-name prefix filter
- Full GATT lifecycle: connect → MTU negotiation → service discovery → auth → notify
- HMAC-SHA256 device-level authentication before any data is consumed
- Foreground `Service` keeps session alive when app is backgrounded
- Coroutine-based GATT callback bridge — no raw threads, clean cancellation

### Medical Device Software (IEC 62304 Class B)
- Clean Architecture: Domain layer has zero Android dependencies
- All async code uses structured concurrency (Coroutines + Flow)
- Room migrations required — no destructive fallbacks for patient data
- Build metadata (`SOFTWARE_VERSION`, `SOFTWARE_SAFETY_CLASS`) embedded in APK
- Full Software Development Plan: `docs/iec62304/SOFTWARE_DEVELOPMENT_PLAN.md`

### Cybersecurity (FDA Pre-Market Guidance 2023)
- SQLCipher AES-256-GCM encryption for all PHI at rest
- Android Keystore hardware-backed keys — never exported to app memory
- `FLAG_SECURE` on all windows — prevents screenshots of PHI
- `allowBackup=false` — PHI never sent to Android cloud backup
- Certificate pinning + TLS 1.3 for all network traffic
- SHA-256 hash-chained audit log — retroactive tampering is detectable
- Biometric authentication at launch and on foreground resume
- Full Cybersecurity Plan: `docs/fda_cybersecurity/CYBERSECURITY_PLAN.md`

### Clinical Alerting
Alert thresholds (per Clinical Requirements Spec CRS-001):

| Vital Sign | Alert Condition |
|---|---|
| Heart Rate | < 40 bpm or > 180 bpm |
| SpO₂ | < 90% |
| Systolic BP | < 70 mmHg or > 180 mmHg |
| Diastolic BP | > 120 mmHg |
| Temperature | < 35°C or > 40°C |

## Building

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing env vars)
KEYSTORE_PATH=/path/to.jks \
KEYSTORE_PASSWORD=... \
KEY_ALIAS=... \
KEY_PASSWORD=... \
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run all checks (lint + test)
./gradlew check
```

## Testing

```
Unit tests:      app/src/test/
Integration:     app/src/androidTest/
HIL tests:       hardware-in-the-loop — requires ISM device or simulator
```

Coverage targets (enforced in CI):
- Domain layer: ≥ 90%
- Data layer: ≥ 80%
- Presentation layer: ≥ 60%

## Documentation

| Document | Location |
|---|---|
| Software Development Plan (IEC 62304) | `docs/iec62304/SOFTWARE_DEVELOPMENT_PLAN.md` |
| Cybersecurity Plan (FDA) | `docs/fda_cybersecurity/CYBERSECURITY_PLAN.md` |
| Software Requirements Spec | *(in project issue tracker)* |
| Risk Management File | *(separate controlled document)* |
