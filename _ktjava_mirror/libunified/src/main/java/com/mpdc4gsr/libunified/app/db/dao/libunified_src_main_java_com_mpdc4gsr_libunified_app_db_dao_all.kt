// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\db\dao' subtree
// Files: 6; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\db\dao\HouseDetectDao.kt =====

package com.mpdc4gsr.libunified.app.db.dao

import androidx.room.*
import com.mpdc4gsr.libunified.app.db.entity.DirDetect
import com.mpdc4gsr.libunified.app.db.entity.HouseDetect
import com.mpdc4gsr.libunified.app.db.entity.ItemDetect

@Dao
abstract class HouseDetectDao {
    @Transaction
    open fun insert(houseDetect: HouseDetect): Long {
        val id: Long = insertDetect(houseDetect)
        val dirList: ArrayList<DirDetect> = DirDetect.buildDefaultDirList(parentId = id)
        for (i in dirList.indices) {
            val dirId = insertDir(dirList[i])
            val itemList: ArrayList<ItemDetect> = ItemDetect.buildDefaultItemList(dirId, i)
            for (item in itemList) {
                insertItem(item)
            }
        }
        return id
    }

    @Transaction
    open fun insertDefaultDirs(houseDetect: HouseDetect) {
        houseDetect.dirList = DirDetect.buildDefaultDirList(parentId = houseDetect.id)
        for (i in houseDetect.dirList.indices) {
            val dir: DirDetect = houseDetect.dirList[i]
            dir.id = insertDir(dir)
            dir.houseDetect = houseDetect
            dir.itemList = ItemDetect.buildDefaultItemList(dir.id, i)
            for (item in dir.itemList) {
                item.id = insertItem(item)
                item.dirDetect = dir
            }
        }
    }

    @Transaction
    open fun queryById(id: Long): HouseDetect? {
        val houseDetect: HouseDetect = queryDetectById(id) ?: return null
        val dirList: List<DirDetect> = queryDirList(id)
        for (dir in dirList) {
            val itemList: List<ItemDetect> = queryItemList(dir.id)
            for (item in itemList) {
                item.dirDetect = dir
            }
            dir.houseDetect = houseDetect
            dir.itemList = ArrayList(itemList)
        }
        houseDetect.dirList = ArrayList(dirList)
        return houseDetect
    }

    open fun queryDir(dirId: Long): DirDetect? {
        val dir: DirDetect = queryDirById(dirId) ?: return null
        val itemList: List<ItemDetect> = queryItemList(dirId)
        for (item in itemList) {
            item.dirDetect = dir
        }
        dir.itemList = ArrayList(itemList)
        return dir
    }

    open fun refreshDetect(houseDetect: HouseDetect) {
        val oldDirList: ArrayList<DirDetect> = ArrayList(queryDirList(houseDetect.id))
        for (i in houseDetect.dirList.indices) {
            val dir = houseDetect.dirList[i]
            dir.position = i
            if (dir.id == 0L) {
                dir.id = insertDir(dir)
                for (item in dir.itemList) {
                    item.parentId = dir.id
                    item.id = insertItem(item)
                    item.dirDetect = dir
                }
            } else {
                updateDir(dir)
                oldDirList.remove(dir)
            }
        }
        for (delDir in oldDirList) {
            deleteDir(delDir)
        }
    }

    open fun refreshDir(dirDetect: DirDetect) {
        if (dirDetect.itemList.isEmpty()) {
            deleteDir(dirDetect)
        } else {
            updateDir(dirDetect)
            val oldItemList: ArrayList<ItemDetect> = ArrayList(queryItemList(dirDetect.id))
            for (i in dirDetect.itemList.indices) {
                val item = dirDetect.itemList[i]
                item.position = i
                if (item.id == 0L) {
                    item.id = insertItem(item)
                } else {
                    updateItem(item)
                    oldItemList.remove(item)
                }
            }
            for (delItem in oldItemList) {
                deleteItem(delItem)
            }
        }
    }

    @Transaction
    open fun copyDetect(oldDetect: HouseDetect): HouseDetect {
        val newDetect = oldDetect.copyOne()
        newDetect.id = insertDetect(newDetect)
        val dirList: List<DirDetect> = queryDirList(oldDetect.id)
        for (dir in dirList) {
            val itemList: List<ItemDetect> = queryItemList(dir.id)
            dir.id = 0
            dir.parentId = newDetect.id
            val dirId: Long = insertDir(dir)
            for (item in itemList) {
                item.id = 0
                item.parentId = dirId
                insertItem(item)
            }
        }
        return newDetect
    }

