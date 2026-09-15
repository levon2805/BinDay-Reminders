package com.example.binminder.domain

import com.example.binminder.data.model.CouncilScheduleResult
import com.example.binminder.data.repository.CouncilLookupRepository

/**
 * Domain Use Case for identifying a user's local UK council via postcode lookup.
 *
 * Returns the council name and a web search URL so users can find their
 * actual bin collection schedule on their council's website.
 */
class LookupCouncilScheduleUseCase(
    private val repository: CouncilLookupRepository
) {
    /**
     * Looks up council identification for a given UK postcode query string.
     */
    suspend operator fun invoke(postcodeOrQuery: String): Result<CouncilScheduleResult> {
        return repository.lookupPostcode(postcodeOrQuery)
    }
}
