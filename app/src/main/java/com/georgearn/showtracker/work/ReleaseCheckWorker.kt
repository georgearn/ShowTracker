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
                val detail = runCatching { repository.getDetail(item.tmdbId, mediaType, bypassCache = true) }.getOrNull() ?: continue

                if (detail.releaseDate != item.releaseDate) {
                    watchlistDao.upsert(item.copy(releaseDate = detail.releaseDate))
                }

                val wasUpcoming = DateUtils.releaseStatus(item.releaseDate) == ReleaseStatus.UPCOMING ||
                    item.lastKnownReleaseStatus == ReleaseStatus.UPCOMING.name
                val nowReleased = detail.releaseStatus == ReleaseStatus.RELEASED

                if (wasUpcoming && nowReleased) {
                    NotificationHelper.notifyReleased(applicationContext, item.tmdbId, item.mediaType, item.title)
                    watchlistDao.markNotified(item.tmdbId, item.mediaType)
                }
                watchlistDao.updateStatus(item.tmdbId, item.mediaType, detail.releaseStatus.name)
            }
            checkFollowedSeries()
            Result.success()
        } catch (t: Throwable) {
            Result.retry()
        }
    }

    /**
     * For each followed series: notify once when a new season gets a premiere date, and once when
     * it actually airs (the latest aired episode moves into that season). Fetches bypass the HTTP
     * cache, otherwise a date change could surface up to 3 days late.
     */
    private suspend fun checkFollowedSeries() {
        for (item in watchlistDao.getFollowedSeries()) {
            val detail = runCatching { repository.getDetail(item.tmdbId, MediaType.TV, bypassCache = true) }.getOrNull() ?: continue
            var updated = item

            val nextSeason = detail.nextSeasonNumber
            val nextAirDate = detail.nextSeasonAirDate
            if (nextSeason != null) {
                updated = updated.copy(nextSeasonNumber = nextSeason, nextSeasonAirDate = nextAirDate)
                val alreadyAnnounced = (updated.lastAnnouncedSeason ?: 0) >= nextSeason
                val alreadyAired = (updated.lastReleasedSeason ?: 0) >= nextSeason
                val isFuture = DateUtils.releaseStatus(nextAirDate) == ReleaseStatus.UPCOMING
                if (nextAirDate != null && isFuture && !alreadyAnnounced && !alreadyAired) {
                    NotificationHelper.notifySeasonAnnounced(applicationContext, item.tmdbId, item.title, nextSeason, nextAirDate)
                    updated = updated.copy(lastAnnouncedSeason = nextSeason)
                }
            }

            val aired = detail.latestAiredSeason
            // Season 0 on TMDB is "Specials" - not a new season.
            if (aired != null && aired > 0 && aired > (updated.lastReleasedSeason ?: 0)) {
                NotificationHelper.notifySeasonOut(applicationContext, item.tmdbId, item.title, aired)
                updated = updated.copy(
                    lastReleasedSeason = aired,
                    lastAnnouncedSeason = maxOf(updated.lastAnnouncedSeason ?: 0, aired)
                )
            }

            if (updated != item) watchlistDao.update(updated)
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "release_check_periodic"
    }
}
