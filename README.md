# Show Tracker

Native Android (Kotlin + Jetpack Compose) app for tracking movies/TV shows you care about:
search, handpick into your own list, get notified when an unreleased title drops, see
IMDb + Rotten Tomatoes scores, cast, where to stream/rent/buy, a synopsis, and a "suggest
something to watch" quiz + swipe flow over your saved list.

## Stack
- Kotlin, Jetpack Compose, Material 3 (dynamic color / Monet, dark+light theme)
- Hilt (DI), Room (local watchlist DB), Retrofit+Moshi (networking), Coil (images),
  WorkManager (background release-status polling -> local notifications), DataStore (settings)
- `minSdk 31` (Android 12), `compileSdk`/`targetSdk 36`

## Data sources
- **TMDB** (themoviedb.org) - search, discover/trending, synopsis, cast, posters, watch
  providers (JustWatch-backed "where to watch" data), region-aware.
- **OMDb** (omdbapi.com) - IMDb rating + Rotten Tomatoes score, looked up by the title's IMDb ID.

## Notifications
Background periodic check, not scheduled local alarms. A WorkManager job
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
  ui/nav            Bottom-nav + NavHost, onboarding gate
  ui/screens/...    One package per screen (ViewModel + Composable):
                      onboarding    first-launch genre + country picker
                      home          "Releasing Soon" rail + "Just Dropped" grid
                      discover      search + All/Movies/Series filter chips, grid results
                      upcoming      full "Releasing Soon" list, sectioned by time window
                      justdropped   full "Just Dropped" list, sectioned by type + genre filter
                      watchlist     active list (ready to watch / waiting on release) + watched history
                      foryou        mood/genre/length quiz -> swipe cards sourced from your watchlist
                      notifications inbox of titles you're set to be notified about, soonest first
                      details       poster, scores, cast, synopsis, where-to-watch, save/notify CTA
                      settings      theme, Monet toggle, watch region, content filters, lookahead
```
