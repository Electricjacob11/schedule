package com.example.scheduleme

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.scheduleme.MainActivity.Companion.messageExtra
import com.example.scheduleme.MainActivity.Companion.titleExtra
import androidx.core.content.edit
import java.util.UUID
import kotlin.math.absoluteValue

object AlarmHelper {

    private const val PREFS_NAME = "scheduled_alarms"

    fun saveAlarm(context: Context, time: Long, title: String, message: String, requestCode: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val alarmList = prefs.getStringSet("alarms", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        alarmList.add("$time||$title||$message||$requestCode")
        prefs.edit { putStringSet("alarms", alarmList) }
    }

    fun rescheduleAlarms(context: Context) {
        val alarms = getSavedAlarms(context)
        for (alarm in alarms) {
            scheduleNotification(context, alarm.time, alarm.title, alarm.message)
        }
    }


    fun scheduleNotification(context: Context, time: Long, title: String, message: String): Boolean {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(titleExtra, title)
            putExtra(messageExtra, message)
        }

        val requestCode = UUID.randomUUID().hashCode().absoluteValue
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

    fun cancelAlarm(context: Context, time: Long, title: String, message: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val alarmList = prefs.getStringSet("alarms", null)?.toMutableSet() ?: return

        val iterator = alarmList.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val parts = entry.split("||")
            if (parts.size == 4) {
                val savedTime = parts[0].toLongOrNull()
                val savedTitle = parts[1]
                val savedMessage = parts[2]
                val requestCode = parts[3].toIntOrNull()

                if (savedTime == time && savedTitle == title && savedMessage == message && requestCode != null) {
                    // Cancel the exact pending intent
                    val intent = Intent(context, NotificationReceiver::class.java).apply {
                        putExtra(titleExtra, title)
                        putExtra(messageExtra, message)
                    }

                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )

                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    alarmManager.cancel(pendingIntent)

                    // Remove from saved alarms
                    iterator.remove()
                }
            }
        }

        prefs.edit { putStringSet("alarms", alarmList) }
    }

    fun getSavedAlarms(context: Context): List<StoredAlarm> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val alarmList = prefs.getStringSet("alarms", null)?.toMutableSet() ?: return emptyList()

        val currentTime = System.currentTimeMillis()
        val validAlarms = mutableListOf<StoredAlarm>()
        val toRemove = mutableSetOf<String>()

        for (entry in alarmList) {
            val parts = entry.split("||")
            if (parts.size == 4) {
                val time = parts[0].toLongOrNull()
                val title = parts[1]
                val message = parts[2]
                val requestCode = parts[3].toIntOrNull()

                if (time != null && requestCode != null) {
                    if (time > currentTime) {
                        validAlarms.add(StoredAlarm(time, title, message, requestCode))
                    } else {
                        toRemove.add(entry)
                    }
                } else {
                    toRemove.add(entry)
                }
            } else {
                toRemove.add(entry)
            }
        }

        // Clean up expired entries
        if (toRemove.isNotEmpty()) {
            alarmList.removeAll(toRemove)
            prefs.edit().putStringSet("alarms", alarmList).apply()
        }

        return validAlarms
    }


}

