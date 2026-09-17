package com.example.binminder.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.binminder.data.local.AppDatabase
import com.example.binminder.data.local.NotificationSettingsDataStore
import com.example.binminder.data.model.Bin
import com.example.binminder.data.model.NotificationSettings
import com.example.binminder.data.repository.BinRepositoryImpl
import com.example.binminder.engine.ScheduleEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

enum class ReminderSlot {
    EVENING,
    MORNING
}

data class ReminderTarget(
    val slot: ReminderSlot,
    val targetDateTime: LocalDateTime,
    val collectionDate: LocalDate,
    val delayMillis: Long
)

/**
 * Dual Notification Engine responsible for scheduling exact alarms via AlarmManager,
 * fallback OneTime WorkManager tasks, and daily Periodic WorkManager tasks.
 */
object NotificationScheduler {

    const val WORK_NAME = "binminder_daily_reminder_work"
    const val WORK_NAME_EVENING = "binminder_evening_reminder_work"
    const val WORK_NAME_MORNING = "binminder_morning_reminder_work"
    const val WORK_NAME_ONETIME_EVENING = "binminder_onetime_EVENING"
    const val WORK_NAME_ONETIME_MORNING = "binminder_onetime_MORNING"

    const val REQUEST_CODE_EVENING = 2001
    const val REQUEST_CODE_MORNING = 2002

    private const val TAG = "NotificationScheduler"

    /**
     * Pure calculation function to determine exact target reminder times and delays in milliseconds
     * for upcoming bin collections based on user settings.
     */
    fun calculateNextReminderTargets(
        bins: List<Bin>,
        settings: NotificationSettings,
        now: LocalDateTime = LocalDateTime.now()
    ): List<ReminderTarget> {
        if (!settings.reminderEnabled || (settings.eveningReminderTime == null && settings.morningReminderTime == null)) {
            return emptyList()
        }

        val activeBins = bins.filter { it.isEnabled }
        if (activeBins.isEmpty()) {
            return emptyList()
        }

        val startDate = now.toLocalDate()
        val endDate = startDate.plusDays(30)
        val events = ScheduleEngine.generateCollectionEvents(activeBins, startDate, endDate)
        val distinctCollectionDates = events.map { it.collectionDate }.distinct().sorted()

        val targets = mutableListOf<ReminderTarget>()

        for (collectionDate in distinctCollectionDates) {
            // 1. Evening Before reminder
            if (settings.eveningReminderTime != null) {
                val eveningTargetDate = collectionDate.minusDays(1)
                val eveningTargetDateTime = LocalDateTime.of(eveningTargetDate, settings.eveningReminderTime)
                if (eveningTargetDateTime.isAfter(now)) {
                    val delay = Duration.between(now, eveningTargetDateTime).toMillis().coerceAtLeast(0)
                    targets.add(ReminderTarget(ReminderSlot.EVENING, eveningTargetDateTime, collectionDate, delay))
                }
            }

            // 2. Morning Of reminder
            if (settings.morningReminderTime != null) {
                val morningTargetDateTime = LocalDateTime.of(collectionDate, settings.morningReminderTime)
                if (morningTargetDateTime.isAfter(now)) {
                    val delay = Duration.between(now, morningTargetDateTime).toMillis().coerceAtLeast(0)
                    targets.add(ReminderTarget(ReminderSlot.MORNING, morningTargetDateTime, collectionDate, delay))
                }
            }
        }

        // Return earliest future target for each slot (EVENING and MORNING)
        val earliestEvening = targets.filter { it.slot == ReminderSlot.EVENING }.minByOrNull { it.targetDateTime }
        val earliestMorning = targets.filter { it.slot == ReminderSlot.MORNING }.minByOrNull { it.targetDateTime }

        return listOfNotNull(earliestEvening, earliestMorning).sortedBy { it.targetDateTime }
    }

    /**
     * Core scheduling method: recalculates target times and schedules exact alarms,
     * high-priority OneTime WorkManager fallbacks, and daily Periodic WorkManager tasks.
     */
    fun scheduleNotificationWorker(context: Context, settings: NotificationSettings? = null) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getInstance(appContext)
                val dataStore = NotificationSettingsDataStore(appContext)
                val repository = BinRepositoryImpl(database.binDao(), dataStore, appContext)

