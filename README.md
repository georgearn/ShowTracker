# Show Tracker

Native Android (Kotlin + Jetpack Compose) app for tracking movies/TV shows you care about:
search, handpick into your own list, get notified when an unreleased title drops, see
IMDb + Rotten Tomatoes scores, where to stream/rent/buy, a synopsis, and a "suggest
something to watch" shuffle over your saved list.

## Stack
- Kotlin, Jetpack Compose, Material 3 (dynamic color / Monet, dark+light theme)
- Hilt (DI), Room (local watchlist DB), Retrofit+Moshi (networking), Coil (images),
  WorkManager (background release-status polling -> local notifications), DataStore (settings)
- `minSdk 31` (Android 12 - required floor for Monet dynamic color anyway), `compileSdk`/`targetSdk 36`
  (bump once Android 17's API level ships in your SDK manager - see comment in `app/build.gradle.kts`)

## Data sources
- **TMDB** (themoviedb.org) - search, discover/trending, synopsis, posters, watch providers
  (JustWatch-backed "where to watch" data), region-aware.
- **OMDb** (omdbapi.com) - IMDb rating + Rotten Tomatoes score, looked up by the title's IMDb ID
  (there is no free official IMDb or Rotten Tomatoes API; OMDb is the standard free proxy for both).

Your API keys are already wired into `local.properties` (gitignored, read into `BuildConfig` at
build time - never hardcoded in source):
```
OMDB_API_KEY=...
TMDB_READ_ACCESS_TOKEN=...
```
Get your own anytime at omdbapi.com/apikey.aspx (free tier) and
themoviedb.org/settings/api if these ever need rotating.

## Notifications
Chosen approach: **background periodic check**, not scheduled local alarms. A WorkManager job
(`ReleaseCheckWorker`) runs every 12h, re-fetches TMDB detail for every saved title still
waiting on release, and fires a notification the moment its status flips from upcoming to
released - so a delayed release date is picked up automatically instead of the app having
scheduled a notification for a date that then changed.

## Project layout
```
app/src/main/java/com/georgearn/showtracker/
  data/local        Room entity/DAO/DB, DataStore prefs
  data/remote       TMDB + OMDb Retrofit interfaces & DTOs
  data/repository   MediaRepository - merges TMDB + OMDb, owns watchlist CRUD + suggestion logic
  di                Hilt modules (network, database)
  notification      Notification channel + builder
  work              ReleaseCheckWorker (WorkManager + Hilt)
  ui/theme          Material3 theme incl. Monet dynamic color
  ui/nav            Bottom-nav + NavHost (Home / Discover / My List / For You, + Settings/Notifications/Details as pushed routes)
  ui/screens/...    One package per screen (ViewModel + Composable):
                      home          "Releasing Soon" rail + "Just Dropped" grid, bell -> Notifications, gear -> Settings
                      discover      search + All/Movies/Series filter chips, grid results
                      watchlist     ready to watch / waiting on release / already watched
                      foryou        mood+type quiz -> swipe cards (like/skip) sourced from your watchlist
                      notifications inbox of titles you're set to be notified about, soonest first
                      details       poster, scores, synopsis, where-to-watch chips, save/notify CTA
                      settings      theme, Monet toggle, watch region
```

## UI source
Rebuilt to match the second mockup you sent (`Show Tracker (standalone-src).html` in
`Entertainment tracker with recommendations.zip`) - that one shipped as real templated
markup + JS logic rather than a compiled blob, so the nav (Home/Discover/Watchlist/For You),
the swipe-based "For You" quiz, the notifications inbox screen, and the plain-chip
watch-provider list all follow it directly. Two adaptations from the mockup's demo data:
Discover's grid can't show IMDb/RT badges per-row (that'd mean one OMDb call per search
result) so it shows the TMDB score there instead, and "For You" draws its queue from your
real saved watchlist rather than a bundled demo catalog.

## Building
This archive ships without the Gradle wrapper jar binary (can't be generated in this
environment). Two ways to get running:
1. **Open in Android Studio** (Koala+ recommended) - it will offer to regenerate the wrapper
   and sync automatically.
2. Or, with a local Gradle install: `gradle wrapper` once inside the project root, then
   `./gradlew assembleDebug`.

Either way, set `sdk.dir` in `local.properties` to your Android SDK path first (Android
Studio does this for you automatically).

## About the mockups
The first mockup you attached (`Show Tracker standalone.html`) shipped as a compressed/bundled
single-file blob, not readable source - couldn't decode it. The second one
(`Show Tracker (standalone-src).html`) was real source, and the UI now follows it - see
"UI source" above.

## Not yet wired up (left as clear next steps)
- Bottom-of-list pagination for search/discover (currently first page only)
- Genre filter UI for the "suggest something" shuffle (the repository method already accepts one)
- Poster/backdrop caching for offline viewing of the watchlist
- Unit/instrumented tests
