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
        binding.submitButton.setOnClickListener { scheduleNotification() }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun scheduleNotification() {
        val intent = Intent(applicationContext, NotificationReceiver::class.java)
        val title = binding.titleET.text.toString()
        val message = binding.messageET.text.toString() // ✅ Corrected here
        intent.putExtra(titleExtra, title)
        intent.putExtra(messageExtra, message)

        val requestCode = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            requestCode, // 👈 UNIQUE for each alarm
            intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )


        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val time = getTime()
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            time,
            pendingIntent
        )
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

    private fun getTime(): Long {
        val minute = binding.timePicker.minute
        val hour = binding.timePicker.hour
        val day = binding.datePicker.dayOfMonth
        val month = binding.datePicker.month
        val year = binding.datePicker.year

        val calendar = Calendar.getInstance()
        calendar.set(year, month, day, hour, minute)
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
