package com.example.hydro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hydro.data.WaterRecord
import com.example.hydro.data.WaterRecordDao
import com.example.hydro.data.SettingsRepository
import com.example.hydro.data.MAX_QUICK_RECORD_ML
import com.example.hydro.data.MIN_QUICK_RECORD_ML
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class HomeViewModel(
    private val waterRecordDao: WaterRecordDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val dateCheckRequests = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val todayRecords: StateFlow<List<WaterRecord>> = merge(flowOf(Unit), dateCheckRequests)
        .flatMapLatest { currentDayRangeFlow() }
        .flatMapLatest { range -> waterRecordDao.observeForDate(range.start, range.end) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val todayWaterMl: StateFlow<Int> = todayRecords
        .map { records ->
            records
                .fold(0L) { total, record -> total + record.amountMl.toLong() }
                .coerceIn(0L, Int.MAX_VALUE.toLong())
                .toInt()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    val canUndoWater: StateFlow<Boolean> = todayRecords
        .map { records -> records.isNotEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
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

    val quickRecordAmounts: StateFlow<List<Int>> = settingsRepository.quickRecordAmounts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = listOf(100, 300, 400)
        )

    fun addWater(amountMl: Int) {
        if (amountMl !in MIN_QUICK_RECORD_ML..MAX_QUICK_RECORD_ML) return
        viewModelScope.launch {
            waterRecordDao.insert(
                WaterRecord(
                    amountMl = amountMl,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun undoLastWater() {
        viewModelScope.launch {
            val range = todayRange()
            val lastRecord = waterRecordDao
                .getForDate(range.start, range.end)
                .firstOrNull() ?: return@launch
            waterRecordDao.deleteById(lastRecord.id)
        }
    }

    fun refreshTodayIfDateChanged() {
        dateCheckRequests.tryEmit(Unit)
    }

    fun updateDailyGoal(goalMl: Int) {
        viewModelScope.launch {
            settingsRepository.setDailyGoalMl(goalMl)
        }
    }

    fun updateQuickRecordAmounts(amounts: List<Int>) {
        viewModelScope.launch {
            settingsRepository.setQuickRecordAmounts(amounts)
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

    private fun currentDayRangeFlow(): Flow<TimeRange> = flow {
        while (true) {
            val range = todayRange()
            emit(range)
            val millisUntilTomorrow =
                range.end - System.currentTimeMillis() + ROLLOVER_GRACE_MILLIS
            delay(millisUntilTomorrow.coerceAtLeast(MIN_RECHECK_INTERVAL_MILLIS))
        }
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

    private companion object {
        const val ROLLOVER_GRACE_MILLIS = 1_000L
        const val MIN_RECHECK_INTERVAL_MILLIS = 1_000L
    }
}
