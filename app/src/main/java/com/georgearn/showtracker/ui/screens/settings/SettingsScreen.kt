package com.georgearn.showtracker.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.georgearn.showtracker.data.local.Countries
import com.georgearn.showtracker.data.local.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit = {}, viewModel: SettingsViewModel = hiltViewModel()) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.dynamicColorEnabled.collectAsStateWithLifecycle()
    val region by viewModel.watchRegion.collectAsStateWithLifecycle()
    val blockedCountries by viewModel.blockedCountries.collectAsStateWithLifecycle()
    val upcomingPages by viewModel.upcomingPagesPerType.collectAsStateWithLifecycle()
    val preferredGenreIds by viewModel.preferredGenreIds.collectAsStateWithLifecycle()
    val genreOptions by viewModel.genreOptions.collectAsStateWithLifecycle()
    val pendingRefresh by viewModel.pendingRefresh.collectAsStateWithLifecycle()
    var countrySearch by remember { mutableStateOf("") }
    var expandedContinents by remember { mutableStateOf(setOf<String>()) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Column(Modifier.padding(top = 12.dp)) {
                    SettingsSection(title = "Appearance") {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(
                                "Theme",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                ThemeMode.entries.forEachIndexed { index, mode ->
                                    SegmentedButton(
                                        selected = themeMode == mode,
                                        onClick = { viewModel.setThemeMode(mode) },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size),
                                        icon = {
                                            Icon(
                                                when (mode) {
                                                    ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                                    ThemeMode.LIGHT -> Icons.Default.LightMode
                                                    ThemeMode.DARK -> Icons.Default.DarkMode
                                                },
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    ) {
                                        Text(
                                            when (mode) {
                                                ThemeMode.SYSTEM -> "System"
                                                ThemeMode.LIGHT -> "Light"
                                                ThemeMode.DARK -> "Dark"
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider()
                        ListItem(
                            headlineContent = { Text("Wallpaper colors (Monet)") },
                            supportingContent = { Text("Matches accent colors to your wallpaper · Android 12+") },
                            leadingContent = {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            trailingContent = {
                                Switch(checked = dynamicColor, onCheckedChange = viewModel::setDynamicColor)
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }

                    SettingsSection(title = "Region", modifier = Modifier.padding(top = 20.dp)) {
                        ListItem(
                            headlineContent = { Text("Streaming region") },
                            supportingContent = { Text("Used to pick which services show under \"Where to watch\"") },
                            leadingContent = {
                                Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        OutlinedTextField(
                            value = region,
                            onValueChange = { viewModel.setWatchRegion(it.uppercase().take(2)) },
                            label = { Text("ISO country code, e.g. US, GB, MD") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 16.dp)
                        )
                    }

                    SettingsSection(title = "Preferred Genres", modifier = Modifier.padding(top = 20.dp)) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(
                                if (preferredGenreIds.isEmpty()) {
                                    "No genre filter set - Home and Just Dropped show everything. Pick genres to only show those."
                                } else {
                                    "Only titles matching a selected genre show on Home and Just Dropped."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (preferredGenreIds.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = viewModel::clearGenrePreference,
                                    modifier = Modifier.padding(top = 8.dp)
                                ) { Text("Clear genre filter") }
                            }
                            SettingsWrapChips(
                                modifier = Modifier.padding(top = 8.dp),
                                items = genreOptions,
                                isSelected = { preferredGenreIds.containsAll(it.ids) },
                                label = { it.label },
                                onClick = viewModel::toggleGenre
                            )
                        }
                    }

                    SettingsSection(title = "Releasing Soon lookahead", modifier = Modifier.padding(top = 20.dp)) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(
                                "How many pages of upcoming titles to fetch per type (movies/series), ~20 titles per page. " +
                                    "Higher values surface releases further in the future but take a bit longer to load.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "$upcomingPages page${if (upcomingPages == 1) "" else "s"} · ~${upcomingPages * 20} titles per type",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Slider(
                                value = upcomingPages.toFloat(),
                                onValueChange = { viewModel.setUpcomingPagesPerType(it.toInt()) },
                                valueRange = 1f..15f,
                                steps = 13,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    SettingsSection(title = "Content Filters", modifier = Modifier.padding(top = 20.dp)) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(
                                "Hide titles originating from selected countries, across Home, Discover and search. " +
                                    "Pick individual countries so you can still allow ones you like (e.g. South Korea).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (blockedCountries.isNotEmpty()) {
                                Text(
                                    "${blockedCountries.size} hidden",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            OutlinedTextField(
                                value = countrySearch,
                                onValueChange = { countrySearch = it },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                placeholder = { Text("Search countries") },
                                singleLine = true,
                                modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                            )
                            Button(
                                onClick = viewModel::refreshContent,
                                modifier = Modifier.padding(top = 12.dp).fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                Text(if (pendingRefresh) "Refresh content (new filters pending)" else "Refresh content")
                            }
                        }
                    }
                }
            }

            val filtered = Countries.ALL.filter { it.displayName.contains(countrySearch, ignoreCase = true) }
            val grouped = filtered.groupBy { it.continent }.toSortedMap()
            val searching = countrySearch.isNotBlank()
            grouped.forEach { (continent, countries) ->
                val blockedInContinent = countries.count { blockedCountries.contains(it.code) }
                val isExpanded = searching || expandedContinents.contains(continent)
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedContinents = if (expandedContinents.contains(continent)) {
                                    expandedContinents - continent
                                } else {
                                    expandedContinents + continent
                                }
                            }
                            .padding(top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            continent,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        if (blockedInContinent > 0) {
                            Text(
                                "$blockedInContinent hidden",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (isExpanded) {
                    items(countries, key = { it.code }) { country ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(country.displayName, modifier = Modifier.weight(1f))
                            Switch(
                                checked = blockedCountries.contains(country.code),
                                onCheckedChange = { viewModel.setCountryBlocked(country.code, it) }
                            )
                        }
                    }
                }
            }

            item {
                SettingsSection(title = "About", modifier = Modifier.padding(top = 20.dp, bottom = 32.dp)) {
                    ListItem(
                        headlineContent = { Text("Show Tracker") },
                        supportingContent = { Text("Version 1.0") },
                        leadingContent = {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> SettingsWrapChips(
    items: List<T>,
    isSelected: (T) -> Boolean,
    label: (T) -> String,
    onClick: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            FilterChip(selected = isSelected(item), onClick = { onClick(item) }, label = { Text(label(item)) })
        }
    }
}
