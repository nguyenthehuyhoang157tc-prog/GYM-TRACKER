package com.hoctin.gymtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hoctin.gymtracker.ui.GymTrackerApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as GymTrackerApplication
        setContent {
            GymTrackerApp(
                repository = app.repository,
                preferencesRepository = app.preferencesRepository,
                workoutDraftRepository = app.workoutDraftRepository,
            )
        }
    }
}
