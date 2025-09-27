package mpdc4gsr.activities

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import com.mpdc4gsr.libunified.app.ktbase.BaseComposeActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.ui.components.CommonComponents
import mpdc4gsr.ui.theme.IRCameraTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Compose version of a Diagnostics Activity demonstrating system information and health checks.
 * Shows how to handle system diagnostics, hardware info, and testing in Compose.
 */
class DiagnosticsComposeActivity : BaseComposeActivity() {

    @Composable
    override fun Content() {
        IRCameraTheme {
            DiagnosticsScreen()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun DiagnosticsScreen() {
        var isRunningTests by remember { mutableStateOf(false) }
        var testResults by remember { mutableStateOf<List<TestResult>>(emptyList()) }
        var systemInfo by remember { mutableStateOf(getSystemInfo()) }

        Scaffold(
            topBar = {
                CommonComponents.IRCameraTopAppBar(
                    title = "System Diagnostics",
                    onNavigationClick = { finish() }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF16131E))
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Quick Actions Section
                item {
                    CommonComponents.IRCameraCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Quick Actions",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        runSystemTests { results ->
                                            testResults = results
                                            isRunningTests = false
                                        }
                                        isRunningTests = true
                                    },
                                    enabled = !isRunningTests,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    if (isRunningTests) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White
                                        )
                                    } else {
                                        Text("Run Tests")
                                    }
                                }

                                Button(
                                    onClick = {
                                        systemInfo = getSystemInfo()
                                        Toast.makeText(this@DiagnosticsComposeActivity, "System info refreshed", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Text("Refresh")
                                }
                            }
                        }
                    }
                }

                // System Information Section
                item {
                    SectionHeader("System Information")
                }

                items(systemInfo) { info ->
                    InfoCard(
                        icon = info.icon,
                        title = info.title,
                        items = info.items
                    )
                }

                // Test Results Section
                if (testResults.isNotEmpty()) {
                    item {
                        SectionHeader("Test Results")
                    }

                    items(testResults) { result ->
                        TestResultCard(result)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    @Composable
    private fun SectionHeader(title: String) {
        Text(
            text = title,
            color = Color(0xFF6B35FF),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
        )
    }

    @Composable
    private fun InfoCard(
        icon: ImageVector,
        title: String,
        items: List<Pair<String, String>>
    ) {
        CommonComponents.IRCameraCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color(0xFF6B35FF),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                items.forEach { (label, value) ->
                    InfoRow(label, value)
                }
            }
        }
    }

    @Composable
    private fun TestResultCard(result: TestResult) {
        CommonComponents.IRCameraCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (result.passed) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = if (result.passed) "Passed" else "Failed",
                    tint = if (result.passed) Color.Green else Color.Red,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = result.description,
                        color = Color(0x80FFFFFF),
                        fontSize = 14.sp
                    )
                    if (result.details.isNotEmpty()) {
                        Text(
                            text = result.details,
                            color = Color(0x60FFFFFF),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Text(
                    text = "${result.duration}ms",
                    color = Color(0x80FFFFFF),
                    fontSize = 12.sp
                )
            }
        }
    }

    @Composable
    private fun InfoRow(label: String, value: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = Color(0x80FFFFFF),
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }
    }

    private fun getSystemInfo(): List<InfoSection> {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory() / (1024 * 1024)
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val freeMemory = runtime.freeMemory() / (1024 * 1024)

        return listOf(
            InfoSection(
                icon = Icons.Default.PhoneAndroid,
                title = "Device Information",
                items = listOf(
                    "Device" to "${Build.MANUFACTURER} ${Build.MODEL}",
                    "Android Version" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                    "Build" to Build.DISPLAY,
                    "Board" to Build.BOARD,
                    "Hardware" to Build.HARDWARE
                )
            ),
            InfoSection(
                icon = Icons.Default.Memory,
                title = "Memory Information",
                items = listOf(
                    "Max Memory" to "${maxMemory}MB",
                    "Used Memory" to "${usedMemory}MB",
                    "Free Memory" to "${freeMemory}MB",
                    "Available Processors" to "${Runtime.getRuntime().availableProcessors()}",
                    "Total Memory" to "${runtime.totalMemory() / (1024 * 1024)}MB"
                )
            ),
            InfoSection(
                icon = Icons.Default.Apps,
                title = "Application Information",
                items = listOf(
                    "Package Name" to packageName,
                    "Version" to "1.0.0",
                    "Build Type" to "Debug",
                    "Target SDK" to "34",
                    "Last Updated" to formatter.format(Date())
                )
            ),
            InfoSection(
                icon = Icons.Default.Settings,
                title = "System Status",
                items = listOf(
                    "Screen Density" to "${resources.displayMetrics.densityDpi} dpi",
                    "Screen Size" to "${resources.displayMetrics.widthPixels}x${resources.displayMetrics.heightPixels}",
                    "Language" to Locale.getDefault().displayLanguage,
                    "Time Zone" to java.util.TimeZone.getDefault().displayName,
                    "Uptime" to "${android.os.SystemClock.elapsedRealtime() / 1000}s"
                )
            )
        )
    }

    private fun runSystemTests(onComplete: (List<TestResult>) -> Unit) {
        lifecycleScope.launch {
            val tests = listOf(
                "Camera Access Test",
                "Storage Permission Test",
                "Bluetooth Connectivity Test",
                "Network Connection Test",
                "Sensor Availability Test",
                "Memory Allocation Test",
                "File System Test"
            )

            val results = mutableListOf<TestResult>()

            tests.forEach { testName ->
                delay(500) // Simulate test execution time
                
                val duration = (100..1000).random()
                val passed = (0..10).random() > 2 // 80% pass rate
                val description = if (passed) "Test completed successfully" else "Test failed with errors"
                val details = if (passed) "" else "Error code: ${(1000..9999).random()}"

                results.add(
                    TestResult(
                        name = testName,
                        passed = passed,
                        description = description,
                        details = details,
                        duration = duration
                    )
                )
            }

            onComplete(results)
            
            val passedCount = results.count { it.passed }
            val totalCount = results.size
            
            Toast.makeText(
                this@DiagnosticsComposeActivity,
                "Tests completed: $passedCount/$totalCount passed",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private data class InfoSection(
        val icon: ImageVector,
        val title: String,
        val items: List<Pair<String, String>>
    )

    private data class TestResult(
        val name: String,
        val passed: Boolean,
        val description: String,
        val details: String,
        val duration: Int
    )
}