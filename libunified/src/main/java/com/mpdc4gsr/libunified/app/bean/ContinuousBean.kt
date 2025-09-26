package com.mpdc4gsr.libunified.app.bean

/**
 * 连续拍照配置.
 * @param isOpen 是否开启
 * @param continuaTime 连续拍照时间间隔，单位毫秒
 * @param count 连续拍照数量
 */
data class ContinuousBean(var isOpen: Boolean = false, var continuaTime: Long = 1000, var count: Int = 3)