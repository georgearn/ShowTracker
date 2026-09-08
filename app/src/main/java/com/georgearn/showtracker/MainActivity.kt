package com.georgearn.showtracker

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.georgearn.showtracker.notification.NotificationHelper
import com.georgearn.showtracker.ui.nav.DeepLinkTarget
import com.georgearn.showtracker.ui.nav.ShowTrackerNavHost
import com.georgearn.showtracker.ui.screens.settings.SettingsViewModel
import com.georgearn.showtracker.ui.theme.ShowTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()
    private var deepLinkState by mutableStateOf<DeepLinkTarget?>(null)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        deepLinkState = deepLinkFromIntent(intent)

        setContent {
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
            val dynamicColor by settingsViewModel.dynamicColorEnabled.collectAsStateWithLifecycle()

            ShowTrackerTheme(themeMode = themeMode, dynamicColorEnabled = dynamicColor) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ShowTrackerNavHost(
                        deepLinkTarget = deepLinkState,
                        onDeepLinkConsumed = { deepLinkState = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkFromIntent(intent)?.let { deepLinkState = it }
    }

    private fun deepLinkFromIntent(intent: Intent?): DeepLinkTarget? {
        val tmdbId = intent?.getIntExtra(NotificationHelper.EXTRA_TMDB_ID, -1) ?: -1
        val mediaType = intent?.getStringExtra(NotificationHelper.EXTRA_MEDIA_TYPE)
        return if (tmdbId != -1 && mediaType != null) DeepLinkTarget(tmdbId, mediaType) else null
    }
}
