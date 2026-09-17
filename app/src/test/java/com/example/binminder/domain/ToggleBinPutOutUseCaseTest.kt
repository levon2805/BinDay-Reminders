package com.example.binminder.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ToggleBinPutOutUseCaseTest {

    private val useCase = ToggleBinPutOutUseCase()
    private val testDate = LocalDate.of(2023, 1, 1)

    @Test
    fun testMarkBinAsPutOut() {
        val initialSet = emptySet<String>()
        val result = useCase(binId = 1L, collectionDate = testDate, binName = "General Waste", currentPutOutBins = initialSet)

        assertTrue(result.updatedPutOutBins.contains("1_${testDate}"))
        assertEquals("Marked 'General Waste' bin as put out for collection.", result.userMessage)
    }

    @Test
    fun testUnmarkBinAsPutOut() {
        val initialSet = setOf("1_${testDate}", "2_${testDate}")
        val result = useCase(binId = 1L, collectionDate = testDate, binName = "General Waste", currentPutOutBins = initialSet)

        assertFalse(result.updatedPutOutBins.contains("1_${testDate}"))
        assertTrue(result.updatedPutOutBins.contains("2_${testDate}"))
        assertEquals("Unmarked 'General Waste' bin.", result.userMessage)
    }

    @Test
    fun testUncheckingBinReturnsIsPutOutFalseForBin() {
        val binId = 101L
        val initialSet = setOf("${binId}_${testDate}", "102_${testDate}")

        // User unchecks "Bins Are Out"
        val result = useCase(binId = binId, collectionDate = testDate, binName = "Dry Mixed Recycling", currentPutOutBins = initialSet)

        val isPutOut = result.updatedPutOutBins.contains("${binId}_${testDate}")
        assertFalse("Unchecking bin must set isPutOut to false", isPutOut)
        assertEquals("Unmarked 'Dry Mixed Recycling' bin.", result.userMessage)
    }
}
