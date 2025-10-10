package mpdc4gsr.core.recording.session

import java.io.Serializable

data class SessionInfo(
    val sessionId: String,
    val startTime: Long,
    var endTime: Long? = null,
    var participantId: String? = null,
    var studyName: String? = null,
    var sampleCount: Long = 0,
    var totalDataSize: Long = 0,
    val metadata: MutableMap<String, String> = mutableMapOf(),
    val syncMarks: MutableList<SyncMark> = mutableListOf(),
    var hasGSRData: Boolean = false,
    var hasRGBData: Boolean = false,
    var hasThermalData: Boolean = false,
) : Serializable {
    fun isActive(): Boolean = endTime == null

    fun getDurationMs(): Long {
        val end = endTime ?: System.currentTimeMillis()
        return end - startTime
    }

    fun getDurationFormatted(): String {
        val durationMs = getDurationMs()
        val seconds = durationMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }

    fun addSyncMark(mark: SyncMark) {
        syncMarks.add(mark)
    }

    fun getDataTypeSummary(): String {
        val types = mutableListOf<String>()
        if (hasGSRData) types.add("GSR")
        if (hasRGBData) types.add("RGB")
        if (hasThermalData) types.add("Thermal")
        return if (types.isEmpty()) "No data" else types.joinToString(", ")
    }
}

data class SyncMark(
    val timestamp: Long,
    val utcTimestamp: Long,
    val eventType: String,
    val sessionId: String,
    val metadata: Map<String, String> = emptyMap(),
) {
    fun toCsvRow(): Array<String> {
        val metadataJson =
            if (metadata.isNotEmpty()) {
                metadata.entries.joinToString(";") { "${it.key}=${it.value}" }
            } else {
                ""
            }
        return arrayOf(
            timestamp.toString(),
            utcTimestamp.toString(),
            eventType,
            sessionId,
            metadataJson,
        )
    }
}

