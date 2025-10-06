package mpdc4gsr.feature.testing.ui
import android.os.Bundle
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import kotlinx.coroutines.launch

class ComposeTestingSuiteActivity : ComponentActivity() {
    companion object {
        private const val TAG = "ComposeTestingSuiteActivity"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LibUnifiedTheme {
                ComposeTestingSuiteScreen()
            }
        }
    }
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ComposeTestingSuiteScreen() {
        var testResults by remember { mutableStateOf<List<TestResult>>(emptyList()) }
        var isTestRunning by remember { mutableStateOf(false) }
        var testProgress by remember { mutableStateOf(0f) }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Comprehensive Test Suite") },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Test Control Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Comprehensive Testing Suite",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Run complete validation across all system components",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        if (isTestRunning) {
                            LinearProgressIndicator(
                                progress = { testProgress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Running tests... ${(testProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                lifecycleScope.launch {
                                    runComprehensiveTests { progress, results ->
                                        testProgress = progress
                                        testResults = results
                                        isTestRunning = progress < 1f
                                    }
                                }
                            },
                            enabled = !isTestRunning,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isTestRunning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(if (isTestRunning) "Running..." else "Start Comprehensive Tests")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Test Results
                if (testResults.isNotEmpty()) {
                    Text(
                        text = "Test Results (${testResults.size} tests)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.height(400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(testResults) { result ->
                            TestResultCard(
                                result = result,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
    private suspend fun runComprehensiveTests(
        onProgress: (Float, List<TestResult>) -> Unit
    ) {
        AppLogger.i(TAG, "Starting comprehensive testing suite")
        try {
            val testingSuite = ComposeTestingSuite()
            val results = mutableListOf<TestResult>()
            // Simulate progressive testing with updates
            onProgress(0.1f, results)
            val finalResults = testingSuite.runAllTests()
            results.addAll(finalResults)
            onProgress(1f, results)
            AppLogger.i(TAG, "Comprehensive tests completed: ${results.size} tests executed")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Comprehensive tests failed: ${e.message}")
            // Add error result
            val errorResult = TestResult(
                testName = "Test Suite Error",
                passed = false,
                executionTimeMs = 0,
                details = "Test suite failed: ${e.message}",
                severity = TestSeverity.ERROR
            )
            onProgress(1f, listOf(errorResult))
        }
    }
}