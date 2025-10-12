package mpdc4gsr.gsr.ui

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.mpdc4gsr.component.shared.app.compose.theme.LibSharedTheme
import kotlinx.coroutines.flow.StateFlow
import mpdc4gsr.gsr.model.RecorderKind
import mpdc4gsr.gsr.model.SessionSnapshot
import mpdc4gsr.gsr.model.TelemetryState
import com.mpdc4gsr.component.shared.app.permissions.FeaturePermissionArea

@Composable
fun GsrApp(
    sessionState: StateFlow<SessionSnapshot?>,
    telemetryState: StateFlow<Map<String, TelemetryState>>,
    onStartSession: (SessionConfiguration) -> Unit,
    onStopSession: () -> Unit,
) {
    LibSharedTheme {
        val context = LocalContext.current
        val monitoredFeatures =
            remember {
                listOf(
                    FeaturePermissionArea.GSR_SENSORS,
                    FeaturePermissionArea.RGB_VIDEO,
                    FeaturePermissionArea.THERMAL_IR,
                    FeaturePermissionArea.NOTIFICATIONS,
                )
            }
        val requiredPermissions =
            remember(monitoredFeatures) {
                data class AggregatedPermission(
                    val id: String,
                    val permissions: MutableSet<String>,
                    val title: String,
                    val rationale: String,
                    val usedBy: MutableSet<FeaturePermissionArea>,
                )

                val aggregated = linkedMapOf<String, AggregatedPermission>()
                monitoredFeatures.forEach { feature ->
                    feature.groups.forEach { group ->
                        val runtimePermissions = group.permissionsForDevice()
                        if (runtimePermissions.isEmpty()) return@forEach
                        val entry =
                            aggregated.getOrPut(group.id) {
                                AggregatedPermission(
                                    id = group.id,
                                    permissions = linkedSetOf(),
                                    title = group.title,
                                    rationale = group.rationale,
                                    usedBy = linkedSetOf(),
                                )
                            }
                        entry.permissions.addAll(runtimePermissions)
                        entry.usedBy.add(feature)
                    }
                }
                aggregated.values.map { entry ->
                    RequiredPermission(
                        id = entry.id,
                        permissions = entry.permissions.toList(),
                        title = entry.title,
                        rationale = entry.rationale,
                        usedBy = entry.usedBy.toList(),
                    )
                }
            }

        var grantedPermissions by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
        var autoRequestLaunched by rememberSaveable { mutableStateOf(false) }
        val permissionLauncher =
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions(),
            ) { _ ->
                // Re-evaluate all groups after user interaction to stay in sync with system settings.
                grantedPermissions = computeGrantedPermissions(context, requiredPermissions)
            }

        LaunchedEffect(requiredPermissions, context) {
            grantedPermissions = computeGrantedPermissions(context, requiredPermissions)
        }
        LaunchedEffect(grantedPermissions, requiredPermissions, autoRequestLaunched) {
            if (!autoRequestLaunched) {
                val pendingPermissions =
                    requiredPermissions
                        .filter { grantedPermissions[it.id] != true }
                        .flatMap { it.permissions }
                        .distinct()
                        .toTypedArray()
                if (pendingPermissions.isNotEmpty()) {
                    autoRequestLaunched = true
                    permissionLauncher.launch(pendingPermissions)
                }
            }
        }

        val viewModel: GsrSessionViewModel = hiltViewModel()

        val navController = rememberNavController()
        val snapshot by sessionState.collectAsStateWithLifecycle()
        val telemetry by telemetryState.collectAsStateWithLifecycle()
        val sessionConfiguration by viewModel.sessionConfiguration.collectAsStateWithLifecycle()
        val onboardingCompleted by viewModel.onboardingCompleted.collectAsStateWithLifecycle()

        val allPermissionsGranted =
            requiredPermissions.all { grantedPermissions[it.id] == true } &&
                grantedPermissions.size == requiredPermissions.size

        LaunchedEffect(allPermissionsGranted, onboardingCompleted) {
            if (allPermissionsGranted && onboardingCompleted) {
                navController.navigate(RootDestination.Main.route) {
                    popUpTo(RootDestination.Welcome.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }

        NavHost(navController = navController, startDestination = RootDestination.Welcome.route) {
            composable(RootDestination.Welcome.route) {
                WelcomeScreen(
                    permissions = requiredPermissions,
                    grantedPermissions = grantedPermissions,
                    onRequestPermission = { permission ->
                        permissionLauncher.launch(permission.permissions.toTypedArray())
                    },
                    onContinue = {
                        viewModel.completeOnboarding()
                        navController.navigate(RootDestination.Main.route) {
                            popUpTo(RootDestination.Welcome.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    isContinueEnabled = allPermissionsGranted,
                )
            }
            composable(RootDestination.Main.route) {
                MainShell(
                    snapshot = snapshot,
                    telemetry = telemetry,
                    configuration = sessionConfiguration,
                    onSessionLabelChange = { viewModel.setSessionLabel(it) },
                    onSensorToggle = { kind, enabled -> viewModel.setSensorEnabled(kind, enabled) },
                    onStartSession = onStartSession,
                    onStopSession = onStopSession,
                    onSensorSelected = { sensor ->
                        navController.navigate(RootDestination.SensorPreview.buildRoute(sensor)) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(
                route = RootDestination.SensorPreview.routePattern,
                arguments =
                    listOf(
                        navArgument(RootDestination.SensorPreview.ARG_KIND) {
                            type = NavType.StringType
                        },
                    ),
            ) { entry ->
                val kindArg =
                    entry.arguments?.getString(RootDestination.SensorPreview.ARG_KIND)
                val kind =
                    kindArg
                        ?.let { runCatching { RecorderKind.valueOf(it) }.getOrNull() }
                SensorPreviewScreen(
                    kind = kind,
                    snapshot = snapshot,
                    onClose = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun computeGrantedPermissions(
    context: Context,
    permissions: List<RequiredPermission>,
): Map<String, Boolean> =
    permissions.associate { permission ->
        permission.id to permission.isGranted(context)
    }

private fun RequiredPermission.isGranted(context: Context): Boolean =
    permissions.all { perm -> isPermissionGranted(context, perm) }

@Composable
private fun MainShell(
    snapshot: SessionSnapshot?,
    telemetry: Map<String, TelemetryState>,
    configuration: SessionConfiguration,
    onSessionLabelChange: (String) -> Unit,
    onSensorToggle: (RecorderKind, Boolean) -> Unit,
    onStartSession: (SessionConfiguration) -> Unit,
    onStopSession: () -> Unit,
    onSensorSelected: (RecorderKind) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "Active session", style = MaterialTheme.typography.titleLarge)
        Text(
            text =
                snapshot?.let { "Session ${it.sessionId} • ${if (it.isRecording) "Recording" else "Idle"}" }
                    ?: "No session started",
            style = MaterialTheme.typography.bodyLarge,
        )
        OutlinedTextField(
            value = configuration.label,
            onValueChange = onSessionLabelChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Session label") },
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Modalities", style = MaterialTheme.typography.titleMedium)
            SensorToggleRow(
                label = "GSR sensor",
                enabled = configuration.useGsr,
                onToggle = { onSensorToggle(RecorderKind.GSR, it) },
            )
            SensorToggleRow(
                label = "RGB video",
                enabled = configuration.useRgb,
                onToggle = { onSensorToggle(RecorderKind.RGB_VIDEO, it) },
            )
            SensorToggleRow(
                label = "Thermal video",
                enabled = configuration.useIr,
                onToggle = { onSensorToggle(RecorderKind.THERMAL_VIDEO, it) },
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { onStartSession(configuration.ensureAtLeastOne()) },
                enabled = !snapshot?.isRecording.orFalse(),
            ) {
                Text("Start session")
            }
            OutlinedButton(
                onClick = onStopSession,
                enabled = snapshot?.isRecording == true,
            ) {
                Text("Stop session")
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Sensors", style = MaterialTheme.typography.titleMedium)
            RecorderKind.values().forEach { kind ->
                OutlinedButton(onClick = { onSensorSelected(kind) }) {
                    Text("Preview ${kind.name}")
                }
            }
        }

        if (telemetry.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Telemetry", style = MaterialTheme.typography.titleMedium)
                telemetry.forEach { (deviceId, state) ->
                    Text(
                        text =
                            buildString {
                                append("$deviceId • ")
                                state.gsrMicrosiemens?.let { append("GSR ${"%.2f".format(it)}µS ") }
                                state.skinTemperatureCelsius?.let { append("Skin ${"%.1f".format(it)}°C ") }
                                state.frameRate?.let { append("FPS ${"%.1f".format(it)}") }
                            }.trim(),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun SensorToggleRow(
    label: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SensorPreviewScreen(
    kind: RecorderKind?,
    snapshot: SessionSnapshot?,
    onClose: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(kind?.name ?: "Sensor preview") },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text =
                    when (kind) {
                        null -> "Unknown sensor"
                        else -> "Live preview for ${kind.name}"
                    },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text =
                    snapshot?.let {
                        "Active recorders: ${
                            it.recorderStates.entries.joinToString { entry ->
                                "${entry.key}:${entry.value}"
                            }
                        }"
                    } ?: "No active session",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onClose) {
                Text("Close")
            }
        }
    }
}

private fun isPermissionGranted(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private data class RequiredPermission(
    val id: String,
    val permissions: List<String>,
    val title: String,
    val rationale: String,
    val usedBy: List<FeaturePermissionArea>,
)

private sealed class RootDestination(val route: String) {
    object Welcome : RootDestination("welcome")
    object Main : RootDestination("main")

    object SensorPreview : RootDestination("sensor_preview") {
        const val ARG_KIND = "kind"
        const val routePattern = "sensor_preview/{$ARG_KIND}"

        fun buildRoute(kind: RecorderKind) = "sensor_preview/${kind.name}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WelcomeScreen(
    permissions: List<RequiredPermission>,
    grantedPermissions: Map<String, Boolean>,
    onRequestPermission: (RequiredPermission) -> Unit,
    onContinue: () -> Unit,
    isContinueEnabled: Boolean,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Welcome to GSR Capture") })
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Let's get you ready",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text =
                        "Grant the permissions below so we can connect to the hardware and start " +
                            "collecting synchronized data streams.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            permissions.forEach { item ->
                val granted = grantedPermissions[item.id] == true
                PermissionCard(
                    item = item,
                    granted = granted,
                    onRequest = onRequestPermission,
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Why we need this",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text =
                            "Permissions are only used while capturing sessions. You can review " +
                                "and change them anytime from Android settings.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                enabled = isContinueEnabled,
            ) {
                Text("Continue")
            }
        }
    }
}

@Composable
private fun PermissionCard(
    item: RequiredPermission,
    granted: Boolean,
    onRequest: (RequiredPermission) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = item.title, style = MaterialTheme.typography.titleMedium)
            Text(text = item.rationale, style = MaterialTheme.typography.bodyMedium)
            if (item.usedBy.isNotEmpty()) {
                Text(
                    text = "Used for: ${item.usedBy.joinToString { it.title }}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                )
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (granted) {
                    RowWithIcon(icon = Icons.Filled.CheckCircle, text = "Granted")
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RowWithIcon(icon = Icons.Filled.Warning, text = "Action required")
                        OutlinedButton(onClick = { onRequest(item) }) {
                            Text("Grant permission")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowWithIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
        )
        Text(text)
    }
}

private fun Boolean?.orFalse(): Boolean = this ?: false
