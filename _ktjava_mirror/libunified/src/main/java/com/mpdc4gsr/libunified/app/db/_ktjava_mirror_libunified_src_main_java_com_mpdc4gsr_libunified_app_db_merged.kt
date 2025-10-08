// Merged ALL .kt and .java files from the '_ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\db' directory and its subdirectories.
// Total files: 3 | Generated on: 2025-10-08 01:42:45


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\db\dao\libunified_src_main_java_com_mpdc4gsr_libunified_app_db_dao_all.kt =====

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


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\db\entity\libunified_src_main_java_com_mpdc4gsr_libunified_app_db_entity_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\db\entity' subtree
// Files: 7; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\db\entity\DirBase.kt =====

package com.mpdc4gsr.libunified.app.db.entity

import androidx.room.*
import com.mpdc4gsr.libunified.compat.ContextProvider
import com.mpdc4gsr.libunified.R

open class DirBase {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    @ColumnInfo(index = true)
    open var parentId: Long = 0

    @ColumnInfo
    var position: Int = 0

    @ColumnInfo
    var dirName: String = ""

    @ColumnInfo
    var goodCount: Int = 0

    @ColumnInfo
    var warnCount: Int = 0

    @ColumnInfo
    var dangerCount: Int = 0
    override fun equals(other: Any?): Boolean = other is DirBase && other.id == id
    override fun hashCode(): Int = id.toInt()
    fun getGoodCountStr(): String = if (goodCount > 99) "99+" else goodCount.toString()
    fun getWarnCountStr(): String = if (warnCount > 99) "99+" else warnCount.toString()
    fun getDangerCountStr(): String = if (dangerCount > 99) "99+" else dangerCount.toString()
}

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = HouseDetect::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
)
class DirDetect() : DirBase() {
    @Ignore
    constructor(parentId: Long, position: Int, dirName: String) : this() {
        this.parentId = parentId
        this.position = position
        this.dirName = dirName
    }

    @ColumnInfo(index = true)
    override var parentId: Long = 0

    @Ignore
    var hasSelect = false

    @Ignore
    var isExpand: Boolean = false

    @Ignore
    var houseDetect = HouseDetect()

    @Ignore
    var itemList: ArrayList<ItemDetect> = ArrayList()
    fun copyOne(): DirDetect {
        val newDirDetect = DirDetect()
        newDirDetect.id = 0
        newDirDetect.parentId = parentId
        newDirDetect.position = position + 1
        newDirDetect.dirName = "$dirName(1)"
        newDirDetect.goodCount = goodCount
        newDirDetect.warnCount = warnCount
        newDirDetect.dangerCount = dangerCount
        newDirDetect.isExpand = isExpand
        newDirDetect.hasSelect = hasSelect
        newDirDetect.houseDetect = houseDetect
        val newItemList: ArrayList<ItemDetect> = ArrayList(itemList.size)
        for (oldItem in itemList) {
            newItemList.add(oldItem.copyOne(parentId = 0))
        }
        newDirDetect.itemList = newItemList
        return newDirDetect
    }

    fun toDirReport(): DirReport {
        val dirReport = DirReport()
        dirReport.id = 0
        dirReport.parentId = 0
        dirReport.position = position
        dirReport.dirName = dirName
        dirReport.goodCount = goodCount
        dirReport.warnCount = warnCount
        dirReport.dangerCount = dangerCount
        val newItemList: ArrayList<ItemReport> = ArrayList(itemList.size)
        for (itemDetect in itemList) {
            if (itemDetect.state > 0 || itemDetect.inputText.isNotEmpty() || itemDetect.image1.isNotEmpty()) {
                newItemList.add(itemDetect.toItemReport())
            }
        }
        dirReport.itemList = newItemList
        return dirReport
    }

    companion object {
        fun buildDefaultDirList(parentId: Long): ArrayList<DirDetect> =
            arrayListOf(
                DirDetect(parentId, 0, ContextProvider.getContext().getString(R.string.detect_dir1_root)),
                DirDetect(parentId, 1, ContextProvider.getContext().getString(R.string.detect_dir2_root)),
                DirDetect(parentId, 2, ContextProvider.getContext().getString(R.string.detect_dir3_root)),
                DirDetect(parentId, 3, ContextProvider.getContext().getString(R.string.detect_dir4_root)),
                DirDetect(parentId, 4, ContextProvider.getContext().getString(R.string.detect_dir5_root)),
                DirDetect(parentId, 5, ContextProvider.getContext().getString(R.string.detect_dir6_root)),
                DirDetect(parentId, 6, ContextProvider.getContext().getString(R.string.detect_dir7_root)),
                DirDetect(parentId, 7, ContextProvider.getContext().getString(R.string.detect_dir8_root)),
                DirDetect(parentId, 8, ContextProvider.getContext().getString(R.string.detect_dir9_root)),
                DirDetect(parentId, 9, ContextProvider.getContext().getString(R.string.detect_dir10_root)),
                DirDetect(parentId, 10, ContextProvider.getContext().getString(R.string.detect_dir11_root)),
            )
    }
}

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = HouseReport::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
)
class DirReport : DirBase() {
    @ColumnInfo(index = true)
    override var parentId: Long = 0

    @Ignore
    var itemList: ArrayList<ItemReport> = ArrayList()
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\db\entity\HouseBase.kt =====

package com.mpdc4gsr.libunified.app.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

open class HouseBase {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    @ColumnInfo
    var name: String = ""

    @ColumnInfo
    var inspectorName: String = ""

    @ColumnInfo
    var address: String = ""

    @ColumnInfo
    var imagePath: String = ""

    @ColumnInfo
    var year: Int? = null

    @ColumnInfo
    var houseSpace: String = ""

    @ColumnInfo
    var houseSpaceUnit: Int = 0

    @ColumnInfo
    var cost: String = ""

    @ColumnInfo
    var costUnit: Int = 0

    @ColumnInfo
    var detectTime: Long = 0

    @ColumnInfo
    var createTime: Long = 0

    @ColumnInfo
    var updateTime: Long = 0
    override fun equals(other: Any?): Boolean = other is HouseBase && other.id == id
    override fun hashCode(): Int = id.toInt()
    fun getSpaceUnitStr(): String =
        when (houseSpaceUnit) {
            0 -> "ac"
            1 -> "mÂ²"
            else -> "ha"
        }

    fun getCostUnitStr(): String =
        when (costUnit) {
            1 -> "EUR"
            2 -> "GBP"
            3 -> "AUD"
            4 -> "JPY"
            5 -> "CAD"
            6 -> "NZD"
            7 -> "RMB"
            8 -> "HKD"
            else -> "USD"
        }

    fun getPdfFileName(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        val formatted = Instant.ofEpochMilli(createTime)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
        return "TC_${formatted}.pdf"
    }
}

@Entity
class HouseDetect : HouseBase() {
    @Ignore
    var dirList: ArrayList<DirDetect> = ArrayList()
    fun copyOne(): HouseDetect {
        val newDetect = HouseDetect()
        newDetect.id = 0
        newDetect.name = "$name(1)"
        newDetect.inspectorName = inspectorName
        newDetect.address = address
        newDetect.imagePath = imagePath
        newDetect.year = year
        newDetect.houseSpace = houseSpace
        newDetect.houseSpaceUnit = houseSpaceUnit
        newDetect.cost = cost
        newDetect.costUnit = costUnit
        newDetect.detectTime = detectTime
        newDetect.createTime = createTime
        newDetect.updateTime = updateTime
        return newDetect
    }

    fun toHouseReport(): HouseReport {
        val houseReport = HouseReport()
        houseReport.id = 0
        houseReport.name = name
        houseReport.inspectorName = inspectorName
        houseReport.address = address
        houseReport.imagePath = imagePath
        houseReport.year = year
        houseReport.houseSpace = houseSpace
        houseReport.houseSpaceUnit = houseSpaceUnit
        houseReport.cost = cost
        houseReport.costUnit = costUnit
        houseReport.detectTime = detectTime
        houseReport.createTime = createTime
        houseReport.updateTime = updateTime
        val newDirList: ArrayList<DirReport> = ArrayList(dirList.size)
        for (dirDetect in dirList) {
            if (dirDetect.itemList.isNotEmpty()) {
                val dirRepost: DirReport = dirDetect.toDirReport()
                if (dirRepost.itemList.isNotEmpty()) {
                    newDirList.add(dirRepost)
                }
            }
        }
        houseReport.dirList = newDirList
        return houseReport
    }
}

@Entity
class HouseReport : HouseBase() {
    @ColumnInfo
    var inspectorWhitePath: String = ""

