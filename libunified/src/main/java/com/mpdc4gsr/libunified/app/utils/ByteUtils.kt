package com.mpdc4gsr.libunified.app.utils

/**
 * Type alias for UnifiedByteUtils to maintain backward compatibility
 */
typealias ByteUtils = UnifiedByteUtils

// Extension functions to match expected interface
val ByteArray.descBytes: ByteArray get() = this
fun ByteArray.bytesToInt(): Int = UnifiedByteUtils.byteToInt(this)
fun ByteArray.toBytes(): ByteArray = this
fun ByteArray.bigBytesToInt(): Int = UnifiedByteUtils.byteToInt(this)
fun Int.toBytes(size: Int = 4): ByteArray = UnifiedByteUtils.run { this@toBytes.toBytes(size) }

// Add the bigBytesToInt function
fun bigBytesToInt(b1: Byte, b2: Byte, b3: Byte, b4: Byte): Int = UnifiedByteUtils.bigBytesToInt(b1, b2, b3, b4)