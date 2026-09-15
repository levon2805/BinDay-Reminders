package com.example.binminder.data.repository

import com.example.binminder.data.service.CouncilLookupService
import com.example.binminder.data.service.PostcodeLookupDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

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
    fun testPostcodeLookupReturnsCouncilName() = runTest {
        fakeService.stubbedDto = PostcodeLookupDto(
            postcode = "M1 1AE",
            adminDistrict = "Manchester",
            region = "North West"
        )

        val result = repository.lookupPostcode("M1 1AE")

        assertTrue(result.isSuccess)
        val council = result.getOrNull()!!
        assertEquals("Manchester Council", council.councilName)
        assertEquals("Manchester", council.adminDistrict)
    }

    @Test
    fun testPostcodeLookupReturnsValidSearchUrl() = runTest {
        fakeService.stubbedDto = PostcodeLookupDto(
            postcode = "SW1A 1AA",
            adminDistrict = "Westminster",
            region = "London"
        )

        val result = repository.lookupPostcode("SW1A 1AA")

        assertTrue(result.isSuccess)
        val council = result.getOrNull()!!
        assertTrue("URL should contain google search", council.councilWebSearchUrl.startsWith("https://www.google.com/search?q="))
        assertTrue("URL should mention council name", council.councilWebSearchUrl.contains("Westminster"))
        assertTrue("URL should mention bin collection", council.councilWebSearchUrl.contains("bin+collection"))
    }

    @Test
    fun testCouncilNameFormattingAddsCouncilSuffix() = runTest {
        fakeService.stubbedDto = PostcodeLookupDto(
            postcode = "EH1 1AA",
            adminDistrict = "Edinburgh",
            region = "Scotland"
        )

        val result = repository.lookupPostcode("EH1 1AA")

        assertTrue(result.isSuccess)
        val council = result.getOrNull()!!
        assertEquals("Edinburgh Council", council.councilName)
    }

    @Test
    fun testCouncilNameFormattingPreservesExistingCouncilSuffix() = runTest {
        fakeService.stubbedDto = PostcodeLookupDto(
            postcode = "B1 1AA",
            adminDistrict = "Birmingham City Council",
            region = "West Midlands"
        )

        val result = repository.lookupPostcode("B1 1AA")

        assertTrue(result.isSuccess)
        val council = result.getOrNull()!!
        assertEquals("Birmingham City Council", council.councilName)
    }

    @Test
    fun testPostcodePreservedInResult() = runTest {
        fakeService.stubbedDto = PostcodeLookupDto(
            postcode = "CF10 1AA",
            adminDistrict = "Cardiff",
            region = "Wales"
        )

        val result = repository.lookupPostcode("CF10 1AA")

        assertTrue(result.isSuccess)
        val council = result.getOrNull()!!
        assertEquals("CF10 1AA", council.postcode)
    }

    @Test
    fun testPostcodeSanitizationAndSpaceHandling() = runTest {
        fakeService.stubbedDto = PostcodeLookupDto(
            postcode = "SW1A 1AA",
            adminDistrict = "Westminster",
            region = "London"
        )

        val result = repository.lookupPostcode("  sw1a   1aa \t")

        assertTrue(result.isSuccess)
        assertEquals("SW1A1AA", fakeService.lastQueriedPostcode)
    }

    @Test
    fun testBlankPostcodeReturnsFailure() = runTest {
        val result = repository.lookupPostcode("   ")
        assertTrue(result.isFailure)
    }

    @Test
    fun testServiceReturnsNullFailsGracefully() = runTest {
        fakeService.stubbedDto = null

        val result = repository.lookupPostcode("INVALID999")
        assertTrue(result.isFailure)
    }

    @Test
    fun testServiceThrowsExceptionHandledSafely() = runTest {
        fakeService.shouldThrowException = true

        val result = repository.lookupPostcode("SW1A1AA")
        assertTrue(result.isFailure)
    }

    @Test
    fun testSearchUrlIsProperlyEncoded() = runTest {
        fakeService.stubbedDto = PostcodeLookupDto(
            postcode = "AL1 3UU",
            adminDistrict = "St Albans",
            region = "East of England"
        )

        val result = repository.lookupPostcode("AL1 3UU")

        assertTrue(result.isSuccess)
        val council = result.getOrNull()!!
        // URL should have encoded spaces as +
        assertTrue("URL should be properly encoded", council.councilWebSearchUrl.contains("St+Albans"))
    }

    private class FakeCouncilLookupService : CouncilLookupService {
        var stubbedDto: PostcodeLookupDto? = null
        var lastQueriedPostcode: String? = null
        var shouldThrowException: Boolean = false

        override suspend fun lookupPostcode(postcode: String): PostcodeLookupDto? {
            if (shouldThrowException) {
                throw IOException("Network timeout or connection error")
            }
            lastQueriedPostcode = postcode
            return stubbedDto
        }
    }
}
