package com.example.binminder.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToggleBinPutOutUseCaseTest {

    private val useCase = ToggleBinPutOutUseCase()

    @Test
    fun testMarkBinAsPutOut() {
        val initialSet = emptySet<Long>()
        val result = useCase(binId = 1L, binName = "General Waste", currentPutOutBins = initialSet)

        assertTrue(result.updatedPutOutBins.contains(1L))
        assertEquals("Marked 'General Waste' bin as put out for collection.", result.userMessage)
    }

    @Test
    fun testUnmarkBinAsPutOut() {
        val initialSet = setOf(1L, 2L)
        val result = useCase(binId = 1L, binName = "General Waste", currentPutOutBins = initialSet)

        assertFalse(result.updatedPutOutBins.contains(1L))
        assertTrue(result.updatedPutOutBins.contains(2L))
        assertEquals("Unmarked 'General Waste' bin.", result.userMessage)
    }

    @Test
    fun testUncheckingBinReturnsIsPutOutFalseForBin() {
        val binId = 101L
        val initialSet = setOf(binId, 102L)

        // User unchecks "Bins Are Out"
        val result = useCase(binId = binId, binName = "Dry Mixed Recycling", currentPutOutBins = initialSet)

        val isPutOut = result.updatedPutOutBins.contains(binId)
        assertFalse("Unchecking bin must set isPutOut to false", isPutOut)
        assertEquals("Unmarked 'Dry Mixed Recycling' bin.", result.userMessage)
    }
}
