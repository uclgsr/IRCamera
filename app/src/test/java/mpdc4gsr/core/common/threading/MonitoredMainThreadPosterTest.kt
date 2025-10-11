package mpdc4gsr.core.common.threading

import kotlin.test.assertEquals
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MonitoredMainThreadPosterTest {

    @After
    fun tearDown() {
        MonitoredMainThreadPoster.resetStatistics()
        MonitoredMainThreadPoster.removeCallbacksAndMessages()
    }

    @Test
    fun `post measures execution time and updates statistics`() {
        MonitoredMainThreadPoster.post("slow") {
            Thread.sleep(150)
        }
        MonitoredMainThreadPoster.post("normal") {
            Thread.sleep(10)
        }

        Shadows.shadowOf(android.os.Looper.getMainLooper()).runToEndOfTasks()

        val stats = MonitoredMainThreadPoster.getStatistics()
        assertEquals(2, stats.totalPosts)
        assertEquals(1, stats.slowPosts)
        assertEquals(0, stats.criticalPosts)
    }
}