    @ColumnInfo
    var inspectorBlackPath: String = ""

    @ColumnInfo
    var houseOwnerWhitePath: String = ""

    @ColumnInfo
    var houseOwnerBlackPath: String = ""

    @Ignore
    var dirList: ArrayList<DirReport> = ArrayList()
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\db\entity\ItemBase.kt =====

package com.mpdc4gsr.libunified.app.db.entity

import android.content.Context
import androidx.room.*
import com.mpdc4gsr.libunified.compat.ContextProvider
import com.mpdc4gsr.libunified.R

open class ItemBase {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    @ColumnInfo(index = true)
    open var parentId: Long = 0

    @ColumnInfo
    var position: Int = 0

    @ColumnInfo
    var itemName: String = ""

    @ColumnInfo
    var state: Int = 0

    @ColumnInfo
    var inputText: String = ""

    @ColumnInfo
    var image1: String = ""

    @ColumnInfo
    var image2: String = ""

    @ColumnInfo
    var image3: String = ""

    @ColumnInfo
    var image4: String = ""
    override fun equals(other: Any?): Boolean = other is ItemBase && other.id == id
    override fun hashCode(): Int = id.toInt()
    fun getStateStr(context: Context): String =
        when (state) {
            1 -> context.getString(R.string.house_state_good)
            2 -> context.getString(R.string.house_state_repair)
            3 -> context.getString(R.string.house_state_replace)
            else -> ""
        }

    fun getImageSize(): Int {
        var result = 0
        if (image1.isNotEmpty()) {
            result++
        }
        if (image2.isNotEmpty()) {
            result++
        }
        if (image3.isNotEmpty()) {
            result++
        }
        if (image4.isNotEmpty()) {
            result++
        }
        return result
    }

    fun buildImageList(): ArrayList<String> {
        val resultList: ArrayList<String> = ArrayList(4)
        if (image1.isNotEmpty()) {
            resultList.add(image1)
        }
        if (image2.isNotEmpty()) {
            resultList.add(image2)
        }
        if (image3.isNotEmpty()) {
            resultList.add(image3)
        }
        if (image4.isNotEmpty()) {
            resultList.add(image4)
        }
        return resultList
    }

    fun addOneImage(imagePath: String?) {
        if (imagePath.isNullOrEmpty()) {
            return
        }
        if (image1.isEmpty()) {
            image1 = imagePath
        } else if (image2.isEmpty()) {
            image2 = imagePath
        } else if (image3.isEmpty()) {
            image3 = imagePath
        } else if (image4.isEmpty()) {
            image4 = imagePath
        }
    }

    fun delOneImage(imageNum: Int) {
        when (imageNum) {
            4 -> {
                image4 = ""
            }

            3 -> {
                if (image4.isEmpty()) {
                    image3 = ""
                } else {
                    image3 = image4
                    image4 = ""
                }
            }

            2 -> {
                if (image3.isEmpty()) {
                    image2 = ""
                } else {
                    image2 = image3
                    if (image4.isEmpty()) {
                        image3 = ""
                    } else {
                        image3 = image4
                        image4 = ""
                    }
                }
            }

            1 -> {
                if (image2.isEmpty()) {
                    image1 = ""
                } else {
                    image1 = image2
                    if (image3.isEmpty()) {
                        image2 = ""
                    } else {
                        image2 = image3
                        if (image4.isEmpty()) {
                            image3 = ""
                        } else {
                            image3 = image4
                            image4 = ""
                        }
                    }
                }
            }
        }
    }
}

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = DirDetect::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
)
class ItemDetect() : ItemBase() {
    @Ignore
    constructor(parentId: Long, position: Int, itemName: String) : this() {
        this.parentId = parentId
        this.position = position
        this.itemName = itemName
    }

    @ColumnInfo(index = true)
    override var parentId: Long = 0

    @Ignore
    var hasSelect = false

    @Ignore
    var dirDetect = DirDetect()
    fun copyName(): String = "$itemName(1)"
    fun copyOne(
        parentId: Long = this.parentId,
        position: Int = this.position,
        itemName: String = this.itemName,
    ): ItemDetect {
        val newItemDetect = ItemDetect()
        newItemDetect.id = 0
        newItemDetect.parentId = parentId
        newItemDetect.position = position
        newItemDetect.itemName = itemName
        newItemDetect.state = state
        newItemDetect.inputText = inputText
        newItemDetect.image1 = image1
        newItemDetect.image2 = image2
        newItemDetect.image3 = image3
        newItemDetect.image4 = image4
        newItemDetect.hasSelect = hasSelect
        newItemDetect.dirDetect = dirDetect
        return newItemDetect
    }

    fun toItemReport(): ItemReport {
        val itemReport = ItemReport()
        itemReport.id = 0
        itemReport.parentId = 0
        itemReport.position = position
        itemReport.itemName = itemName
        itemReport.state = state
        itemReport.inputText = inputText
        itemReport.image1 = image1
        itemReport.image2 = image2
        itemReport.image3 = image3
        itemReport.image4 = image4
        return itemReport
    }

