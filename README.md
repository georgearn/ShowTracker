# Show Tracker

Native Android (Kotlin + Jetpack Compose) application for tracking movies and TV shows you care about: search, handpick into your watchlist, receive notifications when unreleased titles drop, view IMDb & Rotten Tomatoes scores, explore cast and streaming providers ("where to watch"), and get personalized recommendations via an interactive quiz.

---

## Technical Stack & Architecture

- **Build & Tooling**: Gradle 9.7.1, Android Gradle Plugin (AGP) 9.4.1, Gradle Configuration Cache enabled.
- **Language & Runtime**: Kotlin 2.1.10, Compose Compiler Plugin 2.1.10, Java 17 / JVM 25 compatible.
- **UI & Design**: Jetpack Compose, Material 3 Design System (Monet dynamic colors, adaptive color roles, full M3 typography scale, dark & light themes).
- **Target SDK**: `compileSdk 37` / `targetSdk 37` (Android 17), `minSdk 31` (Android 12+).
- **Architecture**: `arm64-v8a` target architecture, MVVM with Clean Architecture principles.
- **Libraries**:
  - **Hilt**: Dependency Injection (DI)
  - **Room**: Local watchlist & notification history persistence
  - **Retrofit 2 + Moshi**: Network layer & JSON serialization with KSP code generation
  - **OkHttp 4**: 50 MB HTTP disk cache + 3-day `Cache-Control` network & offline interceptors
  - **Coil**: Image loading & async poster rendering
  - **WorkManager**: Periodic release-status polling -> local notifications
  - **DataStore**: Preferences & user settings

---

## Data Sources

- **TMDB (themoviedb.org)**: Multi-search, trending, discover, synopsis, cast, posters, and region-aware JustWatch watch providers.
- **OMDb (omdbapi.com)**: IMDb rating and Rotten Tomatoes score lookups by IMDb ID.

---

## Caching Strategy (3-Day Retention)

To minimize network usage and provide instant loading:
- **HTTP Disk Cache**: 50 MB OkHttp cache with network interceptors enforcing a 3-day (`max-age=259200`) freshness window and offline stale fallback.
- **In-Memory Cache**: 3-day in-memory caching layer in `MediaRepository` for `recentlyReleased()` and `upcoming()` queries.

---

## Features & Screen Guide

- **Home Screen**: Defaulted to "New Releases" (with "Upcoming" timeline tab). State preserved via `rememberSaveable`.
- **Discover Screen**: Material 3 search bar with multi-category filters (All / Movies / Series) and instant watchlist quick-add.
- **For You Screen**: Rebuilt intro screen offering:
  - **Take Quick Quiz**: 1-question-per-screen wizard (Mood -> Media Type -> Duration -> Genres) with step counter and progress indicator.
  - **Drop Random Pick / Surprise Me**: Instant watchlist recommendation card.
- **Watchlist Screen**: Active list (ready to watch / waiting on release) and watched history with organization filters.
- **Details Screen**: Backdrop hero, poster, IMDb / Rotten Tomatoes / TMDB scores, cast, synopsis, region-aware "Where to Watch", and save/notify actions.
- **Settings Screen**: Theme selection, Monet dynamic wallpaper colors, streaming region code, content filters, and lookahead parameters.

---

## Background Notifications

A WorkManager job (`ReleaseCheckWorker`) runs periodically every 12 hours:
1. Re-fetches TMDB details for saved upcoming titles.
2. Automatically updates delayed or shifted release dates.
3. Fires a local notification the moment a title status flips from `UPCOMING` to `RELEASED`.

---

## Project Structure

```
app/src/main/java/com/georgearn/showtracker/
  data/local        Room entity/DAO/DB, DataStore prefs, ContentRefreshBus
  data/remote       TMDB + OMDb Retrofit interfaces & DTOs
  data/repository   MediaRepository - data caching, watchlist CRUD, recommendation engine
  di                Hilt modules (network with 3-day HTTP cache, database)
  notification      Notification channel & local alert builders
  work              ReleaseCheckWorker (WorkManager + Hilt)
  ui/theme          Material 3 theme, typography scale, Monet dynamic colors
  ui/nav            Bottom-nav bar (selected/unselected M3 icons), NavHost
  ui/screens/...    Screen implementations (Composable + ViewModel):
                      home          "New Releases" default feed & "Upcoming" timeline
                      discover      Search bar & grid results
                      watchlist     Saved watchlist & history
                      foryou        Intro screen & 1-question-per-screen recommendation wizard
                      details       Movie/Show detail, scores, cast, watch providers
                      settings      Theme, region, content filters, lookahead
                      onboarding    First-launch genre & country selection
                      notifications Notification inbox
                      upcoming      Expanded upcoming releases
                      justdropped   Expanded recent drops
```

---
