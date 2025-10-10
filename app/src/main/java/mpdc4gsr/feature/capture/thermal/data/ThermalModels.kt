package mpdc4gsr.feature.capture.thermal.data

import androidx.compose.ui.graphics.Color

enum class ThermalPalette(
    val colors: List<Color>,
) {
    IRON(listOf(Color.Black, Color.Red, Color.Yellow, Color.White)),
    RAINBOW(listOf(Color.Blue, Color.Green, Color.Yellow, Color.Red)),
    GRAYSCALE(listOf(Color.Black, Color.Gray, Color.White)),
    HOT(listOf(Color.Black, Color.Red, Color(0xFFFFA500), Color.Yellow)),
    MEDICAL(listOf(Color.Blue, Color.Cyan, Color.Green, Color.Yellow)),
    ARCTIC(listOf(Color.Blue, Color.Cyan, Color.White)),
}

enum class TemperatureUnit {
    CELSIUS,
    FAHRENHEIT,
    KELVIN,
}

enum class MeasurementMode {
    SPOT,
    AREA,
    LINE,
    CONTINUOUS,
}
