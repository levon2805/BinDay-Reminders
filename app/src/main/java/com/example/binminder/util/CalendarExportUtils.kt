package com.example.binminder.util

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.binminder.data.model.Bin
import com.example.binminder.data.model.CollectionEvent
import com.example.binminder.data.model.RecurrenceType
import com.example.binminder.engine.ScheduleEngine
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Utility object for exporting bin collection events and recurring schedules to the Android device calendar app.
 */
object CalendarExportUtils {

    /**
     * Builds an RRULE recurrence string for a given interval in weeks according to RFC 5545 format.
     *
     * @param intervalWeeks Recurrence interval in weeks (1 = weekly, 2 = fortnightly, 3 = 3-weekly, 4 = 4-weekly).
     * @param includePrefix Whether to include the "RRULE:" prefix (defaults to false).
     * @return Formatted RRULE string, e.g. "FREQ=WEEKLY;INTERVAL=2".
     */
    fun buildRRule(intervalWeeks: Int, includePrefix: Boolean = false): String {
        val interval = maxOf(1, intervalWeeks)
        val rule = "FREQ=WEEKLY;INTERVAL=$interval"
        return if (includePrefix) "RRULE:$rule" else rule
    }

    /**
     * Overload to build an RRULE recurrence string for a [RecurrenceType].
     */
    fun buildRRule(recurrence: RecurrenceType, includePrefix: Boolean = false): String {
        return buildRRule(recurrence.intervalWeeks, includePrefix)
    }

    /**
     * Calculates the exact next collection date for a specific bin on or after [fromDate].
     */
    fun calculateNextCollectionDate(bin: Bin, fromDate: LocalDate = LocalDate.now()): LocalDate {
        return ScheduleEngine.generateEventsForBin(bin, fromDate, fromDate.plusYears(1))
            .firstOrNull { !it.collectionDate.isBefore(fromDate) }?.collectionDate
            ?: if (bin.startDate.isAfter(fromDate)) bin.startDate else fromDate
    }

