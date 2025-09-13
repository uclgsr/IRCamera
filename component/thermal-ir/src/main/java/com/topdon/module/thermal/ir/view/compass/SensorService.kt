package com.topdon.module.thermal.ir.view.compass

import android.content.Context
import android.hardware.SensorManager
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.andromeda.sense.compass.ICompass

// Maybe use the concept of a use case
// Ex. SensorPurpose.Background, SensorPurpose.Calibration, SensorPurpose.Diagnostics
// Using those, it can adjust settings to be more appropriate for the use case

/**
 * Sensor background service for thermal imaging operations.
 * Performs long-running thermal data processing tasks.
 */
class SensorService(ctx: Context) {
    private var context = ctx.applicationContext

    fun hasCompass(): Boolean {
        return Sensors.hasCompass(context)
    }

    fun getCompass(): ICompass {
        return CompassProvider(context).get()
    }

    companion object {
        const val MOTION_SENSOR_DELAY = SensorManager.SENSOR_DELAY_GAME
        const val ENVIRONMENT_SENSOR_DELAY = SensorManager.SENSOR_DELAY_NORMAL
    }
}
