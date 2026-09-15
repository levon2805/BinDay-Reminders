package com.example.binminder.data.model

import com.example.binminder.data.local.BinEntity
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class BinLidColourTest {

    @Test
    fun testBinColorEnumIncludesAllRequiredUKColours() {
        val expectedColours = listOf(
            BinColor.BLACK,
            BinColor.DARK_GREY,
            BinColor.LIGHT_GREY,
            BinColor.BLUE,
            BinColor.GREEN,
            BinColor.BROWN,
            BinColor.YELLOW,
            BinColor.RED,
            BinColor.PURPLE,
            BinColor.ORANGE,
            BinColor.BURGUNDY,
            BinColor.MAGENTA
        )

        val available = BinColor.entries.toSet()
        expectedColours.forEach { color ->
            Assert.assertTrue("BinColor enum should contain ${color.name}", available.contains(color))
        }
    }

    @Test
    fun testBinColorFromNameLookup() {
        assertEquals(BinColor.DARK_GREY, BinColor.fromName("DARK_GREY"))
        assertEquals(BinColor.DARK_GREY, BinColor.fromName("Dark Grey"))
        assertEquals(BinColor.LIGHT_GREY, BinColor.fromName("LIGHT_GREY"))
        assertEquals(BinColor.LIGHT_GREY, BinColor.fromName("Light Grey"))
        assertEquals(BinColor.BURGUNDY, BinColor.fromName("BURGUNDY"))
        assertEquals(BinColor.ORANGE, BinColor.fromName("ORANGE"))
        assertEquals(BinColor.MAGENTA, BinColor.fromName("MAGENTA"))
        assertEquals(BinColor.BLACK, BinColor.fromName("BLACK / DARK GREY"))
        assertEquals(BinColor.BLACK, BinColor.fromName(null))
        assertEquals(BinColor.BLACK, BinColor.fromName("UNKNOWN_COLOR"))
    }

    @Test
    fun testBinColorDisplayNameHelper() {
        val dualColorBin = Bin(
            name = "Refuse Bin",
            colorHex = BinColor.BLACK.defaultHex,
            presetColor = BinColor.BLACK,
            lidColorHex = BinColor.BLUE.defaultHex,
            lidPresetColor = BinColor.BLUE
        )
        assertEquals("Black Bin with Blue Lid", dualColorBin.colorDisplayName)

        val solidColorBin = Bin(
            name = "Recycling Bin",
            colorHex = BinColor.BLUE.defaultHex,
            presetColor = BinColor.BLUE,
            lidColorHex = null,
            lidPresetColor = null
        )
        assertEquals("Blue Bin", solidColorBin.colorDisplayName)

        val sameLidBin = Bin(
            name = "Garden Bin",
            colorHex = BinColor.GREEN.defaultHex,
            presetColor = BinColor.GREEN,
            lidColorHex = BinColor.GREEN.defaultHex,
            lidPresetColor = BinColor.GREEN
        )
        assertEquals("Green Bin", sameLidBin.colorDisplayName)
    }

    @Test
    fun testBinEntityToAndFromDomainWithLidColors() {
        val originalBin = Bin(
            id = 42L,
            name = "Dry Recycling",
            colorHex = "#212121",
            presetColor = BinColor.DARK_GREY,
            lidColorHex = "#1E88E5",
            lidPresetColor = BinColor.BLUE,
            recurrence = RecurrenceType.FORTNIGHTLY,
            repeatIntervalWeeks = 2,
            startDate = LocalDate.of(2025, 3, 3),
            customNote = "Put lid down firmly",
            isEnabled = true,
            adjustForBankHolidays = true
        )

        val entity = BinEntity.fromDomain(originalBin)
        assertEquals(42L, entity.id)
        assertEquals("Dry Recycling", entity.name)
        assertEquals("#212121", entity.colorHex)
        assertEquals("DARK_GREY", entity.presetColor)
        assertEquals("#1E88E5", entity.lidColorHex)
        assertEquals("BLUE", entity.lidPresetColor)

        val restoredBin = entity.toDomain()
        assertEquals(originalBin.id, restoredBin.id)
        assertEquals(originalBin.name, restoredBin.name)
        assertEquals(originalBin.presetColor, restoredBin.presetColor)
        assertEquals(originalBin.lidPresetColor, restoredBin.lidPresetColor)
        assertEquals(originalBin.lidColorHex, restoredBin.lidColorHex)
        assertEquals(originalBin.startDate, restoredBin.startDate)
    }

    @Test
    fun testBinEntityToAndFromDomainWithNullLidColor() {
        val originalBin = Bin(
            id = 10L,
            name = "Food Caddy",
            colorHex = "#6D4C41",
            presetColor = BinColor.BROWN,
            lidColorHex = null,
            lidPresetColor = null
        )

        val entity = BinEntity.fromDomain(originalBin)
        assertNull(entity.lidColorHex)
        assertNull(entity.lidPresetColor)

        val restoredBin = entity.toDomain()
        assertNull(restoredBin.lidColorHex)
        assertNull(restoredBin.lidPresetColor)
    }
}
