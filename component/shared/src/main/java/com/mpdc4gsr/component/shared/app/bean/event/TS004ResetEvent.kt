package com.mpdc4gsr.component.shared.app.bean.event

data class TS004ResetEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val reason: String = "reset_requested",
)