    companion object {
        fun buildDefaultItemList(
            parentId: Long,
            position: Int,
        ): ArrayList<ItemDetect> =
            when (position) {
                0 ->
                    arrayListOf(
                        ItemDetect(parentId, 0, ContextProvider.getContext().getString(R.string.detect_item1)),
                        ItemDetect(
                            parentId,
                            1,
                            ContextProvider.getContext().getString(R.string.detect_dir1_item2)
                        ),
                        ItemDetect(
                            parentId,
                            2,
                            ContextProvider.getContext().getString(R.string.detect_dir1_item3)
                        ),
                        ItemDetect(
                            parentId,
                            3,
                            ContextProvider.getContext().getString(R.string.detect_dir1_item4)
                        ),
                        ItemDetect(
                            parentId,
                            4,
                            ContextProvider.getContext().getString(R.string.detect_dir1_item5)
                        ),
                        ItemDetect(
                            parentId,
                            5,
                            ContextProvider.getContext().getString(R.string.detect_dir1_item6)
                        ),
                        ItemDetect(
                            parentId,
                            6,
                            ContextProvider.getContext().getString(R.string.detect_dir1_item7)
                        ),
                    )

                1 ->
                    arrayListOf(
                        ItemDetect(parentId, 0, ContextProvider.getContext().getString(R.string.detect_item1)),
                        ItemDetect(
                            parentId,
                            1,
                            ContextProvider.getContext().getString(R.string.detect_dir2_item2)
                        ),
                        ItemDetect(
                            parentId,
                            2,
                            ContextProvider.getContext().getString(R.string.detect_dir2_item3)
                        ),
                        ItemDetect(
                            parentId,
                            3,
                            ContextProvider.getContext().getString(R.string.detect_dir2_item4)
                        ),
                        ItemDetect(
                            parentId,
                            4,
                            ContextProvider.getContext().getString(R.string.detect_dir2_item5)
                        ),
                        ItemDetect(
                            parentId,
                            5,
                            ContextProvider.getContext().getString(R.string.detect_dir2_item6)
                        ),
                    )

                2 ->
                    arrayListOf(
                        ItemDetect(
                            parentId,
                            0,
                            ContextProvider.getContext().getString(R.string.detect_dir3_item1)
                        ),
                        ItemDetect(
                            parentId,
                            1,
                            ContextProvider.getContext().getString(R.string.detect_dir3_item2)
                        ),
                        ItemDetect(
                            parentId,
                            2,
                            ContextProvider.getContext().getString(R.string.detect_dir3_item3)
                        ),
                    )

                3 ->
                    arrayListOf(
                        ItemDetect(parentId, 0, ContextProvider.getContext().getString(R.string.detect_item1)),
                        ItemDetect(
                            parentId,
                            1,
                            ContextProvider.getContext().getString(R.string.detect_dir4_item2)
                        ),
                        ItemDetect(
                            parentId,
                            2,
                            ContextProvider.getContext().getString(R.string.detect_dir4_item3)
                        ),
                        ItemDetect(
                            parentId,
                            3,
                            ContextProvider.getContext().getString(R.string.detect_dir4_item4)
                        ),
                        ItemDetect(
                            parentId,
                            4,
                            ContextProvider.getContext().getString(R.string.detect_dir4_item5)
                        ),
                        ItemDetect(
                            parentId,
                            5,
                            ContextProvider.getContext().getString(R.string.detect_dir4_item6)
                        ),
                        ItemDetect(
                            parentId,
                            6,
                            ContextProvider.getContext().getString(R.string.detect_dir4_item7)
                        ),
                    )

                4 ->
                    arrayListOf(
                        ItemDetect(parentId, 0, ContextProvider.getContext().getString(R.string.detect_item1)),
                        ItemDetect(
                            parentId,
                            1,
                            ContextProvider.getContext().getString(R.string.detect_dir5_item2)
                        ),
                        ItemDetect(
                            parentId,
                            2,
                            ContextProvider.getContext().getString(R.string.detect_dir5_item3)
                        ),
                        ItemDetect(
                            parentId,
                            3,
                            ContextProvider.getContext().getString(R.string.detect_dir5_item4)
                        ),
                        ItemDetect(
                            parentId,
                            4,
                            ContextProvider.getContext().getString(R.string.detect_dir5_item5)
                        ),
                        ItemDetect(
                            parentId,
                            5,
                            ContextProvider.getContext().getString(R.string.detect_dir5_item6)
                        ),
                        ItemDetect(
                            parentId,
                            6,
                            ContextProvider.getContext().getString(R.string.detect_dir5_item7)
                        ),
                        ItemDetect(
                            parentId,
                            7,
                            ContextProvider.getContext().getString(R.string.detect_dir5_item8)
                        ),
                        ItemDetect(
                            parentId,
                            8,
                            ContextProvider.getContext().getString(R.string.detect_dir5_item9)
                        ),
                    )

                5 ->
                    arrayListOf(
                        ItemDetect(parentId, 0, ContextProvider.getContext().getString(R.string.detect_item1)),
                        ItemDetect(
                            parentId,
                            1,
                            ContextProvider.getContext().getString(R.string.detect_dir6_item2)
                        ),
                        ItemDetect(
                            parentId,
                            2,
                            ContextProvider.getContext().getString(R.string.detect_dir6_item3)
                        ),
                    )

                6 ->
                    arrayListOf(
                        ItemDetect(parentId, 0, ContextProvider.getContext().getString(R.string.detect_item1)),
                        ItemDetect(
                            parentId,
                            1,
                            ContextProvider.getContext().getString(R.string.detect_dir7_item2)
                        ),
                        ItemDetect(
                            parentId,
                            2,
                            ContextProvider.getContext().getString(R.string.detect_dir7_item3)
                        ),
                        ItemDetect(
                            parentId,
                            3,
                            ContextProvider.getContext().getString(R.string.detect_dir7_item4)
                        ),
                        ItemDetect(
                            parentId,
                            4,
                            ContextProvider.getContext().getString(R.string.detect_dir7_item5)
                        ),
                        ItemDetect(
                            parentId,
                            5,
                            ContextProvider.getContext().getString(R.string.detect_dir7_item6)
                        ),
                        ItemDetect(
                            parentId,
                            6,
                            ContextProvider.getContext().getString(R.string.detect_dir7_item7)
                        ),
                        ItemDetect(
                            parentId,
                            7,
                            ContextProvider.getContext().getString(R.string.detect_dir7_item8)
                        ),
                        ItemDetect(
                            parentId,
                            8,
                            ContextProvider.getContext().getString(R.string.detect_dir7_item9)
                        ),
                    )

                7 ->
                    arrayListOf(
                        ItemDetect(parentId, 0, ContextProvider.getContext().getString(R.string.detect_item1)),
                        ItemDetect(
                            parentId,
                            1,
                            ContextProvider.getContext().getString(R.string.detect_dir8_item2)
                        ),
                        ItemDetect(
                            parentId,
                            2,
                            ContextProvider.getContext().getString(R.string.detect_dir8_item3)
                        ),
                        ItemDetect(
                            parentId,
                            3,
                            ContextProvider.getContext().getString(R.string.detect_dir8_item4)
                        ),
                        ItemDetect(
                            parentId,
                            4,
                            ContextProvider.getContext().getString(R.string.detect_dir8_item5)
                        ),
                    )

                8 ->
                    arrayListOf(
                        ItemDetect(parentId, 0, ContextProvider.getContext().getString(R.string.detect_item1)),
                        ItemDetect(
                            parentId,
                            1,
                            ContextProvider.getContext().getString(R.string.detect_dir9_item2)
                        ),
                        ItemDetect(
                            parentId,
                            2,
                            ContextProvider.getContext().getString(R.string.detect_dir9_item3)
                        ),
                        ItemDetect(
                            parentId,
                            3,
                            ContextProvider.getContext().getString(R.string.detect_dir9_item4)
                        ),
                        ItemDetect(
                            parentId,
                            4,
                            ContextProvider.getContext().getString(R.string.detect_dir9_item5)
                        ),
                    )

                9 ->
                    arrayListOf(
                        ItemDetect(parentId, 0, ContextProvider.getContext().getString(R.string.detect_item1)),
                        ItemDetect(
                            parentId,
                            1,
                            ContextProvider.getContext().getString(R.string.detect_dir10_item2)
                        ),
                        ItemDetect(
                            parentId,
                            2,
                            ContextProvider.getContext().getString(R.string.detect_dir10_item3)
                        ),
                        ItemDetect(
                            parentId,
                            3,
                            ContextProvider.getContext().getString(R.string.detect_dir10_item4)
                        ),
                    )

                else -> arrayListOf(
                    ItemDetect(
                        parentId,
                        0,
                        ContextProvider.getContext().getString(R.string.detect_item1)
                    )
                )
            }
    }
}

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = DirReport::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
)
class ItemReport : ItemBase() {
    @ColumnInfo(index = true)
    override var parentId: Long = 0
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\db\entity\ThermalDayEntity.kt =====

package com.mpdc4gsr.libunified.app.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mpdc4gsr.libunified.app.tools.TimeTools

@Entity(tableName = "thermal_day")
class ThermalDayEntity {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    @ColumnInfo(name = "thermal_id")
    var thermalId: String = ""

    @ColumnInfo(name = "user_id")
    var userId: String = ""

    @ColumnInfo(name = "thermal")
    var thermal: Float = 0f

    @ColumnInfo(name = "thermal_max")
    var thermalMax: Float = 0f

    @ColumnInfo(name = "thermal_min")
    var thermalMin: Float = 0f

    @ColumnInfo(name = "sn")
    var sn: String = ""

    @ColumnInfo(name = "info")
    var info: String = ""

    @ColumnInfo(name = "type")
    var type: String = ""

    @ColumnInfo(name = "start_time")
    var startTime: Long = 0

    @ColumnInfo(name = "create_time")
    var createTime: Long = 0

    @ColumnInfo(name = "update_time")
    var updateTime: Long = 0
    override fun toString(): String {
        return "ThermalDayEntity(id=$id, thermalId='$thermalId', userId='$userId', thermal=$thermal, thermalMax=$thermalMax, thermalMin=$thermalMin, sn='$sn', info='$info', type='$type', startTime=$startTime, createTime=$createTime, updateTime=$updateTime)"
    }

    fun getTime(): String {
        return TimeTools.reportTime(createTime)
    }

    fun getMaxTemp(): Float {
        return thermalMax
    }

