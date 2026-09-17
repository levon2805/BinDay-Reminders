package com.example.binminder.domain

import java.time.LocalDate

/**
 * Result data class holding updated kerbside bin state and user notification message.
 */
data class ToggleBinPutOutResult(
    val updatedPutOutBins: Set<String>,
    val userMessage: String
)

/**
 * Domain Use Case for marking a wheelie bin as put out on the kerb or unmarking it.
 *
 * Sorted mate! Gives instant feedback when you've wheeled your bin out to the kerbside.
 */
class ToggleBinPutOutUseCase {
    /**
     * Toggles the put-out state of [binId] for [collectionDate] in [currentPutOutBins] and returns the updated set with user feedback.
     */
    operator fun invoke(
        binId: Long,
        collectionDate: LocalDate,
        binName: String,
        currentPutOutBins: Set<String>
    ): ToggleBinPutOutResult {
        val newSet = currentPutOutBins.toMutableSet()
        val key = "${binId}_${collectionDate}"
        val isNowPutOut = if (newSet.contains(key)) {
            newSet.remove(key)
            false
        } else {
            newSet.add(key)
            true
        }

        val message = if (isNowPutOut) {
            "Marked '$binName' bin as put out for collection."
        } else {
            "Unmarked '$binName' bin."
        }

        return ToggleBinPutOutResult(
            updatedPutOutBins = newSet,
            userMessage = message
        )
    }
}
