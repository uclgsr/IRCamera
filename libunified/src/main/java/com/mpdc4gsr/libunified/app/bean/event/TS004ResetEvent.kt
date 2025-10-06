package com.mpdc4gsr.libunified.app.bean.event
data class TS004ResetEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val reason: String = "reset_requested"
)