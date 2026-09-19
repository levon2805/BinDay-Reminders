# Architecture Overview & Technical Design

**BinDay: Reminders** is built following strict **Clean Architecture** principles, **MVVM (Model-View-ViewModel)** presentation pattern, and **Unidirectional Data Flow (UDF)**. This architectural separation ensures testability, maintainability, and clear boundary enforcement across layers.

---

## Layered Architecture

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
                  └────────────────────┬────────────────────┘
                                       │
                                       ▼
                  ┌─────────────────────────────────────────┐
                  │       Engine & Background Layer         │
                  │ (ScheduleEngine, Receivers, Workers)    │
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
* **Advanced Notification UI (`MultipleRemindersDialog`)**: Interactive dialog composable integrated into Onboarding and Settings screens for configuring custom Day Before and Day Of reminder times with in-place addition, editing, and deletion.

#### ViewModels in BinDay
* `DashboardViewModel`: Manages upcoming collection events, hero banner state, bank holiday shift warnings, and marking bins as put out.
* `BinListViewModel`: Manages the complete list of configured bins, enable/disable toggles, deletion dialogs, and manual reset workflows.
* `AddEditBinViewModel`: Handles adding new bins or editing existing ones, including custom body/lid colour selections.
* `SettingsViewModel`: Controls notification settings (evening/morning reminder times, multiple reminder slots), theme selection (Light/Dark/System), and bank holiday shift previews.
* `OnboardingViewModel`: Manages the multi-step setup wizard (Postcode lookup -> Council selection -> Schedule configuration -> Confirmation).

---

### 2. Domain Layer (`com.example.binminder.domain`)

The domain layer encapsulates business rules into pure, reusable Kotlin Use Cases independent of any UI or Android framework dependencies:

* `GetUpcomingCollectionsUseCase`: Evaluates configured active bins and computes sorted upcoming collection events for a specified date range.
* `LookupCouncilScheduleUseCase`: Queries council lookup service to retrieve default bin schedules for a given UK local authority or postcode.
* `ResetTimetableUseCase`: Clears all saved bins and resets onboarding state to permit reconfiguration.
* `ToggleBinPutOutUseCase`: Handles toggling the put-out state of a bin for a specific collection date using date-scoped composite keys (`"${binId}_${collectionDate}"`).

---

### 3. Data Layer (`com.example.binminder.data`)

The data layer implements data storage, remote API communication, and repository abstractions.

#### Repositories
* `BinRepository`: Interface declaring reactive access to bins, notification preferences, theme modes, onboarding state, and the reactive put-out state flow `putOutBins: Flow<Set<String>>` (also accessed via reactive flows `getPutOutBinsFlow()`).
* `BinRepositoryImpl`: Single Source of Truth implementation coordinating Room DAO (`BinDao`), DataStore (`NotificationSettingsDataStore`), and background schedulers.
* `CouncilLookupRepository`: Interface providing postcode resolution and council schedule matching.

#### Data Sources & Local Storage
* **Room Database (`AppDatabase`, `BinDao`, `BinEntity`)**:
  * Stores bin configurations (`id`, `name`, `presetColor`, `colorHex`, `lidPresetColor`, `lidColorHex`, `scheduleDay`, `recurrence`, `startNextWeek`, `isEnabled`, `customNote`, `adjustForBankHolidays`).
  * Database migration handling (e.g. Migration 1->2 adding dual-colour lid columns `lidColorHex` and `lidPresetColor`).
* **DataStore Preferences (`NotificationSettingsDataStore`)**:
  * Stores user preferences (`reminderEnabled`, `eveningReminderTime`, `morningReminderTime`, `eveningReminderTimes`, `morningReminderTimes`, `putOutBins` string set, `onboardingCompleted`, `councilName`, `postcode`, `themeMode`).
  * Stores `putOutBins` as a set of date-scoped composite keys (`"${binId}_${collectionDate}"`).

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

### 5. Background Scheduling & Receiver Layer (`com.example.binminder.worker`)

Handles notification scheduling, exact system alarms, background sync, and notification shade interaction:

* **`NotificationScheduler`**:
  * Central scheduling authority that converts user notification settings into system `AlarmManager` exact alarms.
  * Configures exact alarms using `setExactAndAllowWhileIdle()` on API 23+.
  * Implements graceful `SecurityException` fallback handling on Android 12+ (API 31+) if exact alarm privileges are revoked, falling back to `setAndAllowWhileIdle()` and scheduling `WorkManager` background tasks.
* **`NotificationAlarmReceiver`**:
  * `BroadcastReceiver` invoked directly by `AlarmManager` exact alarms.
  * Evaluates active reminder slots, verifies un-put-out status for the target collection date via `putOutBins`, formats reminder text via `NotificationHelper`, and triggers high-priority heads-up system notifications.
  * Automatically invokes `NotificationScheduler` to reschedule subsequent alarms upon execution.
* **`NotificationActionReceiver` (`MarkBinPutOutReceiver`)**:
  * Direct `BroadcastReceiver` handling interactive "Put Bins Out" ("Mark as Put Out") notification shade quick actions (`com.example.binminder.ACTION_MARK_PUT_OUT`).
  * Executes asynchronously via `goAsync()`, invoking `ToggleBinPutOutUseCase` to update `putOutBins` in DataStore without launching the app UI, and cancels the active notification banner.
* **`BootReceiver`**:
  * Listens for `ACTION_BOOT_COMPLETED`, `ACTION_MY_PACKAGE_REPLACED`, `ACTION_LOCKED_BOOT_COMPLETED`, and `QUICKBOOT_POWERON` broadcasts.
  * Re-enqueues all exact alarms and WorkManager background tasks automatically following device reboots or application updates.
* **`NotificationWorker`**: `WorkManager` background sync task providing secondary redundancy for collection reminders.
* **`NotificationHelper`**: Constructs notification channels (`IMPORTANCE_HIGH`), builds heads-up notifications with embedded pending intents for action buttons, and maintains a 1-hour anti-spam debounce cache.

---

## Reactive Data Flow Architecture

The diagram below illustrates the reactive data flow for bin put-out state (`getPutOutBinsFlow()` / `putOutBins`):

```
┌──────────────────────────┐          ┌───────────────────────────────────┐
│   Dashboard UI Action    │          │  Notification Action Receiver     │
│   (User taps "Put Out")  │          │  (User taps "Put Out" in Shade)   │
└────────────┬─────────────┘          └─────────────────┬─────────────────┘
             │                                          │
             ▼                                          ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        ToggleBinPutOutUseCase                           │
│  (Toggles date-scoped composite key "${binId}_${collectionDate}")        │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           BinRepositoryImpl                             │
│                  updatePutOutBins(updatedBinsSet)                       │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    NotificationSettingsDataStore                        │
│                   (Persists updated StringSet)                          │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
                        Emits updated Flow<Set<String>>
                                     │
          ┌──────────────────────────┴──────────────────────────┐
          │                                                     │
          ▼                                                     ▼
┌───────────────────────────┐                         ┌───────────────────┐
│    DashboardViewModel     │                         │ Alarm / Worker    │
│  (StateFlow UI update)    │                         │  Check logic      │
└───────────────────────────┘                         └───────────────────┘
```

---

## Security & Data Privacy

* **Local-First Architecture**: User postcodes, bin schedules, and notification records remain 100% on-device inside private app storage (Room & DataStore).
* **Minimal Network Requests**: External HTTP requests are strictly limited to postcode district lookups via HTTPS to `postcodes.io`. No user telemetry or tracking data is transmitted.
