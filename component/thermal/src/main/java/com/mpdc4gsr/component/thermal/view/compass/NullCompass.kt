package com.mpdc4gsr.component.thermal.view.compass

import com.kylecorry.andromeda.sense.compass.ICompass
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.CompassDirection

class NullCompass :
    NullSensor(),
    ICompass {
    override val bearing: Bearing = Bearing.from(CompassDirection.North)
    override var declination: Float = 0f
    override val rawBearing: Float = 0f
}

