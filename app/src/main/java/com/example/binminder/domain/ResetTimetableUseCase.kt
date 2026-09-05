package com.example.binminder.domain

import android.content.Context
import com.example.binminder.data.repository.BinRepository
import com.example.binminder.worker.NotificationScheduler

/**
 * Domain Use Case for resetting saved bin timetables, clearing setup preferences, and cancelling reminders.
 *
 * Clears out all the old bin data proper when moving house or restarting onboarding!
 */
class ResetTimetableUseCase(
    private val repository: BinRepository
) {
    /**
     * Clears stored bins, resets onboarding status to incomplete, and cancels scheduled notifications if context is provided.
     */
    suspend operator fun invoke(context: Context? = null): Result<Unit> = runCatching {
        repository.clearAllBins()
        repository.setOnboardingCompleted(false)
        if (context != null) {
            NotificationScheduler.cancelReminder(context)
        }
    }
}