                val currentSettings = settings ?: repository.notificationSettings.first()
                if (!currentSettings.reminderEnabled) {
                    cancelReminder(appContext)
                    return@launch
                }

                val bins = repository.getBinsList().filter { it.isEnabled }
                if (bins.isEmpty()) {
                    cancelReminder(appContext)
                    return@launch
                }

                val now = LocalDateTime.now()
                val targets = calculateNextReminderTargets(bins, currentSettings, now)

                if (targets.isEmpty()) {
                    cancelReminder(appContext)
                    return@launch
                }

                val workManager = try {
                    WorkManager.getInstance(appContext)
                } catch (_: Throwable) {
                    null
                }

                for (target in targets) {
                    // A. Schedule Exact Alarm via AlarmManager
                    scheduleExactAlarm(appContext, target.slot, target.targetDateTime, target.collectionDate)

                    // B. Enqueue High-Priority OneTime WorkManager Fallback
                    if (workManager != null) {
                        // Cancel any legacy periodic workers to prevent incorrect daily firing
                        workManager.cancelUniqueWork(WORK_NAME)
                        workManager.cancelUniqueWork(WORK_NAME_EVENING)
                        workManager.cancelUniqueWork(WORK_NAME_MORNING)

                        try {
                            val workData = workDataOf(
                                "REMINDER_SLOT" to target.slot.name,
                                "TARGET_DATE" to target.collectionDate.toString()
                            )
                            val oneTimeRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                                // Add 2-minute buffer so WorkManager fallback only runs if AlarmManager fails/is killed
                                .setInitialDelay(target.delayMillis + 120_000L, TimeUnit.MILLISECONDS)
                                .setInputData(workData)
                                .build()

                            val oneTimeWorkName = if (target.slot == ReminderSlot.EVENING) WORK_NAME_ONETIME_EVENING else WORK_NAME_ONETIME_MORNING
                            workManager.enqueueUniqueWork(
                                oneTimeWorkName,
                                ExistingWorkPolicy.REPLACE,
                                oneTimeRequest
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error scheduling fallback OneTimeWorkRequest for ${target.slot}", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error running scheduleNotificationWorker", e)
            }
        }
    }

    /**
     * Backward-compatible helper method that delegates to [scheduleNotificationWorker].
     */
    fun scheduleDailyReminder(context: Context, settings: NotificationSettings) {
        scheduleNotificationWorker(context, settings)
    }

    private fun scheduleExactAlarm(
        context: Context,
        slot: ReminderSlot,
        targetDateTime: LocalDateTime,
        collectionDate: LocalDate
    ) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
                action = "com.example.binminder.ACTION_SHOW_REMINDER"
                putExtra("REMINDER_SLOT", slot.name)
                putExtra("TARGET_DATE", collectionDate.toString())
            }

            val requestCode = if (slot == ReminderSlot.EVENING) REQUEST_CODE_EVENING else REQUEST_CODE_MORNING
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerAtMillis = targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule exact alarm for $slot", e)
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
        } catch (_: Throwable) {}
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
        } catch (_: Throwable) {}
    }

    /**
     * Cancels active exact alarms and WorkManager reminder tasks.
     */
    fun cancelReminder(context: Context) {
        try {
            val appContext = context.applicationContext
            val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager != null) {
                val intentEvening = Intent(appContext, NotificationAlarmReceiver::class.java)
                val pendingEvening = PendingIntent.getBroadcast(
                    appContext,
                    REQUEST_CODE_EVENING,
                    intentEvening,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pendingEvening)

                val intentMorning = Intent(appContext, NotificationAlarmReceiver::class.java)
                val pendingMorning = PendingIntent.getBroadcast(
                    appContext,
                    REQUEST_CODE_MORNING,
                    intentMorning,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pendingMorning)
            }

            val workManager = try {
                WorkManager.getInstance(appContext)
            } catch (_: Throwable) {
                null
            }

            if (workManager != null) {
                workManager.cancelUniqueWork(WORK_NAME)
                workManager.cancelUniqueWork(WORK_NAME_EVENING)
                workManager.cancelUniqueWork(WORK_NAME_MORNING)
                workManager.cancelUniqueWork(WORK_NAME_ONETIME_EVENING)
                workManager.cancelUniqueWork(WORK_NAME_ONETIME_MORNING)
            }
        } catch (_: Throwable) {
            // Safe fallback for unit tests or uninitialised WorkManager context
        }
    }
}
