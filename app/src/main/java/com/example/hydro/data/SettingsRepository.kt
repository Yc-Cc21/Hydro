package com.example.hydro.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore by preferencesDataStore(name = "hydro_settings")

class SettingsRepository(
    private val context: Context
) {
    val dailyGoalMl: Flow<Int> = context.settingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[DAILY_GOAL_KEY] ?: DEFAULT_DAILY_GOAL_ML
        }

    suspend fun setDailyGoalMl(goalMl: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[DAILY_GOAL_KEY] = goalMl
        }
    }

    private companion object {
        val DAILY_GOAL_KEY: Preferences.Key<Int> = intPreferencesKey("daily_goal_ml")
        const val DEFAULT_DAILY_GOAL_ML = 2000
    }
}
