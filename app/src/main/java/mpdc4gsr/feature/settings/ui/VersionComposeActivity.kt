package mpdc4gsr.feature.settings.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.csl.irCamera.BuildConfig
import com.csl.irCamera.R
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.utils.UnifiedVersionUtils
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.theme.IRCameraTheme
import java.util.*

class VersionViewModel : AppBaseViewModel() {
    companion object {
        private const val DEFAULT_VERSION = "1.0.0"
    }

    data class VersionInfo(
        val appVersion: String,
        val buildCode: String,
        val buildTime: String,
        val unifiedVersion: String,
        val thermalVersion: String,
        val gsrVersion: String
    )

    private val _versionInfo = mutableStateOf(
        VersionInfo(
            appVersion = BuildConfig.VERSION_NAME,
            buildCode = BuildConfig.VERSION_CODE.toString(),
            buildTime = BuildConfig.BUILD_TYPE,
            unifiedVersion = DEFAULT_VERSION,
            thermalVersion = DEFAULT_VERSION,
            gsrVersion = DEFAULT_VERSION
        )
    )
    val versionInfo: State<VersionInfo> = _versionInfo
    fun updateVersionInfo(context: android.content.Context) {
        _versionInfo.value = _versionInfo.value.copy(
            unifiedVersion = UnifiedVersionUtils.getVersionName(context)
        )
    }
}

class VersionComposeActivity : BaseComposeActivity<VersionViewModel>() {
    override fun createViewModel(): VersionViewModel = viewModels<VersionViewModel>().value
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModels<VersionViewModel>().value.updateVersionInfo(this)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: VersionViewModel) {
        IRCameraTheme {
            val context = LocalContext.current
            val versionInfo by viewModel.versionInfo
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF16131E))
            ) {
                TitleBar(
                    title = stringResource(R.string.version_info),
                    onBackClick = { finish() }
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // App Logo and Name
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Thermal & GSR Data Collection Platform",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    // Version Information
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Version Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            VersionInfoRow(
                                label = "App Version",
                                value = versionInfo.appVersion,
                                icon = Icons.Default.Apps
                            )
                            VersionInfoRow(
                                label = "Build Code",
                                value = versionInfo.buildCode,
                                icon = Icons.Default.Build
                            )
                            VersionInfoRow(
                                label = "Build Type",
                                value = versionInfo.buildTime,
                                icon = Icons.Default.Engineering
                            )
                            VersionInfoRow(
                                label = "Unified Module",
                                value = versionInfo.unifiedVersion,
                                icon = Icons.Default.IntegrationInstructions
                            )
                        }
                    }
                    // Module Versions
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Module Versions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            VersionInfoRow(
                                label = "Thermal Camera",
                                value = versionInfo.thermalVersion,
                                icon = Icons.Default.Thermostat
                            )
                            VersionInfoRow(
                                label = "GSR Sensor",
                                value = versionInfo.gsrVersion,
                                icon = Icons.Default.Sensors
                            )
                        }
                    }
                    // System Information
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "System Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            VersionInfoRow(
                                label = "Android Version",
                                value = android.os.Build.VERSION.RELEASE,
                                icon = Icons.Default.Android
                            )
                            VersionInfoRow(
                                label = "API Level",
                                value = android.os.Build.VERSION.SDK_INT.toString(),
                                icon = Icons.Default.Api
                            )
                            VersionInfoRow(
                                label = "Device Model",
                                value = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
                                icon = Icons.Default.PhoneAndroid
                            )
                        }
                    }
                    // Copyright and Legal
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Copyright © ${Calendar.getInstance().get(Calendar.YEAR)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "MPDC4GSR - Multi-Modal Data Collection Platform",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        // Navigate to privacy policy
                                        val intent =
                                            Intent(context, PolicyComposeActivity::class.java).apply {
                                                putExtra(PolicyComposeActivity.KEY_THEME_TYPE, 2)
                                            }
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Privacy Policy")
                                }
                                OutlinedButton(
                                    onClick = {
                                        // Navigate to terms
                                        val intent =
                                            Intent(context, PolicyComposeActivity::class.java).apply {
                                                putExtra(PolicyComposeActivity.KEY_THEME_TYPE, 1)
                                            }
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Terms")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionInfoRow(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}