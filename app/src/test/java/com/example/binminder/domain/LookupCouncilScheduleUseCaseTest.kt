package com.example.binminder.domain

import com.example.binminder.data.model.BinColor
import com.example.binminder.data.model.CouncilScheduleResult
import com.example.binminder.data.model.OnboardingBinSetup
import com.example.binminder.data.model.RecurrenceType
import com.example.binminder.data.repository.CouncilLookupRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class LookupCouncilScheduleUseCaseTest {

    @Test
    fun testLookupCouncilScheduleSuccess() = runTest {
        val expectedResult = CouncilScheduleResult(
            postcode = "M1 1AE",
            councilName = "Manchester City Council",
            adminDistrict = "Manchester",
            primaryCollectionDay = DayOfWeek.TUESDAY,
            binSetups = listOf(
                OnboardingBinSetup("General Waste", "General Waste", BinColor.GREY, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false)
            )
        )

        val fakeRepo = object : CouncilLookupRepository {
            override suspend fun lookupPostcode(postcodeOrQuery: String): Result<CouncilScheduleResult> {
                return Result.success(expectedResult)
            }
        }

        val useCase = LookupCouncilScheduleUseCase(fakeRepo)
        val result = useCase("M1 1AE")

        assertTrue(result.isSuccess)
        assertEquals(expectedResult, result.getOrNull())
    }

    @Test
    fun testLookupCouncilScheduleFailure() = runTest {
        val fakeRepo = object : CouncilLookupRepository {
            override suspend fun lookupPostcode(postcodeOrQuery: String): Result<CouncilScheduleResult> {
                return Result.failure(Exception("Not found"))
            }
        }

        val useCase = LookupCouncilScheduleUseCase(fakeRepo)
        val result = useCase("INVALID")

        assertTrue(result.isFailure)
        assertEquals("Not found", result.exceptionOrNull()?.message)
    }
}