    fun getMinTemp(): Float {
        return thermalMin
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\db\entity\ThermalEntity.kt =====

package com.mpdc4gsr.libunified.app.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mpdc4gsr.libunified.app.tools.TimeTools

@Entity(tableName = "thermal")
class ThermalEntity {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    @ColumnInfo(name = "thermal_id")
    var thermalId: String = ""

    @ColumnInfo(name = "user_id")
    var userId: String = ""

    @ColumnInfo(name = "thermal")
    var thermal: Float = 0f

    @ColumnInfo(name = "thermal_max")
    var thermalMax: Float = 0f

    @ColumnInfo(name = "thermal_min")
    var thermalMin: Float = 0f

    @ColumnInfo(name = "sn")
    var sn: String = ""

    @ColumnInfo(name = "info")
    var info: String = ""

    @ColumnInfo(name = "type")
    var type: String = ""

    @ColumnInfo(name = "start_time")
    var startTime: Long = 0

    @ColumnInfo(name = "create_time")
    var createTime: Long = 0

    @ColumnInfo(name = "update_time")
    var updateTime: Long = 0

    // Additional properties for comprehensive thermal data
    val temperature: Float get() = thermal
    val maxTemp: Float get() = thermalMax
    val minTemp: Float get() = thermalMin
    val avgTemp: Float get() = (thermalMax + thermalMin) / 2f
    val timestamp: Long get() = createTime
    val notes: String? get() = if (info.isBlank()) null else info
    override fun toString(): String {
        return "ThermalEntity(id=$id, thermalId='$thermalId', userId='$userId', thermal=$thermal, thermalMax=$thermalMax, thermalMin=$thermalMin, sn='$sn', info='$info', type='$type', startTime=$startTime, createTime=$createTime, updateTime=$updateTime)"
    }

    fun getTime(): String {
        return TimeTools.reportTime(createTime)
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\db\entity\ThermalHourEntity.kt =====

package com.mpdc4gsr.libunified.app.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mpdc4gsr.libunified.app.tools.TimeTools

@Entity(tableName = "thermal_hour")
class ThermalHourEntity {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    @ColumnInfo(name = "thermal_id")
    var thermalId: String = ""

    @ColumnInfo(name = "user_id")
    var userId: String = ""

    @ColumnInfo(name = "thermal")
    var thermal: Float = 0f

    @ColumnInfo(name = "thermal_max")
    var thermalMax: Float = 0f

    @ColumnInfo(name = "thermal_min")
    var thermalMin: Float = 0f

    @ColumnInfo(name = "sn")
    var sn: String = ""

    @ColumnInfo(name = "info")
    var info: String = ""

    @ColumnInfo(name = "type")
    var type: String = ""

    @ColumnInfo(name = "start_time")
    var startTime: Long = 0

    @ColumnInfo(name = "create_time")
    var createTime: Long = 0

    @ColumnInfo(name = "update_time")
    var updateTime: Long = 0
    override fun toString(): String {
        return "ThermalHourEntity(id=$id, thermalId='$thermalId', userId='$userId', thermal=$thermal, thermalMax=$thermalMax, thermalMin=$thermalMin, sn='$sn', info='$info', type='$type', startTime=$startTime, createTime=$createTime, updateTime=$updateTime)"
    }

    fun getTime(): String {
        return TimeTools.reportTime(createTime)
    }

    fun getMaxTemp(): Float {
        return thermalMax
    }

    fun getMinTemp(): Float {
        return thermalMin
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\db\entity\ThermalMinuteEntity.kt =====

package com.mpdc4gsr.libunified.app.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mpdc4gsr.libunified.app.tools.TimeTools

@Entity(tableName = "thermal_minute")
class ThermalMinuteEntity {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    @ColumnInfo(name = "thermal_id")
    var thermalId: String = ""

    @ColumnInfo(name = "user_id")
    var userId: String = ""

    @ColumnInfo(name = "thermal")
    var thermal: Float = 0f

    @ColumnInfo(name = "thermal_max")
    var thermalMax: Float = 0f

    @ColumnInfo(name = "thermal_min")
    var thermalMin: Float = 0f

    @ColumnInfo(name = "sn")
    var sn: String = ""

    @ColumnInfo(name = "info")
    var info: String = ""

    @ColumnInfo(name = "type")
    var type: String = ""

    @ColumnInfo(name = "start_time")
    var startTime: Long = 0

    @ColumnInfo(name = "create_time")
    var createTime: Long = 0

    @ColumnInfo(name = "update_time")
    var updateTime: Long = 0
    override fun toString(): String {
        return "ThermalMinuteEntity(id=$id, thermalId='$thermalId', userId='$userId', thermal=$thermal, thermalMax=$thermalMax, thermalMin=$thermalMin, sn='$sn', info='$info', type='$type', startTime=$startTime, createTime=$createTime, updateTime=$updateTime)"
    }

    fun getTime(): String {
        return TimeTools.reportTime(createTime)
    }

    fun getMaxTemp(): Float {
        return thermalMax
    }

    fun getMinTemp(): Float {
        return thermalMin
    }
}


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\db\libunified_src_main_java_com_mpdc4gsr_libunified_app_db_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\db' subtree
// Files: 14; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\db\AppDatabase.kt =====

package com.mpdc4gsr.libunified.app.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mpdc4gsr.libunified.app.db.dao.*
import com.mpdc4gsr.libunified.compat.ContextProvider
import com.mpdc4gsr.libunified.app.db.entity.*

@Database(
    entities = [
        ThermalEntity::class,
        ThermalMinuteEntity::class,
        ThermalHourEntity::class,
        ThermalDayEntity::class,
        HouseDetect::class,
        HouseReport::class,
        DirDetect::class,
        DirReport::class,
        ItemDetect::class,
        ItemReport::class,
    ],
    version = 6,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun thermalDao(): ThermalDao
    abstract fun thermalMinDao(): ThermalMinuteDao
    abstract fun thermalHourDao(): ThermalHourDao
    abstract fun thermalDayDao(): ThermalDayDao
    abstract fun houseDetectDao(): HouseDetectDao
    abstract fun houseReportDao(): HouseReportDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context = ContextProvider.getContext()): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "MPDC4GSR.db")
                .addMigrations(
                    object : Migration(4, 5) {
                        override fun migrate(db: SupportSQLiteDatabase) {
                            db.execSQL("DROP TABLE file")
                            db.execSQL("DROP TABLE tc001_file")
                            db.execSQL("DROP TABLE thermal_minute")
                            db.execSQL("DROP TABLE thermal_hour")
                            db.execSQL("DROP TABLE thermal_day")
                            db.execSQL(
                                "CREATE TABLE IF NOT EXISTS `thermal` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `thermal_id` TEXT NOT NULL, `user_id` TEXT NOT NULL, `thermal` REAL NOT NULL, `thermal_max` REAL NOT NULL, `thermal_min` REAL NOT NULL, `sn` TEXT NOT NULL, `info` TEXT NOT NULL, `type` TEXT NOT NULL, `start_time` INTEGER NOT NULL, `create_time` INTEGER NOT NULL, `update_time` INTEGER NOT NULL)",
                            )
                            db.execSQL(
                                "CREATE TABLE IF NOT EXISTS `HouseDetect` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `inspectorName` TEXT NOT NULL, `address` TEXT NOT NULL, `imagePath` TEXT NOT NULL, `year` INTEGER, `houseSpace` TEXT NOT NULL, `houseSpaceUnit` INTEGER NOT NULL, `cost` TEXT NOT NULL, `costUnit` INTEGER NOT NULL, `detectTime` INTEGER NOT NULL, `createTime` INTEGER NOT NULL, `updateTime` INTEGER NOT NULL)",
                            )
                            db.execSQL(
                                "CREATE TABLE IF NOT EXISTS `HouseReport` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `inspectorName` TEXT NOT NULL, `address` TEXT NOT NULL, `imagePath` TEXT NOT NULL, `year` INTEGER, `houseSpace` TEXT NOT NULL, `houseSpaceUnit` INTEGER NOT NULL, `cost` TEXT NOT NULL, `costUnit` INTEGER NOT NULL, `detectTime` INTEGER NOT NULL, `createTime` INTEGER NOT NULL, `updateTime` INTEGER NOT NULL, `inspectorWhitePath` TEXT NOT NULL, `inspectorBlackPath` TEXT NOT NULL, `houseOwnerWhitePath` TEXT NOT NULL, `houseOwnerBlackPath` TEXT NOT NULL)",
                            )
                            db.execSQL(
                                "CREATE TABLE IF NOT EXISTS `DirDetect` (`parentId` INTEGER NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `position` INTEGER NOT NULL, `dirName` TEXT NOT NULL, `goodCount` INTEGER NOT NULL, `warnCount` INTEGER NOT NULL, `dangerCount` INTEGER NOT NULL, FOREIGN KEY(`parentId`) REFERENCES `HouseDetect`(`id`) ON UPDATE CASCADE ON DELETE CASCADE )",
                            )
                            db.execSQL(
                                "CREATE TABLE IF NOT EXISTS `DirReport` (`parentId` INTEGER NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `position` INTEGER NOT NULL, `dirName` TEXT NOT NULL, `goodCount` INTEGER NOT NULL, `warnCount` INTEGER NOT NULL, `dangerCount` INTEGER NOT NULL, FOREIGN KEY(`parentId`) REFERENCES `HouseReport`(`id`) ON UPDATE CASCADE ON DELETE CASCADE )",
                            )
                            db.execSQL(
                                "CREATE TABLE IF NOT EXISTS `ItemDetect` (`parentId` INTEGER NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `position` INTEGER NOT NULL, `itemName` TEXT NOT NULL, `state` INTEGER NOT NULL, `inputText` TEXT NOT NULL, `image1` TEXT NOT NULL, `image2` TEXT NOT NULL, `image3` TEXT NOT NULL, `image4` TEXT NOT NULL, FOREIGN KEY(`parentId`) REFERENCES `DirDetect`(`id`) ON UPDATE CASCADE ON DELETE CASCADE )",
                            )
                            db.execSQL(
                                "CREATE TABLE IF NOT EXISTS `ItemReport` (`parentId` INTEGER NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `position` INTEGER NOT NULL, `itemName` TEXT NOT NULL, `state` INTEGER NOT NULL, `inputText` TEXT NOT NULL, `image1` TEXT NOT NULL, `image2` TEXT NOT NULL, `image3` TEXT NOT NULL, `image4` TEXT NOT NULL, FOREIGN KEY(`parentId`) REFERENCES `DirReport`(`id`) ON UPDATE CASCADE ON DELETE CASCADE )",
                            )
                            db.execSQL("CREATE INDEX IF NOT EXISTS `index_DirDetect_parentId` ON `DirDetect` (`parentId`)")
                            db.execSQL("CREATE INDEX IF NOT EXISTS `index_DirReport_parentId` ON `DirReport` (`parentId`)")
                            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ItemDetect_parentId` ON `ItemDetect` (`parentId`)")
                            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ItemReport_parentId` ON `ItemReport` (`parentId`)")
                        }
                    },
                )
                .addMigrations(
                    object : Migration(5, 6) {
                        override fun migrate(db: SupportSQLiteDatabase) {
                            db.execSQL(
                                "CREATE TABLE IF NOT EXISTS `thermal_minute` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `thermal_id` TEXT NOT NULL, `user_id` TEXT NOT NULL, `thermal` REAL NOT NULL, `thermal_max` REAL NOT NULL, `thermal_min` REAL NOT NULL, `sn` TEXT NOT NULL, `info` TEXT NOT NULL, `type` TEXT NOT NULL, `start_time` INTEGER NOT NULL, `create_time` INTEGER NOT NULL, `update_time` INTEGER NOT NULL)",
                            )
                            db.execSQL(
                                "CREATE TABLE IF NOT EXISTS `thermal_hour` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `thermal_id` TEXT NOT NULL, `user_id` TEXT NOT NULL, `thermal` REAL NOT NULL, `thermal_max` REAL NOT NULL, `thermal_min` REAL NOT NULL, `sn` TEXT NOT NULL, `info` TEXT NOT NULL, `type` TEXT NOT NULL, `start_time` INTEGER NOT NULL, `create_time` INTEGER NOT NULL, `update_time` INTEGER NOT NULL)",
                            )
                            db.execSQL(
                                "CREATE TABLE IF NOT EXISTS `thermal_day` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `thermal_id` TEXT NOT NULL, `user_id` TEXT NOT NULL, `thermal` REAL NOT NULL, `thermal_max` REAL NOT NULL, `thermal_min` REAL NOT NULL, `sn` TEXT NOT NULL, `info` TEXT NOT NULL, `type` TEXT NOT NULL, `start_time` INTEGER NOT NULL, `create_time` INTEGER NOT NULL, `update_time` INTEGER NOT NULL)",
                            )
                        }
                    },
                )
                .fallbackToDestructiveMigration(true)
                .build()
    }
}


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


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\db\entity\DirBase.kt =====

package com.mpdc4gsr.libunified.app.db.entity

import androidx.room.*
import com.mpdc4gsr.libunified.compat.ContextProvider
import com.mpdc4gsr.libunified.R

open class DirBase {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    @ColumnInfo(index = true)
    open var parentId: Long = 0

    @ColumnInfo
    var position: Int = 0

    @ColumnInfo
    var dirName: String = ""

    @ColumnInfo
    var goodCount: Int = 0

    @ColumnInfo
    var warnCount: Int = 0

    @ColumnInfo
    var dangerCount: Int = 0
    override fun equals(other: Any?): Boolean = other is DirBase && other.id == id
    override fun hashCode(): Int = id.toInt()
    fun getGoodCountStr(): String = if (goodCount > 99) "99+" else goodCount.toString()
    fun getWarnCountStr(): String = if (warnCount > 99) "99+" else warnCount.toString()
    fun getDangerCountStr(): String = if (dangerCount > 99) "99+" else dangerCount.toString()
}

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = HouseDetect::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
)
class DirDetect() : DirBase() {
    @Ignore
    constructor(parentId: Long, position: Int, dirName: String) : this() {
        this.parentId = parentId
        this.position = position
        this.dirName = dirName
    }

    @ColumnInfo(index = true)
    override var parentId: Long = 0

    @Ignore
    var hasSelect = false

    @Ignore
    var isExpand: Boolean = false

    @Ignore
    var houseDetect = HouseDetect()

    @Ignore
    var itemList: ArrayList<ItemDetect> = ArrayList()
    fun copyOne(): DirDetect {
        val newDirDetect = DirDetect()
        newDirDetect.id = 0
        newDirDetect.parentId = parentId
        newDirDetect.position = position + 1
        newDirDetect.dirName = "$dirName(1)"
        newDirDetect.goodCount = goodCount
        newDirDetect.warnCount = warnCount
        newDirDetect.dangerCount = dangerCount
        newDirDetect.isExpand = isExpand
        newDirDetect.hasSelect = hasSelect
        newDirDetect.houseDetect = houseDetect
        val newItemList: ArrayList<ItemDetect> = ArrayList(itemList.size)
        for (oldItem in itemList) {
            newItemList.add(oldItem.copyOne(parentId = 0))
        }
        newDirDetect.itemList = newItemList
        return newDirDetect
    }

    fun toDirReport(): DirReport {
        val dirReport = DirReport()
        dirReport.id = 0
        dirReport.parentId = 0
        dirReport.position = position
        dirReport.dirName = dirName
        dirReport.goodCount = goodCount
        dirReport.warnCount = warnCount
        dirReport.dangerCount = dangerCount
        val newItemList: ArrayList<ItemReport> = ArrayList(itemList.size)
        for (itemDetect in itemList) {
            if (itemDetect.state > 0 || itemDetect.inputText.isNotEmpty() || itemDetect.image1.isNotEmpty()) {
                newItemList.add(itemDetect.toItemReport())
            }
        }
        dirReport.itemList = newItemList
        return dirReport
    }

    companion object {
        fun buildDefaultDirList(parentId: Long): ArrayList<DirDetect> =
            arrayListOf(
                DirDetect(parentId, 0, ContextProvider.getContext().getString(R.string.detect_dir1_root)),
                DirDetect(parentId, 1, ContextProvider.getContext().getString(R.string.detect_dir2_root)),
                DirDetect(parentId, 2, ContextProvider.getContext().getString(R.string.detect_dir3_root)),
                DirDetect(parentId, 3, ContextProvider.getContext().getString(R.string.detect_dir4_root)),
                DirDetect(parentId, 4, ContextProvider.getContext().getString(R.string.detect_dir5_root)),
                DirDetect(parentId, 5, ContextProvider.getContext().getString(R.string.detect_dir6_root)),
                DirDetect(parentId, 6, ContextProvider.getContext().getString(R.string.detect_dir7_root)),
                DirDetect(parentId, 7, ContextProvider.getContext().getString(R.string.detect_dir8_root)),
                DirDetect(parentId, 8, ContextProvider.getContext().getString(R.string.detect_dir9_root)),
                DirDetect(parentId, 9, ContextProvider.getContext().getString(R.string.detect_dir10_root)),
                DirDetect(parentId, 10, ContextProvider.getContext().getString(R.string.detect_dir11_root)),
            )
    }
}

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = HouseReport::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
)
class DirReport : DirBase() {
    @ColumnInfo(index = true)
    override var parentId: Long = 0

