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

## 3. Advanced Notifications Sub-Menu & Slot Management

* **Flexible Reminder Slots**:
  * **Day Before (Evening)**: 19:00, 20:00, 21:00, or custom user-defined times.
  * **Day Of (Morning)**: 06:00, 07:00, 08:00, or custom user-defined times.
* **Interactive `MultipleRemindersDialog` Sub-Menu**:
  * Integrated into both Onboarding and Settings screens for configuring custom Day Before and Day Of reminder slots.
  * Supports in-place adding, editing, and deleting of reminder times for both slots.
  * Features a built-in time picker with instant validation preventing duplicate time entries.

---

## 4. System 12h/24h Clock Detection

* **Automatic System Clock Resolution**:
  * Dynamically queries `DateFormat.is24HourFormat(context)` in `DateUtils.formatTime`.
  * Renders times in either 12-hour (e.g., `7:00 PM`) or 24-hour (e.g., `19:00`) format based on the system device setting.
* **Ubiquitous Formatting**: Applied across Dashboard collection banners, settings sub-menus, onboarding flows, dialogs, and notification messages.

---

## 5. Interactive Notification Shade Action Buttons ("Put Bins Out")

* **Shade-Native Interaction**:
  * System heads-up notifications include an interactive "Done" / "Mark as Put Out" action button (`com.example.binminder.ACTION_MARK_PUT_OUT`).
* **Asynchronous `NotificationActionReceiver`**:
  * Handled by `MarkBinPutOutReceiver` using `goAsync()`.
  * Extracts target collection date and bin IDs from notification intent extras, marks bins as put out without launching the main UI, and cancels the notification banner immediately.

---

## 6. Date-Scoped Put-Out State Isolation

* **Composite Key Architecture (`"${binId}_${collectionDate}"`)**:
  * Scopes put-out status to a composite key combining the bin ID and the specific collection date.
* **Complete State Isolation**:
  * Prevents marking a bin as put out for today from affecting future recurring collection dates or polluting global state.

---

## 7. Bank Holiday Schedule Shifts

* **UK Bank Holiday Engine**: Calculates official bank holidays across the UK:
  * New Year's Day (plus substitute days).
  * Good Friday & Easter Monday (dynamically calculated via Meeus/Jones/Butcher algorithm).
  * Early May Bank Holiday.
  * Spring Bank Holiday.
  * Summer Bank Holiday.
  * Christmas Day & Boxing Day (plus substitute days).
* **Automatic Shift Preview**: Analyzes upcoming collection dates. If a collection falls on or immediately after a bank holiday, BinDay calculates a 1-day collection shift (e.g., Friday collection moved to Saturday) and presents a warning banner on the Dashboard and Settings screens.

---

## 8. 1-Click .ics Calendar Export with RRULEs

* **iCalendar RFC 5545 Compliance**: Generates standard `.ics` files compatible with Google Calendar, Apple Calendar, Microsoft Outlook, and native Android Calendar.
* **Recurrence Rules (RRULE)**: Exports recurring collection schedules with precise iCalendar rules:
  * `FREQ=WEEKLY;INTERVAL=1` (Weekly collections).
  * `FREQ=WEEKLY;INTERVAL=2` (Fortnightly collections).
  * `FREQ=WEEKLY;INTERVAL=4` (Four-weekly collections).
* **VALARM Reminder Rules**: Embeds `VALARM` triggers into exported `.ics` events to automatically fire calendar reminders the evening before or morning of collection.
* **Native Android Calendar Provider**: Direct integration to insert collection events directly into device calendars using `Intent.ACTION_INSERT` or `CalendarContract`.

---

## 9. Triple-Layer Exact Alarm Engine & Safeguards

* **Precision Alarms via `AlarmManager`**:
  * Uses `AlarmManager.setExactAndAllowWhileIdle()` on Android 6.0+ (API 23+) for guaranteed trigger delivery during Doze mode.
* **Android 12+ `SecurityException` Graceful Fallback**:
  * Gracefully handles revoked exact alarm permissions (`SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`), falling back to `setAndAllowWhileIdle()` and enqueuing `WorkManager` background tasks.
* **Reboot Persistence (`BootReceiver`)**:
  * Re-enqueues exact alarms and WorkManager background jobs on `ACTION_BOOT_COMPLETED`, `ACTION_MY_PACKAGE_REPLACED`, `ACTION_LOCKED_BOOT_COMPLETED`, and `QUICKBOOT_POWERON`.
* **Reliability Safeguards**:
  * **`IS_EXACT_DELIVERY` Flagging**: Prevents generic `PeriodicWorkRequest` OS executions from triggering duplicate notifications.
  * **Unified Cancellation Tag (`BINMINDER_REMINDER_WORK`)**: Tags all background jobs for guaranteed global cancellation when reminder settings are turned off.
  * **"Double-Check" Trigger Guard**: `NotificationAlarmReceiver` evaluates `triggeredTime` against live `NotificationSettings` before posting, eliminating stale or ghost alarm firings.
  * **Strict Default Bin Isolation**: Restricts default bin initialization exclusively to onboarding UI flows, preventing background workers from re-injecting deleted fallback bins.
  * **Anti-Spam Debounce Window**: Enforces a 1-hour anti-spam debounce window using shared preferences to suppress duplicate notifications.

---

## 10. Eco-Sleek Design System & UI Ergonomics

* **Prominent Branding**: Features a custom 52dp rounded top bar logo (`ic_app_logo.png`) and cohesive Eco-Sleek aesthetic tokens.
* **Material Design 3 Expressive UI**: Adaptive light and dark themes, smooth card elevation, expressive typography, and full edge-to-edge window inset handling.
* **Accessibility Compliance**: Meets WCAG AA contrast standards across all swatches and UI components with touch target sizes exceeding 48dp.
