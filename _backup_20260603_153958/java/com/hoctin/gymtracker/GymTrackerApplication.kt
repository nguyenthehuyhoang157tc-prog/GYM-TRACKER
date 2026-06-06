package com.hoctin.gymtracker

import android.app.Application
import com.hoctin.gymtracker.data.AppDatabase
import com.hoctin.gymtracker.data.GymRepository
import com.hoctin.gymtracker.data.UserPreferencesRepository

class GymTrackerApplication : Application() {
    lateinit var repository: GymRepository
        private set
    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.create(this)
        repository = GymRepository(database.gymDao())
        preferencesRepository = UserPreferencesRepository(this)
    }
}
