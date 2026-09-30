package com.levdev.binday.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.widget.RemoteViews
import com.levdev.binday.BinMinderApplication
import com.levdev.binday.MainActivity
import com.levdev.binday.R
import com.levdev.binday.data.model.AppThemeMode
import com.levdev.binday.data.model.CollectionEvent
import com.levdev.binday.engine.ScheduleEngine
import com.levdev.binday.worker.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class BinDayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateWidgets(context, appWidgetManager, appWidgetIds)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun updateWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val repository = (context.applicationContext as BinMinderApplication).container.binRepository
        val bins = repository.allBins.firstOrNull() ?: emptyList()
        val putOutBins = repository.putOutBins.firstOrNull() ?: emptySet()
        val themeMode = repository.themeMode.firstOrNull() ?: AppThemeMode.SYSTEM
        
        val today = LocalDate.now()
        val maxDate = today.plusDays(30)
        
        val allEvents = ScheduleEngine.generateCollectionEvents(bins, today, maxDate)
        
        val nextCollectionDate = allEvents.minOfOrNull { it.collectionDate }
        
        val headerText: String
        val dateText: String
        val binsText: String
        val isPutOut: Boolean

        var nextCollectionEvents: List<CollectionEvent> = emptyList()

        if (nextCollectionDate != null) {
            val eventsForNextDate = allEvents.filter { it.collectionDate == nextCollectionDate }
            nextCollectionEvents = eventsForNextDate
            
            val formattedDate = when (nextCollectionDate) {
                today -> "Today"
                today.plusDays(1) -> "Tomorrow, " + nextCollectionDate.format(DateTimeFormatter.ofPattern("d MMM"))
                else -> nextCollectionDate.format(DateTimeFormatter.ofPattern("EEEE, d MMM"))
            }
            
            val binNames = eventsForNextDate.map { it.binName }
            val binNamesStr = binNames.joinToString(", ")
            
            val nextDateStr = nextCollectionDate.toString()
            
            val allPutOutForThisDate = eventsForNextDate.isNotEmpty() && eventsForNextDate.all { event ->
                val putOutId = "${event.binId}_$nextDateStr"
                putOutBins.contains(putOutId)
            }
            
            headerText = "Next Collection"
            dateText = formattedDate
            binsText = binNamesStr
            isPutOut = allPutOutForThisDate
        } else {
            headerText = "Next Collection"
            dateText = "No collections scheduled"
            binsText = ""
            isPutOut = true
        }

        val isSystemDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val isDark = themeMode == AppThemeMode.DARK || (themeMode == AppThemeMode.SYSTEM && isSystemDark)
        val unselectedPutOutColor = if (isDark) Color.parseColor("#00D053") else Color.parseColor("#046A38")

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_next_collection)
            
            // Apply Theme
            when (themeMode) {
                AppThemeMode.DARK -> views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_bg_dark)
                AppThemeMode.LIGHT -> views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_bg_light)
                AppThemeMode.SYSTEM -> views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_bg)
            }
            
            if (isDark) {
                views.setTextColor(R.id.widget_header, Color.WHITE)
                views.setTextColor(R.id.widget_date, Color.WHITE)
                views.setTextColor(R.id.widget_bin_names, Color.WHITE)
            } else {
                views.setTextColor(R.id.widget_header, Color.parseColor("#046A38"))
                views.setTextColor(R.id.widget_date, Color.parseColor("#046A38"))
                views.setTextColor(R.id.widget_bin_names, Color.parseColor("#046A38"))
            }

            views.setTextViewText(R.id.widget_header, headerText)
            views.setTextViewText(R.id.widget_date, dateText)
            views.setTextViewText(R.id.widget_bin_names, binsText)
            
            if (nextCollectionEvents.isNotEmpty()) {
                val bitmap = createBinIconBitmap(nextCollectionEvents)
                views.setImageViewBitmap(R.id.widget_bin_icon, bitmap)
                views.setViewVisibility(R.id.widget_bin_icon, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_bin_icon, View.GONE)
            }
            
            // Launch app when clicking widget background
            val launchAppIntent = Intent(context, MainActivity::class.java)
            val launchAppPendingIntent = PendingIntent.getActivity(
                context, 0, launchAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, launchAppPendingIntent)
            
            // Pending intent for toggling
            val intent = Intent(context, BinDayWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_TOGGLE_PUT_OUT
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra("EXTRA_IS_PUT_OUT", isPutOut)
                putExtra("EXTRA_IS_DARK", isDark)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_put_out_container, pendingIntent)

            if (isPutOut) {
                views.setTextViewText(R.id.widget_btn_put_out, "Bins Are Out ✓")
                views.setTextColor(R.id.widget_btn_put_out, Color.BLACK)
                views.setInt(R.id.widget_btn_put_out_container, "setBackgroundResource", R.drawable.widget_btn_filled)
            } else {
                views.setTextViewText(R.id.widget_btn_put_out, "Put Out")
                views.setTextColor(R.id.widget_btn_put_out, unselectedPutOutColor)
                val bgResource = if (isDark) R.drawable.widget_btn_outlined_dark else R.drawable.widget_btn_outlined
                views.setInt(R.id.widget_btn_put_out_container, "setBackgroundResource", bgResource)
            }
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_TOGGLE_PUT_OUT || intent.action == ACTION_WIDGET_MARK_PUT_OUT) {
            
            // Optimistic UI Update
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val isPutOutCurrently = intent.getBooleanExtra("EXTRA_IS_PUT_OUT", false)
                val isDark = intent.getBooleanExtra("EXTRA_IS_DARK", false)
                val willBePutOut = !isPutOutCurrently
                
                val views = RemoteViews(context.packageName, R.layout.widget_next_collection)
                val unselectedPutOutColor = if (isDark) Color.parseColor("#00D053") else Color.parseColor("#046A38")
                
                if (willBePutOut) {
                    views.setTextViewText(R.id.widget_btn_put_out, "Bins Are Out ✓")
                    views.setTextColor(R.id.widget_btn_put_out, Color.BLACK)
                    views.setInt(R.id.widget_btn_put_out_container, "setBackgroundResource", R.drawable.widget_btn_filled)
                } else {
                    views.setTextViewText(R.id.widget_btn_put_out, "Put Out")
                    views.setTextColor(R.id.widget_btn_put_out, unselectedPutOutColor)
                    val bgResource = if (isDark) R.drawable.widget_btn_outlined_dark else R.drawable.widget_btn_outlined
                    views.setInt(R.id.widget_btn_put_out_container, "setBackgroundResource", bgResource)
                }
                
                val updatedIntent = Intent(context, BinDayWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_TOGGLE_PUT_OUT
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    putExtra("EXTRA_IS_PUT_OUT", willBePutOut)
                    putExtra("EXTRA_IS_DARK", isDark)
                }
                val updatedPendingIntent = PendingIntent.getBroadcast(
                    context, appWidgetId, updatedIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_put_out_container, updatedPendingIntent)

                AppWidgetManager.getInstance(context).partiallyUpdateAppWidget(appWidgetId, views)
            }

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    toggleNextCollectionPutOutStatus(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private suspend fun toggleNextCollectionPutOutStatus(context: Context) {
        val repository = (context.applicationContext as BinMinderApplication).container.binRepository
        val bins = repository.allBins.firstOrNull() ?: emptyList()
        val putOutBins = repository.putOutBins.firstOrNull()?.toMutableSet() ?: mutableSetOf()
        
        val today = LocalDate.now()
        val maxDate = today.plusDays(30)
        
        val allEvents = ScheduleEngine.generateCollectionEvents(bins, today, maxDate)
        val nextCollectionEvent = allEvents.minByOrNull { it.collectionDate }
        val nextCollectionDate = nextCollectionEvent?.collectionDate
        
        if (nextCollectionDate != null) {
            val eventsForNextDate = allEvents.filter { it.collectionDate == nextCollectionDate }
            val nextDateStr = nextCollectionDate.toString()
            
            var changed = false
            
            val allAlreadyPutOut = eventsForNextDate.all { event ->
                putOutBins.contains("${event.binId}_$nextDateStr")
            }
            
            if (allAlreadyPutOut) {
                for (event in eventsForNextDate) {
                    val putOutId = "${event.binId}_$nextDateStr"
                    if (putOutBins.contains(putOutId)) {
                        putOutBins.remove(putOutId)
                        changed = true
                    }
                }
            } else {
                for (event in eventsForNextDate) {
                    val putOutId = "${event.binId}_$nextDateStr"
                    if (!putOutBins.contains(putOutId)) {
                        putOutBins.add(putOutId)
                        changed = true
                    }
                }
            }
            
            if (changed) {
                repository.updatePutOutBins(putOutBins)
                NotificationScheduler.scheduleNotificationWorkerSuspend(context, null)
                updateAllWidgets(context)
            }
        }
    }

    companion object {
        const val ACTION_WIDGET_MARK_PUT_OUT = "com.levdev.binday.widget.ACTION_WIDGET_MARK_PUT_OUT"
        const val ACTION_WIDGET_TOGGLE_PUT_OUT = "com.levdev.binday.widget.ACTION_WIDGET_TOGGLE_PUT_OUT"

        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
                val intent = Intent(context, BinDayWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                }
                val ids = appWidgetManager
                    .getAppWidgetIds(ComponentName(context, BinDayWidgetProvider::class.java))
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                context.sendBroadcast(intent)
            } catch (e: Exception) {
                // Ignore if AppWidgetManager is unavailable (e.g. in unit tests)
            }
        }
        
        private fun createBinIconBitmap(events: List<CollectionEvent>): Bitmap {
            val size = 64
            val padding = 8
            val width = if (events.isEmpty()) size else size * events.size + padding * (events.size - 1)
            val bitmap = Bitmap.createBitmap(width.coerceAtLeast(size), size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            
            if (events.isEmpty()) {
                paint.color = Color.GRAY
                canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
                return bitmap
            }
            
            var xOffset = 0f
            for (event in events) {
                val bodyColor = parseColorSafely(event.binColorHex)
                val lidColorHex = event.lidColorHex
                val hasDistinctLid = event.lidPresetColor != null && lidColorHex != null && lidColorHex != event.binColorHex
                val lidColor = if (hasDistinctLid) parseColorSafely(lidColorHex) else null
                
                val cx = xOffset + size / 2f
                val cy = size / 2f
                val radius = size / 2f
                
                if (lidColor != null) {
                    val rectF = RectF(xOffset, 0f, xOffset + size, size.toFloat())
                    // Top half (lid)
                    paint.color = lidColor
                    canvas.drawArc(rectF, 180f, 180f, true, paint)
                    // Bottom half (body)
                    paint.color = bodyColor
                    canvas.drawArc(rectF, 0f, 180f, true, paint)
                } else {
                    paint.color = bodyColor
                    canvas.drawCircle(cx, cy, radius, paint)
                }
                
                xOffset += size + padding
            }
            return bitmap
        }

        private fun parseColorSafely(colorHex: String?): Int {
            if (colorHex.isNullOrBlank()) return Color.GRAY
            return try {
                val cleanHex = colorHex.trim().removePrefix("#")
                when (cleanHex.length) {
                    6 -> Color.parseColor("#FF$cleanHex")
                    8 -> Color.parseColor("#$cleanHex")
                    else -> Color.GRAY
                }
            } catch (e: Exception) {
                Color.GRAY
            }
        }
    }
}