    @Transaction
    open fun copyDir(
        dirList: ArrayList<DirDetect>,
        position: Int,
    ): DirDetect {
        for (i in position + 1 until dirList.size) {
            val dir: DirDetect = dirList[i]
            dir.position += 1
            updateDir(dir)
        }
        val oldDir = dirList[position]
        val newDir = oldDir.copyOne()
        newDir.id = insertDir(newDir)
        for (item in newDir.itemList) {
            item.parentId = newDir.id
            item.id = insertItem(item)
            item.dirDetect = newDir
        }
        return newDir
    }

    @Transaction
    open fun copyItem(
        itemList: ArrayList<ItemDetect>,
        position: Int,
    ): ItemDetect {
        for (i in position + 1 until itemList.size) {
            val item: ItemDetect = itemList[i]
            item.position += 1
            updateItem(item)
        }
        val oldItem = itemList[position]
        val newItem =
            oldItem.copyOne(position = oldItem.position + 1, itemName = oldItem.copyName())
        newItem.id = insertItem(newItem)
        if (newItem.state > 0) {
            val dir = newItem.dirDetect
            when (newItem.state) {
                1 -> dir.goodCount++
                2 -> dir.warnCount++
                3 -> dir.dangerCount++
            }
            updateDir(dir)
        }
        return newItem
    }

    @Insert
    abstract fun insertDetect(houseDetect: HouseDetect): Long

    @Insert
    abstract fun insertDir(dirDetect: DirDetect): Long

    @Insert
    abstract fun insertItem(itemDetect: ItemDetect): Long

    @Delete
    abstract fun deleteDetect(houseDetect: HouseDetect)

    @Delete
    abstract fun deleteDir(dirDetect: DirDetect)

    @Delete
    abstract fun deleteItem(itemDetect: ItemDetect)

    @Update
    abstract fun updateDetect(houseDetect: HouseDetect)

    @Update
    abstract fun updateDir(dirDetect: DirDetect)

    @Update
    abstract fun updateItem(itemDetect: ItemDetect)

    @Query("SELECT * FROM HouseDetect ORDER BY createTime DESC")
    abstract fun queryAll(): List<HouseDetect>

    @Query("SELECT * FROM HouseDetect WHERE id = :id")
    abstract fun queryDetectById(id: Long): HouseDetect?

    @Query("SELECT * FROM DirDetect WHERE id = :id")
    abstract fun queryDirById(id: Long): DirDetect?

    @Query("SELECT * FROM DirDetect WHERE parentId = :detectId ORDER BY position")
    abstract fun queryDirList(detectId: Long): List<DirDetect>

    @Query("SELECT * FROM ItemDetect WHERE parentId = :dirId ORDER BY position")
    abstract fun queryItemList(dirId: Long): List<ItemDetect>
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\db\dao\HouseReportDao.kt =====

package com.mpdc4gsr.libunified.app.db.dao

import androidx.room.*
import com.mpdc4gsr.libunified.app.db.entity.DirReport
import com.mpdc4gsr.libunified.app.db.entity.HouseReport
import com.mpdc4gsr.libunified.app.db.entity.ItemReport

@Dao
abstract class HouseReportDao {
    @Transaction
    open fun insert(houseReport: HouseReport): Long {
        houseReport.id = insertReport(houseReport)
        for (dir in houseReport.dirList) {
            dir.parentId = houseReport.id
            dir.id = insertDir(dir)
            for (item in dir.itemList) {
                item.parentId = dir.id
                item.id = insertItem(item)
            }
        }
        return houseReport.id
    }

    open fun queryAllReport(): List<HouseReport> {
        val reportList: List<HouseReport> = queryAll()
        for (report in reportList) {
            val dirList: List<DirReport> = queryDirList(report.id)
            for (dir in dirList) {
                dir.itemList = ArrayList(queryItemList(dir.id))
            }
            report.dirList = ArrayList(dirList)
        }
        return reportList
    }

    @Transaction
    open fun queryById(id: Long): HouseReport? {
        val houseReport: HouseReport = queryReportById(id) ?: return null
        val dirList: List<DirReport> = queryDirList(id)
        for (dir in dirList) {
            val itemList: List<ItemReport> = queryItemList(dir.id)
            dir.itemList = ArrayList(itemList)
        }
        houseReport.dirList = ArrayList(dirList)
        return houseReport
    }

    @Insert
    abstract fun insertReport(houseReport: HouseReport): Long

    @Insert
    abstract fun insertDir(dirReport: DirReport): Long

    @Insert
    abstract fun insertItem(itemReport: ItemReport): Long

    @Delete
    abstract fun deleteReport(houseReport: HouseReport)

    @Delete
    abstract fun deleteDir(dirReport: DirReport)

    @Delete
    abstract fun deleteItem(itemReport: ItemReport)

    @Update
    abstract fun updateReport(houseReport: HouseReport)

    @Update
    abstract fun updateDir(dirReport: DirReport)

    @Update
    abstract fun updateItem(itemReport: ItemReport)

    @Query("SELECT * FROM HouseReport ORDER BY createTime DESC")
    abstract fun queryAll(): List<HouseReport>

