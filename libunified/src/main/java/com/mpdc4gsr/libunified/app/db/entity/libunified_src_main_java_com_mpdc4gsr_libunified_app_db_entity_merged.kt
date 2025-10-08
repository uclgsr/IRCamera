// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\app\db\entity' directory and its subdirectories.
// Total files: 7 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\db\entity\DirBase.kt =====

package com.mpdc4gsr.libunified.app.db.entity

import androidx.room.*
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.compat.ContextProvider

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


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\db\entity\HouseBase.kt =====

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


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\db\entity\ItemBase.kt =====

package com.mpdc4gsr.libunified.app.db.entity

import android.content.Context
import androidx.room.*
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.compat.ContextProvider

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


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\db\entity\ThermalDayEntity.kt =====

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


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\db\entity\ThermalEntity.kt =====

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


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\db\entity\ThermalHourEntity.kt =====

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


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\db\entity\ThermalMinuteEntity.kt =====

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