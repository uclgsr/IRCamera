package mpdc4gsr.feature.thermal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
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

@Composable
fun ReportCreationScreen(
    imageUri: String? = null,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var reportTitle by remember { mutableStateOf("Thermal Analysis Report") }
    var description by remember { mutableStateOf("") }
    var observations by remember { mutableStateOf("") }
    var includeMetadata by remember { mutableStateOf(true) }
    var includeTemperatureData by remember { mutableStateOf(true) }
    var includeAnnotations by remember { mutableStateOf(true) }
    var reportFormat by remember { mutableStateOf(ReportFormat.PDF) }
    var currentStep by remember { mutableIntStateOf(0) }
    val steps = listOf("Basic Info", "Content", "Format", "Preview")
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "Create Report",
            showBackButton = true,
            onBackClick = onBackClick
        ) {
            val context = androidx.compose.ui.platform.LocalContext.current
            TitleBarAction(
                icon = Icons.Default.Save,
                contentDescription = "Save Draft",
                onClick = {
                    // TODO: Save report draft
                    android.widget.Toast.makeText(
                        context,
                        "Report draft saved",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Progress Indicator
            ReportProgressIndicator(
                currentStep = currentStep,
                steps = steps,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            // Step Content
            when (currentStep) {
                0 -> BasicInfoStep(
                    title = reportTitle,
                    onTitleChange = { reportTitle = it },
                    description = description,
                    onDescriptionChange = { description = it }
                )

                1 -> ContentStep(
                    observations = observations,
                    onObservationsChange = { observations = it },
                    includeMetadata = includeMetadata,
                    onMetadataChange = { includeMetadata = it },
                    includeTemperatureData = includeTemperatureData,
                    onTemperatureDataChange = { includeTemperatureData = it },
                    includeAnnotations = includeAnnotations,
                    onAnnotationsChange = { includeAnnotations = it }
                )

                2 -> FormatStep(
                    selectedFormat = reportFormat,
                    onFormatChange = { reportFormat = it }
                )

                3 -> PreviewStep(
                    title = reportTitle,
                    description = description,
                    observations = observations,
                    format = reportFormat
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            // Navigation Buttons
            val context = androidx.compose.ui.platform.LocalContext.current
            ReportNavigationButtons(
                currentStep = currentStep,
                totalSteps = steps.size,
                onPrevious = { if (currentStep > 0) currentStep-- },
                onNext = { if (currentStep < steps.size - 1) currentStep++ },
                onFinish = {
                    // TODO: Generate and export report
                    android.widget.Toast.makeText(
                        context,
                        "Generating report...",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }
}

@Composable
private fun ReportProgressIndicator(
    currentStep: Int,
    steps: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Step ${currentStep + 1} of ${steps.size}: ${steps[currentStep]}",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (currentStep + 1).toFloat() / steps.size },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Gray
            )
        }
    }
}

@Composable
private fun BasicInfoStep(
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Basic Information",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("Report Title") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = Color.Gray
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            // Focus moves to description field
                        }
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = Color.Gray
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                        }
                    )
                )
            }
        }
        // Metadata Card
        MetadataInfoCard()
    }
}

@Composable
private fun ContentStep(
    observations: String,
    onObservationsChange: (String) -> Unit,
    includeMetadata: Boolean,
    onMetadataChange: (Boolean) -> Unit,
    includeTemperatureData: Boolean,
    onTemperatureDataChange: (Boolean) -> Unit,
    includeAnnotations: Boolean,
    onAnnotationsChange: (Boolean) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Report Content",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = observations,
                    onValueChange = onObservationsChange,
                    label = { Text("Observations & Analysis") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    maxLines = 8,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = Color.Gray
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                        }
                    )
                )
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Include in Report",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                ContentToggleItem(
                    label = "Image Metadata",
                    description = "Include capture date, settings, and device info",
                    checked = includeMetadata,
                    onCheckedChange = onMetadataChange
                )
                ContentToggleItem(
                    label = "Temperature Data",
                    description = "Include temperature measurements and statistics",
                    checked = includeTemperatureData,
                    onCheckedChange = onTemperatureDataChange
                )
                ContentToggleItem(
                    label = "Annotations",
                    description = "Include all measurement points and areas",
                    checked = includeAnnotations,
                    onCheckedChange = onAnnotationsChange
                )
            }
        }
    }
}

@Composable
private fun FormatStep(
    selectedFormat: ReportFormat,
    onFormatChange: (ReportFormat) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Report Format",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            ReportFormat.entries.forEach { format ->
                ReportFormatOption(
                    format = format,
                    selected = selectedFormat == format,
                    onSelected = { onFormatChange(format) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun PreviewStep(
    title: String,
    description: String,
    observations: String,
    format: ReportFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Report Preview",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            // Report Preview Content
            Text(
                text = title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (description.isNotEmpty()) {
                Text(
                    text = description,
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            if (observations.isNotEmpty()) {
                Text(
                    text = "Observations:",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = observations,
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            Text(
                text = "Export Format: ${format.displayName}",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MetadataInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Image Information",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            val metadata = listOf(
                "Capture Date" to "2024-01-15 14:30:22",
                "Device" to "TOPDON TC001",
                "Resolution" to "256 × 192",
                "Temperature Range" to "-20°C to 120°C",
                "Emissivity" to "0.95"
            )
            metadata.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, color = Color.Gray, fontSize = 14.sp)
                    Text(value, color = Color.White, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun ContentToggleItem(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color.Gray,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun ReportFormatOption(
    format: ReportFormat,
    selected: Boolean,
    onSelected: () -> Unit
) {
    Card(
        onClick = onSelected,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent
        ),
        border = if (selected) CardDefaults.outlinedCardBorder() else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onSelected,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = Color.Gray
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = format.displayName,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = format.description,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun ReportNavigationButtons(
    currentStep: Int,
    totalSteps: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (currentStep > 0) {
            OutlinedButton(
                onClick = onPrevious,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Previous")
            }
        } else {
            Spacer(modifier = Modifier.width(1.dp))
        }
        if (currentStep < totalSteps - 1) {
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Next")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        } else {
            Button(
                onClick = onFinish,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate Report")
            }
        }
    }
}

enum class ReportFormat(val displayName: String, val description: String) {
    PDF("PDF Document", "Portable document format with images and text"),
    WORD("Word Document", "Microsoft Word document with editable content"),
    HTML("HTML Report", "Web-based report with interactive elements"),
    EXCEL("Excel Spreadsheet", "Data-focused report with temperature analysis")
}

@Preview(showBackground = true)
@Composable
private fun ReportCreationScreenPreview() {
    IRCameraTheme {
        ReportCreationScreen()
    }
}