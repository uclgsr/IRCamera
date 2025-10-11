package mpdc4gsr.feature.connectivity.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataManagementServiceTest {

    private lateinit var context: Context
    private lateinit var service: DataManagementService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        service = DataManagementService(context)
        service.initialize()
    }

    @Test
    fun `createSession initializes directory and metadata`() {
        val session = service.createSession("session-1", "device-1", mapOf("key" to "value"))

        assertEquals("session-1", session.sessionId)
        assertEquals("device-1", session.deviceId)
        assertEquals("value", session.metadata["key"])
        assert(session.directory.exists())
    }

    @Test
    fun `registerFile attaches file records to session`() {
        service.createSession("session-2", "device-2")
        service.registerFile(
            filePath = "/tmp/file.txt",
            sessionId = "session-2",
            deviceId = "device-2",
            fileType = "thermal",
            customMetadata = mapOf("quality" to "high"),
        )

        val session = service.getSession("session-2")
        assertNotNull(session)
        val record = session.files["/tmp/file.txt"]
        assertNotNull(record)
        assertEquals("thermal", record.fileType)
        assertEquals("high", record.metadata["quality"])
    }
}

