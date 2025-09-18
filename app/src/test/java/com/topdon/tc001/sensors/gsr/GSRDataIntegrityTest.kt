package com.topdon.tc001.sensors.gsr

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.topdon.tc001.sensors.unified.UnifiedGSRRecorder
import com.topdon.tc001.sensors.TimestampManager
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.StringWriter

@RunWith(RobolectricTestRunner::class)
class GSRDataIntegrityTest {
    
    private lateinit var mockContext: Context
    private lateinit var mockLifecycleOwner: LifecycleOwner
    private lateinit var gsrRecorder: UnifiedGSRRecorder
    private lateinit var timestampManager: TimestampManager
    
    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockLifecycleOwner = mockk(relaxed = true)
        gsrRecorder = UnifiedGSRRecorder(mockContext, mockLifecycleOwner)
        timestampManager = TimestampManager()
    }
    
    @Test
    fun `should generate monotonic timestamps`() {
        // Setup
        val timestamps = mutableListOf<Long>()
        
        // Execute - generate multiple timestamps
        repeat(10) {
            timestamps.add(timestampManager.getCurrentMonotonicNs())
            Thread.sleep(1) // Small delay to ensure different timestamps
        }
        
        // Verify
        for (i in 1 until timestamps.size) {
            assertTrue(
                "Timestamps should be monotonically increasing", 
                timestamps[i] > timestamps[i-1]
            )
        }
    }
    
    @Test
    fun `should validate timestamp alignment with system clock`() {
        // Setup
        val systemTimeBefore = System.currentTimeMillis()
        
        // Execute
        val timestampRecord = timestampManager.createTimestampRecord()
        
        // Verify
        val systemTimeAfter = System.currentTimeMillis()
        val recordTime = timestampRecord.systemTimeMs
        
        assertTrue(
            "Timestamp should align with system clock within tolerance",
            recordTime >= systemTimeBefore && recordTime <= systemTimeAfter + 1000
        )
    }
    
    @Test
    fun `should format CSV data correctly`() {
        // Setup
        val timestampRecord = timestampManager.createTimestampRecord()
        
        // Execute
        val csvLine = timestampRecord.toCsvFormat()
        
        // Verify
        val parts = csvLine.split(",")
        assertEquals("CSV should have 6 fields", 6, parts.size)
        
        parts.forEach { part ->
            assertNotNull("Each field should be non-null", part)
            assertTrue("Each field should be numeric", part.toLongOrNull() != null)
        }
    }
    
    @Test
    fun `should detect data corruption patterns`() {
        // Setup - simulate GSR data with known pattern
        val gsrValues = listOf(
            1000L, 1005L, 1010L, 1015L, // Normal progression
            0L,    // Corrupted value (sudden drop to 0)
            1020L, 1025L, 1030L        // Recovery
        )
        
        // Execute - analyze for corruption
        val corruptedIndices = mutableListOf<Int>()
        for (i in 1 until gsrValues.size - 1) {
            val prev = gsrValues[i-1]
            val current = gsrValues[i]
            val next = gsrValues[i+1]
            
            // Simple corruption detection: value drops to 0 or massive jump
            if (current == 0L || 
                Math.abs(current - prev) > 100 || 
                Math.abs(next - current) > 100) {
                corruptedIndices.add(i)
            }
        }
        
        // Verify
        assertTrue("Should detect corrupted value", corruptedIndices.contains(4))
        assertEquals("Should detect exactly one corruption", 1, corruptedIndices.size)
    }
    
    @Test
    fun `should validate session timing consistency`() = runTest {
        // Setup
        timestampManager.initializeSession()
        val sessionStart = timestampManager.getCurrentSessionRelativeNs()
        
        // Execute - wait and measure
        Thread.sleep(100) // 100ms delay
        val sessionCurrent = timestampManager.getCurrentSessionRelativeNs()
        
        // Verify
        val elapsedNs = sessionCurrent - sessionStart
        val elapsedMs = elapsedNs / 1_000_000
        
        assertTrue(
            "Session relative time should progress correctly", 
            elapsedMs >= 100 && elapsedMs <= 200 // Allow some tolerance
        )
    }
    
    @Test
    fun `should handle GSR stimulus response validation`() {
        // Setup - simulate GSR data with stimulus response
        val baselineGSR = 1000L
        val stimulusResponse = listOf(
            baselineGSR, baselineGSR, baselineGSR,     // Baseline
            baselineGSR + 50, baselineGSR + 150,      // Stimulus response
            baselineGSR + 100, baselineGSR + 50,      // Recovery
            baselineGSR, baselineGSR                   // Return to baseline
        )
        
        // Execute - detect stimulus response
        var maxIncrease = 0L
        var responseDetected = false
        
        for (i in 1 until stimulusResponse.size) {
            val increase = stimulusResponse[i] - stimulusResponse[0]
            if (increase > maxIncrease) {
                maxIncrease = increase
            }
            if (increase > 100) { // Threshold for stimulus detection
                responseDetected = true
            }
        }
        
        // Verify
        assertTrue("Should detect stimulus response", responseDetected)
        assertEquals("Maximum increase should be 150", 150L, maxIncrease)
    }
}