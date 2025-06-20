package com.example.scheduleme

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.scheduleme.MainActivity.Companion.channelID
import com.example.scheduleme.MainActivity.Companion.messageExtra
import com.example.scheduleme.MainActivity.Companion.titleExtra

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(titleExtra) ?: "No Title"
        val message = intent.getStringExtra(messageExtra) ?: "No Message"

        val builder = NotificationCompat.Builder(context, channelID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // You can change this icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val notificationManager = ContextCompat.getSystemService(
            context, NotificationManager::class.java
        )
        notificationManager?.notify(notificationId, builder.build())
    }
}
