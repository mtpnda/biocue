package com.mtpnda.biocue.core.data.local

import androidx.room.*

@Entity(tableName = "sensor_windows")
data class SensorWindowEntity(
    @PrimaryKey val timestamp: Long,
    val hrMean: Double,
    val rmssd: Double,
    val accMag: Double,
    val repetitiveness: Double,
    val hrSlope: Double,
    val isOffBody: Boolean,
    val isExercising: Boolean
)

@Dao
interface SensorDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(window: SensorWindowEntity)

    @Query("SELECT * FROM sensor_windows ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentWindows(limit: Int): List<SensorWindowEntity>
    
    // Purge data older than X days to manage storage limits if needed
    @Query("DELETE FROM sensor_windows WHERE timestamp < :thresholdTime")
    suspend fun deleteOlderThan(thresholdTime: Long)
}

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: StressEventEntity)

    @Update
    suspend fun updateEvent(event: StressEventEntity)

    @Query("SELECT * FROM stress_events WHERE id = :id LIMIT 1")
    suspend fun getEventById(id: String): StressEventEntity?

    @Query("SELECT * FROM stress_events ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatestEvent(): StressEventEntity?

    @Query("SELECT * FROM stress_events WHERE isResolved = 0 ORDER BY startTime DESC")
    suspend fun getUnresolvedEvents(): List<StressEventEntity>

    @Query("SELECT * FROM stress_events WHERE startTime >= :startOfDay ORDER BY startTime DESC")
    suspend fun getEventsSince(startOfDay: Long): List<StressEventEntity>
}

@Database(
    entities = [SensorWindowEntity::class, StressEventEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DetectionDatabase : RoomDatabase() {
    abstract fun sensorDataDao(): SensorDataDao
    abstract fun eventDao(): EventDao
}
