package mpdc4gsr.feature.capture.thermal.data

import androidx.compose.ui.graphics.Color
import kotlin.test.assertEquals
import org.junit.Test

class ThermalModelExtensionsTest {

    @Test
    fun `palette helpers return expected metadata`() {
        assertEquals("Iron", ThermalPalette.IRON.getDisplayName())
        assertEquals(Color(0xFF8B4513), ThermalPalette.IRON.getPreviewColor())
    }

    @Test
    fun `temperature unit helpers provide symbol and name`() {
        assertEquals("°C", TemperatureUnit.CELSIUS.getSymbol())
        assertEquals("Kelvin", TemperatureUnit.KELVIN.getDisplayName())
    }

    @Test
    fun `measurement mode helpers provide display text`() {
        assertEquals(
            "Area Measurement",
            MeasurementMode.AREA.getDisplayName(),
        )
        assertEquals(
            "Measure temperature along a line",
            MeasurementMode.LINE.getDescription(),
        )
    }
}

