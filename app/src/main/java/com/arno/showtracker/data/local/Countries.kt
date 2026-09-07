package com.arno.showtracker.data.local

/** Country of origin, for the content-filter blocklist. Code is the TMDB/ISO-3166-1 alpha-2. */
data class Country(val code: String, val displayName: String, val continent: String)

object Countries {
    val ALL: List<Country> = listOf(
        Country("US", "United States", "North America"),
        Country("CA", "Canada", "North America"),
        Country("MX", "Mexico", "North America"),
        Country("GB", "United Kingdom", "Europe"),
        Country("FR", "France", "Europe"),
        Country("DE", "Germany", "Europe"),
        Country("ES", "Spain", "Europe"),
        Country("IT", "Italy", "Europe"),
        Country("NL", "Netherlands", "Europe"),
        Country("SE", "Sweden", "Europe"),
        Country("NO", "Norway", "Europe"),
        Country("DK", "Denmark", "Europe"),
        Country("PL", "Poland", "Europe"),
        Country("IE", "Ireland", "Europe"),
        Country("PT", "Portugal", "Europe"),
        Country("BE", "Belgium", "Europe"),
        Country("AT", "Austria", "Europe"),
        Country("CH", "Switzerland", "Europe"),
        Country("GR", "Greece", "Europe"),
        Country("RO", "Romania", "Europe"),
        Country("TR", "Turkey", "Europe"),
        Country("RU", "Russia", "Europe"),
        Country("UA", "Ukraine", "Europe"),
        Country("CN", "China", "Asia"),
        Country("JP", "Japan", "Asia"),
        Country("KR", "South Korea", "Asia"),
        Country("IN", "India", "Asia"),
        Country("TH", "Thailand", "Asia"),
        Country("PH", "Philippines", "Asia"),
        Country("ID", "Indonesia", "Asia"),
        Country("HK", "Hong Kong", "Asia"),
        Country("TW", "Taiwan", "Asia"),
        Country("IL", "Israel", "Asia"),
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
}
