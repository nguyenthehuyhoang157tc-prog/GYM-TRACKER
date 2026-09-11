package com.hoctin.gymtracker

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.hoctin.gymtracker.ui.GymTrackerApp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val app: GymTrackerApplication
        get() = application as GymTrackerApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        setContent {
            GymTrackerApp(
                repository = app.repository,
                savedWorkoutRepository = app.savedWorkoutRepository,
                preferencesRepository = app.preferencesRepository,
                workoutDraftRepository = app.workoutDraftRepository,
                catalogRepository = app.catalogRepository,
                recoveryResearchRepository = app.recoveryResearchRepository,
                wearableShadowRepository = app.wearableShadowRepository,
            )
        }
    }

    override fun onStart() {
        super.onStart()
        // Foreground-only, opt-in synchronization. The repository performs a status-only no-op
        // until Recovery Research is granted and Health Connect shadow sync is enabled.
        lifecycleScope.launch {
            runCatching { app.wearableShadowRepository.syncForeground() }
        }
    }
}
