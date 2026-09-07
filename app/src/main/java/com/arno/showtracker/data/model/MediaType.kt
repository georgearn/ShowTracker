package com.arno.showtracker.data.model

enum class MediaType(val apiValue: String) {
    MOVIE("movie"),
    TV("tv");

    companion object {
        fun from(value: String) = if (value == "tv") TV else MOVIE
    }
}

enum class ReleaseStatus {
    RELEASED,
    UPCOMING,
    UNKNOWN
}
