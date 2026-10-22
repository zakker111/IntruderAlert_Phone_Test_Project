package com.example.intruderalert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Best-effort legacy callback; Android/OxygenOS may not deliver it on modern devices. */
class IntruderAdminReceiver : DeviceAdminReceiver() {
    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val count = prefs.getInt(KEY_EVENT_COUNT, 0) + 1
        val line = "$time — Failed-unlock callback received"
        val oldLog = prefs.getString(KEY_EVENT_LOG, "") ?: ""
        prefs.edit().putString(KEY_LAST_EVENT, line).putInt(KEY_EVENT_COUNT, count)
            .putString(KEY_EVENT_LOG, (line + if (oldLog.isBlank()) "" else "\n$oldLog").take(6000))
            .apply()
        notifyUser(context, time, count)
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent) {
        super.onPasswordSucceeded(context, intent)
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_LAST_SUCCESS, "Unlock callback received at $time").apply()
    }

    private fun notifyUser(context: Context, time: String, count: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL, "Intruder Alert", NotificationManager.IMPORTANCE_HIGH))
        }
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0))
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("Intruder Alert: failed-unlock event")
            .setContentText("Callback received at $time. Tap to review evidence options.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("A failed-unlock callback was received at $time (event $count). Tap to open Intruder Alert, check location, capture evidence, and prepare an email report."))
            .setContentIntent(open).setAutoCancel(true).build()
        manager.notify(101, notification)
    }

    companion object {
        const val PREFS = "intruder_alert_test"
        const val KEY_LAST_EVENT = "last_event"
        const val KEY_LAST_SUCCESS = "last_success"
        const val KEY_EVENT_COUNT = "event_count"
        const val KEY_EVENT_LOG = "event_log"
        const val KEY_LOCATION = "location_text"
        const val KEY_EMAIL = "alert_email"
        private const val CHANNEL = "intruder_alert_events"
    }
}
