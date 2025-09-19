package com.guide.zm04c.matrix

/**
 * ResultCode constants for USB connection states
 * Minimal implementation with only the constants needed by GuideUsbManager
 */
object ResultCode {
    // USB connection states
    const val READY_CONNECT_DEVICE = 1
    const val SUCC_FIND_MATCHED_DEVICE = 2
    const val SUCC_FIND_DEVICE_INTERFACE = 3
    const val SUCC_CONNECT_INTERFACE = 4
    
    // Error codes
    const val ERROR_CONNECT_DEVICE_FAILD = -104
}