    @Query("SELECT * FROM HouseReport WHERE id = :id")
    abstract fun queryReportById(id: Long): HouseReport?

    @Query("SELECT * FROM DirReport WHERE parentId = :reportId ORDER BY position")
    abstract fun queryDirList(reportId: Long): List<DirReport>

    @Query("SELECT * FROM ItemReport WHERE parentId = :dirId ORDER BY position")
    abstract fun queryItemList(dirId: Long): List<ItemReport>
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\db\dao\ThermalDao.kt =====

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


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\db\dao\ThermalDayDao.kt =====

package com.mpdc4gsr.libunified.app.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mpdc4gsr.libunified.app.db.entity.ThermalDayEntity

@Dao
interface ThermalDayDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ThermalDayEntity): Long

    @Query(
        "SELECT * FROM thermal_day WHERE user_id = :userId AND create_time >= :startTime AND create_time <= :endTime ORDER BY create_time",
    )
    suspend fun queryByTime(
        userId: String,
        startTime: Long,
        endTime: Long,
    ): List<ThermalDayEntity>

    @Query(
        "SELECT * FROM thermal_day WHERE user_id = :userId AND create_time >= :startTime AND create_time <= :endTime AND type = :type ORDER BY create_time",
    )
    suspend fun queryByTime(
        userId: String,
        startTime: Long,
        endTime: Long,
        type: String,
    ): List<ThermalDayEntity>

    @Query(
        "SELECT COALESCE(MAX(thermal_max), 0.0) FROM thermal_day WHERE user_id = :userId AND create_time >= :startTime AND create_time <= :endTime",
    )
    suspend fun queryByTimeMax(
        userId: String,
        startTime: Long,
        endTime: Long,
    ): Float

    @Query(
        "SELECT COALESCE(MIN(thermal_min), 0.0) FROM thermal_day WHERE user_id = :userId AND create_time >= :startTime AND create_time <= :endTime",
    )
    suspend fun queryByTimeMin(
        userId: String,
        startTime: Long,
        endTime: Long,
    ): Float

    @Query("SELECT COALESCE(MAX(create_time), 0) FROM thermal_day WHERE user_id = :userId")
    suspend fun queryMaxTime(userId: String): Long

    @Query(
        "DELETE FROM thermal_day WHERE user_id = :userId AND id NOT IN (SELECT MAX(id) FROM thermal_day WHERE user_id = :userId GROUP BY create_time)",
    )
    suspend fun deleteRepeatVol(userId: String)

    @Query("DELETE FROM thermal_day WHERE user_id = :userId")
    suspend fun deleteByUserId(userId: String)
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\db\dao\ThermalHourDao.kt =====

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


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\db\dao\ThermalMinuteDao.kt =====

package com.mpdc4gsr.libunified.app.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mpdc4gsr.libunified.app.db.entity.ThermalMinuteEntity

@Dao
interface ThermalMinuteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ThermalMinuteEntity): Long

    @Query(
        "SELECT * FROM thermal_minute WHERE user_id = :userId AND create_time >= :startTime AND create_time <= :endTime ORDER BY create_time",
    )
    suspend fun queryByTime(
        userId: String,
        startTime: Long,
        endTime: Long,
    ): List<ThermalMinuteEntity>

    @Query(
        "SELECT * FROM thermal_minute WHERE user_id = :userId AND create_time >= :startTime AND create_time <= :endTime AND type = :type ORDER BY create_time",
    )
    suspend fun queryByTime(
        userId: String,
        startTime: Long,
        endTime: Long,
        type: String,
    ): List<ThermalMinuteEntity>

    @Query(
        "SELECT COALESCE(MAX(thermal_max), 0.0) FROM thermal_minute WHERE user_id = :userId AND create_time >= :startTime AND create_time <= :endTime",
    )
    suspend fun queryByTimeMax(
        userId: String,
        startTime: Long,
        endTime: Long,
    ): Float

    @Query(
        "SELECT COALESCE(MIN(thermal_min), 0.0) FROM thermal_minute WHERE user_id = :userId AND create_time >= :startTime AND create_time <= :endTime",
    )
    suspend fun queryByTimeMin(
        userId: String,
        startTime: Long,
        endTime: Long,
    ): Float

    @Query("SELECT COALESCE(MAX(create_time), 0) FROM thermal_minute WHERE user_id = :userId")
    suspend fun queryMaxTime(userId: String): Long

    @Query(
        "DELETE FROM thermal_minute WHERE user_id = :userId AND id NOT IN (SELECT MAX(id) FROM thermal_minute WHERE user_id = :userId GROUP BY create_time)",
    )
    suspend fun deleteRepeatVol(userId: String)

    @Query("DELETE FROM thermal_minute WHERE user_id = :userId")
    suspend fun deleteByUserId(userId: String)
}


