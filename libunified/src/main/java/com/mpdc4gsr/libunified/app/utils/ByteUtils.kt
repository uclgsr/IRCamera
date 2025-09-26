package com.mpdc4gsr.libunified.app.utils

/**
 * Type alias for UnifiedByteUtils to maintain backward compatibility
 */
typealias ByteUtils = UnifiedByteUtils

// Extension functions to match expected interface
val ByteArray.descBytes: ByteArray get() = this
fun ByteArray.bytesToInt(): Int = UnifiedByteUtils.bytesToInt(this)
fun ByteArray.toBytes(): ByteArray = this