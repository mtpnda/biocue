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

    @Query("SELECT * FROM stress_events WHERE userId = :userId ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatestEvent(userId: String): StressEventEntity?

    @Query("SELECT * FROM stress_events WHERE userId = :userId AND isResolved = 0 AND endTime IS NOT NULL ORDER BY startTime DESC")
    suspend fun getUnresolvedEvents(userId: String): List<StressEventEntity>
    
    @Query("SELECT * FROM stress_events WHERE userId = :userId AND endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun getOngoingEvent(userId: String): StressEventEntity?

    @Query("SELECT * FROM stress_events WHERE userId = :userId AND startTime >= :startOfDay ORDER BY startTime DESC")
    suspend fun getEventsSince(userId: String, startOfDay: Long): List<StressEventEntity>

    @Query("SELECT * FROM stress_events WHERE userId = :userId AND startTime >= :start AND startTime < :end ORDER BY startTime DESC")
    suspend fun getEventsBetween(userId: String, start: Long, end: Long): List<StressEventEntity>
}

@Database(
    entities = [SensorWindowEntity::class, StressEventEntity::class],
    version = 2,
    exportSchema = false
)
abstract class DetectionDatabase : RoomDatabase() {
    abstract fun sensorDataDao(): SensorDataDao
    abstract fun eventDao(): EventDao
}