    @Ignore
    var itemList: ArrayList<ItemReport> = ArrayList()
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\db\entity\HouseBase.kt =====

package com.mpdc4gsr.libunified.app.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

open class HouseBase {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    @ColumnInfo
    var name: String = ""

    @ColumnInfo
    var inspectorName: String = ""

    @ColumnInfo
    var address: String = ""

    @ColumnInfo
    var imagePath: String = ""

    @ColumnInfo
    var year: Int? = null

    @ColumnInfo
    var houseSpace: String = ""

    @ColumnInfo
    var houseSpaceUnit: Int = 0

    @ColumnInfo
    var cost: String = ""

    @ColumnInfo
    var costUnit: Int = 0

    @ColumnInfo
    var detectTime: Long = 0

    @ColumnInfo
    var createTime: Long = 0

    @ColumnInfo
    var updateTime: Long = 0
    override fun equals(other: Any?): Boolean = other is HouseBase && other.id == id
    override fun hashCode(): Int = id.toInt()
    fun getSpaceUnitStr(): String =
        when (houseSpaceUnit) {
            0 -> "ac"
            1 -> "mÂ²"
            else -> "ha"
        }

    fun getCostUnitStr(): String =
        when (costUnit) {
            1 -> "EUR"
            2 -> "GBP"
            3 -> "AUD"
            4 -> "JPY"
            5 -> "CAD"
            6 -> "NZD"
            7 -> "RMB"
            8 -> "HKD"
            else -> "USD"
        }

