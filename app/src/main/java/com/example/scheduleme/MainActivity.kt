package com.example.scheduleme

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.example.scheduleme.databinding.ActivityMainBinding
import java.util.Calendar
import java.util.Date
import java.util.UUID
import kotlin.math.absoluteValue

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
            val weeks = binding.repeatET.text.toString().toInt()
            if (weeks > 1) {
                for (i in 0..weeks - 1) {
                    scheduleNotification(i, title, message)
                    Thread.sleep(11)
                }
            } else {
                scheduleNotification(0, title, message)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun scheduleNotification(week : Int, title: String, message: String) {
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
        val time = getTime(week)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            time,
            pendingIntent
        )
        AlarmHelper.saveAlarm(applicationContext, time, title, message)
        showAlert(time, title, message)
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

    private fun getTime(weekNumber: Int): Long {
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
            add(Calendar.MINUTE, weekNumber)
        }

        return calendar.timeInMillis
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
}
