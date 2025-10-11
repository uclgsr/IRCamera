package mpdc4gsr.feature.capture.thermal.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.preference.PreferenceManager
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThermalSettingsRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: ThermalSettingsRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        repository = ThermalSettingsRepository(context)
    }

    @After
    fun tearDown() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
    }

    @Test
    fun `updates persist and are exposed via state flow`() {
        repository.updateFrameRate(18)
        repository.updatePalette("Arctic")
        repository.updateSaveRawImages(true)
        repository.updateTemperatureRange("Manual")

        val settings = repository.getSettings()
        assertEquals(18, settings.frameRate)
        assertEquals("Arctic", settings.palette)
        assertTrue(settings.saveRawImages)
        assertEquals("Manual", settings.temperatureRange)
    }

    @Test
    fun `thermal video config clamps bitrate and frame rate`() {
        repository.updateFrameRate(40)
        val config = repository.getThermalVideoConfig()

        assertEquals(30, config.frameRate)
        assertEquals(2_000_000, config.bitrate)
    }
}