    fun getPdfFileName(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        val formatted = Instant.ofEpochMilli(createTime)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
        return "TC_${formatted}.pdf"
    }
}

@Entity
class HouseDetect : HouseBase() {
    @Ignore
    var dirList: ArrayList<DirDetect> = ArrayList()
    fun copyOne(): HouseDetect {
        val newDetect = HouseDetect()
        newDetect.id = 0
        newDetect.name = "$name(1)"
        newDetect.inspectorName = inspectorName
        newDetect.address = address
        newDetect.imagePath = imagePath
        newDetect.year = year
        newDetect.houseSpace = houseSpace
        newDetect.houseSpaceUnit = houseSpaceUnit
        newDetect.cost = cost
        newDetect.costUnit = costUnit
        newDetect.detectTime = detectTime
        newDetect.createTime = createTime
        newDetect.updateTime = updateTime
        return newDetect
    }

    fun toHouseReport(): HouseReport {
        val houseReport = HouseReport()
        houseReport.id = 0
        houseReport.name = name
        houseReport.inspectorName = inspectorName
        houseReport.address = address
        houseReport.imagePath = imagePath
        houseReport.year = year
        houseReport.houseSpace = houseSpace
        houseReport.houseSpaceUnit = houseSpaceUnit
        houseReport.cost = cost
        houseReport.costUnit = costUnit
        houseReport.detectTime = detectTime
        houseReport.createTime = createTime
        houseReport.updateTime = updateTime
        val newDirList: ArrayList<DirReport> = ArrayList(dirList.size)
        for (dirDetect in dirList) {
            if (dirDetect.itemList.isNotEmpty()) {
                val dirRepost: DirReport = dirDetect.toDirReport()
                if (dirRepost.itemList.isNotEmpty()) {
                    newDirList.add(dirRepost)
                }
            }
        }
        houseReport.dirList = newDirList
        return houseReport
    }
}

@Entity
class HouseReport : HouseBase() {
    @ColumnInfo
    var inspectorWhitePath: String = ""

    @ColumnInfo
    var inspectorBlackPath: String = ""

    @ColumnInfo
    var houseOwnerWhitePath: String = ""

    @ColumnInfo
    var houseOwnerBlackPath: String = ""

    @Ignore
    var dirList: ArrayList<DirReport> = ArrayList()
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\db\entity\ItemBase.kt =====

package com.mpdc4gsr.libunified.app.db.entity

import android.content.Context
import androidx.room.*
import com.mpdc4gsr.libunified.compat.ContextProvider
import com.mpdc4gsr.libunified.R

open class ItemBase {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    @ColumnInfo(index = true)
    open var parentId: Long = 0

    @ColumnInfo
    var position: Int = 0

    @ColumnInfo
    var itemName: String = ""

    @ColumnInfo
    var state: Int = 0

    @ColumnInfo
    var inputText: String = ""

    @ColumnInfo
    var image1: String = ""

    @ColumnInfo
    var image2: String = ""

    @ColumnInfo
    var image3: String = ""

    @ColumnInfo
    var image4: String = ""
    override fun equals(other: Any?): Boolean = other is ItemBase && other.id == id
    override fun hashCode(): Int = id.toInt()
    fun getStateStr(context: Context): String =
        when (state) {
            1 -> context.getString(R.string.house_state_good)
            2 -> context.getString(R.string.house_state_repair)
            3 -> context.getString(R.string.house_state_replace)
            else -> ""
        }

    fun getImageSize(): Int {
        var result = 0
        if (image1.isNotEmpty()) {
            result++
        }
        if (image2.isNotEmpty()) {
            result++
        }
        if (image3.isNotEmpty()) {
            result++
        }
        if (image4.isNotEmpty()) {
            result++
        }
        return result
    }

    fun buildImageList(): ArrayList<String> {
        val resultList: ArrayList<String> = ArrayList(4)
        if (image1.isNotEmpty()) {
            resultList.add(image1)
        }
        if (image2.isNotEmpty()) {
            resultList.add(image2)
        }
        if (image3.isNotEmpty()) {
            resultList.add(image3)
        }
        if (image4.isNotEmpty()) {
            resultList.add(image4)
        }
        return resultList
    }

    fun addOneImage(imagePath: String?) {
        if (imagePath.isNullOrEmpty()) {
            return
        }
        if (image1.isEmpty()) {
            image1 = imagePath
        } else if (image2.isEmpty()) {
            image2 = imagePath
        } else if (image3.isEmpty()) {
            image3 = imagePath
        } else if (image4.isEmpty()) {
            image4 = imagePath
        }
    }

    fun delOneImage(imageNum: Int) {
        when (imageNum) {
            4 -> {
                image4 = ""
            }

            3 -> {
                if (image4.isEmpty()) {
                    image3 = ""
                } else {
                    image3 = image4
                    image4 = ""
                }
            }

            2 -> {
                if (image3.isEmpty()) {
                    image2 = ""
                } else {
                    image2 = image3
                    if (image4.isEmpty()) {
                        image3 = ""
                    } else {
                        image3 = image4
                        image4 = ""
                    }
                }
            }

            1 -> {
                if (image2.isEmpty()) {
                    image1 = ""
                } else {
                    image1 = image2
                    if (image3.isEmpty()) {
                        image2 = ""
                    } else {
                        image2 = image3
                        if (image4.isEmpty()) {
                            image3 = ""
                        } else {
                            image3 = image4
                            image4 = ""
                        }
                    }
                }
            }
        }
    }
}

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = DirDetect::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
)
class ItemDetect() : ItemBase() {
    @Ignore
    constructor(parentId: Long, position: Int, itemName: String) : this() {
        this.parentId = parentId
        this.position = position
        this.itemName = itemName
    }

    @ColumnInfo(index = true)
    override var parentId: Long = 0

    @Ignore
    var hasSelect = false

