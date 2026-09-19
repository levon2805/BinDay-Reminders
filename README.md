# BinDay: Reminders

[![Android Version](https://img.shields.io/badge/Android-8.0%2B%20%28API%2026%2B%20to%2035%2B%20--%20Android%2015%29-brightgreen.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-blue.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-M3%20Expressive-purple.svg)](https://developer.android.com/jetpack/compose)
[![Tests](https://img.shields.io/badge/Unit%20Tests-113%20Passing-success.svg)](docs/DEVELOPMENT_AND_TESTING.md)

**BinDay: Reminders** is a modern, native Android application designed to ensure UK households never miss a wheelie bin collection day again. Featuring postcode lookup for over 380 UK local councils, dual-colour wheelie bin representations, automatic UK bank holiday schedule shift handling, ICS calendar exports with RRULEs, and high-priority notification alarms.

---

## 🌟 Key Features

* **380+ UK Local Councils & Postcode Matching**: Automatic council detection using the `postcodes.io` API and built-in database of UK local authorities.
* **Dual-Colour Wheelie Bin Customisation**: Visualise wheelie bins with distinct body and lid colours matching exact council specifications or custom hex colours.
* **Bank Holiday Shift Engine**: Automatic detection of UK Bank Holidays (including Easter via Meeus/Jones/Butcher algorithm) with automatic schedule shift previews.
* **ICS Calendar Export**: Export collection timetables directly to Android system calendars or shareable `.ics` files using standard iCalendar RFC 5545 RRULEs.
* **High-Priority Notification Engine**: Dual-slot reminders (evening before / morning of collection) using exact alarms (`AlarmManager`), `WorkManager` fallback, and interactive notification actions.
* **Material Design 3 Expressive UI**: Built using 100% Jetpack Compose with adaptive layouts, neo-brutalist card styling, dynamic theme support (Light/Dark/System), and Navigation 3.

---

## 🛠️ Tech Stack

* **Language**: [Kotlin](https://kotlinlang.org/) 2.2.10
* **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3 Expressive components
* **Navigation**: [Jetpack Navigation 3](https://developer.android.com/guide/navigation) (`androidx.navigation3`)
* **Architecture**: Clean Architecture + MVVM + Unidirectional Data Flow (UDF)
* **Local Persistence**: [Room Database](https://developer.android.com/training/data-storage/room) & [DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore)
* **Networking**: [Retrofit 2](https://square.github.io/retrofit/) & [Moshi](https://github.org/square/moshi)
* **Background Scheduling**: `AlarmManager`, `WorkManager`, `BroadcastReceiver`
* **Concurrency**: Kotlin Coroutines & `StateFlow`
* **Unit Testing**: JUnit 4, Kotlinx Coroutines Test (113 unit tests)

---

## 📸 App Screenshots

| Dashboard | Bin Management | Onboarding Wizard | Settings & Reminders |
| :---: | :---: | :---: | :---: |
| *Upcoming collection hero banner & timeline* | *Dual-colour wheelie bin list & editor* | *Postcode lookup & council setup* | *Bank holiday previews & notification times* |

*(Screenshots placeholder: Add application screenshots here)*

---

## 🏗️ Architecture Overview

The application follows strict Clean Architecture principles with Unidirectional Data Flow:

```
┌───────────────────────────────────────────────────────────┐
│                      PRESENTATION                         │
│   Jetpack Compose UI  ◄───►  ViewModels (StateFlow)       │
└─────────────────────────────┬─────────────────────────────┘
                              │
                              ▼
┌───────────────────────────────────────────────────────────┐
│                         DOMAIN                            │
│                  Pure Kotlin Use Cases                    │
└─────────────────────────────┬─────────────────────────────┘
                              │
                              ▼
┌───────────────────────────────────────────────────────────┐
│                          DATA                             │
│     Repositories  ◄───►  Room DB / DataStore / Retrofit   │
└───────────────────────────────────────────────────────────┘
```

For full architectural details, see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

---

## 📖 Documentation

Detailed documentation is available in the [`docs/`](docs/) directory:

* 🏛️ [Architecture Guide (`docs/ARCHITECTURE.md`)](docs/ARCHITECTURE.md) - Deep dive into Clean Architecture, MVVM, UDF, Repository pattern, and database schemas.
* ✨ [Features Specification (`docs/FEATURES.md`)](docs/FEATURES.md) - Comprehensive description of council lookup, bank holiday calculations, notification engine, and calendar exports.
* 🧪 [Development & Testing Guide (`docs/DEVELOPMENT_AND_TESTING.md`)](docs/DEVELOPMENT_AND_TESTING.md) - Instructions for building, running all 113 unit tests, generating debug APKs, and contribution guidelines.

---

## 🚀 Getting Started & Installation

### Prerequisites
* **Android Studio**: Ladybug (2024.2.1) or newer
* **JDK**: Version 17
* **Android SDK**: API level 35/37 (Minimum API level 26 / Android 8.0)

### Building from Source

1. **Clone the repository**:
   ```bash
   git clone https://github.com/example/binday.git
   cd binday
   ```

2. **Build the Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```

3. **Run Unit Tests**:
   ```bash
   ./gradlew testDebugUnitTest
   ```

4. **Install on Connected Device / Emulator**:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 🤝 Contributing

Contributions are welcome! Please ensure all code additions maintain 100% test pass rate, follow idiomatic Kotlin guidelines, and enforce British English spelling (`colour`, `customise`, `behaviour`, `organise`, `programme`, `cancelled`, etc.) in all KDoc and inline comments.

---

## 📄 License

This project is licensed under the MIT License - see the `LICENSE` file for details.
