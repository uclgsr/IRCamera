package com.mpdc4gsr.lib.core.repository

// FileBean data class to replace removed TS004 functionality
data class FileBean(
    val id: Int = 0,
    val type: Int = 0,
    val duration: Int = 0,
    val size: Long = 0L,
    val name: String = "",
    val thumb: String = "",
    val time: Long = 0L,
    val timezone: Int = 0
)