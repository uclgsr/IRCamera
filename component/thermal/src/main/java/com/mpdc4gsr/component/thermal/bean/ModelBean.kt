package com.mpdc4gsr.component.thermal.bean

data class ModelBean(
    var defaultModel: DataBean,
    var myselfModel: ArrayList<DataBean> = arrayListOf(),
)

data class DataBean(
    var id: Int = 1,
    var name: String = "1",
    var environment: Float = 30.0f,
    var distance: Float = 0.25f,
    var radiation: Float = 0.95f,
    var use: Boolean = false,
    // Additional thermal configuration properties
    var emissivity: Float = 0.95f,
    var ambientTemperature: Float = 30.0f,
    var humidity: Float = 0.6f,
    var atmosphericTemperature: Float = 30.0f,
    var transmittance: Float = 0.8f,
)

