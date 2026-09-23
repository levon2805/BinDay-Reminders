# BinDay: Reminders

[![Android Version](https://img.shields.io/badge/Android-8.0%2B%20%28API%2026%2B%20to%2037--%20Android%2015%29-brightgreen.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-blue.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-M3%20Expressive-purple.svg)](https://developer.android.com/)
[![Tests](https://img.shields.io/badge/Unit%20Tests-139%20Passing-success.svg)](docs/DEVELOPMENT_AND_TESTING.md)

**BinDay: Reminders** is a portfolio-grade, native Android application engineered to streamline and simplify household waste management across the United Kingdom. Built as a demonstration of modern Android software architecture and Jetpack Compose engineering, BinDay features UK postcode auto-matching against 380+ local councils, custom Canvas dual-colour wheelie bin visualisations (body + lid colours), an Eco-Sleek design system with 52dp rounded top bar logos, bank holiday schedule shift calculations, 1-click RFC 5545 iCalendar exports, and a triple-layer exact notification engine.

---

## Executive Summary & Engineering Highlights

* **Clean Architecture & Unidirectional Data Flow (UDF)**: Strict architectural separation (`UI` -> `Domain Use Cases` -> `Data Repositories`). Reactive state exposed via `StateFlow` leveraging `SharingStarted.WhileSubscribed(5000)` for lifecycle-aware subscription management and memory efficiency.
* **Declarative Jetpack Compose UI**: 100% Jetpack Compose implementation featuring custom Eco-Sleek design tokens, dynamic theme support (Light/Dark/System), custom Canvas swatches for dual-colour wheelie bins (independent body and lid colours), and high-contrast WCAG AA accessibility compliance with 52dp rounded top bar branding.
* **Advanced Notifications Sub-Menu & Clock Detection**: Flexible reminder configuration via `MultipleRemindersDialog` with Day Before and Day Of reminder slots, in-place time editing, slot addition/deletion, and dynamic system 12h/24h clock detection via `DateFormat.is24HourFormat`.
* **Triple-Layer Exact Notification Engine**: Delivers reliable reminders across all Android power-saving states using system `AlarmManager` (`setExactAndAllowWhileIdle` with Android 12+ `SecurityException` fallbacks to `setAndAllowWhileIdle`), high-priority `WorkManager` background sync fallbacks, shade-native `NotificationActionReceiver` interactive "Put Bins Out" quick action buttons, and `BootReceiver` reboot recovery. Fortified with safeguards including `IS_EXACT_DELIVERY` flags, `BINMINDER_REMINDER_WORK` tag cancellation, "Double-Check" trigger verification, and date-scoped put-out state isolation (`"${binId}_${collectionDate}"`).
* **Database, Sync & REST API Integration**: Single Source of Truth architecture powered by Room Database with schema migration handling (e.g. `MIGRATION_1_2` adding dual-colour lid columns), DataStore Preferences for reactive settings, and REST API integration with `postcodes.io` for district resolution across 380+ UK local authorities.
* **KMP-Ready Pure Kotlin Domain Layer**: Zero Android framework dependencies in the domain layer, making business rules, schedule engines, and bank holiday calculation algorithms ready for Kotlin Multiplatform (KMP) cross-platform code sharing.
* **Extensive Unit Test Suite**: **139 unit tests passing across 18 test suites** covering domain logic, ViewModels, schedule calculations, receivers, workers, calendar utilities, and date/time formatters.

---

## Showcase Features

* **UK Postcode Resolution & 380+ Council Database**: Instant local authority district resolution matching UK postcodes via `postcodes.io` and pre-configuring official wheelie bin timetables.
* **Dual-Colour Canvas Wheelie Bins**: Accurate visual representation rendering independent bin bodies and bin lids matching official council specifications or custom user-defined hex colours.
* **Advanced Notifications Sub-Menu**: Flexible Day Before (evening) and Day Of (morning) reminder slot management with in-place time addition, editing, and deletion via `MultipleRemindersDialog`.
* **System 12h/24h Clock Detection**: Automatic detection of device time preferences (`DateFormat.is24HourFormat`), dynamically formatting time displays into 12-hour (e.g. `7:00 PM`) or 24-hour (e.g. `19:00`) formats across all screens and notifications.
* **Interactive Notification Shade Actions ("Put Bins Out")**: Shade-native action buttons (`NotificationActionReceiver`) allowing users to mark bins as put out directly from system notifications without launching the application.
* **Date-Scoped Put-Out Isolation**: Status tracking scoped to specific ISO collection dates using composite keys (`"${binId}_${collectionDate}"`), ensuring put-out actions for today do not affect future recurring dates.
* **Bank Holiday Shift Engine**: Algorithmic detection of UK Bank Holidays (including dynamic Easter calculation via the Meeus/Jones/Butcher algorithm) with automatic 1-day schedule shift previews.
* **1-Click RFC 5545 iCalendar Exports**: Seamless calendar export generating standard `.ics` files with precise `RRULE` recurrence rules and embedded `VALARM` reminder triggers.
* **Eco-Sleek Design System**: Modern Material Design 3 Expressive UI featuring prominent 52dp rounded top bar logos, smooth card elevations, and adaptive dark/light palettes.

---

## Tech Stack & Tooling

| Component | Technology | Description |
| :--- | :--- | :--- |
| **Language** | Kotlin 2.2.10 | Coroutines, Flow, Sealed Interfaces, Pure Kotlin Domain |
| **UI Framework** | Jetpack Compose | Material Design 3 Expressive, Eco-Sleek Tokens, Custom Canvas |
| **Navigation** | Jetpack Navigation 3 | Strongly typed `@Serializable` `NavKey` routes (`androidx.navigation3`) |
| **Architecture** | Clean Architecture + MVVM + UDF | Immutable StateFlow, Sealed UI States, Use Case pattern |
| **Persistence** | Room & DataStore Preferences | SQLite database with migration handling & reactive Key-Value store |
| **Networking** | Retrofit 2 & Moshi | REST client for postcode district resolution via `postcodes.io` |
| **Scheduling** | AlarmManager & WorkManager | Triple-layer exact alarm system with Android 12+ fallbacks, shade actions, and reboot recovery |
| **Testing** | JUnit 4 & Coroutines Test | 139 unit tests passing across 18 test suites |

---

## Technical Architecture

The application enforces strict separation of concerns across Presentation, Domain, Data, Engine, and Background layers:

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
└─────────────────────────────┬─────────────────────────────┘
                              │
                              ▼
┌───────────────────────────────────────────────────────────┐
│                    ENGINE & WORKERS                       │
│    ScheduleEngine / AlarmManager / NotificationWorker     │
└─────────────────────────────┴─────────────────────────────┘
```

> [!NOTE]
> The domain layer contains pure Kotlin models, interfaces, and logic engines with zero Android framework dependencies, enabling seamless migration to Kotlin Multiplatform (KMP).

For detailed architectural specifications, database schemas, and reactive data flow diagrams, refer to [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

---

## Test Suite Status

BinDay maintains a comprehensive automated unit test suite with **139 unit tests passing across 18 test suites**:

* `BinLidColourTest`: Tests dual-colour bin creation, lid colour fallbacks, and preset matching.
* `NotificationSettingsTest`: Verifies settings copying, default times, and 12h/24h time formatting logic.
* `CouncilLookupRepositoryTest`: Validates postcode parsing, council district matching, and portal fallbacks.
* `CouncilLookupServiceTest`: Tests REST API response parsing and network error handling.
* `GetUpcomingCollectionsUseCaseTest`: Evaluates schedule calculation, event sorting, and date filtering.
* `LookupCouncilScheduleUseCaseTest`: Verifies default timetable generation for 380+ UK local authorities.
* `ResetTimetableUseCaseTest`: Tests bin table truncation and onboarding state resets.
* `ToggleBinPutOutUseCaseTest`: Validates date-scoped composite key formatting and state toggling.
* `BankHolidayCalculatorTest`: Tests UK bank holiday calculations, Easter Meeus/Jones/Butcher algorithm, and shift rules.
* `ScheduleEngineTest`: Verifies weekly, fortnightly, four-weekly, and monthly schedule recurrence rules.
* `AddEditBinViewModelTest`: Tests bin creation/editing, colour swatch selection, and form validation.
* `BinListViewModelTest`: Validates bin listing, toggle state handling, and deletion flows.
* `DashboardViewModelTest`: Tests upcoming collection UI states, hero banner, and put-out toggle actions.
* `OnboardingViewModelTest`: Validates multi-step setup wizard, postcode validation, and notification configuration.
* `SettingsViewModelTest`: Tests reminder time updates, multiple reminder sub-menu slots, theme changes, and resets.
* `CalendarExportUtilsTest`: Validates `.ics` string generation, `RRULE` formatting, and `VALARM` triggers.
* `DateUtilsTest`: Tests system 12h/24h clock formatting (`DateFormat.is24HourFormat`) and time parsing.
* `NotificationSchedulerTest`: Tests exact alarm scheduling, WorkManager tasks, Android 12+ fallbacks, and anti-spam debouncing.

---

## Comprehensive Documentation

Further technical documentation is available in the `docs/` directory:

* [Architecture Guide (`docs/ARCHITECTURE.md`)](docs/ARCHITECTURE.md) - In-depth breakdown of Clean Architecture, MVVM, UDF, Room DB migrations, DataStore, and triple-layer exact alarm mechanics.
* [Features Specification (`docs/FEATURES.md`)](docs/FEATURES.md) - Complete functional specification covering postcode lookup, dual-colour bins, advanced notifications sub-menu, shade actions, 12h/24h clock detection, and RFC 5545 calendar export.
* [Development & Testing Guide (`docs/DEVELOPMENT_AND_TESTING.md`)](docs/DEVELOPMENT_AND_TESTING.md) - Environment setup, build instructions, unit test execution, and test suite details.

---

## Quick Start & Installation Guide

### Prerequisites
* **Android Studio**: Ladybug (2024.2.1) or newer
* **Java Development Kit (JDK)**: JDK 17
* **Android SDK**: Compile SDK 35 (or 37), Target SDK 37 (Android 15), Minimum SDK 26 (Android 8.0)

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

All code contributions must maintain 100% test pass status across all 18 test suites and adhere to **British English** spelling (`colour`, `customise`, `behaviour`, `organise`, `programme`, `cancelled`, `centre`, `favourite`, `grey`) in KDoc and inline documentation.

---

## License

This project is licensed under the MIT License - see the `LICENSE` file for details.
