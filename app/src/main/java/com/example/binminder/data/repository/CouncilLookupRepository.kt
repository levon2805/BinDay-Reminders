package com.example.binminder.data.repository

import com.example.binminder.data.model.CouncilScheduleResult
import com.example.binminder.data.service.CouncilLookupService
import com.example.binminder.data.service.CouncilLookupServiceImpl
import com.example.binminder.data.service.PostcodeLookupDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

/**
 * Repository interface for identifying a user's local UK council from a postcode.
 */
interface CouncilLookupRepository {
    /**
     * Looks up the local council for a UK postcode and returns a search URL
     * for the council's bin collection schedule page.
     */
    suspend fun lookupPostcode(postcodeOrQuery: String): Result<CouncilScheduleResult>
}

/**
 * Implementation of [CouncilLookupRepository] that queries postcodes.io to identify
 * the local council and generates a web search URL for the council's bin collection page.
 *
 * This intentionally does NOT attempt to guess bin types, colours, or collection
 * frequencies — those vary too widely across UK councils to be reliably hardcoded.
 * Instead, the user is directed to their council's website and guided through manual setup.
 */
class CouncilLookupRepositoryImpl(
    private val service: CouncilLookupService = CouncilLookupServiceImpl()
) : CouncilLookupRepository {

    /**
     * Resolves the local council from a UK postcode and builds a council website search URL.
     */
    override suspend fun lookupPostcode(postcodeOrQuery: String): Result<CouncilScheduleResult> = withContext(Dispatchers.IO) {
        val trimmed = postcodeOrQuery.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Postcode cannot be blank."))
        }

        val sanitizedPostcode = postcodeOrQuery.replace("\\s+".toRegex(), "").trim().uppercase()

        runCatching {
            val dto = service.lookupPostcode(sanitizedPostcode)
            if (dto != null) {
                return@runCatching resolveCouncilResult(dto)
            }

            throw Exception("Could not identify council for: $postcodeOrQuery")
        }.onFailure { if (it is CancellationException) throw it }
    }

    private fun resolveCouncilResult(dto: PostcodeLookupDto): CouncilScheduleResult {
        val admin = dto.adminDistrict.trim()
        val councilName = formatCouncilName(admin)
        val searchUrl = buildCouncilSearchUrl(councilName)

        return CouncilScheduleResult(
            postcode = dto.postcode,
            councilName = councilName,
            adminDistrict = admin,
            councilWebSearchUrl = searchUrl
        )
    }

    /**
     * Builds a Google search URL for the council's bin collection schedule page.
     */
    private fun buildCouncilSearchUrl(councilName: String): String {
        val query = "$councilName bin collection schedule"
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        return "https://www.google.com/search?q=$encodedQuery"
    }

    private fun formatCouncilName(admin: String): String {
        val trimmed = admin.trim()
        val lower = trimmed.lowercase()

        return when {
            lower.contains("council") -> trimmed
            lower.contains("borough") -> if (lower.contains("council")) trimmed else "$trimmed Council"
            lower.contains("district") -> if (lower.contains("council")) trimmed else "$trimmed Council"
            lower.contains("city") -> if (lower.contains("council")) trimmed else "$trimmed Council"
            else -> "$trimmed Council"
        }
    }
}
