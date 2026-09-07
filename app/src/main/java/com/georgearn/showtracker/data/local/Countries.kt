package com.georgearn.showtracker.data.local

/**
 * Country of origin, for the content-filter blocklist. Code is the TMDB/ISO-3166-1 alpha-2.
 *
 * [language] (ISO 639-1) is the country's primary language - a best-effort single pick even for
 * multilingual countries. TMDB movie list results don't carry origin_country (only tv does), so
 * for movies we fall back to matching original_language. Since a language can be shared by several
 * countries here (English, Spanish, French, ...), blocking on it is only safe once every country
 * that shares it is also blocked - see [Countries.blockableLanguages].
 */
data class Country(val code: String, val displayName: String, val continent: String, val language: String)

object Countries {
    val ALL: List<Country> = listOf(
        Country("US", "United States", "North America", "en"),
        Country("CA", "Canada", "North America", "en"),
        Country("MX", "Mexico", "North America", "es"),
        Country("GB", "United Kingdom", "Europe", "en"),
        Country("FR", "France", "Europe", "fr"),
        Country("DE", "Germany", "Europe", "de"),
        Country("ES", "Spain", "Europe", "es"),
        Country("IT", "Italy", "Europe", "it"),
        Country("NL", "Netherlands", "Europe", "nl"),
        Country("SE", "Sweden", "Europe", "sv"),
        Country("NO", "Norway", "Europe", "no"),
        Country("DK", "Denmark", "Europe", "da"),
        Country("PL", "Poland", "Europe", "pl"),
        Country("IE", "Ireland", "Europe", "en"),
        Country("PT", "Portugal", "Europe", "pt"),
        Country("BE", "Belgium", "Europe", "nl"),
        Country("AT", "Austria", "Europe", "de"),
        Country("CH", "Switzerland", "Europe", "de"),
        Country("GR", "Greece", "Europe", "el"),
        Country("RO", "Romania", "Europe", "ro"),
        Country("TR", "Turkey", "Europe", "tr"),
        Country("RU", "Russia", "Europe", "ru"),
        Country("UA", "Ukraine", "Europe", "uk"),
        Country("CN", "China", "Asia", "zh"),
        Country("JP", "Japan", "Asia", "ja"),
        Country("KR", "South Korea", "Asia", "ko"),
        Country("IN", "India", "Asia", "hi"),
        Country("TH", "Thailand", "Asia", "th"),
        Country("PH", "Philippines", "Asia", "tl"),
        Country("ID", "Indonesia", "Asia", "id"),
        Country("HK", "Hong Kong", "Asia", "zh"),
        Country("TW", "Taiwan", "Asia", "zh"),
        Country("IL", "Israel", "Asia", "he"),
        Country("SA", "Saudi Arabia", "Asia", "ar"),
        Country("AE", "United Arab Emirates", "Asia", "ar"),
        Country("AU", "Australia", "Oceania", "en"),
        Country("NZ", "New Zealand", "Oceania", "en"),
        Country("BR", "Brazil", "South America", "pt"),
        Country("AR", "Argentina", "South America", "es"),
        Country("CO", "Colombia", "South America", "es"),
        Country("CL", "Chile", "South America", "es"),
        Country("ZA", "South Africa", "Africa", "en"),
        Country("EG", "Egypt", "Africa", "ar"),
        Country("NG", "Nigeria", "Africa", "en")
    ).sortedBy { it.displayName }

    val byContinent: Map<String, List<Country>> = ALL.groupBy { it.continent }

    private val byLanguage: Map<String, List<Country>> = ALL.groupBy { it.language }

    /**
     * Languages safe to use as a movie-filtering proxy: only once every country in this list that
     * speaks a language is blocked does blocking that language stop being a false-positive risk
     * for a country the user actually wants (e.g. blocking only Mexico must never silently also
     * hide Spain's Spanish-language movies).
     */
    fun blockableLanguages(blockedCodes: Set<String>): Set<String> =
        byLanguage.filterValues { countries -> countries.all { it.code in blockedCodes } }.keys
}
