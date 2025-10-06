package mpdc4gsr.feature.testing.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class TestCase(
    val id: String,
    val name: String,
    val description: String,
    val status: TestStatus = TestStatus.PENDING,
    val duration: Long = 0,
    val details: String = ""
)

enum class TestStatus {
    PENDING, RUNNING, PASSED, FAILED, SKIPPED
}

@Composable
fun TestResultCard(
    testCase: TestCase,
    modifier: Modifier = Modifier,
    onRunTest: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = testCase.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = testCase.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TestStatusIcon(status = testCase.status)
            }
            if (testCase.details.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = testCase.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (testCase.duration > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Duration: ${testCase.duration}ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (testCase.status == TestStatus.PENDING || testCase.status == TestStatus.FAILED) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onRunTest,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Run Test")
                }
            }
        }
    }
}

@Composable
fun TestStatusIcon(
    status: TestStatus,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when (status) {
        TestStatus.PENDING -> Icons.Default.Schedule to MaterialTheme.colorScheme.outline
        TestStatus.RUNNING -> Icons.Default.Refresh to MaterialTheme.colorScheme.primary
        TestStatus.PASSED -> Icons.Default.CheckCircle to Color(0xFF4CAF50)
        TestStatus.FAILED -> Icons.Default.Error to MaterialTheme.colorScheme.error
        TestStatus.SKIPPED -> Icons.Default.SkipNext to MaterialTheme.colorScheme.outline
    }
    Icon(
        imageVector = icon,
        contentDescription = status.name,
        tint = color,
        modifier = modifier.size(24.dp)
    )
}

@Composable
fun TestProgressIndicator(
    totalTests: Int,
    completedTests: Int,
    passedTests: Int,
    failedTests: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Test Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "$completedTests/$totalTests",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            val progress = if (totalTests > 0) completedTests.toFloat() / totalTests else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TestMetricChip(
                    label = "Passed",
                    count = passedTests,
                    color = Color(0xFF4CAF50)
                )
                TestMetricChip(
                    label = "Failed",
                    count = failedTests,
                    color = MaterialTheme.colorScheme.error
                )
                TestMetricChip(
                    label = "Remaining",
                    count = totalTests - completedTests,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun TestMetricChip(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    FilterChip(
        onClick = { },
        label = {
            Text(
                text = "$label: $count",
                style = MaterialTheme.typography.bodySmall
            )
        },
        selected = false,
        colors = FilterChipDefaults.filterChipColors(
            labelColor = color
        ),
        modifier = modifier
    )
}

@Composable
fun TestMetricsDisplay(
    metrics: Map<String, Any>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Test Metrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            metrics.forEach { (key, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = key,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}