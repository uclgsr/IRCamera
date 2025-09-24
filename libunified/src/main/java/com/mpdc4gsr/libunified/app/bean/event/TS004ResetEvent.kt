package com.mpdc4gsr.libunified.app.bean.event

/**
 * Event for TS004 reset functionality
 * Note: TS004 functionality has been mostly removed but this stub is kept for compatibility
 */
data class TS004ResetEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val reason: String = "reset_requested"
)