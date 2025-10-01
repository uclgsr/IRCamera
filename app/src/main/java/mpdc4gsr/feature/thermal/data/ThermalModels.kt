package mpdc4gsr.feature.thermal.data

import androidx.compose.ui.graphics.Color

enum class ThermalPalette {
    IRON, RAINBOW, WHITE_HOT, BLACK_HOT, RED_HOT, ARCTIC, GRAYSCALE, HOT
}

enum class TemperatureUnit {
    CELSIUS, FAHRENHEIT, KELVIN
}

enum class MeasurementMode {
    POINT, LINE, AREA, ISOTHERMAL
}
