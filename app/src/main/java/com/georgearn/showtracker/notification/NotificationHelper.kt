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
import com.georgearn.showtracker.util.DateUtils

object NotificationHelper {
    const val CHANNEL_ID = "release_alerts"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Release alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Lets you know when a saved title is released or a followed series gets a new season."
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    const val EXTRA_TMDB_ID = "extra_tmdb_id"
    const val EXTRA_MEDIA_TYPE = "extra_media_type"

    fun notifyReleased(context: Context, tmdbId: Int, mediaType: String, title: String) {
        post(
            context, tmdbId, mediaType,
            notificationId = "release:$mediaType:$tmdbId".hashCode(),
            title = "Now out: $title",
            text = "$title just released. Tap to check it out."
        )
    }

    fun notifySeasonAnnounced(context: Context, tmdbId: Int, title: String, season: Int, airDate: String) {
        post(
            context, tmdbId, "tv",
            notificationId = "season:$tmdbId:$season".hashCode(),
            title = "$title: season $season is dated",
            text = "Season $season premieres ${DateUtils.formatForDisplay(airDate)}."
        )
    }

    fun notifySeasonOut(context: Context, tmdbId: Int, title: String, season: Int) {
        post(
            context, tmdbId, "tv",
            // Same id as the announcement: the "is out" notification replaces it.
            notificationId = "season:$tmdbId:$season".hashCode(),
            title = "New season: $title",
            text = "Season $season of $title is out. Tap to check it out."
        )
    }

    private fun post(context: Context, tmdbId: Int, mediaType: String, notificationId: Int, title: String, text: String) {
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TMDB_ID, tmdbId)
            putExtra(EXTRA_MEDIA_TYPE, mediaType)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .build()

        // Without POST_NOTIFICATIONS (Android 13+) this throws; the alert still shows in-app.
        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }
}
