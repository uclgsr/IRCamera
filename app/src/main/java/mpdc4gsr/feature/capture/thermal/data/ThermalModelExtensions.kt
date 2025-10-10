package mpdc4gsr.feature.capture.thermal.data

import androidx.compose.ui.graphics.Color

fun ThermalPalette.getDisplayName(): String =
    when (this) {
        ThermalPalette.IRON -> "Iron"
        ThermalPalette.RAINBOW -> "Rainbow"
        ThermalPalette.GRAYSCALE -> "Grayscale"
        ThermalPalette.HOT -> "Hot"
        ThermalPalette.ARCTIC -> "Arctic"
        ThermalPalette.MEDICAL -> "Medical"
    }

fun ThermalPalette.getPreviewColor(): Color =
    when (this) {
        ThermalPalette.IRON -> Color(0xFF8B4513)
        ThermalPalette.RAINBOW -> Color(0xFF4169E1)
        ThermalPalette.GRAYSCALE -> Color(0xFF808080)
        ThermalPalette.HOT -> Color(0xFFFF4500)
        ThermalPalette.ARCTIC -> Color(0xFF00CED1)
        ThermalPalette.MEDICAL -> Color(0xFF32CD32)
    }

fun TemperatureUnit.getSymbol(): String =
    when (this) {
        TemperatureUnit.CELSIUS -> "°C"
        TemperatureUnit.FAHRENHEIT -> "°F"
        TemperatureUnit.KELVIN -> "K"
    }

fun TemperatureUnit.getDisplayName(): String =
    when (this) {
        TemperatureUnit.CELSIUS -> "Celsius"
        TemperatureUnit.FAHRENHEIT -> "Fahrenheit"
        TemperatureUnit.KELVIN -> "Kelvin"
    }

fun MeasurementMode.getDisplayName(): String =
    when (this) {
        MeasurementMode.SPOT -> "Spot Measurement"
        MeasurementMode.AREA -> "Area Measurement"
        MeasurementMode.LINE -> "Line Profile"
        MeasurementMode.CONTINUOUS -> "Continuous"
    }

fun MeasurementMode.getDescription(): String =
    when (this) {
        MeasurementMode.SPOT -> "Measure temperature at a single point"
        MeasurementMode.AREA -> "Measure average temperature in an area"
        MeasurementMode.LINE -> "Measure temperature along a line"
        MeasurementMode.CONTINUOUS -> "Continuously track temperature"
    }
