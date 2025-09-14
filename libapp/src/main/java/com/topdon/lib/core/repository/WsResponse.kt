package com.topdon.lib.core.repository


data class WsResponse<T>(
    val cmd: Int,
    val data: T?,
    val id: String,
)


data class WsPseudoColor(
    val enable: Boolean?, // 白热-1，黑热-2，警示红-12, 铁红-5，观鸟-16
    val mode: Int?,
)


data class WsRange(
    val state: Int?, // 0-关闭，1-开启
)


data class WsLight(
    val brightness: Int?, // 81-100 高，61-80 中，0-60 低
)


data class WsPip(
    val enable: Int?, // 0-关闭，1-开启
)


data class WsZoom(
    val enable: Boolean?, // 使能缩放标识
    val factor: Int?, // 缩放比例
)
