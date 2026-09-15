package com.example.binminder.engine

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Data class holding information about an official UK Bank Holiday.
 */
data class BankHolidayInfo(
    val name: String,
    val date: LocalDate
)

/**
 * Data class representing a preview of a bin collection date shift during a bank holiday week.
 */
data class BankHolidayShiftPreview(
    val holidayName: String,
    val holidayDate: LocalDate,
    val binName: String,
    val originalDayName: String,
    val shiftedDayName: String,
    val originalDate: LocalDate,
    val shiftedDate: LocalDate
)

/**
 * Calculator object for official UK Bank Holidays in England and Wales and schedule adjustment rules.
 */
object BankHolidayCalculator {

    const val SUBSTITUTE_BANK_HOLIDAY_EXPLANATION =
        "In the UK, when Christmas, Boxing Day, or New Year's Day falls on a weekend, the UK government designates the following Monday/Tuesday as the official 'Substitute' Bank Holiday."

    /**
     * Calculates Easter Sunday for a given year using the Anonymous Gregorian algorithm.
     */
    fun calculateEasterSunday(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1
        return LocalDate.of(year, month, day)
    }

    /**
     * Returns a named list of official UK Bank Holidays in England and Wales for the specified year.
     */
    fun getNamedBankHolidaysForYear(year: Int): List<BankHolidayInfo> {
        val list = mutableListOf<BankHolidayInfo>()

        // 1. New Year's Day (Jan 1, substitute day if Saturday or Sunday)
        val newYearDay = LocalDate.of(year, 1, 1)
        val newYearObserved = when (newYearDay.dayOfWeek) {
            DayOfWeek.SATURDAY -> newYearDay.plusDays(2) // Mon Jan 3
            DayOfWeek.SUNDAY -> newYearDay.plusDays(1)   // Mon Jan 2
            else -> newYearDay
        }
        list.add(BankHolidayInfo("New Year's Day", newYearObserved))

        // 2 & 3. Easter Holidays (Good Friday and Easter Monday)
        val easterSunday = calculateEasterSunday(year)
        val goodFriday = easterSunday.minusDays(2)
        val easterMonday = easterSunday.plusDays(1)
        list.add(BankHolidayInfo("Good Friday", goodFriday))
        list.add(BankHolidayInfo("Easter Monday", easterMonday))

        // 4. Early May Bank Holiday (First Monday of May)
        val earlyMayBH = LocalDate.of(year, 5, 1)
            .with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY))
        list.add(BankHolidayInfo("Early May Bank Holiday", earlyMayBH))

        // 5. Spring Bank Holiday (Last Monday of May)
        val springBH = LocalDate.of(year, 5, 31)
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        list.add(BankHolidayInfo("Spring Bank Holiday", springBH))

        // 6. Summer Bank Holiday (Last Monday of August)
        val summerBH = LocalDate.of(year, 8, 31)
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        list.add(BankHolidayInfo("Summer Bank Holiday", summerBH))

        // 7 & 8. Christmas Day and Boxing Day (with substitute day rules)
        val christmasDay = LocalDate.of(year, 12, 25)
        val boxingDay = LocalDate.of(year, 12, 26)

        when (christmasDay.dayOfWeek) {
            DayOfWeek.SATURDAY -> {
                list.add(BankHolidayInfo("Christmas Day (Substitute)", LocalDate.of(year, 12, 27)))
                list.add(BankHolidayInfo("Boxing Day (Substitute)", LocalDate.of(year, 12, 28)))
            }
            DayOfWeek.SUNDAY -> {
                list.add(BankHolidayInfo("Christmas Day (Substitute)", LocalDate.of(year, 12, 27)))
                list.add(BankHolidayInfo("Boxing Day (Substitute)", LocalDate.of(year, 12, 28)))
            }
            DayOfWeek.FRIDAY -> {
                list.add(BankHolidayInfo("Christmas Day", christmasDay))
                list.add(BankHolidayInfo("Boxing Day (Substitute)", LocalDate.of(year, 12, 28)))
            }
            else -> {
                list.add(BankHolidayInfo("Christmas Day", christmasDay))
                list.add(BankHolidayInfo("Boxing Day", boxingDay))
            }
        }

        return list.sortedBy { it.date }
    }

    /**
     * Returns the list of official UK Bank Holiday dates for the specified year.
     */
    fun getBankHolidaysForYear(year: Int): List<LocalDate> {
        return getNamedBankHolidaysForYear(year).map { it.date }
    }

    /**
     * Returns the next upcoming bank holidays starting from the provided date.
     */
    fun getNextUpcomingBankHolidays(fromDate: LocalDate = LocalDate.now(), limit: Int = 6): List<BankHolidayInfo> {
        val thisYear = getNamedBankHolidaysForYear(fromDate.year)
        val nextYear = getNamedBankHolidaysForYear(fromDate.year + 1)
        return (thisYear + nextYear)
            .filter { !it.date.isBefore(fromDate) }
            .take(limit)
    }

    /**
     * Checks whether a given date is an official UK bank holiday.
     */
    fun isBankHoliday(date: LocalDate): Boolean {
        val holidays = getBankHolidaysForYear(date.year)
        return holidays.contains(date)
    }

    /**
     * Finds all bank holiday dates occurring in the ISO week containing the specified date.
     */
    fun getBankHolidaysInWeek(date: LocalDate): List<LocalDate> {
        val weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

        val years = setOf(weekStart.year, weekEnd.year)
        val allHolidays = years.flatMap { getBankHolidaysForYear(it) }

        return allHolidays.filter { it in weekStart..weekEnd }.sorted()
    }

    /**
     * Shifts a collection date by one day if a bank holiday occurs earlier in that same week.
     *
     * @return A pair containing the updated collection date and a flag indicating whether it was shifted.
     */
    fun adjustForBankHoliday(scheduledDate: LocalDate): Pair<LocalDate, Boolean> {
        val bankHolidaysInWeek = getBankHolidaysInWeek(scheduledDate)
        if (bankHolidaysInWeek.isEmpty()) {
            return Pair(scheduledDate, false)
        }

        val earliestBankHoliday = bankHolidaysInWeek.first()
        if (!scheduledDate.isBefore(earliestBankHoliday)) {
            // Scheduled collection falls on or after the bank holiday in that week: shift +1 day
            return Pair(scheduledDate.plusDays(1), true)
        }

        return Pair(scheduledDate, false)
    }
}
