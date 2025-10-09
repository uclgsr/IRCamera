package mpdc4gsr.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class TimeSyncManagerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val tempDir: File = Files.createTempDirectory("tsync").toFile()
    private val manager = TimeSyncManager(context)

    @AfterTest
    fun tearDown() {
        manager.cleanup()
        tempDir.deleteRecursively()
    }

    @Test
    fun completeSyncCalculationUsesStoredIndexWhenResultMissing() = runBlocking {
        val sessionDir = File(tempDir, "session").apply { mkdirs() }
        manager.initializeSession(sessionDir.absolutePath)
        val config = manager.getSyncConfiguration().copy(enableJsonLogging = false)
        manager.updateSyncConfiguration(config)

        val pcSendTime = System.currentTimeMillis()
        val response = manager.performSyncResponse(pcSendTime)
        assertTrue(response.success)

        val pcReceiveTime = pcSendTime + 15
        val offsetMs = 7L
        val rttMs = pcReceiveTime - pcSendTime

        manager.completeSyncCalculation(
            t1 = pcSendTime,
            t2 = response.t2,
            t3 = pcReceiveTime,
            offsetMs = offsetMs,
            rttMs = rttMs,
            syncIndex = 0
        )

        manager.finalizeSession()

        val logFile = File(sessionDir, "timesync_log.csv")
        assertTrue(logFile.exists())

        val lines = logFile.readLines().filter { it.isNotBlank() && !it.startsWith("//") }
        assertTrue(lines.size >= 2)
        val entry = lines.last()
        val columns = entry.split(",")
        assertEquals(response.syncIndex.toString(), columns[0])
        assertEquals(offsetMs.toString(), columns[5])
        assertEquals(rttMs.toString(), columns[6])
    }
}
