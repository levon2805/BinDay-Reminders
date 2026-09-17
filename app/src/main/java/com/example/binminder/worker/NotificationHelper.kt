package com.example.binminder.worker

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.binminder.MainActivity
import com.example.binminder.R

/**
 * Helper object for building and posting high-priority heads-up bin collection system notifications.
 */
object NotificationHelper {

    const val CHANNEL_ID = "bin_collection_reminders"
    private const val CHANNEL_NAME = "Bin Collection Reminders"
    private const val CHANNEL_DESCRIPTION = "Notifications for upcoming bin collection schedules"

    /**
     * Creates the Android notification channel required for collection reminders with high importance and heads-up visibility.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                enableLights(true)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Posts a high-priority heads-up bin collection reminder notification to the Android system tray.
     */
    @SuppressLint("MissingPermission")
    fun postCollectionReminderNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = 1001,
        binIds: List<Long> = emptyList(),
        binNames: String = "",
        targetDateStr: String = ""
    ) {
        createNotificationChannel(context)

        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        // --- Anti-Spam / Debounce Logic ---
        // Prevents the WorkManager fallback from double-posting if the exact AlarmManager already succeeded.
        // It relies on a local timestamp record rather than active system notifications,
        // so it safely catches duplicates even if the user immediately swipes the first one away!
        val prefs = context.getSharedPreferences("notification_debounce", Context.MODE_PRIVATE)
        val debounceKey = "${notificationId}_${title}"
        val lastKey = prefs.getString("last_key", "")
        val lastTime = prefs.getLong("last_time", 0L)
        
        // 9999 is the test notification ID, always allow it through.
        // 1 hour window (3600000ms) to block the duplicate fallback.
        if (notificationId != 9999 && lastKey == debounceKey && (System.currentTimeMillis() - lastTime) < 3_600_000L) {
            return
        }
        
        prefs.edit()
            .putString("last_key", debounceKey)
            .putLong("last_time", System.currentTimeMillis())
            .apply()
        // ----------------------------------

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Add "Mark as Put Out" action button if bin data is provided
        if (binIds.isNotEmpty() && targetDateStr.isNotEmpty()) {
            val markPutOutIntent = Intent(context, MarkBinPutOutReceiver::class.java).apply {
                action = "com.example.binminder.ACTION_MARK_PUT_OUT"
                putExtra("NOTIFICATION_ID", notificationId)
                putExtra("TARGET_DATE", targetDateStr)
                putExtra("BIN_IDS", binIds.joinToString(","))
                putExtra("BIN_NAMES", binNames)
            }
            
            val markPutOutPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId, // Use notification ID to ensure unique pending intent
                markPutOutIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            builder.addAction(
                0, // No icon, keeps it sleek and system-default
                "Done",
                markPutOutPendingIntent
            )
        }

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    /**
     * Cancels an active bin collection notification from the system tray.
     */
    fun cancelNotification(context: Context, notificationId: Int = 1001) {
        try {
            NotificationManagerCompat.from(context).cancel(notificationId)
        } catch (_: Exception) {}
    }

    /**
     * Clears the anti-spam debounce cache.
     * Should be called whenever the user explicitly changes their reminder time or bin configuration
     * so that immediate testing is not blocked by the 1-hour anti-spam window.
     */
    fun clearDebounceCache(context: Context) {
        context.getSharedPreferences("notification_debounce", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
