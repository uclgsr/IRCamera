package com.mpdc4gsr.component.thermal.chart

import com.mpdc4gsr.component.shared.app.tools.UnitTools
import com.mpdc4gsr.component.shared.ui.formatter.IndexAxisValueFormatter

class YValueFormatter : IndexAxisValueFormatter() {
    override fun getFormattedValue(value: Float): String =
        try {
            String.format("%.1f", value)
            UnitTools.showC(value)
        } catch (e: Exception) {
            UnitTools.showC(value)
        }
}



