package com.georgearn.showtracker.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.georgearn.showtracker.MainActivity
import com.georgearn.showtracker.R

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

    const val EXTRA_TMDB_ID = "extra_tmdb_id"
    const val EXTRA_MEDIA_TYPE = "extra_media_type"

    fun notifyReleased(context: Context, tmdbId: Int, mediaType: String, title: String) {
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TMDB_ID, tmdbId)
            putExtra(EXTRA_MEDIA_TYPE, mediaType)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            tmdbId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle("Now out: $title")
            .setContentText("$title just released. Tap to check it out.")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(tmdbId, notification)
        }
        // If POST_NOTIFICATIONS isn't granted (Android 13+), the SecurityException is swallowed
        // by runCatching; the caller (MainActivity) is responsible for requesting the permission.
    }
}
