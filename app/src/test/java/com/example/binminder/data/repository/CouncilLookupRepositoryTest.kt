package com.example.binminder.data.repository

import com.example.binminder.data.model.BinColor
import com.example.binminder.data.model.RecurrenceType
import com.example.binminder.data.service.CouncilLookupService
import com.example.binminder.data.service.PostcodeLookupDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CouncilLookupRepositoryTest {

    private lateinit var fakeService: FakeCouncilLookupService
    private lateinit var repository: CouncilLookupRepositoryImpl

    @Before
    fun setUp() {
        fakeService = FakeCouncilLookupService()
        repository = CouncilLookupRepositoryImpl(fakeService)
    }

    @Test
    fun testManchesterPostcodeLookup() = runTest {
        fakeService.stubbedDto = PostcodeLookupDto(
            postcode = "M1 1AE",
            adminDistrict = "Manchester",
            region = "North West"
        )

        val result = repository.lookupPostcode("M1 1AE")

        assertTrue(result.isSuccess)
        val schedule = result.getOrNull()!!
        assertEquals("Manchester City Council", schedule.councilName)
        assertEquals(4, schedule.binSetups.size)

        val binColors = schedule.binSetups.map { it.presetColor }
        assertTrue(binColors.contains(BinColor.GREY))
        assertTrue(binColors.contains(BinColor.BLUE))
        assertTrue(binColors.contains(BinColor.BROWN))
        assertTrue(binColors.contains(BinColor.GREEN))

        // Verify clean bin names without colour suffixes in brackets
        schedule.binSetups.forEach { bin ->
            assertFalse("Bin display name '${bin.displayName}' should not contain '('", bin.displayName.contains("("))
        }
    }

    @Test
    fun testPostcodeSanitizationAndSpaceHandling() = runTest {
        fakeService.stubbedDto = PostcodeLookupDto(
            postcode = "SW1A 1AA",
            adminDistrict = "Westminster",
            region = "London"
        )

        // Query with lowercase, extra spaces and tabs
        val result = repository.lookupPostcode("  sw1a   1aa \t")

        assertTrue(result.isSuccess)
        assertEquals("SW1A1AA", fakeService.lastQueriedPostcode)

        val schedule = result.getOrNull()!!
        assertEquals("Westminster City Council", schedule.councilName)
    }

    @Test
    fun testStockportCouncilLookup() = runTest {
        fakeService.stubbedDto = PostcodeLookupDto(
            postcode = "SK1 1AA",
            adminDistrict = "Stockport",
            region = "North West"
        )

        val result = repository.lookupPostcode("SK11AA")

        assertTrue(result.isSuccess)
        val schedule = result.getOrNull()!!
        assertEquals("Stockport Metropolitan Borough Council", schedule.councilName)
        assertEquals(4, schedule.binSetups.size)

        // Ensure clean bin display names
        schedule.binSetups.forEach { bin ->
            assertFalse("Bin display name '${bin.displayName}' should not contain '('", bin.displayName.contains("("))
        }
    }

    @Test
    fun testWestminsterPostcodeLookup() = runTest {
        fakeService.stubbedDto = PostcodeLookupDto(
            postcode = "SW1A 1AA",
            adminDistrict = "Westminster",
            region = "London"
        )

        val result = repository.lookupPostcode("SW1A 1AA")

        assertTrue(result.isSuccess)
        val schedule = result.getOrNull()!!
        assertEquals("Westminster City Council", schedule.councilName)
        assertEquals(3, schedule.binSetups.size)

        val binColors = schedule.binSetups.map { it.presetColor }
        assertTrue(binColors.contains(BinColor.BLACK))
        assertTrue(binColors.contains(BinColor.BLUE))
        assertTrue(binColors.contains(BinColor.BROWN))

        schedule.binSetups.forEach { bin ->
            assertFalse("Bin display name '${bin.displayName}' should not contain '('", bin.displayName.contains("("))
        }
    }

    @Test
    fun testBirminghamPostcodeLookup() = runTest {
        fakeService.stubbedDto = PostcodeLookupDto(
            postcode = "B1 1BB",
            adminDistrict = "Birmingham",
            region = "West Midlands"
        )

        val result = repository.lookupPostcode("B1 1BB")

        assertTrue(result.isSuccess)
        val schedule = result.getOrNull()!!
        assertEquals("Birmingham City Council", schedule.councilName)
    }

    @Test
    fun testDefaultCouncilLookup() = runTest {
        fakeService.stubbedDto = PostcodeLookupDto(
            postcode = "CR0 1EA",
            adminDistrict = "Croydon",
            region = "London"
        )

        val result = repository.lookupPostcode("CR0 1EA")

        assertTrue(result.isSuccess)
        val schedule = result.getOrNull()!!
        assertEquals("Croydon Council", schedule.councilName)
        assertEquals(4, schedule.binSetups.size)

        schedule.binSetups.forEach { bin ->
            assertFalse("Bin display name '${bin.displayName}' should not contain '('", bin.displayName.contains("("))
        }
    }

    @Test
    fun testStAlbansPostcodeLookup() = runTest {
        fakeService.stubbedDto = PostcodeLookupDto(
            postcode = "AL1 3UU",
            adminDistrict = "St Albans",
            region = "East of England"
        )

        val result = repository.lookupPostcode("AL1 3UU")

        assertTrue(result.isSuccess)
        val schedule = result.getOrNull()!!
        assertEquals("St Albans City and District Council", schedule.councilName)
        assertEquals(5, schedule.binSetups.size)

        val generalWaste = schedule.binSetups.first { it.binType == "General Waste" }
        assertEquals(BinColor.BLACK, generalWaste.presetColor)
        assertEquals(RecurrenceType.FORTNIGHTLY, generalWaste.recurrence)

        val recycling = schedule.binSetups.first { it.binType == "Recycling" }
        assertEquals(BinColor.BROWN, recycling.presetColor)
        assertEquals(RecurrenceType.FORTNIGHTLY, recycling.recurrence)

        val paper = schedule.binSetups.first { it.binType == "Paper & Cardboard" }
        assertEquals(BinColor.GREEN, paper.presetColor)
        assertEquals(RecurrenceType.FORTNIGHTLY, paper.recurrence)

        val food = schedule.binSetups.first { it.binType == "Food Waste Caddy" }
        assertEquals(BinColor.GREEN, food.presetColor)
        assertEquals(RecurrenceType.WEEKLY, food.recurrence)

        val garden = schedule.binSetups.first { it.binType == "Garden Waste" }
        assertEquals(BinColor.GREEN, garden.presetColor)
        assertEquals(RecurrenceType.FORTNIGHTLY, garden.recurrence)

        schedule.binSetups.forEach { bin ->
            assertFalse("Bin display name '${bin.displayName}' should not contain '('", bin.displayName.contains("("))
        }
    }

    @Test
    fun testScottishCouncilLookup() = runTest {
        fakeService.stubbedDto = PostcodeLookupDto(
            postcode = "KY1 1AA",
            adminDistrict = "Fife",
            region = "Scotland"
        )

        val result = repository.lookupPostcode("KY1 1AA")

        assertTrue(result.isSuccess)
        val schedule = result.getOrNull()!!
        assertEquals("Fife Council", schedule.councilName)
        assertEquals(4, schedule.binSetups.size)
    }

    @Test
    fun testWelshCouncilLookup() = runTest {
        fakeService.stubbedDto = PostcodeLookupDto(
            postcode = "LD1 5AB",
            adminDistrict = "Powys",
            region = "Wales"
        )

        val result = repository.lookupPostcode("LD1 5AB")

        assertTrue(result.isSuccess)
        val schedule = result.getOrNull()!!
        assertEquals("Powys Council", schedule.councilName)
        assertEquals(4, schedule.binSetups.size)
    }

    @Test
    fun testNorthernIrelandCouncilLookup() = runTest {
        fakeService.stubbedDto = PostcodeLookupDto(
            postcode = "BT1 5GS",
            adminDistrict = "Belfast",
            region = "Northern Ireland"
        )

        val result = repository.lookupPostcode("BT1 5GS")

        assertTrue(result.isSuccess)
        val schedule = result.getOrNull()!!
        assertEquals("Belfast City Council", schedule.councilName)
        assertEquals(3, schedule.binSetups.size)
    }

    @Test
    fun testStAlbansDirectCouncilNameLookup() = runTest {
        fakeService.stubbedDto = null

        val result = repository.lookupPostcode("St Albans City Council")

        assertTrue(result.isSuccess)
        val schedule = result.getOrNull()!!
        assertEquals("St Albans City and District Council", schedule.councilName)
        assertEquals(5, schedule.binSetups.size)
    }

    @Test
    fun testDirectCouncilNameFallbackWhenServiceReturnsNull() = runTest {
        fakeService.stubbedDto = null

        val result = repository.lookupPostcode("Manchester City Council")

        assertTrue(result.isSuccess)
        val schedule = result.getOrNull()!!
        assertEquals("Manchester City Council", schedule.councilName)
    }

    @Test
    fun testBlankPostcodeReturnsFailure() = runTest {
        val result = repository.lookupPostcode("   ")
        assertTrue(result.isFailure)
    }

    @Test
    fun testServiceFailureWithUnknownPostcodeReturnsFailure() = runTest {
        fakeService.stubbedDto = null

        val result = repository.lookupPostcode("INVALID999")
        assertTrue(result.isFailure)
    }

    private class FakeCouncilLookupService : CouncilLookupService {
        var stubbedDto: PostcodeLookupDto? = null
        var lastQueriedPostcode: String? = null

        override suspend fun lookupPostcode(postcode: String): PostcodeLookupDto? {
            lastQueriedPostcode = postcode
            return stubbedDto
        }
    }
}
