package com.topdon.module.thermal.ir.report.bean

/**
 * ImageTempBean - Consolidated image temperature data bean
 * Migrated from thermal-ir module for MPDC4GSR
 */
data class ImageTempBean(
    val temperature: Float = 0.0f,
    val x: Int = 0,
    val y: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)