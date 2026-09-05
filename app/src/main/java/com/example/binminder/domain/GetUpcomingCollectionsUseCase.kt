package com.example.binminder.domain

import com.example.binminder.data.model.CollectionEvent
import com.example.binminder.data.repository.BinRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Domain Use Case for calculating and retrieving chronological bin collection schedules.
 *
 * Keeps your timetable sorted and proper, so you know exactly which wheelie bin goes out next!
 */
class GetUpcomingCollectionsUseCase(
    private val repository: BinRepository
) {
    /**
     * Returns a reactive flow of upcoming collection events between [startDate] and [endDate].
     */
    operator fun invoke(startDate: LocalDate, endDate: LocalDate): Flow<List<CollectionEvent>> {
        return repository.getUpcomingCollectionEvents(startDate, endDate)
    }
}
