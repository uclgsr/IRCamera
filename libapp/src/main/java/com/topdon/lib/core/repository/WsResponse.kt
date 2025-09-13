package com.topdon.lib.core.repository

/**
 * websocket data推送响应.
 * @param cmd 推送指令
 * @param data 推送实体class，视不同指令Return对象不同
 * @param id 推送id
 */
data class WsResponse<T>(
    val cmd: Int,
    val data: T?,
    val id: String,
)

/**
 * websocket - pseudo color样式
 */
data class WsPseudoColor(
    val enable: Boolean?, // white hot-1，black hot-2，警示红-12, iron red-5，观鸟-16
    val mode: Int?,
)

/**
 * websocket - 测距
 */
data class WsRange(
    val state: Int?, // 0-Close，1-开启
)

/**
 * websocket - brightness
 */
data class WsLight(
    val brightness: Int?, // 81-100 高，61-80 中，0-60 低
)

/**
 * websocket - 画中画
 */
data class WsPip(
    val enable: Int?, // 0-Close，1-开启
)

/**
 * websocket - 放大倍数
 */
data class WsZoom(
    val enable: Boolean?, 
    val factor: Int?, 
)
