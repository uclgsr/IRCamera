package com.mpdc4gsr.libunified.app.db.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mpdc4gsr.libunified.app.db.entity.ThermalHourEntity
@Dao
interface ThermalHourDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ThermalHourEntity): Long
    @Query(
        "SELECT * FROM thermal_hour WHERE user_id = :userId AND create_time >= :startTime AND create_time <= :endTime ORDER BY create_time",
    )
    suspend fun queryByTime(
        userId: String,
        startTime: Long,
        endTime: Long,
    ): List<ThermalHourEntity>
    @Query(
        "SELECT * FROM thermal_hour WHERE user_id = :userId AND create_time >= :startTime AND create_time <= :endTime AND type = :type ORDER BY create_time",
    )
    suspend fun queryByTime(
        userId: String,
        startTime: Long,
        endTime: Long,
        type: String,
    ): List<ThermalHourEntity>
    @Query(
        "SELECT COALESCE(MAX(thermal_max), 0.0) FROM thermal_hour WHERE user_id = :userId AND create_time >= :startTime AND create_time <= :endTime",
    )
    suspend fun queryByTimeMax(
        userId: String,
        startTime: Long,
        endTime: Long,
    ): Float
    @Query(
        "SELECT COALESCE(MIN(thermal_min), 0.0) FROM thermal_hour WHERE user_id = :userId AND create_time >= :startTime AND create_time <= :endTime",
    )
    suspend fun queryByTimeMin(
        userId: String,
        startTime: Long,
        endTime: Long,
    ): Float
    @Query("SELECT COALESCE(MAX(create_time), 0) FROM thermal_hour WHERE user_id = :userId")
    suspend fun queryMaxTime(userId: String): Long
    @Query(
        "DELETE FROM thermal_hour WHERE user_id = :userId AND id NOT IN (SELECT MAX(id) FROM thermal_hour WHERE user_id = :userId GROUP BY create_time)",
    )
    suspend fun deleteRepeatVol(userId: String)
    @Query("DELETE FROM thermal_hour WHERE user_id = :userId")
    suspend fun deleteByUserId(userId: String)
}
