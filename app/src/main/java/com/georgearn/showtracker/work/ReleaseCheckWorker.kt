package com.georgearn.showtracker.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.georgearn.showtracker.data.local.WatchlistDao
import com.georgearn.showtracker.data.model.MediaType
import com.georgearn.showtracker.data.model.ReleaseStatus
import com.georgearn.showtracker.data.repository.MediaRepository
import com.georgearn.showtracker.notification.NotificationHelper
import com.georgearn.showtracker.util.DateUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background periodic check (chosen approach): for every saved title still waiting on a
 * release-notification, re-fetch its TMDB detail so date pushes are picked up, and fire
 * a local notification the moment its status flips UPCOMING -> RELEASED.
 */
@HiltWorker
class ReleaseCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: MediaRepository,
    private val watchlistDao: WatchlistDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val pending = watchlistDao.getPendingReleaseWatches()
            for (item in pending) {
                val mediaType = MediaType.from(item.mediaType)
                val detail = runCatching { repository.getDetail(item.tmdbId, mediaType) }.getOrNull() ?: continue

                if (detail.releaseDate != item.releaseDate) {
                    watchlistDao.upsert(item.copy(releaseDate = detail.releaseDate))
                }

                val wasUpcoming = DateUtils.releaseStatus(item.releaseDate) == ReleaseStatus.UPCOMING ||
                    item.lastKnownReleaseStatus == ReleaseStatus.UPCOMING.name
                val nowReleased = detail.releaseStatus == ReleaseStatus.RELEASED

                if (wasUpcoming && nowReleased) {
                    NotificationHelper.notifyReleased(applicationContext, item.tmdbId, item.mediaType, item.title)
                    watchlistDao.markNotified(item.tmdbId)
                }
                watchlistDao.updateStatus(item.tmdbId, detail.releaseStatus.name)
            }
            Result.success()
        } catch (t: Throwable) {
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "release_check_periodic"
    }
}
