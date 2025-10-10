package mpdc4gsr.feature.gsr.ui

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import dagger.hilt.android.AndroidEntryPoint
import mpdc4gsr.core.session.ResearchTemplate
import mpdc4gsr.core.ui.AppBaseViewModel

@AndroidEntryPoint
class ResearchTemplateComposeActivity : BaseComposeActivity<AppBaseViewModel>() {
    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, ResearchTemplateComposeActivity::class.java))
        }
    }

    override fun createViewModel(): AppBaseViewModel = viewModels<AppBaseViewModel>().value

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: AppBaseViewModel) {
        var selectedTemplate by remember { mutableStateOf<ResearchTemplate?>(null) }
        var selectedCategory by remember { mutableStateOf<ResearchTemplate.TemplateCategory?>(null) }
        var showTemplateDetails by remember { mutableStateOf(false) }
        var showCreateDialog by remember { mutableStateOf(false) }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Research Templates",
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { showCreateDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Create Template")
                            }
                            IconButton(onClick = {
                                // TODO: Import template from file
                                android.widget.Toast
                                    .makeText(
                                        this@ResearchTemplateComposeActivity,
                                        "Import template feature coming soon",
                                        android.widget.Toast.LENGTH_SHORT,
                                    ).show()
                            }) {
                                Icon(Icons.Default.FileOpen, contentDescription = "Import")
                            }
                            IconButton(onClick = {
                                // TODO: Show more options menu
                                android.widget.Toast
                                    .makeText(
                                        this@ResearchTemplateComposeActivity,
                                        "More options coming soon",
                                        android.widget.Toast.LENGTH_SHORT,
                                    ).show()
                            }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                        },
                    )
                },
                floatingActionButton = {
                    selectedTemplate?.let { template ->
                        ExtendedFloatingActionButton(
                            onClick = {
                                startRecordingWithTemplate(template)
                            },
                            text = { Text("Start Recording") },
                            icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Start") },
                        )
                    }
                },
            ) { paddingValues ->
                ResearchTemplateContent(
                    selectedTemplate = selectedTemplate,
                    onTemplateSelect = {
                        selectedTemplate = it
                        showTemplateDetails = true
                    },
                    selectedCategory = selectedCategory,
                    onCategoryChange = { selectedCategory = it },
                    modifier = Modifier.padding(paddingValues),
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
                },
            )
        }
        if (showCreateDialog) {
            CreateTemplateDialog(
                onDismiss = { showCreateDialog = false },
                onCreateTemplate = { template ->
                    // Create new template logic
                    showCreateDialog = false
                },
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
    selectedCategory: ResearchTemplate.TemplateCategory?,
    onCategoryChange: (ResearchTemplate.TemplateCategory?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        // Category Filter
        CategoryFilterRow(
            selectedCategory = selectedCategory,
            onCategoryChange = onCategoryChange,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        // Template Grid
        val templates = ResearchTemplate.PREDEFINED_TEMPLATES
        val filteredTemplates =
            if (selectedCategory == null) {
                templates
            } else {
                templates.filter { it.category == selectedCategory }
            }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(filteredTemplates) { template ->
                TemplateCard(
                    template = template,
                    isSelected = selectedTemplate?.id == template.id,
                    onClick = { onTemplateSelect(template) },
                )
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(
    selectedCategory: ResearchTemplate.TemplateCategory?,
    onCategoryChange: (ResearchTemplate.TemplateCategory?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val categories = listOf(null) + ResearchTemplate.TemplateCategory.values().toList()
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        categories.forEach { category ->
            val displayName =
                category
                    ?.name
                    ?.replace("_", " ")
                    ?.lowercase()
                    ?.replaceFirstChar { it.uppercase() } ?: "All"
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategoryChange(category) },
                label = { Text(displayName) },
                leadingIcon =
                    if (selectedCategory == category) {
                        { Icon(Icons.Default.Check, contentDescription = "Selected", modifier = Modifier.size(16.dp)) }
                    } else {
                        null
                    },
            )
        }
    }
}

@Composable
private fun TemplateCard(
    template: ResearchTemplate,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onClick() },
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = if (isSelected) 8.dp else 2.dp,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // Template icon and category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = getTemplateIcon(template.category),
                    contentDescription = template.category.name,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = getCategoryColor(template.category),
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text(
                        text = template.category.name.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
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
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(
                text = template.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            // Template details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Duration",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = template.duration?.let { "${it / (60 * 1000)} min" } ?: "Variable",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Sensors",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = template.sensors.size.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
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
    onStartRecording: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = template.name,
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = template.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                Text(
                    text = "Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Duration:", style = MaterialTheme.typography.bodySmall)
                            Text("${template.duration} minutes", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Sensors:", style = MaterialTheme.typography.bodySmall)
                            Text(template.sensors.joinToString(", "), style = MaterialTheme.typography.bodySmall)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("GSR Sampling Rate:", style = MaterialTheme.typography.bodySmall)
                            Text("${template.gsrSamplingRate} Hz", style = MaterialTheme.typography.bodySmall)
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
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Start Recording")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

@Composable
private fun CreateTemplateDialog(
    onDismiss: () -> Unit,
    onCreateTemplate: (ResearchTemplate) -> Unit,
) {
    var templateName by remember { mutableStateOf("") }
    var templateDescription by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Custom") }
    val keyboardController = LocalSoftwareKeyboardController.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Template") },
        text = {
            Column {
                OutlinedTextField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    label = { Text("Template Name") },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions =
                        KeyboardActions(
                            onNext = {
                                // Focus moves to description field
                            },
                        ),
                )
                OutlinedTextField(
                    value = templateDescription,
                    onValueChange = { templateDescription = it },
                    label = { Text("Description") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions =
                        KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                if (templateName.isNotBlank()) {
                                    val newTemplate =
                                        ResearchTemplate(
                                            id = "custom_${System.currentTimeMillis()}",
                                            name = templateName,
                                            description = templateDescription,
                                            category =
                                                enumValues<ResearchTemplate.TemplateCategory>().firstOrNull { it.name == selectedCategory }
                                                    ?: ResearchTemplate.TemplateCategory.CUSTOM,
                                            duration = 30,
                                            sensors =
                                                setOf(
                                                    ResearchTemplate.SensorType.GSR,
                                                    ResearchTemplate.SensorType.THERMAL_CAMERA,
                                                ),
                                            gsrSamplingRate = 128,
                                        )
                                    onCreateTemplate(newTemplate)
                                    onDismiss()
                                }
                            },
                        ),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (templateName.isNotBlank()) {
                        val newTemplate =
                            ResearchTemplate(
                                id = "custom_${System.currentTimeMillis()}",
                                name = templateName,
                                description = templateDescription,
                                category =
                                    enumValues<ResearchTemplate.TemplateCategory>().firstOrNull { it.name == selectedCategory }
                                        ?: ResearchTemplate.TemplateCategory.CUSTOM,
                                duration = 30,
                                sensors =
                                    setOf(
                                        ResearchTemplate.SensorType.GSR,
                                        ResearchTemplate.SensorType.THERMAL_CAMERA,
                                    ),
                                gsrSamplingRate = 128,
                            )
                        onCreateTemplate(newTemplate)
                    }
                },
                enabled = templateName.isNotBlank(),
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private fun getTemplateIcon(category: ResearchTemplate.TemplateCategory) =
    when (category) {
        ResearchTemplate.TemplateCategory.STRESS_RESPONSE -> Icons.Default.Psychology
        ResearchTemplate.TemplateCategory.COGNITIVE_LOAD -> Icons.Default.Psychology
        ResearchTemplate.TemplateCategory.EMOTION_RECOGNITION -> Icons.Default.Psychology
        ResearchTemplate.TemplateCategory.PHYSIOLOGICAL_MONITORING -> Icons.Default.MonitorHeart
        ResearchTemplate.TemplateCategory.BEHAVIORAL_ANALYSIS -> Icons.Default.Groups
        ResearchTemplate.TemplateCategory.CUSTOM -> Icons.Default.Build
    }

private fun getCategoryColor(category: ResearchTemplate.TemplateCategory) =
    when (category) {
        ResearchTemplate.TemplateCategory.STRESS_RESPONSE -> Color(0xFF9C27B0)
        ResearchTemplate.TemplateCategory.COGNITIVE_LOAD -> Color(0xFF2196F3)
        ResearchTemplate.TemplateCategory.EMOTION_RECOGNITION -> Color(0xFFE91E63)
        ResearchTemplate.TemplateCategory.PHYSIOLOGICAL_MONITORING -> Color(0xFF4CAF50)
        ResearchTemplate.TemplateCategory.BEHAVIORAL_ANALYSIS -> Color(0xFF00BCD4)
        ResearchTemplate.TemplateCategory.CUSTOM -> Color(0xFFFF9800)
    }
