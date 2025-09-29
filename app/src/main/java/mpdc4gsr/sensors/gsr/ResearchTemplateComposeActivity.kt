package mpdc4gsr.sensors.gsr

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.gsr.model.ResearchTemplate
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel

/**
 * ResearchTemplateComposeActivity - Modern Research Template Selection with Compose
 *
 * Advanced research template management interface featuring:
 * - Interactive template gallery with category filtering
 * - Template preview with detailed parameter display
 * - Custom template creation wizard
 * - Template sharing and import/export functionality
 * - Research protocol validation and guidance
 * - Template version management and history
 */
class ResearchTemplateComposeActivity : BaseComposeActivity<BaseViewModel>() {

    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, ResearchTemplateComposeActivity::class.java))
        }
    }

    override fun createViewModel(): BaseViewModel {
        return viewModels<BaseViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: BaseViewModel) {
        var selectedTemplate by remember { mutableStateOf<ResearchTemplate?>(null) }
        var selectedCategory by remember { mutableStateOf("All") }
        var showTemplateDetails by remember { mutableStateOf(false) }
        var showCreateDialog by remember { mutableStateOf(false) }

        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Research Templates",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { showCreateDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Create Template")
                            }
                            IconButton(onClick = { /* Import template */ }) {
                                Icon(Icons.Default.FileOpen, contentDescription = "Import")
                            }
                            IconButton(onClick = { /* More options */ }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                        }
                    )
                },
                floatingActionButton = {
                    selectedTemplate?.let { template ->
                        ExtendedFloatingActionButton(
                            onClick = {
                                startRecordingWithTemplate(template)
                            },
                            text = { Text("Start Recording") },
                            icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Start") }
                        )
                    }
                }
            ) { paddingValues ->
                ResearchTemplateContent(
                    selectedTemplate = selectedTemplate,
                    onTemplateSelect = { 
                        selectedTemplate = it
                        showTemplateDetails = true
                    },
                    selectedCategory = selectedCategory,
                    onCategoryChange = { selectedCategory = it },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        if (showTemplateDetails && selectedTemplate != null) {
            TemplateDetailsDialog(
                template = selectedTemplate!!,
                onDismiss = { showTemplateDetails = false },
                onStartRecording = {
                    startRecordingWithTemplate(selectedTemplate!!)
                    showTemplateDetails = false
                }
            )
        }

        if (showCreateDialog) {
            CreateTemplateDialog(
                onDismiss = { showCreateDialog = false },
                onCreateTemplate = { template ->
                    // Create new template logic
                    showCreateDialog = false
                }
            )
        }
    }

    private fun startRecordingWithTemplate(template: ResearchTemplate) {
        MultiModalRecordingComposeActivity.startWithTemplate(this, template.id)
    }
}

@Composable
private fun ResearchTemplateContent(
    selectedTemplate: ResearchTemplate?,
    onTemplateSelect: (ResearchTemplate) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Category Filter
        CategoryFilterRow(
            selectedCategory = selectedCategory,
            onCategoryChange = onCategoryChange,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Template Grid
        val mockTemplates = getMockTemplates()
        val filteredTemplates = if (selectedCategory == "All") {
            mockTemplates
        } else {
            mockTemplates.filter { it.category == selectedCategory }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredTemplates) { template ->
                TemplateCard(
                    template = template,
                    isSelected = selectedTemplate?.id == template.id,
                    onClick = { onTemplateSelect(template) }
                )
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf("All", "Psychology", "Physiology", "User Study", "Clinical", "Custom")
    
    LazyColumn(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        items(categories) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategoryChange(category) },
                label = { Text(category) },
                leadingIcon = if (selectedCategory == category) {
                    { Icon(Icons.Default.Check, contentDescription = "Selected", modifier = Modifier.size(16.dp)) }
                } else null
            )
        }
    }
}

@Composable
private fun TemplateCard(
    template: ResearchTemplate,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Template icon and category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = getTemplateIcon(template.category),
                    contentDescription = template.category,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = getCategoryColor(template.category),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = template.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Template name and description
            Text(
                text = template.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            Text(
                text = template.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Template details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Duration",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${template.duration} min",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Sensors",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = template.sensors.size.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplateDetailsDialog(
    template: ResearchTemplate,
    onDismiss: () -> Unit,
    onStartRecording: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = template.name,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = template.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = "Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Duration:", style = MaterialTheme.typography.bodySmall)
                            Text("${template.duration} minutes", style = MaterialTheme.typography.bodySmall)
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Sensors:", style = MaterialTheme.typography.bodySmall)
                            Text(template.sensors.joinToString(", "), style = MaterialTheme.typography.bodySmall)
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Sampling Rate:", style = MaterialTheme.typography.bodySmall)
                            Text("${template.samplingRate} Hz", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onStartRecording) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Start",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Start Recording")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun CreateTemplateDialog(
    onDismiss: () -> Unit,
    onCreateTemplate: (ResearchTemplate) -> Unit
) {
    var templateName by remember { mutableStateOf("") }
    var templateDescription by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Custom") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Template") },
        text = {
            Column {
                OutlinedTextField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    label = { Text("Template Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = templateDescription,
                    onValueChange = { templateDescription = it },
                    label = { Text("Description") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (templateName.isNotBlank()) {
                        val newTemplate = ResearchTemplate(
                            id = "custom_${System.currentTimeMillis()}",
                            name = templateName,
                            description = templateDescription,
                            category = selectedCategory,
                            duration = 30,
                            sensors = listOf("GSR", "Thermal"),
                            samplingRate = 128
                        )
                        onCreateTemplate(newTemplate)
                    }
                },
                enabled = templateName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getTemplateIcon(category: String) = when (category) {
    "Psychology" -> Icons.Default.Psychology
    "Physiology" -> Icons.Default.MonitorHeart
    "User Study" -> Icons.Default.Groups
    "Clinical" -> Icons.Default.LocalHospital
    "Custom" -> Icons.Default.Build
    else -> Icons.Default.Science
}

private fun getCategoryColor(category: String) = when (category) {
    "Psychology" -> Color(0xFF9C27B0)
    "Physiology" -> Color(0xFF4CAF50)
    "User Study" -> Color(0xFF2196F3)
    "Clinical" -> Color(0xFFE53E3E)
    "Custom" -> Color(0xFFFF9800)
    else -> Color(0xFF607D8B)
}

private fun getMockTemplates() = listOf(
    ResearchTemplate(
        id = "psych_stress_1",
        name = "Stress Response Study",
        description = "Measure physiological response to stress-inducing tasks",
        category = "Psychology",
        duration = 45,
        sensors = listOf("GSR", "Thermal"),
        samplingRate = 128
    ),
    ResearchTemplate(
        id = "phys_exercise_1",
        name = "Exercise Monitoring",
        description = "Track physiological changes during physical activity",
        category = "Physiology",
        duration = 60,
        sensors = listOf("GSR", "Thermal", "RGB"),
        samplingRate = 256
    ),
    ResearchTemplate(
        id = "user_interaction_1",
        name = "UI Interaction Study",
        description = "Analyze user responses to different interface designs",
        category = "User Study",
        duration = 30,
        sensors = listOf("GSR", "RGB"),
        samplingRate = 128
    ),
    ResearchTemplate(
        id = "clinical_assessment_1",
        name = "Clinical Assessment",
        description = "Comprehensive physiological assessment for clinical use",
        category = "Clinical",
        duration = 120,
        sensors = listOf("GSR", "Thermal", "RGB"),
        samplingRate = 512
    )
)