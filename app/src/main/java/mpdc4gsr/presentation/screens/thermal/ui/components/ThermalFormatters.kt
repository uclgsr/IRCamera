package mpdc4gsr.presentation.screens.thermal.components

import mpdc4gsr.feature.thermal.data.TemperatureUnit

fun formatTemperature(celsius: Float, unit: TemperatureUnit): String {
    return when (unit) {
        TemperatureUnit.CELSIUS -> String.format("%.1f°C", celsius)
        TemperatureUnit.FAHRENHEIT -> String.format("%.1f°F", celsiusToFahrenheit(celsius))
        TemperatureUnit.KELVIN -> String.format("%.1fK", celsiusToKelvin(celsius))
    }
}

fun celsiusToFahrenheit(celsius: Float): Float {
    return celsius * 9f / 5f + 32f
}

fun celsiusToKelvin(celsius: Float): Float {
    return celsius + 273.15f
}

fun fahrenheitToCelsius(fahrenheit: Float): Float {
    return (fahrenheit - 32f) * 5f / 9f
}

fun kelvinToCelsius(kelvin: Float): Float {
    return kelvin - 273.15f
}

fun formatDuration(milliseconds: Long): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    
    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60)
        minutes > 0 -> String.format("%d:%02d", minutes, seconds % 60)
        else -> String.format("0:%02d", seconds)
    }
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

fun formatFrameRate(fps: Double): String {
    return String.format("%.1f fps", fps)
}

fun formatResolution(width: Int, height: Int): String {
    return "${width}×${height}"
}
