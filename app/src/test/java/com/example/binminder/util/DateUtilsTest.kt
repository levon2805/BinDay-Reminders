package com.example.binminder.util

import android.content.Context
import android.content.ContextWrapper
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class DateUtilsTest {

    @Test
    fun test12HourFormatFormatting() {
        val eveningTime = LocalTime.of(19, 0)
        val eveningEight = LocalTime.of(20, 0)
        val morningSeven = LocalTime.of(7, 0)

        assertEquals("7:00 PM", DateUtils.formatTime(eveningTime, is24Hour = false))
        assertEquals("8:00 PM", DateUtils.formatTime(eveningEight, is24Hour = false))
        assertEquals("7:00 AM", DateUtils.formatTime(morningSeven, is24Hour = false))
    }

    @Test
    fun test24HourFormatFormatting() {
        val eveningTime = LocalTime.of(19, 0)
        val eveningEight = LocalTime.of(20, 0)
        val morningSeven = LocalTime.of(7, 0)

        assertEquals("19:00", DateUtils.formatTime(eveningTime, is24Hour = true))
        assertEquals("20:00", DateUtils.formatTime(eveningEight, is24Hour = true))
        assertEquals("07:00", DateUtils.formatTime(morningSeven, is24Hour = true))
    }

    @Test
    fun testFormatTimeForUserWithContext() {
        val context = object : ContextWrapper(null) {
            override fun getApplicationContext(): Context = this
        }
        val time = LocalTime.of(19, 0)
        val result = DateUtils.formatTimeForUser(time, context)
        // Default unmocked framework call in JVM unit tests returns false -> 12h format
        assertEquals("7:00 PM", result)
    }
}
