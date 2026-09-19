package com.example.binminder.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
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
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
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
    val delayMillis: Long,
    val reminderTime: LocalTime? = null
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
        if (!settings.reminderEnabled || (settings.eveningReminderTimes.isEmpty() && settings.morningReminderTimes.isEmpty())) {
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
        val pastCutoff = now.minusMinutes(1)

        for (collectionDate in distinctCollectionDates) {
            // 1. Evening Before reminders for each time in eveningReminderTimes
            for (time in settings.eveningReminderTimes) {
                val eveningTargetDate = collectionDate.minusDays(1)
                val eveningTargetDateTime = LocalDateTime.of(eveningTargetDate, time)
                if (!eveningTargetDateTime.isBefore(pastCutoff)) {
                    val rawDelayMillis = Duration.between(now, eveningTargetDateTime).toMillis()
                    val delayMillis = maxOf(1000L, rawDelayMillis)
                    targets.add(ReminderTarget(ReminderSlot.EVENING, eveningTargetDateTime, collectionDate, delayMillis, time))
                }
            }

            // 2. Morning Of reminders for each time in morningReminderTimes
            for (time in settings.morningReminderTimes) {
                val morningTargetDateTime = LocalDateTime.of(collectionDate, time)
                if (!morningTargetDateTime.isBefore(pastCutoff)) {
                    val rawDelayMillis = Duration.between(now, morningTargetDateTime).toMillis()
                    val delayMillis = maxOf(1000L, rawDelayMillis)
                    targets.add(ReminderTarget(ReminderSlot.MORNING, morningTargetDateTime, collectionDate, delayMillis, time))
                }
            }
        }

        val sortedTargets = targets.sortedBy { it.targetDateTime }
        if (sortedTargets.isEmpty()) return emptyList()

        val earliestCollectionDate = sortedTargets.minOf { it.collectionDate }
        return sortedTargets.filter { it.collectionDate == earliestCollectionDate }
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
                
                // Systematically cancel all previous exact alarms and background work tasks first
                cancelReminder(appContext, currentSettings)

                if (!currentSettings.reminderEnabled) {
                    return@launch
                }

                val bins = repository.getBinsList().filter { it.isEnabled }
                if (bins.isEmpty()) {
                    return@launch
                }

                val now = LocalDateTime.now()
                val targets = calculateNextReminderTargets(bins, currentSettings, now)

                if (targets.isEmpty()) {
                    return@launch
                }

                val workManager = try {
                    WorkManager.getInstance(appContext)
                } catch (_: Throwable) {
                    null
                }

                for (target in targets) {
                    // A. Schedule Exact Alarm via AlarmManager
                    scheduleExactAlarm(appContext, target.slot, target.targetDateTime, target.collectionDate, target.reminderTime)

                    // B. Enqueue High-Priority OneTime WorkManager Fallback
                    if (workManager != null) {
                        workManager.cancelUniqueWork(WORK_NAME)
                        workManager.cancelUniqueWork(WORK_NAME_EVENING)
                        workManager.cancelUniqueWork(WORK_NAME_MORNING)

                        try {
                            val workData = workDataOf(
                                "REMINDER_SLOT" to target.slot.name,
                                "TARGET_DATE" to target.collectionDate.toString()
                            )
                            val oneTimeRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                                .setInitialDelay(target.delayMillis + 120_000L, TimeUnit.MILLISECONDS)
                                .setInputData(workData)
                                .build()

                            val timeTag = target.reminderTime?.toString() ?: ""
                            val oneTimeWorkName = "binminder_onetime_${target.slot.name}_${target.collectionDate}_$timeTag"
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

    fun generateAlarmRequestCode(date: LocalDate, time: LocalTime, isMorning: Boolean): Int {
        val dateHash = date.year * 10000 + date.monthValue * 100 + date.dayOfMonth
        val timeHash = time.hour * 100 + time.minute
        val slotFlag = if (isMorning) 1 else 2
        return (dateHash.hashCode() * 31 + timeHash) * 31 + slotFlag
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: false
        } else {
            true
        }
    }

    private fun scheduleExactAlarm(
        context: Context,
        slot: ReminderSlot,
        targetDateTime: LocalDateTime,
        collectionDate: LocalDate,
        reminderTime: LocalTime? = null
    ) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
                action = "com.example.binminder.ACTION_SHOW_REMINDER"
                putExtra("REMINDER_SLOT", slot.name)
                putExtra("TARGET_DATE", collectionDate.toString())
                reminderTime?.let { putExtra("REMINDER_TIME", it.toString()) }
            }

            val time = reminderTime ?: targetDateTime.toLocalTime()
            val requestCode = generateAlarmRequestCode(collectionDate, time, slot == ReminderSlot.MORNING)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerAtMillis = targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    try {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    } catch (e: SecurityException) {
                        Log.w(TAG, "SecurityException on setExactAndAllowWhileIdle, falling back gracefully to setAndAllowWhileIdle", e)
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    }
                } else {
                    Log.w(TAG, "Exact alarm permission not granted (canScheduleExactAlarms == false). Falling back gracefully to setAndAllowWhileIdle.")
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else {
                try {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } catch (e: SecurityException) {
                    Log.w(TAG, "SecurityException on setExactAndAllowWhileIdle, falling back gracefully to setAndAllowWhileIdle", e)
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule exact alarm for $slot", e)
        }
    }

    /**
     * Cancels exact alarms and WorkManager tasks for a specific collection date when bins are put out.
     */
    fun cancelAlarmsForCollectionDate(context: Context, collectionDate: LocalDate, settings: NotificationSettings? = null) {
        try {
            val appContext = context.applicationContext
            val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager != null) {
                val currentSettings = settings ?: runBlocking { NotificationSettingsDataStore(appContext).notificationSettings.first() }

                for (time in currentSettings.eveningReminderTimes) {
                    val requestCode = generateAlarmRequestCode(collectionDate, time, isMorning = false)
                    val intent = Intent(appContext, NotificationAlarmReceiver::class.java).apply {
                        action = "com.example.binminder.ACTION_SHOW_REMINDER"
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        appContext,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                }

                for (time in currentSettings.morningReminderTimes) {
                    val requestCode = generateAlarmRequestCode(collectionDate, time, isMorning = true)
                    val intent = Intent(appContext, NotificationAlarmReceiver::class.java).apply {
                        action = "com.example.binminder.ACTION_SHOW_REMINDER"
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        appContext,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                }

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

            cancelOrSuppressNotificationForToday(appContext, collectionDate)
        } catch (_: Throwable) {}
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
    fun cancelReminder(context: Context, settings: NotificationSettings? = null) {
        try {
            val appContext = context.applicationContext
            val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager != null) {
                // Cancel legacy request codes
                val intentEvening = Intent(appContext, NotificationAlarmReceiver::class.java)
                val pendingEvening = PendingIntent.getBroadcast(
                    appContext,
                    REQUEST_CODE_EVENING,
                    intentEvening,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pendingEvening)
                pendingEvening.cancel()

                val intentMorning = Intent(appContext, NotificationAlarmReceiver::class.java)
                val pendingMorning = PendingIntent.getBroadcast(
                    appContext,
                    REQUEST_CODE_MORNING,
                    intentMorning,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pendingMorning)
                pendingMorning.cancel()

                // Systematically cancel all dynamic request codes for next 60 days
                val startDate = LocalDate.now().minusDays(2)
                val endDate = startDate.plusDays(60)
                val timesToCancel = mutableSetOf(
                    LocalTime.of(18, 0), LocalTime.of(19, 0), LocalTime.of(20, 0), LocalTime.of(21, 0),
                    LocalTime.of(6, 0), LocalTime.of(7, 0), LocalTime.of(8, 0), LocalTime.of(9, 0)
                )
                if (settings != null) {
                    timesToCancel.addAll(settings.eveningReminderTimes)
                    timesToCancel.addAll(settings.morningReminderTimes)
                }

                var date = startDate
                while (!date.isAfter(endDate)) {
                    for (slot in ReminderSlot.entries) {
                        for (time in timesToCancel) {
                            val requestCode = generateAlarmRequestCode(date, time, isMorning = (slot == ReminderSlot.MORNING))
                            val intent = Intent(appContext, NotificationAlarmReceiver::class.java).apply {
                                action = "com.example.binminder.ACTION_SHOW_REMINDER"
                            }
                            val pendingIntent = PendingIntent.getBroadcast(
                                appContext,
                                requestCode,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            alarmManager.cancel(pendingIntent)
                            pendingIntent.cancel()
                        }
                    }
                    date = date.plusDays(1)
                }
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
