package mpdc4gsr.gsr.ui

import mpdc4gsr.gsr.model.RecorderKind

data class SessionConfiguration(
    val label: String = "GSR Session",
    val useGsr: Boolean = true,
    val useRgb: Boolean = true,
    val useIr: Boolean = true,
) {
    fun enabledModalities(): Set<RecorderKind> {
        val modalities = mutableSetOf<RecorderKind>()
        if (useGsr) modalities += RecorderKind.GSR
        if (useRgb) modalities += RecorderKind.RGB_VIDEO
        if (useIr) modalities += RecorderKind.THERMAL_VIDEO
        return modalities.ifEmpty { setOf(RecorderKind.GSR) }
    }

    fun ensureAtLeastOne(): SessionConfiguration {
        if (useGsr || useRgb || useIr) return this
        return copy(useGsr = true)
    }

    fun resolvedLabel(): String = label.ifBlank { "GSR Session" }
}
