# BinDay: Reminders

[![Android Version](https://img.shields.io/badge/Android-8.0%2B%20%28API%2026%2B%20to%2035%2B%20--%20Android%2015%29-brightgreen.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-blue.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-M3%20Expressive-purple.svg)](https://developer.android.com/)
[![Tests](https://img.shields.io/badge/Unit%20Tests-139%20Passing-success.svg)](docs/DEVELOPMENT_AND_TESTING.md)

**BinDay: Reminders** is a production-grade, native Android application engineered to simplify household waste management across the United Kingdom. Built as a showcase of modern Android software engineering, BinDay features postcode auto-matching for over 380 UK local councils, custom canvas dual-colour wheelie bin visualisations, bank holiday schedule shift handling, iCalendar RFC 5545 exports, and a multi-tiered exact notification engine.

---

## Executive Summary & Engineering Highlights

* **Clean Architecture & Unidirectional Data Flow (UDF)**: Strict architectural boundary decoupling (`UI` -> `Domain Use Cases` -> `Data Repositories`). Reactive state exposed via `StateFlow` leveraging `SharingStarted.WhileSubscribed(5000)` for lifecycle-aware subscription and memory efficiency.
* **Declarative Jetpack Compose UI**: 100% Jetpack Compose implementation featuring custom Neobrutalist design tokens, dynamic light/dark/system theme support, custom Canvas visual swatches for dual-colour wheelie bins (body & lid), and high-contrast WCAG AA accessibility compliance.
* **Advanced Notification Sub-Menu & Clock Detection**: Flexible reminder configuration via `MultipleRemindersDialog` with Day Before and Day Of reminder slots, in-place time editing, and automatic system 12h/24h clock detection (`DateFormat.is24HourFormat`).
* **Triple-Layer Notification Engine**: Guarantees precise reminder delivery across all Android power-saving states using system `AlarmManager` (`setExactAndAllowWhileIdle` with Android 12+ `SecurityException` fallbacks to `setAndAllowWhileIdle`), high-priority `WorkManager` background sync fallbacks, shade-native `NotificationActionReceiver` interactive "Put Bins Out" quick actions, and `BootReceiver` reboot recovery.
* **Database, Sync & REST API Integration**: Single Source of Truth architecture powered by Room Database with schema migration support (`MIGRATION_1_2` with `fallbackToDestructiveMigration`), DataStore Preferences for reactive state, and REST API integration with `postcodes.io` for 380+ UK local authorities.
* **KMP-Ready Pure Kotlin Domain Layer**: Zero Android framework dependencies in the domain layer, making business logic, schedule engines, and bank holiday calculation algorithms ready for Kotlin Multiplatform (KMP) iOS code sharing.
* **Extensive Unit Test Suite**: 139 unit tests passing across 18 test suites covering domain logic, state flows, schedule calculations, receivers, workers, and utilities.

---

## Key Features

* **380+ UK Council Postcode Resolution**: Instant local authority lookup matching UK postcodes via `postcodes.io` and pre-configuring default collection timetables.
* **Dual-Colour Canvas Wheelie Bins**: Accurate visual representation of bin bodies and lids matching official council specifications or custom hex colours.
* **Advanced Notifications Sub-Menu**: Day Before and Day Of reminder configuration with in-place time editing, slot management, and system 12h/24h clock detection (`DateFormat.is24HourFormat`).
* **Interactive Notification Shade Actions**: Shade-native "Put Bins Out" action buttons (`NotificationActionReceiver`) allowing users to mark bins as put out directly from system notifications without launching the app.
* **Bank Holiday Shift Engine**: Algorithmic detection of UK Bank Holidays (including dynamic Easter calculation via the Meeus/Jones/Butcher algorithm) with automatic 1-day schedule shift previews.
* **RFC 5545 iCalendar Exports**: Calendar synchronization exporting collection schedules with precise iCalendar `RRULE`s and embedded `VALARM` triggers.

---

## Tech Stack & Tooling

| Component | Technology | Description |
| :--- | :--- | :--- |
| **Language** | Kotlin 2.2.10 | Coroutines, Flow, Sealed Interfaces, Pure Kotlin Domain |
| **UI Framework** | Jetpack Compose | Material Design 3 Expressive, Neobrutalist Tokens, Custom Canvas |
| **Navigation** | Jetpack Navigation 3 | Strongly typed `@Serializable` `NavKey` routes (`androidx.navigation3`) |
| **Architecture** | Clean Architecture + MVVM + UDF | Immutable State Flow, Sealed UI States, Use Case pattern |
| **Persistence** | Room & DataStore Preferences | SQLite database with migration handling & reactive Key-Value store |
| **Networking** | Retrofit 2 & Moshi | REST client for postcode district resolution via `postcodes.io` |
| **Scheduling** | AlarmManager & WorkManager | Triple-layer exact alarm system with Android 12+ SecurityException fallbacks, NotificationActionReceiver, and BootReceiver recovery |
| **Testing** | JUnit 4 & Coroutines Test | 139 unit tests passing across 18 test suites |

---

## Technical Architecture

The application enforces strict separation of concerns across Presentation, Domain, Data, and Engine layers:

```
┌───────────────────────────────────────────────────────────┐
│                      PRESENTATION                         │
│   Jetpack Compose UI  ◄───►  ViewModels (StateFlow)       │
└─────────────────────────────┬─────────────────────────────┘
                              │
                              ▼
┌───────────────────────────────────────────────────────────┐
│                         DOMAIN                            │
│           Pure Kotlin Use Cases (KMP Ready)               │
└─────────────────────────────┬─────────────────────────────┘
                              │
                              ▼
┌───────────────────────────────────────────────────────────┐
│                          DATA                             │
│     Repositories  ◄───►  Room DB / DataStore / Retrofit   │
└───────────────────────────────────────────────────────────┘
```

> [!NOTE]
> The domain layer contains pure Kotlin models, interfaces, and logic engines with zero Android dependencies, enabling seamless migration to Kotlin Multiplatform (KMP) for cross-platform expansion.

For detailed architectural specifications, database schemas, and data flow diagrams, refer to [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

---

## Comprehensive Documentation

Detailed technical documentation is available in the [`docs/`](docs/) directory:

* [Architecture Guide (`docs/ARCHITECTURE.md`)](docs/ARCHITECTURE.md) - Deep dive into Clean Architecture, MVVM, UDF, Repository pattern, reactive data flows, and background receivers.
* [Features Specification (`docs/FEATURES.md`)](docs/FEATURES.md) - Comprehensive description of council lookup, bank holiday algorithms, advanced notifications sub-menu, 12h/24h time formatting, interactive shade actions, date-scoped put-out isolation, and calendar exports.
* [Development & Testing Guide (`docs/DEVELOPMENT_AND_TESTING.md`)](docs/DEVELOPMENT_AND_TESTING.md) - Build instructions, unit test execution, APK generation, and 18 test suite specifications.

---

## Getting Started & Installation

### Prerequisites
* **Android Studio**: Ladybug (2024.2.1) or newer
* **Java Development Kit (JDK)**: JDK 17
* **Android SDK**: API level 35/37 (Minimum API level 26 / Android 8.0)

### Build Commands

1. **Clone the repository**:
   ```bash
   git clone https://github.com/example/binday.git
   cd binday
   ```

2. **Run Unit Tests**:
   ```bash
   ./gradlew testDebugUnitTest
   ```

3. **Build Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```

4. **Install on Device / Emulator**:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## Code Style & Standards

All code additions must maintain the 139-test pass suite and adhere to British English spelling (`colour`, `customise`, `behaviour`, `organise`, `programme`, `cancelled`) in KDoc and inline comments.

---

## License

This project is licensed under the MIT License - see the `LICENSE` file for details.
