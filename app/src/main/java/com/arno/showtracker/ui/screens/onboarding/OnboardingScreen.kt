package com.arno.showtracker.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arno.showtracker.data.local.Countries

@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 20.dp)) {
            item {
                Column(Modifier.padding(top = 32.dp, bottom = 8.dp)) {
                    Text("Welcome to ShowTracker", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "A couple quick picks so your feed starts off relevant. You can change these anytime in Settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            item {
                Text(
                    "What do you like watching?",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 20.dp, bottom = 4.dp)
                )
                Text(
                    "Pick a few genres, or skip - you can filter by genre later too.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
            item {
                FlowChips(
                    items = state.genreOptions,
                    isSelected = { state.selectedGenreIds.containsAll(it.ids) },
                    label = { it.label },
                    onClick = viewModel::toggleGenre
                )
            }

            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Where should releases come from?", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Untick regions you'd rather not see. Toggle any back on later in Settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(Modifier.padding(top = 8.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { viewModel.setAllCountries(true) }) { Text("Select all") }
                    TextButton(onClick = { viewModel.setAllCountries(false) }) { Text("Clear all") }
                }
            }

            Countries.byContinent.toSortedMap().forEach { (continent, countries) ->
                item {
                    Text(
                        continent,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                items(countries, key = { it.code }) { country ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(country.displayName, modifier = Modifier.weight(1f))
                        Switch(
                            checked = state.selectedCountryCodes.contains(country.code),
                            onCheckedChange = { viewModel.toggleCountry(country.code) }
                        )
                    }
                }
            }

            item {
                Box(Modifier.padding(bottom = 24.dp))
            }
        }

        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Button(onClick = { viewModel.finish(onDone) }, modifier = Modifier.fillMaxWidth()) {
                Text("Get Started")
            }
            TextButton(onClick = { viewModel.finish(onDone) }, modifier = Modifier.fillMaxWidth()) {
                Text("Skip for now", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun <T> FlowChips(
    items: List<T>,
    isSelected: (T) -> Boolean,
    label: (T) -> String,
    onClick: (T) -> Unit
) {
    // Simple wrap: chunk into rows since FlowRow isn't in this project's Compose foundation version yet.
    val rows = mutableListOf<MutableList<T>>()
    var currentLen = 0
    var currentRow = mutableListOf<T>()
    items.forEach { item ->
        val len = label(item).length
        if (currentLen + len > 28 && currentRow.isNotEmpty()) {
            rows.add(currentRow)
            currentRow = mutableListOf()
            currentLen = 0
        }
        currentRow.add(item)
        currentLen += len
    }
    if (currentRow.isNotEmpty()) rows.add(currentRow)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { item ->
                    FilterChip(selected = isSelected(item), onClick = { onClick(item) }, label = { Text(label(item)) })
                }
            }
        }
    }
}
