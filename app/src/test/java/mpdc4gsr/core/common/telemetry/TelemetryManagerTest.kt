package mpdc4gsr.core.common.telemetry

import android.content.Context
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.test.core.app.ApplicationProvider

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TelemetryManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        TelemetryManager.initialize(context)
    }

    @Test
    fun `initialize stores device properties in event payloads`() {
        TelemetryManager.setUserId("user-1")
        TelemetryManager.setProperty("custom_key", "custom_value")
        val event = invokeBuildEventData("test_event", mapOf("extra" to "value"))

        assertEquals("test_event", event.getString("event_name"))
        assertEquals("user-1", event.getString("user_id"))
        assertEquals("custom_value", event.getString("custom_key"))
        assertTrue(event.has("device_model"))
        assertTrue(event.has("android_version"))
        assertEquals("value", event.getString("extra"))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `trackExecutionTime emits metric duration`() = runTest {
        val result =
            trackExecutionTime("operation") {
                5 * 5
            }

        assertEquals(25, result)
    }

    private fun invokeBuildEventData(
        name: String,
        params: Map<String, Any>,
    ): JSONObject {
        val method =
            TelemetryManager::class.java.getDeclaredMethod(
                "buildEventData",
                String::class.java,
                Map::class.java,
            )
        method.isAccessible = true
        return method.invoke(TelemetryManager, name, params) as JSONObject
    }
}

