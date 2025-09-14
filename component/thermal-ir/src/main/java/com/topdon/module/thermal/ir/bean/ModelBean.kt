package com.topdon.module.thermal.ir.bean

/**
// 模式
 */
/**
 * Model data model for thermal imaging information.
 * Encapsulates thermal measurement and configuration data.
 */
data class ModelBean(
    var defaultModel: DataBean,
    var myselfModel: ArrayList<DataBean> = arrayListOf(),
)

/**
 * Data data model for thermal imaging information.
 * Encapsulates thermal measurement and configuration data.
 */
data class DataBean(
    var id: Int = 1,
    var name: String = "1",
    var environment: Float = 30.0f, // 环境温度，单位摄氏度
    var distance: Float = 0.25f, // 距离，单位米
    var radiation: Float = 0.95f, // 发射率
    var use: Boolean = false,
)
