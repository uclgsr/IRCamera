package com.topdon.lib.core.db.dao

import androidx.room.*
import com.topdon.lib.core.db.entity.ThermalMinuteEntity

@Dao
interface ThermalMinuteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: ThermalMinuteEntity): Long

    @Query("SELECT * FROM thermal_minute WHERE user_id = :userId AND create_time >= :startTime AND create_time <= :endTime ORDER BY create_time")
    fun queryByTime(userId: String, startTime: Long, endTime: Long): List<ThermalMinuteEntity>

    @Query("SELECT * FROM thermal_minute WHERE user_id = :userId AND create_time >= :startTime AND create_time <= :endTime AND type = :type ORDER BY create_time")
    fun queryByTime(userId: String, startTime: Long, endTime: Long, type: String): List<ThermalMinuteEntity>

    @Query("SELECT COALESCE(MAX(thermal_max), 0.0) FROM thermal_minute WHERE user_id = :userId AND create_time >= :startTime AND create_time <= :endTime")
    fun queryByTimeMax(userId: String, startTime: Long, endTime: Long): Float

    @Query("SELECT COALESCE(MIN(thermal_min), 0.0) FROM thermal_minute WHERE user_id = :userId AND create_time >= :startTime AND create_time <= :endTime")
    fun queryByTimeMin(userId: String, startTime: Long, endTime: Long): Float

    @Query("SELECT COALESCE(MAX(create_time), 0) FROM thermal_minute WHERE user_id = :userId")
    fun queryMaxTime(userId: String): Long

    @Query("DELETE FROM thermal_minute WHERE user_id = :userId AND id NOT IN (SELECT MAX(id) FROM thermal_minute WHERE user_id = :userId GROUP BY create_time)")
    fun deleteRepeatVol(userId: String)

    @Query("DELETE FROM thermal_minute WHERE user_id = :userId")
    fun deleteByUserId(userId: String)
}