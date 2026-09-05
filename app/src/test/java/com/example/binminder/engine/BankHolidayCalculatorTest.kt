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
}
