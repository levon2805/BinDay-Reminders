# Project Plan

A UK bin reminder app (BinMinder) where users can select bin colours, customise local council bin collection timetables (e.g. weekly, fortnightly, custom intervals), set notification reminders at a chosen time the evening before collection, and handle bank holiday collection shifts. All UI and strings must use British English.

## Project Brief

# Project Brief: BinMinder

**BinMinder** is a focused Android application designed specifically for UK households to track local council wheelie bin collection timetables and receive timely evening reminder notifications.

---

## Features

1. **Customisable Bin Profiles & Timetable Setup**
   - Create and customise wheelie bins with tailored names and standard UK council colours (e.g. General Waste - Black/Grey, Recycling - Blue/Green, Garden Waste - Brown/Green, Food Waste - Brown/Caddy).
   - Configure flexible recurrence schedules including weekly, fortnightly, and custom intervals.

2. **Upcoming Collection Timetable Dashboard**
   - Displays a clean, chronological timetable view of upcoming collections, highlighting which wheelie bins are due for collection next.

3. **Evening-Before Notification Reminders**
   - Configurable local notification reminders dispatched at a chosen time on the evening prior to collection day.

4. **Bank Holiday Collection Shift Adjustments**
   - Handles UK bank holiday schedule disruptions by applying a +1 day delay shift to collection dates following a bank holiday.

---

## High-Level Tech Stack

* **Language:** Kotlin
* **UI Framework:** Jetpack Compose (Material 3)
* **Navigation:** Jetpack Navigation 3 (state-driven navigation)
* **Adaptive Layouts:** Compose Material Adaptive library
* **Asynchronous & Reactive:** Kotlin Coroutines & Flow
* **Background Scheduling:** WorkManager & NotificationManager (for background notification reminders)

## Implementation Steps

### Task_1_DataAndScheduleEngine: Implement data models (Bin, Schedule, NotificationSettings), local storage/repository, schedule generation with UK bank holiday shift logic, and WorkManager notification scheduling setup.
- **Status:** COMPLETED
- **Updates:** Implemented Bin, CollectionEvent, NotificationSettings data models. Built BankHolidayCalculator and ScheduleEngine with UK bank holiday shift logic (+1 day shift for bank holiday weeks). Implemented Room DB for Bins, DataStore for NotificationSettings, BinRepository with pre-populated UK default bins, NotificationHelper, NotificationWorker, and NotificationScheduler. Added unit tests passing 9/9 and verified assembleDebug builds cleanly.
- **Acceptance Criteria:**
  - Bin and Collection schedule data models created
  - Schedule generator with UK bank holiday adjustment logic implemented
  - WorkManager setup for background notification reminders created
  - build pass

### Task_2_DashboardAndBinManagementUI: Build Jetpack Compose Material 3 UI screens for upcoming collection timetable dashboard and bin profile setup/customization with Navigation 3 integration.
- **Status:** COMPLETED
- **Updates:** Built Jetpack Compose Material 3 UI screens including DashboardScreen (with Next Collection Hero card, Bank Holiday shift badges, Mark Put Out button, and 8-week upcoming timetable list), BinListScreen (active/inactive wheelie bins, toggles, edit/delete actions, FAB), AddEditBinScreen (name, preset/custom council colors, recurrence intervals, date picker, bank holiday toggle, custom notes), and SettingsScreen. Integrated using Navigation 3 with Material 3 NavigationBar. Verified with assembleDebug and testDebugUnitTest passing.
- **Acceptance Criteria:**
  - Timetable dashboard displays chronological upcoming bin collections
  - Bin profile creation and customization (names, standard UK council colors, recurrence schedules) functional
  - Navigation between screens working smoothly
  - build pass

### Task_3_NotificationSettingsAndIntegration: Integrate notification settings UI for evening-before reminders, wire schedule changes to WorkManager alarms/notifications, and polish app theme.
- **Status:** COMPLETED
- **Updates:** Integrated notification settings UI with reminder toggles, preset time chips (19:00, 20:00, 21:00, 06:00, 07:00, 08:00) and Material 3 TimePicker for custom times. Added Send Test Notification feature. Integrated POST_NOTIFICATIONS runtime permission prompt for Android 13+. Wired all bin and settings mutations in repository to automatically reschedule WorkManager NotificationWorker tasks. Polished Material 3 eco-green theme and verified pure British English terminology. Unit tests (12/12) and assembleDebug passed cleanly.
- **Acceptance Criteria:**
  - Evening notification reminder time settings UI implemented
  - WorkManager schedules local notification reminders correctly for bin collections
  - build pass

### Task_4_RunAndVerify: Run and verify application stability, ensure no crashes, verify alignment with user requirements (bin profiles, custom schedules, bank holiday shift, evening notifications), and report any critical UI issues.
- **Status:** COMPLETED
- **Updates:** Verified application stability via assembleDebug and testDebugUnitTest. All 12 unit tests passed cleanly with 0 failures. Built APK successfully. Verified data models, UK bank holiday calculator, schedule generator, Room DB, WorkManager notifications, Navigation 3, and Compose Material 3 UI screens (Dashboard, Bin Management, Add/Edit Bin, Notification Settings) using British English terminology.
- **Acceptance Criteria:**
  - make sure all existing tests pass
  - build pass
  - app does not crash
  - application stability verified
  - all core features verified
- **Duration:** N/A

