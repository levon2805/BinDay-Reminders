# BinDay: Reminders - Feature Specifications

This document details the core features and functional mechanics built into **BinDay: Reminders**.

---

## 1. Postcode Lookup & Council Auto-Matching

* **Postcode Resolution**: Users enter any valid UK postcode (e.g., `SW1A 1AA`, `M1 1AE`, `EH1 1YZ`). BinDay queries the open `postcodes.io` API to extract the local authority administrative district.
* **Database Matcher**: Matches the district against an embedded dataset of 380+ UK local authorities spanning England, Scotland, Wales, and Northern Ireland.
* **Default Schedule Provisioning**: Automatically pre-populates default wheelie bin configurations (e.g., General Waste, Dry Mixed Recycling, Food Waste, Garden Waste) based on standard council provision.

---

## 2. Dual-Colour Wheelie Bins

* **Realistic Wheelie Bin Visualiser**: Wheelie bins are displayed using custom SVG/Canvas composables rendering both the bin body and the bin lid independently.
* **Curated Preset Swatches**: Includes official UK council bin colours:
  * Green, Dark Green, Blue, Dark Blue, Brown, Black, Grey, Light Grey, Red, Yellow, Orange, Purple, Burgundy, Magenta, and Custom.
* **Custom Hex Colour Picker**: Includes an interactive Colour Picker with:
  * Curated colour category tabs (Reds, Blues, Greens, Yellows, Browns, Greys).
  * Saturation and Lightness sliders.
  * Direct hex code text entry (`#RRGGBB`).
  * Contrast check ensuring legibility against light/dark themes.

---

## 3. Bank Holiday Schedule Shifts

* **UK Bank Holiday Engine**: Calculates official bank holidays across the UK:
  * New Year's Day (plus substitute days).
  * Good Friday & Easter Monday (dynamically calculated via Meeus/Jones/Butcher algorithm).
  * Early May Bank Holiday.
  * Spring Bank Holiday.
  * Summer Bank Holiday.
  * Christmas Day & Boxing Day (plus substitute days).
* **Automatic Shift Preview**: Analyzes upcoming collection dates. If a collection falls on or immediately after a bank holiday, BinDay calculates a 1-day collection shift (e.g., Friday collection moved to Saturday) and presents a warning banner on the Dashboard and Settings screens.

---

## 4. ICS Calendar Export with RRULEs

* **iCalendar RFC 5545 Compliance**: Generates standard `.ics` files compatible with Google Calendar, Apple Calendar, Microsoft Outlook, and native Android Calendar.
* **Recurrence Rules (RRULE)**: Exports recurring collection schedules with precise iCalendar rules:
  * `FREQ=WEEKLY;INTERVAL=1` (Weekly collections).
  * `FREQ=WEEKLY;INTERVAL=2` (Fortnightly collections).
  * `FREQ=WEEKLY;INTERVAL=4` (Four-weekly collections).
* **VALARM Reminder Rules**: Embeds `VALARM` triggers into exported `.ics` events to automatically fire calendar reminders the evening before or morning of collection.
* **Native Android Calendar Provider**: Direct integration to insert collection events directly into device calendars using `Intent.ACTION_INSERT` or `CalendarContract`.

---

## 5. Notification Engine & Exact Alarms

* **Dual-Slot Notification Strategy**:
  * **Day Before (Evening)**: 19:00, 20:00, 21:00, or custom times.
  * **Day Of (Morning)**: 06:00, 07:00, 08:00, or custom times.
* **Advanced Notifications Sub-Menu (`MultipleRemindersDialog`)**:
  * Provides an interactive dialog interface in both Onboarding and Settings screens for managing multiple reminder times per slot.
  * Supports in-place adding, editing, and deleting of reminder times for both Day Before and Day Of slots.
  * Includes a time picker with instant validation preventing duplicate time entries.
* **System 12h/24h Clock Detection (`DateFormat.is24HourFormat`)**:
  * Dynamically detects user system time formatting preferences via `DateFormat.is24HourFormat(context)` in `DateUtils.formatTime`.
  * Formats time strings across all screens, dialogs, and notifications into 12-hour (e.g., `7:00 PM`) or 24-hour (e.g., `19:00`) formats accordingly.
* **Triple-Layer Exact Alarm Engine (`AlarmManager` & `WorkManager`)**:
  * Uses `AlarmManager.setExactAndAllowWhileIdle()` on Android 6.0+ (API 23+) to guarantee precise trigger delivery even during system Doze mode.
  * Handles Android 12+ (API 31+) `SecurityException` gracefully when exact alarm permissions (`SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`) are revoked, falling back seamlessly to `setAndAllowWhileIdle()` and scheduling background sync tasks via `WorkManager`.
* **Interactive Notification Shade Action ("Put Bins Out") via `NotificationActionReceiver`**:
  * Embeds an interactive "Done" / "Mark as Put Out" action button directly into system heads-up notifications.
  * Handled asynchronously by `NotificationActionReceiver` (`MarkBinPutOutReceiver`), which parses the target collection date and bin IDs from intent extras, marks the bins as put out without opening the main application UI, and cancels the notification.
* **Date-Scoped Put-Out State Isolation (`"${binId}_${collectionDate}"`)**:
  * Scopes bin put-out status to a composite string key consisting of the unique bin ID and the specific ISO date (`"${binId}_${collectionDate}"`).
  * Guarantees complete isolation across collection dates, ensuring that marking a bin as put out for today's collection date does not affect future recurring collection dates or pollute global state.
* **Engine Safeguards & Reliability Guardrails**:
  * **`IS_EXACT_DELIVERY` Flagging**: Prevents random OS `PeriodicWorkRequest` executions from triggering duplicate notifications by explicitly validating alarm sources.
  * **Blanket WorkManager Cancellation (`BINMINDER_REMINDER_WORK`)**: Tags all reminder background jobs with a unified tag to ensure reliable, global cancellation across the entire app ecosystem when settings change.
  * **"Double-Check" Security Guard**: Explicitly verifies the `triggeredTime` against active user reminder settings within `NotificationAlarmReceiver` before posting any notification, eliminating stale or ghost alarm firings.
  * **Strict Default Bin Isolation**: Prevents background workers (like `NotificationWorker`) from inadvertently re-injecting deleted fallback bins during quiet hours by isolating default initialization strictly to onboarding UI flows.
* **Debounce & Suppress Mechanics**:
  * Maintains a 1-hour anti-spam debounce window using shared preferences to prevent duplicate notification posts between exact alarm triggers and background `WorkManager` runs.
  * Automatically suppresses reminder notifications if all bins scheduled for a collection date have already been marked as put out.
* **Reboot Persistence (`BootReceiver`)**:
  * Listens for `ACTION_BOOT_COMPLETED`, `ACTION_MY_PACKAGE_REPLACED`, `ACTION_LOCKED_BOOT_COMPLETED`, and `QUICKBOOT_POWERON` broadcasts.
  * Automatically reschedules all active exact alarms and WorkManager fallback jobs whenever the device reboots or application packages are updated.