data class ResearchTemplate(
    val id: String,
    val name: String,
    val description: String,
    val category: TemplateCategory,
    val sensors: Set<SensorType>,
    val duration: Long? = null,
    val gsrSamplingRate: Int = 128,
    val videoResolution: VideoResolution = VideoResolution.FULL_HD,
    val videoFrameRate: Int = 30,
    val metadata: Map<String, String> = emptyMap(),
    val instructions: String? = null,
    val icon: String? = null,
) {
    enum class TemplateCategory {
        STRESS_RESPONSE,
        COGNITIVE_LOAD,
        EMOTION_RECOGNITION,
        PHYSIOLOGICAL_MONITORING,
        BEHAVIORAL_ANALYSIS,
        CUSTOM,
    }

    enum class SensorType {
        GSR,
        THERMAL_CAMERA,
        RGB_CAMERA,
    }

    enum class VideoResolution(
        val width: Int,
        val height: Int,
    ) {
        SD(720, 480),
        HD(1280, 720),
        FULL_HD(1920, 1080),
        UHD_4K(3840, 2160),
    }

    companion object {
        val PREDEFINED_TEMPLATES: List<ResearchTemplate> =
            listOf(
                ResearchTemplate(
                    id = "stress_response_basic",
                    name = "Stress Response - Basic",
                    description = "Basic stress response measurement with GSR and thermal imaging for physiological arousal detection",
                    category = TemplateCategory.STRESS_RESPONSE,
                    sensors = setOf(SensorType.GSR, SensorType.THERMAL_CAMERA),
                    duration = 10 * 60 * 1000L,
                    gsrSamplingRate = 128,
                    videoResolution = VideoResolution.HD,
                    metadata =
                        mapOf(
                            "study_type" to "stress_response",
                            "measurement_focus" to "physiological_arousal",
                        ),
                    instructions = "Place GSR sensors on participant's fingers. Position thermal camera to capture face. Begin baseline recording for 2 minutes before stress induction.",
                    icon = "",
                ),
                ResearchTemplate(
                    id = "stress_response_comprehensive",
                    name = "Stress Response - Comprehensive",
                    description = "Complete stress analysis with all sensors for comprehensive physiological and behavioral assessment",
                    category = TemplateCategory.STRESS_RESPONSE,
                    sensors =
                        setOf(
                            SensorType.GSR,
                            SensorType.THERMAL_CAMERA,
                            SensorType.RGB_CAMERA,
                        ),
                    duration = 20 * 60 * 1000L,
                    gsrSamplingRate = 128,
                    videoResolution = VideoResolution.FULL_HD,
                    videoFrameRate = 60,
                    metadata =
                        mapOf(
                            "study_type" to "comprehensive_stress",
                            "baseline_duration" to "300",
                            "stress_induction_duration" to "600",
                            "recovery_duration" to "300",
                        ),
                    instructions = "Multi-modal stress response study:\n1. Attach GSR sensors\n2. Position thermal and RGB cameras\n3. Record 5min baseline + 10min stress task + 5min recovery",
                    icon = "",
                ),
                ResearchTemplate(
                    id = "cognitive_load_mental_tasks",
                    name = "Cognitive Load - Mental Tasks",
                    description = "Cognitive workload assessment during mental tasks using GSR and thermal monitoring",
                    category = TemplateCategory.COGNITIVE_LOAD,
                    sensors = setOf(SensorType.GSR, SensorType.THERMAL_CAMERA),
                    duration = 15 * 60 * 1000L,
                    gsrSamplingRate = 128,
                    videoResolution = VideoResolution.HD,
                    metadata =
                        mapOf(
                            "study_type" to "cognitive_load",
                            "task_type" to "mental_arithmetic",
                            "difficulty_levels" to "3",
                        ),
                    instructions = "Measure cognitive load during mental tasks. Begin with 3min rest, then progressive difficulty tasks (easy+medium+hard). Monitor GSR changes and thermal patterns.",
                    icon = "",
                ),
                ResearchTemplate(
                    id = "cognitive_load_learning",
                    name = "Cognitive Load - Learning Assessment",
                    description = "Learning effectiveness measurement with comprehensive physiological monitoring",
                    category = TemplateCategory.COGNITIVE_LOAD,
                    sensors =
                        setOf(
                            SensorType.GSR,
                            SensorType.THERMAL_CAMERA,
                            SensorType.RGB_CAMERA,
                        ),
                    duration = 25 * 60 * 1000L,
                    gsrSamplingRate = 128,
                    videoResolution = VideoResolution.FULL_HD,
                    videoFrameRate = 60,
                    metadata =
                        mapOf(
                            "study_type" to "learning_assessment",
                            "phases" to "introduction_practice_evaluation",
                        ),
                    instructions = "Comprehensive learning assessment with multi-modal recordings. Capture baseline, training, and evaluation phases to analyse physiological engagement.",
                    icon = "",
                ),
                ResearchTemplate(
                    id = "emotion_recognition_multimodal",
                    name = "Emotion Recognition - Multi-Modal",
                    description = "Advanced emotion analysis combining facial expressions, thermal patterns, and GSR",
                    category = TemplateCategory.EMOTION_RECOGNITION,
                    sensors =
                        setOf(
                            SensorType.GSR,
                            SensorType.THERMAL_CAMERA,
                            SensorType.RGB_CAMERA,
                        ),
                    duration = 25 * 60 * 1000L,
                    gsrSamplingRate = 128,
                    videoResolution = VideoResolution.FULL_HD,
                    videoFrameRate = 60,
                    metadata =
                        mapOf(
                            "study_type" to "multimodal_emotion",
                            "modalities" to "facial_thermal_gsr",
                            "stimulus_categories" to "images_audio_video_social",
                        ),
                    instructions = "Comprehensive emotion recognition study:\n- RGB: facial expressions\n- Thermal: arousal patterns\n- GSR: autonomic responses\nPresent varied emotional stimuli and record multi-modal responses.",
                    icon = "",
                ),
                ResearchTemplate(
                    id = "physio_monitoring_baseline",
                    name = "Physiological Monitoring - Baseline",
                    description = "Continuous physiological monitoring for baseline establishment",
                    category = TemplateCategory.PHYSIOLOGICAL_MONITORING,
                    sensors = setOf(SensorType.GSR, SensorType.THERMAL_CAMERA),
                    duration = 60 * 60 * 1000L,
                    gsrSamplingRate = 128,
                    videoResolution = VideoResolution.HD,
                    metadata =
                        mapOf(
                            "study_type" to "baseline_monitoring",
                            "monitoring_duration" to "3600",
                            "activity_level" to "resting",
                        ),
                    instructions = "Long-term physiological baseline recording. Participant should remain in comfortable resting position. Monitor for consistent GSR patterns and thermal stability.",
                    icon = "",
                ),
                ResearchTemplate(
                    id = "behavioral_analysis_social",
                    name = "Behavioral Analysis - Social Interaction",
                    description = "Social behavior analysis with physiological arousal monitoring",
                    category = TemplateCategory.BEHAVIORAL_ANALYSIS,
                    sensors =
                        setOf(
                            SensorType.GSR,
                            SensorType.THERMAL_CAMERA,
                            SensorType.RGB_CAMERA,
                        ),
                    duration = null,
                    gsrSamplingRate = 128,
                    videoResolution = VideoResolution.FULL_HD,
                    videoFrameRate = 60,
                    metadata =
                        mapOf(
                            "study_type" to "social_behavior",
                            "interaction_type" to "dyadic_conversation",
                            "behavioral_measures" to "gaze_gesture_posture_arousal",
                        ),
                    instructions = "Social interaction study with multi-modal monitoring:\n- RGB: behavioral coding\n- Thermal: arousal detection\n- GSR: stress/engagement\nRecord natural conversation or structured interaction tasks.",
                    icon = "",
                ),
                ResearchTemplate(
                    id = "custom_template",
                    name = "Custom Research Template",
                    description = "Customizable template for specific research requirements",
                    category = TemplateCategory.CUSTOM,
                    sensors =
                        setOf(
                            SensorType.GSR,
                            SensorType.THERMAL_CAMERA,
                            SensorType.RGB_CAMERA,
                        ),
                    duration = null,
                    gsrSamplingRate = 128,
                    videoResolution = VideoResolution.FULL_HD,
                    metadata = mapOf("template_type" to "custom"),
                    instructions = "Configure sensors, duration, and parameters according to your specific research protocol.",
                    icon = "",
                ),
            )

        fun getTemplatesByCategory(category: TemplateCategory): List<ResearchTemplate> =
            PREDEFINED_TEMPLATES.filter { it.category == category }

        fun getTemplateById(id: String): ResearchTemplate? = PREDEFINED_TEMPLATES.find { it.id == id }

        fun getTemplatesWithSensor(sensorType: SensorType): List<ResearchTemplate> =
            PREDEFINED_TEMPLATES.filter { sensorType in it.sensors }
    }
}
