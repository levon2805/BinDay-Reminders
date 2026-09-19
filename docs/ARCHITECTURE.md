# Architecture Overview & Technical Design

**BinDay: Reminders** is built following strict **Clean Architecture** principles, **MVVM (Model-View-ViewModel)** presentation pattern, and **Unidirectional Data Flow (UDF)**. This architectural separation ensures testability, maintainability, and clear boundary enforcement across layers.

---

## 🏛️ Layered Architecture

```
                  ┌─────────────────────────────────────────┐
                  │            Presentation Layer           │
                  │   Jetpack Compose UI & ViewModels       │
                  └────────────────────┬────────────────────┘
                                       │
                                       ▼
                  ┌─────────────────────────────────────────┐
                  │               Domain Layer              │
                  │        Pure Kotlin Use Cases            │
                  └────────────────────┬────────────────────┘
                                       │
                                       ▼
                  ┌─────────────────────────────────────────┐
                  │                Data Layer               │
                  │   Repositories & Data Sources           │
                  │ (Room, DataStore, Retrofit, Services)   │
                  └─────────────────────────────────────────┘
```

---

### 1. Presentation Layer (`com.example.binminder.ui`)

The presentation layer is fully declarative, relying on **Jetpack Compose** with Material Design 3 Expressive components.

* **Navigation**: Managed via Jetpack Navigation 3 (`androidx.navigation3`). Routes are strongly typed using `@Serializable` target objects implementing `NavKey` (`Screen.Dashboard`, `Screen.BinList`, `Screen.AddEditBin`, `Screen.Settings`, `Screen.Onboarding`).
* **ViewModels**: State owners exposing `StateFlow<UiState>` to UI components. UI events are passed back to ViewModels via explicit function calls.
* **Unidirectional Data Flow (UDF)**:
  1. The **ViewModel** manages state and emits `StateFlow<UiState>`.
  2. The **Composable UI** collects `UiState` via `collectAsStateWithLifecycle()`.
  3. User interactions trigger callbacks on the **ViewModel**.
  4. The **ViewModel** updates state or executes domain use cases.

#### ViewModels in BinDay
* `DashboardViewModel`: Manages upcoming collection events, hero banner state, bank holiday shift warnings, and marking bins as put out.
* `BinListViewModel`: Manages the complete list of configured bins, enable/disable toggles, deletion dialogs, and manual reset workflows.
* `AddEditBinViewModel`: Handles adding new bins or editing existing ones, including custom body/lid colour selections.
* `SettingsViewModel`: Controls notification settings (evening/morning reminder times), theme selection (Light/Dark/System), and bank holiday shift previews.
* `OnboardingViewModel`: Manages the multi-step setup wizard (Postcode lookup -> Council selection -> Schedule configuration -> Confirmation).

---

### 2. Domain Layer (`com.example.binminder.domain`)

The domain layer encapsulates business rules into pure, reusable Kotlin Use Cases independent of any UI or Android framework dependencies:

* `GetUpcomingCollectionsUseCase`: Evaluates configured active bins and computes sorted upcoming collection events for a specified date range.
* `LookupCouncilScheduleUseCase`: Queries council lookup service to retrieve default bin schedules for a given UK local authority or postcode.
* `ResetTimetableUseCase`: Clears all saved bins and resets onboarding state to permit reconfiguration.
* `ToggleBinPutOutUseCase`: Handles marking a bin as put out for a given collection date or toggling its status back.

---

### 3. Data Layer (`com.example.binminder.data`)

The data layer implements data storage, remote API communication, and repository abstractions.

#### Repositories
* `BinRepository`: Interface declaring reactive access to bins, notification preferences, theme modes, and onboarding state.
* `BinRepositoryImpl`: Single Source of Truth implementation coordinating Room DAO (`BinDao`), DataStore (`NotificationSettingsDataStore`), and background workers.
* `CouncilLookupRepository`: Interface providing postcode resolution and council schedule matching.

#### Data Sources & Local Storage
* **Room Database (`AppDatabase`, `BinDao`, `BinEntity`)**:
  * Stores bin configurations (`id`, `name`, `presetColor`, `colorHex`, `lidPresetColor`, `lidColorHex`, `scheduleDay`, `recurrence`, `startNextWeek`, `isEnabled`, `customNote`, `adjustForBankHolidays`).
  * Database migration handling (e.g. Migration 1->2 adding dual-colour lid columns `lidColorHex` and `lidPresetColor`).
* **DataStore Preferences (`NotificationSettingsDataStore`)**:
  * Stores user preferences (`reminderEnabled`, `eveningReminderTime`, `morningReminderTime`, `putOutBins` string set, `onboardingCompleted`, `councilName`, `postcode`, `themeMode`).

#### Network & External Services (`CouncilLookupService`)
* Integrates with `https://api.postcodes.io/postcodes/{postcode}` via Retrofit/Moshi to resolve administrative district names.
* Fallback and search functionality linking to council bin collection portals for over 380 UK local authorities.

---

### 4. Engine Layer (`com.example.binminder.engine`)

Pure logic engine components handling domain-specific algorithms:

* **`ScheduleEngine`**:
  * Calculates exact future collection dates based on recurrence rules (`WEEKLY`, `FORTNIGHTLY`, `FOUR_WEEKLY`, `MONTHLY`).
  * Accounts for start week offsets (`startNextWeek`) and evaluates bank holiday adjustments.
* **`BankHolidayCalculator`**:
  * Calculates official UK Bank Holidays for any given year.
  * Implements the **Anonymous Gregorian (Meeus/Jones/Butcher) algorithm** to calculate Easter Sunday, Good Friday, and Easter Monday.
  * Computes 1-day collection shifts for bank holidays.

---

### 5. Background Scheduling & Worker Layer (`com.example.binminder.worker`)

Handles notification alarms and background maintenance:

* **`NotificationScheduler`**: Schedules exact alarms via `AlarmManager` for morning/evening reminder times, and enqueues daily periodic checks via `WorkManager`.
* **`NotificationAlarmReceiver`**: Receives exact alarm intents and triggers high-priority heads-up notifications.
* **`NotificationWorker`**: `WorkManager` background job that re-calculates upcoming collections and verifies alarm timers.
* **`NotificationHelper`**: Constructs `NotificationCompat.Builder` with `IMPORTANCE_HIGH` notification channel, vibration patterns, and "Mark Put Out" action intents.
* **`MarkBinPutOutReceiver`**: Direct broadcast receiver handling "Mark Put Out" quick actions directly from the notification shade without launching the full app UI.
* **`BootReceiver`**: Re-enqueues all notification alarms on device reboot (`ACTION_BOOT_COMPLETED`).

---

## 🔒 Security & Data Privacy

* **Local-First Architecture**: User postcodes, bin schedules, and notification records remain 100% on-device inside private app storage (Room & DataStore).
* **Minimal Network Requests**: External HTTP requests are strictly limited to postcode coordinate lookups via HTTPS to `postcodes.io`. No user telemetry or tracking data is transmitted.
