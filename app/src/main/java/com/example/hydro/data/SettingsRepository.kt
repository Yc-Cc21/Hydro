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

const val MIN_QUICK_RECORD_ML = 1
const val MAX_QUICK_RECORD_ML = 10000

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

    val quickRecordAmounts: Flow<List<Int>> = context.settingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            listOf(
                preferences[QUICK_RECORD_1_KEY]
                    ?.takeIf { it in MIN_QUICK_RECORD_ML..MAX_QUICK_RECORD_ML }
                    ?: DEFAULT_QUICK_RECORD_AMOUNTS[0],
                preferences[QUICK_RECORD_2_KEY]
                    ?.takeIf { it in MIN_QUICK_RECORD_ML..MAX_QUICK_RECORD_ML }
                    ?: DEFAULT_QUICK_RECORD_AMOUNTS[1],
                preferences[QUICK_RECORD_3_KEY]
                    ?.takeIf { it in MIN_QUICK_RECORD_ML..MAX_QUICK_RECORD_ML }
                    ?: DEFAULT_QUICK_RECORD_AMOUNTS[2]
            )
        }

    suspend fun setDailyGoalMl(goalMl: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[DAILY_GOAL_KEY] = goalMl
        }
    }

    suspend fun setQuickRecordAmounts(amounts: List<Int>) {
        if (amounts.size != QUICK_RECORD_COUNT ||
            amounts.any { it !in MIN_QUICK_RECORD_ML..MAX_QUICK_RECORD_ML }
        ) {
            return
        }
        context.settingsDataStore.edit { preferences ->
            preferences[QUICK_RECORD_1_KEY] = amounts[0]
            preferences[QUICK_RECORD_2_KEY] = amounts[1]
            preferences[QUICK_RECORD_3_KEY] = amounts[2]
        }
    }

    private companion object {
        val DAILY_GOAL_KEY: Preferences.Key<Int> = intPreferencesKey("daily_goal_ml")
        val QUICK_RECORD_1_KEY: Preferences.Key<Int> = intPreferencesKey("quick_record_1_ml")
        val QUICK_RECORD_2_KEY: Preferences.Key<Int> = intPreferencesKey("quick_record_2_ml")
        val QUICK_RECORD_3_KEY: Preferences.Key<Int> = intPreferencesKey("quick_record_3_ml")
        const val DEFAULT_DAILY_GOAL_ML = 2000
        val DEFAULT_QUICK_RECORD_AMOUNTS = listOf(100, 300, 400)
        const val QUICK_RECORD_COUNT = 3
    }
}