    @Ignore
    var dirDetect = DirDetect()
    fun copyName(): String = "$itemName(1)"
    fun copyOne(
        parentId: Long = this.parentId,
        position: Int = this.position,
        itemName: String = this.itemName,
    ): ItemDetect {
        val newItemDetect = ItemDetect()
        newItemDetect.id = 0
        newItemDetect.parentId = parentId
        newItemDetect.position = position
        newItemDetect.itemName = itemName
        newItemDetect.state = state
        newItemDetect.inputText = inputText
        newItemDetect.image1 = image1
        newItemDetect.image2 = image2
        newItemDetect.image3 = image3
        newItemDetect.image4 = image4
        newItemDetect.hasSelect = hasSelect
        newItemDetect.dirDetect = dirDetect
        return newItemDetect
    }

    fun toItemReport(): ItemReport {
        val itemReport = ItemReport()
        itemReport.id = 0
        itemReport.parentId = 0
        itemReport.position = position
        itemReport.itemName = itemName
        itemReport.state = state
        itemReport.inputText = inputText
        itemReport.image1 = image1
        itemReport.image2 = image2
        itemReport.image3 = image3
        itemReport.image4 = image4
        return itemReport
    }

    companion object {
        fun buildDefaultItemList(
            parentId: Long,
            position: Int,
        ): ArrayList<ItemDetect> =
            when (position) {
                0 ->
                    arrayListOf(
                        ItemDetect(parentId, 0, ContextProvider.getContext().getString(R.string.detect_item1)),
                        ItemDetect(
                            parentId,
                            1,
                            ContextProvider.getContext().getString(R.string.detect_dir1_item2)
                        ),
                        ItemDetect(
                            parentId,
                            2,
                            ContextProvider.getContext().getString(R.string.detect_dir1_item3)
                        ),
                        ItemDetect(
                            parentId,
                            3,
                            ContextProvider.getContext().getString(R.string.detect_dir1_item4)
                        ),
                        ItemDetect(
                            parentId,
                            4,
                            ContextProvider.getContext().getString(R.string.detect_dir1_item5)
                        ),
                        ItemDetect(
                            parentId,
                            5,
                            ContextProvider.getContext().getString(R.string.detect_dir1_item6)
                        ),
                        ItemDetect(
                            parentId,
                            6,
                            ContextProvider.getContext().getString(R.string.detect_dir1_item7)
                        ),
                    )

                1 ->
                    arrayListOf(
                        ItemDetect(parentId, 0, ContextProvider.getContext().getString(R.string.detect_item1)),
                        ItemDetect(
                            parentId,
                            1,
                            ContextProvider.getContext().getString(R.string.detect_dir2_item2)
                        ),
                        ItemDetect(
                            parentId,
                            2,
                            ContextProvider.getContext().getString(R.string.detect_dir2_item3)
                        ),
                        ItemDetect(
                            parentId,
                            3,
                            ContextProvider.getContext().getString(R.string.detect_dir2_item4)
                        ),
                        ItemDetect(
                            parentId,
                            4,
                            ContextProvider.getContext().getString(R.string.detect_dir2_item5)
                        ),
                        ItemDetect(
                            parentId,
                            5,
                            ContextProvider.getContext().getString(R.string.detect_dir2_item6)
                        ),
                    )

                2 ->
                    arrayListOf(
                        ItemDetect(
                            parentId,
                            0,
                            ContextProvider.getContext().getString(R.string.detect_dir3_item1)
                        ),
                        ItemDetect(
                            parentId,
                            1,
                            ContextProvider.getContext().getString(R.string.detect_dir3_item2)
                        ),
                        ItemDetect(
                            parentId,
                            2,
                            ContextProvider.getContext().getString(R.string.detect_dir3_item3)
                        ),
                    )

                3 ->
                    arrayListOf(
                        ItemDetect(parentId, 0, ContextProvider.getContext().getString(R.string.detect_item1)),
                        ItemDetect(
                            parentId,
                            1,
                            ContextProvider.getContext().getString(R.string.detect_dir4_item2)
                        ),
                        ItemDetect(
                            parentId,
                            2,
                            ContextProvider.getContext().getString(R.string.detect_dir4_item3)
                        ),
                        ItemDetect(
                            parentId,
                            3,
                            ContextProvider.getContext().getString(R.string.detect_dir4_item4)
                        ),
                        ItemDetect(
                            parentId,
                            4,
                            ContextProvider.getContext().getString(R.string.detect_dir4_item5)
                        ),
                        ItemDetect(
                            parentId,
                            5,
                            ContextProvider.getContext().getString(R.string.detect_dir4_item6)
                        ),
                        ItemDetect(
                            parentId,
                            6,
                            ContextProvider.getContext().getString(R.string.detect_dir4_item7)
                        ),
                    )

                4 ->
                    arrayListOf(
                        ItemDetect(parentId, 0, ContextProvider.getContext().getString(R.string.detect_item1)),
                        ItemDetect(
                            parentId,
                            1,
                            ContextProvider.getContext().getString(R.string.detect_dir5_item2)
                        ),
                        ItemDetect(
                            parentId,
                            2,
                            ContextProvider.getContext().getString(R.string.detect_dir5_item3)
                        ),
                        ItemDetect(
                            parentId,
                            3,
                            ContextProvider.getContext().getString(R.string.detect_dir5_item4)
                        ),
                        ItemDetect(
                            parentId,
                            4,
                            ContextProvider.getContext().getString(R.string.detect_dir5_item5)
                        ),
                        ItemDetect(
                            parentId,
                            5,
                            ContextProvider.getContext().getString(R.string.detect_dir5_item6)
                        ),
                        ItemDetect(
                            parentId,
                            6,
                            ContextProvider.getContext().getString(R.string.detect_dir5_item7)
                        ),
                        ItemDetect(
                            parentId,
                            7,
                            ContextProvider.getContext().getString(R.string.detect_dir5_item8)
                        ),
                        ItemDetect(
                            parentId,
                            8,
                            ContextProvider.getContext().getString(R.string.detect_dir5_item9)
                        ),
                    )

                5 ->
                    arrayListOf(
                        ItemDetect(parentId, 0, ContextProvider.getContext().getString(R.string.detect_item1)),
                        ItemDetect(
                            parentId,
                            1,
                            ContextProvider.getContext().getString(R.string.detect_dir6_item2)
                        ),
                        ItemDetect(
                            parentId,
                            2,
                            ContextProvider.getContext().getString(R.string.detect_dir6_item3)
                        ),
                    )

                6 ->
                    arrayListOf(
                        ItemDetect(parentId, 0, ContextProvider.getContext().getString(R.string.detect_item1)),
                        ItemDetect(
                            parentId,
                            1,
                            ContextProvider.getContext().getString(R.string.detect_dir7_item2)
                        ),
                        ItemDetect(
                            parentId,
                            2,
                            ContextProvider.getContext().getString(R.string.detect_dir7_item3)
                        ),
                        ItemDetect(
                            parentId,
                            3,
                            ContextProvider.getContext().getString(R.string.detect_dir7_item4)
                        ),
                        ItemDetect(
                            parentId,
                            4,
                            ContextProvider.getContext().getString(R.string.detect_dir7_item5)
                        ),
                        ItemDetect(
                            parentId,
                            5,
                            ContextProvider.getContext().getString(R.string.detect_dir7_item6)
                        ),
                        ItemDetect(
                            parentId,
                            6,
                            ContextProvider.getContext().getString(R.string.detect_dir7_item7)
                        ),
                        ItemDetect(
                            parentId,
                            7,
                            ContextProvider.getContext().getString(R.string.detect_dir7_item8)
                        ),
                        ItemDetect(
                            parentId,
                            8,
                            ContextProvider.getContext().getString(R.string.detect_dir7_item9)
                        ),
                    )

                7 ->
                    arrayListOf(
                        ItemDetect(parentId, 0, ContextProvider.getContext().getString(R.string.detect_item1)),
                        ItemDetect(
                            parentId,
                            1,
                            ContextProvider.getContext().getString(R.string.detect_dir8_item2)
                        ),
                        ItemDetect(
                            parentId,
                            2,
                            ContextProvider.getContext().getString(R.string.detect_dir8_item3)
                        ),
                        ItemDetect(
                            parentId,
                            3,
                            ContextProvider.getContext().getString(R.string.detect_dir8_item4)
                        ),
                        ItemDetect(
                            parentId,
                            4,
                            ContextProvider.getContext().getString(R.string.detect_dir8_item5)
                        ),
                    )

                8 ->
                    arrayListOf(
                        ItemDetect(parentId, 0, ContextProvider.getContext().getString(R.string.detect_item1)),
                        ItemDetect(
                            parentId,
                            1,
                            ContextProvider.getContext().getString(R.string.detect_dir9_item2)
                        ),
                        ItemDetect(
                            parentId,
                            2,
                            ContextProvider.getContext().getString(R.string.detect_dir9_item3)
                        ),
                        ItemDetect(
                            parentId,
                            3,
                            ContextProvider.getContext().getString(R.string.detect_dir9_item4)
                        ),
                        ItemDetect(
                            parentId,
                            4,
                            ContextProvider.getContext().getString(R.string.detect_dir9_item5)
                        ),
                    )

                9 ->
                    arrayListOf(
                        ItemDetect(parentId, 0, ContextProvider.getContext().getString(R.string.detect_item1)),
                        ItemDetect(
                            parentId,
                            1,
                            ContextProvider.getContext().getString(R.string.detect_dir10_item2)
                        ),
                        ItemDetect(
                            parentId,
                            2,
                            ContextProvider.getContext().getString(R.string.detect_dir10_item3)
                        ),
                        ItemDetect(
                            parentId,
                            3,
                            ContextProvider.getContext().getString(R.string.detect_dir10_item4)
                        ),
                    )

                else -> arrayListOf(
                    ItemDetect(
                        parentId,
                        0,
                        ContextProvider.getContext().getString(R.string.detect_item1)
                    )
                )
            }
    }
}

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = DirReport::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
)
class ItemReport : ItemBase() {
    @ColumnInfo(index = true)
    override var parentId: Long = 0
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\db\entity\ThermalDayEntity.kt =====

package com.mpdc4gsr.libunified.app.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mpdc4gsr.libunified.app.tools.TimeTools

@Entity(tableName = "thermal_day")
class ThermalDayEntity {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    @ColumnInfo(name = "thermal_id")
    var thermalId: String = ""

