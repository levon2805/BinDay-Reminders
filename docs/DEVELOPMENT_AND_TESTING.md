# Development & Testing Guide

This guide provides instructions for setting up the development environment, running unit and instrumented tests, building debug APKs, and adhering to codebase conventions.

---

## Environment Prerequisites

* **Operating System**: Windows, macOS, or Linux.
* **Android Studio**: Android Studio Ladybug (2024.2.1) or newer recommended.
* **Java Development Kit (JDK)**: JDK 17 (configured via Gradle wrapper).
* **Android SDK**:
  * Compile SDK: `35` (or `37`)
  * Target SDK: `37` (Android 15)
  * Minimum SDK: `26` (Android 8.0 Oreo)

---

## Build Commands

### 1. Compile and Assemble Debug APK

To compile the application and generate a debug APK:

```bash
./gradlew assembleDebug
```

Output APK location:
`app/build/outputs/apk/debug/app-debug.apk`

### 2. Assemble Release Build

```bash
./gradlew assembleRelease
```

---

## Unit Testing

BinDay features an expanded automated unit test suite comprising **139 unit tests passing across 18 test suites** covering all domain use cases, ViewModels, schedule engines, bank holiday algorithms, data repositories, workers, notification receivers, and date/time utilities.

### Running Unit Tests

Run all unit tests via Gradle:

```bash
./gradlew testDebugUnitTest
```

### Unit Test Suite Structure

The unit tests are located in `app/src/test/java/com/example/binminder/`:

| Test Class | Scope / Target Covered |
| :--- | :--- |
| `BinLidColourTest` | Dual-colour bin creation, lid colour fallbacks, preset matching |
| `NotificationSettingsTest` | Notification preferences copying, defaults, time formatting, 12h/24h formatting |
| `CouncilLookupRepositoryTest` | Postcode lookup parsing, council matching, fallback URLs |
| `CouncilLookupServiceTest` | Retrofit service responses and error handling |
| `GetUpcomingCollectionsUseCaseTest` | Upcoming event calculations, sorting, date filtering |
| `LookupCouncilScheduleUseCaseTest` | Default bin schedule generation by council |
| `ResetTimetableUseCaseTest` | Timetable reset, bin deletion, onboarding state reset |
| `ToggleBinPutOutUseCaseTest` | Marking bin put out, string set formatting, date-scoped composite key toggling |
| `BankHolidayCalculatorTest` | Easter calculation (Meeus/Jones/Butcher), UK bank holiday dates, shifts |
| `ScheduleEngineTest` | Weekly, fortnightly, monthly schedule calculations, start week offsets |
| `AddEditBinViewModelTest` | Bin creation/editing, color swatch selection, input validation |
| `BinListViewModelTest` | Bin listing, enable/disable toggle, deletion flow |
| `DashboardViewModelTest` | Upcoming collections UI state, hero banner, mark put out actions |
| `OnboardingViewModelTest` | Multi-step wizard flow, postcode validation, schedule setup, multiple reminders sub-menu |
| `SettingsViewModelTest` | Notification time updates, multiple reminder slots, theme changes, reset workflows |
| `CalendarExportUtilsTest` | iCalendar ICS string generation, RRULE formatting, VALARM generation |
| `DateUtilsTest` | 12h/24h system clock formatting (`DateFormat.is24HourFormat`) and time string parsing |
| `NotificationSchedulerTest` | Alarm intent generation, WorkManager task scheduling, Android 12+ SecurityException fallback, notification debounce logic |

---

## Installing Debug APK on Device or Emulator

1. Connect an Android device with USB Debugging enabled, or launch an Android Virtual Device (AVD).
2. Install the compiled debug APK:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

3. Launch BinDay from the app drawer or via ADB:

```bash
adb shell am start -n com.example.binminder/.MainActivity
```

---

## Code Style & Standards

* **Language**: Idiomatic Kotlin using extension functions, `StateFlow`, `coroutineScope`, and immutability.
* **Spelling Standard**: **British English** (`colour`, `customise`, `behaviour`, `organise`, `programme`, `cancelled`, `centre`, `favourite`, `grey`) MUST be enforced consistently across all KDoc documentation and inline comments.
* **Material Design 3 Expressive UI**: All Compose screens must strictly adhere to M3 guidelines, including edge-to-edge layout handling with `WindowInsets`.
* **Zero Warnings Policy**: Ensure no unused imports, redundant modifiers, or unnecessary safe calls remain in the codebase.