    /**
     * Creates an Intent to insert a single bin collection event into the calendar app.
     */
    fun createAddCollectionToCalendarIntent(
        events: List<CollectionEvent>,
        collectionDate: LocalDate,
        reminderTime: LocalTime = LocalTime.of(7, 0),
        isRecurring: Boolean = true,
    ): Intent {
        val binNames = events.joinToString(", ") { it.binName }
        val title = if (events.size == 1) {
            "Bin Collection: ${events.first().binName}"
        } else {
            "Bin Collection: $binNames"
        }

        val notesList = events.mapNotNull { event ->
            if (event.customNote.isNotBlank()) "${event.binName}: ${event.customNote}" else null
        }
        val notes = if (notesList.isNotEmpty()) {
            notesList.joinToString("\n")
        } else {
            "Bin collection day. Please put out your wheelie bins."
        }

        val startDateTime = LocalDateTime.of(collectionDate, reminderTime)
        val endDateTime = startDateTime.plusMinutes(30)

        val startMillis = startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = endDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val intervalWeeks = events.firstOrNull()?.repeatIntervalWeeks ?: 2

        return Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.DESCRIPTION, notes)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            putExtra(CalendarContract.Events.HAS_ALARM, 1)
            putExtra(CalendarContract.Events.EVENT_LOCATION, "Home")
            if (isRecurring) {
                putExtra(CalendarContract.Events.RRULE, buildRRule(intervalWeeks, includePrefix = false))
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Creates an Intent to insert a recurring calendar event series for a specific [Bin].
     * Calculates the exact first collection date for this specific bin if [nextCollectionDate] is not provided.
     */
    fun createAddBinToCalendarIntent(
        bin: Bin,
        nextCollectionDate: LocalDate? = null,
        reminderTime: LocalTime = LocalTime.of(7, 0)
    ): Intent {
        val firstCollectionDate = nextCollectionDate ?: calculateNextCollectionDate(bin)
        val title = "Bin Collection: ${bin.name}"
        val notes = if (bin.customNote.isNotBlank()) {
            "${bin.name} (${bin.recurrence.displayName}): ${bin.customNote}"
        } else {
            "${bin.name} (${bin.recurrence.displayName}) collection day. Please put out your wheelie bin."
        }

        val startDateTime = LocalDateTime.of(firstCollectionDate, reminderTime)
        val endDateTime = startDateTime.plusMinutes(30)

        val startMillis = startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = endDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        return Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.DESCRIPTION, notes)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            putExtra(CalendarContract.Events.HAS_ALARM, 1)
            putExtra(CalendarContract.Events.EVENT_LOCATION, "Home")
            putExtra(CalendarContract.Events.RRULE, buildRRule(bin.recurrence.intervalWeeks, includePrefix = false))
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Launches the calendar intent to export a collection event.
     */
    fun exportCollectionToCalendar(
        context: Context,
        events: List<CollectionEvent>,
        collectionDate: LocalDate,
        reminderTime: LocalTime = LocalTime.of(7, 0)
    ) {
        if (events.isEmpty()) {
            Toast.makeText(context, "No bin collections to export.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = createAddCollectionToCalendarIntent(events, collectionDate, reminderTime)
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to open calendar app.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Launches calendar intent for a specific bin as a recurring series.
     */
    fun exportBinToCalendar(
        context: Context,
        bin: Bin,
        nextCollectionDate: LocalDate? = null,
        reminderTime: LocalTime = LocalTime.of(7, 0)
    ) {
        try {
            val intent = createAddBinToCalendarIntent(bin, nextCollectionDate, reminderTime)
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to open calendar app.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Generates a standard RFC 5545 iCalendar (.ics) string containing recurring VEVENT entries
     * for all active bins in [bins].
     */
    fun generateIcsContent(
        bins: List<Bin>,
        reminderTime: LocalTime = LocalTime.of(7, 0)
    ): String {
        val activeBins = bins.filter { it.isEnabled }
        val lineEnding = "\r\n"
        val sb = StringBuilder()

        sb.append("BEGIN:VCALENDAR").append(lineEnding)
        sb.append("VERSION:2.0").append(lineEnding)
        sb.append("PRODID:-//BinMinder//Bin Collection Schedule//EN").append(lineEnding)
        sb.append("CALSCALE:GREGORIAN").append(lineEnding)
        sb.append("METHOD:PUBLISH").append(lineEnding)

        val nowStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"))

        for (bin in activeBins) {
            val firstCollectionDate = calculateNextCollectionDate(bin)
            val startDateTime = LocalDateTime.of(firstCollectionDate, reminderTime)
            val endDateTime = startDateTime.plusMinutes(30)

            val dtStart = startDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
            val dtEnd = endDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
            val uid = "bin-${bin.id}-${firstCollectionDate.format(DateTimeFormatter.BASIC_ISO_DATE)}@binminder"

            val title = "Bin Collection: ${bin.name}"
            val notes = if (bin.customNote.isNotBlank()) {
                "${bin.name} (${bin.recurrence.displayName}): ${bin.customNote}"
            } else {
                "${bin.name} (${bin.recurrence.displayName}) collection day. Please put out your wheelie bin."
            }

            val rrule = buildRRule(bin.recurrence.intervalWeeks, includePrefix = true)

            sb.append("BEGIN:VEVENT").append(lineEnding)
            sb.append("UID:").append(uid).append(lineEnding)
            sb.append("DTSTAMP:").append(nowStamp).append(lineEnding)
            sb.append("DTSTART:").append(dtStart).append(lineEnding)
            sb.append("DTEND:").append(dtEnd).append(lineEnding)
            sb.append("SUMMARY:").append(escapeIcsText(title)).append(lineEnding)
            sb.append("DESCRIPTION:").append(escapeIcsText(notes)).append(lineEnding)
            sb.append("LOCATION:Home").append(lineEnding)
            sb.append(rrule).append(lineEnding)
            sb.append("STATUS:CONFIRMED").append(lineEnding)
            sb.append("BEGIN:VALARM").append(lineEnding)
            sb.append("TRIGGER:-PT0M").append(lineEnding)
            sb.append("ACTION:DISPLAY").append(lineEnding)
            sb.append("DESCRIPTION:").append(escapeIcsText("Bin Collection Reminder: ${bin.name}")).append(lineEnding)
            sb.append("END:VALARM").append(lineEnding)
            sb.append("END:VEVENT").append(lineEnding)
        }

        sb.append("END:VCALENDAR").append(lineEnding)

        return sb.toString()
    }

    /**
     * Creates a standard .ics (iCalendar RFC 5545) file containing recurring VEVENT entries for ALL active bins.
     */
    fun createIcsCalendarFile(
        context: Context,
        bins: List<Bin>,
        reminderTime: LocalTime = LocalTime.of(7, 0)
    ): File {
        val icsContent = generateIcsContent(bins, reminderTime)
        val cacheDir = try { context.cacheDir } catch (_: Exception) { null }
            ?: File(System.getProperty("java.io.tmpdir") ?: ".")
        val exportDir = File(cacheDir, "calendar_exports").apply { mkdirs() }
        val file = File(exportDir, "bin_minder_schedule.ics")
        file.writeText(icsContent, Charsets.UTF_8)
        return file
    }

    /**
     * Shares or opens an .ics file containing all recurring bin schedules using FileProvider.
     */
    fun exportAllBinsViaIcs(
        context: Context,
        bins: List<Bin>,
        reminderTime: LocalTime = LocalTime.of(7, 0)
    ) {
        val activeBins = bins.filter { it.isEnabled }
        if (activeBins.isEmpty()) {
            Toast.makeText(context, "No active bins available to export.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val file = createIcsCalendarFile(context, activeBins, reminderTime)
            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, file)

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "text/calendar")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooserIntent = Intent.createChooser(viewIntent, "Import Bins to Calendar").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(chooserIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to export calendar file.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun escapeIcsText(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\n", "\\n")
    }
}
