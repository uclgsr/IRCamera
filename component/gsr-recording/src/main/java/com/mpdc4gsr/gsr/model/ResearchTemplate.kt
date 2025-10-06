package com.mpdc4gsr.gsr.model

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

    enum class VideoResolution(val width: Int, val height: Int) {
        SD(720, 480),
        HD(1280, 720),
        FULL_HD(1920, 1080),
        UHD_4K(3840, 2160),
    }

    companion object {
        val PREDEFINED_TEMPLATES =
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
                    sensors = setOf(
                        SensorType.GSR,
                        SensorType.THERMAL_CAMERA,
                        SensorType.RGB_CAMERA
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
                    instructions = "Multi-modal stress response study:\n1. Attach GSR sensors\n2. Position thermal and RGB cameras\n3. Record 5min baseline → 10min stress task → 5min recovery",
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
                    instructions = "Measure cognitive load during mental tasks. Begin with 3min rest, then progressive difficulty tasks (easy→medium→hard). Monitor GSR changes and thermal patterns.",
                    icon = "",
                ),
                ResearchTemplate(
                    id = "cognitive_load_learning",
                    name = "Cognitive Load - Learning Assessment",
                    description = "Learning effectiveness measurement with comprehensive physiological monitoring",
                    category = TemplateCategory.COGNITIVE_LOAD,
                    sensors = setOf(
                        SensorType.GSR,
                        SensorType.THERMAL_CAMERA,
                        SensorType.RGB_CAMERA
                    ),
                    duration = 30 * 60 * 1000L,
                    gsrSamplingRate = 128,
                    videoResolution = VideoResolution.FULL_HD,
                    metadata =
                        mapOf(
                            "study_type" to "learning_assessment",
                            "session_structure" to "instruction_practice_test",
                        ),
                    instructions = "Learning session with physiological monitoring:\n1. 10min instruction phase\n2. 15min practice phase\n3. 5min assessment phase\nMonitor engagement and cognitive load throughout.",
                    icon = "",
                ),
                ResearchTemplate(
                    id = "emotion_recognition_basic",
                    name = "Emotion Recognition - Basic",
                    description = "Emotion detection using thermal face imaging and GSR responses",
                    category = TemplateCategory.EMOTION_RECOGNITION,
                    sensors = setOf(SensorType.GSR, SensorType.THERMAL_CAMERA),
                    duration = 12 * 60 * 1000L,
                    gsrSamplingRate = 128,
                    videoResolution = VideoResolution.FULL_HD,
                    videoFrameRate = 30,
                    metadata =
                        mapOf(
                            "study_type" to "emotion_recognition",
                            "stimulus_type" to "images_videos",
                            "emotions_targeted" to "joy_fear_anger_sadness_neutral",
                        ),
                    instructions = "Present emotional stimuli while recording physiological responses. Ensure thermal camera captures full face area. Record GSR baseline before each stimulus presentation.",
                    icon = "",
                ),
                ResearchTemplate(
                    id = "emotion_recognition_multimodal",
                    name = "Emotion Recognition - Multi-Modal",
                    description = "Advanced emotion analysis combining facial expressions, thermal patterns, and GSR",
                    category = TemplateCategory.EMOTION_RECOGNITION,
                    sensors = setOf(
                        SensorType.GSR,
                        SensorType.THERMAL_CAMERA,
                        SensorType.RGB_CAMERA
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
                    sensors = setOf(
                        SensorType.GSR,
                        SensorType.THERMAL_CAMERA,
                        SensorType.RGB_CAMERA
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
                    sensors = setOf(
                        SensorType.GSR,
                        SensorType.THERMAL_CAMERA,
                        SensorType.RGB_CAMERA
                    ),
                    duration = null,
                    gsrSamplingRate = 128,
                    videoResolution = VideoResolution.FULL_HD,
                    metadata = mapOf("template_type" to "custom"),
                    instructions = "Configure sensors, duration, and parameters according to your specific research protocol.",
                    icon = "",
                ),
            )

        fun getTemplatesByCategory(category: TemplateCategory): List<ResearchTemplate> {
            return PREDEFINED_TEMPLATES.filter { it.category == category }
        }

        fun getTemplateById(id: String): ResearchTemplate? {
            return PREDEFINED_TEMPLATES.find { it.id == id }
        }

        fun getTemplatesWithSensor(sensorType: SensorType): List<ResearchTemplate> {
            return PREDEFINED_TEMPLATES.filter { it.sensors.contains(sensorType) }
        }
    }
}
