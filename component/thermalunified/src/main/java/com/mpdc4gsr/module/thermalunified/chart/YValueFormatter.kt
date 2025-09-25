package com.mpdc4gsr.module.thermalunified.chart

import com.mpdc4gsr.libunified.app.tools.UnitTools
import com.mpdc4gsr.libunified.ui.formatter.IndexAxisValueFormatter

class YValueFormatter : IndexAxisValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        return try {
            String.format("%.1f", value)
            UnitTools.showC(value)
        } catch (e: Exception) {
            UnitTools.showC(value)
        }
    }
}
