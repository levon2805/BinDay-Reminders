# BinDay: Reminders - Feature Specifications

This document details the core features and functional mechanics built into **BinDay: Reminders**.

---

## 1. Postcode Lookup & Council Auto-Matching

* **Postcode Resolution**: Users enter any valid UK postcode (e.g., `SW1A 1AA`, `M1 1AE`, `EH1 1YZ`). BinDay queries the open `postcodes.io` API to extract the local authority administrative district.
* **Database Matcher**: Matches the district against an embedded dataset of 380+ UK local authorities spanning England, Scotland, Wales, and Northern Ireland.
* **Default Schedule Provisioning**: Automatically pre-populates default wheelie bin configurations (e.g. General Waste, Dry Mixed Recycling, Food Waste, Garden Waste) based on standard council provision.

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
* **Automatic Shift Preview**: Analyzes upcoming collection dates. If a collection falls on or immediately after a bank holiday, BinDay calculates a 1-day collection shift (e.g. Friday collection moved to Saturday) and presents a warning banner on the Dashboard and Settings screens.

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
  * **Evening Before**: 19:00, 20:00, 21:00, or custom time.
  * **Morning Of**: 06:00, 07:00, 08:00, or custom time.
* **Exact Alarm Scheduling (`AlarmManager`)**: Uses `setExactAndAllowWhileIdle()` on Android 6.0+ to guarantee notifications trigger precisely at user-configured times even in Doze mode.
* **Heads-Up Banner (`IMPORTANCE_HIGH`)**: Displays high-priority notifications with vibration patterns and prominent wheelie bin icons.
* **Interactive Notification Actions**:
  * Includes a **"Mark Put Out"** action button in the notification shade, allowing users to acknowledge putting out their bin without opening the app.
* **Debounce & Suppress Mechanics**: Marking a bin as put out automatically suppresses subsequent reminders for that collection date.
* **Reboot Persistence (`BootReceiver`)**: Automatically re-establishes all alarm triggers whenever the Android device reboots or updates.
