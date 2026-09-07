package com.arno.showtracker.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.arno.showtracker.R

object NotificationHelper {
    const val CHANNEL_ID = "release_alerts"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Release alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Lets you know when a saved title you're waiting on is released."
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun notifyReleased(context: Context, tmdbId: Int, title: String) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Now out: $title")
            .setContentText("$title just released. Tap to check it out.")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(tmdbId, notification)
        }
        // If POST_NOTIFICATIONS isn't granted (Android 13+), the SecurityException is swallowed
        // by runCatching; the caller (MainActivity) is responsible for requesting the permission.
    }
}
