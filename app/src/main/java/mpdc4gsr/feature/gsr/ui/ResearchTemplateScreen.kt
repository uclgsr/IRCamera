package mpdc4gsr.feature.gsr.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.TitleBarAction
import mpdc4gsr.core.ui.theme.IRCameraTheme
data class ResearchTemplate(
    val id: String,
    val title: String,
    val description: String,
    val duration: String,
    val sensorTypes: List<String>,
    val tasks: List<String>,
    val difficulty: TemplateDifficulty,
    val category: TemplateCategory,
    val isCustom: Boolean = false
)
enum class TemplateDifficulty {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED
}
enum class TemplateCategory {
    STRESS_RESPONSE,
    COGNITIVE_LOAD,
    EMOTION_RECOGNITION,
    PHYSIOLOGICAL_MONITORING,
    CUSTOM
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResearchTemplateScreen(
    onNavigateBack: () -> Unit = {},
    onCreateCustomTemplate: () -> Unit = {},
    onUseTemplate: (ResearchTemplate) -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf<TemplateCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val templates = remember { getSampleTemplates() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val filteredTemplates = templates.filter { template ->
        val matchesCategory = selectedCategory == null || template.category == selectedCategory
        val matchesSearch = if (searchQuery.isBlank()) true
        else template.title.contains(searchQuery, ignoreCase = true) ||
                template.description.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }
    IRCameraTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            TitleBar(
                title = "Research Templates",
                showBackButton = true,
                onBackClick = onNavigateBack
            ) {
                TitleBarAction(
                    icon = Icons.Default.Add,
                    contentDescription = "Create custom template",
                    onClick = onCreateCustomTemplate
                )
            }
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search templates...") },
                    leadingIcon = {
                        IconButton(onClick = { keyboardController?.hide() }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6B73FF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF6B73FF)
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                        }
                    )
                )
                // Category Filter Chips
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        CategoryFilterChips(
                            selectedCategory = selectedCategory,
                            onCategorySelected = { selectedCategory = it }
                        )
                    }
                    item {
                        TemplateStatsCard(templates = templates)
                    }
                    items(filteredTemplates) { template ->
                        TemplateItem(
                            template = template,
                            onUse = { onUseTemplate(template) }
                        )
                    }
                    if (filteredTemplates.isEmpty()) {
                        item {
                            EmptyTemplatesState(
                                searchQuery = searchQuery,
                                selectedCategory = selectedCategory,
                                onCreateCustom = onCreateCustomTemplate
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun CategoryFilterChips(
    selectedCategory: TemplateCategory?,
    onCategorySelected: (TemplateCategory?) -> Unit
) {
    Column {
        Text(
            text = "Categories",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // All Categories Chip
            FilterChip(
                onClick = { onCategorySelected(null) },
                label = { Text("All") },
                selected = selectedCategory == null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF6B73FF),
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF2A2A2A),
                    labelColor = Color.White
                )
            )
            // Category-specific chips (showing only a few due to space)
            listOf(
                TemplateCategory.STRESS_RESPONSE to "Stress",
                TemplateCategory.COGNITIVE_LOAD to "Cognitive",
                TemplateCategory.EMOTION_RECOGNITION to "Emotion"
            ).forEach { (category, label) ->
                FilterChip(
                    onClick = {
                        onCategorySelected(if (selectedCategory == category) null else category)
                    },
                    label = { Text(label) },
                    selected = selectedCategory == category,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF6B73FF),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF2A2A2A),
                        labelColor = Color.White
                    )
                )
            }
        }
    }
}
@Composable
fun TemplateStatsCard(templates: List<ResearchTemplate>) {
    val totalTemplates = templates.size
    val customTemplates = templates.count { it.isCustom }
    val avgDuration = templates.map { parseDuration(it.duration) }.average().toInt()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = "Templates",
                value = totalTemplates.toString(),
                color = Color(0xFF6B73FF)
            )
            StatItem(
                label = "Custom",
                value = customTemplates.toString(),
                color = Color(0xFF4ECDC4)
            )
            StatItem(
                label = "Avg Duration",
                value = "${avgDuration}min",
                color = Color(0xFFFF6B6B)
            )
        }
    }
}
@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFFCCFFFFFF)
        )
    }
}
@Composable
fun TemplateItem(
    template: ResearchTemplate,
    onUse: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = template.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DifficultyBadge(difficulty = template.difficulty)
                        Spacer(modifier = Modifier.width(8.dp))
                        CategoryBadge(category = template.category)
                        if (template.isCustom) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFF4ECDC4).copy(alpha = 0.2f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "Custom",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF4ECDC4),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                Button(
                    onClick = onUse,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6B73FF)
                    )
                ) {
                    Text("Use Template")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Description
            Text(
                text = template.description,
                fontSize = 14.sp,
                color = Color(0xFFCCFFFFFF),
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Details Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Duration and Sensors
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Duration",
                        tint = Color(0xFF4ECDC4),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = template.duration,
                        fontSize = 12.sp,
                        color = Color(0xFFCCFFFFFF)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = "Sensors",
                        tint = Color(0xFF4ECDC4),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${template.sensorTypes.size} sensors",
                        fontSize = 12.sp,
                        color = Color(0xFFCCFFFFFF)
                    )
                }
                // Sensor Type Icons
                Row {
                    template.sensorTypes.take(3).forEach { sensorType ->
                        Icon(
                            imageVector = when (sensorType) {
                                "GSR" -> Icons.Default.Sensors
                                "Thermal" -> Icons.Default.Thermostat
                                "Camera" -> Icons.Default.Camera
                                else -> Icons.Default.DeviceHub
                            },
                            contentDescription = sensorType,
                            tint = when (sensorType) {
                                "GSR" -> Color(0xFF4ECDC4)
                                "Thermal" -> Color(0xFFFF6B6B)
                                "Camera" -> Color.White
                                else -> Color(0xFF6B73FF)
                            },
                            modifier = Modifier
                                .size(20.dp)
                                .padding(horizontal = 2.dp)
                        )
                    }
                    if (template.sensorTypes.size > 3) {
                        Text(
                            text = "+${template.sensorTypes.size - 3}",
                            fontSize = 12.sp,
                            color = Color(0xFFCCFFFFFF),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
            // Tasks Preview
            if (template.tasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Tasks: ${
                        template.tasks.take(2).joinToString(", ")
                    }${if (template.tasks.size > 2) "..." else ""}",
                    fontSize = 12.sp,
                    color = Color(0xFFCCFFFFFF)
                )
            }
        }
    }
}
@Composable
fun DifficultyBadge(difficulty: TemplateDifficulty) {
    val (color, text) = when (difficulty) {
        TemplateDifficulty.BEGINNER -> Color(0xFF4ECDC4) to "Beginner"
        TemplateDifficulty.INTERMEDIATE -> Color(0xFFFFB74D) to "Intermediate"
        TemplateDifficulty.ADVANCED -> Color(0xFFFF6B6B) to "Advanced"
    }
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
@Composable
fun CategoryBadge(category: TemplateCategory) {
    val (color, text) = when (category) {
        TemplateCategory.STRESS_RESPONSE -> Color(0xFFFF6B6B) to "Stress"
        TemplateCategory.COGNITIVE_LOAD -> Color(0xFF6B73FF) to "Cognitive"
        TemplateCategory.EMOTION_RECOGNITION -> Color(0xFFFFB74D) to "Emotion"
        TemplateCategory.PHYSIOLOGICAL_MONITORING -> Color(0xFF4ECDC4) to "Physiology"
        TemplateCategory.CUSTOM -> Color(0xFF9E9E9E) to "Custom"
    }
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
@Composable
fun EmptyTemplatesState(
    searchQuery: String,
    selectedCategory: TemplateCategory?,
    onCreateCustom: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (searchQuery.isBlank() && selectedCategory == null)
                Icons.AutoMirrored.Filled.Assignment else Icons.Default.SearchOff,
            contentDescription = if (searchQuery.isBlank() && selectedCategory == null) "No Templates" else "No Search Results",
            tint = Color(0xFF6B73FF),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (searchQuery.isBlank() && selectedCategory == null)
                "No templates available" else "No templates found",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (searchQuery.isBlank() && selectedCategory == null)
                "Create your first custom template to get started"
            else
                "Try adjusting your search or category filter",
            fontSize = 14.sp,
            color = Color(0xFFCCFFFFFF)
        )
        if (searchQuery.isBlank() && selectedCategory == null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCreateCustom,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6B73FF)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Template",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Custom Template")
            }
        }
    }
}
private fun parseDuration(duration: String): Int {
    // Parse "25 min" format to minutes
    return duration.replace(" min", "").toIntOrNull() ?: 0
}
private fun getSampleTemplates() = listOf(
    ResearchTemplate(
        id = "TEMPLATE-001",
        title = "Basic Stress Response",
        description = "Measure physiological responses to cognitive stress tasks. Includes baseline recording, math problems, and recovery period.",
        duration = "20 min",
        sensorTypes = listOf("GSR", "Thermal"),
        tasks = listOf("Baseline (5min)", "Math problems", "Recovery (5min)"),
        difficulty = TemplateDifficulty.BEGINNER,
        category = TemplateCategory.STRESS_RESPONSE
    ),
    ResearchTemplate(
        id = "TEMPLATE-002",
        title = "Cognitive Load Assessment",
        description = "Advanced cognitive load measurement using multi-modal sensors during complex reasoning tasks.",
        duration = "35 min",
        sensorTypes = listOf("GSR", "Thermal", "Camera"),
        tasks = listOf("Training", "N-back task", "Stroop test", "Memory recall"),
        difficulty = TemplateDifficulty.ADVANCED,
        category = TemplateCategory.COGNITIVE_LOAD
    ),
    ResearchTemplate(
        id = "TEMPLATE-003",
        title = "Emotion Recognition Study",
        description = "Capture emotional responses using facial thermal imaging and GSR during video stimuli presentation.",
        duration = "25 min",
        sensorTypes = listOf("GSR", "Thermal", "Camera"),
        tasks = listOf("Baseline", "Happy videos", "Sad videos", "Neutral videos"),
        difficulty = TemplateDifficulty.INTERMEDIATE,
        category = TemplateCategory.EMOTION_RECOGNITION
    ),
    ResearchTemplate(
        id = "TEMPLATE-004",
        title = "Physiological Monitoring",
        description = "Continuous physiological monitoring during extended computer work sessions.",
        duration = "60 min",
        sensorTypes = listOf("GSR", "Thermal"),
        tasks = listOf("Computer work", "Break periods", "Final assessment"),
        difficulty = TemplateDifficulty.BEGINNER,
        category = TemplateCategory.PHYSIOLOGICAL_MONITORING
    ),
    ResearchTemplate(
        id = "CUSTOM-001",
        title = "My Custom Protocol",
        description = "Custom research protocol designed for specific study requirements.",
        duration = "30 min",
        sensorTypes = listOf("GSR", "Thermal", "Camera"),
        tasks = listOf("Custom task 1", "Custom task 2"),
        difficulty = TemplateDifficulty.INTERMEDIATE,
        category = TemplateCategory.CUSTOM,
        isCustom = true
    )
)
@Preview(showBackground = true)
@Composable
fun ResearchTemplateScreenPreview() {
    ResearchTemplateScreen()
}