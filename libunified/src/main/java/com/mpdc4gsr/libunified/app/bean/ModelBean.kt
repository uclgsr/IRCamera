package com.mpdc4gsr.libunified.app.bean

data class ModelBean(
    var defaultModel: DataBean,
    var myselfModel: ArrayList<DataBean> = arrayListOf(),
)