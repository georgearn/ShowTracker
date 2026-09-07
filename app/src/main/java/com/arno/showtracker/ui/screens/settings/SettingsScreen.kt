package com.arno.showtracker.ui.screens.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arno.showtracker.data.local.Countries
import com.arno.showtracker.data.local.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit = {}, viewModel: SettingsViewModel = hiltViewModel()) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.dynamicColorEnabled.collectAsStateWithLifecycle()
    val region by viewModel.watchRegion.collectAsStateWithLifecycle()
    val blockedCountries by viewModel.blockedCountries.collectAsStateWithLifecycle()
    val pendingRefresh by viewModel.pendingRefresh.collectAsStateWithLifecycle()
    var countrySearch by remember { mutableStateOf("") }
    var expandedContinents by remember { mutableStateOf(setOf<String>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(padding).padding(horizontal = 16.dp)) {
            item {
                Text("Appearance", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
            }
            items(ThemeMode.entries) { mode ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(selected = themeMode == mode, onClick = { viewModel.setThemeMode(mode) })
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = themeMode == mode, onClick = { viewModel.setThemeMode(mode) })
                    Icon(
                        imageVector = when (mode) {
                            ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                            ThemeMode.LIGHT -> Icons.Default.LightMode
                            ThemeMode.DARK -> Icons.Default.DarkMode
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    Text(
                        when (mode) {
                            ThemeMode.SYSTEM -> "Follow system"
                            ThemeMode.LIGHT -> "Light"
                            ThemeMode.DARK -> "Dark"
                        },
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                        Text("Use wallpaper colors (Monet)")
                        Text(
                            "Matches accent colors to your wallpaper, Android 12+",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = dynamicColor, onCheckedChange = viewModel::setDynamicColor)
                }

                Text("Region", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp))
                Text(
                    "Used to pick which streaming services show under \"Where to watch\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = region,
                    onValueChange = { viewModel.setWatchRegion(it.uppercase().take(2)) },
                    label = { Text("ISO country code, e.g. US, GB, MD") },
                    singleLine = true,
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                )

                Text("Content Filters", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp))
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
                            .padding(top = 14.dp, bottom = 4.dp),
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
                androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 32.dp))
            }
        }
    }
}
