package com.example.hydro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hydro.data.WaterRecord
import com.example.hydro.data.WaterRecordDao
import com.example.hydro.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class HomeViewModel(
    private val waterRecordDao: WaterRecordDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val today = todayRange()

    val todayWaterMl: StateFlow<Int> = waterRecordDao
        .observeForDate(today.start, today.end)
        .map { records -> records.sumOf { it.amountMl } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    val allWaterRecords: StateFlow<List<WaterRecord>> = waterRecordDao
        .observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val dailyGoalMl: StateFlow<Int> = settingsRepository.dailyGoalMl
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 2000
        )

    fun addWater(amountMl: Int) {
        viewModelScope.launch {
            waterRecordDao.insert(
                WaterRecord(
                    amountMl = amountMl,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun updateDailyGoal(goalMl: Int) {
        viewModelScope.launch {
            settingsRepository.setDailyGoalMl(goalMl)
        }
    }

    private data class TimeRange(
        val start: Long,
        val end: Long
    )

    private fun todayRange(): TimeRange {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val startOfDay = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val startOfNextDay = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return TimeRange(start = startOfDay, end = startOfNextDay)
    }

    class Factory(
        private val waterRecordDao: WaterRecordDao,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                return HomeViewModel(waterRecordDao, settingsRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
