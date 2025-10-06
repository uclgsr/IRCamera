package com.mpdc4gsr.module.thermalunified.activity
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.viewmodel.ThermalViewModel
class ReportPreviewComposeActivity : BaseComposeActivity<ThermalViewModel>() {
    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Report Preview",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                // TODO: Implement print functionality
                                android.widget.Toast.makeText(
                                    this@ReportPreviewComposeActivity,
                                    "Print report feature coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.Print, contentDescription = "Print", tint = Color.White)
                            }
                            IconButton(onClick = {
                                // TODO: Implement share functionality
                                android.widget.Toast.makeText(
                                    this@ReportPreviewComposeActivity,
                                    "Share report feature coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF0D1117)
                        )
                    )
                }
            ) { paddingValues ->
                ReportPreviewContent(
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
    @Composable
    private fun ReportPreviewContent(
        modifier: Modifier = Modifier
    ) {
        var currentPage by remember { mutableStateOf(1) }
        var totalPages by remember { mutableStateOf(5) }
        var showNavigationBookmarks by remember { mutableStateOf(false) }
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0D1117))
        ) {
            // Navigation and Page Controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Page Navigation
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { if (currentPage > 1) currentPage-- },
                            enabled = currentPage > 1
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.NavigateBefore,
                                contentDescription = "Previous",
                                tint = if (currentPage > 1) Color.White else Color(0xFF7D8590)
                            )
                        }
                        Text(
                            "Page $currentPage of $totalPages",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        IconButton(
                            onClick = { if (currentPage < totalPages) currentPage++ },
                            enabled = currentPage < totalPages
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.NavigateNext,
                                contentDescription = "Next",
                                tint = if (currentPage < totalPages) Color.White else Color(0xFF7D8590)
                            )
                        }
                    }
                    // Quick Navigation
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = { showNavigationBookmarks = !showNavigationBookmarks }) {
                            Icon(
                                Icons.Default.Bookmarks,
                                contentDescription = "Bookmarks",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = {  }) {
                            Icon(
                                Icons.Default.FitScreen,
                                contentDescription = "Fit to screen",
                                tint = Color.White
                            )
                        }
                    }
                }
                // Navigation Bookmarks
                if (showNavigationBookmarks) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        BookmarkItem("Executive Summary", 1) { currentPage = 1 }
                        BookmarkItem("Thermal Analysis", 2) { currentPage = 2 }
                        BookmarkItem("Temperature Data", 3) { currentPage = 3 }
                        BookmarkItem("Conclusions", 4) { currentPage = 4 }
                        BookmarkItem("Appendix", 5) { currentPage = 5 }
                    }
                }
            }
            // Report Content Display
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (currentPage) {
                        1 -> ExecutiveSummaryPage()
                        2 -> ThermalAnalysisPage()
                        3 -> TemperatureDataPage()
                        4 -> ConclusionsPage()
                        5 -> AppendixPage()
                    }
                }
            }
            // Format Options
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Export Format",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FormatButton("PDF", Icons.Default.PictureAsPdf, true)
                        FormatButton("Word", Icons.Default.Description, false)
                        FormatButton("HTML", Icons.Default.Language, false)
                        FormatButton("Print", Icons.Default.Print, false)
                    }
                }
            }
        }
    }
    @Composable
    private fun BookmarkItem(
        title: String,
        pageNumber: Int,
        onClick: () -> Unit
    ) {
        TextButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    title,
                    color = Color(0xFF7D8590),
                    fontSize = 14.sp
                )
                Text(
                    "Page $pageNumber",
                    color = Color(0xFF7D8590),
                    fontSize = 12.sp
                )
            }
        }
    }
    @Composable
    private fun RowScope.FormatButton(
        label: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        selected: Boolean,
        onClick: () -> Unit = {}
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selected) Color(0xFFFF6B35) else Color(0xFF161B22),
                contentColor = if (selected) Color.White else Color(0xFF7D8590)
            )
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                label,
                fontSize = 12.sp
            )
        }
    }
    @Composable
    private fun ExecutiveSummaryPage() {
        Text(
            "Executive Summary",
            color = Color.Black,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "This thermal analysis report presents comprehensive findings from thermal imaging inspection conducted on January 15, 2024. The inspection covered critical infrastructure components and identified several areas requiring attention.",
            color = Color.Black,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Key Findings:",
            color = Color.Black,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        BulletPoint("Temperature anomalies detected in sectors 3 and 7")
        BulletPoint("Average operating temperature: 28.7°C")
        BulletPoint("Peak temperature recorded: 42.1°C")
        BulletPoint("No critical temperature violations observed")
    }
    @Composable
    private fun ThermalAnalysisPage() {
        Text(
            "Thermal Analysis",
            color = Color.Black,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Detailed thermal analysis of captured data reveals normal operating conditions with isolated temperature variations within acceptable ranges.",
            color = Color.Black,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
    @Composable
    private fun TemperatureDataPage() {
        Text(
            "Temperature Data",
            color = Color.Black,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Comprehensive temperature measurements and statistical analysis of thermal imaging data.",
            color = Color.Black,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
    @Composable
    private fun ConclusionsPage() {
        Text(
            "Conclusions",
            color = Color.Black,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Based on the thermal analysis results, all systems are operating within normal parameters with no immediate action required.",
            color = Color.Black,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
    @Composable
    private fun AppendixPage() {
        Text(
            "Appendix",
            color = Color.Black,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Additional technical specifications, calibration data, and supporting documentation.",
            color = Color.Black,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
    @Composable
    private fun BulletPoint(text: String) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(vertical = 2.dp)
        ) {
            Text(
                "• ",
                color = Color.Black,
                fontSize = 14.sp
            )
            Text(
                text,
                color = Color.Black,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}