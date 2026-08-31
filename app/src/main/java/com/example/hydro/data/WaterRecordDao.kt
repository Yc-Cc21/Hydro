package com.example.hydro.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterRecordDao {
    @Insert
    suspend fun insert(record: WaterRecord)

    @Query("SELECT * FROM water_records ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<WaterRecord>>

    @Query(
        """
        SELECT * FROM water_records
        WHERE timestamp >= :startOfDay AND timestamp < :startOfNextDay
        ORDER BY timestamp DESC
        """
    )
    fun observeForDate(startOfDay: Long, startOfNextDay: Long): Flow<List<WaterRecord>>

    @Query(
        """
        SELECT * FROM water_records
        WHERE timestamp >= :startOfDay AND timestamp < :startOfNextDay
        ORDER BY timestamp DESC
        """
    )
    suspend fun getForDate(startOfDay: Long, startOfNextDay: Long): List<WaterRecord>
}
