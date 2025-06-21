package com.example.scheduleme

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.scheduleme.MainActivity.Companion.messageExtra
import com.example.scheduleme.MainActivity.Companion.titleExtra
import androidx.core.content.edit

object AlarmHelper {

    private const val PREFS_NAME = "scheduled_alarms"

    fun saveAlarm(context: Context, time: Long, title: String, message: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val alarmList = prefs.getStringSet("alarms", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        alarmList.add("$time||$title||$message")
        prefs.edit { putStringSet("alarms", alarmList) }
    }

    fun rescheduleAlarms(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val alarmList = prefs.getStringSet("alarms", null) ?: return

        for (entry in alarmList) {
            val parts = entry.split("||")
            if (parts.size == 3) {
                val time = parts[0].toLongOrNull() ?: continue
                val title = parts[1]
                val message = parts[2]

                if (time > System.currentTimeMillis()) {
                    scheduleNotification(context, time, title, message)
                }
            }
        }
    }

    fun scheduleNotification(context: Context, time: Long, title: String, message: String): Boolean {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(titleExtra, title)
            putExtra(messageExtra, message)
        }

        val requestCode = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
            } else {
                // Optional: Prompt user to allow exact alarms in settings
                return false // indicate that exact alarm could not be scheduled
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
        }
        return true
    }
}

