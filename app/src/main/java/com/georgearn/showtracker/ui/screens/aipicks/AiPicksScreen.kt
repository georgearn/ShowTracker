package com.georgearn.showtracker.ui.screens.aipicks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.georgearn.showtracker.data.model.key
import com.georgearn.showtracker.data.repository.AiException
import com.georgearn.showtracker.data.repository.AiPick
import com.georgearn.showtracker.ui.screens.common.MediaRow
import com.georgearn.showtracker.ui.screens.common.PlaceholderList
import com.georgearn.showtracker.ui.screens.common.Pill
import com.georgearn.showtracker.ui.screens.common.RowToggle

private const val AI_STUDIO_KEY_URL = "https://aistudio.google.com/apikey"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiPicksScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDetail: (Int, String) -> Unit,
    viewModel: AiPicksViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val hasKey by viewModel.hasKey.collectAsStateWithLifecycle()
    val savedKeys by viewModel.savedKeys.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI Picks")
                        Text(
                            "Beyond your list, powered by Gemini",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (hasKey == true) {
                        IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Key, contentDescription = "Manage API key") }
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            when (hasKey) {
                null -> Unit
                false -> ConnectKeyCard(onSave = viewModel::saveKey)
                true -> LazyColumn(
                    modifier = Modifier.widthIn(max = 840.dp).fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { AskCard(state, viewModel) }

                    if (state.isLoading) {
                        item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                    }

                    state.error?.let { error -> item { AiError(error, onOpenSettings) } }

                    if (state.picks.isNotEmpty()) {
                        item {
                            Text(
                                "Suggested for you",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 8.dp).semantics { heading() }
                            )
                        }
                        items(state.picks, key = { it.summary.key }) { pick ->
                            PickRow(
                                pick = pick,
                                saved = pick.summary.key in savedKeys,
                                onOpen = { onOpenDetail(pick.summary.tmdbId, pick.summary.mediaType.apiValue) },
                                onToggle = { viewModel.toggleWatchlist(pick.summary) }
                            )
                        }
                        item {
                            Text(
                                "AI suggestions can be wrong. Check the details page before you commit an evening.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    } else if (state.isLoading) {
                        item { PlaceholderList(Modifier.height(360.dp), rows = 4) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectKeyCard(onSave: (String) -> Unit) {
    var key by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val keyboard = LocalSoftwareKeyboardController.current

    Column(
        Modifier.widthIn(max = 840.dp).fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Text("Connect Gemini", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                Text(
                    "AI Picks uses your own free Google AI key to suggest titles like the ones you love. " +
                        "Your key stays on this device, and only title names from your list are sent to Google.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it.trim() },
                    label = { Text("Gemini API key") },
                    singleLine = true,
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboard?.hide()
                        if (key.isNotBlank()) onSave(key)
                    }),
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(
                                if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (visible) "Hide key" else "Show key"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { onSave(key) },
                    enabled = key.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Save and continue") }
                TextButton(
                    onClick = { uriHandler.openUri(AI_STUDIO_KEY_URL) },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) { Text("Get a free key from Google AI Studio") }
            }
        }
    }
}

@Composable
private fun AskCard(state: AiPicksState, viewModel: AiPicksViewModel) {
    val keyboard = LocalSoftwareKeyboardController.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Find something like",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SeedMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = state.seedMode == mode,
                        onClick = { viewModel.setSeedMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = SeedMode.entries.size)
                    ) { Text(mode.label) }
                }
            }
            if (state.seedMode == SeedMode.TITLE) {
                OutlinedTextField(
                    value = state.seedTitle,
                    onValueChange = viewModel::setSeedTitle,
                    label = { Text("A title you loved") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    "Uses your most recently saved titles as the taste profile.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::setNote,
                label = { Text("Anything specific? (optional)") },
                placeholder = { Text("Slow-burn, no gore, under 2 hours...") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    keyboard?.hide()
                    viewModel.suggest()
                }),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    keyboard?.hide()
                    viewModel.suggest()
                },
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Text(
                    if (state.picks.isEmpty()) "Suggest titles" else "Suggest more",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun AiError(error: AiException, onOpenSettings: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(error.message.orEmpty(), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
            if (error.kind == AiException.Kind.BAD_KEY) {
                TextButton(onClick = onOpenSettings) { Text("Update key in Settings") }
            }
        }
    }
}

@Composable
private fun PickRow(pick: AiPick, saved: Boolean, onOpen: () -> Unit, onToggle: () -> Unit) {
    val item = pick.summary
    val year = item.releaseDate?.take(4)?.takeIf { it.length == 4 }
    MediaRow(
        posterPath = item.posterPath,
        title = item.title,
        onClick = onOpen,
        details = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(if (item.mediaType.apiValue == "tv") "Series" else "Movie")
                if (year != null) Pill(year)
            }
            if (pick.reason.isNotBlank()) {
                Text(
                    pick.reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }
        },
        trailing = {
            RowToggle(
                checked = saved,
                onToggle = onToggle,
                onIcon = Icons.Default.Check,
                offIcon = Icons.Default.Add,
                description = if (saved) "Remove ${item.title} from your list" else "Add ${item.title} to your list"
            )
        }
    )
}
