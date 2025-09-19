package com.guide.zm04c.matrix

/**
 * NativeGuideCore stub implementation
 * Contains only the methods referenced by GuideUsbManager
 */
class NativeGuideCore {
    
    /**
     * Calculate CRC for the given data
     * Minimal implementation - returns a simple checksum
     */
    fun crc(data: ByteArray): Int {
        var checksum = 0
        for (byte in data) {
            checksum += byte.toInt() and 0xFF
        }
        return checksum and 0xFFFF
    }
}