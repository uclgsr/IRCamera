package com.topdon.lib.core.repository

// 这个file用来放 TS004 interfaceReturn JSON 的封装

/**
 * TS004 所有interface请求Return的format内容.
 * @param command 不知道什么东西，艾睿的document也没说
 * @param data 实际Return的data，视不同的interface而定
 * @param detail 请求结果描述，如 "ok"、"error: request process error"
 * @param status state码 0-success 其他详见document
 * @param transmit_cast 该请求耗时毫秒数
 */
data class TS004Response<T>(
    val command: Int,
    val data: T?,
    val detail: String?,
    val status: Int,
    val transmit_cast: Int,
) {
    /**
     * 判断请求是否success.
     */
    fun isSuccess(): Boolean = status == 0
}

/**
 * TS004 interface请求Return：Get/Retrievepseudo color样式
 * @param enable
 * @param mode 当前pseudo color样式
 */
data class PseudoColorBean(
    val enable: Boolean?,
    val mode: Int?,
)

/**
 * TS004 interface请求Return：Get/Retrieve测距
 * @param state 0-Close，1-开启
 */
data class RangeBean(
    val state: Int?,
)

/**
 * TS004 interface请求Return：Get/Retrieve画中画
 * @param enable true Open，false Close
 */
data class PipBean(
    val enable: Boolean?,
)

/**
 * TS004 interface请求Return：Get/Retrieve屏幕brightness
 * brightness: Int
 */
data class BrightnessBean(
    val brightness: Int,
)

/**
 * TS004 interface请求Return：Get/Retrieve放大倍数
 * @param factor Scale比例
 */
data class ZoomBean(
    val factor: Int?,
)

/**
 * TS004 interface请求Return：Get/Retrieve超分state
 * @param enable 0-Close 1-开启
 */
data class TISRBean(
    val enable: Int?,
)

/**
 * TS004 interface请求Return：versioninfo
 * @param firmware firmwareversion，如1.0
 */
data class VersionBean(
    val firmware: String?,
)

/**
 * TS004 interface请求Return：deviceinfo
 * @param code Activate码（又叫Register码）
 * @param model 应该是devicetypename，如 TS004
 * @param sn sn
 * @param uuid 不知道啥
 */
data class DeviceInfo(
    val code: String,
    val model: String,
    val sn: String,
    val uuid: String,
)

/**
 * TS004 interface请求Return：file数量
 * @param fileCount file数量
 */
data class FileCountBean(
    val fileCount: Int,
)

/**
 * 一页fileinfo
 * @param current 当前Pagination数
 * @param total 总页数
 * @param filelist 当前Paginationfileinfo列表
 */
data class FilePageBean(
    val current: Int,
    val total: Int,
    val filelist: List<FileBean>,
)

/**
 * TS004 interface请求Return：fileinfo
 * @param type 0-photo 1-录像
 * @param duration 录像时长，单位秒
 * @param size file大小，单位 byte
 * @param name filename，如 1970_01_02075103.mp4
 * @param thumb 录像缩略图
 * @param time 拍摄时 Unix 时间戳，单位秒
 * @param timezone 通过时区settingsinterfacesettings的时区
 */
data class FileBean(
    val id: Int,
    val type: Int,
    val duration: Int,
    val size: Long,
    val name: String,
    val thumb: String,
    val time: Long,
    val timezone: Int,
)

/**
 * TS004 interface请求Return：firmwareUpgradestate
 * @param status 当前Upgradestate 1-start 2-running 3-failed 4-success
 * @param percent 当前Upgrade进度百分比
 */
data class UpgradeStatus(
    val status: Int,
    val percent: Int,
)

/**
 * TS004 interface请求Return：fileinfo
 * @param total 总storage大小，单位 byte
 * @param free 剩余可用storage大小，单位 byte
 * @param system 系统占用大小，单位 byte
 * @param image_size photostorage大小，单位 byte
 * @param video_size videostorage大小，单位 byte
 */
data class FreeSpaceBean(
    val total: Long,
    val free: Long,
    val system: Long,
    val image_size: Long,
    val video_size: Long,
) {
    /**
     * Executes hasusesize functionality.
     */
    fun hasUseSize(): Long = system + image_size + video_size
}

/**
 * TS004 interface请求Return：Get/Retrieve录像state
 * @param errCode recording的error代码， 0:无error，1: initializationerror，2: 电池电量低
 * @param path 当前recording的videofile名
 * @param pts 当前recording的时间
 * @param status 当前recording的开关
 */
data class RecordStatusBean(
    val errCode: Int,
    val path: String,
    val pts: Int,
    val status: Boolean,
)
