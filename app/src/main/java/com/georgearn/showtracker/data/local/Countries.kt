package com.georgearn.showtracker.data.local

/**
 * Country of origin, for the content-filter blocklist. Code is the TMDB/ISO-3166-1 alpha-2.
 *
 * [language] (ISO 639-1) is set only when it's a reasonably unique proxy for that single country
 * within this list - TMDB movie list results don't carry origin_country (only tv does), so for
 * movies we fall back to matching original_language against blocked countries' languages. Left
 * null for countries whose language is shared with others here (English, Spanish, French, German,
 * Portuguese, Arabic) since blocking on it would wrongly catch the other countries too.
 */
data class Country(val code: String, val displayName: String, val continent: String, val language: String? = null)

object Countries {
    val ALL: List<Country> = listOf(
        Country("US", "United States", "North America"),
        Country("CA", "Canada", "North America"),
        Country("MX", "Mexico", "North America"),
        Country("GB", "United Kingdom", "Europe"),
        Country("FR", "France", "Europe"),
        Country("DE", "Germany", "Europe"),
        Country("ES", "Spain", "Europe"),
        Country("IT", "Italy", "Europe", "it"),
        Country("NL", "Netherlands", "Europe", "nl"),
        Country("SE", "Sweden", "Europe", "sv"),
        Country("NO", "Norway", "Europe", "no"),
        Country("DK", "Denmark", "Europe", "da"),
        Country("PL", "Poland", "Europe", "pl"),
        Country("IE", "Ireland", "Europe"),
        Country("PT", "Portugal", "Europe"),
        Country("BE", "Belgium", "Europe"),
        Country("AT", "Austria", "Europe"),
        Country("CH", "Switzerland", "Europe"),
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
        Country("HK", "Hong Kong", "Asia"),
        Country("TW", "Taiwan", "Asia"),
        Country("IL", "Israel", "Asia", "he"),
        Country("SA", "Saudi Arabia", "Asia"),
        Country("AE", "United Arab Emirates", "Asia"),
        Country("AU", "Australia", "Oceania"),
        Country("NZ", "New Zealand", "Oceania"),
        Country("BR", "Brazil", "South America"),
        Country("AR", "Argentina", "South America"),
        Country("CO", "Colombia", "South America"),
        Country("CL", "Chile", "South America"),
        Country("ZA", "South Africa", "Africa"),
        Country("EG", "Egypt", "Africa"),
        Country("NG", "Nigeria", "Africa")
    ).sortedBy { it.displayName }

    val byContinent: Map<String, List<Country>> = ALL.groupBy { it.continent }

    fun languagesFor(codes: Set<String>): Set<String> =
        ALL.filter { it.code in codes }.mapNotNull { it.language }.toSet()
}
