package com.example.scheduleme

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.example.scheduleme.databinding.ActivityMainBinding
import java.util.Calendar
import java.util.Date
import java.util.UUID
import kotlin.math.absoluteValue
import androidx.core.net.toUri

private lateinit var binding: ActivityMainBinding

class MainActivity : ComponentActivity() {

    companion object {
        const val channelID = "scheduleme_channel"
        const val titleExtra = "titleExtra"
        const val messageExtra = "messageExtra"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createNotificationChannel()
        binding.submitButton.setOnClickListener {
            val title = binding.titleET.text.toString()
            val message = binding.messageET.text.toString()
            val weeks = binding.repeatET.text.toString().toIntOrNull() ?: 1

            scheduleHelper(title, message, weeks)

            if (title.contains("class", ignoreCase = true)) {
                medHelper("Medication time", "For: $title", weeks)
            }
        }

        binding.cancelButton.setOnClickListener {
            showCancelAlarmSelector()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun scheduleHelper(title: String, message: String, weeks: Int) {

        if (weeks > 1) {
            for (i in 0..weeks - 1) {
                val time = getTime(i).timeInMillis
                scheduleNotification(title, message, time)
            }
        } else {
            val time = getTime(0).timeInMillis
            scheduleNotification(title, message, time)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun medHelper(title: String, message: String, weeks: Int) {

        if (weeks > 1) {
            for (i in 0..weeks - 1) {
                val time = getMedTime(i).timeInMillis
                scheduleNotification(title, message, time)
            }
        } else {
            val time = getMedTime(0).timeInMillis
            scheduleNotification(title, message, time)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun scheduleNotification(title: String, message: String, time: Long) {
        val intent = Intent(applicationContext, NotificationReceiver::class.java)
        intent.putExtra(titleExtra, title)
        intent.putExtra(messageExtra, message)

        val requestCode = UUID.randomUUID().hashCode().absoluteValue
        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            requestCode, // 👈 UNIQUE for each alarm
            intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )


        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
            } else {
                // Optional: Prompt user to allow exact alarms in settings
                showExactAlarmPermissionDialog()
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
        }

        AlarmHelper.saveAlarm(applicationContext, time, title, message, requestCode)
        showAlert(time, title, message)
    }

    private fun getTime(weekNumber: Int): Calendar {
        val minute = binding.timePicker.minute
        val hour = binding.timePicker.hour
        val day = binding.datePicker.dayOfMonth
        val month = binding.datePicker.month
        val year = binding.datePicker.year

        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // ✅ Safely add weeks without worrying about month/year rollovers
            add(Calendar.WEEK_OF_YEAR, weekNumber)
        }

        return calendar
    }

    private fun getMedTime(weekNumber: Int): Calendar {
        val minute = binding.timePicker.minute
        val hour = binding.timePicker.hour
        val day = binding.datePicker.dayOfMonth
        val month = binding.datePicker.month
        val year = binding.datePicker.year

        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // ✅ Safely add weeks without worrying about month/year rollovers
            add(Calendar.WEEK_OF_YEAR, weekNumber)
            add(Calendar.HOUR_OF_DAY, -4)
        }

        return calendar
    }

    private fun showAlert(time: Long, title: String, message: String) {
        val date = Date(time)
        val dateFormat = android.text.format.DateFormat.getLongDateFormat(applicationContext)
        val timeFormat = android.text.format.DateFormat.getTimeFormat(applicationContext)

        AlertDialog.Builder(this)
            .setTitle("Notification scheduled")
            .setMessage("Title: $title\nMessage: $message\nAt: ${dateFormat.format(date)} ${timeFormat.format(date)}")
            .setPositiveButton("Okay") { _, _ -> }
            .show()
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val name = "Notification Channel"
        val descriptionText = "Channel for scheduled notifications"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(channelID, name, importance).apply {
            description = descriptionText
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun showExactAlarmPermissionDialog() {
        val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = "package:$packageName".toUri()
        }
        startActivity(intent)
    }

    private fun showCancelAlarmSelector() {
        val alarms = AlarmHelper.getSavedAlarms(this)
        if (alarms.isEmpty()) {
            Toast.makeText(this, "No alarms to cancel", Toast.LENGTH_SHORT).show()
            return
        }

        val alarmLabels = alarms.map {
            val date = Date(it.time)
            val dateStr = android.text.format.DateFormat.format("yyyy-MM-dd HH:mm", date)
            "${it.title} @ $dateStr"
        }.toTypedArray()

        val checkedItems = BooleanArray(alarms.size)

        AlertDialog.Builder(this)
            .setTitle("Select Alarms to Cancel")
            .setMultiChoiceItems(alarmLabels, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("Cancel Selected") { _, _ ->
                val toCancel = alarms.filterIndexed { index, _ -> checkedItems[index] }
                toCancel.forEach {
                    AlarmHelper.cancelAlarm(this, it.time, it.title, it.message)
                }
                Toast.makeText(this, "Canceled ${toCancel.size} alarms", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Dismiss", null)
            .show()
    }


}
