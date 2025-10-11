package mpdc4gsr.core.common

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlin.math.roundToInt
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DimensionUtilsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `dp and px conversions are inverses`() {
        val px = 24.dpToPx(context)
        val dp = px.pxToDp(context)

        assertEquals(24, dp)
    }

    @Test
    fun `sp conversion uses display metrics`() {
        val px = 16.spToPx(context)
        val density = context.resources.displayMetrics.density

        assertEquals((16 * density).roundToInt(), px)
    }

    @Test
    fun `float conversions preserve precision`() {
        val px = 12.5f.dpToPx(context)
        val dp = px.pxToDp(context)

        assertEquals(12.5f, dp, 0.01f)
    }
}