    @ColumnInfo(name = "user_id")
    var userId: String = ""

    @ColumnInfo(name = "thermal")
    var thermal: Float = 0f

    @ColumnInfo(name = "thermal_max")
    var thermalMax: Float = 0f

    @ColumnInfo(name = "thermal_min")
    var thermalMin: Float = 0f

    @ColumnInfo(name = "sn")
    var sn: String = ""

    @ColumnInfo(name = "info")
    var info: String = ""

    @ColumnInfo(name = "type")
    var type: String = ""

    @ColumnInfo(name = "start_time")
    var startTime: Long = 0

    @ColumnInfo(name = "create_time")
    var createTime: Long = 0

    @ColumnInfo(name = "update_time")
    var updateTime: Long = 0
    override fun toString(): String {
        return "ThermalDayEntity(id=$id, thermalId='$thermalId', userId='$userId', thermal=$thermal, thermalMax=$thermalMax, thermalMin=$thermalMin, sn='$sn', info='$info', type='$type', startTime=$startTime, createTime=$createTime, updateTime=$updateTime)"
    }

    fun getTime(): String {
        return TimeTools.reportTime(createTime)
    }

    fun getMaxTemp(): Float {
        return thermalMax
    }

    fun getMinTemp(): Float {
        return thermalMin
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\db\entity\ThermalEntity.kt =====

package com.mpdc4gsr.libunified.app.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mpdc4gsr.libunified.app.tools.TimeTools

@Entity(tableName = "thermal")
class ThermalEntity {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    @ColumnInfo(name = "thermal_id")
    var thermalId: String = ""

    @ColumnInfo(name = "user_id")
    var userId: String = ""

    @ColumnInfo(name = "thermal")
    var thermal: Float = 0f

    @ColumnInfo(name = "thermal_max")
    var thermalMax: Float = 0f

    @ColumnInfo(name = "thermal_min")
    var thermalMin: Float = 0f

    @ColumnInfo(name = "sn")
    var sn: String = ""

    @ColumnInfo(name = "info")
    var info: String = ""

    @ColumnInfo(name = "type")
    var type: String = ""

    @ColumnInfo(name = "start_time")
    var startTime: Long = 0

    @ColumnInfo(name = "create_time")
    var createTime: Long = 0

    @ColumnInfo(name = "update_time")
    var updateTime: Long = 0

    // Additional properties for comprehensive thermal data
    val temperature: Float get() = thermal
    val maxTemp: Float get() = thermalMax
    val minTemp: Float get() = thermalMin
    val avgTemp: Float get() = (thermalMax + thermalMin) / 2f
    val timestamp: Long get() = createTime
    val notes: String? get() = if (info.isBlank()) null else info
    override fun toString(): String {
        return "ThermalEntity(id=$id, thermalId='$thermalId', userId='$userId', thermal=$thermal, thermalMax=$thermalMax, thermalMin=$thermalMin, sn='$sn', info='$info', type='$type', startTime=$startTime, createTime=$createTime, updateTime=$updateTime)"
    }

    fun getTime(): String {
        return TimeTools.reportTime(createTime)
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\db\entity\ThermalHourEntity.kt =====

package com.mpdc4gsr.libunified.app.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mpdc4gsr.libunified.app.tools.TimeTools

@Entity(tableName = "thermal_hour")
class ThermalHourEntity {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    @ColumnInfo(name = "thermal_id")
    var thermalId: String = ""

    @ColumnInfo(name = "user_id")
    var userId: String = ""

    @ColumnInfo(name = "thermal")
    var thermal: Float = 0f

    @ColumnInfo(name = "thermal_max")
    var thermalMax: Float = 0f

    @ColumnInfo(name = "thermal_min")
    var thermalMin: Float = 0f

    @ColumnInfo(name = "sn")
    var sn: String = ""

    @ColumnInfo(name = "info")
    var info: String = ""

    @ColumnInfo(name = "type")
    var type: String = ""

    @ColumnInfo(name = "start_time")
    var startTime: Long = 0

    @ColumnInfo(name = "create_time")
    var createTime: Long = 0

    @ColumnInfo(name = "update_time")
    var updateTime: Long = 0
    override fun toString(): String {
        return "ThermalHourEntity(id=$id, thermalId='$thermalId', userId='$userId', thermal=$thermal, thermalMax=$thermalMax, thermalMin=$thermalMin, sn='$sn', info='$info', type='$type', startTime=$startTime, createTime=$createTime, updateTime=$updateTime)"
    }

    fun getTime(): String {
        return TimeTools.reportTime(createTime)
    }

    fun getMaxTemp(): Float {
        return thermalMax
    }

    fun getMinTemp(): Float {
        return thermalMin
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\db\entity\ThermalMinuteEntity.kt =====

package com.mpdc4gsr.libunified.app.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mpdc4gsr.libunified.app.tools.TimeTools

@Entity(tableName = "thermal_minute")
class ThermalMinuteEntity {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    @ColumnInfo(name = "thermal_id")
    var thermalId: String = ""

    @ColumnInfo(name = "user_id")
    var userId: String = ""

    @ColumnInfo(name = "thermal")
    var thermal: Float = 0f

    @ColumnInfo(name = "thermal_max")
    var thermalMax: Float = 0f

    @ColumnInfo(name = "thermal_min")
    var thermalMin: Float = 0f

    @ColumnInfo(name = "sn")
    var sn: String = ""

    @ColumnInfo(name = "info")
    var info: String = ""

    @ColumnInfo(name = "type")
    var type: String = ""

    @ColumnInfo(name = "start_time")
    var startTime: Long = 0

    @ColumnInfo(name = "create_time")
    var createTime: Long = 0

    @ColumnInfo(name = "update_time")
    var updateTime: Long = 0
    override fun toString(): String {
        return "ThermalMinuteEntity(id=$id, thermalId='$thermalId', userId='$userId', thermal=$thermal, thermalMax=$thermalMax, thermalMin=$thermalMin, sn='$sn', info='$info', type='$type', startTime=$startTime, createTime=$createTime, updateTime=$updateTime)"
    }

    fun getTime(): String {
        return TimeTools.reportTime(createTime)
    }

    fun getMaxTemp(): Float {
        return thermalMax
    }

    fun getMinTemp(): Float {
        return thermalMin
    }
}