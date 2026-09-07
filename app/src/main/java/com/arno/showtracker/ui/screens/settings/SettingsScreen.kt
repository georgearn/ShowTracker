package com.arno.showtracker.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arno.showtracker.data.local.OriginCountry
import com.arno.showtracker.data.local.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit = {}, viewModel: SettingsViewModel = hiltViewModel()) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.dynamicColorEnabled.collectAsStateWithLifecycle()
    val region by viewModel.watchRegion.collectAsStateWithLifecycle()
    val blockedCountries by viewModel.blockedCountries.collectAsStateWithLifecycle()

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
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        Text("Appearance", style = MaterialTheme.typography.titleMedium)

        ThemeMode.entries.forEach { mode ->
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

        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
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
            "Hide series/movies originating from these countries across Home, Discover and search",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OriginCountry.entries.forEach { country ->
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
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
}
