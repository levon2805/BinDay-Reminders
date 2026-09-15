package com.example.binminder.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.binminder.data.model.NotificationSettings
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Object responsible for scheduling and cancelling periodic WorkManager jobs for bin reminders.
 */
object NotificationScheduler {

    const val WORK_NAME = "binminder_daily_reminder_work"

    /**
     * Schedules or updates the daily background WorkManager task according to user preferences.
     */
    fun scheduleDailyReminder(context: Context, settings: NotificationSettings) {
        try {
            val appContext = context.applicationContext
            val workManager = WorkManager.getInstance(appContext)

            if (!settings.reminderEnabled) {
                workManager.cancelUniqueWork(WORK_NAME)
                return
            }

            val now = LocalDateTime.now()
            val targetToday = LocalDateTime.of(LocalDate.now(), settings.reminderTime)
            val targetTime = if (now.isAfter(targetToday)) {
                targetToday.plusDays(1)
            } else {
                targetToday
            }

            val delayMillis = Duration.between(now, targetTime).toMillis().coerceAtLeast(0)

            val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        } catch (_: Exception) {
            // Safe fallback for unit tests or uninitialised WorkManager context
        }
    }

    /**
     * Sends an immediate test notification to confirm reminder configuration.
     */
    fun sendImmediateTestNotification(context: Context) {
        try {
            NotificationHelper.postCollectionReminderNotification(
                context = context.applicationContext,
                title = "BinDay Test Reminder 🚛",
                message = "Test reminder successful! Your wheelie bin collection notifications are configured correctly.",
                notificationId = 9999
            )
        } catch (_: Exception) {}
    }

    /**
     * Cancels any active periodic reminder task.
     */
    fun cancelReminder(context: Context) {
        try {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
        } catch (_: Exception) {
            // Safe fallback for unit tests or uninitialised WorkManager context
        }
    }
}
