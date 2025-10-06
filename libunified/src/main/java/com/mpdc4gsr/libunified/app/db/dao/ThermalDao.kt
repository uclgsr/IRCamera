package com.mpdc4gsr.libunified.app.db.dao

import androidx.room.*
import com.mpdc4gsr.libunified.app.db.entity.ThermalEntity

@Dao
interface ThermalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ThermalEntity): Long

    @Query("SELECT type AS type, start_time AS startTime, count(*) AS duration FROM thermal GROUP BY start_time ORDER BY start_time DESC")
    suspend fun queryRecordList(): List<Record>

    @Query("SELECT * FROM thermal WHERE start_time = :startTime ORDER BY create_time")
    suspend fun queryDetail(startTime: Long): List<ThermalEntity>

    @Query("DELETE FROM thermal where start_time = :startTime")
    suspend fun delDetail(startTime: Long)

    @Query("delete from thermal where user_id = :userId")
    suspend fun deleteByUserId(userId: String)

    @Query(
        "delete from thermal where user_id = :userId and thermal=0 and thermal_max=0 and thermal_min=0 and create_time<(select max(create_time) from thermal where thermal=0 and thermal_max=0 and thermal_min=0)",
    )
    suspend fun deleteZero(userId: String)

    @Query("SELECT * FROM thermal WHERE user_id = :userId AND create_time >= :startTime AND create_time <= :endTime ORDER BY create_time")
    suspend fun getThermalByDate(
        userId: String,
        startTime: Long,
        endTime: Long,
    ): List<ThermalEntity>

    @Query("SELECT * FROM thermal WHERE user_id = :userId ORDER BY create_time")
    suspend fun getAllThermalByDate(userId: String): List<ThermalEntity>

    @Query("SELECT * FROM thermal WHERE user_id = :userId AND create_time >= :startTime AND create_time <= :endTime ORDER BY create_time")
    suspend fun queryByTime(
        userId: String,
        startTime: Long,
        endTime: Long,
    ): List<ThermalEntity>

    @Query(
        "SELECT * FROM thermal WHERE user_id = :userId AND create_time >= :startTime AND create_time <= :endTime AND type = :type ORDER BY create_time",
    )
    suspend fun queryByTime(
        userId: String,
        startTime: Long,
        endTime: Long,
        type: String,
    ): List<ThermalEntity>

    @Query(
        "SELECT COALESCE(MAX(thermal_max), 0.0) FROM thermal WHERE user_id = :userId AND create_time >= :startTime AND create_time <= :endTime",
    )
    suspend fun queryByTimeMax(
        userId: String,
        startTime: Long,
        endTime: Long,
    ): Float

    @Query(
        "SELECT COALESCE(MIN(thermal_min), 0.0) FROM thermal WHERE user_id = :userId AND create_time >= :startTime AND create_time <= :endTime",
    )
    suspend fun queryByTimeMin(
        userId: String,
        startTime: Long,
        endTime: Long,
    ): Float

    data class Record(
        var type: String? = "point",
        var startTime: Long = 0,
        var duration: Int = 0,
        @Ignore
        var showTitle: Boolean = false,
    )
}
