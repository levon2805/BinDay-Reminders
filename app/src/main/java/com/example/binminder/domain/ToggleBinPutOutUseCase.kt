package com.example.binminder.domain

/**
 * Result data class holding updated kerbside bin state and user notification message.
 */
data class ToggleBinPutOutResult(
    val updatedPutOutBins: Set<Long>,
    val userMessage: String
)

/**
 * Domain Use Case for marking a wheelie bin as put out on the kerb or unmarking it.
 *
 * Sorted mate! Gives instant feedback when you've wheeled your bin out to the kerbside.
 */
class ToggleBinPutOutUseCase {
    /**
     * Toggles the put-out state of [binId] in [currentPutOutBins] and returns the updated set with user feedback.
     */
    operator fun invoke(
        binId: Long,
        binName: String,
        currentPutOutBins: Set<Long>
    ): ToggleBinPutOutResult {
        val newSet = currentPutOutBins.toMutableSet()
        val isNowPutOut = if (newSet.contains(binId)) {
            newSet.remove(binId)
            false
        } else {
            newSet.add(binId)
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
