package com.example.binminder.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
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
    const val WORK_NAME_EVENING = "binminder_evening_reminder_work"
    const val WORK_NAME_MORNING = "binminder_morning_reminder_work"

    /**
     * Schedules or updates daily background WorkManager tasks according to user preferences (Evening, Morning, or Both).
     */
    fun scheduleDailyReminder(context: Context, settings: NotificationSettings) {
        try {
            val appContext = context.applicationContext
            val workManager = WorkManager.getInstance(appContext)

            if (!settings.reminderEnabled || (settings.eveningReminderTime == null && settings.morningReminderTime == null)) {
                workManager.cancelUniqueWork(WORK_NAME)
                workManager.cancelUniqueWork(WORK_NAME_EVENING)
                workManager.cancelUniqueWork(WORK_NAME_MORNING)
                return
            }

            val now = LocalDateTime.now()

            // 1. Evening reminder slot
            if (settings.eveningReminderTime != null) {
                val targetToday = LocalDateTime.of(LocalDate.now(), settings.eveningReminderTime)
                val targetTime = if (now.isAfter(targetToday)) targetToday.plusDays(1) else targetToday
                val delayMillis = Duration.between(now, targetTime).toMillis().coerceAtLeast(0)

                val workData = workDataOf("REMINDER_SLOT" to "EVENING")
                val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(24, TimeUnit.HOURS)
                    .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                    .setInputData(workData)
                    .build()

                workManager.enqueueUniquePeriodicWork(
                    WORK_NAME_EVENING,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )
            } else {
                workManager.cancelUniqueWork(WORK_NAME_EVENING)
            }

            // 2. Morning reminder slot
            if (settings.morningReminderTime != null) {
                val targetToday = LocalDateTime.of(LocalDate.now(), settings.morningReminderTime)
                val targetTime = if (now.isAfter(targetToday)) targetToday.plusDays(1) else targetToday
                val delayMillis = Duration.between(now, targetTime).toMillis().coerceAtLeast(0)

                val workData = workDataOf("REMINDER_SLOT" to "MORNING")
                val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(24, TimeUnit.HOURS)
                    .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                    .setInputData(workData)
                    .build()

                workManager.enqueueUniquePeriodicWork(
                    WORK_NAME_MORNING,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )
            } else {
                workManager.cancelUniqueWork(WORK_NAME_MORNING)
            }

            workManager.cancelUniqueWork(WORK_NAME)
        } catch (_: Exception) {
            // Safe fallback for unit tests or uninitialised WorkManager context
        }
    }

    /**
     * Cancels or suppresses pending notifications when bins are marked as put out.
     */
    fun cancelOrSuppressNotificationForToday(context: Context, targetDate: LocalDate = LocalDate.now()) {
        try {
            val appContext = context.applicationContext
            NotificationHelper.cancelNotification(appContext, targetDate.hashCode())
            NotificationHelper.cancelNotification(appContext, targetDate.plusDays(1).hashCode())
            NotificationHelper.cancelNotification(appContext, 1001)
        } catch (_: Exception) {}
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
            val appContext = context.applicationContext
            val workManager = WorkManager.getInstance(appContext)
            workManager.cancelUniqueWork(WORK_NAME)
            workManager.cancelUniqueWork(WORK_NAME_EVENING)
            workManager.cancelUniqueWork(WORK_NAME_MORNING)
        } catch (_: Exception) {
            // Safe fallback for unit tests or uninitialised WorkManager context
        }
    }
}
