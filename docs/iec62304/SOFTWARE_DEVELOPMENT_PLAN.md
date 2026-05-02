# IronSide Connect - Software Development Plan
**Document ID:** SDP-001  
**Version:** 1.0.0  
**Safety Class:** IEC 62304 Class B  
**Status:** Draft for Review  

---

## 1. Purpose & Scope

This Software Development Plan (SDP) governs the development of **IronSide Connect**, an Android companion application for IronSide Medical devices. The plan establishes processes, standards, and responsibilities to ensure the software meets IEC 62304:2006+AMD1:2015 requirements for a **Class B** medical device software system.

**Class B rationale:** Software failure could result in injury but is not likely to be life-threatening. The BLE companion app displays physiological data and triggers alerts but does not directly control any therapeutic actuators.

---

## 2. Software Safety Classification

| SOUP / Module | Class | Rationale |
|---|---|---|
| Android OS (API 26+) | B | Platform hosting app; failure can affect data display |
| SQLCipher 4.x | B | Encrypts PHI at rest |
| Room 2.x | B | Mediates access to patient data |
| Jetpack Compose | A | Pure UI rendering; no safety function |
| Hilt 2.x | A | DI framework; configuration only |
| Timber 5.x | A | Debug logging only; stripped in release |

---

## 3. Development Lifecycle

### 3.1 Software Planning (§5.1)
- This document (SDP-001) governs planning
- Software Requirements Specification (SRS-001) captures all functional and safety requirements
- Risk Management File (RMF-001) is maintained per ISO 14971

### 3.2 Software Requirements Analysis (§5.2)
Requirements are maintained in the project issue tracker with the following attributes:
- Unique ID (e.g., `SRS-BT-010`)
- Safety classification (A/B/C)
- Acceptance criteria (testable)
- Traceability to system requirements

### 3.3 Software Architecture Design (§5.3)
The application follows **Clean Architecture**:

```
Presentation (Compose + ViewModels)
        ↓
Domain (Use Cases + Domain Models)
        ↓
Data (Repositories + BLE + Room)
        ↓
Android Platform / Hardware
```

No layer may depend on a layer above it. The domain layer has zero Android dependencies.

### 3.4 Software Detailed Design (§5.4)
- All public APIs documented with KDoc
- Sequence diagrams for BLE connection state machine (see `/docs/iec62304/diagrams/`)
- Data flow diagrams for measurement pipeline

### 3.5 Software Unit Implementation (§5.5)
**Coding standards:**
- Kotlin 2.x; no Java interop in new code
- `kotlinx.coroutines` for all async operations - no raw threads
- All suspend functions handle cancellation gracefully
- No force-unwrap (`!!`) except where nullability is a programmer error
- No `System.exit()` or uncaught exception swallowers

**Prohibited patterns:**
- Hard-coded credentials or device addresses
- Cleartext logging of PHI in any build type
- `TODO` or `FIXME` in merged code (tracked in issue system instead)

### 3.6 Software Integration and Integration Testing (§5.6)
- BLE integration tests run against a hardware-in-the-loop (HIL) device simulator
- CI pipeline runs on every PR: unit tests → static analysis → lint → HIL smoke test
- Integration test report archived with each release candidate

### 3.7 Software System Testing (§5.7)
System-level test plan (STP-001) covers:
- BLE pairing and re-pairing after bond loss
- Alert threshold accuracy vs. device output
- Background/foreground lifecycle transitions
- Screen-lock and biometric re-auth flows
- Data persistence across app kills and device reboots
- Export integrity verification

### 3.8 Software Release (§5.8)
- Each release tagged in git with version + build hash
- Build metadata embedded in APK `BuildConfig` (see `build.gradle.kts`)
- Release notes include: resolved issues, known anomalies, SOUP version changes
- APK signed with hardware-backed release key managed in CI secure vault

---

## 4. Configuration Management

- **VCS:** Git; branch strategy: `main` (release), `develop`, feature branches
- **Tagging:** `vMAJOR.MINOR.PATCH` semantic versioning
- **Artifact storage:** CI artefacts archived for 5 years (post-market surveillance)
- **Change control:** All changes to `main` require PR review + CI green

---

## 5. Problem Resolution Process (§9)

All defects discovered in any phase are tracked in the project issue tracker with:
- Severity (Critical / Major / Minor / Enhancement)
- IEC 62304 classification (safety-relevant / non-safety)
- Root cause analysis for Critical/Major items
- Regression test added before closure

---

## 6. SOUP Management (§8)

| Library | Version | Source | Evidence of Safety |
|---|---|---|---|
| Android API | 26-35 | Google | CTS certification |
| SQLCipher | 4.5.4 | Zetetic | FIPS 140-2 validation |
| Kotlin stdlib | 2.0.21 | JetBrains | Open source, auditable |
| Hilt | 2.52 | Google | Widely deployed, tracked CVEs |

SOUP updates require: regression test pass + documented review of changelog for safety impact.

---

## 7. Document History

| Version | Date | Author | Summary |
|---|---|---|---|
| 1.0.0 | 2026-04-29 | Engineering | Initial draft |
