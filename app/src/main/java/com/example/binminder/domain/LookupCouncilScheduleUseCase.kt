package com.example.binminder.domain

import com.example.binminder.data.model.CouncilScheduleResult
import com.example.binminder.data.repository.CouncilLookupRepository

/**
 * Domain Use Case for resolving local UK council bin collection schedules via postcode or council query.
 *
 * Right handy for auto-completing timetable defaults during onboarding without any fuss!
 */
class LookupCouncilScheduleUseCase(
    private val repository: CouncilLookupRepository
) {
    /**
     * Looks up council timetable options for a given UK postcode or council query string.
     */
    suspend operator fun invoke(postcodeOrQuery: String): Result<CouncilScheduleResult> {
        return repository.lookupPostcode(postcodeOrQuery)
    }
}
