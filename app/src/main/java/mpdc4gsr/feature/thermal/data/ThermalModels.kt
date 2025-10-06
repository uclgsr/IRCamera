package mpdc4gsr.feature.thermal.data
import androidx.compose.ui.graphics.Color
enum class ThermalPalette(
    val displayName: String,
    val colors: List<Color>
) {
    IRON("Iron", listOf(Color.Black, Color.Red, Color.Yellow, Color.White)),
    RAINBOW("Rainbow", listOf(Color.Blue, Color.Green, Color.Yellow, Color.Red)),
    GRAYSCALE("Grayscale", listOf(Color.Black, Color.Gray, Color.White)),
    HOT("Hot", listOf(Color.Black, Color.Red, Color(0xFFFFA500), Color.Yellow)),
    MEDICAL("Medical", listOf(Color.Blue, Color.Cyan, Color.Green, Color.Yellow)),
    ARCTIC("Arctic", listOf(Color.Blue, Color.Cyan, Color.White)),
    LAVA("Lava", listOf(Color.Black, Color.Red, Color(0xFFFF4500), Color(0xFFFFA500))),
    CONTRAST("Contrast", listOf(Color.Black, Color.White))
}
enum class TemperatureUnit(val displayName: String, val symbol: String) {
    CELSIUS("Celsius", "C"),
    FAHRENHEIT("Fahrenheit", "F"),
    KELVIN("Kelvin", "K")
}
enum class MeasurementMode(val displayName: String) {
    SPOT("Spot Measurement"),
    AREA("Area Measurement"),
    LINE("Line Measurement"),
    CONTINUOUS("Continuous Tracking")
}
