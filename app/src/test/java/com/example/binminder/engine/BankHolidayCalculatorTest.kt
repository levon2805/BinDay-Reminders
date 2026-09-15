package com.example.binminder.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BankHolidayCalculatorTest {

    @Test
    fun testEasterSundayCalculation() {
        assertEquals(LocalDate.of(2024, 3, 31), BankHolidayCalculator.calculateEasterSunday(2024))
        assertEquals(LocalDate.of(2025, 4, 20), BankHolidayCalculator.calculateEasterSunday(2025))
        assertEquals(LocalDate.of(2026, 4, 5), BankHolidayCalculator.calculateEasterSunday(2026))
    }

    @Test
    fun testBankHolidaysFor2025() {
        val holidays = BankHolidayCalculator.getBankHolidaysForYear(2025)

        assertTrue(holidays.contains(LocalDate.of(2025, 1, 1)))   // New Year's Day
        assertTrue(holidays.contains(LocalDate.of(2025, 4, 18)))  // Good Friday
        assertTrue(holidays.contains(LocalDate.of(2025, 4, 21)))  // Easter Monday
        assertTrue(holidays.contains(LocalDate.of(2025, 5, 5)))   // Early May Bank Holiday
        assertTrue(holidays.contains(LocalDate.of(2025, 5, 26)))  // Spring Bank Holiday
        assertTrue(holidays.contains(LocalDate.of(2025, 8, 25)))  // Summer Bank Holiday
        assertTrue(holidays.contains(LocalDate.of(2025, 12, 25))) // Christmas Day
        assertTrue(holidays.contains(LocalDate.of(2025, 12, 26))) // Boxing Day
    }

    @Test
    fun testNormalWeekNoAdjustment() {
        // Wednesday May 14, 2025 is in a normal week without bank holidays
        val scheduledDate = LocalDate.of(2025, 5, 14)
        val (adjustedDate, isAdjusted) = BankHolidayCalculator.adjustForBankHoliday(scheduledDate)

        assertEquals(scheduledDate, adjustedDate)
        assertFalse(isAdjusted)
    }

    @Test
    fun testBankHolidayWeekAdjustment() {
        // Mon May 26, 2025 is Spring Bank Holiday
        val mondayCollection = LocalDate.of(2025, 5, 26)
        val (adjustedMon, isAdjustedMon) = BankHolidayCalculator.adjustForBankHoliday(mondayCollection)
        assertEquals(LocalDate.of(2025, 5, 27), adjustedMon)
        assertTrue(isAdjustedMon)

        // Friday collection in same BH week (Fri May 30, 2025) should shift to Sat May 31
        val fridayCollection = LocalDate.of(2025, 5, 30)
        val (adjustedFri, isAdjustedFri) = BankHolidayCalculator.adjustForBankHoliday(fridayCollection)
        assertEquals(LocalDate.of(2025, 5, 31), adjustedFri)
        assertTrue(isAdjustedFri)
    }

    @Test
    fun testCollectionBeforeBankHolidayInSameWeek() {
        // Christmas Day 2025 is Thursday Dec 25. Monday Dec 22 is in the same week, BEFORE Christmas Day.
        val mondayCollection = LocalDate.of(2025, 12, 22)
        val (adjustedDate, isAdjusted) = BankHolidayCalculator.adjustForBankHoliday(mondayCollection)

        assertEquals(mondayCollection, adjustedDate)
        assertFalse(isAdjusted)
    }

    @Test
    fun testYearBoundaryNewYearsDaySubstituteWhenWeekend() {
        // 2022 New Year's Day was Saturday Jan 1 -> Substitute Monday Jan 3
        val holidays2022 = BankHolidayCalculator.getNamedBankHolidaysForYear(2022)
        val newYear2022 = holidays2022.first { it.name.contains("New Year") }
        assertEquals(LocalDate.of(2022, 1, 3), newYear2022.date)

        // 2023 New Year's Day was Sunday Jan 1 -> Substitute Monday Jan 2
        val holidays2023 = BankHolidayCalculator.getNamedBankHolidaysForYear(2023)
        val newYear2023 = holidays2023.first { it.name.contains("New Year") }
        assertEquals(LocalDate.of(2023, 1, 2), newYear2023.date)
    }

    @Test
    fun testBankHolidaysInWeekCrossYearBoundary() {
        // Thursday Jan 1, 2026 is New Year's Day.
        // Dec 31, 2025 is Wednesday of the same week.
        val dec31 = LocalDate.of(2025, 12, 31)
        val holidaysInWeek = BankHolidayCalculator.getBankHolidaysInWeek(dec31)

        assertEquals(1, holidaysInWeek.size)
        assertEquals(LocalDate.of(2026, 1, 1), holidaysInWeek[0])

        // Wednesday Dec 31 is before Thursday Jan 1 -> no adjustment
        val (adjustedWednesday, isAdjustedWed) = BankHolidayCalculator.adjustForBankHoliday(dec31)
        assertEquals(dec31, adjustedWednesday)
        assertFalse(isAdjustedWed)

        // Thursday Jan 1 is on New Year's Day -> shifts to Friday Jan 2
        val jan1 = LocalDate.of(2026, 1, 1)
        val (adjustedJan1, isAdjustedJan1) = BankHolidayCalculator.adjustForBankHoliday(jan1)
        assertEquals(LocalDate.of(2026, 1, 2), adjustedJan1)
        assertTrue(isAdjustedJan1)
    }

    @Test
    fun testLeapYearHolidaysAndEaster() {
        // 2028 is a leap year. Easter Sunday is April 16, 2028.
        val easter2028 = BankHolidayCalculator.calculateEasterSunday(2028)
        assertEquals(LocalDate.of(2028, 4, 16), easter2028)

        val holidays2028 = BankHolidayCalculator.getBankHolidaysForYear(2028)
        assertTrue(holidays2028.contains(LocalDate.of(2028, 4, 14))) // Good Friday
        assertTrue(holidays2028.contains(LocalDate.of(2028, 4, 17))) // Easter Monday
    }
}
